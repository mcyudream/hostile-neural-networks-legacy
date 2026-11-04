package dev.shadowsoffire.hostilenetworks.dev;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.block.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.item.*;
import dev.shadowsoffire.hostilenetworks.tile.*;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.items.CapabilityItemHandler;

public final class MachineChecks {
    public static final BlockPos SIM = new BlockPos(0, 4, 0), FAB = new BlockPos(3, 4, 0), DC = new BlockPos(15, 4, 12), PORT = new BlockPos(17, 4, 12);
    private final WorldServer world;
    private final EntityPlayerMP player;
    private final BiConsumer<Boolean, String> check;
    public MachineChecks(WorldServer world, EntityPlayerMP player, BiConsumer<Boolean, String> check) { this.world = world; this.player = player; this.check = check; }
    public SimChamberTile sim() { return (SimChamberTile) world.getTileEntity(SIM); }
    public LootFabTile fab() { return (LootFabTile) world.getTileEntity(FAB); }
    public DataCenterTile dc() { return (DataCenterTile) world.getTileEntity(DC); }
    public ItemStack model(String id, int data) { var model = ModelRegistry.get(id); if (model == null) throw new IllegalStateException("Missing " + id); ItemStack stack = ModelStack.create(model, false); ModelStack.setData(stack, data); return stack; }
    public void setup() {
        world.getGameRules().setOrCreateGameRule("doMobSpawning", "false"); world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false"); world.setWorldTime(6000);
        if (player.connection == null) player.setPosition(1, 5, -4); else player.setPositionAndUpdate(1, 5, -4);
        int optionalModels = IntegrationChecks.loadedModelCount();
        check.accept(ModelRegistry.models().size() == 29 + optionalModels, "29 vanilla entity models plus " + optionalModels + " installed integration models load");
        HostileConfig.enableBlockDataModels = true; ModelRegistry.load();
        check.accept(ModelRegistry.get("hostilenetworks:blocks/iron_ore") != null && ModelRegistry.models().size() == 37 + optionalModels, "Eight optional ore models load with legacy item metadata");
        check.accept(ModelRegistry.get("hostilenetworks:sheep").drops.size() == 17, "All sixteen wool metadata variants and mutton are preserved");
        HostileConfig.enableBlockDataModels = false; ModelRegistry.load();
        for (int x = -3; x <= 5; x++) for (int z = -2; z <= 3; z++) world.setBlockState(new BlockPos(x, 3, z), Blocks.STONE.getDefaultState());
        world.setBlockToAir(SIM); world.setBlockToAir(FAB);
        world.setBlockState(SIM, Hostile.BLOCKS.get("sim_chamber").getDefaultState()); world.setBlockState(FAB, Hostile.BLOCKS.get("loot_fabricator").getDefaultState());
        // All four chunks are naturally loaded before a cross-boundary shell is validated.
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) world.getChunk(x, z);
        for (int x = 12; x <= 18; x++) for (int y = 3; y <= 9; y++) for (int z = 12; z <= 18; z++) {
            BlockPos p = new BlockPos(x, y, z);
            world.setBlockState(p, y == 3 ? Blocks.OBSIDIAN.getDefaultState() : x == 12 || x == 18 || y == 9 || z == 12 || z == 18
                ? Blocks.STAINED_GLASS.getStateFromMeta(15) : Blocks.AIR.getDefaultState(), 3);
        }
        world.setBlockState(DC, Hostile.BLOCKS.get("data_center").getDefaultState().withProperty(MachineBlock.FACING, EnumFacing.NORTH), 3);
        world.setBlockState(PORT, Hostile.BLOCKS.get("data_center_io_port").getDefaultState(), 3);
        dc().recheck(); check.accept(dc().shellValid, "7x7x7 black-glass/obsidian shell forms across four chunks");
        check.accept(((IOPortTile) world.getTileEntity(PORT)).controller() == dc(), "IO port resolves its validated controller");
        var portEnergy = world.getTileEntity(PORT).getCapability(CapabilityEnergy.ENERGY, EnumFacing.NORTH);
        check.accept(portEnergy != null && portEnergy.receiveEnergy(1000, false) == 1000 && portEnergy.extractEnergy(1000, false) == 0, "Energy port accepts FE and refuses extraction");
        world.setBlockState(PORT, world.getBlockState(PORT).withProperty(IOPortBlock.MODE, IOPortBlock.Mode.MODELS), 3);
        var retained = world.getTileEntity(PORT).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH);
        check.accept(retained != null && retained.insertItem(0, model("zombie", 1254), false).isEmpty(), "Model port inserts a single data model");
        check.accept(retained.extractItem(0, 1, false).getCount() == 1, "Model port permits deliberate model extraction");
        BlockPos corner = new BlockPos(18, 9, 18); world.setBlockToAir(corner); dc().recheck();
        check.accept(!dc().shellValid && !retained.insertItem(0, model("zombie", 1254), false).isEmpty(), "Retained capability refuses a broken shell");
        world.setBlockState(corner, Blocks.STAINED_GLASS.getStateFromMeta(15)); dc().recheck();
        check.accept(dc().shellValid && retained.getSlots() == 25, "Reforming the shell reconnects existing pipe references");
        verifyLearner(); verifyRecipes(); verifySafety();
        IntegrationChecks.verify(world, player, dc(), check);
        sim().inventory.setStackInSlot(0, model("zombie", 6)); sim().inventory.setStackInSlot(1, new ItemStack(Hostile.item("prediction_matrix"), 1));
        sim().mode = 1; sim().energy.set(2000000);
        for (int i = 0; i < 25; i++) dc().inventory.setStackInSlot(i, model("zombie", 1254));
        dc().inventory.setStackInSlot(25, new ItemStack(Hostile.item("prediction_matrix"), 25)); dc().selections.select(ModelRegistry.get("zombie"), 1); dc().energy.set(8000000);
        player.inventory.setInventorySlotContents(0, Hostile.stack("deep_learner"));
        DeepLearnerItem.inventory(player.inventory.getStackInSlot(0)).setStackInSlot(0, model("zombie", 354)); player.inventory.currentItem = 0;
        // Replacing the previous run's machines drops their inventory in this isolated test world.
        for (var entity : new ArrayList<>(world.loadedEntityList)) if (entity instanceof net.minecraft.entity.item.EntityItem) entity.setDead();
    }
    private void verifyLearner() {
        player.inventory.clear(); player.setHeldItem(EnumHand.MAIN_HAND, Hostile.stack("blank_data_model"));
        EntityZombie zombie = new EntityZombie(world);
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, EnumHand.MAIN_HAND, zombie));
        check.accept(ModelStack.model(player.getHeldItemMainhand()) == ModelRegistry.get("zombie"), "Right-clicking a real zombie attunes a framework server-side");
        ItemStack learner = Hostile.stack("deep_learner"); DeepLearnerItem.inventory(learner).setStackInSlot(0, player.getHeldItemMainhand()); player.setHeldItem(EnumHand.MAIN_HAND, learner);
        for (int i = 0; i < 6; i++) MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(new EntityZombie(world), net.minecraft.util.DamageSource.causePlayerDamage(player)));
        var stored = DeepLearnerItem.inventory(learner).getStackInSlot(0);
        check.accept(ModelStack.data(stored) == 6 && ModelRegistry.get("zombie").tier(6).id().equals("basic"), "Six credited kills train a Faulty zombie model to Basic");
        ItemStack roundTrip = new ItemStack(learner.writeToNBT(new NBTTagCompound()));
        check.accept(ModelStack.data(DeepLearnerItem.inventory(roundTrip).getStackInSlot(0)) == 6, "Learner contents and progress survive item NBT round trips");
        var open = new dev.shadowsoffire.hostilenetworks.gui.LearnerContainer(player, EnumHand.MAIN_HAND);
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(new EntityZombie(world), net.minecraft.util.DamageSource.causePlayerDamage(player)));
        player.inventory.setInventorySlotContents(9, model("skeleton", 6));
        open.transferStackInSlot(player, 4);
        check.accept(ModelStack.data(DeepLearnerItem.inventory(learner).getStackInSlot(0)) == 10 && ModelStack.model(DeepLearnerItem.inventory(learner).getStackInSlot(1)) == ModelRegistry.get("skeleton"), "Editing an open learner preserves data gained from a concurrent kill");
    }
    private void verifyRecipes() {
        InventoryCrafting grid = new InventoryCrafting(new Container() { @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) { return true; } }, 3, 3);
        grid.setInventorySlotContents(0, new ItemStack(Items.IRON_INGOT)); grid.setInventorySlotContents(1, new ItemStack(Blocks.GLASS_PANE)); grid.setInventorySlotContents(3, new ItemStack(Blocks.GLASS_PANE)); grid.setInventorySlotContents(4, new ItemStack(Items.CLAY_BALL)); grid.setInventorySlotContents(5, new ItemStack(Blocks.GLASS_PANE)); grid.setInventorySlotContents(7, new ItemStack(Blocks.GLASS_PANE)); grid.setInventorySlotContents(8, new ItemStack(Items.GOLD_INGOT));
        ItemStack result = CraftingManager.findMatchingResult(grid, world);
        check.accept(result.getItem() == Hostile.item("prediction_matrix") && result.getCount() == 16, "Prediction matrix crafting produces the original sixteen-item batch");
        long recipes = 0; for (var recipe : net.minecraftforge.fml.common.registry.ForgeRegistries.RECIPES) if (recipe.getRegistryName().getNamespace().equals(HostileNetworks.LEGACY_MOD_ID)) recipes++;
        check.accept(recipes == 30, "All thirty converted base crafting recipes register");
    }
    private void verifySafety() {
        var external = sim().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        check.accept(!external.insertItem(2, new ItemStack(Items.DIAMOND), false).isEmpty(), "Automation cannot insert into output slots");
        sim().inventory.setStackInSlot(0, model("zombie", 6)); check.accept(external.extractItem(0, 1, false).isEmpty(), "Simulation model is protected from automated extraction");
        sim().energy.receiveEnergy(1000, false); check.accept(sim().energy.extractEnergy(1000, false) == 0, "Machine energy is input-only");
        NBTTagCompound saved = sim().writeToNBT(new NBTTagCompound()); SimChamberTile restored = new SimChamberTile(); restored.readFromNBT(saved);
        check.accept(restored.energy.getEnergyStored() == sim().energy.getEnergyStored() && ModelStack.data(restored.inventory.getStackInSlot(0)) == 6, "Machine inventory and full-width FE survive serialization");
        for (int i = 2; i < sim().inventory.getSlots(); i++) sim().inventory.setStackInSlot(i, new ItemStack(Blocks.STONE, 64));
        sim().emit(List.of(new ItemStack(Items.DIAMOND, 256)));
        restored.readFromNBT(sim().writeToNBT(new NBTTagCompound()));
        check.accept(restored.pending.stream().mapToInt(ItemStack::getCount).sum() == 256, "Oversized custom drops preserve all 256 items when a full output buffer is saved");
        sim().pending.clear(); for (int i = 2; i < sim().inventory.getSlots(); i++) sim().inventory.setStackInSlot(i, ItemStack.EMPTY);
    }
    private int count(MachineTile machine, Item item) { int total = 0; for (int i = machine.getOutputStart(); i < machine.inventory.getSlots(); i++) if (machine.inventory.getStackInSlot(i).getItem() == item) total += machine.inventory.getStackInSlot(i).getCount(); return total; }
    public void afterTraining() {
        check.accept(ModelStack.data(sim().inventory.getStackInSlot(0)) == 7 && ModelStack.iterations(sim().inventory.getStackInSlot(0)) == 1, "A real 300-tick Training cycle adds one data and one iteration");
        check.accept(sim().inventory.getStackInSlot(2).isEmpty() && sim().inventory.getStackInSlot(3).isEmpty(), "Training produces no loot or predictions");
        check.accept(sim().energy.getEnergyStored() == 2000000 - 300 * 154, "Training consumes ceil(128 * 1.2) FE on each of 300 processing ticks");
        check.accept(count(dc(), Items.IRON_INGOT) == 200 && count(dc(), Hostile.item("overworld_prediction")) == 25, "Twenty-five concurrent self-aware Data Center runs produce 200 iron and 25 base drops");
        check.accept(dc().energy.getEnergyStored() == 8000000 - 25 * 300 * 192, "Parallel Data Center cycles charge every model at the configured 1.5x FE cost");
        sim().inventory.setStackInSlot(0, model("zombie", 1254)); sim().inventory.setStackInSlot(1, new ItemStack(Hostile.item("prediction_matrix"), 1)); sim().mode = 0; sim().energy.set(2000000);
        for (int i = 0; i < 45; i++) dc().inventory.setStackInSlot(i, ItemStack.EMPTY);
        dc().inventory.setStackInSlot(0, model("zombie", 1254)); dc().inventory.setStackInSlot(25, new ItemStack(Hostile.item("prediction_matrix"), 1)); dc().energy.set(8000000);
    }
    public void fillDuringCycle() {
        check.accept(dc().progress[0] > 0, "Data Center consumes its matrix before the output obstruction is introduced");
        for (int i = 29; i < 45; i++) dc().inventory.setStackInSlot(i, new ItemStack(Blocks.STONE, 64));
    }
    public void afterInference() {
        check.accept(count(sim(), Hostile.item("prediction")) == 1 && count(sim(), Hostile.item("overworld_prediction")) == 1, "Inference produces both guaranteed base drop and successful prediction");
        check.accept(ModelStack.data(sim().inventory.getStackInSlot(0)) == 1254 && sim().energy.getEnergyStored() == 2000000 - 240 * 128, "Inference preserves model data and costs exactly 240 processing ticks");
        ItemStack prediction = ModelStack.create(ModelRegistry.get("zombie"), true); prediction.setCount(2); fab().inventory.setStackInSlot(0, prediction);
        fab().selections.get(ModelRegistry.get("zombie")).queue = true; fab().selections.select(ModelRegistry.get("zombie"), 1); fab().selections.select(ModelRegistry.get("zombie"), 0); fab().energy.set(1000000);
    }
    public void afterFabrication() {
        check.accept(count(fab(), Items.IRON_INGOT) == 8 && count(fab(), Items.ROTTEN_FLESH) == 64 && fab().inventory.getStackInSlot(0).isEmpty(), "Fabricator round-robin queue processes exactly two predictions into both selected stacks");
        check.accept(fab().energy.getEnergyStored() == 1000000 - 120 * 256, "Fabrication charges exactly 60 ticks per prediction");
        check.accept(!dc().pending.isEmpty() && dc().progress[0] == 0, "A paid Data Center cycle buffers its outputs when sixteen output slots become full");
        NBTTagCompound saved = dc().writeToNBT(new NBTTagCompound()); DataCenterTile restored = new DataCenterTile(); restored.readFromNBT(saved);
        check.accept(restored.pending.size() == dc().pending.size(), "Buffered outputs survive a Data Center save/load round trip");
        for (int i = 29; i < 45; i++) dc().inventory.setStackInSlot(i, ItemStack.EMPTY);
        dc().flushPending(); check.accept(dc().pending.isEmpty() && count(dc(), Items.IRON_INGOT) == 8 && count(dc(), Hostile.item("overworld_prediction")) == 1, "Clearing space releases buffered outputs once without loss or duplication");
        var retained = world.getTileEntity(PORT).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH);
        world.setBlockToAir(DC);
        check.accept(retained.extractItem(0, 1, false).isEmpty(), "A cached IO capability cannot access inventory after the controller is broken");
    }
}
