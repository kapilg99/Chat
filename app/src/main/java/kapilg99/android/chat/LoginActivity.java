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
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button mSignInButton;
    private FirebaseAuth mAuth;
    private Toolbar mToolbar;
    private ProgressDialog progressDialog;
    private DatabaseReference userDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        userDatabase = FirebaseDatabase.getInstance().getReference().child("users");

        mToolbar = findViewById(R.id.login_toolbar);
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mSignInButton = findViewById(R.id.email_sign_in_button);
        progressDialog = new ProgressDialog(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Sign in");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Drawable upArrow = ContextCompat.getDrawable(LoginActivity.this, R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(ContextCompat.getColor(LoginActivity.this, R.color.colorTextIcons), PorterDuff.Mode.SRC_ATOP);
        LoginActivity.this.getSupportActionBar().setHomeAsUpIndicator(upArrow);

        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmail.getText().toString();
                String password = mPassword.getText().toString();
                progressDialog.setTitle("Logging in");
                progressDialog.setMessage("Please wait while we check your credentials.");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                if (!TextUtils.isEmpty(password) || !TextUtils.isEmpty(email)) {
                    loginUser(email, password);
                }
            }
        });
    }

    private void loginUser(String email, String password) {

        if (email == null || email == "") {
            mEmail.setError("Can not be Empty");
            mEmail.requestFocus();
        }
        if (password == null || password == "") {
            mPassword.setError("Can not be Empty");
            mPassword.requestFocus();
        }
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    progressDialog.dismiss();
                    String deviceToken = FirebaseInstanceId.getInstance().getToken();
                    String currentUserId = mAuth.getCurrentUser().getUid();

                    userDatabase.child(currentUserId).child("device_token").setValue(deviceToken)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(LoginActivity.this, "Device token stored", Toast.LENGTH_SHORT).show();
                                }
                            });

                    // Sign in success, update UI with the signed-in user's information
                    Log.e("LoginActivity", "signInWithEmail:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    Toast.makeText(LoginActivity.this, "Authentication Successful.", Toast.LENGTH_LONG).show();
                    Intent sendToMain = new Intent(LoginActivity.this, MainActivity.class);
                    sendToMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(sendToMain);
                    finish();
//                            updateUI(user);
                } else {
                    // If sign in fails, display a message to the user.
                    progressDialog.hide();
                    Log.e("LoginActivity", "signInWithEmail:failure", task.getException());
                    Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}
