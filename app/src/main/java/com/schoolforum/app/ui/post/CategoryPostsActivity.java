package com.schoolforum.app.ui.post;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.schoolforum.app.R;

/**
 * 栏目帖子浏览页（对应 Web 端 category.html）
 * 通过 PostsFragment 展示指定栏目的帖子流
 */
public class CategoryPostsActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_posts);

        String categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 标题
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(categoryName != null ? categoryName : "栏目");

        // 加载帖子流（传 categoryId 过滤）
        if (savedInstanceState == null && categoryId != null) {
            PostsFragment fragment = new PostsFragment();
            Bundle args = new Bundle();
            args.putString("categoryId", categoryId);
            fragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, fragment);
            transaction.commit();
        }
    }
}
