package com.schoolforum.app.ui.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Notification;
import com.schoolforum.app.network.ApiClient;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 通知列表适配器
 */
@SuppressWarnings("unused")
public class NotificationsAdapter extends ListAdapter<Notification, NotificationsAdapter.NotificationViewHolder> {

    private OnNotificationClickListener listener;

    public NotificationsAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = getItem(position);
        holder.bind(notification, listener);
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final View unreadIndicator;
        private final CircleImageView ivAvatar;
        private final ImageView ivTypeIcon;
        private final TextView tvUsername;
        private final TextView tvContent;
        private final TextView tvPostTitle;
        private final TextView tvTime;
        private OnNotificationClickListener listener;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivTypeIcon = itemView.findViewById(R.id.ivTypeIcon);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(Notification notification, OnNotificationClickListener clickListener) {
            this.listener = clickListener;
            // 未读指示
            unreadIndicator.setVisibility(Boolean.TRUE.equals(notification.getRead()) 
                    ? View.INVISIBLE : View.VISIBLE);

            // 用户头像
            String avatarUrl = notification.getFromUserAvatar();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl 
                        : ApiClient.getBaseUrl() + avatarUrl;
                Glide.with(itemView.getContext())
                        .load(fullUrl)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }

            // 类型图标
            int iconRes = getIconResource(notification.getType());
            ivTypeIcon.setImageResource(iconRes);

            // 用户名
            tvUsername.setText(notification.getFromUsername());

            // 通知内容
            tvContent.setText(notification.getDisplayText());

            // 帖子标题
            if (notification.getPostTitle() != null && !notification.getPostTitle().isEmpty()) {
                tvPostTitle.setText(notification.getPostTitle());
                tvPostTitle.setVisibility(View.VISIBLE);
            } else {
                tvPostTitle.setVisibility(View.GONE);
            }

            // 时间
            tvTime.setText(com.schoolforum.app.utils.TimeUtils.formatRelativeTime(notification.getTimestamp()));

            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }

        private int getIconResource(String type) {
            if (type == null) return android.R.drawable.ic_menu_info_details;
            
            switch (type) {
                case "like":
                    return android.R.drawable.ic_menu_agenda;
                case "comment":
                case "comment_reply":
                    return android.R.drawable.ic_menu_send;
                case "follow":
                    return android.R.drawable.ic_menu_add;
                default:
                    return android.R.drawable.ic_menu_info_details;
            }
        }
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private static final DiffUtil.ItemCallback<Notification> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<Notification>() {
        @Override
        public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.getId().equals(newItem.getId()) 
                    && Objects.equals(oldItem.getRead(), newItem.getRead());
        }
    };
}
