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
    private final List<EditImagesAdapter.EditImageItem> imageItems = new ArrayList<>();
    private final List<String> deletedImageUrls = new ArrayList<>(); // 需要删除的已有图片URL
    private EditImagesAdapter imagesAdapter;
    private String visibility = "public"; // 帖子可见性（public/followers/self）
    private String selectedCategoryId; // 发布栏目
    private final List<CategoryInfo> categoryList = new ArrayList<>();

    /** 栏目信息（服务端 GET /categories 返回） */
    private static class CategoryInfo {
        String id;
        String name;
    }

    // 工具
    private UserManager userManager;
    private ApiClient apiClient;
    private ExecutorService executor;
    private Handler mainHandler;
    private FileLogger logger;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        // 游客拦截：发帖/编辑需要登录
        if (!UserManager.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, "请先登录后发帖", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, com.schoolforum.app.ui.login.LoginActivity.class));
            finish();
            return;
        }

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

    private void log(String message) {
        Log.d(TAG, message);
        if (logger != null) {
            logger.d(TAG, message);
        }
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
        setupVisibilitySpinner();
        setupCategorySpinner();
        loadCategories();
    }

    /**
     * 初始化栏目选择器（"不选择" + 服务端栏目）
     */
    private void setupCategorySpinner() {
        android.widget.Spinner spinner = findViewById(R.id.spinnerCategory);
        if (spinner == null) return;
        List<String> names = new ArrayList<>();
        names.add("不选择");
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0 && position <= categoryList.size()) {
                    selectedCategoryId = categoryList.get(position - 1).id;
                } else {
                    selectedCategoryId = null;
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedCategoryId = null;
            }
        });
    }

    /**
     * 加载服务端栏目列表并填充 Spinner
     */
    private void loadCategories() {
        apiClient.get("/categories", new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log("加载栏目列表失败: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    final String body = response.body() != null ? response.body().string() : "";
                    mainHandler.post(() -> {
                        try {
                            JsonObject root = gson.fromJson(body, JsonObject.class);
                            if (root != null && root.has("success") && root.get("success").getAsBoolean()
                                    && root.has("categories")) {
                                List<String> names = new ArrayList<>();
                                names.add("不选择");
                                categoryList.clear();
                                com.google.gson.JsonArray arr = root.getAsJsonArray("categories");
                                for (int i = 0; i < arr.size(); i++) {
                                    JsonObject c = arr.get(i).getAsJsonObject();
                                    if (c.has("id") && c.has("name")) {
                                        CategoryInfo info = new CategoryInfo();
                                        info.id = c.get("id").getAsString();
                                        info.name = c.get("name").getAsString();
                                        categoryList.add(info);
                                        names.add(info.name);
                                    }
                                }
                                android.widget.Spinner spinner = findViewById(R.id.spinnerCategory);
                                if (spinner != null) {
                                    android.widget.ArrayAdapter<String> adapter =
                                            new android.widget.ArrayAdapter<>(EditPostActivity.this,
                                                    android.R.layout.simple_spinner_item, names);
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    spinner.setAdapter(adapter);
                                    // 编辑模式回填
                                    if (selectedCategoryId != null) {
                                        for (int i = 0; i < categoryList.size(); i++) {
                                            if (selectedCategoryId.equals(categoryList.get(i).id)) {
                                                spinner.setSelection(i + 1);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log("解析栏目列表失败: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    log("读取栏目列表失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 编辑模式：设置栏目选中项
     */
    private void setCategorySelection(String categoryId) {
        this.selectedCategoryId = categoryId;
    }

    /**
     * 初始化可见性选择器
     */
    private void setupVisibilitySpinner() {
        android.widget.Spinner spinner = findViewById(R.id.spinnerVisibility);
        if (spinner == null) return;
        String[] items = {"公开", "仅粉丝", "仅自己"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                visibility = position == 1 ? "followers" : (position == 2 ? "self" : "public");
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                visibility = "public";
            }
        });
    }

    /**
     * 编辑模式：根据帖子可见性回填选择器
     */
    private void setVisibilitySelection(String postVisibility) {
        android.widget.Spinner spinner = findViewById(R.id.spinnerVisibility);
        if (spinner == null) return;
        int pos = "self".equals(postVisibility) ? 2 : ("followers".equals(postVisibility) ? 1 : 0);
        spinner.setSelection(pos);
        visibility = postVisibility;
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
        imagesAdapter = new EditImagesAdapter(imageItems, this::removeImage);
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
        boolean hasContent = !etContent.getText().toString().trim().isEmpty() || !imageItems.isEmpty();
        tvPublish.setEnabled(hasContent);
    }

    private boolean hasContent() {
        return !etContent.getText().toString().trim().isEmpty() || !imageItems.isEmpty();
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
        int remaining = MAX_IMAGES - imageItems.size();
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
            int rejected = 0;
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (!isAllowedImage(uri)) {
                        rejected++;
                        continue;
                    }
                    if (imageItems.size() < MAX_IMAGES) {
                        imageItems.add(new EditImagesAdapter.EditImageItem(uri));
                    }
                }
            } else if (data.getData() != null) {
                if (isAllowedImage(data.getData())) {
                    if (imageItems.size() < MAX_IMAGES) {
                        imageItems.add(new EditImagesAdapter.EditImageItem(data.getData()));
                    }
                } else {
                    rejected++;
                }
            }
            imagesAdapter.notifyDataSetChanged();
            updateAddImageButton();
            updatePublishButton();
            if (rejected > 0) {
                Toast.makeText(this, rejected + " 张图片格式不支持（仅支持 JPG/PNG/GIF/WebP）", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 图片格式白名单校验（与服务端 upload 白名单一致，提前拦截 bmp/heic/svg 等）
     */
    private boolean isAllowedImage(Uri uri) {
        if (uri == null) return false;
        String mime = null;
        try {
            mime = getContentResolver().getType(uri);
        } catch (Exception ignored) {
        }
        if (mime != null) {
            return mime.equals("image/jpeg") || mime.equals("image/jpg")
                    || mime.equals("image/png") || mime.equals("image/gif")
                    || mime.equals("image/webp");
        }
        // MIME 获取失败时按文件名后缀兜底
        String name = null;
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {
        }
        if (name != null) {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".png") || lower.endsWith(".gif")
                    || lower.endsWith(".webp");
        }
        return false;
    }

    private void removeImage(int position) {
        if (position < 0 || position >= imageItems.size()) return;
        EditImagesAdapter.EditImageItem item = imageItems.get(position);
        // 删除已有图片时记录 URL，提交时告知服务端删除
        if (item.isExisting() && item.url != null && !deletedImageUrls.contains(item.url)) {
            deletedImageUrls.add(item.url);
        }
        imageItems.remove(position);
        imagesAdapter.notifyDataSetChanged();
        updateAddImageButton();
        updatePublishButton();
    }

    private void updateAddImageButton() {
        layoutAddImage.setVisibility(imageItems.size() >= MAX_IMAGES ? View.GONE : View.VISIBLE);
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

                        // 获取可见性
                        final String postVisibility = postData.has("visibility") && !postData.get("visibility").isJsonNull()
                                ? postData.get("visibility").getAsString() : "public";

                        // 获取栏目
                        final String postCategoryId = postData.has("categoryId") && !postData.get("categoryId").isJsonNull()
                                ? postData.get("categoryId").getAsString() : null;

                        mainHandler.post(() -> {
                            etContent.setText(content);
                            switchAnonymous.setChecked(anonymous);

                            // 加载已有图片（url 项展示，可删除）
                            if (postData.has("images") && !postData.get("images").isJsonNull()) {
                                try {
                                    com.google.gson.JsonArray imagesArray = postData.getAsJsonArray("images");
                                    imageItems.clear();
                                    for (int i = 0; i < imagesArray.size(); i++) {
                                        JsonObject imgObj = imagesArray.get(i).getAsJsonObject();
                                        if (imgObj.has("url") && !imgObj.get("url").isJsonNull()) {
                                            imageItems.add(new EditImagesAdapter.EditImageItem(
                                                    imgObj.get("url").getAsString()));
                                        }
                                    }
                                    imagesAdapter.notifyDataSetChanged();
                                    updateAddImageButton();
                                    updatePublishButton();
                                } catch (Exception e) {
                                    Log.e(TAG, "加载图片失败: " + e.getMessage());
                                }
                            }

                            // 设置可见性
                            setVisibilitySelection(postVisibility);

                            // 设置栏目（loadCategories 完成后按此回填 Spinner）
                            setCategorySelection(postCategoryId);

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

        if (content.isEmpty() && imageItems.isEmpty()) {
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
                // 准备请求参数（身份由服务端从 JWT 取）
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("content", content);
                params.put("anonymous", anonymous ? "true" : "false");
                params.put("visibility", visibility);
                if (selectedCategoryId != null) {
                    params.put("categoryId", selectedCategoryId);
                }
                params.put("deviceInfo", com.schoolforum.app.utils.DeviceUtils.getDeviceInfo());

                // 编辑模式：传删除的已有图片列表（服务端 updatePost 支持 deletedImages 数组）
                if (editPostId != null && !deletedImageUrls.isEmpty()) {
                    params.put("deletedImages", new com.google.gson.Gson().toJson(deletedImageUrls));
                }

                // 收集新选图片文件
                List<File> imageFiles = new ArrayList<>();
                for (EditImagesAdapter.EditImageItem item : imageItems) {
                    if (!item.isExisting() && item.uri != null) {
                        File file = uriToFile(item.uri);
                        if (file != null) {
                            imageFiles.add(file);
                        }
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
