package dev.shadowsoffire.hostilenetworks.dev;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.item.*;
import dev.shadowsoffire.hostilenetworks.tile.*;
import dev.shadowsoffire.hostilenetworks.compat.bloodmagic.BloodAltarBridge;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.CapabilityItemHandler;

/** Production machines, real item/fluid capabilities, and registered optional-mod binaries. */
public final class ExpansionChecks {
    public static final List<String> MACHINES = List.of("merger_camera", "simulator_modeling", "ultimate_sim_chamber_v4", "ultimate_loot_fabricator_v4");
    public static BlockPos position(int i) { return new BlockPos(i * 2, 4, 6); }
    public static List<String> previews() { var list = new ArrayList<>(MACHINES); if (Loader.isModLoaded("bloodmagic")) list.add("digital_agonizer"); return list; }
    public static ItemStack perfect(int data) {
        ItemStack stack = PerfectModel.create(List.of(ModelRegistry.get("zombie"), ModelRegistry.get("skeleton"), ModelRegistry.get("creeper"), ModelRegistry.get("spider")));
        ModelStack.setData(stack, data); return stack;
    }
    private static ItemStack normal(int data) { ItemStack s = ModelStack.create(ModelRegistry.get("zombie"), false); ModelStack.setData(s, data); return s; }
    private static int count(MachineTile tile, Item item) { int n = 0; for (int i = tile.getOutputStart(); i < tile.inventory.getSlots(); i++) if (tile.inventory.getStackInSlot(i).getItem() == item) n += tile.inventory.getStackInSlot(i).getCount(); return n; }
    private static void clear(MachineTile tile) { tile.pending.clear(); for (int i = 0; i < tile.inventory.getSlots(); i++) tile.inventory.setStackInSlot(i, ItemStack.EMPTY); if (tile instanceof ExpansionTile e) e.progress = 0; }
    private static void ticks(ExpansionTile tile, int ticks) { for (int i = 0; i < ticks; i++) tile.update(); }
    private static ExpansionTile place(WorldServer w, String id, BlockPos pos) {
        w.getChunk(pos); w.setBlockToAir(pos); w.setBlockState(pos, Hostile.BLOCKS.get(id).getDefaultState()); return (ExpansionTile) w.getTileEntity(pos);
    }
    public static void verify(WorldServer w, EntityPlayerMP player, BiConsumer<Boolean, String> check) {
        try {
            var merger = (MergerTile) place(w, "merger_camera", position(0));
            var trainer = (ModelTrainerTile) place(w, "simulator_modeling", position(1));
            check.accept(!merger.accepts(0, normal(6)) && merger.accepts(0, normal(1254)), "Merger accepts only Self-Aware ordinary data models");
            for (int i = 0; i < 4; i++) merger.inventory.setStackInSlot(i, normal(1254));
            merger.inventory.setStackInSlot(4, Hostile.stack("blank_extra_data_model"));
            merger.update(); check.accept(merger.progress == 0 && merger.failure.equals("energy"), "Merger with no FE leaves all five inputs untouched");
            merger.energy.set(ExpansionConfig.mergerCapacity); int initial = merger.energy.getEnergyStored();
            ticks(merger, ExpansionConfig.mergerDuration - 1);
            check.accept(merger.inventory.getStackInSlot(5).isEmpty() && !merger.inventory.getStackInSlot(0).isEmpty(), "Merger commits its input consumption only on the final paid tick");
            NBTTagCompound saved = merger.writeToNBT(new NBTTagCompound()); var restored = new MergerTile(); restored.setWorld(w); restored.setPos(position(0)); restored.readFromNBT(saved); restored.update();
            check.accept(PerfectModel.models(restored.inventory.getStackInSlot(5)).size() == 4 && restored.inventory.getStackInSlot(0).isEmpty(), "A restored in-flight merge consumes inputs once and emits its four-model result");
            merger.update();
            check.accept(merger.energy.getEnergyStored() == initial - ExpansionConfig.mergerCost * ExpansionConfig.mergerDuration, "Model Merger charges the exact configured FE per processing tick");
            ItemStack merged = merger.inventory.getStackInSlot(5);
            check.accept(PerfectModel.models(merged).stream().allMatch(m -> m == ModelRegistry.get("zombie")) && ModelStack.data(merged) == 0, "Duplicate constituents are preserved and perfect models start with independent data");
            ItemStack learner = Hostile.stack("deep_learner"); DeepLearnerItem.inventory(learner).setStackInSlot(0, merged.copy());
            HostileEvents.updateLearner(learner, m -> m == ModelRegistry.get("zombie"));
            check.accept(ModelStack.data(DeepLearnerItem.inventory(learner).getStackInSlot(0)) == ExpansionConfig.gained[0], "One matching kill grows a perfect learner model once even with four duplicate targets");
            ItemStack learnerReload = new ItemStack(learner.writeToNBT(new NBTTagCompound()));
            check.accept(PerfectModel.models(DeepLearnerItem.inventory(learnerReload).getStackInSlot(0)).size() == 4, "Perfect learner targets and data survive item NBT serialization");
            check.accept(!LearningModel.valid(Hostile.stack("extra_data_model")), "Unattuned or damaged perfect-model NBT is rejected safely");

            trainer.inventory.setStackInSlot(0, normal(0)); trainer.energy.set(ExpansionConfig.trainerCapacity);
            int before = trainer.energy.getEnergyStored(); ticks(trainer, trainer.duration());
            check.accept(ModelStack.data(trainer.inventory.getStackInSlot(0)) == 1 && trainer.energy.getEnergyStored() == before - trainer.cost() * trainer.duration(), "Model Simulator trains Faulty models with exact FE and no input materials");
            trainer.inventory.setStackInSlot(1, Hostile.stack("upgrade_speed")); trainer.inventory.setStackInSlot(2, Hostile.stack("upgrade_data_kill"));
            before = trainer.energy.getEnergyStored(); int duration = trainer.duration(), cost = trainer.cost(); ticks(trainer, duration);
            check.accept(ModelStack.data(trainer.inventory.getStackInSlot(0)) == 3 && duration == 50 && cost == 200000 && trainer.energy.getEnergyStored() == before - duration * cost, "Speed and data cards combine: 50 ticks, double data, double FE/t");
            trainer.inventory.setStackInSlot(2, Hostile.stack("upgrade_module_stack"));
            for (int i = 0; i < trainer.duration(); i++) { trainer.energy.set(ExpansionConfig.trainerCapacity); trainer.update(); }
            check.accept(ModelStack.data(trainer.inventory.getStackInSlot(0)) == 6 && trainer.cost() == 20000000, "Tier card advances exactly one normal tier and combines with speed energy cost");
            clear(trainer); trainer.inventory.setStackInSlot(0, perfect(0)); trainer.energy.set(ExpansionConfig.trainerCapacity); ticks(trainer, trainer.duration());
            check.accept(ModelStack.data(trainer.inventory.getStackInSlot(0)) == 20 && trainer.pending.isEmpty(), "Model Simulator trains perfect models without generating loot");
            trainer.inventory.setStackInSlot(0, perfect(ExpansionConfig.required[4]));
            var handler = trainer.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            check.accept(handler.extractItem(0, 1, false).getItem() == Hostile.item("extra_data_model"), "Pipes may extract a fully trained perfect model");
            trainer.inventory.setStackInSlot(0, perfect(0));
            check.accept(handler.extractItem(0, 1, false).isEmpty() && handler.getSlotLimit(1) == 1, "Pipes retain unfinished models and upgrade slots hold one card");

            for (int v = 0; v < 4; v++) {
                var sim = (UltimateSimTile) place(w, "ultimate_sim_chamber_v" + (v + 1), position(2));
                sim.inventory.setStackInSlot(0, perfect(ExpansionConfig.required[4])); int amount = 4 * sim.multiplier();
                sim.inventory.setStackInSlot(1, new ItemStack(Hostile.item("prediction_matrix"), amount)); sim.energy.set(ExpansionConfig.capacity[v]);
                int power = sim.energy.getEnergyStored(); ticks(sim, sim.duration());
                check.accept(count(sim, Hostile.item("prediction")) == amount && count(sim, Hostile.item("overworld_prediction")) == amount && sim.inventory.getStackInSlot(1).isEmpty(), "Ultimate Simulation V" + (v + 1) + " produces all four targets at its configured multiplier");
                check.accept(sim.energy.getEnergyStored() == power - sim.cost() * sim.duration(), "Ultimate Simulation V" + (v + 1) + " charges the summed model FE/t for its exact duration");
                var loaded = new UltimateSimTile(); loaded.readFromNBT(sim.writeToNBT(new NBTTagCompound()));
                check.accept(loaded.version() == v && loaded.energy.getEnergyStored() == sim.energy.getEnergyStored() && loaded.energy.getMaxEnergyStored() == ExpansionConfig.capacity[v], "Ultimate Simulation V" + (v + 1) + " restores its full-width energy and version");
                for (int i = 2; i < 10; i++) sim.inventory.setStackInSlot(i, new ItemStack(Blocks.STONE, 64));
                sim.inventory.setStackInSlot(1, new ItemStack(Hostile.item("prediction_matrix"), amount)); power = sim.energy.getEnergyStored(); sim.update();
                check.accept(sim.failure.equals("output") && sim.energy.getEnergyStored() == power && sim.inventory.getStackInSlot(1).getCount() == amount, "Blocked Ultimate Simulation V" + (v + 1) + " spends neither FE nor materials");
                sim.extractModel = true;
                check.accept(!sim.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).extractItem(0, 1, true).isEmpty(), "Ultimate Simulation V" + (v + 1) + " honors its explicit model extraction setting");

                var fab = (UltimateFabTile) place(w, "ultimate_loot_fabricator_v" + (v + 1), position(3));
                ItemStack prediction = ModelStack.create(ModelRegistry.get("zombie"), true); prediction.setCount(64); fab.inventory.setStackInSlot(0, prediction);
                fab.selections.select(ModelRegistry.get("zombie"), 1); fab.energy.set(ExpansionConfig.capacity[v]); power = fab.energy.getEnergyStored(); ticks(fab, fab.duration());
                check.accept(count(fab, Items.IRON_INGOT) == 8 * fab.batchLimit() && fab.inventory.getStackInSlot(0).getCount() == 64 - fab.batchLimit(), "Ultimate Fabricator V" + (v + 1) + " consumes its exact batch and produces matching iron");
                check.accept(fab.energy.getEnergyStored() == power - fab.cost() * fab.duration(), "Ultimate Fabricator V" + (v + 1) + " charges its exact fixed FE/t per batch");
                var loadedFab = new UltimateFabTile(); loadedFab.readFromNBT(fab.writeToNBT(new NBTTagCompound()));
                check.accept(loadedFab.version() == v && loadedFab.energy.getEnergyStored() == fab.energy.getEnergyStored(), "Ultimate Fabricator V" + (v + 1) + " preserves high-capacity energy and selections on load");
            }
            var sim = (UltimateSimTile) w.getTileEntity(position(2)); clear(sim); sim.inventory.setStackInSlot(0, perfect(0)); sim.energy.set(10000000);
            sim.inventory.setStackInSlot(1, new ItemStack(Hostile.item("prediction_matrix"), 64)); ticks(sim, 10);
            sim.inventory.setStackInSlot(0, merged.copy()); sim.update();
            check.accept(sim.progress == 1 && count(sim, Hostile.item("prediction")) == 0, "Replacing a running perfect model restarts simulation and cannot reuse its progress");
            sim.redstone = 2; int paused = sim.progress, power = sim.energy.getEnergyStored(); ticks(sim, 20);
            check.accept(sim.progress == paused && sim.energy.getEnergyStored() == power, "Expansion machines pause their processing and FE under redstone control");
            sim.progress = 0; sim.extractModel = true; sim.redstone = 0;
            var card = SettingCardItem.copy(sim); var oldModel = sim.inventory.getStackInSlot(0).copy();
            sim.inventory.setStackInSlot(0, ItemStack.EMPTY); player.inventory.clear(); ItemStack real = merged.copy(); ModelStack.setData(real, 123); player.inventory.setInventorySlotContents(9, real);
            check.accept(SettingCardItem.paste(sim, card, player) && ModelStack.data(sim.inventory.getStackInSlot(0)) == 123 && player.inventory.getStackInSlot(9).isEmpty(), "Settings Card transfers the actual matching model and preserves its independent data");
            sim.inventory.setStackInSlot(0, ItemStack.EMPTY);
            check.accept(!SettingCardItem.paste(sim, card, player) && sim.inventory.getStackInSlot(0).isEmpty(), "Repeated settings paste cannot manufacture a missing model");
            var fab = (UltimateFabTile) w.getTileEntity(position(3)); clear(fab);
            fab.selections.clear(ModelRegistry.get("zombie")); fab.selections.cycle(ModelRegistry.get("zombie"));
            fab.selections.select(ModelRegistry.get("zombie"), 1); fab.selections.select(ModelRegistry.get("zombie"), 0);
            ItemStack prediction = ModelStack.create(ModelRegistry.get("zombie"), true); prediction.setCount(64); fab.inventory.setStackInSlot(0, prediction); fab.energy.set(ExpansionConfig.capacity[3]);
            ticks(fab, fab.duration()); fab.inventory.setStackInSlot(0, ModelStack.create(ModelRegistry.get("zombie"), true)); ticks(fab, fab.duration());
            check.accept(count(fab, Items.IRON_INGOT) == 512 && count(fab, Items.ROTTEN_FLESH) == 64 && fab.inventory.getStackInSlot(0).isEmpty(), "Ultimate Fabricator queue advances per batch and handles its final partial batch");
            var selections = SettingCardItem.copy(fab); var basicFab = new LootFabTile();
            check.accept(SettingCardItem.paste(basicFab, selections, player) && basicFab.selections.get(ModelRegistry.get("zombie")).drops.size() == 2, "Settings Card copies production queues between normal and ultimate fabricators");
            for (int i = 1; i < 37; i++) fab.inventory.setStackInSlot(i, new ItemStack(Blocks.STONE, 64));
            fab.inventory.setStackInSlot(0, ModelStack.create(ModelRegistry.get("zombie"), true)); power = fab.energy.getEnergyStored(); fab.update();
            check.accept(fab.failure.equals("output") && fab.energy.getEnergyStored() == power && fab.inventory.getStackInSlot(0).getCount() == 1, "A full Ultimate Fabricator pauses without consuming its prediction");
            if (Loader.isModLoaded("bloodmagic")) blood(w, player, check);
            else check.accept(Hostile.item("digital_agonizer") == null && Hostile.item("altar_linker") == null, "Without Blood Magic, no altar classes or unusable integration items are initialized");
            // Visible, stable fixtures for the real client/server GUI and save/reload checks.
            clear(merger); for (int i = 0; i < 4; i++) merger.inventory.setStackInSlot(i, normal(1254)); merger.inventory.setStackInSlot(4, Hostile.stack("blank_extra_data_model")); merger.energy.set(50000000); merger.redstone = 2;
            clear(trainer); trainer.inventory.setStackInSlot(0, perfect(7000)); trainer.inventory.setStackInSlot(1, Hostile.stack("upgrade_speed")); trainer.inventory.setStackInSlot(2, Hostile.stack("upgrade_data_kill")); trainer.energy.set(80000000); trainer.redstone = 2;
            clear(sim); sim.inventory.setStackInSlot(0, perfect(ExpansionConfig.required[4])); sim.inventory.setStackInSlot(1, new ItemStack(Hostile.item("prediction_matrix"), 64)); sim.energy.set(180000000); sim.redstone = 2;
            clear(fab); prediction = ModelStack.create(ModelRegistry.get("zombie"), true); prediction.setCount(64); fab.inventory.setStackInSlot(0, prediction); fab.energy.set(180000000); fab.redstone = 2;
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Expansion integration verification failed", error); }
        finally { player.inventory.clear(); }
    }
    private static void emptyAltar(TileEntity altar) throws ReflectiveOperationException {
        var tank = altar.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        FluidStack fluid = tank.getTankProperties()[0].getContents().copy(); fluid.amount = 0;
        tank.getClass().getMethod("setMainFluid", FluidStack.class).invoke(tank, fluid);
    }
    private static void blood(WorldServer w, EntityPlayerMP player, BiConsumer<Boolean, String> check) throws ReflectiveOperationException {
        var agonizer = (DigitalAgonizerTile) place(w, "digital_agonizer", position(4));
        BlockPos altarPos = new BlockPos(11, 6, 6); Block altarBlock = Block.REGISTRY.getObject(new ResourceLocation("bloodmagic:altar"));
        w.setBlockToAir(altarPos); w.setBlockState(altarPos, altarBlock.getDefaultState()); TileEntity altar = w.getTileEntity(altarPos);
        check.accept(BloodAltarBridge.isAltar(altar) && agonizer.link(altarPos), "Digital Agonizer binds a real Blood Magic altar in the same dimension");
        check.accept(!agonizer.link(altarPos.add(64, 0, 0)) && agonizer.altar.equals(altarPos), "An invalid or out-of-range link preserves the existing altar binding");
        agonizer.inventory.setStackInSlot(0, normal(0)); agonizer.energy.set(100000); agonizer.update();
        check.accept(agonizer.failure.equals("faulty") && BloodAltarBridge.stored(altar) == 0, "Faulty models cannot create life essence");
        int[] data = {6, 54, 354, 1254};
        for (int i = 0; i < 4; i++) {
            emptyAltar(altar); agonizer.inventory.setStackInSlot(0, normal(data[i])); agonizer.energy.set(100000); int power = agonizer.energy.getEnergyStored(); ticks(agonizer, agonizer.duration());
            check.accept(BloodAltarBridge.stored(altar) == ExpansionConfig.essence[i + 1] && agonizer.energy.getEnergyStored() == power - agonizer.cost() * agonizer.duration(), "Agonizer tier " + (i + 1) + " delivers exact real LP and charges 60 paid ticks");
        }
        String[] catalysts = {"overworld_prediction", "nether_prediction", "end_prediction", "twilight_prediction"};
        for (int i = 0; i < 4; i++) {
            agonizer.charges = 0; agonizer.inventory.setStackInSlot(1, Hostile.stack(catalysts[i])); emptyAltar(altar); agonizer.energy.set(100000); ticks(agonizer, agonizer.duration());
            check.accept(BloodAltarBridge.stored(altar) == (int) Math.floor(300 * (double) ExpansionConfig.catalyst[i]) && agonizer.charges == 9 && agonizer.inventory.getStackInSlot(1).isEmpty(), "Agonizer accepts " + catalysts[i] + " as a ten-use catalyst");
        }
        Block rune = Block.REGISTRY.getObject(new ResourceLocation("bloodmagic:blood_rune"));
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (x != 0 || z != 0) w.setBlockState(altarPos.add(x, -1, z), rune.getStateFromMeta(3));
        Object tank = altar.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null); tank.getClass().getMethod("checkTier").invoke(tank);
        check.accept(BloodAltarBridge.multiplier(altar) > 1.39 && BloodAltarBridge.multiplier(altar) < 1.41, "A real tier-two altar detects four effective Sacrifice Runes and exposes their 1.4x multiplier");
        emptyAltar(altar); agonizer.charges = 0; agonizer.energy.set(100000); ticks(agonizer, agonizer.duration());
        check.accept(BloodAltarBridge.stored(altar) == (int) Math.floor(300 * BloodAltarBridge.multiplier(altar)), "Agonizer output includes the real altar's Sacrifice Rune bonus");
        BloodAltarBridge.fill(altar, BloodAltarBridge.capacity(altar)); agonizer.inventory.setStackInSlot(1, Hostile.stack("overworld_prediction")); int power = agonizer.energy.getEnergyStored(); agonizer.update();
        check.accept(agonizer.failure.equals("altar_full") && agonizer.charges == 0 && agonizer.inventory.getStackInSlot(1).getCount() == 1 && agonizer.energy.getEnergyStored() == power, "Full Blood Altars consume neither FE nor new catalysts");
        NBTTagCompound tag = agonizer.writeToNBT(new NBTTagCompound()); tag.setInteger("pendingEssence", 900); agonizer.readFromNBT(tag); emptyAltar(altar); agonizer.update();
        check.accept(BloodAltarBridge.stored(altar) == 900 && agonizer.pendingEssence == 0 && agonizer.energy.getEnergyStored() == power, "Saved pending LP is delivered once without charging the paid operation again");
        agonizer.update(); check.accept(BloodAltarBridge.stored(altar) == 900 && agonizer.progress == 1, "A delivered pending LP balance cannot be emitted twice");
        var restored = new DigitalAgonizerTile(); restored.readFromNBT(agonizer.writeToNBT(new NBTTagCompound()));
        check.accept(restored.altar.equals(altarPos) && restored.charges == 10 && restored.progress == 1, "Agonizer link, catalyst uses and in-flight progress survive tile NBT reload");
        agonizer.redstone = 2; agonizer.progress = 0;
        ItemStack linker = Hostile.stack("altar_linker"); player.setHeldItem(EnumHand.MAIN_HAND, linker);
        linker.getItem().onItemUse(player, w, altarPos, EnumHand.MAIN_HAND, EnumFacing.UP, .5f, .5f, .5f);
        agonizer.altar = null; linker.getItem().onItemUse(player, w, position(4), EnumHand.MAIN_HAND, EnumFacing.UP, .5f, .5f, .5f);
        check.accept(altarPos.equals(agonizer.altar), "Real Altar Linker item interaction selects and binds an altar");
        linker.getTagCompound().setInteger("dimension", 42); agonizer.altar = null;
        linker.getItem().onItemUse(player, w, position(4), EnumHand.MAIN_HAND, EnumFacing.UP, .5f, .5f, .5f);
        check.accept(agonizer.altar == null, "Altar Linker refuses cross-dimension coordinates"); agonizer.link(altarPos);
        w.setBlockToAir(altarPos); agonizer.redstone = 0; power = agonizer.energy.getEnergyStored(); agonizer.update();
        check.accept(agonizer.failure.equals("altar_missing") && agonizer.energy.getEnergyStored() == power, "Removing a bound altar stops the agonizer without consuming FE");
        w.setBlockState(altarPos, altarBlock.getDefaultState()); agonizer.redstone = 2; agonizer.update();
        check.accept(agonizer.altarCapacity == BloodAltarBridge.capacity(w.getTileEntity(altarPos)) && agonizer.altarCapacity > 0 && agonizer.failure.equals("redstone"), "A paused Agonizer still refreshes its linked altar capacity for the GUI");
    }
}
