package com.example.smartchatbot.profile;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartchatbot.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    // Step layouts
    private LinearLayout step1Layout, step2Layout, step3Layout, step4Layout;

    // Step 1
    private EditText etName, etEmail, etPhone, etDob;

    // Step 2
    private EditText etEducation, etSkills, etLanguages;

    // Step 3
    private EditText etExperience, etJobRole, etLocation;

    // Step 4
    private ImageView ivProfile;
    private Button btnUploadImage, btnUploadResume, btnSubmit;

    // Buttons
    private Button btnNext1, btnNext2, btnNext3;
    private ImageButton btnCloseProfile;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    // Uri for uploads
    private Uri profileImageUri, resumeUri;

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int PICK_RESUME_REQUEST = 1002;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Step layouts
        step1Layout = view.findViewById(R.id.step1Layout);
        step2Layout = view.findViewById(R.id.step2Layout);
        step3Layout = view.findViewById(R.id.step3Layout);
        step4Layout = view.findViewById(R.id.step4Layout);

        // Step 1
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etDob = view.findViewById(R.id.etDob);

        btnNext1 = view.findViewById(R.id.btnNext1);

        // Step 2
        etEducation = view.findViewById(R.id.etEducation);
        etSkills = view.findViewById(R.id.etSkills);
        etLanguages = view.findViewById(R.id.etLanguages);
        btnNext2 = view.findViewById(R.id.btnNext2);

        // Step 3
        etExperience = view.findViewById(R.id.etExperience);
        etJobRole = view.findViewById(R.id.etJobRole);
        etLocation = view.findViewById(R.id.etLocation);
        btnNext3 = view.findViewById(R.id.btnNext3);

        // Step 4
        ivProfile = view.findViewById(R.id.ivProfile);
        btnUploadImage = view.findViewById(R.id.btnUploadImage);
        btnUploadResume = view.findViewById(R.id.btnUploadResume);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        // Close
        btnCloseProfile = view.findViewById(R.id.btnCloseProfile);

        // Listeners
        etDob.setOnClickListener(v -> showDatePicker());

        btnNext1.setOnClickListener(v -> {
            if (validateStep1()) {
                step1Layout.setVisibility(View.GONE);
                step2Layout.setVisibility(View.VISIBLE);
            }
        });

        btnNext2.setOnClickListener(v -> {
            if (validateStep2()) {
                step2Layout.setVisibility(View.GONE);
                step3Layout.setVisibility(View.VISIBLE);
            }
        });

        btnNext3.setOnClickListener(v -> {
            if (validateStep3()) {
                step3Layout.setVisibility(View.GONE);
                step4Layout.setVisibility(View.VISIBLE);
            }
        });

        btnUploadImage.setOnClickListener(v -> openImagePicker());
        btnUploadResume.setOnClickListener(v -> openResumePicker());

        btnSubmit.setOnClickListener(v -> saveProfileToFirebase());

        btnCloseProfile.setOnClickListener(v -> requireActivity().getSupportFragmentManager().beginTransaction().remove(ProfileFragment.this).commit());

        return view;
    }

    // Step validations
    private boolean validateStep1() {
        // Name validation
        if (TextUtils.isEmpty(etName.getText())) {
            etName.setError("Enter name");
            return false;
        }

        // Email validation
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return false;
        }

        // Phone validation (must be 10 digits)
        String phone = etPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Enter phone");
            return false;
        }
        if (!phone.matches("^[0-9]{10}$")) {
            etPhone.setError("Enter valid 10-digit phone number");
            return false;
        }

        // DOB validation (dd/MM/yyyy format + age check)
        String dob = etDob.getText().toString().trim();
        if (TextUtils.isEmpty(dob)) {
            etDob.setError("Enter DOB");
            return false;
        }
        if (!dob.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
            etDob.setError("Enter DOB in dd/MM/yyyy format (dd/MM/yyyy)");
            return false;
        }

        // Parse DOB and check age
        try {
            String[] parts = dob.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1; // Month is 0-based in Calendar
            int year = Integer.parseInt(parts[2]);

            Calendar dobCalendar = Calendar.getInstance();
            dobCalendar.set(year, month, day);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR);

            if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--; // Adjust if birthday not reached this year
            }

            if (age < 18) {
                etDob.setError("You must be at least 18 years old");
                return false;
            }

        } catch (Exception e) {
            etDob.setError("Invalid DOB format");
            return false;
        }

        return true;
    }


    private boolean validateStep2() {
        if (TextUtils.isEmpty(etEducation.getText())) {
            etEducation.setError("Enter education");
            return false;
        }
        if (TextUtils.isEmpty(etSkills.getText())) {
            etSkills.setError("Enter skills");
            return false;
        }
        if (TextUtils.isEmpty(etLanguages.getText())) {
            etLanguages.setError("Enter languages");
            return false;
        }
        return true;
    }

    private boolean validateStep3() {
        if (TextUtils.isEmpty(etExperience.getText())) {
            etExperience.setError("Enter experience");
            return false;
        }
        if (TextUtils.isEmpty(etJobRole.getText())) {
            etJobRole.setError("Enter job role");
            return false;
        }
        if (TextUtils.isEmpty(etLocation.getText())) {
            etLocation.setError("Enter location");
            return false;
        }
        return true;
    }

    // Date picker
    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) ->
                        etDob.setText(dayOfMonth + "/" + (month + 1) + "/" + year),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    // Pickers
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void openResumePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_RESUME_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                profileImageUri = data.getData();
                ivProfile.setImageURI(profileImageUri);
            } else if (requestCode == PICK_RESUME_REQUEST) {
                resumeUri = data.getData();
                Toast.makeText(getContext(), "Resume Selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Save to Firebase
    private void saveProfileToFirebase() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("name", etName.getText().toString());
        userProfile.put("email", etEmail.getText().toString());
        userProfile.put("phone", etPhone.getText().toString());
        userProfile.put("dob", etDob.getText().toString());
        userProfile.put("education", etEducation.getText().toString());
        userProfile.put("skills", etSkills.getText().toString());
        userProfile.put("languages", etLanguages.getText().toString());
        userProfile.put("experience", etExperience.getText().toString());
        userProfile.put("jobRole", etJobRole.getText().toString());
        userProfile.put("location", etLocation.getText().toString());

        DocumentReference docRef = db.collection("users").document(uid);

        docRef.set(userProfile).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                uploadFiles(uid);
            } else {
                Toast.makeText(getContext(), "Error saving profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadFiles(String uid) {
        if (profileImageUri != null) {
            StorageReference imgRef = storageRef.child("profileImages/" + uid + ".jpg");
            imgRef.putFile(profileImageUri);
        }
        if (resumeUri != null) {
            StorageReference resumeRef = storageRef.child("resumes/" + uid + ".pdf");
            resumeRef.putFile(resumeUri);
        }
        Toast.makeText(getContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().beginTransaction().remove(ProfileFragment.this).commit();
    }
}