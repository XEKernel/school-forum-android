package com.schoolforum.app.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 时间工具类
 */
@SuppressWarnings("unused")
public class TimeUtils {
    
    private static final SimpleDateFormat ISO_FORMAT;
    
    static {
        ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /**
     * 解析ISO时间字符串
     */
    public static Date parseIsoTime(String isoString) {
        if (isoString == null || isoString.isEmpty()) {
            return null;
        }
        
        try {
            return ISO_FORMAT.parse(isoString);
        } catch (ParseException e) {
            // 尝试其他格式
            try {
                SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
                altFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                return altFormat.parse(isoString);
            } catch (ParseException e2) {
                return null;
            }
        }
    }
    
    /**
     * 格式化相对时间（如：刚刚、5分钟前、昨天等）
     */
    public static String formatRelativeTime(String isoString) {
        Date date = parseIsoTime(isoString);
        if (date == null) {
            return "";
        }
        
        return formatRelativeTime(date);
    }
    
    /**
     * 格式化相对时间
     */
    public static String formatRelativeTime(Date date) {
        if (date == null) {
            return "";
        }
        
        long now = System.currentTimeMillis();
        long time = date.getTime();
        long diff = now - time;
        
        if (diff < 0) {
            return "刚刚";
        }
        
        // 1分钟内
        if (diff < 60 * 1000) {
            return "刚刚";
        }
        
        // 1小时内
        if (diff < 60 * 60 * 1000) {
            return (diff / (60 * 1000)) + "分钟前";
        }
        
        // 今天
        Date today = new Date(now);
        if (isSameDay(date, today)) {
            return formatTime(date);
        }
        
        // 昨天
        Date yesterday = new Date(now - 24 * 60 * 60 * 1000);
        if (isSameDay(date, yesterday)) {
            return "昨天 " + formatTime(date);
        }
        
        // 今年
        if (isSameYear(date, today)) {
            return formatDateShort(date);
        }
        
        // 更早
        return formatDateFull(date);
    }
    
    private static String formatTime(Date date) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
    }
    
    private static String formatDateShort(Date date) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date);
    }
    
    private static String formatDateFull(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }
    
    /**
     * 判断是否同一天
     */
    private static boolean isSameDay(Date date1, Date date2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) 
            && cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }
    
    /**
     * 判断是否同一年
     */
    private static boolean isSameYear(Date date1, Date date2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR);
    }
    
    /**
     * 格式化时间戳
     */
    public static String formatTimestamp(long timestamp) {
        return formatRelativeTime(new Date(timestamp));
    }
}
