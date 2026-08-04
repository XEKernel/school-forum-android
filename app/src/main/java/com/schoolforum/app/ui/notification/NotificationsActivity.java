package com.schoolforum.app.ui.notification;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Notification;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.ui.post.PostDetailActivity;
import com.schoolforum.app.utils.UserManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知列表Activity
 */
public class NotificationsActivity extends AppCompatActivity implements NotificationsAdapter.OnNotificationClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View emptyView;
    private TextView tvMarkAllRead;

    private NotificationsAdapter adapter;
    private List<Notification> notifications;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notifications = new ArrayList<>();
        initViews();
        loadNotifications();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead);

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new NotificationsAdapter();
        adapter.setOnNotificationClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadNotifications);

        tvMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void loadNotifications() {
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) {
            showError("请先登录");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        ApiClient.getInstance(this).get("/notifications", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        try {
                            NotificationsResponse response = gson.fromJson(responseBody, NotificationsResponse.class);
                            if (response.success && response.notifications != null) {
                                notifications = new ArrayList<>(response.notifications);
                                adapter.submitList(notifications);
                                emptyView.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
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
                        swipeRefresh.setRefreshing(false);
                        showError(error);
                    });
                }
            }, null);
    }

    private void markAllAsRead() {
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) return;

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        ApiClient.getInstance(this).postForm("/notifications/read-all", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        try {
                            BaseResponse response = gson.fromJson(responseBody, BaseResponse.class);
                            if (response.success) {
                                List<Notification> updated = new ArrayList<>();
                                for (Notification n : notifications) {
                                    n.setRead(true);
                                    updated.add(n);
                                }
                                notifications = updated;
                                adapter.submitList(notifications);
                                Snackbar.make(findViewById(android.R.id.content), "已全部标记为已读", Snackbar.LENGTH_SHORT).show();
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
    public void onNotificationClick(Notification notification) {
        markAsRead(notification);

        String postId = notification.getPostId();
        if (postId != null && Boolean.TRUE.equals(notification.getPostExists())) {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
            startActivity(intent);
        }
    }

    private void markAsRead(Notification notification) {
        if (Boolean.TRUE.equals(notification.getRead())) return;
        
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) return;

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        ApiClient.getInstance(this).postForm("/notifications/" + notification.getId() + "/read", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        notification.setRead(true);
                        adapter.submitList(new ArrayList<>(notifications));
                    });
                }

                @Override
                public void onError(String error) {
                    // 静默失败
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

    private static class NotificationsResponse extends BaseResponse {
        @SerializedName("notifications")
        List<Notification> notifications;
    }
}
