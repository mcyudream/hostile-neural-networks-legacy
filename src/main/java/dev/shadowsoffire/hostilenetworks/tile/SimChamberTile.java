package dev.shadowsoffire.hostilenetworks.tile;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class SimChamberTile extends MachineTile {
    public int mode, progress, successes;
    private String runningModel = "";
    private int runningMode;
    public SimChamberTile() { super(4, 2, HostileConfig.simPowerCap); }
    @Override public boolean isModelSlot(int slot) { return slot == 0; }
    @Override public boolean accepts(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() == Hostile.item("data_model");
        DataModel model = ModelStack.model(inventory.getStackInSlot(0));
        return slot == 1 && model != null && model.accepts(stack);
    }
    public int duration() { return (progress > 0 ? runningMode : mode) == 0 ? 240 : 300; }
    public int cost(DataModel model) { return (int) Math.ceil(model.simCost * ((progress > 0 ? runningMode : mode) == 0 ? 1D : 1.2D)); }
    public boolean canTrain(ItemStack stack, DataModel model) {
        return HostileConfig.simModelUpgrade != 0 && model.tier(ModelStack.data(stack)) != ModelRegistry.tiers().getLast()
            && (HostileConfig.simModelUpgrade != 2 || !ModelStack.atBoundary(stack));
    }
    public void changeMode() { if (progress == 0) { mode = mode == 0 && HostileConfig.simModelUpgrade != 0 ? 1 : 0; settingsChanged(); } }
    @Override protected void tickServer() {
        flushPending();
        ItemStack source = inventory.getStackInSlot(0); DataModel model = ModelStack.model(source);
        if (model == null) { progress = 0; failure = "model"; return; }
        if (progress > 0 && !runningModel.equals(model.id.toString())) { progress = 0; markDirty(); }
        if (!model.tier(ModelStack.data(source)).canSim()) { progress = 0; failure = "faulty"; return; }
        if (!enabled()) { failure = "redstone"; return; }
        if (!pending.isEmpty()) { failure = "buffering"; return; }
        if (progress == 0) {
            if (HostileConfig.simModelUpgrade == 0) mode = 0;
            if (mode == 1 && !canTrain(source, model)) { failure = "cannot_train"; return; }
            if (!model.accepts(inventory.getStackInSlot(1))) { failure = "input"; return; }
            if (energy.getEnergyStored() < cost(model)) { failure = "energy"; return; }
            successes = model.roll(ModelStack.data(source), world.rand);
            if (mode == 0 && !canFit(outputs(model))) { failure = "output"; return; }
            inventory.extractItem(1, 1, false); runningModel = model.id.toString(); runningMode = mode;
            progress = duration(); failure = "none"; markDirty(); return;
        }
        if (!energy.spend(cost(model))) { failure = "energy_mid_cycle"; return; }
        failure = "none";
        if (--progress == 0) {
            if (runningMode == 0) emit(outputs(model));
            else if (canTrain(source, model)) {
                ModelTier previous = model.tier(ModelStack.data(source));
                ModelStack.gain(source, 1, false);
                if (HostileConfig.simModelUpgrade == 2 && model.tier(ModelStack.data(source)) != previous)
                    ModelStack.tag(source).setBoolean("awaiting_action", true);
            }
            ModelStack.iteration(source); markDirty();
        }
    }
    private List<ItemStack> outputs(DataModel model) {
        List<ItemStack> drops = new ArrayList<>();
        if (!model.baseDrop.isEmpty()) drops.add(model.baseDrop.copy());
        if (successes > 0) { ItemStack prediction = ModelStack.create(model, true); prediction.setCount(successes); drops.add(prediction); }
        return drops;
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag); tag.setInteger("mode", mode); tag.setInteger("progress", progress); tag.setInteger("successes", successes);
        tag.setString("runningModel", runningModel); tag.setInteger("runningMode", runningMode); return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag); mode = Math.clamp(tag.getInteger("mode"), 0, 1); progress = Math.clamp(tag.getInteger("progress"), 0, 300);
        successes = Math.clamp(tag.getInteger("successes"), 0, 64); runningModel = tag.getString("runningModel"); runningMode = Math.clamp(tag.getInteger("runningMode"), 0, 1);
    }
}
