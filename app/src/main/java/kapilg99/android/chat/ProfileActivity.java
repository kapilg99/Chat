package kapilg99.android.chat;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
    FirebaseUser currentUser;
    private static final String NOT_FREINDS = "not_friends";
    private static final String REQUEST_SENT = "request_sent";
    private static final String REQUEST_RECEIVED = "request_received";
    private static final String FRIENDS = "friends";
    private static final String UNFRIEND = "unfriend";

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
        currentState = NOT_FREINDS;

        userDB = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
        userDB.keepSynced(true);
        friendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("friend_request");
        friendListDatabase = FirebaseDatabase.getInstance().getReference().child("friends");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User Data");
        mProgressDialog.setMessage("Please wait while we load data...");
        mProgressDialog.setCanceledOnTouchOutside(false);

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
                                sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                sendRequest.setTextColor(Color.BLACK);
                            } else if (requestType.equals("sent")) {
                                currentState = REQUEST_SENT;
                                sendRequest.setText(R.string.cancel_request);
                                sendRequest.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                sendRequest.setTextColor(getResources().getColor(R.color.colorTextIcons));
                                sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
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
                                        sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
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
                if (currentState.equals(NOT_FREINDS)) {
                    if (currentUser.getUid().equals(userId)) {
                        return;
                    }
                    friendRequestDatabase.child(currentUser.getUid())
                            .child(userId)
                            .child("request_type").setValue("sent")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ProfileActivity.this, "Friend Request Sent", Toast.LENGTH_SHORT).show();
                                        friendRequestDatabase.child(userId).child(currentUser.getUid())
                                                .child("request_type").setValue("received")
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(ProfileActivity.this, "Friend Request received", Toast.LENGTH_SHORT).show();
                                                        currentState = REQUEST_SENT;
                                                        sendRequest.setText(R.string.cancel_request);
                                                        sendRequest.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                                        sendRequest.setTextColor(getResources().getColor(R.color.colorTextIcons));
                                                        sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(ProfileActivity.this, "Friend Request Failed", Toast.LENGTH_SHORT).show();
                                    }
                                    sendRequest.setEnabled(true);
                                }
                            });
                }

//      ============================   CANCEL REQUEST   ==========================================
                if (currentState.equals(REQUEST_SENT)) {
                    friendRequestDatabase.child(currentUser.getUid()).child(userId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            friendRequestDatabase.child(userId).child(currentUser.getUid()).removeValue()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            currentState = NOT_FREINDS;
                                            sendRequest.setText(R.string.send_request);
                                            sendRequest.setBackgroundColor(getResources().getColor(R.color.colorTextIcons));
                                            sendRequest.setTextColor(Color.BLACK);
                                            sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                        }
                                    });
                            sendRequest.setEnabled(true);
                        }
                    });
                }
//      ============================   REQUEST RECEIVED   ==========================================
                if (currentState.equals(REQUEST_RECEIVED)) {
                    final String currentdate = DateFormat.getDateTimeInstance().format(new Date());
                    friendListDatabase.child(currentUser.getUid()).child(userId).setValue(currentdate)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    friendListDatabase.child(userId).child(currentUser.getUid()).setValue(currentdate)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    friendRequestDatabase.child(currentUser.getUid()).child(userId).removeValue()
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    friendRequestDatabase.child(userId).child(currentUser.getUid()).removeValue()
                                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void aVoid) {
                                                                                    currentState = FRIENDS;
                                                                                    sendRequest.setText(R.string.unfriend);
                                                                                    sendRequest.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                                                                    sendRequest.setTextColor(getResources().getColor(R.color.colorTextIcons));
                                                                                    sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                                                                }
                                                                            });
                                                                    sendRequest.setEnabled(true);
                                                                }
                                                            });
                                                }
                                            });
                                }
                            });
                }
//      ============================   UNFRIEND   ==========================================
                if (currentState.equals(FRIENDS)) {
                    friendListDatabase.child(currentUser.getUid()).child(userId).removeValue()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    friendListDatabase.child(userId).child(currentUser.getUid()).removeValue()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    currentState = NOT_FREINDS;
                                                    sendRequest.setText(R.string.send_request);
                                                    sendRequest.setBackgroundColor(getResources().getColor(R.color.colorTextIcons));
                                                    sendRequest.setTextColor(Color.BLACK);
                                                    sendRequest.setElevation(3 * getResources().getDisplayMetrics().density);
                                                }
                                            });
                                }
                            });
                }
            }
        });
    }
}
