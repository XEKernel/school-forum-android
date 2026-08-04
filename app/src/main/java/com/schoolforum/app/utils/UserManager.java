package com.schoolforum.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.schoolforum.app.model.User;
import com.schoolforum.app.network.ApiClient;

/**
 * 用户管理工具类
 * 整合用户信息和 Token 管理
 */
@SuppressWarnings("unused")
public class UserManager {
    private static final String TAG = "UserManager";
    private static final String PREF_NAME = "school_forum";
    private static final String KEY_USER = "current_user";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    
    private static UserManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private User currentUser;
    private FileLogger logger;
    private Context context;
    
    private UserManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        logger = FileLogger.getInstance(context);
        loadUser();
        log("UserManager initialized, currentUser: " + (currentUser != null ? currentUser.getId() : "null"));
    }
    
    private void log(String message) {
        Log.d(TAG, message);
        if (logger != null) {
            logger.d(TAG, message);
        }
    }
    
    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 加载用户信息
     */
    private void loadUser() {
        String userJson = prefs.getString(KEY_USER, null);
        log("Loading user from prefs: " + (userJson != null ? "exists, length=" + userJson.length() : "null"));
        if (userJson != null) {
            try {
                currentUser = gson.fromJson(userJson, User.class);
                log("User loaded: " + (currentUser != null ? currentUser.getId() : "null"));
            } catch (Exception e) {
                log("Failed to parse user json: " + e.getMessage());
                currentUser = null;
            }
        }
    }
    
    /**
     * 保存用户信息（不包含 Token）
     * Token 由 ApiClient 单独管理
     */
    public void saveUser(User user) {
        this.currentUser = user;
        String userJson = gson.toJson(user);
        log("Saving user: " + userJson);
        // 使用 commit() 确保同步保存
        boolean success = prefs.edit()
                .putString(KEY_USER, userJson)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .commit();
        log("User saved: success=" + success + ", currentUser id: " + (currentUser != null ? currentUser.getId() : "null"));
    }
    
    /**
     * 保存用户和 Token
     * @param user 用户对象
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param adminToken 管理员令牌（可为 null）
     */
    public void saveUserWithTokens(User user, String accessToken, String refreshToken, String adminToken) {
        saveUser(user);
        ApiClient.getInstance(context).saveTokens(accessToken, refreshToken, adminToken);
        log("User and tokens saved");
    }
    
    /**
     * 强制重新加载用户信息
     */
    public void reloadUser() {
        loadUser();
        log("User reloaded: " + (currentUser != null ? currentUser.getId() : "null"));
    }
    
    /**
     * 获取当前用户
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * 获取当前用户ID
     */
    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }
    
    /**
     * 获取当前用户名
     */
    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }
    
    /**
     * 是否已登录
     */
    public boolean isLoggedIn() {
        boolean isLoggedInFlag = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        // 确保用户数据有效（不为null且有有效的id）
        boolean hasValidUser = currentUser != null && currentUser.getId() != null && !currentUser.getId().isEmpty();
        // 同时检查 Token 是否存在
        boolean hasToken = ApiClient.getInstance(context).hasValidToken();
        boolean result = isLoggedInFlag && hasValidUser && hasToken;
        log("isLoggedIn check: flag=" + isLoggedInFlag + ", hasValidUser=" + hasValidUser + ", hasToken=" + hasToken + ", result=" + result);
        return result;
    }
    
    /**
     * 是否是管理员
     */
    public boolean isAdmin() {
        return currentUser != null && Boolean.TRUE.equals(currentUser.getIsAdmin());
    }
    
    /**
     * 清除用户信息（登出）
     */
    public void clearUser() {
        currentUser = null;
        prefs.edit()
                .remove(KEY_USER)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
        // 同时清除 Token
        ApiClient.getInstance(context).clearTokens();
        log("User and tokens cleared");
    }
    
    /**
     * 登出（调用服务器 API）
     */
    public void logout(final LogoutCallback callback) {
        log("Logging out...");
        ApiClient.getInstance(context).logout(new ApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String data) {
                currentUser = null;
                prefs.edit()
                        .remove(KEY_USER)
                        .putBoolean(KEY_IS_LOGGED_IN, false)
                        .apply();
                log("Logout successful");
                if (callback != null) callback.onSuccess();
            }
            
            @Override
            public void onError(String error) {
                // 即使服务器请求失败，也清除本地数据
                currentUser = null;
                prefs.edit()
                        .remove(KEY_USER)
                        .putBoolean(KEY_IS_LOGGED_IN, false)
                        .apply();
                log("Logout local cleared: " + error);
                if (callback != null) callback.onSuccess(); // 仍然返回成功，因为本地已清除
            }
        });
    }
    
    /**
     * 登出回调接口
     */
    public interface LogoutCallback {
        void onSuccess();
    }
    
    /**
     * 更新用户头像
     */
    public void updateAvatar(String avatarUrl) {
        if (currentUser != null) {
            currentUser.setAvatar(avatarUrl);
            saveUser(currentUser);
        }
    }
    
    /**
     * 更新用户名
     */
    public void updateUsername(String username) {
        if (currentUser != null) {
            currentUser.setUsername(username);
            saveUser(currentUser);
        }
    }
    
    /**
     * 获取访问 Token
     */
    public String getAccessToken() {
        return ApiClient.getInstance(context).getAccessToken();
    }
    
    /**
     * 获取管理员 Token
     */
    public String getAdminToken() {
        return ApiClient.getInstance(context).getAdminToken();
    }
    
    /**
     * 是否有管理员 Token
     */
    public boolean hasAdminToken() {
        return ApiClient.getInstance(context).hasAdminToken();
    }
}