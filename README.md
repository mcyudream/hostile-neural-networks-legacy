# Hostile Neural Networks Legacy

![Hostile Neural Networks Legacy](branding/cover.png)

<!-- legacy-overview:start -->
把 HNN 6.5.1 的生物数据模型、模拟生产和战利品制造带到 1.12.2。通过击杀训练模型，再用 FE 和预测矩阵自动产出生物预测及战利品；数据中心可同时处理 25 个模型。

## 当前状态

> **公开仓库目前包含项目介绍、上游差异、图像与 CI 框架。Mod 源码尚未提交，等待对应迁移验收完成。CI 此时只报告 scaffold，没有执行 Mod 编译。**

当前为进行中的迁移或改名后验收阶段。构建通过、接口已导入和历史测试结果均不等于完整移植完成。当前源码提交与产物状态见 [PUBLICATION.md](PUBLICATION.md)；未通过验收的项目暂不提供成品发布。

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

源码提交后，使用 Java 25，在项目目录执行：

```bash
python3 tools/bootstrap_legacy.py
bash gradlew --no-daemon :build :reobfJar --console=plain
python3 tools/verify_legacy_package.py
```

CI 会准备固定版本的源码依赖，构建完整主源码与测试，并检查发行 jar 的身份、作者和图标。
仅有文档的仓库会明确报告 scaffold，不把跳过编译标为 Mod 构建成功。CI 构建不能替代客户端玩法和专服验收。

## 授权与图像

保留 Shadows-of-Fire 的代码、贴图、模型与翻译来源；见 LICENSE、NOTICE.md 和导入清单。

此版本作者为 **yudream**，上游作者与版权保留如上。`branding/icon.png` 和 `branding/cover.png` 为本项目生成的展示图，
以代表物品或概念表现项目；不作为游戏实机截图，也不改变上游游戏资源的授权。生成记录见 `branding/provenance.json`。

<!-- legacy-overview:end -->
