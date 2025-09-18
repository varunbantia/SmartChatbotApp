package com.example.smartchatbot.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartchatbot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etSkills, etEmail, etPhone;
    private Button btnSave;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize views
        etName = findViewById(R.id.etName);
        etSkills = findViewById(R.id.etSkills);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSave = findViewById(R.id.btnSave);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Load current user data
        loadUserProfile();

        // Save button
        btnSave.setOnClickListener(v -> saveProfile());
    }

    /**
     * Load current user profile from Firestore
     */
    private void loadUserProfile() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String skills = documentSnapshot.getString("skills");
                String email = documentSnapshot.getString("email");
                String phone = documentSnapshot.getString("phone");

                etName.setText(name != null ? name : "");
                etSkills.setText(skills != null ? skills : "");
                etEmail.setText(email != null ? email : "");
                etPhone.setText(phone != null ? phone : "");
            }
        });
    }

    /**
     * Save updated profile to Firestore
     */
    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String skills = etSkills.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone is required");
            return;
        }

        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("skills", skills);
        updates.put("email", email);
        updates.put("phone", phone);

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(EditProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
