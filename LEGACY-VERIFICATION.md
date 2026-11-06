# Hostile Neural Networks Legacy 正式基线验收

## 当前正式发行 6.5.1-cleanroom.4

日期：2026-11-06。新增不依赖 AE2 的通用外部机器控制 API，供独立 HNN AE2S 附属使用。
控制期间保护库存与固定产物，保留真实机器的 FE 消耗、进度及用户原有选择。详见 [API 说明](docs/AUTOMATION-API.md)。
以下为正式 Cleanroom 0.4.0 / Java 25 的生产包回归验收。

- 安装包：`hostile-neural-networks-legacy-6.5.1-cleanroom.4.jar`。
- SHA-256：`a3c60aac621926e3c97f5e2dcf657e8c0b3b4989faed8f55bf0a8ebd2458eaa9`。
- 生产源码指纹：`9de5bc96a876f7f9f7803c8e2e66cd2a601fb561803fba1666757293f27bb2e7`。
- 正式发行：[`v6.5.1-cleanroom.4`](https://github.com/mcyudream/hostile-neural-networks-legacy/releases/tag/v6.5.1-cleanroom.4)，提交 `90e21dd842c16dee60f54f050b94026f86b3c252`。
- [主分支 CI](https://github.com/mcyudream/hostile-neural-networks-legacy/actions/runs/42184723021) 与 [标签构建 / 发布 CI](https://github.com/mcyudream/hostile-neural-networks-legacy/actions/runs/42185094662)：**PASS**。已下载 CI 及正式发行的安装包、源码包和 `SHA256SUMS`，确认与验收产物一致，详见 [发布核验记录](docs/releases/6.5.1-cleanroom.4-ci.json)。

| 组合 | 结果 | 检查数 | 证据 |
| --- | --- | --- | --- |
| client-without-ponder | PASS | 184 | [报告](docs/verification/6.5.1-cleanroom.4/cleanroom-0.4.0/client-without-ponder/report.json) |
| client-with-ponder-jei-integrations | PASS | 376 | [报告](docs/verification/6.5.1-cleanroom.4/cleanroom-0.4.0/client-with-ponder-jei-integrations/report.json) |
| server-without-ponder | PASS | 101 | [报告](docs/verification/6.5.1-cleanroom.4/cleanroom-0.4.0/server-without-ponder/report.json) |
| server-with-ponder-integrations | PASS | 213 | [报告](docs/verification/6.5.1-cleanroom.4/cleanroom-0.4.0/server-with-ponder-integrations/report.json) |

上述四组检查沿用并覆盖上一正式版核心机器、扩展机器、可选模型与客户端 / 专服流程。
AE2S 订单、库存锁和中途重载属于独立附属的单独验收范围。

### 终极模拟室背包回归

修复 `GuiScreen` 鼠标事件被再次缩放导致的点击错位；覆盖按下、释放、拖动及侧栏操作。扩展机器背包恢复 Extra HNN 原始贴图。
以 36 槽满背包，在 1280×800 普通窗口与 960×480 小窗口检查三行主背包和快捷栏两端的取出 / 放回，并核对服务器收到的格子与物品数量。
小窗口断言实际缩放小于 0.95，避免中文 GUI 比例设置仍退回普通尺寸而漏测。
旧安装包在小窗口下取错第一行物品的 [失败记录](docs/verification/6.5.1-cleanroom.4/inventory-before-fix/report.json) 已保留；修复后的同一流程在上述两套客户端均通过。
普通窗口与小窗口的满背包截图见 [界面记录](docs/UI.md)。

### 本轮可选依赖边界

修复验证过程中，一次全联动客户端启动触发龙研主菜单随机彩蛋，CodeChickenLib 在 Java 25 下反射写入静态常量时抛出 `InternalError`；[失败日志与运行清单](docs/verification/6.5.1-cleanroom.4/draconic-main-menu-failure/crash.txt) 对应当时的中间构建，后续启动完成全部检查。该可选运行库的主菜单问题仍可能偶发。

## 历史正式发行 6.5.1-cleanroom.3

- 日期：2026-11-04。
- 环境：Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25。
- 生产安装包：`hostile-neural-networks-legacy-6.5.1-cleanroom.3.jar`。
- SHA-256：`0f0c8d53f22e9f12594f870e61a0faf2be0ffaf34453ab1ccf89a0ec97dfdcd7`。
- 生产源码指纹：`92e85036599cd951c565ac47a1982af86fab2a194b5ee3692c6066f737e94773`。
- 正式发行：[`v6.5.1-cleanroom.3`](https://github.com/mcyudream/hostile-neural-networks-legacy/releases/tag/v6.5.1-cleanroom.3)，提交 `470b483934b7aed5fd9ed9709655703e5e28c3a6`。
- [主分支 CI](https://github.com/mcyudream/hostile-neural-networks-legacy/actions/runs/41836794886) 与 [标签构建 / 发布 CI](https://github.com/mcyudream/hostile-neural-networks-legacy/actions/runs/41837841584)：**PASS**。已下载正式发行的安装包、源码包及 `SHA256SUMS`，确认与验收产物一致；详见 [发布核验记录](docs/releases/6.5.1-cleanroom.3-ci.json)。

| 组合 | 结果 | 检查数 | 证据 |
| --- | --- | --- | --- |
| client-without-ponder | PASS | 119 | [报告](docs/verification/6.5.1-cleanroom.3/cleanroom-0.4.0/client-without-ponder/report.json) |
| client-with-ponder-jei-integrations | PASS | 310 | [报告](docs/verification/6.5.1-cleanroom.3/cleanroom-0.4.0/client-with-ponder-jei-integrations/report.json) |
| server-without-ponder | PASS | 101 | [报告](docs/verification/6.5.1-cleanroom.3/cleanroom-0.4.0/server-without-ponder/report.json) |
| server-with-ponder-integrations | PASS | 213 | [报告](docs/verification/6.5.1-cleanroom.3/cleanroom-0.4.0/server-with-ponder-integrations/report.json) |

四套报告的 `runtime.json` 均记录同一安装包 SHA-256。无前置组合验证可选代码隔离；全联动组合安装龙研、热力、匠魂、血魔法及十二个锁定依赖。Ponder 使用此前验收的独立 `1.4.0-cleanroom-alpha.1` 运行库，Loader ID 为 `ponder-legacy`；加载器严格为正式 `0.4.0`。

本轮新增检查包括四模型融合、重复目标独立成长、普通 / 完美模型训练、三种升级组合、V1～V4 各档产量和精确 FE、36 槽制造、部分批次与队列、满仓暂停、中途换模、模型管道抽取、高容量 NBT 恢复、设定卡转移真实模型及防复制。

血魔法使用真实祭坛二进制：四档 LP、四种十次催化剂、真实二级祭坛四个有效献祭符文的 1.4 倍倍率、满坛停机、待交付 LP 防重复、链接器交互 / 跨维度拒绝、祭坛消失以及红石暂停时容量显示。

客户端通过真实网络打开全部新容器，验证模式 / 抽取按钮、原始方块模型、中文 GUI、普通与大比例缩放、JEI 避让、两套新 Ponder 场景完整执行与播放器推进。实机图见 [UI 记录](docs/UI.md)。完美模型渲染产生的光照状态泄漏已修复。

构建命令：`./tools/dev.sh :build :reobfDevHarnessJar`。安装包和源码包均保留 Extra HNN MIT 许可证及固定来源；安装包不包含开发验收类或可选前置。详见 [扩展玩法](docs/EXPANSION.md)。

### 已知验证边界

继承上一版的可选运行库限制：本环境下匠魂 2.13.0.183 的部分工具模型加载会写出警告 / 错误，上一版也存在；本轮 HNN 新方块与物品模型已检查。这里验收 HNN 的数据模型和机器联动，不将其等同于完整匠魂客户端渲染验收。

## 历史已发行构建 6.5.1-cleanroom.2

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
