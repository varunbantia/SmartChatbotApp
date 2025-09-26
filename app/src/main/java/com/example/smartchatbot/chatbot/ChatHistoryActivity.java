package com.example.smartchatbot.chatbot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartchatbot.R;
import com.example.smartchatbot.dashboard.ChatbotActivity;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Objects;

public class ChatHistoryActivity extends AppCompatActivity {

    private RecyclerView rvChatHistory;
    private FloatingActionButton fabNewChat;
    private LinearLayout emptyStateLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ChatHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvChatHistory = findViewById(R.id.rvChatHistory);
        fabNewChat = findViewById(R.id.fabNewChat);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        setupRecyclerView();

        fabNewChat.setOnClickListener(v -> {
            // Start a new chat without a chat ID
            startActivity(new Intent(ChatHistoryActivity.this, ChatbotActivity.class));
        });
    }

    private void setupRecyclerView() {
        if (mAuth.getCurrentUser() == null) {
            // Handle user not logged in
            return;
        }

        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        // Query to get chat sessions ordered by the most recent
        Query query = db.collection("users").document(uid).collection("chats")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatSession> options = new FirestoreRecyclerOptions.Builder<ChatSession>()
                .setQuery(query, ChatSession.class)
                .build();

        adapter = new ChatHistoryAdapter(options, chatId -> {
            // When a chat is clicked, open ChatbotActivity with the chat ID
            Intent intent = new Intent(ChatHistoryActivity.this, ChatbotActivity.class);
            intent.putExtra("CHAT_ID", chatId);
            startActivity(intent);
        }) {
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                // Show empty state layout if no items
                emptyStateLayout.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        };

        rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        rvChatHistory.setHasFixedSize(true); // improves performance
        rvChatHistory.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
