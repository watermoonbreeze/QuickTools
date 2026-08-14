# QuickTools 路线图

学习目标:第一个 Kotlin Multiplatform + Compose Desktop 实战项目,用户本人全程编码,AI 角色是教学/评审,不代笔核心逻辑(见 `public-agent-rules.md`)。

## 0. 项目愿景

一个"可插拔的桌面开发者工具框架",不是一个功能写死的单体应用:
- 内置若干工具(迁移自 `adblistener`),每个工具是独立模块,可以整合进主界面,也可以单独运行
- 长期目标:支持第三方以 JVM JAR 的形式扩展工具,再往后支持用 TypeScript/Python 脚本写工具(类似 uTools,但基于 GraalVM Polyglot 而不是内嵌浏览器,更轻量)
- 性能对标:比 Electron 类应用启动快、常驻内存低——这是选 Compose Desktop 而非 Electron/Tauri-web 的核心理由,要在实现细节上兑现,不能只停留在"编译成原生"这句口号上
- UI 是两层:一个 uTools/Raycast 式的**启动器主窗口**(工具网格 + 搜索,点击后新开独立窗口),各工具**自己的窗口**内部布局参考 AYA(Electron+TS 技术栈的 Android 调试工具)的思路——按数据类型选组件、专属工具条、设备选择器只在需要设备的工具窗口里出现,不是全局常驻。全部用 Compose Desktop 原生实现,不是套壳网页

## 1. 架构总览

```
QuickTools/
├── core-api/           纯 Kotlin(commonMain),定义 ToolPlugin 契约、ToolMetadata、ToolContext
├── core-ui/             Compose 共享 UI(commonMain):AYA 风格深色主题、DataTable/CardGrid/LogView/FilterBar/DeviceSelector 等通用组件
├── adb-core/            ADB 进程调用公共逻辑(jvmMain,协程 + Flow 重写版 CMD),供多个工具复用
├── settings-core/       跨平台本地设置持久化(窗口大小/当前设备/主题/已装插件),按 OS 区分存储路径
├── tools/
│   ├── device-info/     实现 ToolPlugin,依赖 core-api + core-ui + adb-core
│   ├── adb-logcat/
│   ├── screenshot/
│   ├── des3/
│   ├── color-converter/  纯逻辑可以下沉到 commonMain,不依赖 JVM 特有 API
│   ├── time-converter/   建议用 kotlinx-datetime 而非 java.text,真正跨平台
│   └── encode-converter/
└── desktopApp/          宿主:launcher 主窗口(工具网格+搜索)+ ToolWindowLauncher(点击工具→新开独立 Window 渲染该 ToolPlugin.Content)
```

**2026-08-14 结构性调整**:放弃"顶部 Tab 常驻 + 设备选择器全局"的壳,改成**启动器 + 独立窗口**模型(理由和最终设计见第 2 节)。`desktopApp` 不再持有一个"当前激活 Tab"的状态,而是持有"当前打开了哪些工具窗口"。

### ToolPlugin 契约(设计草图,不是最终代码——具体接口由你来定稿)

概念上,每个工具模块需要暴露:
- 一份 **元数据**:id、显示名、图标、分类、版本、作者——这份形状从第一天就按"以后要给 JAR 插件、脚本插件复用"来设计,即使 v1 只有内置模块在用
- 一个 **Composable 入口**:工具自己的 UI 内容,宿主壳只负责把它放进当前选中的 Tab 位置
- 一个 **依赖注入入口**(`ToolContext`):工具需要用到的公共服务(当前选中设备、`adb-core`、`settings-core`、日志)由宿主注入,工具本身不直接 new 这些依赖——方便后面把工具单独拆出去独立运行,也方便测试时替换掉真实依赖

### ToolPlugin 的渲染宿主:独立窗口,不是共享 Tab 壳

`ToolPlugin.Content` 这个 Composable 的运行宿主是**它自己的 `Window`**,不是嵌进主界面某个 Tab 位置。这一点决定了它和 Phase 0b 里"每个工具模块自带小 `main()` 独立运行"是**同一条代码路径**:启动器点击某个工具,做的事情就是"用同一份 `Content` 新开一个 `Window`";工具模块自己 `./gradlew :tools:des3:run` 直接启动,做的也是同一件事,只是入口不同。不需要额外维护一套"应用内导航/页面栈"逻辑。

设备选择器(以及任何工具专属的全局状态,比如 AYA 里那种常驻工具条)只存在于**需要它的工具自己的窗口**里(比如 `adb-logcat`、`device-info` 各自窗口顶部),不会出现在启动器主窗口或者跟设备无关的工具窗口(颜色转换、时间转换)里——这是这次调整解决的核心问题:之前的"顶部 Tab + 全局设备选择器"设计,是把 AYA(纯设备调试工具)的假设直接套过来了,但 QuickTools 大部分工具跟"当前设备"毫无关系。

多窗口生命周期需要在 Phase 1 设计清楚:关闭启动器窗口不应该杀掉已经打开的工具窗口(应用进程应该在还有任意窗口存活时保持运行,只有用户明确退出或所有窗口都关闭时才退出)——Compose Desktop 的 `application { }` 块原生支持多 `Window`,但退出时机需要显式管理,不是每个 `Window` 默认行为。

### 三层插件加载模型(分阶段实现,不是一次到位)

| 层级 | 加载方式 | 阶段 |
|---|---|---|
| 内置模块 | Gradle 编译期依赖,`desktopApp` 显式持有一份 `ToolPlugin` 列表 | Phase 1~7,主线 |
| 外部 JAR 插件 | `ServiceLoader` + 独立 `URLClassLoader` 扫描 `plugins/` 目录 | Phase 9(进阶) |
| 脚本插件(TS/Python) | GraalVM Polyglot(GraalJS 优先,GraalPy 后置),脚本只提供 `transform(input)->output` 纯函数,UI 由宿主根据 schema 自动生成 | Phase 10+(远期,先 JS 后 Python) |

JAR 插件没有沙箱(同进程运行),先约定"只装自己写的/信任来源的"。脚本插件走 Polyglot `Context` 天然支持权限隔离(限制文件系统/网络访问),是它长期更适合"给别人用"的地方。

### 导航:启动器 + 搜索,天然不怕工具变多

改成"启动器主窗口 + 独立工具窗口"之后,顶部 Tab 数量有限的问题不存在了——启动器本来就是网格/列表 + 搜索框(类似 uTools/Raycast/Launchpad),工具数量从 7 个涨到 70 个,无非是网格多滚几屏、搜索更有用,不需要额外设计"Tab 放不下怎么办"。这也是选启动器模型而不是 Tab 模型的一个附带好处,不是主要理由(主要理由是设备选择器不该是全局状态,见上一节)。

## 2. UI 设计

两层界面,分工不同,不共用一套壳:

### 2.1 启动器主窗口(uTools/Raycast/Launchpad 式,不是 AYA 式)

- 一个相对小的常驻窗口:顶部搜索框(模糊匹配工具名/分类)+ 下方工具网格(图标 + 名称,按分类分组或平铺)
- 点击一个工具 tile → 调用 `ToolWindowLauncher` 新开一个独立 `Window` 渲染该工具的 `ToolPlugin.Content`,启动器窗口本身不跳转、不替换内容
- 启动器只管"发现和启动工具",不持有任何具体工具的业务状态(不放设备选择器,不放任何工具专属工具条)
- 设置入口(主题切换/预设主题/自定义强调色、已装插件管理)放在启动器里,这是全局唯一需要的"壳级" UI
- 深色主题优先,但由于这层界面本身信息密度低(就是个网格+搜索),风格上可以比"工具内容窗口"更接近简洁的消费级 launcher,不用刻意堆专业感

### 2.2 各工具自己的窗口(内容布局可以参考 AYA)

工具窗口内部**才是** AYA 风格布局思路生效的地方——每个工具窗口打开后是一个独立、专业、信息密度高的工作区:

- 需要"当前设备"概念的工具(`adb-logcat`、`device-info`、`screenshot`)自己在窗口顶部放设备选择器 + 该工具专属的工具条(筛选器/搜索框/操作按钮),互不影响
- **内容区按数据类型选组件**:
  - 概览类信息 → 卡片网格(图标 + 标签 + 值),对应设备信息工具
  - 列表类数据 → 表格
  - 日志流 → `LazyColumn` + 等宽字体 + 按日志级别着色,**有界缓冲**(不能无限追加,这是老代码的坑)
  - 简单转换工具(颜色/时间/编码/DES3)→ 输入区 + 操作按钮 + 结果区的标准三段式布局,这套模板也是以后脚本插件"schema 自动生成表单 UI"要复用的样子
- 深色主题优先,专业信息密度高的配色,不是消费级 App 那种大留白风格
- 主题(预设/明暗/自定义强调色)在启动器里设置一次,全局生效到所有工具窗口——每个工具窗口不用重复做主题切换 UI
- **主题系统**:不是"写死一套深色配色"就完事,要支持:
  - 明暗切换(浅色/深色/跟随系统三态,系统态需要按 OS 检测当前明暗偏好,JVM 没有统一 API,得按 OS 分别处理——Windows 读注册表 `AppsUseLightTheme`,macOS 读 `defaults read -g AppleInterfaceStyle`,Linux 桌面环境差异大,可以先只做浅色/深色手动切换,"跟随系统"作为加分项)
  - **预设主题**:除了 AYA 风格深色,再定义 2~3 套预设配色(比如一套浅色、一套高对比度),本质是几组 `ColorScheme` 数据
  - **自定义主题**:用户可以选一个强调色(accent color),由它派生出完整 `ColorScheme`(Material3 有 `dynamicColorScheme`/调色板生成工具可以参考思路,不一定直接照搬 Android 的动态取色)
  - 主题定义为纯数据(`ThemeSpec`:一组颜色 token),预设主题和自定义主题走同一套渲染路径,自定义只是"运行时生成的一个 `ThemeSpec` 实例",不是单独一套代码
  - 当前选中的主题(含自定义参数)要能持久化,见 Phase 8 的 `settings-core`
- Compose Desktop 的标准 `Window` 默认走系统原生标题栏,不需要像 AYA 那样自己实现无边框窗口+自绘标题栏(那是 Electron 为了跨平台一致外观不得不做的事,我们不需要)

## 3. 旧项目功能迁移映射

见 `adblistener-migration-map.md`,包含每个功能对应到哪个新模块、以及明确标注的跨平台隐患(shell 管道依赖、`SimpleDateFormat` 线程安全、无界日志缓冲等)。

## 4. 分阶段计划

每个阶段:目标、交付物、验收标准、AI 的角色。阶段之间不要求严格顺序完成,但建议按顺序推进,后面阶段依赖前面搭好的基础设施。

**用户背景:Java 熟练,Kotlin 是初学者**——所以开局顺序刻意做了调整:先在现成的两模块脚手架里直接写最简单的功能,建立 Kotlin 语法 + Compose 声明式 UI 的手感,再引入多模块拆分和插件契约这些"架构层"的抽象。不要一上来就跳进接口设计,那对一个还不熟悉语言的人是本末倒置。

### Phase 0a — Kotlin / Compose 破冰(不动多模块结构,就在现有 `shared`+`desktopApp` 里写)
- 目标:直接在 `App.kt` 里写一个最简版颜色转换 UI(输入框 + 按钮 + 结果文本),先不抽 `ToolPlugin`、不拆模块,就是单文件能跑起来
- 建立的手感:Kotlin 基础语法(`val`/`var`、`data class`、null 安全 `?`/`?:`/`!!`、字符串模板、`when` 表达式、lambda 尾随语法)和 Java 的对照;Compose 的声明式心智模型(`@Composable`、`remember { mutableStateOf(...) }`、状态变化触发重组)和 JavaFX 那种"拿到控件引用手动 `setText`"命令式写法的本质区别——这一点对 Java/Swing/JavaFX 背景的人是最大的思维转弯,比语法本身更重要
- 验收标准:你能不看示例,自己解释清楚"为什么改一个 `var` 就能让界面自动刷新"(对比老代码里 `Platform.runLater(() -> textAreaShow.appendText(...))` 这种手动触发 UI 更新的方式)
- AI 角色:这一阶段可以多给"Java 里这么写,Kotlin/Compose 里对应这么写"的对照讲解,比后面阶段更倾向教学,但代码还是你敲

### Phase 0b — 项目结构改造(脚手架,可以让 AI 代劳)
- 目标:把当前 JetBrains 向导生成的 `shared` + `desktopApp` 两模块结构,改造成上面的多模块结构
- 交付物:`settings.gradle.kts` 包含所有新模块,各模块 `build.gradle.kts` 依赖关系正确,能跑通空壳(`desktopApp` 显示一个空窗口)
- 验收标准:`./gradlew build` 全绿,`./gradlew :desktopApp:run` 能打开窗口
- AI 角色:这是纯样板代码,你可以直接让 AI 生成,不算"代笔核心逻辑"

### Phase 1 — 核心契约、启动器与主题(`core-api` + `core-ui`)
- 目标:定义 `ToolPlugin`/`ToolMetadata`/`ToolContext`;设计 `ThemeSpec` 数据模型 + 2~3 套预设主题(至少一深一浅)+ 明暗切换 + 强调色驱动的自定义主题生成;搭启动器主窗口(网格 + 搜索)+ `ToolWindowLauncher`(点击工具→新开独立 `Window` 渲染 `ToolPlugin.Content`);处理好多窗口生命周期(关闭启动器不杀掉已开的工具窗口,应用在还有窗口存活时不退出)
- 交付物:两三个假的 "Hello Tool" 插件出现在启动器网格里,点击后各自弹出独立窗口;设置面板里能实时切换预设主题、切换明暗、挑一个强调色生成自定义主题并立即生效,且对所有已开的工具窗口同步生效
- 验收标准:启动器搜索框能按名称过滤工具;点击工具→独立窗口打开,关掉工具窗口不影响启动器,关掉启动器不影响已开的工具窗口;所有预设主题在浅色/深色两态下都不难看(尤其日志等宽字体区的对比度);自定义主题切换强调色后全局颜色实时更新,不需要重启
- AI 角色:讲解 Compose Desktop 的 `application { }` 多 `Window` API 和退出时机管理、`CompositionLocal`(用来传 `ToolContext`/当前 `ThemeSpec`,注意它要能跨多个独立 `Window` 共享)、`MaterialTheme` 自定义配色和从强调色派生调色板的原理,评审你写的接口设计是否合理,不代写

### Phase 2 — 颜色转换工具(最简单的开局)
- 目标:迁移 `RGBUtils.java` 逻辑到 Kotlin commonMain 纯函数 + Compose UI
- 交付物:`tools/color-converter` 模块,支持 hex ↔ rgba 双向转换 + 颜色选择器
- 验收标准:对照老代码的输入输出写单元测试(`commonTest`),覆盖 `#rgb`/`#rrggbb`/`#aarrggbb`/`0x...`/`rgba(...)` 几种格式
- AI 角色:评审 Kotlin 惯用法(比如用 `sealed class`/`Result` 处理错误 vs 老代码的 `errMsg` 字段模式),不代写转换逻辑

### Phase 3 — 时间转换工具
- 目标:迁移时间格式互转逻辑,改用 `kotlinx-datetime` 替代 `SimpleDateFormat`
- 交付物:`tools/time-converter`
- 验收标准:单元测试覆盖老代码支持的几种格式(`yyyy-MM-dd`、`yyyy/MM/dd`、中文日期、纯数字时间戳)
- AI 角色:讲解 `kotlinx-datetime` API 和 `java.time`/`SimpleDateFormat` 的对应关系

### Phase 4 — 编码转换 + DES3 加解密
- 目标:编码转换(老代码是半成品,重新设计)+ DES3 加解密迁移(`javax.crypto`,只能放 `jvmMain`)
- 交付物:`tools/encode-converter`、`tools/des3`
- 验收标准:DES3 加解密结果和老代码 `DES3Util` 的 `main()` 测试输出一致;引入协程 `suspend` 包装耗时操作(即使 DES3 本身很快,练习一下异步边界的设计)
- AI 角色:讲解 `expect`/`actual` 是什么、为什么 `javax.crypto` 现在只能放 jvmMain、以后加 Android target 要怎么补 `actual`

### Phase 5 — ADB 设备日志监听(旗舰功能)
- 目标:重写 `CMD.java` 为协程 `Flow` 版本(`adb-core`),搭配 `tools/adb-logcat`
- 交付物:设备/包名/端口/日志级别筛选联动,`LazyColumn` 展示日志,客户端搜索过滤,**不依赖宿主 shell 管道**(自己在 Kotlin 里过滤,不用 `| grep`)
- 验收标准:能在 Windows 上连接真实设备跑通;切换设备/工具时协程正确取消,不留后台线程;长时间运行日志量大时内存不爆(有界缓冲验证)
- AI 角色:讲解 `callbackFlow`/`Flow.cancellable()`、`Process`/`ProcessBuilder` 跨平台注意事项,评审并发正确性

### Phase 6 — 设备信息工具
- 目标:复用 `adb-core`,把老代码的嵌套回调链改成线性 `suspend` 调用序列
- 交付物:`tools/device-info`,卡片网格展示型号/版本/分辨率/DPI/尺寸/CPU
- 验收标准:5 个 adb 命令的调用顺序和数据依赖(尺寸计算依赖分辨率+DPI)逻辑正确
- AI 角色:讲解协程顺序执行 vs 回调链的可读性对比

### Phase 7 — 截屏工具
- 目标:文件路径选择(Compose Desktop 无内置文件选择器,需要 Swing `JFileChooser`/AWT `FileDialog` 桥接)+ adb 截屏两步命令
- 交付物:`tools/screenshot`
- 验收标准:三个 OS 上文件选择器能正常弹出(至少 Windows 验证,macOS/Linux 靠代码审查+已知兼容性)
- AI 角色:讲解 Compose Desktop 与 AWT/Swing 的 interop 边界

### Phase 8 — 设置持久化 + 打包
- 目标:`settings-core`(窗口大小/当前设备/主题选择包括自定义强调色/已装插件记忆),`jpackage`/`compose.desktop.nativeDistributions` 三平台打包验证
- 交付物:能生成 Windows Msi(本机验证),Dmg/Deb 配置正确(异机验证或先审查配置);重启应用后主题选择(含自定义强调色)、窗口大小、上次设备都能恢复
- 验收标准:设置项按 OS 存到正确路径(`%APPDATA%`/`~/Library/Application Support`/`~/.config`)
- AI 角色:讲解各 OS 标准配置目录约定

### Phase 9 — 外部 JAR 插件加载(进阶)
- 目标:`ServiceLoader` + `URLClassLoader` 扫描 `plugins/` 目录,动态加载实现了 `ToolPlugin` 的第三方 JAR
- 交付物:能把 Phase 2~7 的某个内置工具改造成外部插件形式验证加载机制
- AI 角色:讲解 JVM 类加载器隔离、`ServiceLoader` SPI 机制、动态加载的安全边界

### Phase 10+ — 脚本插件(远期)
- 目标:GraalVM Polyglot 接入,先 JS/TypeScript(GraalJS),脚本插件走"声明 schema + `transform` 函数"模式,宿主自动生成表单 UI
- 这是明确排期靠后的项,不在骨架阶段做任何预留实现,只保证 `ToolPlugin` 契约设计得足够通用、不会被这个远期需求逼着重构

## 5. 技术选型清单

| 关注点 | 选型 | 理由 |
|---|---|---|
| 状态管理 | ViewModel(`androidx.lifecycle.viewmodel-compose`,已在模板依赖里)+ `StateFlow` | 官方推荐做法,和旧 MVP 的 Presenter/View 概念能对应上,便于理解迁移 |
| 主题系统 | `ThemeSpec` 数据模型(预设 + 强调色派生)+ Compose `CompositionLocal` 分发当前主题 | 明暗切换、预设主题、自定义主题三者共用一套渲染路径,避免"自定义主题"单独写一套 UI |
| 多窗口管理 | Compose Desktop `application { }` 块内多个 `Window`,启动器持有"已打开工具窗口"的状态,显式控制退出时机 | 启动器 = launcher,工具 = 各自独立窗口,和"工具可单独运行"共用同一套 Composable 入口,不需要额外的应用内导航/路由库 |
| 异步/进程 I/O | Kotlin 协程 + `Flow`(`callbackFlow` 包装进程输出流) | 替代老代码 Thread + 回调金字塔,天然支持取消 |
| 长列表渲染 | `LazyColumn` + 有界缓冲 | 避免老代码 TextArea 无界追加的内存问题 |
| 日期时间 | `kotlinx-datetime` | 真正跨平台,替代 `SimpleDateFormat`(JVM-only 且线程不安全) |
| 加解密 | `javax.crypto`(`jvmMain`) | JVM-only,Desktop-only 阶段够用,未来加 Android target 需要 `expect/actual` |
| 本地设置 | `multiplatform-settings` 或自写 JSON + OS 感知路径 | 统一存取,避免各工具各写一套 |
| 测试 | `commonTest`/`jvmTest`(模板已搭好) | 迁移纯逻辑工具时,老代码行为就是现成的预期输出 |
| 插件动态加载 | `ServiceLoader` + `URLClassLoader` | JVM 原生机制,不需要额外依赖 |
| 脚本插件运行时 | GraalVM Polyglot(GraalJS → GraalPy) | 同进程多语言,比内嵌浏览器/子进程轻量 |
| 打包 | `compose.desktop.nativeDistributions`(已配置 Dmg/Msi/Deb) | 模板已有,不需要额外工具链 |

## 6. 明确不做/明确靠后的事(避免范围蔓延)

- 不现在加 Android/iOS target——`jvmMain` 里的代码要写得"未来加 target 时容易补 actual",但不预先搭空的 `androidMain`/`iosMain`
- 不在 Phase 1~8 做任何 GraalVM/脚本插件相关实现,只保证接口设计不挡路
- 不做插件市场/在线安装机制——`plugins/` 目录手动放 JAR 就够,这不是要做一个产品级分发平台
- 不做插件沙箱/权限系统的完整实现——JAR 插件先用"信任来源"这种社会约定代替真正的安全隔离,脚本插件的权限隔离等做到 Phase 10 再细化
