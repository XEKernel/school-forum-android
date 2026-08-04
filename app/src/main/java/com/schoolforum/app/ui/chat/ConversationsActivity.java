package com.schoolforum.app.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Conversation;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.UserManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话列表Activity
 */
public class ConversationsActivity extends AppCompatActivity implements ConversationsAdapter.OnConversationClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View emptyView;

    private ConversationsAdapter adapter;
    private List<Conversation> conversations;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        conversations = new ArrayList<>();
        initViews();
        loadConversations();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new ConversationsAdapter();
        adapter.setOnConversationClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadConversations);
    }

    private void loadConversations() {
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) {
            showError("请先登录");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        ApiClient.getInstance(this).get("/conversations", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        try {
                            ConversationsResponse response = gson.fromJson(responseBody, ConversationsResponse.class);
                            if (response.success && response.conversations != null) {
                                conversations = new ArrayList<>(response.conversations);
                                adapter.submitList(conversations);
                                emptyView.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
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

    @Override
    public void onConversationClick(Conversation conversation, Conversation.OtherUser otherUser) {
        android.content.Intent intent = new android.content.Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, otherUser.getId());
        intent.putExtra(ChatActivity.EXTRA_OTHER_USERNAME, otherUser.getUsername());
        if (otherUser.getAvatar() != null) {
            intent.putExtra(ChatActivity.EXTRA_OTHER_AVATAR, otherUser.getAvatar());
        }
        startActivity(intent);
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

    private static class ConversationsResponse {
        boolean success;
        String message;
        @SerializedName("conversations")
        List<Conversation> conversations;
    }
}
