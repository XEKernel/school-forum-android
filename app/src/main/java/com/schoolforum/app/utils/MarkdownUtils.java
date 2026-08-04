package com.schoolforum.app.utils;

import android.content.Context;
import android.widget.TextView;

import java.util.WeakHashMap;

import io.noties.markwon.Markwon;
import io.noties.markwon.image.glide.GlideImagesPlugin;

/**
 * Markdown渲染工具类
 * 使用 Markwon 库渲染 Markdown
 * 使用 WeakHashMap 缓存 Markwon 实例，避免内存泄漏
 */
public class MarkdownUtils {
    // 使用 WeakHashMap 缓存 Markwon 实例，Context 被销毁时自动释放
    private static final WeakHashMap<Context, Markwon> markwonCache = new WeakHashMap<>();

    /**
     * 获取Markwon实例（基于 Context 缓存）
     */
    public static Markwon getMarkwon(Context context) {
        // 使用 ApplicationContext 避免Activity泄漏
        Context appContext = context.getApplicationContext();
        Markwon markwon = markwonCache.get(appContext);
        if (markwon == null) {
            try {
                markwon = Markwon.builder(appContext)
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
            getMarkwon(context).setMarkdown(textView, markdown);
        } catch (Exception e) {
            // 如果渲染失败，直接显示原文
            textView.setText(markdown);
        }
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