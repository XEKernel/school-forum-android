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
 * 修改邮箱页面
 */
public class ChangeEmailActivity extends AppCompatActivity {

    private TextInputEditText etCurrentPassword, etNewEmail, etVerificationCode;
    private android.widget.Button btnSendCode, btnSubmit;
    private CountDownTimer countDownTimer;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);
        initViews();
    }

    private void initViews() {
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewEmail = findViewById(R.id.etNewEmail);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnSubmit = findViewById(R.id.btnSubmit);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> sendCode());
        btnSubmit.setOnClickListener(v -> changeEmail());
    }

    private void sendCode() {
        String email = etNewEmail.getText() != null ? etNewEmail.getText().toString().trim() : "";
        String currentPassword = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString() : "";
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "请输入新邮箱", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, "请输入当前密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 服务端从 JWT 取 userId；发送接口需验证当前密码（与 verifyEmailChange 的 emailChange 场景一致）
        Map<String, String> params = new HashMap<>();
        params.put("newEmail", email);
        params.put("currentPassword", currentPassword);

        btnSendCode.setEnabled(false);
        ApiClient.getInstance(this).postForm("/send-email-change-code", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                Toast.makeText(ChangeEmailActivity.this, "验证码已发送", Toast.LENGTH_SHORT).show();
                                startCountDown();
                            } else {
                                String message = response.has("message") ? response.get("message").getAsString() : "发送失败";
                                Toast.makeText(ChangeEmailActivity.this, message, Toast.LENGTH_SHORT).show();
                                btnSendCode.setEnabled(true);
                            }
                        } catch (Exception e) {
                            btnSendCode.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChangeEmailActivity.this, error, Toast.LENGTH_SHORT).show();
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

    private void changeEmail() {
        String password = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString().trim() : "";
        String email = etNewEmail.getText() != null ? etNewEmail.getText().toString().trim() : "";
        String code = etVerificationCode.getText() != null ? etVerificationCode.getText().toString().trim() : "";

        if (TextUtils.isEmpty(password) || TextUtils.isEmpty(email) || TextUtils.isEmpty(code)) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }

        // 服务端 verify-email-change 从 JWT 取 userId，参数为 newEmail + verificationCode（发送时已验密码）
        Map<String, String> params = new HashMap<>();
        params.put("newEmail", email);
        params.put("verificationCode", code);

        btnSubmit.setEnabled(false);
        ApiClient.getInstance(this).postForm("/verify-email-change", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                Toast.makeText(ChangeEmailActivity.this, "邮箱修改成功", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                String message = response.has("message") ? response.get("message").getAsString() : "修改失败";
                                Toast.makeText(ChangeEmailActivity.this, message, Toast.LENGTH_SHORT).show();
                                btnSubmit.setEnabled(true);
                            }
                        } catch (Exception e) {
                            btnSubmit.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChangeEmailActivity.this, error, Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                    });
                }
            }, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
