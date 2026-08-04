package com.schoolforum.app.ui.post;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Post;
import com.schoolforum.app.model.Post.Image;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.MarkdownUtils;
import com.schoolforum.app.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 帖子列表适配器
 */
@SuppressWarnings("unused")
public class PostsAdapter extends ListAdapter<Post, PostsAdapter.PostViewHolder> {

    private OnPostClickListener listener;

    public PostsAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = getItem(position);
        holder.bind(post, listener);
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivAvatar;
        private final TextView tvUsername;
        private final TextView tvAnonymous;
        private final TextView tvMeta;
        private final TextView tvContent;
        private final RecyclerView rvImages;
        private final View btnLike;
        private final ImageView ivLike;
        private final TextView tvLikes;
        private final TextView tvComments;
        private final TextView tvViews;
        private final View btnMore;
        private OnPostClickListener listener;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvAnonymous = itemView.findViewById(R.id.tvAnonymous);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvContent = itemView.findViewById(R.id.tvContent);
            rvImages = itemView.findViewById(R.id.rvImages);
            btnLike = itemView.findViewById(R.id.btnLike);
            ivLike = itemView.findViewById(R.id.ivLike);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvComments = itemView.findViewById(R.id.tvComments);
            tvViews = itemView.findViewById(R.id.tvViews);
            btnMore = itemView.findViewById(R.id.btnMore);
        }

        void bind(Post post, OnPostClickListener clickListener) {
            this.listener = clickListener;
            // 设置用户信息
            if (Boolean.TRUE.equals(post.getAnonymous())) {
                ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
                tvUsername.setText("匿名用户");
                tvAnonymous.setVisibility(View.GONE);
            } else {
                // 加载头像
                String avatarUrl = post.getUserAvatar();
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
                tvUsername.setText(post.getUsername());
                tvAnonymous.setVisibility(View.GONE);
                
                // 点击头像进入原生用户主页
                ivAvatar.setOnClickListener(v -> {
                    if (post.getUserId() != null) {
                        android.content.Intent intent = new android.content.Intent(itemView.getContext(), com.schoolforum.app.ui.profile.ProfileActivity.class);
                        intent.putExtra("userId", post.getUserId());
                        itemView.getContext().startActivity(intent);
                    }
                });
            }

            // 设置元信息
            String meta = "";
            if (post.getGrade() != null) {
                meta += post.getGrade();
            }
            if (post.getClassName() != null) {
                meta += post.getClassName();
            }
            if (post.getTimestamp() != null) {
                if (!meta.isEmpty()) meta += " · ";
                meta += TimeUtils.formatRelativeTime(post.getTimestamp());
            }
            // 添加设备信息
            if (post.getDeviceInfo() != null && !post.getDeviceInfo().isEmpty()) {
                meta += " · " + post.getDeviceInfo();
            }
            tvMeta.setText(meta);

            // 设置内容 - 使用纯文本预览
            String plainContent = MarkdownUtils.toPlainText(post.getContent());
            tvContent.setText(plainContent);

            // 设置图片
            List<Image> postImages = post.getImages();
            if (postImages != null && !postImages.isEmpty()) {
                List<String> imageUrls = new ArrayList<>();
                for (Image img : postImages) {
                    if (img != null && img.getUrl() != null) {
                        imageUrls.add(img.getUrl());
                    }
                }
                
                if (!imageUrls.isEmpty()) {
                    rvImages.setVisibility(View.VISIBLE);
                    // 根据图片数量设置列数
                    int spanCount = imageUrls.size() == 1 ? 1 : (imageUrls.size() <= 4 ? 2 : 3);
                    rvImages.setLayoutManager(new GridLayoutManager(itemView.getContext(), spanCount));
                    PostImagesAdapter imagesAdapter = new PostImagesAdapter(itemView.getContext(), imageUrls);
                    rvImages.setAdapter(imagesAdapter);
                } else {
                    rvImages.setVisibility(View.GONE);
                }
            } else {
                rvImages.setVisibility(View.GONE);
            }

            // 设置统计数据
            tvLikes.setText(String.valueOf(post.getLikes() != null ? post.getLikes() : 0));
            tvComments.setText(String.valueOf(post.getComments() != null ? post.getComments().size() : 0));
            tvViews.setText(String.valueOf(post.getViewCount() != null ? post.getViewCount() : 0));

            // 设置点赞状态
            if (Boolean.TRUE.equals(post.getLiked())) {
                ivLike.setColorFilter(itemView.getContext().getColor(R.color.error));
                tvLikes.setTextColor(itemView.getContext().getColor(R.color.error));
            } else {
                ivLike.setColorFilter(itemView.getContext().getColor(R.color.text_secondary));
                tvLikes.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            }

            // 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostClick(post);
                }
            });

            btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(post, getBindingAdapterPosition());
                }
            });

            btnMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMoreClick(post, v);
                }
            });
        }
    }

    /**
     * 帖子点击监听器
     */
    public interface OnPostClickListener {
        void onPostClick(Post post);
        void onLikeClick(Post post, int position);
        void onMoreClick(Post post, View anchor);
    }

    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return oldItem.getId().equals(newItem.getId()) 
                    && java.util.Objects.equals(oldItem.getLikes(), newItem.getLikes())
                    && java.util.Objects.equals(oldItem.getLiked(), newItem.getLiked());
        }
    };
}