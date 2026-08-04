package com.schoolforum.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.schoolforum.app.MainActivity;
import com.schoolforum.app.R;
import com.schoolforum.app.model.ApiResponse;
import com.schoolforum.app.model.Post;
import com.schoolforum.app.model.User;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.ui.post.EditPostActivity;

import java.util.HashMap;
import java.util.Map;
import com.schoolforum.app.ui.post.PostsAdapter;
import com.schoolforum.app.utils.FileLogger;
import com.schoolforum.app.utils.UserManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 个人主页Activity
 */
public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    // UI组件
    private CircleImageView ivAvatar;
    private TextView tvUsername;
    private TextView tvSchool;
    private TextView tvBio;
    private TextView tvPostCount;
    private TextView tvFollowingCount;
    private TextView tvFollowerCount;
    private TextView tvLikeCount;
    private RecyclerView rvPosts;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabNewPost;
    private Toolbar toolbar;
    private LinearLayout layoutSettings;
    private LinearLayout layoutFavorites;
    private LinearLayout layoutMyFollowing;
    private LinearLayout layoutBlacklist;
    private LinearLayout layoutFollowers;
    private LinearLayout layoutFollowing;
    private LinearLayout layoutPosts;
    private android.widget.Button btnFollow;

    // 数据
    private User currentUser;
    private String userId;
    private boolean isOwnProfile = true;
    private boolean isFollowing = false;
    private PostsAdapter postsAdapter;

    // 工具
    private UserManager userManager;
    private ApiClient apiClient;
    private ExecutorService executor;
    private Handler mainHandler;
    private FileLogger logger;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initUtils();
        initViews();
        setupRecyclerView();
        setupClickListeners();
        setupToolbar();

        // 获取用户ID
        userId = getIntent().getStringExtra("userId");
        log("Intent userId: " + userId);
        
        if (userId == null || userId.isEmpty()) {
            // 尝试从UserManager获取
            userId = userManager.getCurrentUserId();
            log("UserManager userId: " + userId);
            isOwnProfile = true;
        } else {
            String currentUserId = userManager.getCurrentUserId();
            isOwnProfile = userId.equals(currentUserId);
        }
        
        log("Final userId: " + userId + ", isOwnProfile: " + isOwnProfile);
        
        // 更新标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isOwnProfile ? "个人主页" : "用户主页");
        }
        
        // 非自己的主页隐藏设置入口
        if (layoutSettings != null) {
            layoutSettings.setVisibility(isOwnProfile ? View.VISIBLE : View.GONE);
        }
        
        // 关注按钮（非自己的主页显示）
        btnFollow = findViewById(R.id.btnFollow);
        btnFollow.setVisibility(isOwnProfile ? View.GONE : View.VISIBLE);
        btnFollow.setOnClickListener(v -> toggleFollow());
        
        if (!isOwnProfile) {
            // 隐藏功能入口卡片中的收藏和黑名单
            if (layoutFavorites != null) layoutFavorites.setVisibility(View.GONE);
            if (layoutBlacklist != null) layoutBlacklist.setVisibility(View.GONE);
            // 隐藏发帖按钮
            fabNewPost.setVisibility(View.GONE);
        }

        // 加载用户数据
        if (userId != null && !userId.isEmpty()) {
            loadUserData();
        } else {
            Toast.makeText(this, "无法获取用户信息", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void log(String message) {
        Log.d(TAG, message);
        if (logger != null) {
            logger.d(TAG, message);
        }
    }

    private void initUtils() {
        userManager = UserManager.getInstance(this);
        apiClient = ApiClient.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        logger = FileLogger.getInstance(this);
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvSchool = findViewById(R.id.tvSchool);
        tvBio = findViewById(R.id.tvBio);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        tvFollowerCount = findViewById(R.id.tvFollowerCount);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        rvPosts = findViewById(R.id.rvPosts);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
        fabNewPost = findViewById(R.id.fabNewPost);
        toolbar = findViewById(R.id.toolbar);
        layoutSettings = findViewById(R.id.layoutSettings);
        layoutFavorites = findViewById(R.id.layoutFavorites);
        layoutMyFollowing = findViewById(R.id.layoutMyFollowing);
        layoutBlacklist = findViewById(R.id.layoutBlacklist);
        layoutFollowers = findViewById(R.id.layoutFollowers);
        layoutFollowing = findViewById(R.id.layoutFollowing);
        layoutPosts = findViewById(R.id.layoutPosts);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("个人主页");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        postsAdapter = new PostsAdapter();
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postsAdapter);
        
        // 设置点击监听
        postsAdapter.setOnPostClickListener(new PostsAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(Post post) {
                // 跳转到原生帖子详情页
                Intent intent = new Intent(ProfileActivity.this, com.schoolforum.app.ui.post.PostDetailActivity.class);
                intent.putExtra("post_id", post.getId());
                startActivity(intent);
            }

            @Override
            public void onLikeClick(Post post, int position) {
                // 处理点赞
            }

            @Override
            public void onMoreClick(Post post, View anchor) {
                // 显示更多选项
            }
        });
    }

    private void setupClickListeners() {
        // 发帖按钮
        fabNewPost.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditPostActivity.class);
            startActivity(intent);
        });

        // 设置入口（卡片中）
        if (layoutSettings != null) {
            layoutSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.schoolforum.app.ui.settings.SettingsActivity.class);
                startActivity(intent);
            });
        }

        // 我的收藏
        layoutFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.schoolforum.app.ui.favorites.FavoritesActivity.class);
            startActivity(intent);
        });

        // 我的关注
        layoutMyFollowing.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.schoolforum.app.ui.follow.FollowListActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("type", "following");
            startActivity(intent);
        });

        // 黑名单
        layoutBlacklist.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.schoolforum.app.ui.blacklist.BlacklistActivity.class);
            startActivity(intent);
        });

        // 关注/粉丝点击
        layoutFollowers.setOnClickListener(v -> showFollowList("followers"));
        layoutFollowing.setOnClickListener(v -> showFollowList("following"));
    }

    private void showFollowList(String type) {
        Intent intent = new Intent(this, com.schoolforum.app.ui.follow.FollowListActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("type", type);
        startActivity(intent);
    }

    private void loadUserData() {
        if (userId == null || userId.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "用户ID无效", Toast.LENGTH_SHORT).show();
            return;
        }
        
        log("Loading user data for: " + userId);
        showLoading(true);

        // 获取当前用户ID用于帖子可见性过滤
        String viewerId = userManager.getCurrentUserId();
        String url = "/users/" + userId;
        if (viewerId != null && !viewerId.isEmpty()) {
            url += "?viewerId=" + viewerId;
        }

        // 获取用户资料
        apiClient.get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log("API call failed: " + e.getMessage());
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    log("User response: " + body);
                    
                    // 直接使用JsonObject解析
                    Gson gson = new Gson();
                    com.google.gson.JsonObject rootObj = gson.fromJson(body, com.google.gson.JsonObject.class);
                    
                    if (rootObj != null && rootObj.has("success") && rootObj.get("success").getAsBoolean()) {
                        // 后端返回的数据直接在根对象中，不是在 data 字段里
                        com.google.gson.JsonObject userJson = rootObj.has("user") ? rootObj.getAsJsonObject("user") : null;
                        com.google.gson.JsonObject statsJson = rootObj.has("stats") ? rootObj.getAsJsonObject("stats") : null;
                        com.google.gson.JsonArray recentPostsJson = rootObj.has("recentPosts") ? rootObj.getAsJsonArray("recentPosts") : null;
                        
                        log("userJson: " + (userJson != null ? userJson.toString() : "null"));
                        log("statsJson: " + (statsJson != null ? statsJson.toString() : "null"));
                        log("recentPostsJson count: " + (recentPostsJson != null ? recentPostsJson.size() : 0));
                        
                        if (userJson != null) {
                            currentUser = gson.fromJson(userJson, User.class);
                            log("Parsed user: " + (currentUser != null ? currentUser.getUsername() : "null"));
                            
                            // 解析帖子数据
                            List<Post> posts = null;
                            if (recentPostsJson != null && recentPostsJson.size() > 0) {
                                posts = gson.fromJson(recentPostsJson, new TypeToken<List<Post>>(){}.getType());
                                log("Parsed posts count: " + (posts != null ? posts.size() : 0));
                            }
                            
                            final com.google.gson.JsonObject stats = statsJson;
                            final List<Post> finalPosts = posts;
                            mainHandler.post(() -> {
                                updateUserUI(currentUser, stats);
                                updatePostsUI(finalPosts);
                            });
                        } else {
                            mainHandler.post(() -> {
                                showLoading(false);
                                Toast.makeText(ProfileActivity.this, "用户数据格式错误", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        String msg = rootObj != null && rootObj.has("message") ? 
                            rootObj.get("message").getAsString() : "未知错误";
                        log("API returned error: " + msg);
                        mainHandler.post(() -> {
                            showLoading(false);
                            Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    log("Parse error: " + e.getMessage());
                    mainHandler.post(() -> {
                        showLoading(false);
                        Toast.makeText(ProfileActivity.this, "解析数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });

        // 获取关注统计
        loadFollowStats();
    }

    private void loadFollowStats() {
        if (userId == null || userId.isEmpty()) return;
        
        apiClient.get("/follow/stats/" + userId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取关注统计失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    Gson gson = new Gson();
                    com.google.gson.JsonObject rootObj = gson.fromJson(body, com.google.gson.JsonObject.class);
                    
                    if (rootObj != null && rootObj.has("success") && rootObj.get("success").getAsBoolean()) {
                        com.google.gson.JsonObject data = rootObj.has("data") ? rootObj.getAsJsonObject("data") : null;
                        if (data != null) {
                            mainHandler.post(() -> {
                                if (data.has("followingCount")) {
                                    tvFollowingCount.setText(String.valueOf(data.get("followingCount").getAsInt()));
                                }
                                if (data.has("followerCount")) {
                                    tvFollowerCount.setText(String.valueOf(data.get("followerCount").getAsInt()));
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析关注统计失败: " + e.getMessage());
                }
            }
        });
        
        // 检查关注状态（非自己的主页）
        if (!isOwnProfile) {
            checkFollowStatus();
        }
    }
    
    /**
     * 检查关注状态
     */
    private void checkFollowStatus() {
        String currentUserId = userManager.getCurrentUserId();
        if (currentUserId == null || userId == null) {
            log("checkFollowStatus: currentUserId or userId is null");
            return;
        }
        
        log("checkFollowStatus: currentUserId=" + currentUserId + ", userId=" + userId);
        
        String url = "/follow/status?followerId=" + currentUserId + "&followingId=" + userId;
        
        apiClient.get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "检查关注状态失败: " + e.getMessage());
                // 失败时，尝试从数据库直接获取状态
                checkFollowStatusFromDB();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    log("checkFollowStatus response: " + body);
                    Gson gson = new Gson();
                    com.google.gson.JsonObject rootObj = gson.fromJson(body, com.google.gson.JsonObject.class);
                    
                    if (rootObj != null && rootObj.has("success") && rootObj.get("success").getAsBoolean()) {
                        // 数据直接在根对象中（不在 data 字段中）
                        boolean following = rootObj.has("isFollowing") && rootObj.get("isFollowing").getAsBoolean();
                        log("checkFollowStatus: isFollowing=" + following);
                        mainHandler.post(() -> {
                            isFollowing = following;
                            updateFollowButton();
                        });
                    } else {
                        // 响应失败，尝试从数据库获取
                        checkFollowStatusFromDB();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析关注状态失败: " + e.getMessage());
                    checkFollowStatusFromDB();
                }
            }
        });
    }
    
    /**
     * 从数据库直接检查关注状态（不依赖Redis缓存）
     */
    private void checkFollowStatusFromDB() {
        String currentUserId = userManager.getCurrentUserId();
        if (currentUserId == null || userId == null) return;
        
        // 通过获取关注列表来检查
        String url = "/following/" + currentUserId + "?page=1&limit=100";
        
        apiClient.get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "从数据库检查关注状态失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    Gson gson = new Gson();
                    com.google.gson.JsonObject rootObj = gson.fromJson(body, com.google.gson.JsonObject.class);
                    
                    if (rootObj != null && rootObj.has("success") && rootObj.get("success").getAsBoolean()) {
                        com.google.gson.JsonObject data = rootObj.has("data") ? rootObj.getAsJsonObject("data") : rootObj;
                        com.google.gson.JsonArray listArray = data.has("list") ? data.getAsJsonArray("list") : new com.google.gson.JsonArray();
                        
                        // 检查目标用户是否在关注列表中
                        boolean found = false;
                        for (int i = 0; i < listArray.size(); i++) {
                            com.google.gson.JsonObject userObj = listArray.get(i).getAsJsonObject();
                            if (userObj.has("id") && userId.equals(userObj.get("id").getAsString())) {
                                found = true;
                                break;
                            }
                        }
                        
                        log("checkFollowStatusFromDB: isFollowing=" + found);
                        final boolean isFollowingResult = found;
                        mainHandler.post(() -> {
                            isFollowing = isFollowingResult;
                            updateFollowButton();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析关注列表失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 切换关注状态
     */
    private void toggleFollow() {
        String currentUserId = userManager.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Map<String, String> params = new HashMap<>();
        params.put("followerId", currentUserId);
        params.put("followingId", userId);
        
        if (isFollowing) {
            // 取消关注
            apiClient.delete("/follow", params, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> Toast.makeText(ProfileActivity.this, "操作失败", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String body = response.body() != null ? response.body().string() : "";
                        Gson gson = new Gson();
                        com.google.gson.JsonObject rootObj = gson.fromJson(body, com.google.gson.JsonObject.class);
                        boolean success = rootObj != null && rootObj.has("success") && rootObj.get("success").getAsBoolean();
                        mainHandler.post(() -> {
                            if (success) {
                                isFollowing = false;
                                updateFollowButton();
                                Toast.makeText(ProfileActivity.this, "已取消关注", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ProfileActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> Toast.makeText(ProfileActivity.this, "操作失败", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } else {
            // 关注
            apiClient.postForm("/follow", params, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> Toast.makeText(ProfileActivity.this, "操作失败", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String body = response.body() != null ? response.body().string() : "";
                        Gson gson = new Gson();
                        com.google.gson.JsonObject rootObj = gson.fromJson(body, com.google.gson.JsonObject.class);
                        boolean success = rootObj != null && rootObj.has("success") && rootObj.get("success").getAsBoolean();
                        mainHandler.post(() -> {
                            if (success) {
                                isFollowing = true;
                                updateFollowButton();
                                Toast.makeText(ProfileActivity.this, "关注成功", Toast.LENGTH_SHORT).show();
                            } else {
                                String msg = rootObj != null && rootObj.has("message") ? 
                                    rootObj.get("message").getAsString() : "操作失败";
                                Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> Toast.makeText(ProfileActivity.this, "操作失败", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }
    }
    
    private void updateFollowButton() {
        if (btnFollow == null) return;
        if (isFollowing) {
            btnFollow.setText("已关注");
            btnFollow.setBackgroundResource(R.drawable.btn_outline);
        } else {
            btnFollow.setText("关注");
            btnFollow.setBackgroundResource(R.drawable.btn_primary);
        }
    }

    private void updateUserUI(User user, com.google.gson.JsonObject stats) {
        if (user == null) {
            log("updateUserUI: user is null");
            return;
        }
        
        log("updateUserUI: username=" + user.getUsername() + ", avatar=" + user.getAvatar());
        
        // 设置头像
        String avatarUrl = user.getAvatar();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (!avatarUrl.startsWith("http")) {
                avatarUrl = MainActivity.getBaseUrl() + avatarUrl;
            }
            log("Loading avatar: " + avatarUrl);
            com.bumptech.glide.Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }

        // 设置用户名
        String username = user.getUsername();
        tvUsername.setText(username != null ? username : "未知用户");

        // 设置学校和班级
        StringBuilder schoolInfo = new StringBuilder();
        if (user.getGrade() != null && !user.getGrade().isEmpty()) {
            schoolInfo.append(user.getGrade());
        }
        if (user.getClassName() != null && !user.getClassName().isEmpty()) {
            if (schoolInfo.length() > 0) schoolInfo.append(" · ");
            schoolInfo.append(user.getClassName());
        }
        tvSchool.setText(schoolInfo.length() > 0 ? schoolInfo.toString() : "未设置班级");

        // 设置简介
        String bio = user.getBio();
        tvBio.setText(bio != null && !bio.isEmpty() ? bio : "这个人很懒，什么都没写");

        // 设置统计数据
        if (stats != null) {
            log("Stats available: " + stats.toString());
            if (stats.has("postCount")) {
                tvPostCount.setText(String.valueOf(stats.get("postCount").getAsInt()));
            }
            if (stats.has("totalLikes")) {
                tvLikeCount.setText(String.valueOf(stats.get("totalLikes").getAsInt()));
            }
        } else {
            log("Stats is null");
            Integer postCount = user.getPostCount();
            tvPostCount.setText(postCount != null ? String.valueOf(postCount) : "0");
            tvLikeCount.setText("0");
        }
    }

    private void updatePostsUI(List<Post> posts) {
        showLoading(false);
        if (posts != null && !posts.isEmpty()) {
            postsAdapter.submitList(posts);
            layoutEmpty.setVisibility(View.GONE);
            rvPosts.setVisibility(View.VISIBLE);
        } else {
            postsAdapter.submitList(new ArrayList<>());
            layoutEmpty.setVisibility(View.VISIBLE);
            rvPosts.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvPosts.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvPosts.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 数据已在onCreate中加载
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
