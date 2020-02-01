package kapilg99.android.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    private Button mRegButton, mSigninButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mRegButton = findViewById(R.id.start_reg_activity);
        mSigninButton = findViewById(R.id.start_login_activity);

        mRegButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent regIntent = new Intent(StartActivity.this, RegisterActivity.class);
                startActivity(regIntent);
            }
        });

        mSigninButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent regIntent = new Intent(StartActivity.this, LoginActivity.class);
                startActivity(regIntent);
            }
        });


    }
}
