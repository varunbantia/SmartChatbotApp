package com.example.smartchatbot.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartchatbot.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatHistoryAdapter extends FirestoreRecyclerAdapter<ChatSession, ChatHistoryAdapter.ChatSessionViewHolder> {

    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String chatId);
    }

    public ChatHistoryAdapter(@NonNull FirestoreRecyclerOptions<ChatSession> options, OnItemClickListener listener) {
        super(options);
        this.listener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatSessionViewHolder holder, int position, @NonNull ChatSession model) {
        holder.bind(model);
    }

    @NonNull
    @Override
    public ChatSessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_history, parent, false);
        return new ChatSessionViewHolder(view);
    }

    class ChatSessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvLastMessage;
        TextView tvTimestamp;

        public ChatSessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        public void bind(ChatSession session) {
            tvLastMessage.setText(session.getLastMessage());
            if (session.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                tvTimestamp.setText(sdf.format(session.getTimestamp()));
            }

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position).getChatId());
                }
            });
        }
    }
}