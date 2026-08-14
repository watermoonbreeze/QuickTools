# CLAUDE.md

## 踩坑红线（必避）

> 由 /zongjie 维护,每条一行、命令式、可识别;详情见 `.ai-context/docs/experience/`。

- Gradle 报"编译报错"先看是否卡在 Daemon JVM/toolchain 下载阶段(报错含 `api.foojay.io` 等陌生域名)而非源码本身,不要一上来就怀疑 Compose/Kotlin 依赖版本 → `.ai-context/docs/experience/06_问题与踩坑.md`

Claude 专属入口。项目通用规则、协作模式、架构设计全部在 `.ai-context/` 下,本文件只做指引:

- 公共规则(**AI 是导师不是代笔**,读这个先):`.ai-context/public-agent-rules.md`
- 项目概览/技术栈/构建命令:`.ai-context/PROJECT.md`
- 旧项目功能迁移映射:`.ai-context/docs/feature/adblistener-migration-map.md`
- 完整路线图(架构 + UI 设计 + 分阶段计划):`.ai-context/docs/feature/roadmap.md`

Claude 专属工具配置(hooks/commands/权限)在 `.claude/` 下,不在本文件展开。

用户全局规则见 `~/.claude/CLAUDE.md`,与本文件不冲突时两者都遵守。
