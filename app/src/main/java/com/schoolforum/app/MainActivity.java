package com.schoolforum.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.ValueCallback;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.ui.chat.ConversationsActivity;
import com.schoolforum.app.ui.follow.FollowingActivity;
import com.schoolforum.app.ui.login.LoginActivity;
import com.schoolforum.app.ui.notification.NotificationsActivity;
import com.schoolforum.app.ui.post.PostsFragment;
import com.schoolforum.app.ui.profile.ProfileActivity;
import com.schoolforum.app.utils.FileLogger;
import com.schoolforum.app.utils.UserManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 主Activity - 原生页面为主，WebView仅用于外部链接
 */
@SuppressWarnings({"unused", "deprecation"})
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // 文件日志
    private FileLogger logger;

    // 服务器地址（统一使用 BuildConfig）
    private static final String BASE_URL = BuildConfig.BASE_URL;

    // 权限请求码
    private static final int REQUEST_PERMISSIONS = 1001;

    // UI组件
    private BottomNavigationView bottomNav;
    
    // Fragment
    private Fragment postsFragment;
    private Fragment webViewFragment;
    private int currentNavId = R.id.nav_home;
    
    // 双击退出
    private boolean doubleBackToExitPressedOnce = false;

    // 服务器连通性检查
    private AlertDialog connectivityDialog;
    private boolean connectivityCheckInFlight = false;

    // 文件上传相关（用于WebView）
    private ValueCallback<Uri[]> filePathCallback;
    private String cameraPhotoPath;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        logger = FileLogger.getInstance(this);
        
        // 检查登录状态
        boolean isLoggedIn = UserManager.getInstance(this).isLoggedIn();
        log("onCreate: isLoggedIn=" + isLoggedIn);
        if (!isLoggedIn) {
            log("Not logged in, starting LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        log("Logged in, showing main content");
        setContentView(R.layout.activity_main);
        
        initLaunchers();
        initViews();
        checkPermissions();
        setupNotificationChannel();
        setupNavigation();
        
        // 启动时检查服务器连通性
        checkServerConnectivity();
        
        // 处理Intent
        handleIntent(getIntent());
        
        // 默认显示首页
        if (savedInstanceState == null) {
            showPostsFragment();
        }
    }

    /**
     * 启动时检查服务器连通性（后台线程，5 秒短超时）
     */
    private void checkServerConnectivity() {
        if (connectivityCheckInFlight) {
            return;
        }
        connectivityCheckInFlight = true;
        com.schoolforum.app.utils.ServerConnectivityChecker.check(BASE_URL,
                (reachable, error) -> {
                    connectivityCheckInFlight = false;
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (reachable) {
                        log("服务器连通性检查通过: " + BASE_URL);
                    } else {
                        log("服务器连通性检查失败: " + (error != null ? error : "未知错误"));
                        showConnectivityDialog();
                    }
                });
    }

    /**
     * 显示无法连接服务器的对话框（提供重试/继续）
     */
    private void showConnectivityDialog() {
        if (connectivityDialog != null && connectivityDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("无法连接服务器");
        builder.setMessage("无法连接到服务器（" + BASE_URL + "）。\n\n请检查：\n1. 服务器是否已启动\n2. 设备网络是否正常\n3. 服务器地址配置是否正确");
        builder.setPositiveButton("重试", (dialog, which) -> checkServerConnectivity());
        builder.setNegativeButton("继续使用", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        connectivityDialog = builder.show();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
    
    /**
     * 处理Intent - 仅用于打开外部链接
     * 论坛内部功能全部使用原生页面
     */
    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("openWebView", false)) {
            String url = intent.getStringExtra("url");
            if (url != null && !url.isEmpty()) {
                // 检查是否是外部链接
                boolean isExternal = !url.contains(BuildConfig.BASE_URL.replace("http://", "").replace("https://", "")) && 
                                     !url.contains("localhost:2080") &&
                                     !url.contains("/post-detail.html") &&
                                     !url.contains("/profile.html");
                if (isExternal) {
                    log("Opening external URL in WebView: " + url);
                    showWebViewFragment(url);
                }
            }
        }
    }
    
    private void log(String message) {
        Log.d(TAG, message);
        if (logger != null) {
            logger.d(TAG, message);
        }
    }

    private void initLaunchers() {
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handleCameraResult(result.getResultCode())
        );

        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handleFilePickerResult(result.getResultCode(), result.getData())
        );
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottomNav);
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == currentNavId) {
                return true;
            }
            
            currentNavId = itemId;
            
            if (itemId == R.id.nav_home) {
                showPostsFragment();
                return true;
            } else if (itemId == R.id.nav_following) {
                startActivity(new Intent(this, FollowingActivity.class));
                // 清除徽章
                clearFollowingBadge();
                bottomNav.postDelayed(() -> {
                    if (!isFinishing()) {
                        bottomNav.setSelectedItemId(R.id.nav_home);
                    }
                }, 300);
                return false;
            } else if (itemId == R.id.nav_publish) {
                // 打开发布帖子页面
                startActivity(new Intent(this, com.schoolforum.app.ui.post.EditPostActivity.class));
                // 延迟重置导航选中状态，让用户能够返回
                bottomNav.postDelayed(() -> {
                    if (!isFinishing()) {
                        bottomNav.setSelectedItemId(R.id.nav_home);
                    }
                }, 300);
                return false;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                // 延迟重置导航选中状态，让用户能够返回
                bottomNav.postDelayed(() -> {
                    if (!isFinishing()) {
                        bottomNav.setSelectedItemId(R.id.nav_home);
                    }
                }, 300);
                return false;
            }
            
            return false;
        });
    }
    
    /**
     * 更新动态徽章
     */
    public void updateFollowingBadge(int count) {
        runOnUiThread(() -> {
            if (bottomNav == null) return;
            
            com.google.android.material.navigation.NavigationBarView navView = bottomNav;
            com.google.android.material.badge.BadgeDrawable badge = navView.getBadge(R.id.nav_following);
            
            if (count > 0) {
                if (badge == null) {
                    badge = navView.getOrCreateBadge(R.id.nav_following);
                }
                badge.setVisible(true);
                badge.setNumber(count);
            } else {
                if (badge != null) {
                    badge.setVisible(false);
                }
            }
        });
    }
    
    /**
     * 清除动态徽章
     */
    private void clearFollowingBadge() {
        if (bottomNav != null) {
            com.google.android.material.badge.BadgeDrawable badge = bottomNav.getBadge(R.id.nav_following);
            if (badge != null) {
                badge.setVisible(false);
            }
        }
    }
    
    /**
     * 获取新动态数量
     */
    private void fetchNewFollowingPostsCount() {
        String userId = UserManager.getInstance(this).getCurrentUserId();
        if (userId == null) return;
        
        ApiClient.getInstance(this).get("/follow/new-posts/" + userId, null,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    try {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        com.google.gson.JsonObject response = gson.fromJson(data, com.google.gson.JsonObject.class);
                        if (response.has("success") && response.get("success").getAsBoolean()) {
                            int count = response.has("count") ? response.get("count").getAsInt() : 0;
                            updateFollowingBadge(count);
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                }
                
                @Override
                public void onError(String error) {
                    // 忽略
                }
            }, null);
    }

    private void showPostsFragment() {
        if (postsFragment == null) {
            postsFragment = new PostsFragment();
        }
        replaceFragment(postsFragment);
    }

    private void showWebViewFragment() {
        showWebViewFragment(null);
    }
    
    private void showWebViewFragment(String url) {
        if (url != null && !url.isEmpty()) {
            webViewFragment = WebViewFragment.newInstance(url);
        } else if (webViewFragment == null) {
            webViewFragment = new WebViewFragment();
        }
        replaceFragment(webViewFragment);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.commit();
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        String[] permissions = getRequiredPermissions();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                permissionsNeeded.toArray(new String[0]),
                REQUEST_PERMISSIONS);
        }
    }

    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA,
                Manifest.permission.VIBRATE,
                Manifest.permission.POST_NOTIFICATIONS
            };
        }
        return new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.VIBRATE
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "school_forum",
                "校园论坛",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("校园论坛消息通知");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // ==================== 文件上传相关方法（供WebView使用） ====================

    private void showImagePickerDialog() {
        String[] items = {"拍照", "从相册选择"};
        new AlertDialog.Builder(this)
            .setTitle("选择图片")
            .setItems(items, (dialog, which) -> {
                if (which == 0) {
                    openCamera();
                } else {
                    openImagePicker();
                }
            })
            .setOnCancelListener(dialog -> cancelFilePathCallback())
            .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = createImageFile();
            cameraPhotoPath = photoFile.getAbsolutePath();
            Uri photoUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(this, "无法创建图片文件", Toast.LENGTH_SHORT).show();
            cancelFilePathCallback();
        }
    }

    private File createImageFile() throws IOException {
        String fileName = "IMG_" + System.currentTimeMillis();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(intent);
    }

    private void handleCameraResult(int resultCode) {
        if (filePathCallback == null) return;
        Uri[] results = null;
        if (resultCode == RESULT_OK && cameraPhotoPath != null) {
            results = new Uri[]{Uri.fromFile(new File(cameraPhotoPath))};
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    private void handleFilePickerResult(int resultCode, @Nullable Intent data) {
        if (filePathCallback == null) return;
        Uri[] results = null;
        if (resultCode == RESULT_OK && data != null) {
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                int count = clipData.getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) {
                    results[i] = clipData.getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    private void cancelFilePathCallback() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
    }

    // ==================== 供WebViewFragment调用 ====================

    public void setFilePathCallback(ValueCallback<Uri[]> callback) {
        this.filePathCallback = callback;
    }

    public void showImagePicker() {
        showImagePickerDialog();
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    // ==================== 生命周期方法 ====================
    
    @Override
    protected void onResume() {
        super.onResume();
        // 获取新动态数量
        fetchNewFollowingPostsCount();
    }

    // ==================== 返回键处理 ====================

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        
        doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
        
        new Handler(Looper.getMainLooper()).postDelayed(() ->
            doubleBackToExitPressedOnce = false, 2000);
    }
}
