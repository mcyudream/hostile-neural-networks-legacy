# Hostile Neural Networks Legacy 正式基线验收

- 日期：2026-11-03。
- 环境：Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25。
- 生产安装包：`hostile-neural-networks-legacy-6.5.1-cleanroom.1.jar`。
- SHA-256：`3b920160364857763e3c6c4320620900f55998106a33176550e1815a11e96d24`。
- 作者、loader ID、展示名与打包图标另由 `tools/verify_legacy_package.py` 检查。

| 组合 | 结果 | 断言数 | 证据 |
| --- | --- | --- | --- |
| client-without-ponder | PASS | 46 | [报告](docs/verification/legacy-identity/cleanroom-0.4.0-final/client-without-ponder/report.json) |
| client-with-ponder-jei | PASS | 70 | [报告](docs/verification/legacy-identity/cleanroom-0.4.0-final/client-with-ponder-jei/report.json) |
| server-without-ponder | PASS | 38 | [报告](docs/verification/legacy-identity/cleanroom-0.4.0-final/server-without-ponder/report.json) |
| server-with-ponder | PASS | 38 | [报告](docs/verification/legacy-identity/cleanroom-0.4.0-final/server-with-ponder/report.json) |

## 覆盖范围

29 个基础模型及 8 个可选矿物模型、学习器训练与存储、模拟/推理/战利品生产、Data Center 多方块及并行处理、自动化边界、真实网络 GUI、世界断开与重载；安装 Ponder/JEI 时的教程播放、分类与配方查询，以及两种专服组合。

每份 runtime.json 记录该组合实际安装的 jar 和哈希，包括可选 Ponder、CraftTweaker 或 JEI。验收使用官方安装器的生产运行库与 SRG 安装包，不将 userdev / 其他 Cleanroom 版本作为本基线的运行证据。

正式 0.4.0 专服的 Kirino 发现兼容修复仅在该版本的物理专服生效，不改动官方加载器 jar。客户端图像检查使用 Linux llvmpipe 和空音频后端；未覆盖全部硬件及任意整合包组合。功能差异与未实现项目仍以 README 为准。
