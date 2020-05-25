package kapilg99.android.chat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;
    private FirebaseUser currentUser;
    private Button changeStatus, changeImage;
    private CircleImageView avatar;
    TextView mDisplayName, mStatus;
    private Toolbar mToolbar;
    private static final int GALLERY_PICK = 1;
    private StorageReference mImageStorage;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        changeImage = findViewById(R.id.changeImage);
        changeStatus = findViewById(R.id.changeStatus);
        avatar = findViewById(R.id.settings_default_avatar);
        mDisplayName = findViewById(R.id.displayname);
        mStatus = findViewById(R.id.status);

        mImageStorage = FirebaseStorage.getInstance().getReference();

        mToolbar = findViewById(R.id.appbar_settings);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(uid);
        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();
                String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();

                mDisplayName.setText(name);
                mStatus.setText(status);
                Picasso.get().load(image).into(avatar);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        changeStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status_text = mStatus.getText().toString();
                Intent sendToStatus = new Intent(SettingsActivity.this, StatusActivity.class);
                sendToStatus.putExtra("status_text", status_text);
                startActivity(sendToStatus);
            }
        });

        changeImage.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLERY_PICK);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            String destinationFilename = mDisplayName.getText().toString() + ".jpg";
            UCrop uCrop = UCrop.of(data.getData(), Uri.fromFile(new File(getCacheDir(), destinationFilename)));
            uCrop.withAspectRatio(1, 1);
            uCrop.start(this);
        }
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {

            progressDialog = new ProgressDialog(SettingsActivity.this);
            progressDialog.setTitle("Uploading Image");
            progressDialog.setMessage("Please wait while we upload image..");
            progressDialog.setCanceledOnTouchOutside(false);

            final Uri resultUri = UCrop.getOutput(data);
            String profName = currentUser.getUid();
            Toast.makeText(this, resultUri.toString(), Toast.LENGTH_SHORT).show();
            final StorageReference filepath = mImageStorage.child("profile_images").child(profName + ".jpg");
            UploadTask uploadTask = filepath.putFile(resultUri);
            Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return filepath.getDownloadUrl();
                }
            })
                    .addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();
                                progressDialog.dismiss();
                                mDatabaseReference.child("image").setValue(downloadUri.toString())
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(SettingsActivity.this, "Profile uploaded in Storage and Database", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(SettingsActivity.this, "Profile uploaded in Storage", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        }
                    });
        } else if (requestCode == UCrop.RESULT_ERROR) {
            Toast.makeText(this, "UCrop Result error", Toast.LENGTH_SHORT).show();
        }
    }


}
