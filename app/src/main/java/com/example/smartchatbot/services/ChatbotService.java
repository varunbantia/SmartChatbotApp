package com.example.smartchatbot.services;

import android.util.Log;

import com.example.smartchatbot.api.ChatbotApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatbotService {
    private static final String BASE_URL = "http://172.17.28.67:5000/"; // Added port 5000 // Emulator localhost
    private static ChatbotService instance;
    private final ChatbotApi api;

    private ChatbotService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(ChatbotApi.class);
    }

    public static ChatbotService getInstance() {
        if (instance == null) {
            instance = new ChatbotService();
        }
        return instance;
    }

    public void sendMessage(String message, ChatbotCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);

        api.sendMessage(body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().get("reply"));
                } else {
                    callback.onError("⚠️ Server error");
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e("ChatbotService", "Error: " + t.getMessage());
                callback.onError("❌ " + t.getMessage());
            }
        });
    }

    // Callback interface
    public interface ChatbotCallback {
        void onSuccess(String reply);
        void onError(String error);
    }
}
