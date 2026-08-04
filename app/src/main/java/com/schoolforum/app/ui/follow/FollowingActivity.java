package com.schoolforum.app.ui.follow;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.schoolforum.app.R;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.UserManager;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 动态页面 - 显示关注用户的帖子和关注列表
 */
public class FollowingActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private UserManager userManager;
    private ApiClient apiClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);

        userManager = UserManager.getInstance(this);
        apiClient = ApiClient.getInstance(this);

        initViews();
        setupViewPager();
        markFollowingViewed();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "动态" : "列表");
        }).attach();
    }

    /**
     * 标记已查看动态
     */
    private void markFollowingViewed() {
        String userId = userManager.getCurrentUserId();
        if (userId == null) return;

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        apiClient.postForm("/follow/mark-viewed", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    // 标记成功
                }

                @Override
                public void onError(String error) {
                    // 忽略错误
                }
            }, null);
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new FollowingPostsFragment();
            } else {
                return new FollowingListFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
