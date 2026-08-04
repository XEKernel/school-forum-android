# 校园论坛 Android 客户端

校园论坛的 Android 原生客户端（Java + OkHttp），与服务端配套使用。

## 相关仓库

| 项目 | 仓库地址 |
|-----|---------|
| 服务端（含 Web 端） | https://github.com/XEKernel/school-forum |
| Android 客户端（本仓库） | https://github.com/XEKernel/school-forum-android |

> 服务端基于 Node.js + Express + MongoDB + Redis，Web 端为原生 HTML/CSS/JS；
> 本客户端通过 REST API 与服务端通信（BASE_URL 见下方配置）。

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
- JDK 21（AGP 9.1 构建必需）
- Android SDK 34
- Gradle 9.3+（由 wrapper 管理）

## 配置服务器地址

在 `app/build.gradle` 的 `BuildConfig` 中修改 `BASE_URL`：

```groovy
buildConfigField "String", "BASE_URL", "\"http://10.0.2.2:2080\""  // 模拟器访问本机
// 真机访问局域网：改为实际 IP，如 "http://192.168.x.x:2080"
```

## 构建步骤

### 使用 Android Studio

1. 打开 Android Studio
2. 选择 `Open an Existing Project`
3. 选择 `android` 文件夹（或本仓库根目录）
4. **Gradle JDK 必须选择 JDK 21**（JDK 26 与 AGP 9.1 的 jlink 不兼容）
5. 等待 Gradle 同步完成
6. 点击 `Run` 按钮运行

### 使用命令行

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 生成的 APK 位于:
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
```

## 功能特性

- 原生 Java 客户端（非 WebView 封装），OkHttp 网络层 + JWT 双 Token 认证
- 帖子发布/浏览（Markdown + LaTeX 渲染）、分类栏目
- 评论/嵌套回复（最多 6 层）、点赞、收藏
- 私信会话、通知消息（含系统群发消息）
- 用户关注/粉丝、拉黑、举报
- 登录图形验证码、Token 自动刷新拦截器
- 图片选择/拍照上传（压缩处理）

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

## 版本更新

修改 `app/build.gradle` 中的版本信息：

```groovy
versionCode 1      // 版本号（整数）
versionName "1.0.0" // 版本名（字符串）
```

## 注意事项

1. 确保服务端已启动并可访问（服务端启动流程见服务端仓库 README）
2. 真机调试需要开启 USB 调试
3. 生产环境建议使用 HTTPS
