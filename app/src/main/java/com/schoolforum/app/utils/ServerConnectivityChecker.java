package com.schoolforum.app.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 服务器连通性检查工具
 * APP 启动时检测服务器是否可达：
 * - 5 秒连接/读取短超时（比 ApiClient 的 30 秒快，避免启动时长时间卡顿）
 * - 后台线程执行，回调回到主线程，不阻塞 UI
 * - 检查 GET {baseUrl}/health，200 即视为可达
 */
public class ServerConnectivityChecker {

    public interface Callback {
        /**
         * @param reachable 服务器是否可达
         * @param error     失败原因（可达时为 null）
         */
        void onResult(boolean reachable, String error);
    }

    private ServerConnectivityChecker() {
    }

    /**
     * 异步检查服务器连通性
     *
     * @param baseUrl  服务器根地址，如 http://10.0.2.2:2080
     * @param callback 结果回调（主线程执行），可为 null
     */
    public static void check(String baseUrl, final Callback callback) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(false, "服务器地址未配置"));
            }
            return;
        }

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            String error = null;
            boolean reachable = false;
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .callTimeout(8, TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder()
                    .url(baseUrl + "/health")
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                reachable = response.isSuccessful();
            } catch (Exception e) {
                error = e.getMessage();
            }
            final boolean ok = reachable;
            final String errMsg = error;
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onResult(ok, errMsg);
                }
            });
        }, "server-connectivity-check").start();
    }
}
