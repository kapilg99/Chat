package kapilg99.android.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private DatabaseReference userDatabase;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        toolbar = findViewById(R.id.users_app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("All users");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        userDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        userDatabase.keepSynced(true);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        recyclerView = findViewById(R.id.users_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    protected void onStart() {
        super.onStart();

        userDatabase.child(currentUser.getUid()).child("online").setValue(true);

        FirebaseRecyclerOptions<Users> options =
                new FirebaseRecyclerOptions.Builder<Users>()
                        .setQuery(userDatabase, Users.class)
                        .build();
        FirebaseRecyclerAdapter<Users, UsersViewHolder> firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<Users, UsersViewHolder>(options) {
                    @NonNull
                    @Override
                    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.single_user_display, parent, false);
                        return new UsersViewHolder(view);
                    }

                    @Override
                    protected void onBindViewHolder(@NonNull UsersViewHolder holder, int position, @NonNull Users user) {
                        if (currentUser.getUid().equals(getRef(position).getKey())) {
                            return;
                        }
                        holder.setName(user.getName());
                        holder.setStatus(user.getStatus());
//                        holder.setImage(user.getImage());
                        holder.setThumbImage(user.getThumb_image());

                        final String user_id = getRef(position).getKey();

                        holder.view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent profileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                                profileIntent.putExtra("userid", user_id);
                                startActivity(profileIntent);
                            }
                        });
                    }

                    @Override
                    public void onDataChanged() {
                        super.onDataChanged();
                    }
                };
        recyclerView.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();

    }

    @Override
    protected void onPause() {
        super.onPause();
        userDatabase.child(currentUser.getUid()).child("online").setValue(false);
        userDatabase.child(currentUser.getUid()).child("last_seen").setValue(ServerValue.TIMESTAMP);
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {

        View view;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
        }

        public void setName(String name) {
            TextView usernameView = view.findViewById(R.id.username);
            usernameView.setText(name);
        }

        public void setStatus(String status) {
            TextView userStatusView = view.findViewById(R.id.userstatus);
            userStatusView.setText(status);
        }

        public void setImage(final String image) {
            final CircleImageView thumbView = view.findViewById(R.id.user_avatar);
            Picasso.get().load(image)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.avatar_default2)
                    .into(thumbView, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(image)
                                    .placeholder(R.drawable.avatar_default2)
                                    .into(thumbView);
                        }
                    });
        }

        public void setThumbImage(final String thumbImage) {
            final CircleImageView thumbView = view.findViewById(R.id.user_avatar);
            Picasso.get().load(thumbImage)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.avatar_default2)
                    .into(thumbView, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(thumbImage)
                                    .placeholder(R.drawable.avatar_default2)
                                    .into(thumbView);
                        }
                    });
        }
    }
}
