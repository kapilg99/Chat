package kapilg99.android.chat;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private DatabaseReference rootDatabase;

    private Toolbar chatToolbar;
    private TextView userNameView;
    private TextView lastSeenView;
    private CircleImageView avatarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootDatabase = FirebaseDatabase.getInstance().getReference();

        final String userId = getIntent().getStringExtra("userid");
        final String userName = getIntent().getStringExtra("user_name");

        chatToolbar = findViewById(R.id.chat_app_bar);

        setSupportActionBar(chatToolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        View actionBarView = inflater.inflate(R.layout.chat_custom_bar, null);
        actionBar.setCustomView(actionBarView);

        userNameView = findViewById(R.id.chat_bar_user_name);
        lastSeenView = findViewById(R.id.chat_bar_last_seen);
        avatarView = findViewById(R.id.chat_bar_avatar);

        userNameView.setText(userName);
        rootDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((Boolean) dataSnapshot.child("online").getValue()) {
                    lastSeenView.setText("Online");
                } else {
                    long lastSeenTime = Long.parseLong(dataSnapshot.child("last_seen").getValue().toString());
                    String lastSeen = TimeAgo.getTimeAgo(lastSeenTime);
                    if (lastSeen != null) {
                        lastSeenView.setText(lastSeen);
                    } else {
                        lastSeenView.setText("Unavailable");
                    }

                }
                final String userThumbImage = dataSnapshot.child("thumb_image").getValue().toString();
                Picasso.get().load(userThumbImage)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.avatar_default2)
                        .into(avatarView, new Callback() {
                            @Override
                            public void onSuccess() {
                            }

                            @Override
                            public void onError(Exception e) {
                                Picasso.get().load(userThumbImage)
                                        .placeholder(R.drawable.avatar_default2)
                                        .into(avatarView);
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }
}
