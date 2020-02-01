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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class LoginActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button mSignInButton;
    private FirebaseAuth mAuth;
    private MaterialToolbar materialToolbar;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        materialToolbar = findViewById(R.id.login_toolbar);
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mSignInButton = findViewById(R.id.email_sign_in_button);
        progressDialog = new ProgressDialog(this);

        setSupportActionBar(materialToolbar);
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
                    // Sign in success, update UI with the signed-in user's information
                    Log.e("LoginActivity", "signInWithEmail:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    Toast.makeText(LoginActivity.this, "Authentication Successful.", Toast.LENGTH_LONG).show();
                    Intent sendToMain = new Intent(LoginActivity.this, MainActivity.class);
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
