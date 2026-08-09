package com.schoolforum.app.ui.login;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.caverock.androidsvg.SVG;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.schoolforum.app.R;
import com.schoolforum.app.model.BaseResponse;
import com.schoolforum.app.network.ApiClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 忘记密码（找回密码）页面
 * 流程：QQ号 + 注册邮箱 + 图形验证码 → 发送邮箱验证码 → 验证码 + 新密码 → 重置
 * 服务端接口：POST /api/forgot-password/send-code、POST /api/forgot-password/reset
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etQq, etEmail, etCaptchaCode, etCode, etNewPassword, etConfirmPassword;
    private ImageView ivCaptcha;
    private Button btnSendCode, btnReset;
    private ProgressBar progressBar;

    // 图形验证码（服务端要求发送邮箱验证码前先通过图形验证码防滥用）
    private String captchaId;

    private final Gson gson = new Gson();
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();

        // 加载图形验证码，点击刷新
        loadCaptcha();
    }

    private void initViews() {
        etQq = findViewById(R.id.etQq);
        etEmail = findViewById(R.id.etEmail);
        etCaptchaCode = findViewById(R.id.etCaptchaCode);
        etCode = findViewById(R.id.etCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivCaptcha = findViewById(R.id.ivCaptcha);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnReset = findViewById(R.id.btnReset);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        if (ivCaptcha != null) {
            ivCaptcha.setOnClickListener(v -> loadCaptcha());
        }
        btnSendCode.setOnClickListener(v -> sendVerificationCode());
        btnReset.setOnClickListener(v -> resetPassword());
    }

    /**
     * 加载图形验证码（GET /captcha，SVG + X-Captcha-Id 头）
     */
    private void loadCaptcha() {
        ApiClient.getInstance(this).get("/captcha", new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ForgotPasswordActivity.this,
                        "加载图形验证码失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    final String svgBody = response.body() != null ? response.body().string() : "";
                    final String newCaptchaId = response.header("X-Captcha-Id");
                    runOnUiThread(() -> {
                        captchaId = newCaptchaId;
                        if (ivCaptcha != null && !TextUtils.isEmpty(svgBody)) {
                            try {
                                SVG svg = SVG.getFromString(svgBody);
                                Bitmap bitmap = Bitmap.createBitmap(220, 88, Bitmap.Config.ARGB_8888);
                                android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                                svg.renderToCanvas(canvas);
                                ivCaptcha.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                            } catch (Exception ex) {
                                Toast.makeText(ForgotPasswordActivity.this,
                                        "验证码渲染失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        });
    }

    /**
     * 发送邮箱验证码（校验 QQ + 邮箱匹配注册信息 + 图形验证码）
     */
    private void sendVerificationCode() {
        String qq = textOf(etQq);
        String email = textOf(etEmail);
        String captchaCode = textOf(etCaptchaCode);

        if (TextUtils.isEmpty(qq)) {
            etQq.setError("请输入QQ号");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入注册邮箱");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("请输入有效的邮箱地址");
            return;
        }
        if (TextUtils.isEmpty(captchaId) || TextUtils.isEmpty(captchaCode)) {
            Toast.makeText(this, "请输入图形验证码", Toast.LENGTH_SHORT).show();
            loadCaptcha();
            return;
        }

        setLoading(true);

        Map<String, String> params = new HashMap<>();
        params.put("qq", qq);
        params.put("email", email);
        params.put("captchaId", captchaId);
        params.put("captchaCode", captchaCode);

        ApiClient.getInstance(this).postForm("/forgot-password/send-code", params,
                new ApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String responseBody) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            try {
                                BaseResponse response = gson.fromJson(responseBody, BaseResponse.class);
                                if (response != null && response.success) {
                                    Toast.makeText(ForgotPasswordActivity.this,
                                            "验证码已发送至邮箱", Toast.LENGTH_SHORT).show();
                                    startCountDown();
                                } else {
                                    Toast.makeText(ForgotPasswordActivity.this,
                                            response != null ? response.message : "发送失败，请稍后再试",
                                            Toast.LENGTH_SHORT).show();
                                    loadCaptcha();
                                }
                            } catch (Exception e) {
                                Toast.makeText(ForgotPasswordActivity.this,
                                        "发送失败，请稍后再试", Toast.LENGTH_SHORT).show();
                                loadCaptcha();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "网络错误：" + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }, null);
    }

    /**
     * 重置密码
     */
    private void resetPassword() {
        String email = textOf(etEmail);
        String code = textOf(etCode);
        String newPassword = textOf(etNewPassword);
        String confirmPassword = textOf(etConfirmPassword);

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入注册邮箱");
            return;
        }
        if (TextUtils.isEmpty(code)) {
            etCode.setError("请输入邮箱验证码");
            return;
        }
        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("请输入新密码");
            return;
        }
        if (newPassword.length() < 8) {
            etNewPassword.setError("密码至少8位");
            return;
        }
        if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
            etNewPassword.setError("密码需同时包含字母和数字");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("两次输入的密码不一致");
            return;
        }

        setLoading(true);

        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("verificationCode", code);
        params.put("newPassword", newPassword);

        ApiClient.getInstance(this).postForm("/forgot-password/reset", params,
                new ApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String responseBody) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            try {
                                BaseResponse response = gson.fromJson(responseBody, BaseResponse.class);
                                if (response != null && response.success) {
                                    Toast.makeText(ForgotPasswordActivity.this,
                                            "密码重置成功，请使用新密码登录", Toast.LENGTH_SHORT).show();
                                    // 销毁倒计时，延迟返回登录页
                                    if (countDownTimer != null) countDownTimer.cancel();
                                    btnSendCode.postDelayed(ForgotPasswordActivity.this::finish, 1500);
                                } else {
                                    Toast.makeText(ForgotPasswordActivity.this,
                                            response != null ? response.message : "重置失败，请稍后再试",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(ForgotPasswordActivity.this,
                                        "重置失败，请稍后再试", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "网络错误：" + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }, null);
    }

    private void startCountDown() {
        btnSendCode.setEnabled(false);
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnSendCode.setText(millisUntilFinished / 1000 + "s 后重发");
            }

            @Override
            public void onFinish() {
                btnSendCode.setEnabled(true);
                btnSendCode.setText("发送邮箱验证码");
            }
        }.start();
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        btnSendCode.setEnabled(!loading);
        btnReset.setEnabled(!loading);
    }

    private String textOf(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
