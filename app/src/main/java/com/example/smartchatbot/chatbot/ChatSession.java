package com.example.smartchatbot.chatbot;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatSession {

    private @DocumentId String chatId;
    private String lastMessage;
    private @ServerTimestamp Date timestamp;

    public ChatSession() {} // Required for Firestore

    public ChatSession(String lastMessage, Date timestamp) {
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getChatId() { return chatId; }
    public String getLastMessage() { return lastMessage; }
    public Date getTimestamp() { return timestamp; }
}