package com.schoolforum.app.ui.follow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.schoolforum.app.R;
import com.schoolforum.app.model.User;
import com.schoolforum.app.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 关注列表适配器
 */
public class FollowListAdapter extends RecyclerView.Adapter<FollowListAdapter.ViewHolder> {

    private List<User> users = new ArrayList<>();
    private OnUserActionListener listener;

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnUserActionListener(OnUserActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_follow_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivAvatar;
        private final TextView tvUsername;
        private final TextView tvMeta;
        private final Button btnFollow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnFollow = itemView.findViewById(R.id.btnFollow);
        }

        void bind(User user, OnUserActionListener listener) {
            // 头像
            String avatarUrl = user.getAvatar();
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

            // 用户名
            tvUsername.setText(user.getUsername() != null ? user.getUsername() : "用户");

            // 元信息
            StringBuilder meta = new StringBuilder();
            if (user.getGrade() != null) {
                meta.append(user.getGrade());
            }
            if (user.getClassName() != null) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append(user.getClassName());
            }
            tvMeta.setText(meta.length() > 0 ? meta.toString() : "");

            // 关注按钮
            boolean isFollowing = Boolean.TRUE.equals(user.getIsFollowing());
            updateFollowButton(btnFollow, isFollowing);

            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });

            btnFollow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFollowClick(user, getBindingAdapterPosition());
                }
            });
        }

        private void updateFollowButton(Button btn, boolean isFollowing) {
            if (isFollowing) {
                btn.setText("已关注");
                btn.setBackgroundResource(R.drawable.btn_outline);
                btn.setTextColor(btn.getContext().getColor(R.color.text_secondary));
            } else {
                btn.setText("关注");
                btn.setBackgroundResource(R.drawable.btn_primary);
                btn.setTextColor(btn.getContext().getColor(R.color.white));
            }
        }
    }

    public interface OnUserActionListener {
        void onUserClick(User user);
        void onFollowClick(User user, int position);
    }
}
