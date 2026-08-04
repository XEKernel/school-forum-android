package com.schoolforum.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * WebView Fragment - 用于显示其他页面
 */
@SuppressWarnings("unused")
public class WebViewFragment extends Fragment {

    private static final String ARG_URL = "url";
    
    // 使用 BuildConfig 获取服务器地址
    private static String getBaseUrl() {
        return com.schoolforum.app.BuildConfig.BASE_URL;
    }
    
    private WebView webView;
    private ProgressBar progressBar;
    private FrameLayout errorLayout;
    private String currentUrl;
    private String initialUrl;

    /**
     * 创建带有URL的Fragment实例
     */
    public static WebViewFragment newInstance(String url) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialUrl = getArguments().getString(ARG_URL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        webView = view.findViewById(R.id.webView);
        progressBar = view.findViewById(R.id.progressBar);
        errorLayout = view.findViewById(R.id.errorLayout);
        
        setupWebView(view);
        
        // 加载初始URL或默认页面
        String urlToLoad = initialUrl != null ? initialUrl : getBaseUrl() + "/profile.html";
        loadUrl(urlToLoad);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(View view) {
        WebSettings settings = webView.getSettings();
        
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // 收紧混合内容策略：HTTPS 页面不再无条件加载 HTTP 资源（防中间人注入）
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        // 关闭本地文件访问：页面资源均来自服务器，无需读取本地文件（防恶意 JS 读取）
        settings.setAllowFileAccess(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // User-Agent
        String defaultUserAgent = settings.getUserAgentString();
        settings.setUserAgentString(defaultUserAgent + " SchoolForumApp/1.0");
        
        // Cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        // 仅接受第一方 Cookie（页面均为本站资源，无第三方依赖）
        cookieManager.setAcceptThirdPartyCookies(webView, false);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                errorLayout.setVisibility(View.GONE);
                currentUrl = url;
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
            
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    showError();
                }
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                android.util.Log.d("WebViewFragment", "shouldOverrideUrlLoading: " + url);
                
                // 处理特殊链接
                if (url.startsWith("tel:")) {
                    return true;
                }
                
                // 判断是否是内部论坛链接
                boolean isInternalUrl = url.contains(getBaseUrl().replace("http://", "").replace("https://", "")) || 
                                        url.contains("localhost:2080") ||
                                        url.contains("/post-detail.html") ||
                                        url.contains("/profile.html");
                
                if (isInternalUrl) {
                    // 拦截内部链接，跳转到原生页面
                    android.content.Context context = getContext();
                    if (context != null) {
                        // 帖子详情页
                        if (url.contains("/post-detail.html")) {
                            String postId = extractQueryParam(url, "id");
                            if (postId != null) {
                                android.content.Intent intent = new android.content.Intent(context, 
                                    com.schoolforum.app.ui.post.PostDetailActivity.class);
                                intent.putExtra("post_id", postId);
                                startActivity(intent);
                                return true;
                            }
                        }
                        
                        // 用户主页
                        if (url.contains("/profile.html")) {
                            String userId = extractQueryParam(url, "id");
                            if (userId != null) {
                                android.content.Intent intent = new android.content.Intent(context, 
                                    com.schoolforum.app.ui.profile.ProfileActivity.class);
                                intent.putExtra("userId", userId);
                                startActivity(intent);
                                return true;
                            }
                        }
                    }
                    return true;
                }
                
                // 外部链接在 WebView 中打开
                return false;
            }
        });
        
        // 错误页面重试
        view.findViewById(R.id.btnRetry).setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            loadUrl(currentUrl != null ? currentUrl : getBaseUrl() + "/profile.html");
        });
    }

    public void loadUrl(String url) {
        if (webView != null) {
            webView.loadUrl(url);
        }
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        }
    }

    private void showError() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (errorLayout != null) {
            errorLayout.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 从 URL 中提取查询参数
     */
    private String extractQueryParam(String url, String paramName) {
        try {
            android.net.Uri uri = android.net.Uri.parse(url);
            return uri.getQueryParameter(paramName);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroyView();
    }
}
