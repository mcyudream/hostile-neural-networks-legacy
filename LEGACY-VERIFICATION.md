# Hostile Neural Networks Legacy 正式基线验收

## 当前构建 6.5.1-cleanroom.2

- 日期：2026-11-04。
- 环境：Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25。
- 生产安装包：`hostile-neural-networks-legacy-6.5.1-cleanroom.2.jar`。
- SHA-256：`3b1fc502314b510fc0b4a10143e1db63f4c6dc82fbe0f81d6546c2933c28bd13`。
- 生产源码指纹：`0d2e3b82df73922e15cd9a8fe55f7e022b1ac15fcd76e959715d8295d660ed50`。
- 正式发行：[`v6.5.1-cleanroom.2`](https://github.com/mcyudream/hostile-neural-networks-legacy/releases/tag/v6.5.1-cleanroom.2)，提交 `9b3d1dc7f8c57fa4e06e1d9d40c2d6af4628ef79`。
- [标签 CI](https://github.com/mcyudream/hostile-neural-networks-legacy/actions/runs/41821908208) 已核对同一安装包哈希和以下四套验收记录；正式发行下载文件的核验结果见 [发布核验记录](docs/releases/6.5.1-cleanroom.2-ci.json)。

| 组合 | 结果 | 检查数 | 证据 |
| --- | --- | --- | --- |
| client-without-ponder | PASS | 54 | [报告](docs/verification/6.5.1-cleanroom.2/cleanroom-0.4.0/client-without-ponder/report.json) |
| client-with-ponder-jei-integrations | PASS | 176 | [报告](docs/verification/6.5.1-cleanroom.2/cleanroom-0.4.0/client-with-ponder-jei-integrations/report.json) |
| server-without-ponder | PASS | 46 | [报告](docs/verification/6.5.1-cleanroom.2/cleanroom-0.4.0/server-without-ponder/report.json) |
| server-with-ponder-integrations | PASS | 138 | [报告](docs/verification/6.5.1-cleanroom.2/cleanroom-0.4.0/server-with-ponder-integrations/report.json) |

四套记录均对应上述同一份生产安装包。每份 `runtime.json` 保留所有安装 jar 的散列、官方安装器和运行库信息；联动测试的 12 个独立依赖另由 [锁文件](integration-runtime-lock.json) 固定。作者、Loader ID、版本、图标、开发代码隔离及禁止捆绑可选运行库由 `tools/verify_legacy_package.py` 检查，发布标签检查还会强制核对这四套记录与安装包 SHA-256。

## 覆盖范围

- 默认 29 个原版模型、8 个可选矿物模型；全部联动安装时分别为 35 / 43 个模型。
- 混沌守卫、三种热力元素生物、蓝色史莱姆，以及血魔法四种腐化生物的真实实体注册、框架绑定和致命伤害驱动的模型成长。
- 混沌守卫全部 7 个、末影龙全部 8 个头部 / 身体 / 尾部 / 翅膀碰撞箱的绑定。
- 每个新增模型的模拟室推理、全部制造产物和成形数据中心生产；精确 FE、原料数量、物品、metadata、NBT 以及机器序列化。
- 血魔法实际 API 读取和消耗 50 意志，五种意志晶体的意志数量，末影龙可选龙之心。
- 25 模型并行、自动化边界、满仓缓冲及保存；真实网络 GUI、世界断开与重载、模型数据同步。
- 六种新增模型的客户端实体预览、JEI 全模型及产物配方、界面避让和 Ponder 演示播放；Ponder 使用 `ponder-legacy` ID。
- 未安装四项联动 / Ponder / JEI 时的客户端与专服正常启动和核心玩法。

完整依赖、模型注册映射与供能说明见 [联动说明](docs/INTEGRATIONS.md)。客户端截图保存在对应验收目录，混沌守卫已调整预览比例以避免翅膀遮挡文字。测试使用独立开发 harness；它不在主安装包内。

## 验收边界

使用官方安装器的生产运行库与重混淆安装包；开发 SDK 或带 `-alpha` 的加载器结果不能替代上述正式基线。正式 0.4.0 专服的 Kirino 发现兼容修复仅在该版本的物理专服生效，未修改官方加载器 jar。

客户端图像检查使用 Linux llvmpipe 和空音频后端。混沌守卫死亡计数通过真实头部的致命伤害验证，未覆盖混沌岛完整战斗和死亡动画。未测试所有硬件或任意整合包组合，功能范围仍以 README 为准。

原版 CodeChickenLib 3.2.3.358 在本客户端 / Java 25 下递归崩溃；CRE 3.3.10 要求更高加载器。本次最终使用并验证 **CodeChickenLib CRE 3.3.5**，依赖选择的失败诊断保存在 [记录目录](docs/verification/6.5.1-cleanroom.2/old-codechickenlib-failure/)。

## 历史发行版 6.5.1-cleanroom.1

- 验收日期：2026-11-03。
- 安装包：`hostile-neural-networks-legacy-6.5.1-cleanroom.1.jar`。
- SHA-256：`3b920160364857763e3c6c4320620900f55998106a33176550e1815a11e96d24`。
- [已发布发行版](https://github.com/mcyudream/hostile-neural-networks-legacy/releases/tag/v6.5.1-cleanroom.1)。
- 无 Ponder 客户端 / Ponder + JEI 客户端分别为 46 / 70 项检查；无 Ponder / 安装 Ponder 的专服各 38 项。该版本不含本次新增联动。

历史证据保留在 [原验收目录](docs/verification/legacy-identity/cleanroom-0.4.0-final/)，不作为当前安装包的验收记录。
