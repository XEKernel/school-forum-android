package com.schoolforum.app.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Conversation;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.TimeUtils;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 会话列表适配器
 */
public class ConversationsAdapter extends ListAdapter<Conversation, ConversationsAdapter.ConversationViewHolder> {

    private OnConversationClickListener listener;

    public ConversationsAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = getItem(position);
        holder.bind(conversation, listener);
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivAvatar;
        private final TextView tvUnread;
        private final TextView tvUsername;
        private final TextView tvLastMessage;
        private final TextView tvTime;
        private OnConversationClickListener listener;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUnread = itemView.findViewById(R.id.tvUnread);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(Conversation conversation, OnConversationClickListener clickListener) {
            this.listener = clickListener;
            Conversation.OtherUser otherUser = conversation.getOtherUser();
            
            // 头像
            if (otherUser != null && otherUser.getAvatar() != null) {
                String avatarUrl = otherUser.getAvatar().startsWith("http") 
                        ? otherUser.getAvatar() 
                        : ApiClient.getBaseUrl() + otherUser.getAvatar();
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }

            // 用户名
            tvUsername.setText(otherUser != null ? otherUser.getUsername() : "未知用户");

            // 未读数
            int unreadCount = conversation.getUnreadCount() != null ? conversation.getUnreadCount() : 0;
            if (unreadCount > 0) {
                tvUnread.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                tvUnread.setVisibility(View.VISIBLE);
            } else {
                tvUnread.setVisibility(View.GONE);
            }

            // 最后一条消息
            Conversation.LastMessage lastMessage = conversation.getLastMessage();
            if (lastMessage != null) {
                tvLastMessage.setText(lastMessage.getContent());
                tvTime.setText(TimeUtils.formatRelativeTime(lastMessage.getCreatedAt()));
            } else {
                tvLastMessage.setText("");
                tvTime.setText("");
            }

            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null && otherUser != null) {
                    listener.onConversationClick(conversation, otherUser);
                }
            });
        }
    }

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation, Conversation.OtherUser otherUser);
    }

    private static final DiffUtil.ItemCallback<Conversation> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<Conversation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
    };
}
