# 自定义模型和等级

本移植用配置目录中的 JSON 替代现代数据包注册。修改模型和等级后执行 `/hostilenetworks reload`；主配置 `hostilenetworks.cfg` 的修改需要重启。

## 模型示例

保存为 `config/hostilenetworks/data_models/mypack/test_zombie.json` 后得到 `mypack:test_zombie`：

```json
{
  "entity": "minecraft:zombie",
  "variants": ["minecraft:husk", "minecraft:zombie_villager"],
  "name": {"text": "测试僵尸"},
  "name_color": "#3B622F",
  "display": {},
  "sim_cost": 128,
  "input": {"item": "hostilenetworks:prediction_matrix"},
  "base_drop": {"id": "hostilenetworks:overworld_prediction"},
  "fabricator_drops": [
    {"id": "minecraft:rotten_flesh", "count": 64},
    {"id": "minecraft:iron_ingot", "count": 8}
  ],
  "attunement": {"attunable": false}
}
```

示例关闭右键绑定，避免和内置僵尸模型冲突；通过 `/hostilenetworks give_model <玩家> mypack:test_zombie basic` 获得。若要替换原模型，请改存为 `data_models/zombie.json` 并移除 `attunement`。有多个定义同时匹配绑定目标时会提示冲突。

- 使用真实的 **1.12 注册名**，例如 `minecraft:zombie_pigman`、`minecraft:evocation_illager`、`minecraft:villager_golem`。第三方模型应先核实实体、物品及 Mod ID。
- 实体使用 `entity`；方块模型将其换成 `block`。方块升级来自生存模式挖掘，不包含创造模式破坏。
- `variants` 是额外目标 ID 数组。不存在的变体忽略；主目标不存在则该定义无效。
- `name` 使用 1.12 文本组件；省略时采用实体或方块的本地化名称。
- `name_color` 接受 Minecraft 颜色名、`#RRGGBB`、`rainbow`（当前显示青色）。
- `sim_cost` 为整数 FE/t，范围 0 到 21,474,836。
- 物品支持 `id` 或 `item`、`count`、`data`（旧版元数据）、`nbt`（SNBT 字符串）、`optional`。数量范围 1–4096，实际产出仍受机器槽位容量限制；大堆叠进入缓冲前会拆分保存。`minecraft:air` 表示无产物。
- 可选产物的物品不存在时忽略；其他不存在的物品会报错。`base_drop` 可省略或为 `null`。

输入 `input` 支持单个物品、多个备选输入和 Ore Dictionary：

```json
{"ore": "ingotIron"}
```

```json
[
  {"item": "minecraft:dye", "data": 4},
  {"item": "minecraft:diamond", "nbt": "{hnn_input:1b}"}
]
```

每轮消耗一个匹配输入；`count` 不会改变每轮消耗量。指定 NBT 的输入要求 NBT 相同。

## 条件和覆盖

保留以下条件键，方便使用已导入的数据：

```json
"neoforge:conditions": [
  {"type": "neoforge:mod_loaded", "modid": "examplemod"},
  {"type": "hostilenetworks:block_data_models_enabled"}
]
```

这只是 JSON 字段兼容，不需要 NeoForge。两个条件都满足时才加载该模型。未知条件会报错。

模型可以分别覆盖等级的累计需求和每次世界行动增量：

```json
"required_data": {"faulty": 0, "basic": 6, "advanced": 54, "superior": 354, "self_aware": 1254},
"data_gained": {"faulty": 1, "basic": 4, "advanced": 10, "superior": 18, "self_aware": 0}
```

未列出的等级沿用公共定义。也接受旧键 `data_per_kill`。累计需求必须从 0 开始，严格递增；增量不得为负。

## 绑定与挖掘条件

省略 `attunement` 时允许右键绑定。显式配置格式：

```json
"attunement": {
  "attunable": true,
  "predicate": {"nbt": "{IsBaby:1b}"}
}
```

实体条件支持 SNBT 子集匹配 `nbt`、精确实体 ID `type` 和 `flags`。已实现的 flags 为 `is_on_fire`、`is_sneaking`、`is_sprinting`、`is_invisible`、`is_baby`。`is_baby` 以 1.12 的 `EntityAgeable` 为准；僵尸等其他幼年实体建议使用对应 NBT 字段。

方块条件支持属性精确匹配 `state` 和方块实体 SNBT 子集匹配 `nbt`。例如 `"state": {"variant": "granite"}`。不要给实体条件使用 `state`，也不要给方块条件使用实体 flags。

矿物模型可添加非精准采集要求：

```json
"upgrade_conditions": [
  {"condition": "minecraft:reference", "name": "hostilenetworks:not_silk_touch"}
]
```

没有实现现代 Minecraft 的完整 Predicate / LootCondition 编解码器。其他属性、嵌套判定和条件类型不在支持范围内。

## 公共等级

例如 `config/hostilenetworks/model_tiers/basic.json`：

```json
{
  "required_data": 6,
  "data_gained": 4,
  "color": "green",
  "accuracy": 0.05,
  "can_sim": true
}
```

等级只支持 `hostilenetworks` 命名空间。按 `required_data` 排序，需要从 0 开始、严格递增，并至少有一个允许模拟的等级。`can_sim` 默认 true，故障级应设置为 false。准确率范围 0–64；例如 1.5 表示一次保证成功，另有 50% 概率额外成功。数据中心要求等级允许模拟且该等级的基础准确率至少为 1。

服务器成功重载后会同步模型和等级给玩家。同步 JSON 最大为 1 MiB，整合包应控制模型集合大小。无效自定义定义会撤销整次重载；内置定义中无法解析的条目会记录到 `model-report.json`。
