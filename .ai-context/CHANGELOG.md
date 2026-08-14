# CHANGELOG

## 0.1.2 — 2026-08-14
- 新增 `docs/context_memory/handoff_2026-08-14.md`:首次方案设计会话的交接记录,供计划中的 GitHub 仓库迁移后在新会话/新路径下恢复上下文用(个人跨会话记忆是按工作目录路径生成的,换路径不会跟过去,仓库内 `.ai-context/` 才是可迁移的真相源)。
- 待跟进未闭环事项(记在交接文档里,不重复列):IntelliJ 编译报错原文未获取、GitHub 仓库迁移未执行。

## 0.1.1 — 2026-08-14
- 架构调整:UI 从"顶部 Tab 常驻 + 设备选择器全局"改为"启动器主窗口(工具网格+搜索)+ 每个工具独立 `Window`"。原因:AYA 的顶部 Tab + 全局设备选择器是纯设备调试工具的合理设计,但 QuickTools 大部分工具(颜色/时间/DES3/编码转换)跟"当前设备"无关,不该被迫共享这个全局状态;独立窗口模型还天然复用了"工具可单独运行"的同一套 Composable 入口,不需要额外的应用内导航/路由。
- 影响文件:`PROJECT.md`(UI 设计参考一节)、`docs/feature/roadmap.md`(架构总览、UI 设计第 2 节、Phase 1、技术选型表)。
- Claude/Codex 适配状态:文档变更,双端直接读取同一份,无需单独适配。

## 0.1.0 — 2026-08-14
- 初始化双模型工作区(myinit):建立 `.ai-context/`、`.claude/`、`.codex/`、根 `CLAUDE.md`/`AGENTS.md`。
- 新增 `PROJECT.md`、`public-agent-rules.md`(核心规则:AI 教学/评审角色,不代笔核心业务逻辑,用户全程亲自实现)。
- 新增 `docs/feature/adblistener-migration-map.md`(旧项目 Java/JavaFX 功能到 QuickTools 的迁移映射)。
- 新增 `docs/feature/roadmap.md`(KMP/Compose Desktop 学习路线图 + 架构设计 + UI 方案,参考 AYA 的布局)。
- Claude/Codex 适配状态:均为新建,双端一致,无待同步项。
