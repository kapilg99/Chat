package kapilg99.android.chat;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private DatabaseReference rootDatabase;
    private DatabaseReference messageDatabase;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private Toolbar chatToolbar;
    private TextView userNameView;
    private TextView lastSeenView;
    private CircleImageView avatarView;
    private TextInputEditText chat_msg_body;
    private AppCompatImageButton sendButton;
    private AppCompatImageButton chat_addButton;
    private RecyclerView messagesRecycler;

    private String userId;
    private String userName;
    private List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        userId = getIntent().getStringExtra("userid");
        userName = getIntent().getStringExtra("user_name");

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
        sendButton = findViewById(R.id.chat_send);
        chat_addButton = findViewById(R.id.chat_add_btn);
        chat_msg_body = findViewById(R.id.chat_msg_body);

        messageAdapter = new MessageAdapter(messagesList);
        messagesRecycler = findViewById(R.id.messages_list);
        linearLayoutManager = new LinearLayoutManager(this);
        messagesRecycler.setAdapter(messageAdapter);
        messagesRecycler.setHasFixedSize(true);
        messagesRecycler.setLayoutManager(linearLayoutManager);

        loadMessages();

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

        rootDatabase.child("chat").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(userId)) {
                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("chat/" + currentUserId + "/" + userId, chatAddMap);
                    chatUserMap.put("chat/" + userId + "/" + currentUserId, chatAddMap);

                    rootDatabase.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Toast.makeText(ChatActivity.this, "Message not sent", Toast.LENGTH_SHORT).show();
                                Log.e("ChatActivity", databaseError.getDetails());
                            }
                        }
                    });
                } else {
                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
                chat_msg_body.setText("");
            }
        });
    }

    private void loadMessages() {
        rootDatabase.child("messages").child(currentUserId).child(userId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages message = dataSnapshot.getValue(Messages.class);
                messagesList.add(message);
                messageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage() {
        String message = chat_msg_body.getText().toString().trim();
        if (!TextUtils.isEmpty(message)) {
            DatabaseReference userMessageRef = rootDatabase.child("messages")
                    .child(currentUserId).child(userId).push();
            String pushId = userMessageRef.getKey();
            String currentUserRef = "messages/" + currentUserId + "/" + userId + "/" + pushId;
            String otherUserRef = "messages/" + userId + "/" + currentUserId + "/" + pushId;

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", currentUserId);

            Map mapUserMessage = new HashMap();
            mapUserMessage.put(currentUserRef, messageMap);
            mapUserMessage.put(otherUserRef, messageMap);

            rootDatabase.updateChildren(mapUserMessage, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Toast.makeText(ChatActivity.this, "Message not sent", Toast.LENGTH_SHORT).show();
                        Log.e("ChatActivity", databaseError.getDetails());
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentUserId != null) {
            rootDatabase.child("users").child(currentUserId).child("online").setValue(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        rootDatabase.child("users").child(currentUserId).child("online").setValue(true);
    }


}
