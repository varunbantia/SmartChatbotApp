package com.example.smartchatbot.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartchatbot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private LinearLayout step1Layout, step2Layout, step3Layout, step4Layout;
    private EditText etName, etEmail, etPhone, etSkills;
    private Button btnNext1, btnNext2, btnNext3, btnSubmit;
    private ImageButton btnClose;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find Views
        step1Layout = view.findViewById(R.id.step1Layout);
        step2Layout = view.findViewById(R.id.step2Layout);
        step3Layout = view.findViewById(R.id.step3Layout);
        step4Layout = view.findViewById(R.id.step4Layout);

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etSkills = view.findViewById(R.id.etSkills);

        btnNext1 = view.findViewById(R.id.btnNext1);
        btnNext2 = view.findViewById(R.id.btnNext2);
        btnNext3 = view.findViewById(R.id.btnNext3);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        btnClose = view.findViewById(R.id.btnCloseProfile);
        btnClose.setOnClickListener(v -> closeFragment());

        // Step navigation
        btnNext1.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }
            showStep(2);
        });

        btnNext2.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Valid email is required");
                return;
            }
            showStep(3);
        });

        btnNext3.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (phone.isEmpty() || phone.length() < 10) {
                etPhone.setError("Valid phone number is required");
                return;
            }
            showStep(4);
        });

        btnSubmit.setOnClickListener(v -> {
            String skills = etSkills.getText().toString().trim();
            if (skills.isEmpty()) {
                etSkills.setError("Enter at least one skill");
                return;
            }
            saveProfile();
        });

        return view;
    }

    // Show specific step layout and hide others
    private void showStep(int step) {
        step1Layout.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2Layout.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3Layout.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        step4Layout.setVisibility(step == 4 ? View.VISIBLE : View.GONE);
    }

    // Save profile data in Firestore
    private void saveProfile() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String skills = etSkills.getText().toString().trim();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("skills", skills);

        db.collection("users").document(uid).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show();
                    closeFragment();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Close the fragment and hide the container
    private void closeFragment() {
        View containerView = requireActivity().findViewById(R.id.fragmentContainer);
        containerView.setVisibility(View.GONE);
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
