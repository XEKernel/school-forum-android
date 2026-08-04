package com.schoolforum.app.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.schoolforum.app.MainActivity;
import com.schoolforum.app.R;
import com.schoolforum.app.utils.FileLogger;
import com.schoolforum.app.utils.UserManager;

/**
 * 登录Activity
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private TextView tvSkip;
    private FileLogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        logger = FileLogger.getInstance(this);
        
        // 已登录则跳过
        boolean isLoggedIn = UserManager.getInstance(this).isLoggedIn();
        log("onCreate: isLoggedIn=" + isLoggedIn);
        if (isLoggedIn) {
            navigateToMain();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_login);

        initViews();
        setupViewPager();

        // 启动时检查服务器连通性（未登录用户同样需要提示）
        checkServerConnectivity();
    }

    /**
     * 启动时检查服务器连通性（后台线程，5 秒短超时）
     * 失败时以 Snackbar 非阻塞提示，避免打断登录操作
     */
    private void checkServerConnectivity() {
        com.schoolforum.app.utils.ServerConnectivityChecker.check(
                com.schoolforum.app.BuildConfig.BASE_URL,
                (reachable, error) -> {
                    if (isFinishing() || isDestroyed() || reachable) {
                        return;
                    }
                    com.google.android.material.snackbar.Snackbar.make(
                            findViewById(android.R.id.content),
                            "无法连接服务器，请检查网络或服务器状态",
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                            .setAction("重试", v -> checkServerConnectivity())
                            .show();
                });
    }
    
    private void log(String message) {
        Log.d(TAG, message);
        if (logger != null) {
            logger.d(TAG, message);
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        progressBar = findViewById(R.id.progressBar);
        tvSkip = findViewById(R.id.tvSkip);

        tvSkip.setOnClickListener(v -> {
            navigateToMain();
        });
    }

    private void setupViewPager() {
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return position == 0 ? new LoginFragment() : new RegisterFragment();
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        };

        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "登录" : "注册");
        }).attach();
    }

    /**
     * 登录成功回调
     */
    public void onLoginSuccess() {
        log("onLoginSuccess called");
        navigateToMain();
    }

    private void navigateToMain() {
        log("navigateToMain: starting MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        // 复用已有的 MainActivity 实例（游客模式时栈中可能已存在），避免叠加
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}