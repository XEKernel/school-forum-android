package com.schoolforum.app.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
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

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Post;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.ui.post.PostDetailActivity;
import com.schoolforum.app.ui.post.PostsAdapter;
import com.schoolforum.app.utils.FileLogger;
import com.schoolforum.app.utils.UserManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 收藏列表Activity
 */
public class FavoritesActivity extends AppCompatActivity implements PostsAdapter.OnPostClickListener {
    private static final String TAG = "FavoritesActivity";

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyView;
    private TextView tvEmptyText;

    private PostsAdapter adapter;
    private List<Post> posts = new ArrayList<>();
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    private UserManager userManager;
    private ApiClient apiClient;
    private ExecutorService executor;
    private Handler mainHandler;
    private FileLogger logger;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        initUtils();
        initViews();
        loadFavorites();
    }

    private void initUtils() {
        userManager = UserManager.getInstance(this);
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
        tvEmptyText = findViewById(R.id.tvEmptyText);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("我的收藏");
        }

        adapter = new PostsAdapter();
        adapter.setOnPostClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMore = true;
            loadFavorites();
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
                        loadMoreFavorites();
                    }
                }
            }
        });
    }

    private void loadFavorites() {
        if (isLoading) return;
        
        String userId = userManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        isLoading = true;
        if (currentPage == 1) {
            progressBar.setVisibility(View.VISIBLE);
        }

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(currentPage));
        params.put("limit", "20");

        apiClient.get("/favorites/user/" + userId, params, 
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
                                JsonArray postsArray = data.has("posts") ? data.getAsJsonArray("posts") : new JsonArray();
                                
                                List<Post> newPosts = gson.fromJson(postsArray, new TypeToken<List<Post>>(){}.getType());
                                
                                if (currentPage == 1) {
                                    posts.clear();
                                }
                                
                                if (newPosts != null) {
                                    posts.addAll(newPosts);
                                }
                                
                                adapter.submitList(new ArrayList<>(posts));
                                updateEmptyState();

                                // 检查分页
                                if (data.has("pagination")) {
                                    JsonObject pagination = data.getAsJsonObject("pagination");
                                    hasMore = pagination.has("hasNext") && pagination.get("hasNext").getAsBoolean();
                                } else {
                                    hasMore = newPosts != null && newPosts.size() >= 20;
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

    private void loadMoreFavorites() {
        currentPage++;
        loadFavorites();
    }

    private void updateEmptyState() {
        if (posts.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onPostClick(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getId());
        startActivity(intent);
    }

    @Override
    public void onLikeClick(Post post, int position) {
        String userId = userManager.getCurrentUserId();
        if (userId == null) {
            showError("请先登录");
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        apiClient.postForm("/posts/" + post.getId() + "/like", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject data = gson.fromJson(responseBody, JsonObject.class);
                            if (data.has("success") && data.get("success").getAsBoolean()) {
                                post.setLikes(data.get("likes").getAsInt());
                                post.setLiked(data.get("liked").getAsBoolean());
                                adapter.notifyItemChanged(position);
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

    @Override
    public void onMoreClick(Post post, View anchor) {
        // 显示取消收藏选项
        showUnfavoriteDialog(post);
    }

    private void showUnfavoriteDialog(Post post) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("取消收藏")
            .setMessage("确定要取消收藏这篇帖子吗？")
            .setPositiveButton("确定", (dialog, which) -> unfavoritePost(post))
            .setNegativeButton("取消", null)
            .show();
    }

    private void unfavoritePost(Post post) {
        String userId = userManager.getCurrentUserId();
        if (userId == null) return;

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        apiClient.delete("/favorites/" + post.getId(), params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        int index = posts.indexOf(post);
                        if (index >= 0) {
                            posts.remove(index);
                            adapter.submitList(new ArrayList<>(posts));
                            updateEmptyState();
                        }
                        Toast.makeText(FavoritesActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> showError(error));
                }
            }, null);
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
