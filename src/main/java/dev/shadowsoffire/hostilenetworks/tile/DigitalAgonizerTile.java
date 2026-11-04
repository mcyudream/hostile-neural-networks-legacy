package dev.shadowsoffire.hostilenetworks.tile;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.compat.bloodmagic.BloodAltarBridge;
import dev.shadowsoffire.hostilenetworks.data.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;

/** Independent HNN implementation of the public Digital Mob Agonizer gameplay. */
public final class DigitalAgonizerTile extends ExpansionTile {
    public BlockPos altar;
    public int charges, pendingEssence, altarBlood, altarCapacity, lastAmount, cycleEssence;
    public double catalystMultiplier = 1, sacrificeMultiplier = 1;
    public DigitalAgonizerTile() { super(2, 2, ExpansionConfig.agonizerCapacity); }
    @Override public int duration() { return ExpansionConfig.agonizerDuration; }
    @Override public int cost() { return ExpansionConfig.agonizerCost; }
    @Override public boolean isModelSlot(int slot) { return slot == 0; }
    public static int catalystIndex(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        String[] ids = {"overworld_prediction", "nether_prediction", "end_prediction", "twilight_prediction"};
        for (int i = 0; i < ids.length; i++) if (stack.getItem() == Hostile.item(ids[i])) return i;
        return -1;
    }
    @Override public boolean accepts(int slot, ItemStack stack) {
        return slot == 0 ? stack.getItem() == Hostile.item("data_model") && ModelStack.model(stack) != null : slot == 1 && catalystIndex(stack) >= 0;
    }
    public boolean link(BlockPos target) {
        if (!Loader.isModLoaded("bloodmagic") || world == null || target.distanceSq(pos) > (long) ExpansionConfig.linkRange * ExpansionConfig.linkRange
            || !world.isBlockLoaded(target) || !BloodAltarBridge.isAltar(world.getTileEntity(target))) return false;
        altar = target.toImmutable(); settingsChanged(); return true;
    }
    public TileEntity loadedAltar() {
        if (!Loader.isModLoaded("bloodmagic") || altar == null || world == null || !world.isBlockLoaded(altar)
            || altar.distanceSq(pos) > (long) ExpansionConfig.linkRange * ExpansionConfig.linkRange) return null;
        TileEntity tile = world.getTileEntity(altar); return BloodAltarBridge.isAltar(tile) ? tile : null;
    }
    private int baseEssence(ItemStack stack) {
        DataModel model = ModelStack.model(stack); if (model == null) return 0;
        int tier = ModelRegistry.tiers().indexOf(model.tier(ModelStack.data(stack)));
        return tier <= 0 ? 0 : ExpansionConfig.essence[Math.min(4, tier)];
    }
    public int predictedEssence() {
        int index = catalystIndex(inventory.getStackInSlot(1));
        double multiplier = charges > 0 ? catalystMultiplier : index >= 0 ? ExpansionConfig.catalyst[index] : 1;
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(baseEssence(inventory.getStackInSlot(0)) * multiplier * sacrificeMultiplier));
    }
    @Override protected void tickServer() {
        TileEntity tile = loadedAltar();
        if (tile == null) { altarBlood = altarCapacity = 0; if (ready()) failure = "altar_missing"; return; }
        altarBlood = BloodAltarBridge.stored(tile); altarCapacity = BloodAltarBridge.capacity(tile); sacrificeMultiplier = BloodAltarBridge.multiplier(tile);
        if (!ready()) return;
        if (pendingEssence > 0) {
            int accepted = BloodAltarBridge.fill(tile, Math.min(pendingEssence, Math.max(0, altarCapacity - altarBlood)));
            pendingEssence -= accepted; markDirty(); failure = pendingEssence == 0 ? "none" : "altar_full"; return;
        }
        ItemStack source = inventory.getStackInSlot(0); changedInputs(source.writeToNBT(new NBTTagCompound()).toString());
        if (!accepts(0, source)) { progress = 0; failure = "model"; return; }
        if (baseEssence(source) == 0) { progress = 0; failure = "faulty"; return; }
        int amount = progress == 0 ? predictedEssence() : cycleEssence;
        if (altarCapacity - altarBlood < amount) { failure = "altar_full"; return; }
        if (energy.getEnergyStored() < cost()) { failure = "energy"; return; }
        if (progress == 0) {
            int index = catalystIndex(inventory.getStackInSlot(1));
            if (charges == 0 && index >= 0) { catalystMultiplier = ExpansionConfig.catalyst[index]; charges = 10; inventory.extractItem(1, 1, false); }
            cycleEssence = amount;
        }
        if (payTick() && progress >= duration()) {
            lastAmount = cycleEssence; pendingEssence = Math.max(0, cycleEssence - BloodAltarBridge.fill(tile, cycleEssence));
            if (charges > 0 && --charges == 0) catalystMultiplier = 1;
            progress = 0; settingsChanged();
        }
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound n) {
        super.writeToNBT(n); if (altar != null) n.setLong("altar", altar.toLong());
        n.setInteger("charges", charges); n.setDouble("catalystMultiplier", catalystMultiplier); n.setDouble("sacrificeMultiplier", sacrificeMultiplier);
        n.setInteger("pendingEssence", pendingEssence); n.setInteger("cycleEssence", cycleEssence); n.setInteger("lastAmount", lastAmount);
        n.setInteger("altarBlood", altarBlood); n.setInteger("altarCapacity", altarCapacity); return n;
    }
    @Override public void readFromNBT(NBTTagCompound n) {
        super.readFromNBT(n); altar = n.hasKey("altar", 4) ? BlockPos.fromLong(n.getLong("altar")) : null;
        charges = Math.clamp(n.getInteger("charges"), 0, 10); catalystMultiplier = n.getDouble("catalystMultiplier");
        if (!Double.isFinite(catalystMultiplier)) catalystMultiplier = 1; catalystMultiplier = Math.clamp(catalystMultiplier, 1, 10);
        sacrificeMultiplier = n.getDouble("sacrificeMultiplier"); if (!Double.isFinite(sacrificeMultiplier) || sacrificeMultiplier < 1) sacrificeMultiplier = 1;
        pendingEssence = Math.max(0, n.getInteger("pendingEssence")); cycleEssence = Math.max(0, n.getInteger("cycleEssence")); lastAmount = n.getInteger("lastAmount");
        altarBlood = n.getInteger("altarBlood"); altarCapacity = n.getInteger("altarCapacity");
    }
}
