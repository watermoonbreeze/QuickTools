# shared-runtime

Claude/Codex 共用的可移植、非敏感、格式中立运行资料区。

- 可按需建立 `handoffs/`、`cache/`、`exports/`、`plugin-sources/`、`config/`;每个文件应标注来源、生成时间、是否可再生成。
- 严禁存放:认证信息、token、cookie、API Key、私钥、证书、真实环境文件、原始会话记录、SQLite、历史记录、工具缓存、遥测数据、插件安装状态、锁文件。外部服务密钥只能来自环境变量/系统凭据库/获准的密钥管理服务。
- 本目录是**项目级**共享区,存放本项目相关的可移植资料;跨项目资料放用户级 `~/.ai-context/shared-runtime`。
- 长期规则/能力写入 `.ai-context` 对应规范目录;正式项目文档写入 `docs/`,不要混放。

当前为新建项目,此目录暂无内容。
