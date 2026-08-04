package com.schoolforum.app.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.schoolforum.app.BuildConfig;
import com.schoolforum.app.utils.FileLogger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * API客户端 - 网络请求工具类
 * 支持 JWT Token 认证
 */
@SuppressWarnings("unused")
public class ApiClient {
    private static final String TAG = "ApiClient";
    
    private static String BASE_URL = BuildConfig.BASE_URL;
    
    // SharedPreferences keys
    private static final String PREF_NAME = "school_forum";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ADMIN_TOKEN = "admin_token";
    private static final String KEY_SESSION_COOKIE = "session_cookie";
    
    private static ApiClient instance;
    private final OkHttpClient client;
    private final Gson gson;
    private final SharedPreferences prefs;
    private FileLogger logger;
    
    // Token 存储
    private String accessToken;
    private String refreshToken;
    private String adminToken;
    private String sessionCookie; // 保留 Cookie 兼容
    
    // Token 刷新回调接口
    public interface TokenRefreshCallback {
        void onRefreshSuccess(String newToken);
        void onRefreshFailed();
    }
    
    // Token 刷新状态锁，防止并发刷新
    private volatile boolean isRefreshing = false;
    private final Object refreshLock = new Object();
    
    private ApiClient(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        logger = FileLogger.getInstance(context);
        
        // 加载存储的 Token
        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null);
        adminToken = prefs.getString(KEY_ADMIN_TOKEN, null);
        sessionCookie = prefs.getString(KEY_SESSION_COOKIE, null);
        
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new TokenRefreshInterceptor())
                .build();
        
        gson = new Gson();
        
        log("ApiClient initialized, hasAccessToken: " + (accessToken != null));
    }
    
    private void log(String message) {
        Log.d(TAG, message);
        if (logger != null) {
            logger.d(TAG, message);
        }
    }

    /**
     * 脱敏日志中的敏感字段（token/password 等），防止写入本地日志文件泄露
     */
    private String sanitizeLog(String body) {
        if (body == null || body.isEmpty()) return body;
        return body
                .replaceAll("(\"(?:accessToken|refreshToken|adminToken|token|password)\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
    }
    
    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }
    
    public static void setBaseUrl(String url) {
        BASE_URL = url;
    }
    
    public static String getBaseUrl() {
        return BASE_URL;
    }
    
    /**
     * 根据文件扩展名获取 MIME 类型
     */
    private MediaType getMediaTypeFromFile(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return MediaType.parse("image/jpeg");
        } else if (name.endsWith(".png")) {
            return MediaType.parse("image/png");
        } else if (name.endsWith(".gif")) {
            return MediaType.parse("image/gif");
        } else if (name.endsWith(".webp")) {
            return MediaType.parse("image/webp");
        } else if (name.endsWith(".bmp")) {
            return MediaType.parse("image/bmp");
        } else if (name.endsWith(".heic") || name.endsWith(".heif")) {
            return MediaType.parse("image/heic");
        } else {
            return MediaType.parse("image/jpeg"); // 默认使用 jpeg
        }
    }
    
    // ==================== Token 管理方法 ====================
    
    /**
     * 保存认证 Token
     */
    public void saveTokens(String access, String refresh, String admin) {
        this.accessToken = access;
        this.refreshToken = refresh;
        this.adminToken = admin;
        
        SharedPreferences.Editor editor = prefs.edit();
        if (access != null) editor.putString(KEY_ACCESS_TOKEN, access);
        if (refresh != null) editor.putString(KEY_REFRESH_TOKEN, refresh);
        if (admin != null) editor.putString(KEY_ADMIN_TOKEN, admin);
        editor.apply();
        
        log("Tokens saved: hasAccess=" + (access != null) + ", hasRefresh=" + (refresh != null) + ", hasAdmin=" + (admin != null));
    }
    
    /**
     * 保存访问 Token
     */
    public void saveAccessToken(String token) {
        this.accessToken = token;
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }
    
    /**
     * 保存刷新 Token
     */
    public void saveRefreshToken(String token) {
        this.refreshToken = token;
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply();
    }
    
    /**
     * 保存管理员 Token
     */
    public void saveAdminToken(String token) {
        this.adminToken = token;
        prefs.edit().putString(KEY_ADMIN_TOKEN, token).apply();
    }
    
    /**
     * 获取访问 Token
     */
    public String getAccessToken() {
        return accessToken;
    }
    
    /**
     * 获取刷新 Token
     */
    public String getRefreshToken() {
        return refreshToken;
    }
    
    /**
     * 获取管理员 Token
     */
    public String getAdminToken() {
        return adminToken;
    }
    
    /**
     * 是否有有效的访问 Token
     */
    public boolean hasValidToken() {
        return accessToken != null && !accessToken.isEmpty();
    }
    
    /**
     * 是否有管理员 Token
     */
    public boolean hasAdminToken() {
        return adminToken != null && !adminToken.isEmpty();
    }
    
    /**
     * 清除所有 Token（登出）
     */
    public void clearTokens() {
        accessToken = null;
        refreshToken = null;
        adminToken = null;
        sessionCookie = null;
        
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_ADMIN_TOKEN)
                .remove(KEY_SESSION_COOKIE)
                .apply();
        
        log("All tokens cleared");
    }
    
    // ==================== 兼容旧 Cookie 方法 ====================
    
    public void saveSessionCookie(String cookie) {
        this.sessionCookie = cookie;
        prefs.edit().putString(KEY_SESSION_COOKIE, cookie).apply();
    }
    
    public void clearSessionCookie() {
        this.sessionCookie = null;
        prefs.edit().remove(KEY_SESSION_COOKIE).apply();
    }
    
    // ==================== 请求构建方法 ====================
    
    /**
     * 构建请求 - 自动添加 Authorization 头
     */
    private Request.Builder buildRequest(String path) {
        return buildRequest(path, false);
    }
    
    /**
     * 构建请求
     * @param path 请求路径
     * @param useAdminToken 是否使用管理员 Token
     */
    private Request.Builder buildRequest(String path, boolean useAdminToken) {
        // 统一添加 /api 前缀
        String apiPath = path.startsWith("/api/") ? path : "/api" + path;
        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + apiPath)
                .header("Accept", "application/json");
        
        // 优先使用 JWT Token
        if (useAdminToken && adminToken != null && !adminToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + adminToken);
            log("Using admin token for: " + path);
        } else if (accessToken != null && !accessToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
            log("Using access token for: " + path);
        } else if (sessionCookie != null && !sessionCookie.isEmpty()) {
            // 兼容旧的 Cookie 方式
            builder.header("Cookie", sessionCookie);
            log("Using session cookie for: " + path);
        }
        
        return builder;
    }
    
    /**
     * 构建请求（使用管理员 Token）
     */
    private Request.Builder buildAdminRequest(String path) {
        return buildRequest(path, true);
    }
    
    /**
     * GET请求 - 返回解析后的对象
     */
    public <T> void get(String path, Map<String, String> params, final ApiCallback<T> callback, @Nullable final TypeToken<T> typeToken) {
        get(path, params, callback, typeToken, false);
    }
    
    /**
     * GET请求 - 支持管理员 Token
     */
    public <T> void get(String path, Map<String, String> params, final ApiCallback<T> callback, 
                        @Nullable final TypeToken<T> typeToken, boolean useAdminToken) {
        // 统一添加 /api 前缀，与 buildRequest 保持一致（此前用 BASE_URL+path 覆盖导致 GET 全部丢失前缀）
        String apiPath = path.startsWith("/api/") ? path : "/api" + path;
        StringBuilder urlBuilder = new StringBuilder(BASE_URL + apiPath);
        
        if (params != null && !params.isEmpty()) {
            urlBuilder.append("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                try {
                    urlBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                              .append("=")
                              .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                              .append("&");
                } catch (UnsupportedEncodingException e) {
                    // UTF-8 always available, but fallback to unencoded
                    urlBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }
        
        Request.Builder builder = buildRequest(apiPath, useAdminToken);
        builder.url(urlBuilder.toString());
        Request request = builder.get().build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log("GET request failed: " + path + ", error: " + e.getMessage());
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleResponse(response, callback, typeToken);
            }
        });
    }
    
    /**
     * POST请求 (JSON)
     */
    public <T> void post(String path, Object body, final ApiCallback<T> callback, @Nullable final TypeToken<T> typeToken) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);
        
        Request.Builder builder = buildRequest(path);
        Request request = builder.post(requestBody).build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log("POST request failed: " + path + ", error: " + e.getMessage());
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleResponse(response, callback, typeToken);
            }
        });
    }
    
    /**
     * POST请求 (表单) - 支持泛型返回
     * 如果 typeToken 为 null，返回原始字符串（通过 onSuccess(responseBody)）
     * 如果 typeToken 不为 null，返回解析后的对象
     */
    public <T> void postForm(String path, Map<String, String> params, final ApiCallback<T> callback, @Nullable final TypeToken<T> typeToken) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        
        Request.Builder builder = buildRequest(path);
        Request request = builder.post(formBuilder.build()).build();
        
        log("POST form request: " + path);
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log("POST form request failed: " + path + ", error: " + e.getMessage());
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleResponse(response, callback, typeToken);
            }
        });
    }
    
    /**
     * DELETE请求
     */
    public <T> void delete(String path, Map<String, String> params, final ApiCallback<T> callback, @Nullable final TypeToken<T> typeToken) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(gson.toJson(params), JSON);
        
        Request.Builder builder = buildRequest(path);
        Request request = builder.delete(requestBody).build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log("DELETE request failed: " + path + ", error: " + e.getMessage());
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleResponse(response, callback, typeToken);
            }
        });
    }
    
    /**
     * DELETE请求 - 简化版本
     */
    public void delete(String path, Map<String, String> params, Callback callback) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(gson.toJson(params), JSON);
        
        Request.Builder builder = buildRequest(path);
        Request request = builder.delete(requestBody).build();
        
        log("DELETE request: " + path);
        
        client.newCall(request).enqueue(callback);
    }
    
    @SuppressWarnings("unchecked")
    private <T> void handleResponse(Response response, ApiCallback<T> callback, @Nullable TypeToken<T> typeToken) throws IOException {
        String cookieHeader = response.header("Set-Cookie");
        if (cookieHeader != null) {
            saveSessionCookie(cookieHeader);
        }
        
        String responseBody = response.body() != null ? response.body().string() : "";
        log("Response [" + response.code() + "]: " + sanitizeLog(responseBody));
        
        if (!response.isSuccessful()) {
            // 尝试从响应体中提取服务器返回的具体错误信息
            String errorMsg = "服务器错误: " + response.code();
            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    JsonObject errObj = gson.fromJson(responseBody, JsonObject.class);
                    if (errObj != null && errObj.has("message") && !errObj.get("message").isJsonNull()) {
                        errorMsg = errObj.get("message").getAsString();
                    }
                } catch (Exception ignored) {
                    // 响应体不是 JSON 格式，保留默认错误信息
                }
            }
            log("Request failed [" + response.code() + "]: " + errorMsg);
            callback.onError(errorMsg);
            return;
        }
        
        try {
            if (typeToken == null) {
                // 返回原始字符串
                callback.onSuccess((T) responseBody);
            } else {
                // 解析为指定类型
                Type type = typeToken.getType();
                // 如果是 String 类型，直接返回字符串
                if (type == String.class) {
                    callback.onSuccess((T) responseBody);
                } else {
                    T data = gson.fromJson(responseBody, type);
                    callback.onSuccess(data);
                }
            }
        } catch (Exception e) {
            log("JSON解析失败: " + e.getMessage() + ", response: " + responseBody);
            callback.onError("数据解析失败: " + e.getMessage());
        }
    }
    
    public interface ApiCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }
    
    /**
     * Multipart表单上传 - 支持文件和文本参数
     */
    public void postMultipart(String path, Map<String, Object> params, List<File> files, 
                              String fileFieldName, Callback callback) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        
        // 添加文本参数
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                multipartBuilder.addFormDataPart(entry.getKey(), value);
            }
        }
        
        // 添加文件
        if (files != null) {
            for (File file : files) {
                if (file != null && file.exists()) {
                    MediaType imageType = getMediaTypeFromFile(file);
                    multipartBuilder.addFormDataPart(fileFieldName, file.getName(),
                        RequestBody.create(file, imageType));
                }
            }
        }
        
        Request.Builder builder = buildRequest(path);
        Request request = builder.post(multipartBuilder.build()).build();
        
        log("POST multipart request: " + path);
        
        client.newCall(request).enqueue(callback);
    }
    
    /**
     * PUT Multipart请求 - 用于更新帖子等需要上传文件的操作
     */
    public void putMultipart(String path, Map<String, Object> params, List<File> files,
                              String fileFieldName, Callback callback) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        
        // 添加文本参数
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                multipartBuilder.addFormDataPart(entry.getKey(), value);
            }
        }
        
        // 添加文件
        if (files != null) {
            for (File file : files) {
                if (file != null && file.exists()) {
                    MediaType imageType = getMediaTypeFromFile(file);
                    multipartBuilder.addFormDataPart(fileFieldName, file.getName(),
                        RequestBody.create(file, imageType));
                }
            }
        }
        
        Request.Builder builder = buildRequest(path);
        Request request = builder.put(multipartBuilder.build()).build();
        
        log("PUT multipart request: " + path);
        
        client.newCall(request).enqueue(callback);
    }
    
    /**
     * GET请求 - 简化版本，直接返回Response
     */
    public void get(String path, Callback callback) {
        Request.Builder builder = buildRequest(path);
        Request request = builder.get().build();
        
        log("GET request: " + path);
        
        client.newCall(request).enqueue(callback);
    }
    
    /**
     * POST表单请求 - 简化版本
     */
    public void postForm(String path, Map<String, String> params, Callback callback) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        
        Request.Builder builder = buildRequest(path);
        Request request = builder.post(formBuilder.build()).build();
        
        log("POST form request: " + path);
        
        client.newCall(request).enqueue(callback);
    }
    
    /**
     * POST JSON请求 - 简化版本
     */
    public void post(String path, Object body, Callback callback) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);
        
        Request.Builder builder = buildRequest(path);
        Request request = builder.post(requestBody).build();
        
        log("POST json request: " + path);
        
        client.newCall(request).enqueue(callback);
    }
    
    // ==================== Token 刷新和登出 ====================
    
    /**
     * 刷新访问令牌
     */
    public void refreshToken(final TokenRefreshCallback callback) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log("No refresh token available");
            if (callback != null) callback.onRefreshFailed();
            return;
        }
        
        log("Refreshing access token...");
        
        Map<String, String> params = new HashMap<>();
        params.put("refreshToken", refreshToken);
        
        // 不使用 buildRequest，因为刷新 Token 不需要 Authorization 头
        FormBody.Builder formBuilder = new FormBody.Builder();
        formBuilder.add("refreshToken", refreshToken);
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/refresh-token")
                .header("Accept", "application/json")
                .post(formBuilder.build())
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log("Token refresh failed: " + e.getMessage());
                if (callback != null) callback.onRefreshFailed();
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                log("Token refresh response: " + sanitizeLog(body));
                
                if (response.isSuccessful()) {
                    try {
                        RefreshResponse refreshResponse = gson.fromJson(body, RefreshResponse.class);
                        // 兼容根级 token 与旧 data.token 两种格式
                        String newToken = refreshResponse != null && refreshResponse.success
                                ? (refreshResponse.token != null ? refreshResponse.token
                                   : (refreshResponse.data != null ? refreshResponse.data.token : null))
                                : null;
                        if (newToken != null) {
                            saveAccessToken(newToken);
                            log("Token refresh successful");
                            if (callback != null) callback.onRefreshSuccess(newToken);
                        } else {
                            log("Token refresh failed: invalid response");
                            if (callback != null) callback.onRefreshFailed();
                        }
                    } catch (Exception e) {
                        log("Token refresh parse error: " + e.getMessage());
                        if (callback != null) callback.onRefreshFailed();
                    }
                } else {
                    log("Token refresh failed: " + response.code());
                    if (callback != null) callback.onRefreshFailed();
                }
            }
        });
    }
    
    /**
     * 用户登出
     */
    public void logout(final ApiCallback<String> callback) {
        if (accessToken == null) {
            clearTokens();
            if (callback != null) callback.onSuccess("已清除本地登录状态");
            return;
        }
        
        log("Logging out...");
        
        Map<String, String> params = new HashMap<>();
        params.put("token", accessToken);
        
        postForm("/logout", params, new ApiCallback<String>() {
            @Override
            public void onSuccess(String data) {
                log("Logout successful");
                clearTokens();
                if (callback != null) callback.onSuccess("登出成功");
            }
            
            @Override
            public void onError(String error) {
                log("Logout failed: " + error + ", clearing local tokens anyway");
                clearTokens();
                if (callback != null) callback.onSuccess("已清除本地登录状态");
            }
        }, null);
    }
    
    /**
     * 管理员登出
     */
    public void adminLogout(final ApiCallback<String> callback) {
        if (adminToken == null) {
            saveAdminToken(null);
            if (callback != null) callback.onSuccess("已清除管理员状态");
            return;
        }
        
        log("Admin logging out...");
        
        Map<String, String> params = new HashMap<>();
        params.put("token", adminToken);
        
        postForm("/admin/logout", params, new ApiCallback<String>() {
            @Override
            public void onSuccess(String data) {
                log("Admin logout successful");
                saveAdminToken(null);
                if (callback != null) callback.onSuccess("管理员登出成功");
            }
            
            @Override
            public void onError(String error) {
                log("Admin logout failed: " + error);
                saveAdminToken(null);
                if (callback != null) callback.onSuccess("已清除管理员状态");
            }
        }, null);
    }
    
    // ==================== 响应模型 ====================
    
    private static class RefreshResponse {
        boolean success;
        String message;
        // 服务端 refresh-token 响应格式：{ success, message, token }（业务字段根级展开）
        String token;
        // 兼容旧格式 { success, message, data: { token } }
        RefreshData data;
    }
    
    private static class RefreshData {
        String token;
    }
    
    /**
     * Token 自动刷新拦截器
     * 当收到 401 响应时，自动用 refreshToken 获取新的 accessToken 并重试请求
     */
    private class TokenRefreshInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Response response = chain.proceed(originalRequest);
            
            // 只处理 401 响应
            if (response.code() != 401) {
                return response;
            }
            
            // 检查是否是 token 相关的 401（排除登录等不需要 token 的请求）
            String path = originalRequest.url().encodedPath();
            String authHeader = originalRequest.header("Authorization");
            if (authHeader == null || authHeader.isEmpty()) {
                // 请求本身没有携带 token，不需要刷新
                return response;
            }
            
            // 检查响应体中是否包含 TOKEN_EXPIRED 或 token 相关错误
            String responseBody = "";
            try {
                // 读取响应体，但需要重新创建 Response 因为 body 只能读一次
                responseBody = response.body() != null ? response.body().string() : "";
            } catch (Exception e) {
                log("Failed to read 401 response body: " + e.getMessage());
                return response;
            }
            
            boolean isTokenError = responseBody.contains("TOKEN_EXPIRED") 
                    || responseBody.contains("过期")
                    || responseBody.contains("身份验证")
                    || responseBody.contains("令牌已失效");
            
            if (!isTokenError) {
                // 不是 token 过期导致的 401，直接返回
                // 重建 response（因为 body 已经被读取了）
                Response.Builder newResponseBuilder = new Response.Builder()
                        .code(response.code())
                        .message(response.message())
                        .headers(response.headers())
                        .body(okhttp3.ResponseBody.create(responseBody, 
                                okhttp3.MediaType.parse("application/json; charset=utf-8")))
                        .request(response.request())
                        .protocol(response.protocol());
                return newResponseBuilder.build();
            }
            
            log("401 detected, attempting token refresh for: " + path);
            response.close();
            
            // 同步刷新 Token（在拦截器中必须同步，因为需要返回 Response）
            synchronized (refreshLock) {
                // 双重检查：如果其他线程已经刷新了 token，直接重试
                String currentToken = accessToken;
                if (authHeader.equals("Bearer " + currentToken)) {
                    // token 没有被其他线程更新，需要刷新
                    boolean refreshSuccess = refreshSync();
                    if (!refreshSuccess) {
                        log("Token refresh failed, returning 401");
                        // 刷新失败，清除 token 并返回 401
                        clearTokens();
                        // 返回一个新的 401 响应
                        String failBody = "{\"success\":false,\"message\":\"登录已过期，请重新登录\",\"code\":\"TOKEN_EXPIRED\"}";
                        return new Response.Builder()
                                .code(401)
                                .message("Unauthorized")
                                .header("Content-Type", "application/json; charset=utf-8")
                                .body(okhttp3.ResponseBody.create(failBody, 
                                        okhttp3.MediaType.parse("application/json; charset=utf-8")))
                                .request(originalRequest)
                                .protocol(Protocol.HTTP_1_1)
                                .build();
                    }
                } else {
                    log("Token was already refreshed by another thread");
                }
            }
            
            // 用新 token 重新构建请求并重试
            Request newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .build();
            log("Retrying request with new token: " + path);
            return chain.proceed(newRequest);
        }
        
        /**
         * 同步刷新 Token（阻塞式，用于拦截器中）
         */
        private boolean refreshSync() {
            if (refreshToken == null || refreshToken.isEmpty()) {
                log("No refresh token available for sync refresh");
                return false;
            }
            
            try {
                FormBody.Builder formBuilder = new FormBody.Builder();
                formBuilder.add("refreshToken", refreshToken);
                
                Request refreshRequest = new Request.Builder()
                        .url(BASE_URL + "/api/refresh-token")
                        .header("Accept", "application/json")
                        .post(formBuilder.build())
                        .build();
                
                // 同步执行刷新请求
                Response refreshResponse = client.newBuilder()
                        .build()
                        .newCall(refreshRequest)
                        .execute();
                
                String body = refreshResponse.body() != null ? refreshResponse.body().string() : "";
                log("Sync token refresh response: " + refreshResponse.code());
                
                if (refreshResponse.isSuccessful()) {
                    RefreshResponse refreshObj = gson.fromJson(body, RefreshResponse.class);
                    String newToken = refreshObj != null && refreshObj.success
                            ? (refreshObj.token != null ? refreshObj.token
                               : (refreshObj.data != null ? refreshObj.data.token : null))
                            : null;
                    if (newToken != null) {
                        saveAccessToken(newToken);
                        log("Sync token refresh successful");
                        return true;
                    }
                }
                
                log("Sync token refresh failed: " + sanitizeLog(body));
                return false;
            } catch (Exception e) {
                log("Sync token refresh error: " + e.getMessage());
                return false;
            }
        }
    }
}