package dev.shadowsoffire.hostilenetworks.dev;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.hostilenetworks.tile.*;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;

/** Runs only in the separate verification jar, against real optional-mod binaries. */
public final class IntegrationChecks {
    private record Spec(String mod, String model, String entity, int cost, int dropCount) {}
    private static final List<Spec> SPECS = List.of(
        new Spec("draconicevolution", "draconic/chaos_guardian", "draconicevolution:chaosguardian", 32768, 3),
        new Spec("thermalfoundation", "thermal/blizz", "thermalfoundation:blizz", 512, 1),
        new Spec("thermalfoundation", "thermal/blitz", "thermalfoundation:blitz", 512, 1),
        new Spec("thermalfoundation", "thermal/basalz", "thermalfoundation:basalz", 512, 1),
        new Spec("tconstruct", "tconstruct/blue_slime", "tconstruct:blueslime", 256, 3),
        new Spec("bloodmagic", "bloodmagic/demon", "bloodmagic:corrupted_zombie", 2048, 6));

    public static int loadedModelCount() { return (int) SPECS.stream().filter(s -> Loader.isModLoaded(s.mod)).count(); }
    public static List<String> loadedModels() { return SPECS.stream().filter(s -> Loader.isModLoaded(s.mod)).map(Spec::model).toList(); }

    public static void verify(WorldServer world, EntityPlayerMP player, DataCenterTile center, BiConsumer<Boolean, String> check) {
        boolean mobLoot = world.getGameRules().getBoolean("doMobLoot");
        world.getGameRules().setOrCreateGameRule("doMobLoot", "false");
        try {
            for (Spec spec : SPECS) {
                DataModel model = ModelRegistry.get(spec.model);
                if (!Loader.isModLoaded(spec.mod)) {
                    check.accept(model == null, spec.model + " is absent when " + spec.mod + " is not installed");
                    continue;
                }
                check.accept(model != null && model.target.toString().equals(spec.entity) && model.simCost == spec.cost
                    && model.drops.size() == spec.dropCount, spec.model + " resolves its real 1.12 entity, cost and all drops");
                verifyDrops(spec, model, check);
                List<ResourceLocation> targets = new ArrayList<>(); targets.add(model.target); targets.addAll(model.variants);
                if (spec.mod.equals("bloodmagic")) check.accept(targets.size() == 4, "All four Blood Magic corrupted entity variants are registered");
                for (ResourceLocation target : targets) verifyLearner(world, player, model, target, check);
                verifyMachines(world, center, model, spec.cost, check);
            }
            boolean heart = ModelRegistry.get("ender_dragon").drops.stream().anyMatch(s -> s.getItem().getRegistryName().toString().equals("draconicevolution:dragon_heart"));
            check.accept(heart == Loader.isModLoaded("draconicevolution"), "Ender Dragon dragon-heart output follows the optional Draconic Evolution installation");
            DataModel dragon = ModelRegistry.get("ender_dragon");
            verifyPartBindings(world, player, dragon, entity(world, dragon.target), check);
        } finally {
            world.getGameRules().setOrCreateGameRule("doMobLoot", Boolean.toString(mobLoot));
            player.inventory.clear(); clear(center);
        }
    }

    private static void drop(DataModel model, int index, String id, int meta, int count, BiConsumer<Boolean, String> check) {
        ItemStack stack = model.drops.get(index);
        check.accept(stack.getItem().getRegistryName().toString().equals(id) && stack.getMetadata() == meta && stack.getCount() == count,
            model.id + " output " + index + " is " + count + "x " + id + "@" + meta);
    }
    private static void verifyDrops(Spec spec, DataModel model, BiConsumer<Boolean, String> check) {
        switch (spec.model) {
            case "thermal/blizz" -> drop(model, 0, "thermalfoundation:material", 2048, 16, check);
            case "thermal/blitz" -> drop(model, 0, "thermalfoundation:material", 2050, 16, check);
            case "thermal/basalz" -> drop(model, 0, "thermalfoundation:material", 2052, 16, check);
            case "tconstruct/blue_slime" -> {
                drop(model, 0, "tconstruct:edible", 1, 32, check);
                drop(model, 1, "tconstruct:edible", 2, 16, check);
                drop(model, 2, "tconstruct:edible", 4, 8, check);
            }
            case "draconic/chaos_guardian" -> {
                drop(model, 0, "draconicevolution:chaos_shard", 0, 1, check);
                drop(model, 1, "draconicevolution:dragon_heart", 0, 1, check);
                drop(model, 2, "draconicevolution:draconium_dust", 0, 64, check);
            }
            case "bloodmagic/demon" -> {
                drop(model, 0, "bloodmagic:monster_soul", 0, 1, check);
                ItemStack soul = model.drops.getFirst().copy();
                try {
                    var item = soul.getItem();
                    double will = ((Number) item.getClass().getMethod("getWill", ItemStack.class).invoke(item, soul)).doubleValue();
                    double drained = ((Number) item.getClass().getMethod("drainWill", ItemStack.class, double.class).invoke(item, soul, 10D)).doubleValue();
                    check.accept(will == 50D && drained == 10D && soul.getTagCompound().getDouble("souls") == 40D,
                        "Blood Magic API reads and drains the fabricated soul's 50 usable will");
                    for (int i = 0; i < 5; i++) {
                        drop(model, i + 1, "bloodmagic:item_demon_crystal", i, 1, check);
                        ItemStack crystal = model.drops.get(i + 1).copy();
                        double crystalWill = ((Number) crystal.getItem().getClass().getMethod("getWill", ItemStack.class).invoke(crystal.getItem(), crystal)).doubleValue();
                        check.accept(crystalWill == 50D, "Blood Magic API recognizes 50 will in crystal variant " + i);
                    }
                } catch (ReflectiveOperationException error) { throw new IllegalStateException("Blood Magic runtime API verification failed", error); }
            }
        }
    }

    private static EntityLivingBase entity(WorldServer world, ResourceLocation id) {
        Entity entity = EntityList.createEntityByIDFromName(id, world);
        if (!(entity instanceof EntityLivingBase living)) throw new IllegalStateException("Cannot create real entity " + id);
        living.setPosition(6, 5, 6); return living;
    }
    private static void verifyLearner(WorldServer world, EntityPlayerMP player, DataModel model, ResourceLocation target, BiConsumer<Boolean, String> check) {
        player.inventory.clear(); player.inventory.currentItem = 0;
        player.setHeldItem(EnumHand.MAIN_HAND, Hostile.stack("blank_data_model"));
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, EnumHand.MAIN_HAND, entity(world, target)));
        ItemStack attuned = player.getHeldItemMainhand();
        check.accept(sameModel(attuned, model), "Right-clicking real " + target + " uniquely binds " + model.id);
        verifyPartBindings(world, player, model, entity(world, target), check);
        ItemStack learner = Hostile.stack("deep_learner"); DeepLearnerItem.inventory(learner).setStackInSlot(0, attuned);
        player.setHeldItem(EnumHand.MAIN_HAND, learner);
        int kills = target.toString().equals("draconicevolution:chaosguardian") ? 1 : 6;
        // Lethal damage calls Forge's actual death hook. Do not spawn/tick a dying Chaos Guardian:
        // its death animation intentionally scans and alters an entire chaos island.
        for (int i = 0; i < kills; i++) {
            EntityLivingBase victim = entity(world, target); victim.setHealth(1);
            Entity[] parts = victim.getParts();
            if (parts != null && parts.length > 0) parts[0].attackEntityFrom(DamageSource.causePlayerDamage(player), 50F);
            else victim.attackEntityFrom(DamageSource.causePlayerDamage(player), 1000F);
        }
        ItemStack trained = DeepLearnerItem.inventory(learner).getStackInSlot(0);
        check.accept(model.tier(ModelStack.data(trained)).id().equals("basic"), kills + " lethal player hits on real " + target + " train the bound learner to Basic");
        ItemStack restored = new ItemStack(learner.writeToNBT(new NBTTagCompound()));
        check.accept(ModelStack.data(DeepLearnerItem.inventory(restored).getStackInSlot(0)) == ModelStack.data(trained), target + " learner progress survives NBT serialization");
    }

    private static void verifyPartBindings(WorldServer world, EntityPlayerMP player, DataModel model, EntityLivingBase entity, BiConsumer<Boolean, String> check) {
        Entity[] parts = entity.getParts(); if (parts == null || parts.length == 0) return;
        boolean allBound = true;
        for (Entity part : parts) {
            player.setHeldItem(EnumHand.MAIN_HAND, Hostile.stack("blank_data_model"));
            MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, EnumHand.MAIN_HAND, part));
            allBound &= sameModel(player.getHeldItemMainhand(), model);
        }
        check.accept(allBound, model.id + " binds through all " + parts.length + " real head, body, tail and wing hitboxes");
    }

    private static boolean sameModel(ItemStack stack, DataModel expected) {
        // Login sync can replace registry instances in an integrated server.
        // Item NBT identifies models by registry ID, rather than object identity.
        DataModel actual = ModelStack.model(stack); return actual != null && actual.id.equals(expected.id);
    }

    private static void clear(MachineTile tile) {
        for (int i = 0; i < tile.inventory.getSlots(); i++) tile.inventory.setStackInSlot(i, ItemStack.EMPTY);
        tile.pending.clear();
        if (tile instanceof DataCenterTile dc) {
            Arrays.fill(dc.progress, 0); Arrays.fill(dc.successes, 0); Arrays.fill(dc.runningModels, "");
            dc.selections.read(new NBTTagCompound());
        }
    }
    private static long cycle(MachineTile tile, int ticks) {
        long spent = 0;
        for (int i = 0; i < ticks; i++) {
            tile.energy.set(tile.energy.getMaxEnergyStored()); int before = tile.energy.getEnergyStored();
            tile.update(); spent += before - tile.energy.getEnergyStored();
        }
        return spent;
    }
    private static int count(MachineTile tile, ItemStack expected) {
        int result = 0;
        for (int i = tile.getOutputStart(); i < tile.inventory.getSlots(); i++) {
            ItemStack actual = tile.inventory.getStackInSlot(i);
            if (ItemStack.areItemsEqual(actual, expected) && ItemStack.areItemStackTagsEqual(actual, expected)) result += actual.getCount();
        }
        return result;
    }
    private static ItemStack selfAware(DataModel model) {
        ItemStack stack = ModelStack.create(model, false); ModelStack.setData(stack, model.required(ModelRegistry.tiers().getLast())); return stack;
    }
    private static void verifyMachines(WorldServer world, DataCenterTile center, DataModel model, int cost, BiConsumer<Boolean, String> check) {
        SimChamberTile sim = new SimChamberTile(); sim.setWorld(world); sim.setPos(MachineChecks.SIM);
        sim.inventory.setStackInSlot(0, selfAware(model)); sim.inventory.setStackInSlot(1, model.inputs.getFirst().copy());
        sim.inventory.getStackInSlot(1).setCount(1);
        long spent = cycle(sim, 241);
        ItemStack prediction = ModelStack.create(model, true);
        check.accept(sim.progress == 0 && sim.inventory.getStackInSlot(1).isEmpty() && count(sim, prediction) == 1
            && count(sim, model.baseDrop) == model.baseDrop.getCount() && spent == 240L * cost,
            model.id + " completes real inference with exact input, prediction, base drop and FE cost");
        for (int index = 0; index < model.drops.size(); index++) {
            LootFabTile fab = new LootFabTile(); fab.setWorld(world); fab.setPos(MachineChecks.FAB);
            fab.inventory.setStackInSlot(0, prediction.copy()); fab.selections.select(model, index);
            long fabSpent = cycle(fab, 60); ItemStack output = model.drops.get(index);
            check.accept(fab.inventory.getStackInSlot(0).isEmpty() && count(fab, output) == output.getCount() && fabSpent == 60L * 256,
                model.id + " fabricates output " + index + " with exact metadata, NBT, count and FE cost");
            LootFabTile restored = new LootFabTile(); restored.readFromNBT(fab.writeToNBT(new NBTTagCompound()));
            check.accept(count(restored, output) == output.getCount(), model.id + " fabricated output " + index + " survives machine save/load");
        }
        clear(center); center.inventory.setStackInSlot(0, selfAware(model));
        center.inventory.setStackInSlot(25, model.inputs.getFirst().copy()); center.inventory.getStackInSlot(25).setCount(1);
        center.selections.select(model, 0);
        long centerSpent = cycle(center, 301);
        check.accept(center.shellValid && center.progress[0] == 0 && center.inventory.getStackInSlot(25).isEmpty()
            && count(center, model.drops.getFirst()) == model.drops.getFirst().getCount()
            && count(center, model.baseDrop) == model.baseDrop.getCount() && centerSpent == 300L * (int) Math.ceil(cost * 1.5D),
            model.id + " runs in the formed Data Center with correct output and exact FE cost");
        clear(center);
    }
}
