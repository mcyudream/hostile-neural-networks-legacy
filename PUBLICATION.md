# Hostile Neural Networks Legacy 发布状态

- Legacy 作者：**yudream**；上游作者与来源见 [README](README.md)。
- 当前发行版本：**`6.5.1-cleanroom.2`**，新增龙研、热力、匠魂和血魔法联动。
- 正式 Minecraft 1.12.2 / Cleanroom 0.4.0 / Java 25 客户端与专服验收：**PASS**，见 [记录](LEGACY-VERIFICATION.md)。
- 发行标签：`v6.5.1-cleanroom.2`；[GitHub Release](https://github.com/mcyudream/hostile-neural-networks-legacy/releases/tag/v6.5.1-cleanroom.2) 由标签 CI 创建。
- CurseForge / Modrinth：尚未创建项目或上传版本。
- Loader Mod ID：`hostile-neural-networks-legacy`；保留资源空间：`hostilenetworks`。Ponder 前置 ID 为 `ponder-legacy`。

已验收安装包 SHA-256：`3b1fc502314b510fc0b4a10143e1db63f4c6dc82fbe0f81d6546c2933c28bd13`。生产包、源码包和 `SHA256SUMS` 可由 `tools/prepare_release.py` 整理到 `build/release/`。标签 CI 强制核对安装包和四套运行报告，通过后自动上传这些发行资产。

`mcp` 开发包、验收 harness 和可选依赖不作为发行资产。安装依赖及 CodeChickenLib CRE 3.3.5 的要求见 [联动说明](docs/INTEGRATIONS.md)。
