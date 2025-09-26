package com.example.smartchatbot.chatbot;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatMessage {

    public static final int TYPE_USER_TEXT = 0;
    public static final int TYPE_BOT = 1;
    public static final int TYPE_USER_VOICE = 2;

    private String message;
    private int type;
    private String audioFilePath;

    // ✅ 1. Added a timestamp field for ordering messages
    private @ServerTimestamp Date timestamp;

    // ✅ 2. Added a required no-argument constructor for Firestore
    public ChatMessage() {}

    // Constructor for text messages
    public ChatMessage(String message, int type) {
        this.message = message;
        this.type = type;
    }

    // Constructor for voice messages
    public ChatMessage(String message, String audioFilePath, int type) {
        this.message = message;
        this.audioFilePath = audioFilePath;
        this.type = type;
    }

    // --- Getters ---
    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    // --- Setters ---
    public void setMessage(String message) {
        this.message = message;
    }

    // ✅ 3. Added the remaining required setters for Firestore
    public void setType(int type) {
        this.type = type;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}