# adblistener → QuickTools 功能迁移映射

来源:`D:\Company\Gitee\adblistener`(Java + JavaFX,手写 MVP 架构,单窗口 + 多弹窗)

## 功能清单与旧代码定位

| 功能 | 旧代码(adblistener) | 迁移到的 QuickTools 模块 | 说明 |
|---|---|---|---|
| ADB 设备搜索/选择 | `Main.java` 顶部 ChoiceBox + `ADBListenerMode.searchConnectDevice` | `adb-core` + `tools/adb-logcat`(设备选择器提到 `desktopApp` 壳里全局共享) | 旧版设备选择器是每个功能各自持有,新版做成宿主壳全局状态,所有工具共享"当前设备" |
| 按包名/端口/日志级别筛选 + 实时日志 | `ADBListenerMode` 的 `searchPackages`/`searchPorts`/`searchLogByLevel`,`CMD.searchLog` | `tools/adb-logcat` | 核心功能。旧版 `textAreaShow.appendText` 无界追加是内存隐患,新版用 `LazyColumn` + 有界日志缓冲。旧版线程回调金字塔改协程 `Flow` |
| 日志搜索/过滤 | `ADBListenerEvent.filterLogContentFromShowList` | `tools/adb-logcat` | 在已产生的日志流上做客户端过滤,不重新发起 adb 命令 |
| 设备信息(型号/版本/分辨率/DPI/尺寸/CPU) | `deviceInfo/DeviceInfoMode.java`,嵌套回调链 | `tools/device-info` | 旧版靠回调链顺序发起 5 个 adb 命令,新版用协程顺序 `suspend` 调用替代,逻辑更线性 |
| 截屏 | `ADBListenerMode.screenCap` + `screenCapStep2`,`Main.chooseDirectory`(`DirectoryChooser`) | `tools/screenshot` | Compose Desktop 没有内置文件选择器,需要用 AWT `FileDialog`/Swing `JFileChooser` 桥接——这是一个专门的 Compose-AWT interop 练习点 |
| DES3 加解密 | `des3/DES3Util.java`(`javax.crypto`,3DES/CBC/PKCS5Padding + Base64) | `tools/des3` | `javax.crypto` 是 JVM-only API,天然只能放 `jvmMain`;若未来做 Android target 需要 expect/actual 换成 Android 的 crypto API |
| 颜色转换(hex ↔ rgba,多种格式) | `colorutil/RGBUtils.java`,`colorutil/ColorBean.java` | `tools/color-converter` | 纯字符串/数值运算,**可以放在纯 Kotlin 的 commonMain 层**,不依赖 JVM 特有 API,是很好的"写一次多处复用"练习 |
| 时间转换(时间戳 ↔ 多种日期格式) | `timeutil/TimeBean.java` + `Main`/`ADBListenerMode` 里的 `SimpleDateFormat` 逻辑 | `tools/time-converter` | 旧版用 `java.text.SimpleDateFormat`(JVM-only,还线程不安全);新版建议用 `kotlinx-datetime`,这是真正跨平台的日期时间库,同样适合放 commonMain |
| 编码转换 | `Main.java` 里 `cbEncodeType`/`encode0`/`encodeResult` 相关代码(原实现较简陋,只有壳没填完整逻辑) | `tools/encode-converter` | 旧版这块基本是半成品,是重新设计的好机会,不用照抄 |

## 跨平台隐患(老代码里明确要注意、不能直接照搬的地方)

1. **ADB 可执行文件解析**(`CMD.java` 的 `getADBHome()`):已经写了 Windows/非 Windows 分支(`adb.exe` vs `adb`)+ `ADB_HOME` 环境变量 + `PATH` 查找 + 打包目录旁 `platform-tools` 兜底,这部分逻辑设计得不错,迁移时可以保留思路,用 Kotlin 重写。
2. **进程调用不能依赖宿主 shell 管道/`grep`**:旧代码 `CMD_PORT = "%s -s %s shell ps | grep %s"` 这类命令字符串里的 `|` 依赖本地 shell 解释管道,但 `Runtime.exec(String)`/`ProcessBuilder` 默认不经过 shell,在 Windows 上大概率不会按预期工作(Windows 也不一定装了 `grep`)。新版应该:自己发起不带管道的原始命令(如 `adb shell ps`),拿到全部输出后在 Kotlin 里用字符串/正则过滤,不依赖宿主系统装没装 `grep`。
3. **路径分隔符**:旧代码用 `File.separator` 拼路径(这个写法是对的,可以保留思路),但新版整体建议统一用 `java.nio.file.Path` API,更不容易出跨平台错误。
4. **`System.out.println` 调试输出**:旧代码里大量裸 `println`,新版建议换成统一的日志抽象,方便区分 debug/info/error,也方便以后做"应用内日志查看"这类自省功能。

## 明确不直接照搬、要重新设计的部分

- 老版是"一个主窗口 + 一堆模态弹窗(工具箱弹窗里塞 DES3/颜色/时间/编码四个功能)"的结构,新版按可插拔模块拆开,每个工具是独立的 `ToolPlugin`,不再用弹窗嵌套。
- 老版 MVP 里 `View` 接口方法有 20+ 个(`IADBListenerView`),职责糅在一起;新版按 MVVM(ViewModel + StateFlow)拆到各工具模块内部,不搞一个大而全的 View 接口。
