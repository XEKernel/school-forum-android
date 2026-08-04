package com.schoolforum.app.ui.follow;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.schoolforum.app.R;
import com.schoolforum.app.model.User;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.ui.profile.ProfileActivity;
import com.schoolforum.app.utils.FileLogger;
import com.schoolforum.app.utils.UserManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 关注/粉丝列表Activity
 */
public class FollowListActivity extends AppCompatActivity {
    private static final String TAG = "FollowListActivity";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_TYPE = "type"; // "following" or "followers"

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyView;

    private FollowListAdapter adapter;
    private List<User> users = new ArrayList<>();
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    private String userId;
    private String type; // "following" or "followers"
    private String currentUserId;

    private UserManager userManager;
    private ApiClient apiClient;
    private ExecutorService executor;
    private Handler mainHandler;
    private FileLogger logger;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_list);

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        type = getIntent().getStringExtra(EXTRA_TYPE);
        
        if (userId == null || type == null) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initUtils();
        initViews();
        loadUsers();
    }

    private void initUtils() {
        userManager = UserManager.getInstance(this);
        currentUserId = userManager.getCurrentUserId();
        apiClient = ApiClient.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        logger = FileLogger.getInstance(this);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("following".equals(type) ? "关注" : "粉丝");
        }

        adapter = new FollowListAdapter();
        adapter.setOnUserActionListener(new FollowListAdapter.OnUserActionListener() {
            @Override
            public void onUserClick(User user) {
                Intent intent = new Intent(FollowListActivity.this, ProfileActivity.class);
                intent.putExtra("userId", user.getId());
                startActivity(intent);
            }

            @Override
            public void onFollowClick(User user, int position) {
                toggleFollow(user, position);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMore = true;
            loadUsers();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    if (!isLoading && hasMore && lastVisiblePosition >= totalItemCount - 3) {
                        loadMoreUsers();
                    }
                }
            }
        });
    }

    private void loadUsers() {
        if (isLoading) return;
        isLoading = true;
        
        if (currentPage == 1) {
            progressBar.setVisibility(View.VISIBLE);
        }

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(currentPage));
        params.put("limit", "20");
        if (currentUserId != null) {
            params.put("currentUserId", currentUserId);
        }

        String endpoint = "following".equals(type) ? "/following/" + userId : "/followers/" + userId;

        apiClient.get(endpoint, params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        try {
                            JsonObject rootObj = gson.fromJson(responseBody, JsonObject.class);
                            if (rootObj.has("success") && rootObj.get("success").getAsBoolean()) {
                                JsonObject data = rootObj.has("data") ? rootObj.getAsJsonObject("data") : rootObj;
                                JsonArray listArray = data.has("list") ? data.getAsJsonArray("list") : new JsonArray();
                                
                                List<User> newUsers = gson.fromJson(listArray, new TypeToken<List<User>>(){}.getType());
                                
                                if (currentPage == 1) {
                                    users.clear();
                                }
                                
                                if (newUsers != null) {
                                    users.addAll(newUsers);
                                }
                                
                                adapter.setUsers(new ArrayList<>(users));
                                updateEmptyState();

                                if (data.has("pagination")) {
                                    JsonObject pagination = data.getAsJsonObject("pagination");
                                    hasMore = pagination.has("hasNext") && pagination.get("hasNext").getAsBoolean();
                                } else {
                                    hasMore = newUsers != null && newUsers.size() >= 20;
                                }
                            }
                        } catch (Exception e) {
                            showError("解析数据失败: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        showError(error);
                    });
                }
            }, null);
    }

    private void loadMoreUsers() {
        currentPage++;
        loadUsers();
    }

    private void updateEmptyState() {
        emptyView.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(users.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void toggleFollow(User user, int position) {
        if (currentUserId == null) {
            showError("请先登录");
            return;
        }

        boolean isFollowing = Boolean.TRUE.equals(user.getIsFollowing());
        Map<String, String> params = new HashMap<>();
        params.put("followerId", currentUserId);
        params.put("followingId", user.getId());

        if (isFollowing) {
            // 取消关注
            apiClient.delete("/follow", params,
                new ApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String responseBody) {
                        runOnUiThread(() -> {
                            user.setIsFollowing(false);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(FollowListActivity.this, "已取消关注", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> showError(error));
                    }
                }, null);
        } else {
            // 关注
            apiClient.postForm("/follow", params,
                new ApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String responseBody) {
                        runOnUiThread(() -> {
                            user.setIsFollowing(true);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(FollowListActivity.this, "关注成功", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> showError(error));
                    }
                }, null);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
