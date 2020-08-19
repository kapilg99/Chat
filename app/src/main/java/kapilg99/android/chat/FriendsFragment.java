package kapilg99.android.chat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {

    private DatabaseReference friendsDatabase;
    private DatabaseReference userDatabase;
    private FirebaseAuth mAuth;
    private RecyclerView friendsList;
    private String currentUserId;
    private View mainView;

    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mainView = inflater.inflate(R.layout.fragment_friends, container, false);

        friendsList = mainView.findViewById(R.id.friends_list);
        mAuth = FirebaseAuth.getInstance();
        currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        friendsDatabase = FirebaseDatabase.getInstance().getReference().child("friends").child(currentUserId);
        friendsDatabase.keepSynced(true);
        userDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        userDatabase.keepSynced(true);
        friendsList.setHasFixedSize(true);
        friendsList.setLayoutManager(new LinearLayoutManager(getContext()));

        return mainView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Friends> options = new FirebaseRecyclerOptions.Builder<Friends>()
                .setQuery(friendsDatabase, Friends.class)
                .build();
        FirebaseRecyclerAdapter<Friends, FriendsViewHolder> recyclerAdapter
                = new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsViewHolder holder, int position, @NonNull Friends friend) {
                holder.setDate(friend.getDate());
                final String userId = getRef(position).getKey();
                assert userId != null;
                userDatabase.child(userId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String userName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                        final String userThumbImage = Objects.requireNonNull(dataSnapshot.child("thumb_image").getValue()).toString();
                        holder.setName(userName);
                        holder.setThumbImage(userThumbImage);
                        if (dataSnapshot.hasChild("online")) {
                            Boolean onlineStatus = (Boolean) dataSnapshot.child("online").getValue();
                            assert onlineStatus != null;
                            holder.setUserOnline(onlineStatus);
                        }
                        holder.view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CharSequence[] friendOptions = {"Send Message", "Open Profile"};
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setItems(friendOptions, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 1) {
                                            Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                            profileIntent.putExtra("userid", userId);
                                            startActivity(profileIntent);
                                        } else if (which == 0) {
                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            chatIntent.putExtra("userid", userId);
                                            chatIntent.putExtra("user_name", userName);
                                            startActivity(chatIntent);
                                        }
                                    }
                                });
                                builder.show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });

            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.single_user_display, parent, false);
                return new FriendsViewHolder(view);
            }
        };
        friendsList.setAdapter(recyclerAdapter);
        recyclerAdapter.startListening();
    }

    class FriendsViewHolder extends RecyclerView.ViewHolder {
        View view;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
        }

        public void setDate(String date) {
            TextView userStatusView = view.findViewById(R.id.userstatus);
            userStatusView.setText(date);
        }

        public void setName(String userName) {
            TextView userDisplayName = view.findViewById(R.id.username);
            userDisplayName.setText(userName);
        }

        public void setUserOnline(Boolean online) {
            ImageView userOnlineView = view.findViewById(R.id.online_status);
            if (online) {
                userOnlineView.setVisibility(View.VISIBLE);
            } else {
                userOnlineView.setImageResource(R.drawable.user_offline_24px);
                userOnlineView.setVisibility(View.VISIBLE);
            }
        }

        public void setThumbImage(final String userThumbImage) {
            final CircleImageView thumbView = view.findViewById(R.id.user_avatar);
            Picasso.get().load(userThumbImage)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.avatar_default2)
                    .into(thumbView, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(userThumbImage)
                                    .placeholder(R.drawable.avatar_default2)
                                    .into(thumbView);
                        }
                    });
        }
    }
}
