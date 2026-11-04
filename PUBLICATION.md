# Hostile Neural Networks Legacy 发布状态

- Legacy 作者：**yudream**；上游作者与来源见 [README](README.md)。
- 本次待发布版本：**`6.5.1-cleanroom.3`**，并入 Extra HNN 扩展玩法与血魔法数字化苦难之井。
- 正式 Minecraft 1.12.2 / Cleanroom 0.4.0 / Java 25 客户端与专服验收：**PASS**，见 [记录](LEGACY-VERIFICATION.md)。
- 发布流程：先由 `main` CI 构建并核对验收包，再推送 `v6.5.1-cleanroom.3` 标签，由标签 CI 创建正式 GitHub Release。
- 上一正式发行：[`6.5.1-cleanroom.2`](https://github.com/mcyudream/hostile-neural-networks-legacy/releases/tag/v6.5.1-cleanroom.2)，其 CI 与下载文件核验记录保留在 [历史记录](docs/releases/6.5.1-cleanroom.2-ci.json)。
- CurseForge / Modrinth：尚未创建项目或上传版本。
- Loader Mod ID：`hostile-neural-networks-legacy`；保留资源空间：`hostilenetworks`。Ponder 前置 ID 为 `ponder-legacy`。

已验收安装包 SHA-256：`0f0c8d53f22e9f12594f870e61a0faf2be0ffaf34453ab1ccf89a0ec97dfdcd7`。生产包、源码包和 `SHA256SUMS` 可由 `tools/prepare_release.py` 整理到 `build/release/`。标签 CI 强制核对安装包和四套运行报告，通过后自动上传这些发行资产。

`mcp` 开发包、验收 harness 和可选依赖不作为发行资产。安装依赖及 CodeChickenLib CRE 3.3.5 的要求见 [联动说明](docs/INTEGRATIONS.md)。
