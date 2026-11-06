package dev.shadowsoffire.hostilenetworks.tile;

import net.minecraft.nbt.NBTTagCompound;
import dev.shadowsoffire.hostilenetworks.data.DataModel;

public abstract class ProductionTile extends MachineTile {
    public final Selections selections = new Selections();
    protected int selectedDrop(DataModel model) { var c = controller(); return c == null ? selections.current(model) : c.selectedDrop(this, model); }
    protected void advanceSelection(DataModel model) { if (!isExternallyControlled()) selections.get(model).advance(); }
    protected ProductionTile(int slots, int output, int capacity) { super(slots, output, capacity); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); tag.setTag("selections", selections.write()); return tag; }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); selections.read(tag.getCompoundTag("selections")); }
}
