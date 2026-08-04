package com.schoolforum.app.ui.blacklist;

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
 * 黑名单列表适配器
 */
public class BlacklistAdapter extends RecyclerView.Adapter<BlacklistAdapter.ViewHolder> {

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
                .inflate(R.layout.item_blacklist_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, listener, position);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivAvatar;
        private final TextView tvUsername;
        private final TextView tvMeta;
        private final Button btnUnblock;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnUnblock = itemView.findViewById(R.id.btnUnblock);
        }

        void bind(User user, OnUserActionListener listener, int position) {
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

            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });

            btnUnblock.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnblockClick(user, position);
                }
            });
        }
    }

    public interface OnUserActionListener {
        void onUserClick(User user);
        void onUnblockClick(User user, int position);
    }
}
