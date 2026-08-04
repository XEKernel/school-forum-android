package com.schoolforum.app.ui.settings;

import android.os.Bundle;
import android.os.CountDownTimer;
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
 * 修改密码页面
 */
public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword, etVerificationCode;
    private android.widget.Button btnSendCode, btnSubmit;

    private final Gson gson = new Gson();
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        initViews();
    }

    private void initViews() {
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnSubmit = findViewById(R.id.btnSubmit);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> sendVerificationCode());
        btnSubmit.setOnClickListener(v -> changePassword());
    }

    private void sendVerificationCode() {
        String currentPassword = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString().trim() : "";
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, "请输入当前密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 服务端从 JWT 取 userId；发送验证码需验证当前密码
        Map<String, String> params = new HashMap<>();
        params.put("currentPassword", currentPassword);

        btnSendCode.setEnabled(false);
        ApiClient.getInstance(this).postForm("/send-password-change-code", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                Toast.makeText(ChangePasswordActivity.this, "验证码已发送", Toast.LENGTH_SHORT).show();
                                startCountDown();
                            } else {
                                String message = response.has("message") ? response.get("message").getAsString() : "发送失败";
                                Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                                btnSendCode.setEnabled(true);
                            }
                        } catch (Exception e) {
                            Toast.makeText(ChangePasswordActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                            btnSendCode.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChangePasswordActivity.this, error, Toast.LENGTH_SHORT).show();
                        btnSendCode.setEnabled(true);
                    });
                }
            }, null);
    }

    private void startCountDown() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnSendCode.setText((millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                btnSendCode.setText("发送验证码");
                btnSendCode.setEnabled(true);
            }
        }.start();
    }

    private void changePassword() {
        String currentPassword = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString().trim() : "";
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
        String code = etVerificationCode.getText() != null ? etVerificationCode.getText().toString().trim() : "";

        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, "请输入当前密码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 服务端从 JWT 取 userId；字段为 verificationCode（密码修改场景）
        Map<String, String> params = new HashMap<>();
        params.put("currentPassword", currentPassword);
        params.put("newPassword", newPassword);
        params.put("verificationCode", code);

        btnSubmit.setEnabled(false);
        ApiClient.getInstance(this).postForm("/change-password", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                Toast.makeText(ChangePasswordActivity.this, "密码修改成功", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                String message = response.has("message") ? response.get("message").getAsString() : "修改失败";
                                Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                                btnSubmit.setEnabled(true);
                            }
                        } catch (Exception e) {
                            Toast.makeText(ChangePasswordActivity.this, "修改失败", Toast.LENGTH_SHORT).show();
                            btnSubmit.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChangePasswordActivity.this, error, Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                    });
                }
            }, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
