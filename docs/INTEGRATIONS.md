# 1.12.2 可选联动

从 `6.5.1-cleanroom.2` 开始提供龙之研究、热力基本、匠魂和血魔法的六个数据模型。装入对应 Mod 后自动启用；未安装时跳过对应模型，原版生物模型照常使用。运行库均由玩家单独安装。

## 模型和产物

以下模型 ID 均以 `hostilenetworks:` 为前缀。每轮推理消耗一个预测矩阵，产出一份通用预测，并按模型准确率判定生物预测；一份生物预测在制造机中换取表中任选一组产物。

| 联动 | 模型 ID | 绑定生物 | 推理耗能 | 可选制造产物 |
| --- | --- | --- | --- | --- |
| 龙之研究 | `draconic/chaos_guardian` | 混沌守卫 | 32,768 FE/t | 混沌碎片 ×1；龙之心 ×1；龙尘 ×64 |
| 热力基本 | `thermal/blizz` | 暴雪人（Blizz） | 512 FE/t | 暴雪棒 ×16 |
| 热力基本 | `thermal/blitz` | 狂风人（Blitz） | 512 FE/t | 狂风棒 ×16 |
| 热力基本 | `thermal/basalz` | 岩石人（Basalz） | 512 FE/t | 岩石棒 ×16 |
| 匠魂 | `tconstruct/blue_slime` | 蓝色史莱姆 | 256 FE/t | 蓝色史莱姆球 ×32；紫色史莱姆球 ×16；岩浆史莱姆球 ×8 |
| 血魔法 | `bloodmagic/demon` | 腐化僵尸、腐化绵羊、腐化鸡、腐化蜘蛛 | 2,048 FE/t | 含 50 意志的普通恶魔意志 ×1；普通、腐蚀、破坏、复仇、坚毅意志结晶各为独立可选产物 ×1 |

此外，龙之研究存在时，已有的 `hostilenetworks:ender_dragon` 模型增加 **龙之心 ×1** 选项。混沌守卫的通用预测为末地预测，其余新增模型为主世界预测。

全部安装时默认加载 **35 个生物模型**；再开启矿物模型则为 **43 个模型**。匠魂蓝色史莱姆与原版史莱姆各自绑定。血魔法四种腐化生物共用一个模型，任一种的击杀都计入同一份数据。

### 培养和供能

1. 手持模型框架右键目标生物，将绑定后的模型放入深度学习器。混沌守卫和末影龙的头、身体、尾部、翅膀均可用于绑定。
2. 携带学习器击杀对应生物。热力、匠魂和血魔法模型使用普通等级配置，首次升到基础需要 6 次击杀。
3. 混沌守卫模型击败一次即可升到基础。其累计数据要求为 **0 / 3 / 12 / 30 / 60**，对应等级的击杀收益为 **3 / 3 / 6 / 10 / 0**。
4. 在模拟室推理，或达到自我意识后放进成形的数据中心直接制造产物；制造机与数据中心均支持选取、轮转和指令卡。

混沌守卫需要持续输入能量：一轮推理为 **7,864,320 FE**，超过模拟室默认的 2,000,000 FE 缓存；数据中心每个守卫模型为 **49,152 FE/t**，每轮 **14,745,600 FE**。能量不足时轮次暂停并保留进度，补能后继续。多模型并行时耗能相加。

JEI 自动显示已加载模型的模拟及制造配方。Ponder 仍是可选组件，Loader ID 为 `ponder-legacy`，资源命名空间为 `ponder`；现有演示中的框架、学习器与机器流程同样适用于这些模型。

## 验证用依赖版本

| 组件 | 实际测试安装文件 |
| --- | --- |
| 龙之研究 | `Draconic-Evolution-1.12.2-2.3.28.354-universal.jar` |
| Brandon's Core | `BrandonsCore-1.12.2-2.4.20.162-universal.jar` |
| CodeChickenLib CRE | **`codechickenlib-3.3.5-universal.jar`** |
| 热力基本 | `ThermalFoundation-1.12.2-2.6.7.1-universal.jar` |
| 热力前置 | CoFH Core `4.6.6.1`、CoFH World `1.4.0.1`、Redstone Flux `2.1.1.1` |
| 匠魂 | `TConstruct-1.12.2-2.13.0.183.jar`、Mantle `1.3.3.55` |
| 血魔法 | `BloodMagic-1.12.2-2.4.3-105.jar`、Guide API `2.1.8-63`、Baubles `1.5.2` |

测试以官方 **Cleanroom 0.4.0 正式版 / Java 25** 为基线。龙研与 Brandon's Core 的官方文件名版本和其 `mcmod.info` 版本有差异，验收按文件及 SHA-256 标识。每个文件的来源和散列记录在 [integration-runtime-lock.json](../integration-runtime-lock.json)。

**CodeChickenLib 版本必须配合加载器选择。** 本次实测旧版 `3.2.3.358` 在 Java 25 客户端的反射调用中出现 `StackOverflowError`；[CRE 3.3.5](https://github.com/Refitbench/CodeChickenLib/releases/tag/3.3.5) 的最低加载器要求为 `0.3.13-alpha`。本次同时核查的 [CRE 3.3.10](https://github.com/Refitbench/CodeChickenLib/releases/tag/3.3.10) 要求 `0.6.6-alpha`，不能用于本项目的 0.4.0 基线。不要同时安装两个 CodeChickenLib。

HNN 只编译自身及可选兼容 API，**不编译或嵌入上述 Mod**；联动通过受条件控制的模型 JSON 和实际物品 / 实体注册表实现。

## 注册映射与来源

| 模型 | 实体注册名 | 产物注册名及 metadata |
| --- | --- | --- |
| 混沌守卫 | `draconicevolution:chaosguardian` | `chaos_shard@0`、`dragon_heart@0`、`draconium_dust@0`（均为 `draconicevolution:`） |
| 三种热力生物 | `thermalfoundation:blizz` / `blitz` / `basalz` | `thermalfoundation:material@2048` / `2050` / `2052` |
| 蓝色史莱姆 | `tconstruct:blueslime` | `tconstruct:edible@1` / `2` / `4` |
| 腐化恶魔 | `bloodmagic:corrupted_zombie` / `corrupted_sheep` / `corrupted_chicken` / `corrupted_spider` | `bloodmagic:monster_soul@0`，NBT `{souls:50.0d}`；`bloodmagic:item_demon_crystal@0..4` |

三份热力定义根据已固定提交的 HNN 原定义调整，其他三份为本移植新增的模型。1.12 注册名、意志 NBT 和元数据对应关系依据以下项目的源代码核对，并由实际运行包复验：

- [Draconic Evolution，提交 5c29fe1e](https://github.com/brandon3055/Draconic-Evolution/tree/5c29fe1e4c2544ae159253dcdeedacadcb0e8c51)
- [Thermal Foundation，提交 4f088d82](https://github.com/CoFH/ThermalFoundation-1.12-Legacy/tree/4f088d82a8e6fbd767cd4bc7d0c3428888a6ff2f)
- [Tinkers' Construct，提交 c01173c0](https://github.com/SlimeKnights/TinkersConstruct/tree/c01173c0408352c50a2e8c5017552323ce42f5b4)
- [Blood Magic，提交 e830736e](https://github.com/WayofTime/BloodMagic/tree/e830736ee8ee10b0eff208504db8f92f24cd1f1c)

安装组合、检查结果与截图见 [本版本验收记录](../LEGACY-VERIFICATION.md)。验收覆盖实体绑定、死亡事件驱动的成长、所有制造产物、意志 API 可读和可消耗的 50 意志、元数据 / NBT 存储、模拟室与成形数据中心耗能、客户端预览及 JEI 配方注册。混沌守卫通过实际头部的致命伤害验证死亡计数，其他模型通过真实实体的致命伤害验证。混沌守卫和末影龙还验证了头、身体、尾部与翅膀的框架绑定；未覆盖混沌岛完整战斗和死亡动画。

## 复现验证

```bash
./tools/dev.sh --no-daemon :build :reobfDevHarnessJar :reobfJar --console=plain
python3 tools/verify_cleanroom_040.py server
python3 tools/verify_cleanroom_040.py client
python3 tools/verify_cleanroom_040.py server --with-ponder --with-integrations
python3 tools/verify_cleanroom_040.py client --with-ponder --with-jei --with-integrations
```

`--with-integrations` 按锁文件获取并校验这 12 个独立测试依赖，写入隔离实例。`LEGACY_PONDER_JAR` 可指定已有的独立 Ponder 运行包。服务器验证使用已接受 Minecraft EULA 的本地实例；客户端需要图形环境。
