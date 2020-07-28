package kapilg99.android.chat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final int GALLERY_PICK = 1;
    private DatabaseReference rootDatabase;
    private FirebaseAuth mAuth;
    private String currentUserId;
    FirebaseStorage storage;
    StorageReference storageRef;


    private Toolbar chatToolbar;
    private TextView userNameView;
    private TextView lastSeenView;
    private CircleImageView avatarView;
    private TextInputEditText chat_msg_body;
    private AppCompatImageButton sendButton;
    private AppCompatImageButton chat_addButton;
    private RecyclerView messagesRecycler;
    private SwipeRefreshLayout refreshLayout;

    private String userId;
    private String userName;
    private List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;

    private static final int MESSAGES_TO_LOAD = 20;
    private int itemPosition = 0;
    private String lastMessageKey;
    private String lastPrevMessageKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

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

        refreshLayout = findViewById(R.id.swipeRefreshLayout);
        messageAdapter = new MessageAdapter(messagesList);
        messagesRecycler = findViewById(R.id.messages_list);
        linearLayoutManager = new LinearLayoutManager(this);
        messagesRecycler.setAdapter(messageAdapter);
        messagesRecycler.setHasFixedSize(true);
        messagesRecycler.setItemViewCacheSize(20);
        messagesRecycler.setDrawingCacheEnabled(true);
        messagesRecycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
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
                messagesRecycler.scrollToPosition(messagesList.size() - 1);
            }
        });

        chat_addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLERY_PICK);
            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                itemPosition = 0;
                loadMoreMessages();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            DatabaseReference userMsgPush = rootDatabase.child("messages")
                    .child(currentUserId).child(userId).push();
            final String pushId = userMsgPush.getKey();
            final String currentUserRef = "messages/" + currentUserId + "/" + userId + "/" + pushId;
            final String otherUserRef = "messages/" + userId + "/" + currentUserId + "/" + pushId;
            Toast.makeText(this, pushId, Toast.LENGTH_SHORT).show();
            final StorageReference filePath = storageRef.child("message_images").child(pushId + ".jpg");
            UploadTask uploadTask = filePath.putFile(imageUri);
            Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        Toast.makeText(ChatActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                        throw task.getException();
                    }
                    return filePath.getDownloadUrl();
                }
            })
                    .addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();
                                Toast.makeText(ChatActivity.this, downloadUri.toString(), Toast.LENGTH_SHORT).show();
                                Map messageMap = new HashMap();
                                messageMap.put("message", downloadUri.toString());
                                messageMap.put("seen", false);
                                messageMap.put("type", "image");
                                messageMap.put("time", ServerValue.TIMESTAMP);
                                messageMap.put("from", currentUserId);

                                Map msgUserMap = new HashMap();
                                msgUserMap.put(currentUserRef, messageMap);
                                msgUserMap.put(otherUserRef, messageMap);

                                rootDatabase.updateChildren(msgUserMap, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            Toast.makeText(ChatActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                            Log.e("TAG", databaseError.getDetails());
                                        }
                                    }
                                });

                                chat_msg_body.setText("");

                                messagesRecycler.scrollToPosition(messagesList.size() - 1);
                            } else {
                                Log.e("Upload link to database", task.getException().getMessage());
                            }
                        }
                    });
        }
    }

    private void loadMoreMessages() {
        final DatabaseReference messageReference = rootDatabase.child("messages").child(currentUserId).child(userId);
        Query messageQuery = messageReference.orderByKey().endAt(lastMessageKey).limitToLast(MESSAGES_TO_LOAD);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages message = dataSnapshot.getValue(Messages.class);
                if (!message.getSeen()) {
                    messageReference.child(dataSnapshot.getKey()).child("seen").setValue(true);
                }
                if (!lastPrevMessageKey.equals(dataSnapshot.getKey())) {
                    messagesList.add(itemPosition, message);
                    itemPosition++;
                } else {
                    lastPrevMessageKey = lastMessageKey;
                }
                if (itemPosition == 1) {
                    lastMessageKey = dataSnapshot.getKey();
                }
                messageAdapter.notifyDataSetChanged();
                refreshLayout.setRefreshing(false);
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

    private void loadMessages() {
        final DatabaseReference messageReference = rootDatabase.child("messages").child(currentUserId).child(userId);
        Query messageQuery = messageReference.limitToLast(MESSAGES_TO_LOAD);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages message = dataSnapshot.getValue(Messages.class);
                if (!message.getSeen()) {
                    messageReference.child(dataSnapshot.getKey()).child("seen").setValue(true);
                }
                messagesList.add(itemPosition, message);
                itemPosition++;
                messageAdapter.notifyDataSetChanged();
                refreshLayout.setRefreshing(false);
                messagesRecycler.scrollToPosition(messagesList.size() - 1);
                if (itemPosition == 1) {
                    lastMessageKey = dataSnapshot.getKey();
                    lastPrevMessageKey = lastMessageKey;
                }

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

            Map otherUserMessageMap = new HashMap();
            otherUserMessageMap.put("message", message);
            otherUserMessageMap.put("seen", false);
            otherUserMessageMap.put("type", "text");
            otherUserMessageMap.put("time", ServerValue.TIMESTAMP);
            otherUserMessageMap.put("from", currentUserId);

            Map currentUserMessageMap = new HashMap();
            currentUserMessageMap.put("message", message);
            currentUserMessageMap.put("seen", true);
            currentUserMessageMap.put("type", "text");
            currentUserMessageMap.put("time", ServerValue.TIMESTAMP);
            currentUserMessageMap.put("from", currentUserId);

            String convCurrentUserRef = "chat/" + currentUserId + "/" + userId;
            String convOtherUserRef = "chat/" + userId + "/" + currentUserId;
            Map conversationMap = new HashMap();
            conversationMap.put("seen", true);
            conversationMap.put("timestamp", ServerValue.TIMESTAMP);
            Map conversationUserMap = new HashMap();
            conversationUserMap.put("seen", false);
            conversationUserMap.put("timestamp", ServerValue.TIMESTAMP);

            Map mapUserMessage = new HashMap();
            mapUserMessage.put(currentUserRef, currentUserMessageMap);
            mapUserMessage.put(otherUserRef, otherUserMessageMap);
            mapUserMessage.put(convCurrentUserRef, conversationMap);
            mapUserMessage.put(convOtherUserRef, conversationUserMap);

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
