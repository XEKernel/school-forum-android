# 校园论坛 Android 客户端

基于 WebView 的 Android 客户端应用，封装校园论坛 Web 端。

## 项目结构

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/schoolforum/app/
│   │   │   ├── MainActivity.java          # 主活动
│   │   │   └── FilePickerActivity.java    # 文件选择
│   │   ├── res/
│   │   │   ├── layout/                    # 布局文件
│   │   │   ├── values/                    # 资源值
│   │   │   ├── drawable/                  # 绘图资源
│   │   │   ├── mipmap-*/                  # 图标资源
│   │   │   └── xml/                       # XML配置
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 8 或更高版本
- Android SDK 34
- Gradle 8.2

## 配置服务器地址

在 `MainActivity.java` 中修改 `BASE_URL`：

```java
// 模拟器访问本机
private static final String BASE_URL = "http://10.0.2.2:2080";

// 真机访问局域网（修改为实际IP）
// private static final String BASE_URL = "http://192.168.x.x:2080";
```

## 构建步骤

### 使用 Android Studio

1. 打开 Android Studio
2. 选择 `Open an Existing Project`
3. 选择 `android` 文件夹
4. 等待 Gradle 同步完成
5. 点击 `Run` 按钮运行

### 使用命令行

```bash
# 进入项目目录
cd android

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 生成的 APK 位于:
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
```

## 功能特性

- WebView 容器封装
- 下拉刷新
- 文件上传（图片选择、拍照）
- 文件下载
- 消息通知
- 震动反馈
- 剪贴板操作
- 分享功能
- 原生 API 桥接

## 原生 API 接口

网页可以通过 `window.NativeApp` 调用原生功能：

```javascript
// 检测是否在App中
if (window.NativeApp) {
    console.log('运行在原生App中');
    
    // 显示Toast
    NativeApp.showToast('消息');
    
    // 震动
    NativeApp.vibrate(100);
    
    // 复制到剪贴板
    NativeApp.copyToClipboard('文本');
    
    // 分享
    NativeApp.share('标题', '内容', 'URL');
    
    // 获取设备信息
    const info = NativeApp.getDeviceInfo();
}
```

## 权限说明

| 权限 | 用途 |
|-----|------|
| INTERNET | 网络访问 |
| ACCESS_NETWORK_STATE | 网络状态 |
| READ_EXTERNAL_STORAGE | 读取文件 |
| READ_MEDIA_IMAGES | 读取图片（Android 13+） |
| CAMERA | 拍照 |
| VIBRATE | 震动反馈 |
| POST_NOTIFICATIONS | 推送通知（Android 13+） |

## 自定义图标

替换 `res/mipmap-*/` 目录下的图标文件，或使用 Android Studio 的 Image Asset Studio：

1. 右键点击 `res` 目录
2. 选择 `New > Image Asset`
3. 配置图标并生成各尺寸版本

## 混淆配置

Release 构建已启用 ProGuard 混淆，配置见 `app/proguard-rules.pro`。

## 版本更新

修改 `app/build.gradle` 中的版本信息：

```groovy
versionCode 1      // 版本号（整数）
versionName "1.0.0" // 版本名（字符串）
```

## 注意事项

1. 确保服务器已启动并可访问
2. 真机调试需要开启 USB 调试
3. HTTPS 环境需要配置证书
4. 生产环境建议使用 HTTPS
