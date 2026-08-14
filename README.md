# QuickTools

基于 **Kotlin Multiplatform + Compose Multiplatform** 的桌面开发者工具箱,目前是 Desktop-only(单一 `jvm()` target),但要求真正跨 Windows / macOS / Linux 运行,不只兼容 Windows。

这个项目有两个目的:

1. 学习 Kotlin Multiplatform / Compose Desktop 这套现代跨平台开发方式——这是我的第一个 KMP 实战项目。
2. 把旧项目 `adblistener`(Java + JavaFX + 手写 MVP 架构的 ADB 日志监听工具)的功能迁移过来,用现代架构重新实现一遍。

> 项目由本人全程亲自编码,AI(Claude / Codex)只承担教学与代码评审的角色,不代笔业务逻辑,详见 [`.ai-context/public-agent-rules.md`](.ai-context/public-agent-rules.md)。

## 技术栈

- Kotlin 2.4.10 + Kotlin Multiplatform(当前仅 `jvm()` target)
- Compose Multiplatform 1.11.1 + Material3(alpha)
- androidx-lifecycle-viewmodel-compose / lifecycle-runtime-compose(MVVM,ViewModel + StateFlow)
- kotlinx-coroutines(含 `coroutines-swing`,桌面协程调度)
- Gradle 9.1.0,仓库走国内镜像(阿里云,见 [`settings.gradle.kts`](settings.gradle.kts))

## 项目结构

```
shared/                — 跨平台共享代码
  commonMain/           纯 Kotlin 业务逻辑(不依赖 JVM 特有 API)
  jvmMain/               桌面专属实现(ADB 进程调用、DES3 加解密等依赖 java.*/javax.* 的部分)
desktopApp/             — 桌面应用入口(main.kt、compose.desktop 打包配置)
.ai-context/            — 项目文档与双模型(Claude/Codex)协作配置,见下方"了解更多"
```

## 快速开始

```bash
# 运行桌面应用
./gradlew :desktopApp:run

# 热重载
./gradlew :desktopApp:hotRun --auto

# 运行测试
./gradlew :shared:jvmTest
```

推荐使用 **IntelliJ IDEA**(而非 Android Studio)——当前只有 Desktop target,IntelliJ + Kotlin Multiplatform 插件是 JetBrains 官方对 Compose Desktop 的推荐组合。

## 当前状态

项目脚手架已搭建完毕,功能开发尚未开始。整体架构方案(可插拔工具框架、启动器+独立窗口的两层 UI、主题系统)已经确定,记录在路线图文档中,正在按 Phase 0a → 0b 的节奏逐步实现。

## 了解更多

- [`.ai-context/PROJECT.md`](.ai-context/PROJECT.md) — 项目概览、技术栈细节、构建命令
- [`.ai-context/docs/feature/roadmap.md`](.ai-context/docs/feature/roadmap.md) — 完整路线图(架构设计 + UI 设计 + 分阶段计划)
- [`.ai-context/docs/feature/adblistener-migration-map.md`](.ai-context/docs/feature/adblistener-migration-map.md) — 旧项目 `adblistener` 的功能迁移映射
- [Kotlin Multiplatform 官方文档](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
