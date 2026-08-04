package com.schoolforum.app.ui.post;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Post.Comment;
import com.schoolforum.app.model.Post.Reply;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 评论列表适配器（支持多层嵌套回复）
 */
@SuppressWarnings("unused")
public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments = new ArrayList<>();
    private OnCommentActionListener listener;
    private String currentUserId; // 当前登录用户ID
    private String postAuthorId; // 帖子作者ID

    public void setComments(List<Comment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnCommentActionListener(OnCommentActionListener listener) {
        this.listener = listener;
    }
    
    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }
    
    public void setPostAuthorId(String authorId) {
        this.postAuthorId = authorId;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment, listener, currentUserId, postAuthorId);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivAvatar;
        private final TextView tvUsername;
        private final TextView tvTime;
        private final TextView tvContent;
        private final TextView btnReply;
        private final LinearLayout btnLike;
        private final ImageView ivLike;
        private final TextView tvLikes;
        private final LinearLayout layoutReplies;
        private final RecyclerView rvReplies;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnLike = itemView.findViewById(R.id.btnLike);
            ivLike = itemView.findViewById(R.id.ivLike);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            layoutReplies = itemView.findViewById(R.id.layoutReplies);
            rvReplies = itemView.findViewById(R.id.rvReplies);
        }

        void bind(Comment comment, OnCommentActionListener listener, 
                  String currentUserId, String postAuthorId) {
            // 设置用户信息
            if (Boolean.TRUE.equals(comment.getAnonymous())) {
                ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
                tvUsername.setText("匿名用户");
            } else {
                String avatarUrl = comment.getUserAvatar();
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
                tvUsername.setText(comment.getUsername() != null ? comment.getUsername() : "用户");
            }

            // 设置时间和内容
            tvTime.setText(comment.getTimestamp() != null 
                    ? TimeUtils.formatRelativeTime(comment.getTimestamp()) : "");
            tvContent.setText(comment.getContent() != null ? comment.getContent() : "");

            // 设置评论点赞状态（服务端 likeComment 返回 likes/likedBy，likedBy 为空时视为未点赞）
            boolean liked = comment.isLikedByUser(currentUserId);
            if (tvLikes != null) {
                int likeCount = comment.getLikes() != null ? comment.getLikes() : 0;
                if (likeCount > 0) {
                    tvLikes.setVisibility(View.VISIBLE);
                    tvLikes.setText(String.valueOf(likeCount));
                } else {
                    tvLikes.setVisibility(View.GONE);
                }
            }
            if (ivLike != null) {
                ivLike.setImageResource(liked ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);
                ivLike.setColorFilter(liked
                        ? itemView.getContext().getColor(R.color.primary)
                        : itemView.getContext().getColor(R.color.text_hint));
            }
            if (btnLike != null) {
                btnLike.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCommentLike(comment, getBindingAdapterPosition());
                    }
                });
            }

            // 设置回复列表（支持嵌套）
            List<Reply> replies = comment.getReplies();
            if (replies != null && !replies.isEmpty()) {
                layoutReplies.setVisibility(View.VISIBLE);
                rvReplies.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                NestedRepliesAdapter repliesAdapter = new NestedRepliesAdapter(1, comment.getId());
                repliesAdapter.setReplies(replies);
                repliesAdapter.setCurrentUserId(currentUserId);
                repliesAdapter.setPostAuthorId(postAuthorId);
                repliesAdapter.setOnReplyActionListener(new NestedRepliesAdapter.OnReplyActionListener() {
                    @Override
                    public void onReplyClick(Reply reply, int position) {
                        if (listener != null) {
                            listener.onReplyClick(comment, reply, getBindingAdapterPosition());
                        }
                    }
                    
                    @Override
                    public void onDeleteReply(String commentId, String replyId, String nestedReplyId) {
                        if (listener != null) {
                            listener.onDeleteReply(commentId, replyId, nestedReplyId);
                        }
                    }
                    
                    @Override
                    public void onReportReply(String commentId, String replyId) {
                        if (listener != null) {
                            listener.onReportReply(commentId, replyId);
                        }
                    }

                    @Override
                    public void onUserClick(String userId) {
                        if (listener != null) {
                            listener.onUserClick(userId);
                        }
                    }
                });
                rvReplies.setAdapter(repliesAdapter);
            } else {
                layoutReplies.setVisibility(View.GONE);
            }

            // 回复按钮
            btnReply.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReplyClick(comment, null, getBindingAdapterPosition());
                }
            });

            // 长按显示删除选项
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    showCommentMenu(v, comment, listener, currentUserId);
                    return true;
                }
                return false;
            });

            // 点击头像
            ivAvatar.setOnClickListener(v -> {
                if (listener != null && !Boolean.TRUE.equals(comment.getAnonymous()) 
                        && comment.getUserId() != null) {
                    listener.onUserClick(comment.getUserId());
                }
            });
        }
        
        private void showCommentMenu(View anchor, Comment comment, OnCommentActionListener listener, String currentUserId) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            
            boolean isOwner = currentUserId != null && currentUserId.equals(comment.getUserId());
            if (isOwner) {
                popup.getMenu().add(0, 1, 0, "删除评论");
            } else {
                popup.getMenu().add(0, 2, 0, "举报");
            }
            
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    listener.onDeleteComment(comment.getId());
                    return true;
                } else if (item.getItemId() == 2) {
                    listener.onReportComment(comment.getId());
                    return true;
                }
                return false;
            });
            
            popup.show();
        }
    }

    /**
     * 嵌套回复适配器（支持多层递归）
     */
    static class NestedRepliesAdapter extends RecyclerView.Adapter<NestedRepliesAdapter.ReplyViewHolder> {
        private List<Reply> replies = new ArrayList<>();
        private OnReplyActionListener listener;
        private int depth; // 当前嵌套层级
        private String commentId; // 所属评论ID
        private String currentUserId;
        private String postAuthorId;

        NestedRepliesAdapter(int depth, String commentId) {
            this.depth = depth;
            this.commentId = commentId;
        }

        void setReplies(List<Reply> replies) {
            this.replies = replies != null ? replies : new ArrayList<>();
            notifyDataSetChanged();
        }

        void setOnReplyActionListener(OnReplyActionListener listener) {
            this.listener = listener;
        }
        
        void setCurrentUserId(String userId) {
            this.currentUserId = userId;
        }
        
        void setPostAuthorId(String authorId) {
            this.postAuthorId = authorId;
        }

        @NonNull
        @Override
        public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reply, parent, false);
            return new ReplyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
            holder.bind(replies.get(position), listener, position, depth, commentId, currentUserId, postAuthorId);
        }

        @Override
        public int getItemCount() {
            return replies.size();
        }

        /**
         * wrap_content 嵌套 RecyclerView 高度修复：
         * 外层 RecyclerView 无法测量内层 wrap_content RecyclerView 的高度，
         * 导致回复列表高度为 0 不显示。此处逐项测量累加得到真实高度。
         */
        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (replies.isEmpty()) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            try {
                int totalHeight = 0;
                int widthSize = android.view.View.MeasureSpec.getSize(widthMeasureSpec);
                for (int i = 0; i < replies.size(); i++) {
                    ReplyViewHolder holder = (ReplyViewHolder) onCreateViewHolder(
                            new android.widget.FrameLayout(getContext()), 0);
                    holder.bind(replies.get(i), null, i, depth, commentId, currentUserId, postAuthorId);
                    holder.itemView.measure(
                            android.view.View.MeasureSpec.makeMeasureSpec(widthSize, android.view.View.MeasureSpec.AT_MOST),
                            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED));
                    totalHeight += holder.itemView.getMeasuredHeight();
                }
                setMeasuredDimension(widthSize, Math.max(1, totalHeight));
            } catch (Exception e) {
                // 测量失败时回退默认行为
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        static class ReplyViewHolder extends RecyclerView.ViewHolder {
            private final CircleImageView ivAvatar;
            private final TextView tvUsername;
            private final TextView tvContent;
            private final TextView tvTime;
            private final TextView btnReply;
            private final TextView tvReplyTo;
            private final TextView tvReplyToUsername;
            private final LinearLayout layoutNestedReplies;
            private final RecyclerView rvNestedReplies;

            ReplyViewHolder(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvUsername = itemView.findViewById(R.id.tvUsername);
                tvContent = itemView.findViewById(R.id.tvContent);
                tvTime = itemView.findViewById(R.id.tvTime);
                btnReply = itemView.findViewById(R.id.btnReply);
                tvReplyTo = itemView.findViewById(R.id.tvReplyTo);
                tvReplyToUsername = itemView.findViewById(R.id.tvReplyToUsername);
                layoutNestedReplies = itemView.findViewById(R.id.layoutNestedReplies);
                rvNestedReplies = itemView.findViewById(R.id.rvNestedReplies);
            }

            void bind(Reply reply, OnReplyActionListener listener, int position, int depth,
                      String commentId, String currentUserId, String postAuthorId) {
                if (Boolean.TRUE.equals(reply.getAnonymous())) {
                    ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
                    tvUsername.setText("匿名用户");
                } else {
                    String avatarUrl = reply.getUserAvatar();
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
                    tvUsername.setText(reply.getUsername() != null ? reply.getUsername() : "用户");
                }

                tvTime.setText(reply.getTimestamp() != null 
                        ? TimeUtils.formatRelativeTime(reply.getTimestamp()) : "");
                tvContent.setText(reply.getContent() != null ? reply.getContent() : "");

                // 显示回复给谁
                if (reply.getReplyToUsername() != null && !reply.getReplyToUsername().isEmpty()) {
                    tvReplyTo.setVisibility(View.VISIBLE);
                    tvReplyToUsername.setVisibility(View.VISIBLE);
                    tvReplyToUsername.setText(reply.getReplyToUsername());
                } else {
                    tvReplyTo.setVisibility(View.GONE);
                    tvReplyToUsername.setVisibility(View.GONE);
                }

                // 显示嵌套回复列表（递归）
                List<Reply> nestedReplies = reply.getReplies();
                if (nestedReplies != null && !nestedReplies.isEmpty()) {
                    layoutNestedReplies.setVisibility(View.VISIBLE);
                    rvNestedReplies.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                    
                    // 创建下一层级的适配器（depth + 1）
                    NestedRepliesAdapter nestedAdapter = new NestedRepliesAdapter(depth + 1, commentId);
                    nestedAdapter.setReplies(nestedReplies);
                    nestedAdapter.setCurrentUserId(currentUserId);
                    nestedAdapter.setPostAuthorId(postAuthorId);
                    nestedAdapter.setOnReplyActionListener(new OnReplyActionListener() {
                        @Override
                        public void onReplyClick(Reply nestedReply, int pos) {
                            if (listener != null) {
                                listener.onReplyClick(nestedReply, pos);
                            }
                        }
                        
                        @Override
                        public void onDeleteReply(String cId, String replyId, String nestedReplyId) {
                            if (listener != null) {
                                listener.onDeleteReply(cId, replyId, nestedReplyId);
                            }
                        }
                        
                        @Override
                        public void onReportReply(String cId, String replyId) {
                            if (listener != null) {
                                listener.onReportReply(cId, replyId);
                            }
                        }

                        @Override
                        public void onUserClick(String userId) {
                            if (listener != null) {
                                listener.onUserClick(userId);
                            }
                        }
                    });
                    rvNestedReplies.setAdapter(nestedAdapter);
                } else {
                    layoutNestedReplies.setVisibility(View.GONE);
                }

                btnReply.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onReplyClick(reply, position);
                    }
                });

                // 长按显示删除或举报选项
                itemView.setOnLongClickListener(v -> {
                    if (listener != null) {
                        showReplyMenu(v, reply, commentId, listener, currentUserId);
                        return true;
                    }
                    return false;
                });

                // 点击头像跳转用户主页
                ivAvatar.setOnClickListener(v -> {
                    if (listener != null && reply.getUserId() != null) {
                        listener.onUserClick(reply.getUserId());
                    }
                });
            }
            
            private void showReplyMenu(View anchor, Reply reply, String commentId, 
                                       OnReplyActionListener listener, String currentUserId) {
                PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
                
                boolean isOwner = currentUserId != null && currentUserId.equals(reply.getUserId());
                if (isOwner) {
                    popup.getMenu().add(0, 1, 0, "删除回复");
                } else {
                    popup.getMenu().add(0, 2, 0, "举报");
                }
                
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        // 判断是普通回复还是嵌套回复
                        // 如果有 replyTo，说明是嵌套回复
                        String replyToId = reply.getReplyTo();
                        if (replyToId != null && !replyToId.isEmpty()) {
                            // 嵌套回复：需要传递 replyId（父回复）和 nestedReplyId（当前回复）
                            listener.onDeleteReply(commentId, replyToId, reply.getId());
                        } else {
                            // 普通回复：只传递 replyId
                            listener.onDeleteReply(commentId, reply.getId(), null);
                        }
                        return true;
                    } else if (item.getItemId() == 2) {
                        listener.onReportReply(commentId, reply.getId());
                        return true;
                    }
                    return false;
                });
                
                popup.show();
            }
        }

        interface OnReplyActionListener {
            void onReplyClick(Reply reply, int position);
            void onDeleteReply(String commentId, String replyId, String nestedReplyId);
            void onReportReply(String commentId, String replyId);
            void onUserClick(String userId);
        }
    }

    /**
     * 评论操作监听器
     */
    public interface OnCommentActionListener {
        void onReplyClick(Comment comment, Reply reply, int commentPosition);
        void onUserClick(String userId);
        void onDeleteComment(String commentId);
        void onDeleteReply(String commentId, String replyId, String nestedReplyId);
        void onReportComment(String commentId);
        void onReportReply(String commentId, String replyId);
        void onCommentLike(Comment comment, int position);
    }
}
