package com.schoolforum.app.utils;

import android.os.Build;
import android.text.TextUtils;

/**
 * 设备信息工具类
 */
public class DeviceUtils {
    
    /**
     * 获取设备信息字符串
     * @return 如 "iPhone 15 Pro" 或 "Samsung Galaxy S23" 或 "Windows 11"
     */
    public static String getDeviceInfo() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        
        // 清理制造商名称
        if (!TextUtils.isEmpty(manufacturer)) {
            manufacturer = capitalize(manufacturer);
        }
        
        // 如果型号已经包含制造商名称，只返回型号
        if (!TextUtils.isEmpty(model) && model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return model;
        }
        
        // 否则返回 "制造商 型号"
        if (!TextUtils.isEmpty(manufacturer) && !TextUtils.isEmpty(model)) {
            return manufacturer + " " + model;
        }
        
        // 如果只有型号
        if (!TextUtils.isEmpty(model)) {
            return model;
        }
        
        return "Android Device";
    }
    
    /**
     * 获取简短的设备信息
     * @return 如 "iPhone" 或 "Samsung"
     */
    public static String getShortDeviceInfo() {
        String manufacturer = Build.MANUFACTURER;
        if (!TextUtils.isEmpty(manufacturer)) {
            return capitalize(manufacturer);
        }
        return "Android";
    }
    
    /**
     * 首字母大写
     */
    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        StringBuilder result = new StringBuilder();
        for (char c : arr) {
            if (Character.isSpaceChar(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}
