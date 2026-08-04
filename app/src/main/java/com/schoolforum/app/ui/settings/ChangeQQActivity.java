package com.schoolforum.app.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.schoolforum.app.R;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.UserManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 修改QQ号页面
 */
public class ChangeQQActivity extends AppCompatActivity {

    private TextInputEditText etQQ;
    private android.widget.Button btnSubmit;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_qq);

        initViews();
        loadCurrentQQ();
    }

    private void initViews() {
        etQQ = findViewById(R.id.etQQ);
        btnSubmit = findViewById(R.id.btnSubmit);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> changeQQ());
    }

    private void loadCurrentQQ() {
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
                                etQQ.setText(qq);
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

    private void changeQQ() {
        String qq = etQQ.getText() != null ? etQQ.getText().toString().trim() : "";

        if (TextUtils.isEmpty(qq)) {
            Toast.makeText(this, "请输入QQ号", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qq.length() < 5 || qq.length() > 12) {
            Toast.makeText(this, "QQ号应为5-12位数字", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("newQQ", qq);

        btnSubmit.setEnabled(false);
        ApiClient.getInstance(this).postForm("/change-qq", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                Toast.makeText(ChangeQQActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                String message = response.has("message") ? response.get("message").getAsString() : "修改失败";
                                Toast.makeText(ChangeQQActivity.this, message, Toast.LENGTH_SHORT).show();
                                btnSubmit.setEnabled(true);
                            }
                        } catch (Exception e) {
                            Toast.makeText(ChangeQQActivity.this, "修改失败", Toast.LENGTH_SHORT).show();
                            btnSubmit.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChangeQQActivity.this, error, Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                    });
                }
            }, null);
    }
}
