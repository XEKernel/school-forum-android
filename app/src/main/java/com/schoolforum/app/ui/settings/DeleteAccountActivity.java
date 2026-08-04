package com.schoolforum.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.schoolforum.app.R;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.ui.login.LoginActivity;
import com.schoolforum.app.utils.UserManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 注销账号页面
 */
public class DeleteAccountActivity extends AppCompatActivity {

    private TextInputEditText etPassword, etCode;
    private View layoutCode, btnSendCode, btnSubmit;
    private MaterialCheckBox cbKeepData, cbConfirm;
    private CountDownTimer countDownTimer;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);
        initViews();
    }

    private void initViews() {
        etPassword = findViewById(R.id.etPassword);
        etCode = findViewById(R.id.etCode);
        layoutCode = findViewById(R.id.layoutCode);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnSubmit = findViewById(R.id.btnSubmit);
        cbKeepData = findViewById(R.id.cbKeepData);
        cbConfirm = findViewById(R.id.cbConfirm);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> sendCode());
        btnSubmit.setOnClickListener(v -> deleteAccount());
    }

    private void sendCode() {
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 服务端从 JWT 取 userId（/send-deletion-code 无 user 前缀）
        Map<String, String> params = new HashMap<>();
        params.put("password", password);

        btnSendCode.setEnabled(false);
        ApiClient.getInstance(this).postForm("/send-deletion-code", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                Toast.makeText(DeleteAccountActivity.this, "验证码已发送到您的邮箱", Toast.LENGTH_SHORT).show();
                                layoutCode.setVisibility(View.VISIBLE);
                                startCountDown();
                            } else {
                                String message = response.has("message") ? response.get("message").getAsString() : "发送失败";
                                Toast.makeText(DeleteAccountActivity.this, message, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(DeleteAccountActivity.this, error, Toast.LENGTH_SHORT).show();
                        btnSendCode.setEnabled(true);
                    });
                }
            }, null);
    }

    private void startCountDown() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                ((android.widget.Button) btnSendCode).setText((millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                ((android.widget.Button) btnSendCode).setText("发送验证码");
                btnSendCode.setEnabled(true);
            }
        }.start();
    }

    private void deleteAccount() {
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String code = etCode.getText() != null ? etCode.getText().toString().trim() : "";

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!cbConfirm.isChecked()) {
            Toast.makeText(this, "请确认注销账号", Toast.LENGTH_SHORT).show();
            return;
        }

        // 服务端从 JWT 取 userId；参数为 password + verificationCode + keepData
        Map<String, String> params = new HashMap<>();
        params.put("password", password);
        params.put("verificationCode", code);
        params.put("keepData", String.valueOf(cbKeepData.isChecked()));

        btnSubmit.setEnabled(false);
        ApiClient.getInstance(this).postForm("/delete-account", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject response = gson.fromJson(data, JsonObject.class);
                            if (response.has("success") && response.get("success").getAsBoolean()) {
                                Toast.makeText(DeleteAccountActivity.this, "账号已注销", Toast.LENGTH_SHORT).show();
                                UserManager.getInstance(DeleteAccountActivity.this).clearUser();
                                startActivity(new Intent(DeleteAccountActivity.this, LoginActivity.class));
                                finishAffinity();
                            } else {
                                String message = response.has("message") ? response.get("message").getAsString() : "注销失败";
                                Toast.makeText(DeleteAccountActivity.this, message, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(DeleteAccountActivity.this, error, Toast.LENGTH_SHORT).show();
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
