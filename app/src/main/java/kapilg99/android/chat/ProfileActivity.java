package kapilg99.android.chat;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView displayName;
    private TextView displayStatus;
    private TextView displayTotalFriends;
    private ImageView displayPic;
    private Button sendRequest;
    private Button declineRequest;
    private String currentState;

    private DatabaseReference userDB;
    private DatabaseReference friendRequestDatabase;
    private DatabaseReference friendListDatabase;
    private DatabaseReference notificationDatabase;
    private DatabaseReference rootDatabase;
    FirebaseUser currentUser;

    private static final String NOT_FRIENDS = "not_friends";
    private static final String REQUEST_SENT = "request_sent";
    private static final String REQUEST_RECEIVED = "request_received";
    private static final String FRIENDS = "friends";

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String userId = getIntent().getStringExtra("userid");

        displayName = findViewById(R.id.profile_displayName);
        displayStatus = findViewById(R.id.profile_displayStatus);
        displayTotalFriends = findViewById(R.id.profile_totalFriends);
        displayPic = findViewById(R.id.profile_displayPic);
        sendRequest = findViewById(R.id.profile_SendRequest);
        declineRequest = findViewById(R.id.profile_DeclineRequest);
        currentState = NOT_FRIENDS;

        userDB = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
        userDB.keepSynced(true);
        friendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("friend_request");
        friendListDatabase = FirebaseDatabase.getInstance().getReference().child("friends");
        notificationDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
        rootDatabase = FirebaseDatabase.getInstance().getReference();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User Data");
        mProgressDialog.setMessage("Please wait while we load data...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        declineRequest.setVisibility(View.INVISIBLE);
        declineRequest.setEnabled(false);

        userDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                final String image = dataSnapshot.child("image").getValue().toString();
//                String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();

                displayName.setText(name);
                displayStatus.setText(status);
                Picasso.get().load(image).networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.avatar_default2).into(displayPic, new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(image)
                                .placeholder(R.drawable.avatar_default2).into(displayPic);
                    }
                });

                friendRequestDatabase.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(userId)) {
                            String requestType = dataSnapshot.child(userId).child("request_type").getValue().toString();
                            if (requestType.equals("received")) {
                                currentState = REQUEST_RECEIVED;
                                sendRequest.setText(R.string.accept_request);
                                sendRequest.setBackgroundColor(getResources().getColor(R.color.colorTextIcons));
                                if (Build.VERSION.SDK_INT >= 21) {
                                    sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                }
                                sendRequest.setTextColor(Color.BLACK);
                                declineRequest.setEnabled(true);
                                declineRequest.setVisibility(View.VISIBLE);
                            } else if (requestType.equals("sent")) {
                                currentState = REQUEST_SENT;
                                sendRequest.setText(R.string.cancel_request);
                                sendRequest.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                sendRequest.setTextColor(getResources().getColor(R.color.colorTextIcons));
                                if (Build.VERSION.SDK_INT >= 21) {
                                    sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                }
                                declineRequest.setEnabled(false);
                                declineRequest.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            friendListDatabase.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(userId)) {
                                        currentState = FRIENDS;
                                        sendRequest.setText(R.string.unfriend);
                                        sendRequest.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                        sendRequest.setTextColor(getResources().getColor(R.color.colorTextIcons));
                                        if (Build.VERSION.SDK_INT >= 21) {
                                            sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                        }
                                        declineRequest.setEnabled(false);
                                        declineRequest.setVisibility(View.INVISIBLE);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                        mProgressDialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        sendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRequest.setEnabled(false);
//      ============================   IF NOT FREINDS   ==========================================
                if (currentState.equals(NOT_FRIENDS)) {

                    DatabaseReference newNotificationRef = notificationDatabase.child(userId).push();
                    String notificationId = newNotificationRef.getKey();
                    HashMap<String, String> notificationData = new HashMap<>();
                    notificationData.put("from", currentUser.getUid());
                    notificationData.put("type", "request");

                    Map requestMap = new HashMap();
                    requestMap.put("friend_request/" + currentUser.getUid() + "/" + userId + "/request_type", "sent");
                    requestMap.put("friend_request/" + userId + "/" + currentUser.getUid() + "/request_type", "received");
                    requestMap.put("notifications/" + userId + "/" + notificationId, notificationData);

                    rootDatabase.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("Line 196", databaseError.getDetails());
                            } else {
                                Toast.makeText(ProfileActivity.this, "Friend Request Sent", Toast.LENGTH_SHORT).show();
                                currentState = REQUEST_SENT;
                                sendRequest.setText(R.string.cancel_request);
                                sendRequest.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                sendRequest.setTextColor(getResources().getColor(R.color.colorTextIcons));
                                sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                declineRequest.setEnabled(false);
                                declineRequest.setVisibility(View.INVISIBLE);
                            }
                            sendRequest.setEnabled(true);
                        }
                    });
                }

//      ============================   CANCEL REQUEST   ==========================================
                if (currentState.equals(REQUEST_SENT)) {
                    Map requestMap = new HashMap();
                    requestMap.put("friend_request/" + currentUser.getUid() + "/" + userId, null);
                    requestMap.put("friend_request/" + userId + "/" + currentUser.getUid(), null);
                    requestMap.put("notifications/" + userId, null);

                    rootDatabase.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("Line 224", databaseError.getDetails());
                            } else {
                                currentState = NOT_FRIENDS;
                                sendRequest.setText(R.string.send_request);
                                sendRequest.setBackgroundColor(getResources().getColor(R.color.colorTextIcons));
                                sendRequest.setTextColor(Color.BLACK);
                                sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                declineRequest.setEnabled(false);
                                declineRequest.setVisibility(View.INVISIBLE);
                            }
                            sendRequest.setVisibility(View.VISIBLE);
                            sendRequest.setEnabled(true);
                        }
                    });
                }
//      ============================   REQUEST RECEIVED   ==========================================
                if (currentState.equals(REQUEST_RECEIVED)) {
                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());
                    Map requestMap = new HashMap();
                    requestMap.put("friends/" + currentUser.getUid() + "/" + userId + "/date", currentDate);
                    requestMap.put("friends/" + userId + "/" + currentUser.getUid() + "/date", currentDate);
                    requestMap.put("friend_request/" + currentUser.getUid() + "/" + userId, null);
                    requestMap.put("friend_request/" + userId + "/" + currentUser.getUid(), null);

                    rootDatabase.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Log.e("Line 251", databaseError.getDetails());
                            } else {
                                Toast.makeText(ProfileActivity.this, "Request Accepted", Toast.LENGTH_SHORT).show();
                                currentState = FRIENDS;
                                sendRequest.setText(R.string.unfriend);
                                sendRequest.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                sendRequest.setTextColor(getResources().getColor(R.color.colorTextIcons));
                                sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                declineRequest.setEnabled(false);
                                declineRequest.setVisibility(View.INVISIBLE);
                            }
                            sendRequest.setEnabled(true);
                        }
                    });
                }
//      ============================   UNFRIEND   ==========================================
                if (currentState.equals(FRIENDS)) {
                    Map requestMap = new HashMap();
                    requestMap.put("friends/" + currentUser.getUid() + "/" + userId, null);
                    requestMap.put("friends/" + userId + "/" + currentUser.getUid(), null);
                    rootDatabase.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("Line 278", databaseError.getDetails());
                            } else {
                                currentState = NOT_FRIENDS;
                                sendRequest.setText(R.string.send_request);
                                sendRequest.setBackgroundColor(getResources().getColor(R.color.colorTextIcons));
                                sendRequest.setTextColor(Color.BLACK);
                                sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                declineRequest.setEnabled(false);
                                declineRequest.setVisibility(View.INVISIBLE);
                            }
                            sendRequest.setEnabled(true);
                            sendRequest.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });

        declineRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map requestMap = new HashMap();
                requestMap.put("friend_request/" + currentUser.getUid() + "/" + userId, null);
                requestMap.put("friend_request/" + userId + "/" + currentUser.getUid(), null);
                rootDatabase.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("Line 308", databaseError.getDetails());
                        } else {
                            Toast.makeText(ProfileActivity.this, "Friend Request Declined", Toast.LENGTH_SHORT).show();
                            currentState = NOT_FRIENDS;
                            sendRequest.setText(R.string.send_request);
                            sendRequest.setBackgroundColor(getResources().getColor(R.color.colorTextIcons));
                            sendRequest.setTextColor(Color.BLACK);
                            sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                            declineRequest.setEnabled(false);
                            declineRequest.setVisibility(View.INVISIBLE);
                        }
                        sendRequest.setEnabled(true);
                        sendRequest.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        rootDatabase.child("users").child(currentUser.getUid()).child("online").setValue(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        rootDatabase.child("users").child(currentUser.getUid()).child("online").setValue(true);
    }
}
