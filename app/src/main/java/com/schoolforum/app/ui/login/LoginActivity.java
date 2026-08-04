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
        startActivity(intent);
        finish();
    }
}