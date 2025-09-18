package com.example.smartchatbot.chatbot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartchatbot.R;
import com.example.smartchatbot.chatbot.ChatMessage;
import com.example.smartchatbot.dashboard.ChatbotActivity;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> chatMessages;
    private Context context;

    public ChatAdapter(List<ChatMessage> chatMessages, Context context) {
        this.chatMessages = chatMessages;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return chatMessages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);

        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).tvUserMessage.setText(message.getMessage());
        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).tvBotMessage.setText(message.getMessage());

            ((BotViewHolder) holder).btnSpeak.setOnClickListener(v -> {
                if (context instanceof ChatbotActivity) {
                    ((ChatbotActivity) context).speakText(message.getMessage());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMessage;

        UserViewHolder(View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvBotMessage;
        ImageButton btnSpeak;

        BotViewHolder(View itemView) {
            super(itemView);
            tvBotMessage = itemView.findViewById(R.id.tvBotMessage);
            btnSpeak = itemView.findViewById(R.id.btnSpeak);
        }
    }
}
