package com.example.smartchatbot.chatbot;

public class ChatMessage {
    // New message types to distinguish text from voice
    public static final int TYPE_USER_TEXT = 0;
    public static final int TYPE_BOT = 1;
    public static final int TYPE_USER_VOICE = 2;

    private String message;
    private int type;
    private String audioFilePath; // Field to store the path to the audio file

    // Constructor for text messages
    public ChatMessage(String message, int type) {
        this.message = message;
        this.type = type;
        this.audioFilePath = null;
    }

    // Constructor for voice messages
    public ChatMessage(String message, String audioFilePath, int type) {
        this.message = message;
        this.audioFilePath = audioFilePath;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}