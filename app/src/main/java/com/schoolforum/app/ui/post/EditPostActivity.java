package com.schoolforum.app.ui.post;

import android.Manifest;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.schoolforum.app.MainActivity;
import com.schoolforum.app.R;
import com.schoolforum.app.model.ApiResponse;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.FileLogger;
import com.schoolforum.app.utils.UserManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 帖子编辑Activity - 发帖和编辑帖子
 */
public class EditPostActivity extends AppCompatActivity {
    private static final String TAG = "EditPostActivity";
    private static final int REQUEST_CODE_PICK_IMAGES = 1001;
    private static final int REQUEST_CODE_PERMISSION = 1002;
    private static final int MAX_IMAGES = 20;

    // UI组件
    private Toolbar toolbar;
    private TextView tvPublish;
    private EditText etContent;
    private TextView tvCharCount;
    private RecyclerView rvImages;
    private LinearLayout layoutAddImage;
    private SwitchCompat switchAnonymous;
    private ProgressBar progressBar;

    // 数据
    private String editPostId; // 编辑帖子时的ID
    private List<Uri> selectedImages = new ArrayList<>();
    private List<String> existingImageUrls = new ArrayList<>(); // 已有图片URL
    private List<String> deletedImageUrls = new ArrayList<>(); // 需要删除的图片URL
    private EditImagesAdapter imagesAdapter;

    // 工具
    private UserManager userManager;
    private ApiClient apiClient;
    private ExecutorService executor;
    private Handler mainHandler;
    private FileLogger logger;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        initUtils();
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        // 检查是否是编辑模式
        editPostId = getIntent().getStringExtra("postId");
        if (editPostId != null) {
            toolbar.setTitle("编辑帖子");
            loadPostData();
        }
    }

    private void initUtils() {
        userManager = UserManager.getInstance(this);
        apiClient = ApiClient.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        logger = FileLogger.getInstance(this);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvPublish = findViewById(R.id.tvPublish);
        etContent = findViewById(R.id.etContent);
        tvCharCount = findViewById(R.id.tvCharCount);
        rvImages = findViewById(R.id.rvImages);
        layoutAddImage = findViewById(R.id.layoutAddImage);
        switchAnonymous = findViewById(R.id.switchAnonymous);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            if (hasContent()) {
                showDiscardDialog();
            } else {
                finish();
            }
        });
    }

    private void setupRecyclerView() {
        imagesAdapter = new EditImagesAdapter(selectedImages, this::removeImage);
        rvImages.setLayoutManager(new GridLayoutManager(this, 3));
        rvImages.setAdapter(imagesAdapter);
    }

    private void setupListeners() {
        // 内容输入监听
        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int count = s.length();
                tvCharCount.setText(count + "/10000");
                updatePublishButton();
            }
        });

        // 发布按钮
        tvPublish.setOnClickListener(v -> publishPost());

        // 添加图片
        layoutAddImage.setOnClickListener(v -> checkPermissionAndPickImages());
    }

    private void updatePublishButton() {
        boolean hasContent = !etContent.getText().toString().trim().isEmpty() || !selectedImages.isEmpty();
        tvPublish.setEnabled(hasContent);
    }

    private boolean hasContent() {
        return !etContent.getText().toString().trim().isEmpty() || !selectedImages.isEmpty();
    }

    private void showDiscardDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("放弃编辑")
            .setMessage("确定要放弃当前编辑的内容吗？")
            .setPositiveButton("放弃", (dialog, which) -> finish())
            .setNegativeButton("取消", null)
            .show();
    }

    private void checkPermissionAndPickImages() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_CODE_PERMISSION);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION);
                return;
            }
        }
        openImagePicker();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        int remaining = MAX_IMAGES - selectedImages.size();
        if (remaining <= 0) {
            Toast.makeText(this, "最多只能添加" + MAX_IMAGES + "张图片", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_CODE_PICK_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGES && resultCode == RESULT_OK && data != null) {
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (selectedImages.size() < MAX_IMAGES) {
                        selectedImages.add(uri);
                    }
                }
            } else if (data.getData() != null) {
                if (selectedImages.size() < MAX_IMAGES) {
                    selectedImages.add(data.getData());
                }
            }
            imagesAdapter.notifyDataSetChanged();
            updateAddImageButton();
            updatePublishButton();
        }
    }

    private void removeImage(int position) {
        selectedImages.remove(position);
        imagesAdapter.notifyDataSetChanged();
        updateAddImageButton();
        updatePublishButton();
    }

    private void updateAddImageButton() {
        layoutAddImage.setVisibility(selectedImages.size() >= MAX_IMAGES ? View.GONE : View.VISIBLE);
    }

    private void loadPostData() {
        // 加载帖子数据进行编辑
        showLoading(true);
        apiClient.get("/posts/" + editPostId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(EditPostActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    Gson gson = new Gson();
                    JsonObject rootObj = gson.fromJson(body, JsonObject.class);
                    
                    if (rootObj != null && rootObj.has("success") && rootObj.get("success").getAsBoolean()) {
                        // 服务端返回格式: { success: true, post: {...} }
                        JsonObject postData = rootObj.has("post") ? rootObj.getAsJsonObject("post") : null;
                        
                        if (postData == null) {
                            mainHandler.post(() -> {
                                showLoading(false);
                                Toast.makeText(EditPostActivity.this, "帖子数据为空", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }
                        
                        // 获取内容
                        final String content = postData.has("content") && !postData.get("content").isJsonNull() 
                                ? postData.get("content").getAsString() : "";
                        
                        // 获取匿名状态
                        final boolean anonymous = postData.has("anonymous") && !postData.get("anonymous").isJsonNull()
                                && postData.get("anonymous").getAsBoolean();
                        
                        mainHandler.post(() -> {
                            etContent.setText(content);
                            switchAnonymous.setChecked(anonymous);
                            
                            // 加载已有图片
                            if (postData.has("images") && !postData.get("images").isJsonNull()) {
                                try {
                                    com.google.gson.JsonArray imagesArray = postData.getAsJsonArray("images");
                                    for (int i = 0; i < imagesArray.size(); i++) {
                                        JsonObject imgObj = imagesArray.get(i).getAsJsonObject();
                                        if (imgObj.has("url")) {
                                            existingImageUrls.add(imgObj.get("url").getAsString());
                                        }
                                    }
                                    // TODO: 显示已有图片
                                } catch (Exception e) {
                                    Log.e(TAG, "加载图片失败: " + e.getMessage());
                                }
                            }
                            
                            showLoading(false);
                        });
                    } else {
                        mainHandler.post(() -> {
                            showLoading(false);
                            String message = rootObj != null && rootObj.has("message") 
                                    ? rootObj.get("message").getAsString() : "加载失败";
                            Toast.makeText(EditPostActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析失败: " + e.getMessage());
                    mainHandler.post(() -> {
                        showLoading(false);
                        Toast.makeText(EditPostActivity.this, "解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void publishPost() {
        String content = etContent.getText().toString().trim();
        boolean anonymous = switchAnonymous.isChecked();

        if (content.isEmpty() && selectedImages.isEmpty()) {
            Toast.makeText(this, "请输入内容或添加图片", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // 获取当前用户信息
        com.schoolforum.app.model.User user = userManager.getCurrentUser();
        if (user == null) {
            showLoading(false);
            Toast.makeText(this, "用户信息无效", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建表单数据
        executor.execute(() -> {
            try {
                // 准备请求参数
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("userId", user.getId());
                params.put("username", user.getUsername());
                params.put("school", user.getSchool() != null ? user.getSchool() : "");
                params.put("grade", user.getGrade() != null ? user.getGrade() : "");
                params.put("className", user.getClassName() != null ? user.getClassName() : "");
                params.put("content", content);
                params.put("anonymous", anonymous ? "true" : "false");
                params.put("deviceInfo", com.schoolforum.app.utils.DeviceUtils.getDeviceInfo());

                // 准备图片文件
                List<File> imageFiles = new ArrayList<>();
                for (Uri uri : selectedImages) {
                    File file = uriToFile(uri);
                    if (file != null) {
                        imageFiles.add(file);
                    }
                }

                // 发送请求 - 编辑使用PUT，新发帖使用POST
                String url = editPostId != null ? "/posts/" + editPostId : "/posts";
                Callback callback = new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> {
                            showLoading(false);
                            Toast.makeText(EditPostActivity.this, "发布失败: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            String body = response.body().string();
                            Gson gson = new Gson();
                            ApiResponse<?> apiResponse = gson.fromJson(body,
                                new TypeToken<ApiResponse<?>>(){}.getType());

                            mainHandler.post(() -> {
                                showLoading(false);
                                if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                                    Toast.makeText(EditPostActivity.this, 
                                        editPostId != null ? "帖子已更新" : "发布成功", 
                                        Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(EditPostActivity.this, 
                                        apiResponse.getMessage() != null ? apiResponse.getMessage() : "发布失败", 
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            mainHandler.post(() -> {
                                showLoading(false);
                                Toast.makeText(EditPostActivity.this, "解析响应失败", 
                                    Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                };
                
                // 根据是否为编辑模式选择正确的HTTP方法
                if (editPostId != null) {
                    apiClient.putMultipart(url, params, imageFiles, "images", callback);
                } else {
                    apiClient.postMultipart(url, params, imageFiles, "images", callback);
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(EditPostActivity.this, "准备数据失败: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private File uriToFile(Uri uri) throws IOException {
        // 使用 ImageUtils 压缩图片
        File compressedFile = com.schoolforum.app.utils.ImageUtils.compressImage(this, uri);
        if (compressedFile != null) {
            Log.d(TAG, "图片压缩成功: " + compressedFile.length() + " bytes");
            return compressedFile;
        }
        
        // 如果压缩失败，使用原始方式复制文件
        Log.w(TAG, "图片压缩失败，使用原始文件");
        ContentResolver resolver = getContentResolver();
        String fileName = "img_" + System.currentTimeMillis() + ".jpg";
        
        File cacheDir = new File(getCacheDir(), "upload");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        File file = new File(cacheDir, fileName);
        
        try (InputStream is = resolver.openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(file)) {
            if (is != null) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
        }
        
        return file;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        tvPublish.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
