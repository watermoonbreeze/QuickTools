# 同步协议

## 发布协议
1. 共享资产(`.ai-context/` 下的规则、角色、Skill、工作流、Prompt、Hook、共享文档约定)实质变更 → 同一改动必须更新 `ASSET_REGISTRY.md` + `VERSION.md` + `CHANGELOG.md`。
2. `CHANGELOG.md` 当前版本条目必须列出:变更资产的相对路径、Claude/Codex 适配状态、必要的迁移动作。
3. 用户说"模型切换"或给出明确的同步口令时,当前模型读取自己在下表的同步游标,应用尚未处理的版本;不得把普通项目任务误当作全量同步。
4. 初始化/重大变更验收时检查:发布三件套(ASSET_REGISTRY/VERSION/CHANGELOG)是否存在、根 `CLAUDE.md`/`AGENTS.md` 是否都链接到 `public-agent-rules.md` 与 `PROJECT.md`。

## 同步游标

| 模型 | 已同步版本 |
|---|---|
| Claude | 0.1.2 |
| Codex | 0.1.0 |
