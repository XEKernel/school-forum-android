package com.schoolforum.app.ui.follow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.schoolforum.app.MainActivity;
import com.schoolforum.app.R;
import com.schoolforum.app.model.User;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 关注用户列表适配器
 */
public class FollowingUserAdapter extends RecyclerView.Adapter<FollowingUserAdapter.ViewHolder> {

    private List<User> users = new ArrayList<>();
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onUserClick(User user);
        void onUnfollowClick(User user, int position);
    }

    public void setOnUserActionListener(OnUserActionListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_following_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, position);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView ivAvatar;
        private TextView tvUsername;
        private TextView tvMeta;
        private MaterialButton btnUnfollow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnUnfollow = itemView.findViewById(R.id.btnUnfollow);
        }

        public void bind(User user, int position) {
            // 头像
            String avatarUrl = user.getAvatar();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : MainActivity.getBaseUrl() + avatarUrl;
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

            // 年级班级
            StringBuilder meta = new StringBuilder();
            if (user.getGrade() != null && !user.getGrade().isEmpty()) {
                meta.append(user.getGrade());
            }
            if (user.getClassName() != null && !user.getClassName().isEmpty()) {
                if (meta.length() > 0) meta.append(" ");
                meta.append(user.getClassName());
            }
            if (user.getSchool() != null && !user.getSchool().isEmpty()) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append(user.getSchool());
            }
            tvMeta.setText(meta.length() > 0 ? meta.toString() : "");

            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });

            ivAvatar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });

            btnUnfollow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnfollowClick(user, position);
                }
            });
        }
    }
}
