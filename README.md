# CherryClip · 樱桃剪贴板

参照 [CherryHQ/cherry-studio-app](https://github.com/CherryHQ/cherry-studio-app) 的现代移动端设计体系（Geist 色彩令牌、双主题、胶囊控件、BottomSheet）实现的 Android 剪贴板工具，功能全面、上手极简。

- 包名：`com.cherryclip.app`
- 技术栈：Kotlin · Jetpack Compose · Material3 · SQLite（原生 SQLiteOpenHelper）· Gson · ZXing
- 最低版本：Android 8.0（API 26），编译 SDK 36

---

## 一、构建

```bash
# 环境要求：JDK 17、Android SDK（platform 36 / build-tools 34+）
# 首次构建前，复制 local.properties.example 为 local.properties 并填写 SDK 路径
#   sdk.dir=C\:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk

gradlew.bat assembleDebug          # 输出：app/build/outputs/apk/debug/app-debug.apk
gradlew.bat testDebugUnitTest      # 单元测试（10 个用例）
gradlew.bat installDebug           # 连接设备后直接安装
```

已构建好的安装包：**`CherryClip-debug.apk`**（本目录根下）。

### 关于中文路径 & 单元测试

本工程位于含中文的路径下，因此 `gradle.properties` 中有两处必要配置，**请勿删除**：

| 配置 | 作用 |
|---|---|
| `android.overridePathCheck=true` | 关闭 AGP「路径含非 ASCII 字符」的拦截 |
| `org.gradle.jvmargs` 中**不加** `-Dfile.encoding=UTF-8` | 守护进程会把测试 worker 的 classpath 写入 `@argfile`，Windows JVM 启动器按本地编码（中文系统 GBK）读取它。若守护进程用 UTF-8 写入，中文路径会被读成乱码，JUnit 会报 `ClassNotFoundException`。 |

源码文件编码由 Kotlin / javac 各自的 UTF-8 选项保证，不受影响。

---

## 二、功能一览

### 1. 多渠道采集（适配 Android 10+ 后台剪贴板限制）
| 方式 | 入口 | 说明 |
|---|---|---|
| 切回应用自动同步 | `MainActivity.onWindowFocusChanged` | 回到应用即读取系统剪贴板（文本 / 图片） |
| 快捷设置磁贴 | `ClipboardTileService` + `QuickPasteActivity` | 下拉控制中心添加「CherryClip」，一键呼出半屏浮窗，点选即复制并返回 |
| 选词即存 | `ShareCaptureActivity`（`ACTION_PROCESS_TEXT`） | 任意 App 长按选词 →「存入 CherryClip」 |
| 系统分享 | `ShareCaptureActivity`（`ACTION_SEND`） | 分享文本 / 图片到 CherryClip 永久归档 |
| 常驻通知栏 | `ClipboardMonitorService`（前台服务） | 可选开启，下拉随时同步 / 呼出 |

### 2. 智能识别与富预览
`ContentClassifier` 自动归类并给出专属渲染：

- **链接**：域名 + 「打开」快捷按钮（去 `www.` 归一化）
- **验证码**：抽取 4–8 位数字，渲染「复制验证码 482910」绿色药丸
- **颜色**：`#RGB` / `#RRGGBB` / `#AARRGGBB` / `rgb()/rgba()` → 归一化 HEX + 实时色块
- **代码 / JSON**：等宽字体代码块容器，标注语言（JSON / Kotlin / JS / Python / SQL / Shell …）
- **邮箱 / 电话**：一键发信 / 拨号
- **图片**：缩略图预览，可回写系统剪贴板
- **敏感内容**：命中 API Key、`password:` 模式、银行卡号时打锁标，并可设置自动跳过

### 3. 整理与批量操作
- 点击即复制、长按进入多选；置顶、自定义 `#标签`、全文 / 标签 / 类型筛选与搜索
- 多选合并复制：换行 / 空行 / 空格 / 逗号 / 无分隔，可选「仅复制」或「另存为新条」
- 单条详情页：编辑标题、内容、标签；分享；存为常用片段；删除；生成二维码

### 4. 智能文本工具箱（`TextTransformers`）
清理空行空格 · **去除链接追踪参数**（`utm_*`、`fbclid`、`gclid`、`spm`、`igshid` 等 20+ 项）· JSON 美化 / 压缩 · Base64 编解码 · URL 编解码 · 大小写 / 首字母大写 · 提取链接 / 邮箱 / 数字

### 5. 常用片段与动态模板（`SnippetsScreen`）
分类 + 收藏 + 使用次数统计，复制时展开变量：
`{{date}}` `{{time}}` `{{datetime}}` `{{weekday}}` `{{clipboard}}` `{{uuid}}`

### 6. 隐私锁与数据管理（`SettingsScreen` / `AppLockScreen`）
- 4–8 位数字 **PIN 应用锁**（SHA-256 存储），主页与快捷浮窗均受保护
- 自动去重、跳过敏感内容、复制触感反馈
- 历史数量上限（100 / 500 / 1000 / 不限；置顶不受限）、按天自动清理、一键清空未置顶
- **JSON 全量备份导出 / 合并恢复**（SAF 文件选择器）

---

## 三、目录结构

```
剪切板/
├── build.gradle.kts                 根构建脚本（AGP 9.1.0 / Compose 插件 2.4.20）
├── settings.gradle.kts
├── gradle.properties                ⚠️ 中文路径相关配置，勿删
├── local.properties                 SDK 路径（不入库，见 .example）
├── gradlew / gradlew.bat / gradle/  Gradle 9.5.0 Wrapper
├── CherryClip-debug.apk             已构建安装包
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/cherryclip/app/
        │   │   ├── CherryClipApp.kt               Application + 通知渠道
        │   │   ├── MainActivity.kt                主界面、导航、全部 Sheets 编排
        │   │   ├── QuickPasteActivity.kt          磁贴/通知呼出的半屏快捷浮窗
        │   │   ├── ShareCaptureActivity.kt        分享 / 选词 静默入库
        │   │   ├── clipboard/
        │   │   │   ├── ClipboardHelper.kt         读写剪贴板、分享、打开链接/拨号/发信
        │   │   │   ├── ClipboardMonitorService.kt 前台监听服务
        │   │   │   └── ClipboardTileService.kt    快捷设置磁贴
        │   │   ├── data/
        │   │   │   ├── ClipDatabaseHelper.kt      SQLite 表结构（clips / snippets）
        │   │   │   └── ClipRepository.kt          仓库层：StateFlow、去重、留存策略、备份
        │   │   ├── model/
        │   │   │   ├── ClipModels.kt              数据模型与枚举
        │   │   │   ├── ContentClassifier.kt       内容智能识别
        │   │   │   ├── TextTransformers.kt        文本变换与模板变量
        │   │   │   └── QrCodeGenerator.kt         ZXing 二维码
        │   │   └── ui/
        │   │       ├── theme/CherryTokens.kt      设计令牌（浅色 / 深色配色、尺寸）
        │   │       ├── theme/Theme.kt             主题装配 + 系统栏样式
        │   │       ├── components/                品牌标、搜索胶囊、筛选 Chip、Toast、剪贴卡、各类 Sheet
        │   │       └── screens/                   主页 / 常用片段 / 设置 / PIN 锁屏
        │   └── res/                               图标（自适应）、主题、字符串、FileProvider
        └── test/java/com/cherryclip/app/          单元测试（识别 / 变换 / 二维码）
```

---

## 四、设计体系对照

| 角色 | 浅色 | 深色 | 参考令牌 |
|---|---|---|---|
| 画布背景 | `#F5F5F5` | `#000000` | `--background` |
| 卡片表面 | `#FFFFFF` | `#171717` | `--card` |
| 主文本 | `#171717` | `#F0F0F0` | `--foreground` |
| 主操作色 | `#128846` | `#28C96D` | `--primary`（green-900） |
| 品牌红 | `#FF5757` | `#FF5757` | `--brand` |
| 边框 / 强边框 | `rgba(0,0,0,.08)` | `rgba(255,255,255,.10)` | `--border` / `--border-strong` |
| 分类标签 | blue / green / amber / red / violet 各 100 底 + 900 字 | 深色对应档 | `--tag-*` |
| 圆角 | 6 / 10 / 14 / 16 / 24dp | 同 | `--radius-*` |

---

## 五、已做验证

| 项目 | 结果 |
|---|---|
| `gradlew.bat assembleDebug` | BUILD SUCCESSFUL，产出 `app-debug.apk`（约 18.4 MB） |
| `gradlew.bat testDebugUnitTest` | 10/10 通过（ContentClassifier 5、TextTransformers 4、QrCodeGenerator 1） |
| 迁移前原始位置构建 | 同样通过，功能一致 |

---

## 六、常见问题

**Q：Android 10+ 后台复制的内容抓不到？**
A：系统限制后台读取剪贴板。请用「切回应用自动同步」、控制中心磁贴、选词菜单或系统分享，任一方式都不需要 Root 或无障碍权限。

**Q：通知栏常驻 / 磁贴没反应？**
A：需要授予通知权限（Android 13+ 会弹窗申请）；磁贴需在控制中心编辑页手动添加一次。

**Q：备份能跨机恢复吗？**
A：文本、片段与图片元数据可以；图片原文件不入 JSON，跨机恢复后仅保留记录与尺寸信息。
