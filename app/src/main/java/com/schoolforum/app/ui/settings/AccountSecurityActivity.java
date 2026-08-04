package com.schoolforum.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.schoolforum.app.R;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.UserManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 账户安全页面
 */
public class AccountSecurityActivity extends AppCompatActivity {

    private TextView tvQQ, tvEmail;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_security);

        initViews();
        loadUserInfo();
    }

    private void initViews() {
        tvQQ = findViewById(R.id.tvQQ);
        tvEmail = findViewById(R.id.tvEmail);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.itemPassword).setOnClickListener(v -> 
            startActivity(new Intent(this, ChangePasswordActivity.class)));
        findViewById(R.id.itemEmail).setOnClickListener(v -> 
            startActivity(new Intent(this, ChangeEmailActivity.class)));
        findViewById(R.id.itemQQ).setOnClickListener(v -> 
            startActivity(new Intent(this, ChangeQQActivity.class)));
        findViewById(R.id.itemDeleteAccount).setOnClickListener(v -> 
            startActivity(new Intent(this, DeleteAccountActivity.class)));
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
                                String qq = user.has("qq") ? user.get("qq").getAsString() : "";
                                String email = user.has("email") ? user.get("email").getAsString() : "";
                                tvQQ.setText(qq);
                                tvEmail.setText(email);
                            }
                        } catch (Exception e) {
                            Toast.makeText(AccountSecurityActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(AccountSecurityActivity.this, error, Toast.LENGTH_SHORT).show());
                }
            }, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
    }
}
