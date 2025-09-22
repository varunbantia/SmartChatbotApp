package com.example.smartchatbot.dashboard;

// Import necessary classes
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartchatbot.R;
import com.example.smartchatbot.api.ChatbotApi;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatbotActivity extends AppCompatActivity implements ChatAdapter.OnPlayButtonClickListener {

    // --- Views ---
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private EditText etMessage;
    private ImageButton btnSend, btnMic, btnMenu;
    private TextView tvWelcome;
    private LinearLayout textInputLayout, voiceRecordingLayout, reviewVoiceLayout;
    private TextView tvRecordingTime, tvSlideToCancel, tvVoiceDuration;
    private ImageButton btnDeleteVoice, btnPlayPause, btnSendVoice, btnStopRecording;
    private ImageView ivRecordingDot;
    private SeekBar voiceSeekBar;
    private Spinner languageSpinner;

    // --- Data & Logic ---
    private List<ChatMessage> chatMessages;
    private TextToSpeech tts;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ChatbotApi api;
    private Handler timerHandler = new Handler();

    // --- Audio Recording & Playback ---
    private MediaRecorder recorder;
    private MediaPlayer reviewPlayer;
    private File audioFile;
    private boolean isRecording = false;
    private boolean isLocked = false;
    private long startTime = 0;
    private String selectedLanguageCode = "en-IN";

    // --- Playback Tracking for RecyclerView ---
    private MediaPlayer chatPlayer;
    private ImageButton currentlyPlayingButton = null;
    private SeekBar currentlyPlayingSeekBar = null;
    private String currentlyPlayingFilePath = null;

    // --- Constants ---
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final float CANCEL_THRESHOLD_DP = 100;
    private static final float LOCK_THRESHOLD_DP = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        initializeViews();
        setupRecyclerView();
        setupFirebase();
        setupTts();
        setupRetrofit();
        setupInputListeners();
        setupMenuListener();
        // setupLanguageSpinner(); // Uncomment this if you add the Spinner to your XML
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        btnMenu = findViewById(R.id.btnMenu);
        tvWelcome = findViewById(R.id.tvWelcome);
        textInputLayout = findViewById(R.id.textInputLayout);
        voiceRecordingLayout = findViewById(R.id.voiceRecordingLayout);
        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        tvSlideToCancel = findViewById(R.id.tvSlideToCancel);
        ivRecordingDot = findViewById(R.id.ivRecordingDot);
        reviewVoiceLayout = findViewById(R.id.reviewVoiceLayout);
        btnDeleteVoice = findViewById(R.id.btnDeleteVoice);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnSendVoice = findViewById(R.id.btnSendVoice);
        btnStopRecording = findViewById(R.id.btnStopRecording);
        voiceSeekBar = findViewById(R.id.voiceSeekBar);
        tvVoiceDuration = findViewById(R.id.tvVoiceDuration);
        // languageSpinner = findViewById(R.id.languageSpinner);
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadUserNameAndCheckProfile();
    }

    private void setupTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
            }
        });
    }

    private void setupRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(logging).build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.29.140:5000/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(ChatbotApi.class);
    }

    private void setupLanguageSpinner() {
        // This requires you to add a Spinner with the id 'languageSpinner' to your activity_chatbot.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) selectedLanguageCode = "en-IN";
                else if (position == 1) selectedLanguageCode = "hi-IN";
                else if (position == 2) selectedLanguageCode = "pa-IN";
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupInputListeners() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    btnSend.setVisibility(View.GONE);
                    btnMic.setVisibility(View.VISIBLE);
                } else {
                    btnSend.setVisibility(View.VISIBLE);
                    btnMic.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                addUserMessage(message, null);
                etMessage.setText("");
                sendMessageToBot(message);
            }
        });

        btnMic.setOnTouchListener((v, event) -> {
            float initialX = event.getX(), initialY = event.getY();
            float cancelThresholdPx = CANCEL_THRESHOLD_DP * getResources().getDisplayMetrics().density;
            float lockThresholdPx = LOCK_THRESHOLD_DP * getResources().getDisplayMetrics().density;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (checkPermissions()) startRecording(); else requestPermissions();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (isRecording && !isLocked) {
                        if (initialY - event.getY() > lockThresholdPx) {
                            lockRecording();
                        } else if (event.getX() < initialX - cancelThresholdPx) {
                            tvSlideToCancel.setText("Release to cancel");
                        } else {
                            tvSlideToCancel.setText("< Slide to cancel");
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    if (isRecording && !isLocked) {
                        if (event.getX() < initialX - cancelThresholdPx) {
                            cancelRecording();
                        } else {
                            stopAndSendRecording();
                        }
                    }
                    return true;
            }
            return false;
        });

        btnStopRecording.setOnClickListener(v -> stopRecordingAndShowReview());
        btnDeleteVoice.setOnClickListener(v -> deleteRecording());
        btnPlayPause.setOnClickListener(v -> playOrPauseReview());
        btnSendVoice.setOnClickListener(v -> sendRecording());
    }

    // --- Recording Logic ---
    private void startRecording() {
        try {
            audioFile = File.createTempFile("voice_input", ".3gp", getCacheDir());
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            isRecording = true;
            textInputLayout.setVisibility(View.GONE);
            voiceRecordingLayout.setVisibility(View.VISIBLE);
            startTime = System.currentTimeMillis();
            timerHandler.post(updateTimer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void lockRecording() {
        isLocked = true;
        tvSlideToCancel.setVisibility(View.GONE);
        ivRecordingDot.setVisibility(View.GONE);
        btnStopRecording.setVisibility(View.VISIBLE);
    }

    private void stopAndSendRecording() {
        if (!isRecording) return;
        try {
            recorder.stop(); recorder.release(); recorder = null;
            isRecording = false;
            timerHandler.removeCallbacks(updateTimer);
            addUserMessage("🎤 Voice Message", audioFile.getAbsolutePath());
            sendAudioToBackend(audioFile);
        } catch (Exception e) { e.printStackTrace(); } finally {
            resetInputUI();
        }
    }

    private void cancelRecording() {
        if (!isRecording) return;
        try {
            recorder.stop(); recorder.release(); recorder = null;
            isRecording = false;
            timerHandler.removeCallbacks(updateTimer);
            if (audioFile != null) audioFile.delete();
            addUserMessage("Voice recording cancelled.", null);
        } catch (Exception e) { e.printStackTrace(); } finally {
            resetInputUI();
        }
    }

    // --- Review Panel Logic ---
    private void stopRecordingAndShowReview() {
        if (!isRecording) return;
        try {
            recorder.stop(); recorder.release(); recorder = null;
            isRecording = false;
            timerHandler.removeCallbacks(updateTimer);
            voiceRecordingLayout.setVisibility(View.GONE);
            reviewVoiceLayout.setVisibility(View.VISIBLE);
            prepareMediaPlayer();
        } catch (Exception e) { e.printStackTrace(); resetInputUI(); }
    }

    private void prepareMediaPlayer() {
        try {
            reviewPlayer = new MediaPlayer();
            reviewPlayer.setDataSource(audioFile.getAbsolutePath());
            reviewPlayer.prepare();
            int duration = reviewPlayer.getDuration();
            voiceSeekBar.setMax(duration);
            tvVoiceDuration.setText(String.format(Locale.getDefault(), "%d:%02d", (duration / 1000) / 60, (duration / 1000) % 60));
            reviewPlayer.setOnCompletionListener(mp -> {
                btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
                voiceSeekBar.setProgress(0);
            });
            voiceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser) reviewPlayer.seekTo(progress); }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void playOrPauseReview() {
        if (reviewPlayer == null) return;
        if (reviewPlayer.isPlaying()) {
            reviewPlayer.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
            timerHandler.removeCallbacks(updateReviewSeekBar);
        } else {
            reviewPlayer.start();
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            timerHandler.post(updateReviewSeekBar);
        }
    }

    private void deleteRecording() {
        if (reviewPlayer != null) { reviewPlayer.release(); reviewPlayer = null; }
        if (audioFile != null && audioFile.exists()) audioFile.delete();
        resetInputUI();
    }

    private void sendRecording() {
        if (audioFile != null) {
            addUserMessage("🎤 Voice Message", audioFile.getAbsolutePath());
            sendAudioToBackend(audioFile);
        }
        resetInputUI();
    }

    // --- Playback from Chat Bubble ---
    @Override
    public void onPlayButtonClick(String filePath, ImageButton playButton, SeekBar seekBar, TextView durationView) {
        // If another audio file is already playing, stop it first.
        if (chatPlayer != null && chatPlayer.isPlaying()) {
            chatPlayer.stop();
            chatPlayer.release();
            if (currentlyPlayingButton != null) {
                currentlyPlayingButton.setImageResource(R.drawable.ic_play_arrow);
            }
            if (currentlyPlayingSeekBar != null) {
                currentlyPlayingSeekBar.setProgress(0);
            }
            // If the user clicked the same button that was playing, it was a "stop" action.
            if (filePath.equals(currentlyPlayingFilePath)) {
                chatPlayer = null;
                return;
            }
        }

        try {
            // Store the views of the item that is now playing
            currentlyPlayingFilePath = filePath;
            currentlyPlayingButton = playButton;
            currentlyPlayingSeekBar = seekBar;

            // Start playback
            chatPlayer = new MediaPlayer();
            chatPlayer.setDataSource(filePath);
            chatPlayer.prepare();
            chatPlayer.start();

            playButton.setImageResource(R.drawable.ic_pause); // Change icon to pause
            seekBar.setMax(chatPlayer.getDuration());

            // When playback finishes, reset everything
            chatPlayer.setOnCompletionListener(mp -> {
                playButton.setImageResource(R.drawable.ic_play_arrow);
                seekBar.setProgress(0);
                chatPlayer.release();
                chatPlayer = null;
                currentlyPlayingFilePath = null;
            });

            // Create a new Runnable to update the SeekBar progress
            timerHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Check if this is still the currently playing item
                    if (chatPlayer != null && chatPlayer.isPlaying() && seekBar.equals(currentlyPlayingSeekBar)) {
                        seekBar.setProgress(chatPlayer.getCurrentPosition());
                        timerHandler.postDelayed(this, 500); // Update every half second
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void resetInputUI() {
        textInputLayout.setVisibility(View.VISIBLE);
        voiceRecordingLayout.setVisibility(View.GONE);
        reviewVoiceLayout.setVisibility(View.GONE);
        btnStopRecording.setVisibility(View.GONE);
        ivRecordingDot.setVisibility(View.VISIBLE);
        tvSlideToCancel.setVisibility(View.VISIBLE);
        tvSlideToCancel.setText("< Slide to cancel");
        tvRecordingTime.setText("0:00");
        isRecording = false;
        isLocked = false;
        timerHandler.removeCallbacks(updateTimer);
        timerHandler.removeCallbacks(updateReviewSeekBar);
        if (reviewPlayer != null) { reviewPlayer.release(); reviewPlayer = null; }
    }

    private final Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds %= 60;
                tvRecordingTime.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };
    private final Runnable updateReviewSeekBar = new Runnable() {
        @Override
        public void run() {
            if (reviewPlayer != null && reviewPlayer.isPlaying()) {
                voiceSeekBar.setProgress(reviewPlayer.getCurrentPosition());
                timerHandler.postDelayed(this, 500);
            }
        }
    };

    private void sendAudioToBackend(File file) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/3gpp"), file);
        MultipartBody.Part audioBody = MultipartBody.Part.createFormData("audio", file.getName(), requestFile);
        RequestBody languageBody = RequestBody.create(MediaType.parse("text/plain"), selectedLanguageCode);

        api.sendAudio(audioBody, languageBody).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String text = response.body().get("text");
                    chatMessages.get(chatMessages.size() - 1).setMessage(text); // Just set the text, no emoji here
                    chatAdapter.notifyItemChanged(chatMessages.size() - 1);
                    sendMessageToBot(text);
                } else { addBotMessage("⚠️ Failed to transcribe."); }
            }
            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                addBotMessage("❌ Transcription Error.");
            }
        });
    }

    private void addUserMessage(String message, String audioPath) {
        ChatMessage chatMessage;
        if (audioPath != null) {
            chatMessage = new ChatMessage(message, audioPath, ChatMessage.TYPE_USER_VOICE);
        } else {
            chatMessage = new ChatMessage(message, ChatMessage.TYPE_USER_TEXT);
        }
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    // Original addUserMessage now calls the new one for compatibility
    private void addUserMessage(String message) {
        addUserMessage(message, null);
    }

    private void addBotMessage(String message) {
        chatMessages.add(new ChatMessage(message, ChatMessage.TYPE_BOT));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    private void sendMessageToBot(String userMessage) {
        Map<String, String> body = new HashMap<>();
        body.put("message", userMessage);
        api.sendMessage(body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String reply = response.body().get("reply");
                    addBotMessage(reply);
                    speakText(reply);
                } else {
                    addBotMessage("⚠️ Failed to get response from server.");
                }
            }
            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                addBotMessage("❌ Error: " + t.getMessage());
            }
        });
    }

    private void setupMenuListener() {
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

    private void loadUserNameAndCheckProfile() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            tvWelcome.setText("👋 Welcome!");
            addBotMessage("👋 Welcome! I am your PGRKAM Assistant.\nAsk me about jobs, skills, or foreign counseling.");
            showProfileCompletionDialog();
            return;
        }
        db.collection("users").document(uid)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) { return; }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        tvWelcome.setText("👋 Welcome, " + (name != null && !name.isEmpty() ? name : "") + "!");
                        checkProfileCompletion(documentSnapshot);
                    } else {
                        tvWelcome.setText("👋 Welcome!");
                        showProfileCompletionDialog();
                    }
                });
    }

    private void checkProfileCompletion(DocumentSnapshot document) {
        if (document != null && document.exists()) {
            if (document.getString("skills") == null || document.getString("skills").trim().isEmpty()) {
                showProfileCompletionDialog();
            }
        } else {
            showProfileCompletionDialog();
        }
    }

    private void showProfileCompletionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Complete Your Profile")
                .setMessage("To get personalized job and skill recommendations, please complete your profile.")
                .setPositiveButton("Go to Profile", (dialog, which) -> loadFragment(new ProfileFragment()))
                .setNegativeButton("Maybe Later", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void loadFragment(Fragment fragment) {
        FrameLayout container = findViewById(R.id.fragmentContainer);
        if (container != null) {
            container.setVisibility(View.VISIBLE);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragmentContainer, fragment);
            ft.addToBackStack(null);
            ft.commit();
        }
    }

    public void speakText(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addBotMessage("Audio permission granted.");
            } else {
                addBotMessage("⚠️ Permission Denied. Cannot record audio.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (reviewPlayer != null) reviewPlayer.release();
        if (chatPlayer != null) chatPlayer.release();
        super.onDestroy();
    }
}