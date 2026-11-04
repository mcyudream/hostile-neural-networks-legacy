package dev.shadowsoffire.hostilenetworks.tile;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class UltimateSimTile extends ExpansionTile {
    private int version;
    private boolean success;
    public UltimateSimTile() { this(0); }
    public UltimateSimTile(int version) { super(10, 2, ExpansionConfig.capacity[version]); this.version = version; }
    public int version() { return version; }
    public int multiplier() { return ExpansionConfig.MULTIPLIER[version]; }
    @Override public int duration() { return ExpansionConfig.simDuration[version]; }
    @Override public int cost() { return PerfectModel.cost(inventory.getStackInSlot(0)); }
    @Override public boolean isModelSlot(int slot) { return slot == 0; }
    @Override public boolean canExtract(int slot) { return super.canExtract(slot) || slot == 0 && extractModel && progress == 0; }
    @Override public boolean accepts(int slot, ItemStack stack) {
        if (slot == 0) return !PerfectModel.models(stack).isEmpty();
        List<DataModel> models = PerfectModel.models(inventory.getStackInSlot(0));
        return slot == 1 && !models.isEmpty() && models.stream().allMatch(m -> m.accepts(stack));
    }
    private List<ItemStack> outputs(ItemStack source, boolean prediction) {
        List<ItemStack> result = new ArrayList<>();
        for (DataModel model : PerfectModel.models(source)) {
            if (!model.baseDrop.isEmpty()) { ItemStack base = model.baseDrop.copy(); base.setCount(base.getCount() * multiplier()); result.add(base); }
            if (prediction) { ItemStack drop = ModelStack.create(model, true); drop.setCount(multiplier()); result.add(drop); }
        }
        return result;
    }
    @Override protected void tickServer() {
        ItemStack source = inventory.getStackInSlot(0);
        changedInputs(source.writeToNBT(new NBTTagCompound()).toString());
        if (!ready()) return;
        if (PerfectModel.models(source).isEmpty()) { progress = 0; failure = "model"; return; }
        if (!accepts(1, inventory.getStackInSlot(1)) || inventory.getStackInSlot(1).getCount() < 4 * multiplier()) { failure = "input"; return; }
        // Reserve enough output room for either roll before consuming power or materials.
        if (!canFit(outputs(source, true))) { failure = "output"; return; }
        if (energy.getEnergyStored() < cost()) { failure = "energy"; return; }
        if (progress == 0) success = world.rand.nextFloat() < PerfectModel.accuracy(source);
        if (payTick() && progress >= duration()) {
            inventory.extractItem(1, 4 * multiplier(), false); emit(outputs(source, success));
            PerfectModel.gain(source); ModelStack.iteration(source); progress = 0; running = ""; settingsChanged();
        }
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound n) { super.writeToNBT(n); n.setInteger("version", version); n.setBoolean("success", success); return n; }
    @Override public void readFromNBT(NBTTagCompound n) { version = Math.clamp(n.getInteger("version"), 0, 3); energy.resize(ExpansionConfig.capacity[version]); super.readFromNBT(n); success = n.getBoolean("success"); }
}
