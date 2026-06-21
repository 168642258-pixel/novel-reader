# 项目记忆 - 轻阅小说阅读器

## 项目概览
安卓小说阅读 App，路径：C:\Users\Administrator\Desktop\小说app
- 技术栈：Kotlin + Jetpack Compose(Material3) + Room + KSP + jsoup
- 包名 com.novel.reader，minSdk 24 / compileSdk 34 / JDK 17
- Gradle 版本目录管理依赖：gradle/libs.versions.toml

## 架构
- data/：Book 实体、BookDao、AppDatabase、BookRepository、ReadingPrefs(SharedPreferences)
- parser/：TxtParser(GBK/UTF-8探测+正则分章)、EpubParser(ZIP+XmlPullParser+jsoup)
- ui/library/：书架列表+导入+删除
- ui/reader/：三种阅读模式(左右/上下翻页/连续滚动)+主题+字号+章节导航
- ui/theme/：浅深色 Material3 主题

## 编译运行
- 本机：用 Android Studio 打开项目根目录，Gradle 同步后 Run。命令行：gradlew.bat assembleDebug（需本机 JDK17+Android SDK）
- 云端（用户无 Android Studio）：GitHub Actions 工作流 .github/workflows/build.yml，push 后在 Actions 页面下载 Artifacts
- Gradle Wrapper 已补全：gradlew/gradlew.bat/gradle-wrapper.jar + properties(Gradle 8.9)
- push-to-github.sh 一键初始化 git 并推送到 GitHub 触发构建
- AGP 8.5.2 + Gradle 8.9 + Kotlin 2.0.20 + Compose BOM 2024.09.03

## 约定
- 阅读偏好键：reading_mode/dark_theme(-1跟随系统)/font_size/line_height
- 阅读器菜单显隐控制沉浸式 systemBars，内容仅用 displayCutout 内边距
