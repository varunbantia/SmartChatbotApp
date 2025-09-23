package com.example.smartchatbot.chatbot;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartchatbot.R;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final OnPlayButtonClickListener playButtonClickListener;

    public interface OnPlayButtonClickListener {
        void onPlayButtonClick(String filePath, ImageButton playButton, SeekBar seekBar, TextView duration);
        void onSpeakerIconClick(String textToSpeak);
    }

    public ChatAdapter(List<ChatMessage> chatMessages, Context context) {
        this.chatMessages = chatMessages;
        this.playButtonClickListener = (OnPlayButtonClickListener) context;
    }

    // ✅ ADD THIS HELPER METHOD FOR COPYING TEXT
    private void copyTextToClipboard(String text, Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return chatMessages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ChatMessage.TYPE_USER_TEXT:
                return new UserTextViewHolder(inflater.inflate(R.layout.item_chat_user_text, parent, false));
            case ChatMessage.TYPE_USER_VOICE:
                return new UserVoiceViewHolder(inflater.inflate(R.layout.item_chat_user_voice, parent, false));
            case ChatMessage.TYPE_BOT:
            default:
                return new BotViewHolder(inflater.inflate(R.layout.chat_item_bot, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        switch (holder.getItemViewType()) {
            case ChatMessage.TYPE_USER_TEXT:
                ((UserTextViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_USER_VOICE:
                ((UserVoiceViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_BOT:
                ((BotViewHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvBotMessage;
        ImageButton btnSpeak;

        BotViewHolder(View itemView) {
            super(itemView);
            tvBotMessage = itemView.findViewById(R.id.tvBotMessage);
            btnSpeak = itemView.findViewById(R.id.btnSpeak);

            // ✅ ADD LONG-CLICK LISTENER FOR BOT MESSAGES
            tvBotMessage.setOnLongClickListener(v -> {
                copyTextToClipboard(tvBotMessage.getText().toString(), v.getContext());
                return true; // Return true to indicate the click was handled
            });
        }

        void bind(ChatMessage message) {
            tvBotMessage.setText(message.getMessage());
            btnSpeak.setOnClickListener(v -> {
                if (playButtonClickListener != null) {
                    playButtonClickListener.onSpeakerIconClick(message.getMessage());
                }
            });
        }
    }

    class UserTextViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMessage;
        UserTextViewHolder(View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);

            // ✅ ADD LONG-CLICK LISTENER FOR USER MESSAGES
            tvUserMessage.setOnLongClickListener(v -> {
                copyTextToClipboard(tvUserMessage.getText().toString(), v.getContext());
                return true; // Return true to indicate the click was handled
            });
        }
        void bind(ChatMessage message) {
            tvUserMessage.setText(message.getMessage());
        }
    }

    class UserVoiceViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnPlayPauseBubble;
        SeekBar seekBarBubble;
        TextView tvDurationBubble;

        UserVoiceViewHolder(View itemView) {
            super(itemView);
            btnPlayPauseBubble = itemView.findViewById(R.id.btnPlayPauseBubble);
            seekBarBubble = itemView.findViewById(R.id.seekBarBubble);
            tvDurationBubble = itemView.findViewById(R.id.tvDurationBubble);
        }

        void bind(ChatMessage message) {
            btnPlayPauseBubble.setImageResource(R.drawable.ic_play_arrow);
            seekBarBubble.setProgress(0);

            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(message.getAudioFilePath());
                mp.prepare();
                int duration = mp.getDuration();
                tvDurationBubble.setText(String.format(Locale.getDefault(), "%d:%02d", (duration / 1000) / 60, (duration / 1000) % 60));
            } catch (IOException e) {
                tvDurationBubble.setText("E:RR");
                e.printStackTrace();
            } finally {
                mp.release();
            }

            btnPlayPauseBubble.setOnClickListener(v -> {
                if (playButtonClickListener != null) {
                    playButtonClickListener.onPlayButtonClick(message.getAudioFilePath(), btnPlayPauseBubble, seekBarBubble, tvDurationBubble);
                }
            });
        }
    }
}