package com.schoolforum.app.utils;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 文件日志工具类 - 将日志保存到文件中
 * 使用后台 HandlerThread 异步写入，避免阻塞 UI 线程
 */
public class FileLogger {
    private static final String LOG_DIR = "school_forum_logs";
    private static final String LOG_FILE = "debug.log";
    private static FileLogger instance;

    private final File logFile;
    private final SimpleDateFormat dateFormat;

    // 后台写日志的线程和 Handler
    private final HandlerThread writerThread;
    private final Handler writerHandler;

    // 持久打开的 BufferedWriter，避免每次写都开关文件
    private BufferedWriter bufferedWriter;

    private FileLogger(Context context) {
        File logDir = new File(context.getExternalFilesDir(null), LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        logFile = new File(logDir, LOG_FILE);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

        // 清空旧日志并打开持久 BufferedWriter
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(logFile, false));
        } catch (IOException e) {
            Log.e("FileLogger", "Failed to open log file", e);
        }

        // 启动后台写日志线程
        writerThread = new HandlerThread("FileLogger-Writer");
        writerThread.start();
        writerHandler = new Handler(writerThread.getLooper());
    }

    public static synchronized FileLogger getInstance(Context context) {
        if (instance == null) {
            instance = new FileLogger(context.getApplicationContext());
        }
        return instance;
    }

    public void d(String tag, String message) {
        enqueueLog("DEBUG", tag, message);
        Log.d(tag, message);
    }

    public void e(String tag, String message) {
        enqueueLog("ERROR", tag, message);
        Log.e(tag, message);
    }

    public void w(String tag, String message) {
        enqueueLog("WARN", tag, message);
        Log.w(tag, message);
    }

    public void i(String tag, String message) {
        enqueueLog("INFO", tag, message);
        Log.i(tag, message);
    }

    /**
     * 将日志任务提交到后台线程队列，不阻塞调用方线程
     */
    private void enqueueLog(String level, String tag, String message) {
        // 提前格式化时间戳（在调用方线程，避免时序混乱）
        final String timestamp = dateFormat.format(new Date());
        final String line = String.format("[%s] [%s] %s: %s\n", timestamp, level, tag, message);

        writerHandler.post(() -> writeToFile(line));
    }

    /**
     * 实际写入操作，运行在后台线程
     */
    private synchronized void writeToFile(String line) {
        if (bufferedWriter == null) return;
        try {
            bufferedWriter.write(line);
            bufferedWriter.flush();
        } catch (IOException e) {
            Log.e("FileLogger", "Failed to write log", e);
        }
    }

    public String getLogFilePath() {
        return logFile.getAbsolutePath();
    }

    public String readLogs() {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(logFile));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            return "Failed to read logs: " + e.getMessage();
        }
    }

    /**
     * 关闭日志（应在 Application.onTerminate 或不再需要时调用）
     */
    public synchronized void close() {
        try {
            if (bufferedWriter != null) {
                bufferedWriter.flush();
                bufferedWriter.close();
                bufferedWriter = null;
            }
        } catch (IOException e) {
            Log.e("FileLogger", "Failed to close log file", e);
        }
        writerThread.quitSafely();
    }
}
