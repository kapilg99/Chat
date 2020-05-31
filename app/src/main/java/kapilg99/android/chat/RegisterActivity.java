package kapilg99.android.chat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout mDisplayName, mEmail, mPassword;
    private Button mCreateButton;
    private FirebaseAuth mAuth;
    private Toolbar mToolbar;

    private ProgressDialog progressDialog;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        mToolbar = findViewById(R.id.register_toolbar);
        mDisplayName = findViewById(R.id.reg_display_name);
        mEmail = findViewById(R.id.reg_email);
        mPassword = findViewById(R.id.reg_password);
        mCreateButton = findViewById(R.id.reg_create_account);
        progressDialog = new ProgressDialog(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Drawable upArrow = ContextCompat.getDrawable(RegisterActivity.this, R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(ContextCompat.getColor(RegisterActivity.this, R.color.colorTextIcons), PorterDuff.Mode.SRC_ATOP);
        RegisterActivity.this.getSupportActionBar().setHomeAsUpIndicator(upArrow);

        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String displayName = mDisplayName.getEditText().getText().toString();
                String email = mEmail.getEditText().getText().toString();
                String password = mPassword.getEditText().getText().toString();

                if (!TextUtils.isEmpty(displayName) || !TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)) {
                    progressDialog.setTitle("Registering user");
                    progressDialog.setMessage("Please wait while we set up your account.");
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                    registerUser(displayName, email, password);
                }
            }
        });
    }

    private void registerUser(final String displayName, String email, String password) {
        if (displayName == null || displayName == "") {
            mDisplayName.setError("Can not be Empty");
            mDisplayName.requestFocus();
        }
        if (email == null || email == "") {
            mEmail.setError("Can not be Empty");
            mEmail.requestFocus();
        }
        if (password == null || password == "") {
            mPassword.setError("Can not be Empty");
            mPassword.requestFocus();
        }
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    String uid = currentUser.getUid();

                    mDatabase = FirebaseDatabase.getInstance().getReference()
                            .child("users").child(uid);

                    String tokenId = FirebaseInstanceId.getInstance().getToken();

                    HashMap<String, String> userMap = new HashMap<>();
                    userMap.put("name", displayName);
                    userMap.put("status", "Hey there, I'm using Chat");
                    userMap.put("image", "https://firebasestorage.googleapis.com/v0/b/chat-7dd7d.appspot.com/o/profile_images%2Favatar_default2.png?alt=media&token=4d21def7-e3a6-4f0a-b84a-6d67667582ea");
                    userMap.put("thumb_image", "https://firebasestorage.googleapis.com/v0/b/chat-7dd7d.appspot.com/o/profile_images%2Favatar_default2.png?alt=media&token=4d21def7-e3a6-4f0a-b84a-6d67667582ea");
                    userMap.put("device_token", tokenId);
                    mDatabase.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                progressDialog.dismiss();
                                Log.e("RegisterActivity", "createUserWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                Toast.makeText(RegisterActivity.this, "Authentication Successful.", Toast.LENGTH_LONG).show();
                                Intent sendToMain = new Intent(RegisterActivity.this, MainActivity.class);
                                sendToMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(sendToMain);
                                finish();
                            }
                        }
                    });

                } else {
                    progressDialog.hide();
//                    https://stackoverflow.com/a/48503254/12785964
                    // If sign in fails, display a message to the user.
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthWeakPasswordException e) {
                        mPassword.setError(e.getMessage());
                        mPassword.requestFocus();
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        mEmail.setError(e.getMessage());
                        mEmail.requestFocus();
                    } catch (FirebaseAuthUserCollisionException e) {
                        mEmail.setError(e.getMessage());
                        mEmail.requestFocus();
                    } catch (Exception e) {
                        Log.e("RegisterActivity", e.getMessage());
                    }
                    Log.e("RegisterActivity", "createUserWithEmail:failure", task.getException());
                    Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_LONG).show();
//                            updateUI(null);
                }
            }
        });
    }
}
