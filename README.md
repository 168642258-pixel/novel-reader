# 轻阅 · 轻量化安卓小说阅读器

一个用 **Kotlin + Jetpack Compose** 编写的轻量级本地小说阅读 App，支持 TXT / EPUB 导入，专注简洁纯粹的阅读体验。

## 功能特性

| 功能 | 说明 |
|------|------|
| 📖 多种翻页方式 | 左右翻页 / 上下翻页 / 上下连续滚动，阅读中可随时切换 |
| 🌗 浅色 / 深色 / 护眼米黄 | 三种阅读底色一键切换，深色模式夜间护眼 |
| 📂 本地导入 | 支持 `.txt`（自动探测 GBK/UTF-8 编码、正则分章）与 `.epub`（ZIP 解包 + jsoup 正文提取） |
| 📱 全面屏适配 | 沉浸式全屏阅读，自动避开挖孔屏 / 刘海区域，内容不遮挡 |
| 🔤 字号 / 行距可调 | 12–36sp 字号无级调节，行距自由设置 |
| 🧭 章节导航 | 章节进度条拖拽跳转、上下章按钮、阅读进度自动保存 |
| 📚 书架管理 | 书籍列表、长按删除，按最近阅读排序 |
| 🔗 系统关联 | 从文件管理器直接用本 App 打开 txt/epub |

## 项目结构

```
小说app/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml          # 全屏、挖孔适配、文件关联
│       ├── java/com/novel/reader/
│       │   ├── MainActivity.kt          # 入口 + 导航 + edge-to-edge
│       │   ├── data/                    # Room 数据库、书籍实体、阅读偏好
│       │   │   ├── Book.kt
│       │   │   ├── BookDao.kt
│       │   │   ├── AppDatabase.kt
│       │   │   ├── BookRepository.kt
│       │   │   └── ReadingPrefs.kt
│       │   ├── parser/                  # TXT / EPUB 解析
│       │   │   ├── BookParseResult.kt
│       │   │   ├── TxtParser.kt
│       │   │   └── EpubParser.kt
│       │   └── ui/
│       │       ├── theme/               # 颜色与浅/深色主题
│       │       │   ├── Color.kt
│       │       │   └── Theme.kt
│       │       ├── library/             # 书架界面
│       │       │   ├── LibraryScreen.kt
│       │       │   └── LibraryViewModel.kt
│       │       └── reader/              # 阅读器界面
│       │           ├── ReaderScreen.kt
│       │           ├── ReaderViewModel.kt
│       │           └── Pagination.kt
│       └── res/                         # 图标、主题、字符串资源
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```

## 如何编译运行

### 环境要求
- **Android Studio** Ladybug 及以上（或 IntelliJ + Android 插件）
- **JDK 17**
- Android SDK 34（compileSdk），最低支持 Android 7.0（minSdk 24）

### 步骤
1. 用 Android Studio 打开 `小说app` 目录（**File → Open** 选择该文件夹）。
2. 等待 Gradle 自动同步下载依赖（首次需联网）。
3. 连接安卓手机（开启 USB 调试）或启动模拟器。
4. 点击 **Run ▶** 编译安装。

> 本项目使用 Gradle 版本目录（`libs.versions.toml`）管理依赖，无需手动配置版本。

### 直接用命令行编译
```bash
# Windows（需本机已装 Android SDK）
gradlew.bat assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 不装 Android Studio 也能拿到 APK：GitHub Actions 云端构建

本机没装 JDK / Android SDK / Android Studio 也没关系，用 GitHub 的云端服务器免费编译。

### 一次性准备
1. 注册一个 GitHub 账号（https://github.com，免费）。
2. 在 GitHub 上新建一个空仓库（**New repository**），名字随意（例如 `novel-reader`），**不要**勾选添加 README / .gitignore / license，保持完全空。
3. 本机装 Git for Windows（https://git-scm.com/download/win，一路下一步）。

### 推送代码触发构建
项目根目录已经准备好：
- `.github/workflows/build.yml` —— 云端构建脚本
- `push-to-github.sh` —— 一键推送脚本

在项目文件夹里 **右键 → Git Bash Here**，执行：
```bash
bash push-to-github.sh
```
脚本会自动初始化 git、提交代码，然后提示你粘贴刚才创建的 GitHub 仓库地址，回车后自动推送。

> 首次推送时 Git 会弹窗要求登录 GitHub（用浏览器授权即可），之后不再询问。

### 下载 APK
1. 推送完成后，打开你的 GitHub 仓库网页，点击 **Actions** 标签。
2. 等待名为 **Build APK** 的工作流跑完（首次约 5–8 分钟，绿色 ✓ 表示成功）。
3. 点开这次运行，页面最下方 **Artifacts** 区域有 `novel-reader-debug-apk`，点击下载得到一个 zip，解压里面就是 `app-debug.apk`。
4. 把 apk 传到安卓手机安装即可。

### 以后改了代码怎么办
在项目里再次执行 `bash push-to-github.sh`（或直接 `git push`），GitHub 会自动重新构建，几分钟后又能下载新 APK。Public 仓库每月有 2000 分钟免费额度，私库也有 500 分钟，编译一个 APK 只用 5–8 分钟，完全够用。

## 使用说明

1. **导入书籍**：打开 App → 书架页右上角 **＋** → 选择本地 `.txt` 或 `.epub` 文件。
2. **阅读**：点击书架上的书籍进入阅读。
3. **切换模式**：阅读时点击屏幕中央唤出菜单 → 底部「左右翻页 / 上下翻页 / 连续滚动」。
   - 左右/上下翻页模式下，点击屏幕左 1/3 上一页、右 1/3 下一页、中间唤出菜单。
   - 连续滚动模式下，点击屏幕唤出菜单，滑到底部可切换上下章。
4. **切换主题 / 字号**：底部菜单的色块切换浅色/深色/护眼，＋/− 调整字号。
5. **全屏沉浸**：菜单隐藏后自动隐藏状态栏与导航栏，实现真正全屏阅读；上滑边缘可临时唤出系统栏。

## 技术栈

- Kotlin 2.0 + Jetpack Compose（Material3）
- Room（书架持久化）+ KSP
- Coroutines / Flow
- jsoup（EPUB 正文清洗）
- WindowInsetsController（沉浸式）+ displayCutout（挖孔屏适配）

## 许可

本项目仅供学习交流使用。
