package kapilg99.android.chat;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class StatusActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private TextInputLayout statusText;
    private Button saveStatus;

    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        statusText = findViewById(R.id.statuslayout);
        saveStatus = findViewById(R.id.set_status_button);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(uid);

        mToolbar = findViewById(R.id.appbar_status);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Your Status");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String status_text = getIntent().getStringExtra("status_text");
        statusText.getEditText().setText(status_text);

        saveStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog = new ProgressDialog(StatusActivity.this);
                progressDialog.setTitle("Saving Changes");
                progressDialog.setMessage("Please wait while we save the changes");
                String status = statusText.getEditText().getText().toString();
                progressDialog.show();
                mDatabase.child("status").setValue(status).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            progressDialog.dismiss();

                        } else {
                            Toast.makeText(StatusActivity.this, "Changes not saved", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}
