package com.example.smartchatbot.api;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ChatbotApi {

    @POST("/chat")
    Call<Map<String, String>> sendMessage(@Body Map<String, String> body);

    @Multipart
    @POST("/stt") // Or your transcribe endpoint name
    Call<Map<String, String>> sendAudio(
            @Part MultipartBody.Part audio,
            @Part("language") RequestBody language // Add this new parameter
    );
}