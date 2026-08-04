package com.schoolforum.app.ui.preview;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.schoolforum.app.MainActivity;
import com.schoolforum.app.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片预览Activity - 支持缩放、滑动、保存
 */
public class ImagePreviewActivity extends AppCompatActivity {
    private static final String TAG = "ImagePreviewActivity";
    private static final int REQUEST_WRITE_STORAGE = 1001;

    // UI组件
    private ViewPager2 viewPager;
    private TextView tvIndicator;
    private ImageView ivBack;
    private ImageView ivSave;
    private ProgressBar progressBar;

    // 数据
    private List<String> imageUrls;
    private int currentPosition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_image_preview);

            // 获取数据
            imageUrls = getIntent().getStringArrayListExtra("images");
            currentPosition = getIntent().getIntExtra("position", 0);

            if (imageUrls == null || imageUrls.isEmpty()) {
                Toast.makeText(this, "没有图片", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // 确保位置有效
            if (currentPosition < 0 || currentPosition >= imageUrls.size()) {
                currentPosition = 0;
            }

            initViews();
            setupViewPager();
            updateIndicator(currentPosition);
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: " + e.getMessage(), e);
            Toast.makeText(this, "打开图片失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tvIndicator = findViewById(R.id.tvIndicator);
        ivBack = findViewById(R.id.ivBack);
        ivSave = findViewById(R.id.ivSave);
        progressBar = findViewById(R.id.progressBar);

        ivBack.setOnClickListener(v -> finish());
        ivSave.setOnClickListener(v -> saveCurrentImage());
    }

    private void setupViewPager() {
        ImagePreviewAdapter adapter = new ImagePreviewAdapter(imageUrls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position);
                currentPosition = position;
            }
        });
    }

    private void updateIndicator(int position) {
        tvIndicator.setText((position + 1) + " / " + imageUrls.size());
    }

    private void saveCurrentImage() {
        if (imageUrls == null || currentPosition >= imageUrls.size()) return;

        String url = imageUrls.get(currentPosition);
        if (!url.startsWith("http")) {
            url = MainActivity.getBaseUrl() + url;
        }

        // 检查权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_STORAGE);
                return;
            }
        }

        downloadImage(url);
    }

    private void downloadImage(String url) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) return;

            Uri uri = Uri.parse(url);
            String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle(fileName);
            request.setDescription("正在保存图片...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "SchoolForum/" + fileName);

            downloadManager.enqueue(request);
            Toast.makeText(this, "开始保存图片", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Save image error: " + e.getMessage());
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveCurrentImage();
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 图片预览适配器
     */
    private class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {
        private final List<String> urls;

        ImagePreviewAdapter(List<String> urls) {
            this.urls = urls;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position < 0 || position >= urls.size()) {
                return;
            }
            
            String url = urls.get(position);
            if (url == null || url.isEmpty()) {
                holder.progressBar.setVisibility(View.GONE);
                return;
            }
            
            if (!url.startsWith("http")) {
                url = MainActivity.getBaseUrl() + url;
            }

            holder.progressBar.setVisibility(View.VISIBLE);
            
            final String finalUrl = url;
            try {
                Glide.with(ImagePreviewActivity.this)
                        .load(finalUrl)
                        .error(R.mipmap.ic_launcher)
                        .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                         Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                holder.progressBar.setVisibility(View.GONE);
                                Log.e(TAG, "Image load failed: " + finalUrl + ", error: " + (e != null ? e.getMessage() : "unknown"));
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                                                            Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                holder.progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(holder.photoView);
            } catch (Exception e) {
                Log.e(TAG, "Glide load error: " + e.getMessage());
                holder.progressBar.setVisibility(View.GONE);
            }

            // 点击退出
            holder.photoView.setOnClickListener(v -> {
                if (!isFinishing() && !isDestroyed()) {
                    finish();
                }
            });
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            com.github.chrisbanes.photoview.PhotoView photoView;
            ProgressBar progressBar;

            ViewHolder(View itemView) {
                super(itemView);
                photoView = itemView.findViewById(R.id.photoView);
                progressBar = itemView.findViewById(R.id.progressBar);
            }
        }
    }
}
