package dev.shadowsoffire.hostilenetworks.tile;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class LootFabTile extends ProductionTile {
    public int progress;
    private String runningModel = "";
    private int runningDrop = -1;
    public LootFabTile() { super(17, 1, HostileConfig.fabPowerCap); }
    @Override public boolean accepts(int slot, ItemStack stack) { return slot == 0 && stack.getItem() == Hostile.item("prediction") && ModelStack.model(stack) != null; }
    @Override protected void tickServer() {
        flushPending();
        if (!enabled()) { failure = "redstone"; return; }
        if (!pending.isEmpty()) { failure = "buffering"; return; }
        ItemStack input = inventory.getStackInSlot(0); DataModel model = ModelStack.model(input);
        if (model == null) { progress = 0; failure = "model"; return; }
        int selection = selections.current(model);
        if (!runningModel.equals(model.id.toString()) || runningDrop != selection) { progress = 0; runningModel = model.id.toString(); runningDrop = selection; }
        if (selection < 0) { failure = "no_selection"; return; }
        ItemStack output = model.drops.get(selection);
        if (!canFit(List.of(output))) { failure = "output"; return; }
        if (!energy.spend(HostileConfig.fabPowerCost)) { failure = "energy"; return; }
        failure = "none";
        if (++progress >= 60) {
            inventory.extractItem(0, 1, false); emit(List.of(output)); selections.get(model).advance(); progress = 0; settingsChanged();
        }
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag); tag.setInteger("progress", progress); tag.setString("runningModel", runningModel); tag.setInteger("runningDrop", runningDrop); return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag); progress = Math.clamp(tag.getInteger("progress"), 0, 60); runningModel = tag.getString("runningModel"); runningDrop = tag.getInteger("runningDrop");
    }
}
