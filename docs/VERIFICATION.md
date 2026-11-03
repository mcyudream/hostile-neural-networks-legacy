# 验证记录

验证日期：2026-11-03。环境：Linux x64、Java 25.0.5+8、Minecraft 1.12.2、**Cleanroom 0.4.0 正式版**。

以下四组测试均在独立运行实例中加载发行用 SRG jar。启动类路径使用官方安装配方、原版 Minecraft jar 和官方依赖，没有开发环境资源覆盖。测试同时断言 `CleanroomVersion.getVersion()` 精确等于 `0.4.0`。客户端还断言 HNN 从打包后的 jar 加载。

## 最终发行文件

文件：[HostileNeuralNetworks-Cleanroom-1.12.2-6.5.1-cleanroom.1.jar](../build/libs/HostileNeuralNetworks-Cleanroom-1.12.2-6.5.1-cleanroom.1.jar)

```text
SHA-256 dac99c043d0dbb74eb8c2ebb5eb712a45c579f0cd755907d0c9c348128e21a72
```

大小 301,888 字节；60 个主代码类，Java class version 69；包含 38 份模型定义和 30 个合成配方。已检查不包含开发测试 Mod、Ponder、CraftTweaker 或 JEI 的实现类。元数据和启动依赖下限为 Cleanroom `0.4.0`。

构建：[build.log](verification/build.log)。打包检查：[package.json](verification/package.json)。七张主要 GUI / HUD 贴图与锁定上游文件逐字节相同，散列见 [ui-assets.json](verification/ui-assets.json)。

## 正式版运行矩阵

| 环境 | 可选组件 | 结果 | 检查数 | 报告 / 运行文件与散列 |
| --- | --- | --- | --- | --- |
| 客户端 | 不安装 Ponder、CraftTweaker、JEI | PASS | 46 | [报告](verification/cleanroom-0.4.0/client-without-ponder/report.json) · [运行记录](verification/cleanroom-0.4.0/client-without-ponder/runtime.json) |
| 客户端 | Ponder + CraftTweaker + JEI | PASS | 70 | [报告](verification/cleanroom-0.4.0/client-with-ponder-jei/report.json) · [运行记录](verification/cleanroom-0.4.0/client-with-ponder-jei/runtime.json) |
| 专用服务器 | 不安装 Ponder、CraftTweaker、JEI | PASS | 38 | [报告](verification/cleanroom-0.4.0/server-without-ponder/report.json) · [运行记录](verification/cleanroom-0.4.0/server-without-ponder/runtime.json) |
| 专用服务器 | Ponder + CraftTweaker | PASS | 38 | [报告](verification/cleanroom-0.4.0/server-with-ponder/report.json) · [运行记录](verification/cleanroom-0.4.0/server-with-ponder/runtime.json) |

四组记录中的 HNN 和验证 Mod 散列均与最终构建一致。每组目录保留完整 `startup.log`；客户端目录还包含实机截图。

可选组件版本：Ponder `1.4.0-cleanroom-alpha.1`、CraftTweaker `4.1.20.698`、JEI `4.16.1.302`。这些是可选 Mod 的版本号，所用 Cleanroom 运行时始终是 **0.4.0 正式版**。

官方安装器 SHA-256：`882435d851a51e14b802e0a9384e4a05cfbb3890fca435c40c494af75e5a166f`。其安装配方 ID 为 `cleanroom-0.4.0`，所用 universal jar SHA-256 为 `703121f60cd20363752320b9574f927084d0cd69973a0aff24ce3fbea9dbb351`。

## 已覆盖的行为

- 29 个默认原版生物模型、8 个可选矿物模型、羊毛元数据，以及全部 30 个基础合成配方。
- 右键真实僵尸绑定框架、六次击杀升到基础级、学习器物品存储；编辑已打开的学习器时保留同时获得的击杀数据。
- 真实世界 tick 中完成 300 tick 训练、240 tick 推理和 60 tick 制造，核对 FE 消耗、数据成长、基础预测与生物预测产量。
- 跨四区块的 7×7×7 数据中心结构、25 个自我意识模型并行，核对 200 个铁锭和 25 个基础预测及相应耗能。
- 输入专用 FE、受保护的模型槽和输出槽、缓存 IO capability 在坏壳 / 拆机后拒绝访问、结构修复后重新可用。
- 满仓时保留已付费产物；256 件自定义产物拆分后存档不丢失；腾出空间后只释放一次。
- 客户端真实 GUI 打开协议、中文和大 GUI 比例截图、制造机红石按钮通过网络改变服务端状态。
- 数据中心右键模型进入制造设置页，隐藏并禁用全部库存槽，返回后恢复。
- JEI 两个配方类别覆盖全部模型 / 产物；实际 JEI 列表范围避开学习器实体面板、模拟室按钮、数据中心设置页和两种比例下的制造队列。
- 六个关联物品的 Ponder 场景编译、结构加载、完整脚本执行；两套播放器实际推进时间线、显示场景和步骤文字，并在播放后截图。
- 客户端退出世界后重进，保留机器 FE、预测产物和学习器数据；专服完成世界 tick 后保存并正常停止。

测试 Mod 位于 `src/dev/`，是独立打包的本地测试工具，未进入发行文件。核心断言由客户端集成服务器和独立专服分别执行。

## Cleanroom 0.4.0 专服适配

官方 0.4.0 在专服发现 Mod 时会调用客户端 Kirino 入口。发行 jar 的 `Cleanroom040ServerPlugin` 仅在物理端 SERVER 且运行版本精确为 `0.4.0` 时启用转换器：把 `Loader.identifyMods` 中该次客户端静态调用替换为参数出栈。客户端及其他 Cleanroom 版本不注册此转换器；其他 Mod 已移除同一调用时不重复修改。

两组专服日志均记录此适配生效，随后进入 `Done` 并完成全部游戏断言。兼容结果包含这一实际运行修复。

## 复现

先按 [README](../README.md) 构建主 jar、SRG 验证 Mod 与可选 Ponder jar，再运行：

```bash
python3 tools/verify_cleanroom_040.py client
python3 tools/verify_cleanroom_040.py client --with-ponder --with-jei
python3 tools/verify_cleanroom_040.py server
python3 tools/verify_cleanroom_040.py server --with-ponder
```

客户端测试依次运行。两组专服使用相同的本地端口 25579，也应依次运行。实例位于 `run/cleanroom-0.4.0/`；脚本仅在最终报告 PASS 且进程正常退出后归档结果。服务器脚本会记录 Minecraft EULA 接受状态，应先接受 EULA。

## 验收范围

编译 SDK 为 `0.7.14-alpha`，本页结果来自官方 `0.4.0` 运行环境。较早开发测试及旧 UI 截图保存在 [development-0.7.14-alpha](verification/development-0.7.14-alpha/)，不作为正式版基线证据。

本轮未验收普通 Forge / Java 8、Reliquary 或现代第三方 Mod 模型，也未进行长期多人服务器负载测试。完整移植差异见 [README](../README.md#与上游的差异)，界面范围和截图见 [UI.md](UI.md)。
