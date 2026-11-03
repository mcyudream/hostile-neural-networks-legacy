# Hostile Neural Networks Legacy

![Hostile Neural Networks Legacy](branding/cover.png)

<!-- legacy-overview:start -->
把 HNN 6.5.1 的生物数据模型、模拟生产和战利品制造带到 1.12.2。通过击杀训练模型，再用 FE 和预测矩阵自动产出生物预测及战利品；数据中心可同时处理 25 个模型。

## 当前状态

本版本已完成当前声明范围的正式 Cleanroom 0.4.0 安装包验收。实际运行包的 SHA-256、检查项和限制见 [验收记录](LEGACY-VERIFICATION.md)。这不表示上游所有扩展或任意整合包组合均已验证。

<!-- legacy-authorship:start -->
## 作者与来源 / Authors and upstream

- 此 Legacy 版本作者与维护者 / Legacy author and maintainer：**yudream**。
- 原作者 / Original author(s)：**Brennan Ward / Shadows_of_Fire**。
- 原仓库 / Original repository：[Shadows-of-Fire/Hostile-Neural-Networks](https://github.com/Shadows-of-Fire/Hostile-Neural-Networks)。
- 上游基线：`6.5.1`。
- 固定上游提交：[`db9db507c490`](https://github.com/Shadows-of-Fire/Hostile-Neural-Networks/commit/db9db507c49008ea80fd422c998bfb9a602e64e1)。
- 本版本独立维护；上游版权、许可证及资源来源声明保留于 LICENSE / NOTICE 文件中。
<!-- legacy-authorship:end -->

## 运行环境与身份

| 项目 | 要求 / 标识 |
| --- | --- |
| Minecraft | 1.12.2 |
| 运行基线 | Cleanroom **0.4.0 正式版（不带 `-alpha`）** |
| Java | 25 |
| 加载器 Mod ID | `hostile-neural-networks-legacy` |
| 资源与原注册命名空间 | `hostilenetworks`（保留） |

Mod 的 alpha 版本号与加载器版本无关。开发 SDK 可以较新，兼容结论必须来自上述正式运行基线。
加载器身份改名后，安装时应替换本项目旧 jar；原资源命名空间的保留不构成跨实现的存档转换承诺。

## 功能与迁移范围

- 深度学习器、模型框架、模拟室、战利品制造机、制造指令卡。
- 7×7×7 数据中心、共享原料与产物库存，以及能量/模型/原料/产物四种 IO 端口。
- 训练与推理模式、连续准确率、固定制造产物或轮转队列、模型和等级 JSON。

## 与上游的具体差异

| 项目 | 上游基线 | 本 Legacy 版本 |
| --- | --- | --- |
| 平台依赖 | Minecraft 1.21 分支及现代 Placebo / NeoForge 接口 | 以 1.12 FML、Forge Energy、IItemHandler、GUI 与数据同步实现，不需要 Placebo 运行库。 |
| 模型范围 | 固定上游中的现代生物与第三方模型 | 默认 29 个原版生物模型，可选 8 个矿物模型；排除 1.12 不存在或未核实的模型，未用其他生物冒名替换。 |
| 配方与掉落 | 现代原版材料与粗矿物品 | 转换 30 个基础合成配方；下界合金→钻石、回响碎片→龙息、平滑石→石头；粗铁/粗金→矿石方块。 |
| 界面 | 现代 GUI 与实体渲染接口 | 使用上游贴图和槽位布局重写 1.12 界面、实体预览与 HUD；保留制造设置独立页面和小窗口缩放。 |
| 教程与配方页 | 上游集成体系 | 新增本地 Ponder 的两套演示和 JEI 4 可选分类；均不内嵌运行库。 |
| 存档与服务端 | 现代组件与网络协议 | 以 1.12 NBT、服务器校验和模型同步实现；正式 0.4.0 专服发现兼容修复限定于该版本。 |

## 尚未实现与兼容边界

- 蝙蝠定义保留 Reliquary 条件，但未核实对应 1.12 注册名，不能算作已验证兼容。
- 不含 Curios、Jade 或现代 Placebo；部分 display 偏移、动画与 rainbow 颜色尚未完全复现。
- 不提供其他 HNN / Deep Mob Learning 实现的存档转换；切换不同模型定义的世界前需重启客户端。
- JEI 模型配方页在客户端初始化时建立，自定义模型重载后需重启客户端刷新 JEI。

## 依赖

- 必需：Cleanroom；无需 Placebo。
- 可选：Ponder Legacy 1.4.0-cleanroom-alpha.1（另需 CraftTweaker 4.1.20.698）、JEI 4.16.1.302。

## 使用方式

1. 模型框架右键绑定生物，装入深度学习器后击杀对应生物升级。
2. 基础级及以上模型装入模拟室，输入预测矩阵和 FE，选择训练或推理。
3. 把生物预测交给制造机，选择固定产物或制造队列；进阶使用数据中心并行生产。

## 构建与 CI

在完整源码项目目录使用 Java 25 执行：

```bash
python3 tools/bootstrap_legacy.py
bash gradlew --no-daemon :build :reobfJar --console=plain
python3 tools/verify_legacy_package.py
```

CI 会准备固定版本的源码依赖，构建完整主源码与测试，并检查发行 jar 的身份、作者和图标。
仅有文档的仓库会明确报告 scaffold，不把跳过编译标为 Mod 构建成功。CI 构建不能替代客户端玩法和专服验收。

## 发布平台

CurseForge / Modrinth 的项目资料、文件、依赖和 CI 凭据配置见 [发布说明](docs/PUBLISHING.md)。平台尚未发布的状态见 PUBLICATION.md。

## 授权与图像

保留 Shadows-of-Fire 的代码、贴图、模型与翻译来源；见 LICENSE、NOTICE.md 和导入清单。

此版本作者为 **yudream**，上游作者与版权保留如上。`branding/icon.png` 和 `branding/cover.png` 为本项目生成的展示图，
以代表物品或概念表现项目；不作为游戏实机截图，也不改变上游游戏资源的授权。生成记录见 `branding/provenance.json`。

<!-- legacy-overview:end -->

## 详细使用与开发说明

面向 **Minecraft 1.12.2 / Cleanroom 0.4.0 正式版 / Java 25** 的独立移植，版本 `6.5.1-cleanroom.1`。兼容基线为不带 `-alpha` 的 Cleanroom `0.4.0`。

来源为 [Shadows-of-Fire/Hostile-Neural-Networks](https://github.com/Shadows-of-Fire/Hostile-Neural-Networks) 的 `1.21` 分支、HNN 6.5.1，固定提交 `db9db507c49008ea80fd422c998bfb9a602e64e1`。代码、贴图、模型与翻译的授权说明见 [NOTICE.md](NOTICE.md) 和 [LICENSE](LICENSE)。这不是上游官方发行版。

## 安装

将 `build/libs/hostile-neural-networks-legacy-6.5.1-cleanroom.1.jar` 放入客户端和服务器的 `mods/`。使用 Java 25 启动 Cleanroom。`-mcp.jar` 和 `-sources.jar` 是开发产物，不要重复安装。

运行必需项只有 Cleanroom；不依赖 Placebo。以下组件可选，均独立安装：

| 组件 | 已适配版本 | 用途 |
| --- | --- | --- |
| [本仓库 Ponder](https://github.com/mcyudream/ponder-legacy) | `1.4.0-cleanroom-alpha.1`，Mod ID `ponder-legacy` | 两套游戏内分步演示，覆盖六个核心物品 |
| CraftTweaker | `4.1.20.698` | 仅安装上述 Ponder 时需要 |
| JEI | `4.16.1.302` | 模拟、制造配方及数据中心结构说明 |

Ponder、CraftTweaker、JEI 均未嵌入 HNN jar。当前目标是 Cleanroom / Java 25；未验证普通 Forge / Java 8。

## 已移植内容

- 深度学习器、模型框架、数据模型、生物预测、预测矩阵、基础预测和制造指令卡。
- 模拟室：训练 / 推理、数据成长、连续准确率、超过 100% 准确率时的多次产出。
- 战利品制造机：按模型保存的固定产物或轮转队列。
- 数据中心：7×7×7 空心结构、25 个并行模型、共享原料和产物槽、四种 IO 端口。
- Forge Energy、物品管道 / 漏斗接口、红石控制、满仓缓冲、存档、服务端 GUI 校验与模型定义同步。
- 自定义 JSON 模型、等级与原子重载；中英文界面；学习器快捷键、模型实体预览与手持 HUD。
- Ponder 可选演示和 JEI 可选配方页；未安装时核心玩法正常运行。

默认启用 **29 个原版生物模型**。可在配置中开启 **8 个矿物方块模型**。共导入 38 份定义，其中蝙蝠模型仍带 Reliquary 条件，未验证 1.12 Reliquary 注册名，不计入已验证兼容范围。导入、物品映射和排除清单见 [import-report.json](docs/import-report.json)。

## 核心玩法

1. 合成模型框架，右键受支持的生物进行绑定。新模型从故障级开始。
2. 将模型放进深度学习器的四个槽位之一，携带学习器击杀对应生物。僵尸模型从故障升到基础需要六次击杀；其他模型可能覆盖等级需求。
3. 把基础级或更高模型放入模拟室，输入预测矩阵和 FE。点击模式按钮切换训练 / 推理；正在运行的轮次结束后才能切换。
4. 推理得到生物预测后，将其送入战利品制造机。在界面左侧的 3×3 产物选板点击所需产物，供能后开始制造；右侧为输出库存。
5. 选择队列模式后，依次点击多个产物组成轮转队列；可以分页、移除或清空。制造指令卡可潜行右键复制制造机 / 数据中心设置，直接右键粘贴。

默认参数：

| 设备 / 模式 | 一轮处理时间 | 耗能 | 结果 |
| --- | --- | --- | --- |
| 模拟室：训练 | 300 tick | 模型基础 FE/t × 1.2，向上取整 | 数据 +1、迭代 +1，无产物 |
| 模拟室：推理 | 240 tick | 模型基础 FE/t | 固定基础预测，按准确率判定生物预测；不增加数据 |
| 战利品制造机 | 60 tick | 256 FE/t | 一个生物预测换取所选物品堆 |
| 数据中心 | 每个模型 300 tick | 各模型基础 FE/t × 1.5，向上取整 | 直接产出所选战利品及基础预测 |

20 tick 约为一秒。模拟室和数据中心还需要一 tick 建立轮次；表中列的是消耗能量的处理 tick。默认容量分别为模拟室 2,000,000 FE、制造机 1,000,000 FE、数据中心 8,000,000 FE。以僵尸为例，基础耗能为 128 FE/t。

| 默认等级 | 所需累计数据 | 每次世界行动增加的数据 | 基础准确率 |
| --- | --- | --- | --- |
| 故障 | 0 | 1 | 0% |
| 基础 | 6 | 4 | 5% |
| 进阶 | 54 | 10 | 22% |
| 卓越 | 354 | 18 | 65% |
| 自我意识 | 1254 | 0 | 100% |

默认在相邻等级间插值准确率。等级及各模型的需求 / 成长均可自定义。

学习器默认按 **U** 从快捷栏或副手打开。安装 Ponder 后，悬停学习器、框架、模拟室、制造机、数据中心或 IO 端口并按 **W** 可打开演示。

### 数据中心

1. 铺设 7×7 黑曜石地板，用黑色染色玻璃搭建墙壁和顶棚，外部总尺寸为 7×7×7。
2. 内部 5×5×5 完全留空。将紧贴地板上一层的一块墙面玻璃替换为数据中心，控制器正面朝外。
3. 界面确认结构完整后，放入最多 25 个自我意识级模型，提供原料和 FE。
4. **右键模型槽**打开该模型的独立制造设置页，选择产物或编辑队列，点击左侧返回箭头或按 ESC 返回。相同模型 ID 共享制造设置。
5. 可用 IO 端口替换墙壁或顶棚玻璃。右键端口循环切换「能量、模型、原料、产物」模式，再连接对应管线。模型端口允许有意提取模型。

控制器所在区块需要正常加载；运行时只临时加载结构跨越的相邻区块。结构被破坏或控制器拆除后，已有管线引用会拒绝访问。输出空间不足时停止新轮次；已付费轮次的产物进入可保存的缓冲区，腾出空间后释放，拆除机器也会掉落缓冲内容。

## 配置和整合包

普通配置位于 `config/hostilenetworks.cfg`，修改后重启游戏 / 服务器。包括能量容量、制造耗能、数据中心倍率、绑定开关、行动升级、连续准确率、训练限制、矿物模型开关及结构材质。

模型定义位于 `config/hostilenetworks/data_models/`，等级定义位于 `config/hostilenetworks/model_tiers/`。文件是完整定义，覆盖时不会与内置文件逐字段合并。

```text
data_models/zombie.json                         → hostilenetworks:zombie
data_models/mypack/custom_zombie.json            → mypack:custom_zombie
data_models/hostilenetworks/blocks/iron_ore.json  → hostilenetworks:blocks/iron_ore
model_tiers/basic.json                          → hostilenetworks:basic
```

管理员命令：

```text
/hostilenetworks reload
/hostilenetworks give_model <玩家> <模型ID> [等级ID或累计数据]
/hostilenetworks give_model Player zombie basic
/hostilenetworks give_model Player hostilenetworks:zombie 1254
```

`reload` 只重载模型和等级 JSON，成功后同步给在线客户端；无效的自定义定义会使本次重载失败并保留上一套定义。模型加载报告写入 `config/hostilenetworks/model-report.json`。JEI 当前在客户端初始化时建立配方页，修改模型后需重启客户端以刷新 JEI。

格式与示例见 [自定义模型说明](docs/CUSTOM-MODELS.md)。

## 界面

学习器、模拟室、制造机与数据中心使用上游原有 GUI 贴图、槽位位置、图标、翻页箭头和外置控制按钮。学习器保留实体预览、名称、trivia、属性图标与等级进度；数据中心右键模型进入独立制造设置页。小窗口会整体缩放，JEI 按实际显示范围避让侧边控件。布局对照与实机截图见 [界面对齐说明](docs/UI.md)。

## 与上游的差异

- 1.12 不存在的生物（例如溺尸、幻翼、猪灵、监守者、旋风人）不作替换；现代第三方 Mod 的模型未直接声明兼容，共排除 39 份定义。
- 30 个基础合成配方已转换。制造机配方以钻石替代下界合金，数据中心以龙息替代回响碎片，框架中的平滑石改为石头。铁 / 金的粗矿产物改为对应矿石方块，需要后续冶炼。
- 保留暮色基础预测物品，但没有自动移植现代 Twilight Forest 的模型和额外配方；可通过自定义 JSON 为已核实的 1.12 目标添加模型。
- 不包含 Curios、Jade、现代 Placebo / NeoForge 注册与模型生成命令。绑定条件只支持本移植说明中的子集。
- 实体预览只适配部分上游 `display` 缩放与 NBT；尚未完整复现显示偏移、动画等参数。trivia 已在学习器中显示，按 1.12 字体换行。`rainbow` 颜色目前显示为青色。
- 上游 `pt_br.json` 存在语法错误，导入时跳过；其余可解析语言保留，中英文补充了本移植界面文本。
- 未提供旧世界或其他 HNN / Deep Mob Learning 实现的存档转换；不要假设它们的物品 NBT 可直接互换。
- 客户端收到服务器模型与配置后会保留同步内容；切换到使用另一套本地定义的世界前，需要重启客户端以重新加载本地配置。

## 构建和验证

编译可选 Ponder 适配时，可使用同级 `../ponder`，或先运行 `python3 tools/bootstrap_legacy.py` 下载固定提交到 `.legacy-deps/ponder`。首次构建需联网下载依赖和游戏资源。

```bash
./tools/dev.sh :build :reobfDevHarnessJar :ponder:reobfJar
# 使用其他 Java 25 安装时：HNN_JAVA_HOME=/path/to/jdk25 ./tools/dev.sh :build

# 官方 Cleanroom 0.4.0 正式版，使用发行 jar、原版 Minecraft 及官方依赖
python3 tools/verify_cleanroom_040.py client
python3 tools/verify_cleanroom_040.py client --with-ponder --with-jei
python3 tools/verify_cleanroom_040.py server
python3 tools/verify_cleanroom_040.py server --with-ponder

# 开发环境快速检查；此环境的编译 SDK 为 0.7.14-alpha，不能替代上述基线验收
./tools/dev.sh :runClient -PhnnSmoke -PpackagedSmoke
./tools/dev.sh :runClient -PhnnSmoke -PpackagedSmoke -PwithPonder -PwithJei
./tools/dev.sh :runServer -PhnnServerSmoke -PpackagedSmoke
./tools/dev.sh :runServer -PhnnServerSmoke -PpackagedSmoke -PwithPonder
```

正式版验收脚本支持 Linux x64，复用校验过散列的运行库或按官方安装配方下载缺失文件；需要先完成一次开发环境资源与可选依赖下载。脚本会在 `run/cleanroom-0.4.0-legacy/` 下建立独立测试实例并写入 EULA 接受状态，使用前应已接受 Minecraft EULA。客户端实例需要图形环境。

官方 Cleanroom 0.4.0 在专服发现 Mod 时会调用客户端 Kirino 入口。HNN 包含只在物理端 SERVER 且加载器精确为 `0.4.0` 时启用的兼容修复，隔离这一调用；在客户端和其他版本不注册该转换器。若其他 Mod 已修复同一调用，则不再修改。

测试会修改专用测试世界，请勿在 `run/` 下的验收实例保存个人内容。测试 Mod 位于 `src/dev/`，不包含在发行 jar 中。实际验证范围、运行库版本、jar 散列和截图见 [验证记录](docs/VERIFICATION.md)。
