package dev.shadowsoffire.hostilenetworks.tile;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class UltimateFabTile extends ExpansionTile {
    private int version;
    public UltimateFabTile() { this(0); }
    public UltimateFabTile(int version) { super(37, 1, ExpansionConfig.capacity[version]); this.version = version; }
    public int version() { return version; }
    public int batchLimit() { return 4 * ExpansionConfig.MULTIPLIER[version]; }
    @Override public int duration() { return ExpansionConfig.fabDuration[version]; }
    @Override public int cost() { return ExpansionConfig.fabCost[version]; }
    @Override public boolean accepts(int slot, ItemStack stack) { return slot == 0 && stack.getItem() == Hostile.item("prediction") && ModelStack.model(stack) != null; }
    @Override protected void tickServer() {
        if (!ready()) return;
        ItemStack input = inventory.getStackInSlot(0); DataModel model = ModelStack.model(input);
        if (model == null) { progress = 0; failure = "model"; return; }
        int drop = selections.current(model);
        changedInputs(model.id + ":" + drop);
        if (drop < 0) { failure = "no_selection"; return; }
        ItemStack output = model.drops.get(drop).copy();
        int count = Math.min(input.getCount(), batchLimit());
        for (; count > 0; count--) { output.setCount(model.drops.get(drop).getCount() * count); if (canFit(List.of(output))) break; }
        if (count == 0) { failure = "output"; return; }
        if (payTick() && progress >= duration()) {
            inventory.extractItem(0, count, false); emit(List.of(output)); selections.get(model).advance(); progress = 0; running = ""; settingsChanged();
        }
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound n) { super.writeToNBT(n); n.setInteger("version", version); return n; }
    @Override public void readFromNBT(NBTTagCompound n) { version = Math.clamp(n.getInteger("version"), 0, 3); energy.resize(ExpansionConfig.capacity[version]); super.readFromNBT(n); }
}
