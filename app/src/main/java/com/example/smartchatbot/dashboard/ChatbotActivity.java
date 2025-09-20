package com.example.smartchatbot.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartchatbot.R;
import com.example.smartchatbot.chatbot.ChatAdapter;
import com.example.smartchatbot.chatbot.ChatMessage;
import com.example.smartchatbot.counseling.CounselingFragment;
import com.example.smartchatbot.jobs.JobsFragment;
import com.example.smartchatbot.profile.EditProfileActivity;
import com.example.smartchatbot.profile.ProfileFragment;
import com.example.smartchatbot.settings.SettingsActivity;
import com.example.smartchatbot.skills.SkillsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnMic, btnMenu;
    private TextView tvWelcome;
    private TextToSpeech tts;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        btnMenu = findViewById(R.id.btnMenu);
        tvWelcome = findViewById(R.id.tvWelcome);

        // Initialize chat
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Load user data with real-time updates
        loadUserNameAndCheckProfile();

        // Text-to-Speech init
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
            }
        });

        // Send button
        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                addUserMessage(message);
                etMessage.setText("");
                addBotMessage("🤖 I am still learning! Please explore more at www.pgrkam.com.");
            }
        });

        // Menu button → popup menu
        btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(ChatbotActivity.this, v);
            popup.getMenuInflater().inflate(R.menu.chatbot_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_jobs) {
                    loadFragment(new JobsFragment());
                    return true;
                } else if (id == R.id.menu_skills) {
                    loadFragment(new SkillsFragment());
                    return true;
                } else if (id == R.id.menu_counseling) {
                    loadFragment(new CounselingFragment());
                    return true;
                } else if (id == R.id.menu_profile) {
                    startActivity(new Intent(ChatbotActivity.this, EditProfileActivity.class));
                    return true;
                } else if (id == R.id.menu_settings) {
                    startActivity(new Intent(ChatbotActivity.this, SettingsActivity.class));
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    /**
     * Load current user info from Firestore with real-time updates.
     * Updates tvWelcome automatically if user updates their name.
     */
    private void loadUserNameAndCheckProfile() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (uid == null) {
            // No user logged in
            tvWelcome.setText("👋 Welcome!");
            addBotMessage("👋 Welcome! I am your PGRKAM Assistant.\nAsk me about jobs, skills, or foreign counseling.");
            showProfileCompletionDialog();
            return;
        }

        // Real-time listener
        db.collection("users").document(uid)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        // Error
                        tvWelcome.setText("👋 Welcome!");
                        addBotMessage("👋 Welcome! I am your PGRKAM Assistant.\nAsk me about jobs, skills, or foreign counseling.");
                        showProfileCompletionDialog();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");

                        // Update welcome TextView
                        if (name != null && !name.isEmpty()) {
                            tvWelcome.setText("👋 Welcome, " + name + "!");
                        } else {
                            tvWelcome.setText("👋 Welcome!");
                        }

                        // Check profile completion
                        checkProfileCompletion(documentSnapshot);

                    } else {
                        tvWelcome.setText("👋 Welcome!");
                        showProfileCompletionDialog();
                    }
                });
    }

    /**
     * Check if the user profile is complete (e.g., missing skills)
     */
    private void checkProfileCompletion(DocumentSnapshot document) {
        if (document != null && document.exists()) {
            String skills = document.getString("skills");
            if (skills == null || skills.trim().isEmpty()) {
                // Prompt user to complete profile
                showProfileCompletionDialog();
            }
        } else {
            showProfileCompletionDialog();
        }
    }

    /**
     * Show profile completion dialog
     */
    private void showProfileCompletionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Complete Your Profile")
                .setMessage("To get personalized job and skill recommendations, please complete your profile.")
                .setPositiveButton("Go to Profile", (dialog, which) -> loadFragment(new ProfileFragment()))
                .setNegativeButton("Maybe Later", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    /**
     * Add user message to chat
     */
    private void addUserMessage(String message) {
        chatMessages.add(new ChatMessage(message, ChatMessage.TYPE_USER));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    /**
     * Add bot message to chat
     */
    private void addBotMessage(String message) {
        chatMessages.add(new ChatMessage(message, ChatMessage.TYPE_BOT));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    /**
     * Load fragment in fragmentContainer
     */
    private void loadFragment(Fragment fragment) {
        FrameLayout container = findViewById(R.id.fragmentContainer);
        container.setVisibility(View.VISIBLE);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragmentContainer, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    /**
     * Speak text using TTS
     */
    public void speakText(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
