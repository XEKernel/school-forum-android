package com.schoolforum.app.ui.post;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Post;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.ui.preview.ImagePreviewActivity;
import com.schoolforum.app.utils.FileLogger;
import com.schoolforum.app.utils.MarkdownUtils;
import com.schoolforum.app.utils.TimeUtils;
import com.schoolforum.app.utils.UserManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 帖子详情Activity
 */
@SuppressWarnings("unused")
public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "post_id";

    private CircleImageView ivAvatar;
    private TextView tvUsername, tvMeta, tvContent, tvLikes, tvComments, tvViews;
    private View btnLike, btnSend, btnFavorite;
    private ImageButton btnMore;
    private ImageView ivFavorite;
    private EditText etComment;
    private ImageButton btnCommentImage;
    private ProgressBar progressBar;
    private RecyclerView rvComments, rvImages;
    private Toolbar toolbar;

    private String postId;
    private Post currentPost;
    private FileLogger logger;
    private final Gson gson = new Gson();
    private CommentsAdapter commentsAdapter;
    
    // 回复相关
    private Post.Comment replyingToComment = null;
    private Post.Reply replyingToReply = null;
    private boolean isFavorited = false;

    // 评论/回复带图（可选，最多 1 张）
    private static final int REQUEST_PICK_COMMENT_IMAGE = 2001;
    private Uri selectedCommentImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        logger = FileLogger.getInstance(this);
        
        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (TextUtils.isEmpty(postId)) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadPostDetail();
    }
    
    private void log(String message) {
        android.util.Log.d("PostDetailActivity", message);
        if (logger != null) {
            logger.d("PostDetailActivity", message);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvMeta = findViewById(R.id.tvMeta);
        tvContent = findViewById(R.id.tvContent);
        tvLikes = findViewById(R.id.tvLikes);
        tvComments = findViewById(R.id.tvComments);
        tvViews = findViewById(R.id.tvViews);
        btnLike = findViewById(R.id.btnLike);
        btnSend = findViewById(R.id.btnSend);
        etComment = findViewById(R.id.etComment);
        btnCommentImage = findViewById(R.id.btnCommentImage);
        progressBar = findViewById(R.id.progressBar);
        rvComments = findViewById(R.id.rvComments);
        rvImages = findViewById(R.id.rvImages);
        btnMore = findViewById(R.id.btnMore);
        
        // 收藏按钮
        btnFavorite = findViewById(R.id.btnFavorite);
        ivFavorite = findViewById(R.id.ivFavorite);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        
        commentsAdapter = new CommentsAdapter();
        commentsAdapter.setOnCommentActionListener(new CommentsAdapter.OnCommentActionListener() {
            @Override
            public void onReplyClick(Post.Comment comment, Post.Reply reply, int commentPosition) {
                // 设置回复状态
                replyingToComment = comment;
                replyingToReply = reply;
                
                // 更新输入框提示
                String hint;
                if (reply != null) {
                    hint = "回复 " + (reply.getUsername() != null ? reply.getUsername() : "用户");
                } else {
                    hint = "回复 " + (comment.getUsername() != null ? comment.getUsername() : "用户");
                }
                etComment.setHint(hint);
                etComment.requestFocus();
                
                // 显示软键盘
                android.view.inputmethod.InputMethodManager imm = 
                    (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }

            @Override
            public void onUserClick(String userId) {
                openUserProfile(userId);
            }
            
            @Override
            public void onDeleteComment(String commentId) {
                showDeleteCommentDialog(commentId, null, null);
            }
            
            @Override
            public void onDeleteReply(String commentId, String replyId, String nestedReplyId) {
                showDeleteCommentDialog(commentId, replyId, nestedReplyId);
            }
            
            @Override
            public void onReportComment(String commentId) {
                showReportCommentDialog(commentId);
            }
            
            @Override
            public void onReportReply(String commentId, String replyId) {
                showReportCommentDialog(replyId);
            }

            @Override
            public void onCommentLike(Post.Comment comment, int position) {
                toggleCommentLike(comment, position);
            }

            @Override
            public void onReplyLike(Post.Comment comment, Post.Reply reply) {
                toggleReplyLike(comment, reply);
            }
        });
        rvComments.setAdapter(commentsAdapter);

        btnLike.setOnClickListener(v -> toggleLike());
        btnSend.setOnClickListener(v -> sendCommentOrReply());

        // 评论带图：选择一张图片（再次点击可更换）
        if (btnCommentImage != null) {
            btnCommentImage.setOnClickListener(v -> pickCommentImage());
        }
        
        // 收藏按钮点击
        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> toggleFavorite());
        }
        
        // 更多按钮点击
        if (btnMore != null) {
            btnMore.setOnClickListener(v -> showMoreMenu());
        }
        
        // 点击输入框外部取消回复状态
        etComment.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && replyingToComment != null) {
                // 可选：失去焦点时取消回复状态
            }
        });
        
        // 长按输入框清除回复状态
        etComment.setOnLongClickListener(v -> {
            clearReplyState();
            Toast.makeText(this, "已取消回复", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        ivAvatar.setOnClickListener(v -> {
            if (currentPost != null && !Boolean.TRUE.equals(currentPost.getAnonymous()) && currentPost.getUserId() != null) {
                openUserProfile(currentPost.getUserId());
            }
        });
        
        tvUsername.setOnClickListener(v -> {
            if (currentPost != null && !Boolean.TRUE.equals(currentPost.getAnonymous()) && currentPost.getUserId() != null) {
                openUserProfile(currentPost.getUserId());
            }
        });
    }
    
    private void clearReplyState() {
        replyingToComment = null;
        replyingToReply = null;
        etComment.setHint("发表评论...");
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void openUserProfile(String userId) {
        Intent intent = new Intent(this, com.schoolforum.app.ui.profile.ProfileActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void loadPostDetail() {
        progressBar.setVisibility(View.VISIBLE);

        ApiClient.getInstance(this).get("/posts/" + postId, null,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        try {
                            PostResponse response = gson.fromJson(responseBody, PostResponse.class);
                            if (response.success && response.post != null) {
                                currentPost = response.post;
                                bindPost(currentPost);
                            } else {
                                showError(response.message != null ? response.message : "加载失败");
                            }
                        } catch (Exception e) {
                            showError("解析数据失败");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        showError(error);
                    });
                }
            }, null);
    }

    private void bindPost(Post post) {
        if (Boolean.TRUE.equals(post.getAnonymous())) {
            ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
            tvUsername.setText("匿名用户");
        } else {
            String avatarUrl = post.getUserAvatar();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl 
                        : ApiClient.getBaseUrl() + avatarUrl;
                Glide.with(this)
                        .load(fullUrl)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(ivAvatar);
            }
            tvUsername.setText(post.getUsername());
        }

        String meta = "";
        if (post.getGrade() != null) meta += post.getGrade();
        if (post.getClassName() != null) meta += post.getClassName();
        if (post.getTimestamp() != null) {
            if (!meta.isEmpty()) meta += " · ";
            meta += TimeUtils.formatRelativeTime(post.getTimestamp());
        }
        // 添加设备信息
        if (post.getDeviceInfo() != null && !post.getDeviceInfo().isEmpty()) {
            meta += " · " + post.getDeviceInfo();
        }
        tvMeta.setText(meta);

        MarkdownUtils.render(this, tvContent, post.getContent());

        tvLikes.setText(String.valueOf(post.getLikes() != null ? post.getLikes() : 0));
        tvComments.setText(String.valueOf(post.getComments() != null ? post.getComments().size() : 0));
        tvViews.setText(String.valueOf(post.getViewCount() != null ? post.getViewCount() : 0));

        // 显示图片
        List<Post.Image> postImages = post.getImages();
        if (postImages != null && !postImages.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (Post.Image img : postImages) {
                if (img != null && img.getUrl() != null) {
                    imageUrls.add(img.getUrl());
                }
            }
            
            if (!imageUrls.isEmpty()) {
                rvImages.setVisibility(View.VISIBLE);
                int spanCount = imageUrls.size() == 1 ? 1 : (imageUrls.size() <= 4 ? 2 : 3);
                rvImages.setLayoutManager(new GridLayoutManager(this, spanCount));
                PostImagesAdapter imagesAdapter = new PostImagesAdapter(this, imageUrls);
                rvImages.setAdapter(imagesAdapter);
            } else {
                rvImages.setVisibility(View.GONE);
            }
        } else {
            rvImages.setVisibility(View.GONE);
        }

        // 显示评论列表
        List<Post.Comment> postComments = post.getComments();
        // 设置当前用户ID和帖子作者ID，用于判断删除权限
        commentsAdapter.setCurrentUserId(UserManager.getInstance(this).getCurrentUserId());
        commentsAdapter.setPostAuthorId(post.getUserId());
        commentsAdapter.setComments(postComments);
        tvComments.setText(String.valueOf(postComments != null ? postComments.size() : 0));

        updateLikeUI();
        
        // 检查收藏状态
        checkFavoriteStatus();
    }

    private void toggleLike() {
        if (currentPost == null) return;
        
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        ApiClient.getInstance(this).postForm("/posts/" + currentPost.getId() + "/like", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        try {
                            LikeResponse data = gson.fromJson(responseBody, LikeResponse.class);
                            if (data.success) {
                                currentPost.setLikes(data.likes);
                                currentPost.setLiked(data.liked);
                                tvLikes.setText(String.valueOf(data.likes));
                                updateLikeUI();
                            }
                        } catch (Exception e) {
                            showError("操作失败");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> showError(error));
                }
            }, null);
    }

    /**
     * 评论点赞/取消点赞
     */
    private void toggleCommentLike(Post.Comment comment, int position) {
        if (currentPost == null || comment == null) return;
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>();
        ApiClient.getInstance(this).postForm(
                "/posts/" + currentPost.getId() + "/comments/" + comment.getId() + "/like", params,
                new ApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String responseBody) {
                        runOnUiThread(() -> {
                            try {
                                CommentLikeResponse resp = gson.fromJson(responseBody, CommentLikeResponse.class);
                                if (resp != null && resp.success) {
                                    comment.setLikes(resp.likes);
                                    if (comment.getLikedBy() == null) {
                                        comment.setLikedBy(new ArrayList<>());
                                    }
                                    if (resp.liked) {
                                        if (!comment.getLikedBy().contains(userId)) {
                                            comment.getLikedBy().add(userId);
                                        }
                                    } else {
                                        comment.getLikedBy().remove(userId);
                                    }
                                    commentsAdapter.notifyItemChanged(position);
                                } else {
                                    showError(resp != null && resp.message != null ? resp.message : "操作失败");
                                }
                            } catch (Exception e) {
                                showError("操作失败");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> showError(error));
                    }
                }, null);
    }
    
    private void toggleReplyLike(Post.Comment comment, Post.Reply reply) {
        if (currentPost == null || comment == null || reply == null) return;
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>();
        ApiClient.getInstance(this).postForm(
                "/posts/" + currentPost.getId() + "/comments/" + comment.getId()
                        + "/replies/" + reply.getId() + "/like", params,
                new ApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String responseBody) {
                        runOnUiThread(() -> {
                            try {
                                CommentLikeResponse resp = gson.fromJson(responseBody, CommentLikeResponse.class);
                                if (resp != null && resp.success) {
                                    reply.setLikes(resp.likes);
                                    if (reply.getLikedBy() == null) {
                                        reply.setLikedBy(new ArrayList<>());
                                    }
                                    if (resp.liked) {
                                        if (!reply.getLikedBy().contains(userId)) {
                                            reply.getLikedBy().add(userId);
                                        }
                                    } else {
                                        reply.getLikedBy().remove(userId);
                                    }
                                    // 回复在嵌套 adapter 内，整体刷新评论列表以同步点赞状态
                                    commentsAdapter.notifyDataSetChanged();
                                } else {
                                    showError(resp != null && resp.message != null ? resp.message : "操作失败");
                                }
                            } catch (Exception e) {
                                showError("操作失败");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> showError(error));
                    }
                }, null);
    }

    private void sendComment() {
        if (currentPost == null) return;
        
        UserManager userManager = UserManager.getInstance(this);
        String userId = userManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String username = userManager.getCurrentUsername();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "无法获取用户信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String content = etComment.getText() != null ? etComment.getText().toString().trim() : "";
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnSend.setEnabled(false);

        Map<String, String> params = new HashMap<>();
        params.put("content", content);

        log("Sending comment to post: " + currentPost.getId());

        // 带图走 multipart（服务端 addComment 为 upload.array('images', 5)）
        File imageFile = selectedCommentImage != null ? prepareCommentImage(selectedCommentImage) : null;
        if (imageFile != null) {
            Map<String, Object> multiParams = new HashMap<>();
            multiParams.put("content", content);
            ApiClient.getInstance(this).postMultipart("/posts/" + currentPost.getId() + "/comments", multiParams,
                java.util.Collections.singletonList(imageFile), "images", new okhttp3.Callback() {
                    @Override
                    public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            btnSend.setEnabled(true);
                            showError("评论失败: " + e.getMessage());
                        });
                    }

                    @Override
                    public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                        final String body = response.body() != null ? response.body().string() : "";
                        runOnUiThread(() -> {
                            btnSend.setEnabled(true);
                            try {
                                BaseResponse resp = gson.fromJson(body, BaseResponse.class);
                                if (resp != null && Boolean.TRUE.equals(resp.success)) {
                                    etComment.setText("");
                                    selectedCommentImage = null;
                                    Toast.makeText(PostDetailActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                                    loadPostDetail();
                                } else {
                                    showError(resp != null && resp.message != null ? resp.message : "评论失败");
                                }
                            } catch (Exception e) {
                                showError("操作失败");
                            }
                        });
                    }
                });
        } else {
            ApiClient.getInstance(this).postForm("/posts/" + currentPost.getId() + "/comments", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        try {
                            BaseResponse response = gson.fromJson(responseBody, BaseResponse.class);
                            if (response.success) {
                                etComment.setText("");
                                Toast.makeText(PostDetailActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                                loadPostDetail();
                            } else {
                                showError(response.message != null ? response.message : "评论失败");
                            }
                        } catch (Exception e) {
                            showError("操作失败");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        log("Comment error: " + error);
                        showError(error);
                    });
                }
            }, null);
        }
    }
    
    /**
     * 发送评论或回复
     */
    private void sendCommentOrReply() {
        if (currentPost == null) return;
        
        String content = etComment.getText() != null ? etComment.getText().toString().trim() : "";
        if (TextUtils.isEmpty(content) && selectedCommentImage == null) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (replyingToComment != null) {
            // 发送回复
            sendReply(content);
        } else {
            // 发送评论
            sendComment();
        }
    }

    /**
     * 选择评论图片（单张）
     */
    private void pickCommentImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_PICK_COMMENT_IMAGE);
    }

    /**
     * 压缩评论图片为可上传文件（失败返回 null）
     */
    private File prepareCommentImage(Uri uri) {
        try {
            File file = com.schoolforum.app.utils.ImageUtils.compressImage(this, uri);
            if (file != null) return file;
        } catch (Exception e) {
            log("评论图片压缩失败: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_COMMENT_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedCommentImage = data.getData();
            Toast.makeText(this, "已选择图片（再次点击可更换）", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 发送回复
     */
    private void sendReply(String content) {
        UserManager userManager = UserManager.getInstance(this);
        String userId = userManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String username = userManager.getCurrentUsername();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "无法获取用户信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnSend.setEnabled(false);
        
        Map<String, String> params = new HashMap<>();
        params.put("content", content);
        
        // 如果是回复的回复，设置 replyToId
        if (replyingToReply != null && replyingToReply.getId() != null) {
            params.put("replyToId", replyingToReply.getId());
        }

        String url = "/posts/" + currentPost.getId() + "/comments/" + replyingToComment.getId() + "/replies";
        log("Sending reply to: " + url);

        // 带图走 multipart（服务端 replyComment 为 upload.array('images', 5)）
        File imageFile = selectedCommentImage != null ? prepareCommentImage(selectedCommentImage) : null;
        if (imageFile != null) {
            Map<String, Object> multiParams = new HashMap<>();
            multiParams.put("content", content);
            if (replyingToReply != null && replyingToReply.getId() != null) {
                multiParams.put("replyToId", replyingToReply.getId());
            }
            ApiClient.getInstance(this).postMultipart(url, multiParams,
                java.util.Collections.singletonList(imageFile), "images", new okhttp3.Callback() {
                    @Override
                    public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            btnSend.setEnabled(true);
                            showError("回复失败: " + e.getMessage());
                        });
                    }

                    @Override
                    public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                        final String body = response.body() != null ? response.body().string() : "";
                        runOnUiThread(() -> {
                            btnSend.setEnabled(true);
                            try {
                                BaseResponse resp = gson.fromJson(body, BaseResponse.class);
                                if (resp != null && Boolean.TRUE.equals(resp.success)) {
                                    etComment.setText("");
                                    selectedCommentImage = null;
                                    clearReplyState();
                                    Toast.makeText(PostDetailActivity.this, "回复成功", Toast.LENGTH_SHORT).show();
                                    loadPostDetail();
                                } else {
                                    showError(resp != null && resp.message != null ? resp.message : "回复失败");
                                }
                            } catch (Exception e) {
                                showError("操作失败");
                            }
                        });
                    }
                });
        } else {
            ApiClient.getInstance(this).postForm(url, params,
                new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        try {
                            BaseResponse response = gson.fromJson(responseBody, BaseResponse.class);
                            if (response.success) {
                                etComment.setText("");
                                clearReplyState();
                                Toast.makeText(PostDetailActivity.this, "回复成功", Toast.LENGTH_SHORT).show();
                                loadPostDetail();
                            } else {
                                showError(response.message != null ? response.message : "回复失败");
                            }
                        } catch (Exception e) {
                            showError("操作失败");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        showError(error);
                    });
                }
            }, null);
        }
    }
    
    /**
     * 切换收藏状态
     */
    private void toggleFavorite() {
        if (currentPost == null) return;
        
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);
        
        if (isFavorited) {
            // 取消收藏
            ApiClient.getInstance(this).delete("/favorites/" + currentPost.getId(), params,
                new ApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String responseBody) {
                        runOnUiThread(() -> {
                            isFavorited = false;
                            updateFavoriteUI();
                            Toast.makeText(PostDetailActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> showError(error));
                    }
                }, null);
        } else {
            // 添加收藏
            ApiClient.getInstance(this).postForm("/favorites/" + currentPost.getId(), params,
                new ApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String responseBody) {
                        runOnUiThread(() -> {
                            isFavorited = true;
                            updateFavoriteUI();
                            Toast.makeText(PostDetailActivity.this, "收藏成功", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> showError(error));
                    }
                }, null);
        }
    }
    
    /**
     * 检查收藏状态
     */
    private void checkFavoriteStatus() {
        if (currentPost == null) return;
        
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) return;
        
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);
        
        ApiClient.getInstance(this).get("/favorites/" + currentPost.getId() + "/check", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject data = gson.fromJson(responseBody, JsonObject.class);
                            if (data.has("success") && data.get("success").getAsBoolean()) {
                                JsonObject result = data.has("data") ? data.getAsJsonObject("data") : data;
                                isFavorited = result.has("favorited") && result.get("favorited").getAsBoolean();
                                updateFavoriteUI();
                            }
                        } catch (Exception e) {
                            // 忽略
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    // 忽略
                }
            }, null);
    }
    
    private void updateFavoriteUI() {
        if (ivFavorite == null) return;
        ivFavorite.setColorFilter(
            isFavorited ? getColor(R.color.warning) : getColor(R.color.text_secondary)
        );
        // 更新文字
        TextView tvFavorite = findViewById(R.id.tvFavorite);
        if (tvFavorite != null) {
            tvFavorite.setText(isFavorited ? "已收藏" : "收藏");
            tvFavorite.setTextColor(isFavorited ? getColor(R.color.warning) : getColor(R.color.text_secondary));
        }
    }

    private void updateLikeUI() {
        if (currentPost == null) return;
        
        boolean liked = Boolean.TRUE.equals(currentPost.getLiked());
        ImageView ivLike = findViewById(R.id.ivLike);
        ivLike.setColorFilter(
            liked ? getColor(R.color.error) : getColor(R.color.text_secondary)
        );
        tvLikes.setTextColor(liked ? getColor(R.color.error) : getColor(R.color.text_secondary));
    }
    
    /**
     * 显示更多菜单
     */
    private void showMoreMenu() {
        if (currentPost == null) return;
        
        String currentUserId = UserManager.getInstance(this).getCurrentUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(currentPost.getUserId());
        
        PopupMenu popup = new PopupMenu(this, btnMore);
        
        if (isOwner) {
            // 帖子作者：显示编辑和删除
            popup.getMenu().add(0, 1, 0, "编辑");
            popup.getMenu().add(0, 2, 1, "删除");
        } else {
            // 非作者：显示举报
            popup.getMenu().add(0, 3, 0, "举报");
        }
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    editPost();
                    return true;
                case 2:
                    showDeleteConfirmDialog();
                    return true;
                case 3:
                    showReportDialog();
                    return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("删除帖子")
            .setMessage("确定要删除这篇帖子吗？删除后无法恢复。")
            .setPositiveButton("删除", (dialog, which) -> deletePost())
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 显示举报页面
     */
    private void showReportDialog() {
        if (currentPost == null) return;
        startActivity(com.schoolforum.app.ui.report.ReportActivity.createIntent(
            this,
            com.schoolforum.app.ui.report.ReportActivity.TARGET_TYPE_POST,
            currentPost.getId()
        ));
    }
    
    /**
     * 显示举报评论页面
     */
    private void showReportCommentDialog(String commentId) {
        startActivity(com.schoolforum.app.ui.report.ReportActivity.createIntent(
            this,
            com.schoolforum.app.ui.report.ReportActivity.TARGET_TYPE_COMMENT,
            commentId
        ));
    }
    
    /**
     * 编辑帖子
     */
    private void editPost() {
        Intent intent = new Intent(this, EditPostActivity.class);
        intent.putExtra("postId", currentPost.getId());
        startActivity(intent);
    }
    
    /**
     * 删除帖子
     */
    private void deletePost() {
        if (currentPost == null) return;
        
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);
        
        ApiClient.getInstance(this).delete("/posts/" + currentPost.getId(), params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        try {
                            BaseResponse response = gson.fromJson(responseBody, BaseResponse.class);
                            if (response.success) {
                                Toast.makeText(PostDetailActivity.this, "帖子已删除", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                showError(response.message != null ? response.message : "删除失败");
                            }
                        } catch (Exception e) {
                            showError("操作失败");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        showError(error);
                    });
                }
            }, null);
    }
    
    /**
     * 显示删除评论/回复确认对话框
     */
    private void showDeleteCommentDialog(String commentId, String replyId, String nestedReplyId) {
        String title, message;
        if (nestedReplyId != null) {
            title = "删除嵌套回复";
            message = "确定要删除这条嵌套回复吗？";
        } else if (replyId != null) {
            title = "删除回复";
            message = "确定要删除这条回复吗？";
        } else {
            title = "删除评论";
            message = "确定要删除这条评论吗？删除后该评论下的所有回复也会被删除。";
        }
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("删除", (dialog, which) -> deleteComment(commentId, replyId, nestedReplyId))
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 删除评论或回复
     */
    private void deleteComment(String commentId, String replyId, String nestedReplyId) {
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);
        if (replyId != null) {
            params.put("replyId", replyId);
        }
        if (nestedReplyId != null) {
            params.put("nestedReplyId", nestedReplyId);
        }
        
        ApiClient.getInstance(this).delete("/posts/" + currentPost.getId() + "/comments/" + commentId, params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        try {
                            BaseResponse response = gson.fromJson(responseBody, BaseResponse.class);
                            if (response.success) {
                                Toast.makeText(PostDetailActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                loadPostDetail();
                            } else {
                                showError(response.message != null ? response.message : "删除失败");
                            }
                        } catch (Exception e) {
                            showError("操作失败");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        showError(error);
                    });
                }
            }, null);
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class BaseResponse {
        boolean success;
        String message;
    }

    private static class PostResponse extends BaseResponse {
        @SerializedName("post")
        Post post;
    }

    private static class LikeResponse extends BaseResponse {
        @SerializedName("likes")
        int likes;
        @SerializedName("liked")
        boolean liked;
        @SerializedName("dislikes")
        int dislikes;
        @SerializedName("disliked")
        boolean disliked;
    }

    private static class CommentLikeResponse extends BaseResponse {
        @SerializedName("likes")
        int likes;
        @SerializedName("liked")
        boolean liked;
    }
}