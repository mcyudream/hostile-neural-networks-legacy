# 外部机器控制接口（6.5.1-cleanroom.4）

`dev.shadowsoffire.hostilenetworks.api.MachineController` 是不依赖 AE2 或任何其他 Mod 的服务端接口。
当前 HNN AE2S 附属使用它管理普通模拟室、普通战利品制造机和终极战利品制造器。

1. 控制器本身应为同世界中已加载的 TileEntity，并实现 `MachineController`。
2. 建立持久任务后，使 `controls(machine)` 返回 true，再在服务端线程调用 `machine.claimController(controllerPos)`。
3. `canRun(machine)` 决定机器本刻能否运行；耗能、进度、原料消耗和产物仍由真实 HNN 机器处理。
4. 普通 / 终极制造器通过 `selectedDrop(machine, model)` 读取该任务固定产物的有效索引；不会改写或推进用户的原有选择队列。
5. 订单原料和产物由控制器通过真实 `machine.inventory` 转移，按 `insertItem` / `extractItem` 返回的实际数量记账，并同步保存任务状态。
6. 所有已提交产物回收完成后调用 `machine.releaseController(controllerPos)`。

控制期间，普通物品 Capability、机器 GUI、设定卡及制造指令卡不能更改库存或覆盖生产选择。
能量 Capability 保持开放。控制器坐标写入机器 NBT；控制器区块未加载时暂停，不主动加载区块。
已加载位置不再具有有效控制器时自动释放预约。控制器拆除时也应主动释放；机器原有拆除掉落规则保持有效。

控制器须自行保存已接收原料、目标输出、目标机器身份和订单状态，避免依赖瞬时 Java 引用。
停止处理不应重置付费进度或重新生成原料 / 产物。HNN 本体单独安装时不需要附属，正常手动与管道玩法继续工作。
