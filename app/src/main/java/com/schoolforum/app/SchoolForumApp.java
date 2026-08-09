package com.schoolforum.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * 应用入口：启动时应用用户保存的主题偏好
 * 主题偏好值：system（跟随系统）/ light（浅色）/ dark（深色）
 * 通过 AppCompatDelegate.setDefaultNightMode 配合
 * values / values-night 资源限定符实现明暗切换
 */
public class SchoolForumApp extends android.app.Application {

    public static final String PREF_NAME = "app_prefs";
    public static final String KEY_THEME = "theme_preference";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    @Override
    public void onCreate() {
        super.onCreate();
        applyThemePreference(getThemePreference(this));
    }

    /**
     * 读取主题偏好
     */
    public static String getThemePreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, THEME_SYSTEM);
    }

    /**
     * 保存主题偏好
     */
    public static void saveThemePreference(Context context, String theme) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME, theme).apply();
    }

    /**
     * 应用主题（设置夜间模式）
     */
    public static void applyThemePreference(String theme) {
        if (THEME_LIGHT.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (THEME_DARK.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
