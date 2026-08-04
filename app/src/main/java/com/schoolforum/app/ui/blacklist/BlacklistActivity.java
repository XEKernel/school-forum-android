package com.schoolforum.app.ui.blacklist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
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

/**
 * 黑名单列表Activity
 */
public class BlacklistActivity extends AppCompatActivity implements BlacklistAdapter.OnUserActionListener {
    private static final String TAG = "BlacklistActivity";

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyView;

    private BlacklistAdapter adapter;
    private List<User> users = new ArrayList<>();
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
        setContentView(R.layout.activity_blacklist);

        initUtils();
        initViews();
        loadBlacklist();
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

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("黑名单");
        }

        adapter = new BlacklistAdapter();
        adapter.setOnUserActionListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMore = true;
            loadBlacklist();
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
                        loadMoreBlacklist();
                    }
                }
            }
        });
    }

    private void loadBlacklist() {
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

        apiClient.get("/blocked/" + userId, params,
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

    private void loadMoreBlacklist() {
        currentPage++;
        loadBlacklist();
    }

    private void updateEmptyState() {
        emptyView.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(users.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onUserClick(User user) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("userId", user.getId());
        startActivity(intent);
    }

    @Override
    public void onUnblockClick(User user, int position) {
        String currentUserId = userManager.getCurrentUserId();
        if (currentUserId == null) return;

        Map<String, String> params = new HashMap<>();
        params.put("blockerId", currentUserId);
        params.put("blockedId", user.getId());

        apiClient.postForm("/unblock", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        users.remove(position);
                        adapter.setUsers(new ArrayList<>(users));
                        updateEmptyState();
                        Toast.makeText(BlacklistActivity.this, "已移出黑名单", Toast.LENGTH_SHORT).show();
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
