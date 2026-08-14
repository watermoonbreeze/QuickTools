# PROJECT.md

## 项目概述
QuickTools 是基于 Kotlin Multiplatform + Compose Multiplatform 的桌面开发者工具箱。目标有两个:
1. 学习 Kotlin Multiplatform / Compose Desktop 的现代跨平台开发方式(用户本人的第一个 KMP 实战项目)。
2. 把 `D:\Company\Gitee\adblistener`(Java + JavaFX + 手写 MVP 架构的旧项目)里的功能迁移过来,用现代架构重做一遍。

## 协作模式(重要,优先于常规默认行为)
本项目由用户本人全程亲自编码实现,AI(Claude/Codex)的角色是**教学与辅助**,不是代笔。核心规则见 `public-agent-rules.md` 第一节,任何时候两者冲突以 `public-agent-rules.md` 为准。

## 技术栈
- Kotlin 2.4.10 + Kotlin Multiplatform(当前仅 `jvm()` target,Desktop-only;Android/iOS target 作为后续进阶练习,非当前范围)
- **"Desktop-only" ≠ "只兼容 Windows"**:`jvm()` 是单一 KMP target,但产物要能在 macOS / Windows / Linux 三个桌面系统上正确运行(`compose.desktop.nativeDistributions` 已配置 Dmg/Msi/Deb 三种产物)。开发机是 Windows,但写 `jvmMain` 代码时不能假设 Windows 特有行为——具体见 `docs/feature/adblistener-migration-map.md` 里"跨平台隐患"一节(尤其是 ADB 可执行文件解析、进程调用不能依赖宿主 shell 管道/`grep`、路径分隔符)。
- Compose Multiplatform 1.11.1 + Material3 1.11.0-alpha07(**alpha 版本**,IDE/编译器兼容性需留意,IntelliJ 报编译错误优先检查这里)
- androidx-lifecycle-viewmodel-compose / lifecycle-runtime-compose 2.11.0-beta01(用于 MVVM 架构,ViewModel + StateFlow)
- kotlinx-coroutines 1.11.0(含 coroutines-swing,用于桌面协程调度)
- Gradle 9.1.0,仓库走腾讯云镜像(见 `settings.gradle.kts`)

## 目录结构
- `shared/` — 跨平台共享代码
  - `commonMain` — 纯 Kotlin 业务逻辑(颜色/时间/编码转换等不依赖 JVM 特有 API 的部分)
  - `jvmMain` — 桌面专属实现(ADB 进程调用、DES3 加解密等依赖 `java.*`/`javax.*` 的部分)
- `desktopApp/` — 桌面应用入口(`main.kt`,`compose.desktop` 打包配置,`nativeDistributions` 已配置 Dmg/Msi/Deb)

## 构建/运行/测试命令
- 运行:`./gradlew :desktopApp:run`
- 热重载:`./gradlew :desktopApp:hotRun --auto`
- 测试:`./gradlew :shared:jvmTest`

## IDE
推荐 **IntelliJ IDEA**(非 Android Studio)——当前只有 Desktop target,用不到 Android Studio 的 Android 专属工具链(AVD/Profiler/Layout Inspector),IntelliJ + Kotlin Multiplatform 插件是 JetBrains 官方对 Compose Desktop 的推荐组合。

## 迁移来源
`D:\Company\Gitee\adblistener` — Java + JavaFX + 手写 MVP 的 ADB 日志监听工具。功能清单与新旧映射见 `docs/feature/adblistener-migration-map.md`。

## UI 设计参考
参考了 AYA(Android 设备调试工具)的界面布局。AYA 技术栈是 **Electron + TypeScript**(主进程 `import { app, BrowserWindow, session } from 'electron'`,渲染进程用 `custom-electron-titlebar` 做无边框自绘标题栏,`app.asar` 打包),本质是网页套壳。

**QuickTools 整体是"启动器 + 独立工具窗口"两层结构,不是照搬 AYA 的单一 Tab 壳**(2026-08-14 定的,原因见下)。启动器主窗口是 uTools/Raycast/Launchpad 式的工具网格 + 搜索;点击某个工具会新开一个独立 `Window` 渲染该工具。

AYA 的布局模式只在**工具自己的窗口内部**值得借鉴(不是整个 App 的壳):
- 专属工具条(筛选器/搜索/刷新)
- "当前设备"选择器——但只出现在真正需要设备的工具窗口里(ADB 日志/设备信息/截屏),不是全局常驻,因为颜色转换、DES3 这些工具跟"当前设备"毫无关系,不该被迫带着这个状态
- 内容区按数据类型选组件:概览用卡片网格,列表用表格,实时数据用图表,日志用等宽字体流式列表+颜色分级

QuickTools 用 Compose Desktop 复刻同类深色专业风格,但作为原生 JVM 应用运行,免去 Electron/Chromium 的启动开销与内存占用——这是选 KMP 而非 Electron 的实际优势,值得在学习过程中体会。

完整方案见 `docs/feature/roadmap.md`。

## 当前状态
2026-08-14:项目刚由 JetBrains 向导生成默认模板,尚未开始功能开发。已完成 myinit 双模型初始化。
