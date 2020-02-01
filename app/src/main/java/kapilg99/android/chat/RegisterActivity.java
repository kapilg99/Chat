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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout mDisplayName, mEmail, mPassword;
    private Button mCreateButton;
    private FirebaseAuth mAuth;
    private MaterialToolbar materialToolbar;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        materialToolbar = findViewById(R.id.register_toolbar);
        mDisplayName = findViewById(R.id.reg_display_name);
        mEmail = findViewById(R.id.reg_email);
        mPassword = findViewById(R.id.reg_password);
        mCreateButton = findViewById(R.id.reg_create_account);
        progressDialog = new ProgressDialog(this);

        setSupportActionBar(materialToolbar);
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

    private void registerUser(String displayName, String email, String password) {
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
                    progressDialog.dismiss();
                    Log.e("RegisterActivity", "createUserWithEmail:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    Toast.makeText(RegisterActivity.this, "Authentication Successful.", Toast.LENGTH_LONG).show();
                    Intent sendToMain = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(sendToMain);
                    finish();
//                            updateUI(user);
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
