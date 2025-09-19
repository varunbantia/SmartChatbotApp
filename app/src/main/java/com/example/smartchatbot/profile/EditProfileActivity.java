package com.example.smartchatbot.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.smartchatbot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etDob, etSkills, etQualification, etLanguages,
            etExperience, etJobRole, etLocation;
    private Button btnSave, btnUploadImage, btnUploadResume;
    private ImageView ivProfileImage;
    private TextView tvResumeName;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private Uri imageUri = null;
    private Uri resumeUri = null;

    private static final int PICK_IMAGE = 101;
    private static final int PICK_RESUME = 102;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Init views
        ivProfileImage = findViewById(R.id.ivProfileImage);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnUploadResume = findViewById(R.id.btnUploadResume);
        tvResumeName = findViewById(R.id.tvResumeName);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDob = findViewById(R.id.etDob);
        etSkills = findViewById(R.id.etSkills);
        etQualification = findViewById(R.id.etQualification);
        etLanguages = findViewById(R.id.etLanguages);
        etExperience = findViewById(R.id.etExperience);
        etJobRole = findViewById(R.id.etJobRole);
        etLocation = findViewById(R.id.etLocation);
        btnSave = findViewById(R.id.btnSave);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        loadUserProfile();

        btnUploadImage.setOnClickListener(v -> pickImage());
        btnUploadResume.setOnClickListener(v -> pickResume());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void pickResume() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_RESUME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE) {
                imageUri = data.getData();
                ivProfileImage.setImageURI(imageUri);
            } else if (requestCode == PICK_RESUME) {
                resumeUri = data.getData();
                tvResumeName.setText("Resume: " + resumeUri.getLastPathSegment());
            }
        }
    }

    private void loadUserProfile() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                etName.setText(snapshot.getString("name"));
                etEmail.setText(snapshot.getString("email"));
                etPhone.setText(snapshot.getString("phone"));
                etDob.setText(snapshot.getString("dob"));
                etSkills.setText(snapshot.getString("skills"));
                etQualification.setText(snapshot.getString("qualification"));
                etLanguages.setText(snapshot.getString("languages"));
                etExperience.setText(snapshot.getString("experience"));
                etJobRole.setText(snapshot.getString("jobRole"));
                etLocation.setText(snapshot.getString("location"));

                String imageUrl = snapshot.getString("profileImageUrl");
                String resumeUrl = snapshot.getString("resumeUrl");

                if (imageUrl != null) Glide.with(this).load(imageUrl).into(ivProfileImage);
                if (resumeUrl != null) tvResumeName.setText("Resume uploaded");
            }
        });
    }

    private void saveProfile() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", etName.getText().toString().trim());
        profile.put("email", etEmail.getText().toString().trim());
        profile.put("phone", etPhone.getText().toString().trim());
        profile.put("dob", etDob.getText().toString().trim());
        profile.put("skills", etSkills.getText().toString().trim());
        profile.put("qualification", etQualification.getText().toString().trim());
        profile.put("languages", etLanguages.getText().toString().trim());
        profile.put("experience", etExperience.getText().toString().trim());
        profile.put("jobRole", etJobRole.getText().toString().trim());
        profile.put("location", etLocation.getText().toString().trim());

        // Save profile image
        if (imageUri != null) {
            StorageReference imgRef = storageRef.child("users/" + uid + "/profile.jpg");
            imgRef.putFile(imageUri).addOnSuccessListener(task ->
                    imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        profile.put("profileImageUrl", uri.toString());
                        updateFirestore(uid, profile);
                    })
            );
        }

        // Save resume
        if (resumeUri != null) {
            StorageReference resumeRef = storageRef.child("users/" + uid + "/resume.pdf");
            resumeRef.putFile(resumeUri).addOnSuccessListener(task ->
                    resumeRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        profile.put("resumeUrl", uri.toString());
                        updateFirestore(uid, profile);
                    })
            );
        }

        // Always update details
        updateFirestore(uid, profile);
    }

    private void updateFirestore(String uid, Map<String, Object> profile) {
        db.collection("users").document(uid)
                .set(profile)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
