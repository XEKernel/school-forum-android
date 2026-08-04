package com.schoolforum.app.ui.login;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.caverock.androidsvg.SVG;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.schoolforum.app.R;
import com.schoolforum.app.model.BaseResponse;
import com.schoolforum.app.model.User;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.FileLogger;
import com.schoolforum.app.utils.UserManager;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 登录Fragment
 */
public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";

    private TextInputEditText etEmail, etQq, etPassword, etCode, etCaptchaCode;
    private View btnSendCode, btnLogin;
    private ImageView ivCaptcha;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private FileLogger logger;
    private final Gson gson = new Gson();

    // 图形验证码（服务端要求发送邮箱验证码前先通过图形验证码防滥用）
    private String captchaId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        logger = FileLogger.getInstance(requireContext());
        log("LoginFragment onViewCreated");
        log("Log file path: " + logger.getLogFilePath());
        
        etEmail = view.findViewById(R.id.etEmail);
        etQq = view.findViewById(R.id.etQq);
        etPassword = view.findViewById(R.id.etPassword);
        etCode = view.findViewById(R.id.etCode);
        etCaptchaCode = view.findViewById(R.id.etCaptchaCode);
        btnSendCode = view.findViewById(R.id.btnSendCode);
        btnLogin = view.findViewById(R.id.btnLogin);
        ivCaptcha = view.findViewById(R.id.ivCaptcha);
        progressBar = getActivity().findViewById(R.id.progressBar);

        btnSendCode.setOnClickListener(v -> sendVerificationCode());
        btnLogin.setOnClickListener(v -> login());

        // 加载图形验证码，点击刷新
        if (ivCaptcha != null) {
            ivCaptcha.setOnClickListener(v -> loadCaptcha());
            loadCaptcha();
        }
    }
    
    private void log(String message) {
        Log.d(TAG, message);
        if (logger != null) {
            logger.d(TAG, message);
        }
    }

    /**
     * 加载图形验证码（GET /captcha，SVG + X-Captcha-Id 头）
     */
    private void loadCaptcha() {
        ApiClient.getInstance(requireContext()).get("/captcha", new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                log("加载图形验证码失败: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws java.io.IOException {
                try {
                    final String svgBody = response.body() != null ? response.body().string() : "";
                    final String newCaptchaId = response.header("X-Captcha-Id");
                    requireActivity().runOnUiThread(() -> {
                        captchaId = newCaptchaId;
                        if (ivCaptcha != null && !TextUtils.isEmpty(svgBody)) {
                            try {
                                SVG svg = SVG.getFromString(svgBody);
                                Bitmap bitmap = Bitmap.createBitmap(220, 88, Bitmap.Config.ARGB_8888);
                                android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                                svg.renderToCanvas(canvas);
                                ivCaptcha.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                            } catch (Exception e) {
                                log("SVG 渲染失败: " + e.getMessage());
                                ivCaptcha.setImageResource(R.drawable.ic_image);
                            }
                        }
                    });
                } catch (Exception e) {
                    log("解析图形验证码失败: " + e.getMessage());
                }
            }
        });
    }


    private void sendVerificationCode() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("请输入有效的邮箱地址");
            return;
        }

        showLoading(true);

        String captchaCode = etCaptchaCode != null && etCaptchaCode.getText() != null
                ? etCaptchaCode.getText().toString().trim() : "";
        if (TextUtils.isEmpty(captchaId) || TextUtils.isEmpty(captchaCode)) {
            showLoading(false);
            Toast.makeText(getContext(), "请输入图形验证码", Toast.LENGTH_SHORT).show();
            loadCaptcha();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("captchaId", captchaId);
        params.put("captchaCode", captchaCode);
        
        ApiClient.getInstance(requireContext()).postForm("/send-login-verification-code", params, 
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        try {
                            BaseResponse response = gson.fromJson(data, BaseResponse.class);
                            if (response.success) {
                                Toast.makeText(getContext(), "验证码已发送", Toast.LENGTH_SHORT).show();
                                startCountDown();
                            } else {
                                Toast.makeText(getContext(), response.message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "发送失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        // 图形验证码一次性，失败后刷新
                        loadCaptcha();
                    });
                }
            }, null);
    }

    private void startCountDown() {
        btnSendCode.setEnabled(false);
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                ((com.google.android.material.button.MaterialButton) btnSendCode)
                    .setText(millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                btnSendCode.setEnabled(true);
                ((com.google.android.material.button.MaterialButton) btnSendCode).setText("获取验证码");
            }
        }.start();
    }

    private void login() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String qq = etQq.getText() != null ? etQq.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String code = etCode.getText() != null ? etCode.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
            return;
        }
        if (TextUtils.isEmpty(qq)) {
            etQq.setError("请输入QQ号");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("请输入密码");
            return;
        }
        if (TextUtils.isEmpty(code)) {
            etCode.setError("请输入验证码");
            return;
        }

        showLoading(true);
        log("Starting login request...");

        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("qq", qq);
        params.put("password", password);
        params.put("verificationCode", code);

        ApiClient.getInstance(requireContext()).postForm("/login", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        // 不记录完整响应体：其中包含 token，会写入本地日志文件泄露
                        log("Login response received, length=" + (responseBody != null ? responseBody.length() : 0));
                        
                        try {
                            LoginResponse response = gson.fromJson(responseBody, LoginResponse.class);
                            log("Parsed: success=" + response.success + ", user=" + (response.user != null ? response.user.getId() : "null"));
                            
                            if (response.success && response.user != null) {
                                User user = response.user;
                                
                                if (user.getId() == null) {
                                    log("ERROR: Invalid user data - no id");
                                    Toast.makeText(getContext(), "登录失败：用户数据无效", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                
                                if (response.isAdmin) {
                                    user.setIsAdmin(true);
                                    log("User is admin");
                                }
                                
                                // 保存用户信息和 Token
                                UserManager.getInstance(requireContext()).saveUserWithTokens(
                                    user,
                                    response.token,
                                    response.refreshToken,
                                    response.adminToken
                                );
                                log("User and tokens saved: hasToken=" + (response.token != null) + 
                                    ", hasRefresh=" + (response.refreshToken != null) + 
                                    ", hasAdmin=" + (response.adminToken != null));
                                
                                boolean saved = UserManager.getInstance(requireContext()).isLoggedIn();
                                log("User saved, isLoggedIn=" + saved);
                                
                                Toast.makeText(getContext(), "登录成功", Toast.LENGTH_SHORT).show();
                                
                                // 新设备登录安全提示（服务端检测到新设备时弹对话框）
                                if (response.isNewDevice && response.device != null) {
                                    showNewDeviceDialog(response.device);
                                } else {
                                    goMainAfterLogin();
                                }
                            } else {
                                // 处理特定错误代码
                                String errorMsg = response.message != null ? response.message : "登录失败";
                                if ("TOKEN_EXPIRED".equals(response.code)) {
                                    errorMsg = "登录已过期，请重新登录";
                                }
                                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            log("Parse error: " + e.getMessage());
                            Toast.makeText(getContext(), "数据解析失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        log("Login ERROR: " + error);
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        log("Log file path: " + FileLogger.getInstance(requireContext()).getLogFilePath());
                        // 图形验证码一次性，失败后刷新
                        loadCaptcha();
                    });
                }
            }, null);
    }

    private static class LoginResponse extends BaseResponse {
        boolean success;
        String message;
        User user;
        boolean isAdmin;
        // JWT Token 字段
        String token;           // 访问令牌
        String refreshToken;    // 刷新令牌
        String adminToken;      // 管理员令牌（仅管理员登录时返回）
        String code;            // 错误代码（如 TOKEN_EXPIRED）
        // 新设备登录检测
        boolean isNewDevice;
        DeviceInfo device;
    }

    /** 新设备登录信息（服务端解析 User-Agent 返回） */
    private static class DeviceInfo {
        String source;
        String browser;
        String os;
        String device;
        String ip;
        String time;
    }

    /** 登录成功跳转主界面 */
    private void goMainAfterLogin() {
        if (getActivity() instanceof LoginActivity) {
            log("Calling onLoginSuccess");
            ((LoginActivity) getActivity()).onLoginSuccess();
        } else {
            log("WARN: Activity is not LoginActivity: " + (getActivity() != null ? getActivity().getClass().getName() : "null"));
        }
    }

    /** 新设备登录安全提示对话框（服务端检测到新设备时展示） */
    private void showNewDeviceDialog(DeviceInfo device) {
        String time = device.time != null ? device.time.replace("T", " ").replace("Z", "").substring(0, Math.min(16, device.time.length())) : "";
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("检测到新设备登录")
                .setMessage("为了您的账号安全，请确认是否为本人操作：\n\n"
                        + "📱 设备：" + (device.device != null ? device.device : "未知") + "\n"
                        + "🖥 系统：" + (device.os != null ? device.os : "未知") + "\n"
                        + "🌐 IP：" + (device.ip != null ? device.ip : "未知") + "\n"
                        + "🕐 时间：" + time + "\n\n"
                        + "如非本人操作，请尽快修改密码保护账号安全")
                .setPositiveButton("我知道了", (dialog, which) -> goMainAfterLogin())
                .setCancelable(false)
                .show();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnLogin.setEnabled(!show);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}