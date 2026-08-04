package com.schoolforum.app.ui.chat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Message;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.TimeUtils;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 消息列表适配器
 */
@SuppressWarnings("unused")
public class MessagesAdapter extends ListAdapter<Message, MessagesAdapter.MessageViewHolder> {

    private String currentUserId;

    public MessagesAdapter(String currentUserId) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = getItem(position);
        boolean isSelf = message.getSenderId().equals(currentUserId);
        
        // 时间戳显示
        boolean showTime = shouldShowTime(message.getCreatedAt(), position);
        holder.bind(message, isSelf, showTime);
    }

    private boolean shouldShowTime(String timestamp, int position) {
        if (position == 0) return true;
        
        Message prevMessage = getItem(position - 1);
        if (prevMessage == null) return true;
        
        // 间隔超过5分钟显示时间
        try {
            long currentTime = TimeUtils.parseIsoTime(timestamp).getTime();
            long prevTime = TimeUtils.parseIsoTime(prevMessage.getCreatedAt()).getTime();
            return (currentTime - prevTime) > 5 * 60 * 1000;
        } catch (Exception e) {
            return false;
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTime;
        private final LinearLayout messageContainer;
        private final CircleImageView ivAvatar;
        private final TextView tvMessage;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        void bind(Message message, boolean isSelf, boolean showTime) {
            // 时间戳
            if (showTime) {
                tvTime.setText(TimeUtils.formatRelativeTime(message.getCreatedAt()));
                tvTime.setVisibility(View.VISIBLE);
            } else {
                tvTime.setVisibility(View.GONE);
            }

            // 消息布局
            if (isSelf) {
                // 自己的消息 - 右对齐
                messageContainer.setGravity(Gravity.END);
                ivAvatar.setVisibility(View.GONE);
                tvMessage.setBackgroundResource(R.drawable.chat_bubble_self);
                tvMessage.setTextColor(itemView.getContext().getColor(android.R.color.white));
            } else {
                // 对方的消息 - 左对齐
                messageContainer.setGravity(Gravity.START);
                ivAvatar.setVisibility(View.VISIBLE);
                tvMessage.setBackgroundResource(R.drawable.chat_bubble_other);
                tvMessage.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            }

            // 头像
            if (!isSelf && message.getSenderAvatar() != null) {
                String avatarUrl = message.getSenderAvatar().startsWith("http") 
                        ? message.getSenderAvatar() 
                        : ApiClient.getBaseUrl() + message.getSenderAvatar();
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(ivAvatar);
            }

            // 消息内容
            tvMessage.setText(message.getContent());
        }
    }

    private static final DiffUtil.ItemCallback<Message> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<Message>() {
        @Override
        public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            return oldItem.getId().equals(newItem.getId()) 
                    && Objects.equals(oldItem.getRead(), newItem.getRead());
        }
    };
}
