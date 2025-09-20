package com.example.smartchatbot.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    // Views
    private ImageView ivProfileImage;
    private TextView tvName, tvEmail, tvPhone, tvDob, tvResumeName, btnUploadImage;
    private EditText etSkills, etQualification, etLanguages, etExperience, etJobRole, etLocation;
    private Button btnSave, btnUploadResume;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // URIs for new file selections
    private Uri newImageUri = null;
    private Uri newResumeUri = null;

    // Modern ActivityResultLaunchers
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> resumePickerLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initializeViews();
        initializeFirebase();
        initializeLaunchers();
        setupClickListeners();

        loadUserProfile();
    }

    private void initializeViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnUploadResume = findViewById(R.id.btnUploadResume);
        tvResumeName = findViewById(R.id.tvResumeName);
        progressBar = findViewById(R.id.progressBar);

        // Non-editable TextViews (Corrected from EditText)
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvDob = findViewById(R.id.tvDob);

        // Editable EditTexts
        etSkills = findViewById(R.id.etSkills);
        etQualification = findViewById(R.id.etQualification);
        etLanguages = findViewById(R.id.etLanguages);
        etExperience = findViewById(R.id.etExperience);
        etJobRole = findViewById(R.id.etJobRole);
        etLocation = findViewById(R.id.etLocation);

        btnSave = findViewById(R.id.btnSave);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void initializeLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        newImageUri = result.getData().getData();
                        Glide.with(this).load(newImageUri).into(ivProfileImage);
                    }
                });

        resumePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        newResumeUri = result.getData().getData();
                        tvResumeName.setText(newResumeUri.getLastPathSegment());
                    }
                });
    }

    private void setupClickListeners() {
        View.OnClickListener pickImageListener = v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        };
        btnUploadImage.setOnClickListener(pickImageListener);
        ivProfileImage.setOnClickListener(pickImageListener); // Make image itself clickable

        btnUploadResume.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            resumePickerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadUserProfile() {
        progressBar.setVisibility(View.VISIBLE);
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            finish(); // Close activity if user is not logged in
            return;
        }

        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // Load non-editable fields
                tvName.setText("Name: " + snapshot.getString("name"));
                tvEmail.setText("Email: " + snapshot.getString("email"));
                tvPhone.setText("Phone: " + snapshot.getString("phone"));
                tvDob.setText("DOB: " + snapshot.getString("dob"));

                // Load editable fields
                etSkills.setText(snapshot.getString("skills"));
                etQualification.setText(snapshot.getString("qualification"));
                etLanguages.setText(snapshot.getString("languages"));
                etExperience.setText(snapshot.getString("experience"));
                etJobRole.setText(snapshot.getString("preferredJobRole"));
                etLocation.setText(snapshot.getString("preferredLocation"));

                // Load profile image and resume info
                String imageUrl = snapshot.getString("profilePictureUrl");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_user).into(ivProfileImage);
                }

                String resumeUrl = snapshot.getString("resumeUrl");
                if (resumeUrl != null && !resumeUrl.isEmpty()) {
                    tvResumeName.setText("Current resume is on file");
                } else {
                    tvResumeName.setText("No resume uploaded");
                }
            } else {
                Toast.makeText(this, "Profile not found. Please complete it first.", Toast.LENGTH_LONG).show();
            }
            progressBar.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }


    /**
     * This is the master save function that starts the sequential process.
     */
    private void saveProfile() {
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        String uid = mAuth.getCurrentUser().getUid();

        // Get all text field data into a map
        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("skills", etSkills.getText().toString().trim());
        profileUpdates.put("qualification", etQualification.getText().toString().trim());
        profileUpdates.put("languages", etLanguages.getText().toString().trim());
        profileUpdates.put("experience", etExperience.getText().toString().trim());
        profileUpdates.put("preferredJobRole", etJobRole.getText().toString().trim());
        profileUpdates.put("preferredLocation", etLocation.getText().toString().trim());

        // Start the upload chain, beginning with the profile image.
        // The final update to Firestore will happen at the end of the chain.
        uploadProfileImage(uid, profileUpdates);
    }

    /**
     * Step 1: Uploads the profile image if a new one was selected.
     * On success, it proceeds to the resume upload step.
     */
    private void uploadProfileImage(String uid, Map<String, Object> profileUpdates) {
        if (newImageUri != null) {
            StorageReference imgRef = storage.getReference().child("profile_pictures/" + uid);
            imgRef.putFile(newImageUri)
                    .addOnSuccessListener(task -> imgRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                profileUpdates.put("profilePictureUrl", uri.toString());
                                // Image done, now upload resume
                                uploadResume(uid, profileUpdates);
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetButtonState();
                    });
        } else {
            // No new image, proceed directly to resume upload
            uploadResume(uid, profileUpdates);
        }
    }

    /**
     * Step 2: Uploads the resume if a new one was selected.
     * On success, it proceeds to the final Firestore update step.
     */
    private void uploadResume(String uid, Map<String, Object> profileUpdates) {
        if (newResumeUri != null) {
            StorageReference resumeRef = storage.getReference().child("resumes/" + uid);
            resumeRef.putFile(newResumeUri)
                    .addOnSuccessListener(task -> resumeRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                profileUpdates.put("resumeUrl", uri.toString());
                                // Resume done, now update Firestore
                                updateFirestore(uid, profileUpdates);
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Resume upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetButtonState();
                    });
        } else {
            // No new resume, proceed to update Firestore with whatever data we have
            updateFirestore(uid, profileUpdates);
        }
    }

    /**
     * Step 3: The final step. Updates Firestore with all collected text and URL data.
     */
    private void updateFirestore(String uid, Map<String, Object> profile) {
        // We also need to mark the profile as complete if it wasn't already.
        profile.put("isProfileComplete", true);

        db.collection("users").document(uid)
                .update(profile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after saving
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButtonState();
                });
    }

    private void resetButtonState() {
        progressBar.setVisibility(View.GONE);
        btnSave.setEnabled(true);
    }
}