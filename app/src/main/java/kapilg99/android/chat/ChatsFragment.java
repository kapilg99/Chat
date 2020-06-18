package kapilg99.android.chat;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {

    private DatabaseReference convDatabase;
    private DatabaseReference userDatabase;
    private DatabaseReference msgDatabase;
    private FirebaseAuth mAuth;

    private RecyclerView convList;
    private String currentUserId;
    private View mainView;

    public ChatsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mainView = inflater.inflate(R.layout.fragment_chats, container, false);
        convList = mainView.findViewById(R.id.conv_list);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        convDatabase = FirebaseDatabase.getInstance().getReference().child("chat").child(currentUserId);
        convDatabase.keepSynced(true);
        userDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        userDatabase.keepSynced(true);
        msgDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(currentUserId);
        msgDatabase.keepSynced(true);
        convList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        convList.setLayoutManager(linearLayoutManager);

        return mainView;

    }

    @Override
    public void onStart() {
        super.onStart();

        Query conversationQuery = convDatabase.orderByChild("timestamp");

        FirebaseRecyclerOptions<Conversations> options = new FirebaseRecyclerOptions.Builder<Conversations>()
                .setQuery(conversationQuery, Conversations.class)
                .build();
        FirebaseRecyclerAdapter<Conversations, ConvViewHolder> firebaseRecyclerAdapter
                = new FirebaseRecyclerAdapter<Conversations, ConvViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ConvViewHolder holder, int position, @NonNull final Conversations conversation) {
                final String userId = getRef(position).getKey();
                final String[] userName = new String[1];
                Query lastMessageQuery = msgDatabase.child(userId).limitToLast(1);
                lastMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        String lastMessage = dataSnapshot.child("message").getValue().toString();
                        if (lastMessage.matches(".*message_images.*")) {
                            holder.setMessage("Image", conversation.isSeen());
                        } else {
                            holder.setMessage(lastMessage, conversation.isSeen());
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

                Query userInfoQuery = userDatabase.child(userId);
                userInfoQuery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        userName[0] = dataSnapshot.child("name").getValue().toString();
                        holder.setName(userName[0]);
                        holder.setThumb(dataSnapshot.child("thumb_image").getValue().toString());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                holder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                        chatIntent.putExtra("userid", userId);
                        chatIntent.putExtra("user_name", userName[0]);
                        conversation.setSeen(true);
                        startActivity(chatIntent);
                    }
                });
            }

            @NonNull
            @Override
            public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.single_user_display, parent, false);
                return new ConvViewHolder(view);
            }
        };

        convList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    private class ConvViewHolder extends RecyclerView.ViewHolder {
        View view;

        public ConvViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
        }

        public void setMessage(String message, boolean seen) {
            TextView userMessage = view.findViewById(R.id.userstatus);
            userMessage.setText(message);

            if (seen) {
                userMessage.setTypeface(userMessage.getTypeface(), Typeface.NORMAL);
            } else {
                userMessage.setTypeface(userMessage.getTypeface(), Typeface.BOLD);
            }
        }

        public void setName(String username) {
            TextView userName = view.findViewById(R.id.username);
            userName.setText(username);
        }

        public void setThumb(final String userThumb) {
            final CircleImageView thumbView = view.findViewById(R.id.user_avatar);
            Picasso.get().load(userThumb)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.avatar_default2)
                    .into(thumbView, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(userThumb)
                                    .placeholder(R.drawable.avatar_default2)
                                    .into(thumbView);
                        }
                    });
        }
    }
}

