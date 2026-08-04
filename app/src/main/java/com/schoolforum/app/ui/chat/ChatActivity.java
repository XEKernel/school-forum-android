package com.schoolforum.app.ui.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Message;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.UserManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 聊天Activity
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_OTHER_USER_ID = "other_user_id";
    public static final String EXTRA_OTHER_USERNAME = "other_username";
    public static final String EXTRA_OTHER_AVATAR = "other_avatar";

    // 消息轮询间隔（毫秒）
    private static final long POLL_INTERVAL_MS = 5000L;

    private RecyclerView recyclerView;
    private EditText etMessage;
    private View btnSend;
    private ProgressBar progressBar;
    private CircleImageView ivAvatar;

    private MessagesAdapter adapter;
    private List<Message> messages;
    
    private String otherUserId;
    private String currentUserId;
    private final Gson gson = new Gson();

    // 消息轮询
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed()) {
                refreshMessages();
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        currentUserId = UserManager.getInstance(this).getCurrentUserId();

        if (TextUtils.isEmpty(otherUserId) || TextUtils.isEmpty(currentUserId)) {
            finish();
            return;
        }

        messages = new ArrayList<>();
        initViews();
        loadMessages();
        // 启动消息轮询（实时接收新消息）
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止轮询，防止内存泄漏
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);
        ivAvatar = findViewById(R.id.ivAvatar);

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String username = getIntent().getStringExtra(EXTRA_OTHER_USERNAME);
        if (!TextUtils.isEmpty(username)) {
            ((android.widget.TextView) findViewById(R.id.tvUsername)).setText(username);
        }

        String avatar = getIntent().getStringExtra(EXTRA_OTHER_AVATAR);
        if (!TextUtils.isEmpty(avatar)) {
            String avatarUrl = avatar.startsWith("http") ? avatar : ApiClient.getBaseUrl() + avatar;
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .into(ivAvatar);
        }

        adapter = new MessagesAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        progressBar.setVisibility(View.VISIBLE);
        refreshMessages(true);
    }

    /**
     * 拉取消息并合并去重
     * @param showLoading 是否显示加载指示（首屏 true，轮询 false）
     */
    private void refreshMessages(boolean showLoading) {
        if (showLoading) {
            progressBar.setVisibility(View.VISIBLE);
        }

        Map<String, String> params = new HashMap<>();
        params.put("otherUserId", otherUserId);
        params.put("limit", "50");

        ApiClient.getInstance(this).get("/messages", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        try {
                            MessagesResponse response = gson.fromJson(responseBody, MessagesResponse.class);
                            if (response.success && response.messages != null) {
                                // 合并去重（按消息 id），保留原顺序
                                Map<String, Message> merged = new LinkedHashMap<>();
                                for (Message m : messages) {
                                    if (m != null && m.getId() != null) merged.put(m.getId(), m);
                                }
                                for (Message m : response.messages) {
                                    if (m != null && m.getId() != null) merged.put(m.getId(), m);
                                }
                                List<Message> newMessages = new ArrayList<>(merged.values());
                                boolean hasNew = newMessages.size() > messages.size();
                                messages = newMessages;
                                adapter.submitList(messages);

                                if (!messages.isEmpty() && (hasNew || showLoading)) {
                                    recyclerView.smoothScrollToPosition(messages.size() - 1);
                                }
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
                        // 轮询失败静默，避免频繁弹窗
                        if (showLoading) {
                            showError(error);
                        }
                    });
                }
            }, null);
    }

    private void sendMessage() {
        String content = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        
        if (TextUtils.isEmpty(content)) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        // senderId 由服务端从 JWT 取身份（此处保留 receiverId 即可）
        params.put("receiverId", otherUserId);
        params.put("content", content);

        btnSend.setEnabled(false);

        ApiClient.getInstance(this).postForm("/messages", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        
                        try {
                            SendMessageResponse response = gson.fromJson(responseBody, SendMessageResponse.class);
                            // 服务端把消息对象放在 message 键（与通用提示语冲突，实际为消息对象）
                            if (response.success && response.sentMessage != null) {
                                messages.add(response.sentMessage);
                                adapter.submitList(new ArrayList<>(messages));
                                etMessage.setText("");
                                recyclerView.smoothScrollToPosition(messages.size() - 1);
                            } else {
                                showError("发送失败");
                            }
                        } catch (Exception e) {
                            showError("解析数据失败");
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

    // 响应模型
    private static class MessagesResponse {
        boolean success;
        String message;
        @SerializedName("messages")
        List<Message> messages;
    }

    private static class SendMessageResponse {
        boolean success;
        // 服务端 sendMessage 返回 { success, message: {消息对象} }（业务键 message 与提示语冲突，
        // 实际为消息对象），解析为 Message
        @SerializedName("message")
        Message sentMessage;
    }
}
