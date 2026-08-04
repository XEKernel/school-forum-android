package com.schoolforum.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.schoolforum.app.MainActivity;
import com.schoolforum.app.R;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.ui.login.LoginActivity;
import com.schoolforum.app.utils.UserManager;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 设置页面（简化版）
 */
public class SettingsActivity extends AppCompatActivity {

    private CircleImageView ivAvatar;
    private TextView tvUsername, tvSignature, tvQQ, tvEmail;
    private TextView tvVisibilityGender, tvVisibilityBirthday, tvVisibilitySchool;
    private TextView tvThemeValue;
    private SwitchMaterial switchNotifyLike, switchNotifyComment, switchNotifyFollow, switchNotifySystem;

    private final Gson gson = new Gson();
    // 标记是否正在加载设置数据，防止开关监听器在设置初始值时误触发保存请求
    private boolean isLoadingSettings = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        loadUserInfo();
        loadNotificationSettings();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvSignature = findViewById(R.id.tvSignature);
        tvQQ = findViewById(R.id.tvQQ);
        tvEmail = findViewById(R.id.tvEmail);
        tvVisibilityGender = findViewById(R.id.tvVisibilityGenderValue);
        tvVisibilityBirthday = findViewById(R.id.tvVisibilityBirthdayValue);
        tvVisibilitySchool = findViewById(R.id.tvVisibilitySchoolValue);
        tvThemeValue = findViewById(R.id.tvThemeValue);
        switchNotifyLike = findViewById(R.id.switchNotifyLike);
        switchNotifyComment = findViewById(R.id.switchNotifyComment);
        switchNotifyFollow = findViewById(R.id.switchNotifyFollow);
        switchNotifySystem = findViewById(R.id.switchNotifySystem);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 个人信息入口
        findViewById(R.id.cardProfile).setOnClickListener(v ->
            startActivity(new Intent(this, EditProfileActivity.class)));

        // 账户安全入口
        findViewById(R.id.cardAccountSecurity).setOnClickListener(v ->
            startActivity(new Intent(this, AccountSecurityActivity.class)));

        // 隐私设置
        findViewById(R.id.itemVisibilityGender).setOnClickListener(v -> showVisibilityDialog("gender", tvVisibilityGender));
        findViewById(R.id.itemVisibilityBirthday).setOnClickListener(v -> showVisibilityDialog("birthday", tvVisibilityBirthday));
        findViewById(R.id.itemVisibilitySchool).setOnClickListener(v -> showVisibilityDialog("school", tvVisibilitySchool));

        // 通知设置
        switchNotifyLike.setOnCheckedChangeListener((btn, checked) -> updateNotificationSetting("like", checked));
        switchNotifyComment.setOnCheckedChangeListener((btn, checked) -> updateNotificationSetting("comment", checked));
        switchNotifyFollow.setOnCheckedChangeListener((btn, checked) -> updateNotificationSetting("follow", checked));
        switchNotifySystem.setOnCheckedChangeListener((btn, checked) -> updateNotificationSetting("system", checked));

        // 主题设置
        findViewById(R.id.itemTheme).setOnClickListener(v -> showThemeDialog());

        // 关于
        findViewById(R.id.itemAbout).setOnClickListener(v -> showAboutDialog());

        // 退出登录
        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
    }

    private void loadUserInfo() {
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) return;

        ApiClient.getInstance(this).get("/user/profile/" + userId, null,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                JsonObject user = response.getAsJsonObject("user");

                                // 头像
                                String avatar = user.has("avatar") && !user.get("avatar").isJsonNull() ? user.get("avatar").getAsString() : null;
                                if (avatar != null && !avatar.isEmpty()) {
                                    String url = avatar.startsWith("http") ? avatar : MainActivity.getBaseUrl() + avatar;
                                    Glide.with(SettingsActivity.this).load(url).into(ivAvatar);
                                }

                                // 用户名和签名
                                tvUsername.setText(user.has("username") ? user.get("username").getAsString() : "");
                                tvSignature.setText(user.has("signature") && !user.get("signature").isJsonNull() ? user.get("signature").getAsString() : "这个人很懒，什么都没写");

                                // QQ和邮箱
                                tvQQ.setText(user.has("qq") ? user.get("qq").getAsString() : "");
                                tvEmail.setText(user.has("email") ? user.get("email").getAsString() : "");

                                // 隐私设置
                                loadPrivacySettings(user);
                            }
                        } catch (Exception e) {
                            Toast.makeText(SettingsActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(SettingsActivity.this, error, Toast.LENGTH_SHORT).show());
                }
            }, null);
    }

    private void loadPrivacySettings(JsonObject user) {
        if (user.has("privacySettings")) {
            JsonObject privacy = user.getAsJsonObject("privacySettings");
            tvVisibilityGender.setText(getVisibilityText(privacy.has("gender") ? privacy.get("gender").getAsString() : "public"));
            tvVisibilityBirthday.setText(getVisibilityText(privacy.has("birthday") ? privacy.get("birthday").getAsString() : "public"));
            tvVisibilitySchool.setText(getVisibilityText(privacy.has("school") ? privacy.get("school").getAsString() : "public"));
        }
    }

    private String getVisibilityText(String visibility) {
        if ("public".equals(visibility)) return "公开";
        if ("friends".equals(visibility)) return "仅好友";
        if ("private".equals(visibility)) return "私密";
        return "公开";
    }

    private void loadNotificationSettings() {
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) return;
        
        isLoadingSettings = true;
        ApiClient.getInstance(this).get("/user/notification-settings/" + userId, null,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                JsonObject settings = response.getAsJsonObject("settings");
                                switchNotifyLike.setChecked(settings.has("like") && settings.get("like").getAsBoolean());
                                switchNotifyComment.setChecked(settings.has("comment") && settings.get("comment").getAsBoolean());
                                switchNotifyFollow.setChecked(settings.has("follow") && settings.get("follow").getAsBoolean());
                                switchNotifySystem.setChecked(!settings.has("system") || settings.get("system").getAsBoolean());
                            }
                        } catch (Exception e) {
                            // 忽略
                        } finally {
                            isLoadingSettings = false;
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    isLoadingSettings = false;
                }
            }, null);
    }

    private void updateNotificationSetting(String type, boolean enabled) {
        // 如果正在加载数据，不发送请求
        if (isLoadingSettings) return;
        
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) return;

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("type", type);
        params.put("enabled", String.valueOf(enabled));

        ApiClient.getInstance(this).postForm("/user/notification-settings", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    // 成功
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "保存失败", Toast.LENGTH_SHORT).show());
                }
            }, null);
    }

    private void showVisibilityDialog(String field, TextView tvValue) {
        String[] options = {"公开", "仅好友", "私密"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择可见范围")
            .setItems(options, (dialog, which) -> {
                String value = which == 0 ? "public" : which == 1 ? "friends" : "private";
                tvValue.setText(options[which]);
                updatePrivacySetting(field, value);
            })
            .show();
    }

    private void updatePrivacySetting(String field, String value) {
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) return;

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("field", field);
        params.put("value", value);

        ApiClient.getInstance(this).postForm("/user/privacy-settings", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    // 成功
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "保存失败", Toast.LENGTH_SHORT).show());
                }
            }, null);
    }

    private void showThemeDialog() {
        String[] options = {"跟随系统", "浅色模式", "深色模式"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择主题")
            .setItems(options, (dialog, which) -> {
                tvThemeValue.setText(options[which]);
                // TODO: 实际切换主题
            })
            .show();
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage("校园论坛 v1.0.0\n\n一个面向校园用户的交流平台")
            .setPositiveButton("确定", null)
            .show();
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("退出", (dialog, which) -> logout())
            .setNegativeButton("取消", null)
            .show();
    }

    private void logout() {
        UserManager.getInstance(this).logout(new UserManager.LogoutCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                    finishAffinity();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
    }
}