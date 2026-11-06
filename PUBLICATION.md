# Hostile Neural Networks Legacy 发布状态

- Legacy 作者：**yudream**；上游作者与来源见 [README](README.md)。
- 当前发行版本：**`6.5.1-cleanroom.4`**，修复背包点击错位、扩展机器贴图与 JEI 缩放遮挡，并提供外部机器控制 API。
- 正式 Minecraft 1.12.2 / Cleanroom 0.4.0 / Java 25 客户端与专服验收：**PASS**，见 [记录](LEGACY-VERIFICATION.md)。
- GitHub Release：[`6.5.1-cleanroom.4`](https://github.com/mcyudream/hostile-neural-networks-legacy/releases/tag/v6.5.1-cleanroom.4)，2026-11-06 由标签 CI **正式发布**，非草稿、非预发行，已设为最新版本。
- 发行提交：[`90e21dd842c1`](https://github.com/mcyudream/hostile-neural-networks-legacy/commit/90e21dd842c16dee60f54f050b94026f86b3c252)。
- [主分支 CI](https://github.com/mcyudream/hostile-neural-networks-legacy/actions/runs/42184723021) 与 [标签构建 / 发布 CI](https://github.com/mcyudream/hostile-neural-networks-legacy/actions/runs/42185094662)：**PASS**。已下载 CI 产物和正式发行的生产包、源码包与校验清单，两者均与验收产物一致，见 [发布核验记录](docs/releases/6.5.1-cleanroom.4-ci.json)。
- 上一正式发行：[`6.5.1-cleanroom.3`](https://github.com/mcyudream/hostile-neural-networks-legacy/releases/tag/v6.5.1-cleanroom.3)，其 CI 与下载文件核验记录保留在 [历史记录](docs/releases/6.5.1-cleanroom.3-ci.json)。
- CurseForge / Modrinth：尚未创建项目或上传版本。
- Loader Mod ID：`hostile-neural-networks-legacy`；保留资源空间：`hostilenetworks`。Ponder 前置 ID 为 `ponder-legacy`。

已验收安装包 SHA-256：`a3c60aac621926e3c97f5e2dcf657e8c0b3b4989faed8f55bf0a8ebd2458eaa9`。生产包、源码包和 `SHA256SUMS` 可由 `tools/prepare_release.py` 整理到 `build/release/`。标签 CI 强制核对安装包和四套运行报告，通过后自动上传这些发行资产。

`mcp` 开发包、验收 harness 和可选依赖不作为发行资产。安装依赖及 CodeChickenLib CRE 3.3.5 的要求见 [联动说明](docs/INTEGRATIONS.md)。
