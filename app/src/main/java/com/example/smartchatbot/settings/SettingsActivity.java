package com.example.smartchatbot.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.example.smartchatbot.R;
// Make sure to import your LoginActivity
// import com.example.smartchatbot.auth.LoginActivity;
import com.example.smartchatbot.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private TextView tvCurrentTheme, tvAppVersion;
    private int selectedTheme = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadTheme(); // Load and apply theme BEFORE setting content view
        setContentView(R.layout.activity_settings);




        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views
        LinearLayout btnLogOut = findViewById(R.id.btnLogOut);
        LinearLayout btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        LinearLayout btnTheme = findViewById(R.id.btnTheme);
        tvCurrentTheme = findViewById(R.id.tvCurrentTheme);
        tvAppVersion = findViewById(R.id.tvAppVersion);

        // Setup UI
        updateThemeTextView();
        setAppVersion();

        // Click Listeners
        btnLogOut.setOnClickListener(v -> showLogoutDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        btnTheme.setOnClickListener(v -> showThemeDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                     startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This is irreversible. All your data will be permanently deleted. Are you sure?")
                .setPositiveButton("DELETE", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        StorageReference profilePicRef = storage.getReference().child("profile_pictures/" + uid);
        StorageReference resumeRef = storage.getReference().child("resumes/" + uid);

        profilePicRef.delete().addOnCompleteListener(task1 ->
                resumeRef.delete().addOnCompleteListener(task2 ->
                        db.collection("users").document(uid).delete().addOnCompleteListener(task3 ->
                                user.delete().addOnCompleteListener(task4 -> {
                                    if (task4.isSuccessful()) {
                                        Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_LONG).show();
                                        // Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        // startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Failed to delete account: " + task4.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                })
                        )
                )
        );
    }

    private void showThemeDialog() {
        String[] themes = {"System Default", "Light", "Dark"};
        new AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themes, selectedTheme, (dialog, which) -> {
                    selectedTheme = which;
                    saveTheme(selectedTheme);
                    applyTheme(selectedTheme);
                    updateThemeTextView();
                    dialog.dismiss();
                })
                .show();
    }

    private void loadTheme() {
        SharedPreferences prefs = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        selectedTheme = prefs.getInt("Theme", 0);
        applyTheme(selectedTheme);
    }

    private void saveTheme(int theme) {
        SharedPreferences.Editor editor = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE).edit();
        editor.putInt("Theme", theme);
        editor.apply();
    }

    private void applyTheme(int theme) {
        switch (theme) {
            case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case 2: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }

    private void updateThemeTextView() {
        switch (selectedTheme) {
            case 1: tvCurrentTheme.setText("Light"); break;
            case 2: tvCurrentTheme.setText("Dark"); break;
            default: tvCurrentTheme.setText("System Default"); break;
        }
    }

    private void setAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            tvAppVersion.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}