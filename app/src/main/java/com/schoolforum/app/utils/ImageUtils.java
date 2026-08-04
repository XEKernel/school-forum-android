package com.schoolforum.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 图片处理工具类
 */
public class ImageUtils {
    private static final String TAG = "ImageUtils";
    
    // 最大图片尺寸（宽或高）
    private static final int MAX_DIMENSION = 1920;
    // 最大文件大小（1MB）
    private static final long MAX_FILE_SIZE = 1024 * 1024;
    // 压缩质量
    private static final int COMPRESS_QUALITY = 85;
    
    /**
     * 压缩图片
     * @param context 上下文
     * @param uri 图片URI
     * @return 压缩后的临时文件
     */
    public static File compressImage(Context context, Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            
            // 先获取原始图片的尺寸
            InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "无法打开图片流");
                return null;
            }
            
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            String mimeType = options.outMimeType;
            
            Log.d(TAG, "原始图片尺寸: " + originalWidth + "x" + originalHeight);
            
            // 计算采样率
            int sampleSize = calculateSampleSize(originalWidth, originalHeight, MAX_DIMENSION);
            
            // 解码图片（使用采样率）
            options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            inputStream = resolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            if (bitmap == null) {
                Log.e(TAG, "无法解码图片");
                return null;
            }
            
            Log.d(TAG, "解码后尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            
            // 如果图片仍然太大，进一步缩放
            bitmap = scaleBitmap(bitmap, MAX_DIMENSION);
            
            // 读取EXIF信息并旋转
            bitmap = rotateImageIfRequired(context, bitmap, uri);
            
            // 创建临时文件
            File cacheDir = new File(context.getCacheDir(), "upload");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            
            String extension = getExtension(mimeType);
            File outputFile = new File(cacheDir, "img_" + System.currentTimeMillis() + extension);
            
            // 压缩并保存
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            Bitmap.CompressFormat format = getCompressFormat(mimeType);
            
            // 尝试不同质量进行压缩
            int quality = COMPRESS_QUALITY;
            bitmap.compress(format, quality, outputStream);
            outputStream.close();
            
            // 如果文件仍然太大，降低质量重新压缩
            while (outputFile.length() > MAX_FILE_SIZE && quality > 10) {
                quality -= 10;
                outputStream = new FileOutputStream(outputFile);
                bitmap.compress(format, quality, outputStream);
                outputStream.close();
                Log.d(TAG, "降低质量重新压缩: quality=" + quality + ", size=" + outputFile.length());
            }
            
            // 如果压缩后还是太大，缩小尺寸
            if (outputFile.length() > MAX_FILE_SIZE) {
                int newDimension = (int) (MAX_DIMENSION * 0.8);
                Bitmap scaledBitmap = scaleBitmap(bitmap, newDimension);
                outputStream = new FileOutputStream(outputFile);
                scaledBitmap.compress(format, COMPRESS_QUALITY, outputStream);
                outputStream.close();
                scaledBitmap.recycle();
                Log.d(TAG, "缩小尺寸重新压缩: dimension=" + newDimension);
            }
            
            bitmap.recycle();
            
            Log.d(TAG, "最终文件大小: " + formatFileSize(outputFile.length()));
            return outputFile;
            
        } catch (Exception e) {
            Log.e(TAG, "压缩图片失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 计算采样率
     */
    private static int calculateSampleSize(int width, int height, int maxDimension) {
        int sampleSize = 1;
        
        if (width > maxDimension || height > maxDimension) {
            int halfWidth = width / 2;
            int halfHeight = height / 2;
            
            while ((halfWidth / sampleSize) >= maxDimension 
                    && (halfHeight / sampleSize) >= maxDimension) {
                sampleSize *= 2;
            }
        }
        
        return sampleSize;
    }
    
    /**
     * 缩放图片
     */
    private static Bitmap scaleBitmap(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap;
        }
        
        float scale;
        if (width > height) {
            scale = (float) maxDimension / width;
        } else {
            scale = (float) maxDimension / height;
        }
        
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    
    /**
     * 根据EXIF信息旋转图片
     */
    private static Bitmap rotateImageIfRequired(Context context, Bitmap bitmap, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return bitmap;
            
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 
                    ExifInterface.ORIENTATION_NORMAL);
            inputStream.close();
            
            int rotation = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
            }
            
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, 
                        bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return rotatedBitmap;
            }
        } catch (Exception e) {
            Log.e(TAG, "读取EXIF失败: " + e.getMessage());
        }
        
        return bitmap;
    }
    
    /**
     * 获取文件扩展名
     */
    private static String getExtension(String mimeType) {
        if (mimeType == null) return ".jpg";
        if (mimeType.contains("png")) return ".png";
        if (mimeType.contains("webp")) return ".webp";
        return ".jpg";
    }
    
    /**
     * 获取压缩格式
     */
    private static Bitmap.CompressFormat getCompressFormat(String mimeType) {
        if (mimeType != null && mimeType.contains("png")) {
            return Bitmap.CompressFormat.PNG;
        }
        if (mimeType != null && mimeType.contains("webp")) {
            return Bitmap.CompressFormat.WEBP;
        }
        return Bitmap.CompressFormat.JPEG;
    }
    
    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}
