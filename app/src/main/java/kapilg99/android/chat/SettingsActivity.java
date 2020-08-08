package kapilg99.android.chat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;
    private FirebaseUser currentUser;
    private Button changeStatus, changeImage;
    private CircleImageView avatar;
    private TextView mDisplayName, mStatus;
    private Toolbar mToolbar;
    private static final int GALLERY_PICK = 1;
    private StorageReference mImageStorage;
    private ProgressDialog progressDialog;
    private TextView dividerView;

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
        assert currentUser != null;
        String uid = currentUser.getUid();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(uid);
        mDatabaseReference.keepSynced(true);
        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                final String image = dataSnapshot.child("image").getValue().toString();
                final String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();

                mDisplayName.setText(name);
                mStatus.setText(status);
//                Picasso.get().load(image).placeholder(R.drawable.avatar_default2).into(avatar);
                Picasso.get().load(thumb_image).networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.avatar_default2).into(avatar, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(thumb_image)
                                .placeholder(R.drawable.avatar_default2).into(avatar);
                    }
                });

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
    protected void onPause() {
        super.onPause();
        mDatabaseReference.child("online").setValue(false);
        mDatabaseReference.child("last_seen").setValue(ServerValue.TIMESTAMP);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDatabaseReference.child("online").setValue(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            String destinationFilename = mDisplayName.getText().toString() + ".jpg";
            UCrop uCrop = UCrop.of(data.getData(), Uri.fromFile(new File(getCacheDir(), destinationFilename)));
            uCrop.withAspectRatio(1, 1);
            uCrop.withMaxResultSize(500, 500);
            uCrop.start(this);
        }
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {

            progressDialog = new ProgressDialog(SettingsActivity.this);
            progressDialog.setTitle("Uploading Image");
            progressDialog.setMessage("Please wait while we upload image..");
            progressDialog.setCanceledOnTouchOutside(false);

            final Uri resultUri = UCrop.getOutput(data);
            String profName = currentUser.getUid();

            File thumb_filepath = new File(resultUri.getPath());
            byte[] compressedByteArray = new byte[0];

            try {
                Bitmap compressedImageBitmap = new Compressor(this)
                        .setMaxHeight(200)
                        .setMaxWidth(200)
                        .setQuality(75)
                        .compressToBitmap(thumb_filepath);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                compressedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                compressedByteArray = baos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("Error in Compression", Arrays.toString(e.getStackTrace()));
            }

            Toast.makeText(this, resultUri.toString(), Toast.LENGTH_SHORT).show();
            final StorageReference filepath = mImageStorage.child("profile_images").child(profName + ".jpg");
            UploadTask uploadTask = filepath.putFile(resultUri);
            Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
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
            final StorageReference thumbPath = mImageStorage.child("profile_images").child("thumbs").child(profName + ".jpg");
            UploadTask uploadTask_thumb = thumbPath.putBytes(compressedByteArray);
            Task<Uri> uriThumbTask = uploadTask_thumb.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return thumbPath.getDownloadUrl();
                }
            })
                    .addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();
                                progressDialog.dismiss();
                                mDatabaseReference.child("thumb_image").setValue(downloadUri.toString())
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(SettingsActivity.this, "Thumb Profile uploaded in Storage and Database", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(SettingsActivity.this, "Thumb Profile uploaded in Storage", Toast.LENGTH_SHORT).show();
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
