package com.schoolforum.app.utils;

import android.content.Context;
import android.webkit.WebView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.WeakHashMap;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TableAwareMovementMethod;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.glide.GlideImagesPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;
import io.noties.markwon.syntax.Prism4jTheme;
import io.noties.markwon.syntax.Prism4jThemeDarkula;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;
import ru.noties.jlatexmath.JLatexMathAndroid;

/**
 * Markdown渲染工具类
 * - render()：Markwon 原生渲染到 TextView（表格/代码高亮/LaTeX 公式，无 WebView）
 * - renderWebView()：WebView 渲染（保留作回退，默认不再使用）
 */
public class MarkdownUtils {
    // 使用 WeakHashMap 缓存 Markwon 实例，Context 被销毁时自动释放
    private static final WeakHashMap<Context, Markwon> markwonCache = new WeakHashMap<>();

    /** Prism4j 实例（语言由 prism4j-bundler 编译期生成，注册见 ForumPrismBundle） */
    private static final Prism4j PRISM4J = new Prism4j(new Prism4jBundler());

    /** 代码块高亮主题（深色，与网页 github-dark 风格一致） */
    private static final Prism4jTheme PRISM_THEME = Prism4jThemeDarkula.create();

    /** WebView 渲染模板（assets/markdown/render.html），缓存避免重复 IO */
    private static volatile String renderTemplate = null;

    /**
     * 获取Markwon实例（基于 Context 缓存）
     * 原生渲染插件：表格 + 代码高亮 + LaTeX 公式
     */
    public static Markwon getMarkwon(Context context) {
        // 使用 ApplicationContext 避免Activity泄漏
        Context appContext = context.getApplicationContext();
        Markwon markwon = markwonCache.get(appContext);
        if (markwon == null) {
            try {
                // jlatexmath 渲染前必须初始化字体工厂（否则 TeXFormula 加载字体抛 NPE，
                // 所有公式渲染失败——行内/希腊字母/数学符号全部空白）。幂等，可重复调用
                JLatexMathAndroid.init(appContext);
                // 公式字号跟随系统密度（14sp 基准）
                final float textSize = 14f * appContext.getResources().getDisplayMetrics().scaledDensity;
                markwon = Markwon.builder(appContext)
                        .usePlugin(MarkwonInlineParserPlugin.create())
                        .usePlugin(TablePlugin.create(appContext))
                        .usePlugin(StrikethroughPlugin.create())
                        .usePlugin(TaskListPlugin.create(appContext))
                        .usePlugin(HtmlPlugin.create())
                        .usePlugin(SyntaxHighlightPlugin.create(PRISM4J, PRISM_THEME))
                        .usePlugin(JLatexMathPlugin.create(textSize, builder -> builder.inlinesEnabled(true)))
                        .usePlugin(GlideImagesPlugin.create(appContext))
                        .build();
            } catch (Exception e) {
                markwon = Markwon.create(appContext);
            }
            markwonCache.put(appContext, markwon);
        }
        return markwon;
    }

    /**
     * 渲染Markdown文本到TextView
     */
    public static void render(Context context, TextView textView, String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            textView.setText("");
            return;
        }
        try {
            getMarkwon(context).setMarkdown(textView, preprocessLatex(markdown));
            // 链接可点击（TableAwareMovementMethod 同时保留表格横向滚动能力）
            textView.setMovementMethod(TableAwareMovementMethod.create());
        } catch (Exception e) {
            // 如果渲染失败，直接显示原文
            textView.setText(markdown);
        }
    }

    /**
     * LaTeX 预处理：jlatexmath 不支持 \tag/\tag*（amsmath 宏），
     * 降级为行末括号标签 \quad(\text{内容})，避免整条公式渲染失败
     * 注：对代码块中的字面 \tag 文本也会生效（出现概率极低，可接受）
     */
    private static String preprocessLatex(String markdown) {
        return markdown.replaceAll("\\\\tag\\*?\\{([^}]*)\\}", "\\\\quad(\\\\text{$1})");
    }

    /**
     * 渲染 Markdown 到 WebView（支持表格 / 代码高亮 / LaTeX 公式，背景跟随 App 主题）
     * 加载 assets/markdown/render.html，通过 JS 注入内容渲染，
     * baseUrl 使用服务端地址以便帖子内相对图片路径（/images/...）正常加载
     *
     * @param webView  已配置的 WebView（调用方需启用 JavaScript）
     * @param baseUrl  服务端基础地址（如 http://192.168.x.x:2080/），用于解析相对图片
     * @param markdown Markdown 内容
     */
    public static void renderWebView(WebView webView, String baseUrl, String markdown) {
        if (webView == null) return;
        try {
            String template = renderTemplate;
            if (template == null) {
                template = loadAsset(webView.getContext(), "markdown/render.html");
                renderTemplate = template;
            }
            webView.loadDataWithBaseURL(baseUrl, template, "text/html", "utf-8", null);
            // 等待页面加载后注入主题与内容并渲染
            final String md = markdown == null ? "" : markdown;
            final String theme = isDarkMode(webView.getContext()) ? "dark" : "light";
            webView.postDelayed(() -> webView.loadUrl(
                    "javascript:setTheme('" + theme + "');renderMarkdown(" + jsonEscape(md) + ")"), 150);
        } catch (Exception e) {
            // WebView 渲染失败时回退：不显示内容（保持空白即可，避免崩溃）
            webView.postDelayed(() -> webView.loadUrl("javascript:document.body.innerHTML=''"), 100);
        }
    }

    /**
     * 判断当前是否深色模式（AppCompatDelegate 应用后的实际配置）
     */
    private static boolean isDarkMode(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * 读取 assets 文件内容
     */
    private static String loadAsset(Context context, String path) throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getAssets().open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * JS 字符串转义（防注入）
     */
    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * 将Markdown转换为纯文本（用于列表预览）
     */
    public static String toPlainText(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        // 移除Markdown语法，保留纯文本
        String plain = markdown
                // 移除标题标记
                .replaceAll("#{1,6}\\s*", "")
                // 移除粗体/斜体
                .replaceAll("[*_]{1,3}([^*_]+)[*_]{1,3}", "$1")
                // 移除链接，保留文字
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                // 移除图片
                .replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "[图片]")
                // 移除代码块标记
                .replaceAll("```[\\s\\S]*?```", "[代码]")
                // 移除行内代码
                .replaceAll("`([^`]+)`", "$1")
                // 移除引用标记
                .replaceAll("^>\\s*", "")
                // 移除列表标记
                .replaceAll("^[*+-]\\s+", "")
                .replaceAll("^\\d+\\.\\s+", "")
                // 移除水平线
                .replaceAll("^[-*_]{3,}\\s*$", "")
                // 移除LaTeX行内公式
                .replaceAll("\\$([^$]+)\\$", "[公式]")
                // 移除LaTeX块级公式
                .replaceAll("\\$\\$[^$]*\\$\\$", "[公式]")
                .trim();

        // 限制预览长度
        if (plain.length() > 200) {
            plain = plain.substring(0, 200) + "...";
        }

        return plain;
    }
}