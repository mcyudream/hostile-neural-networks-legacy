package dev.shadowsoffire.hostilenetworks.tile;

import net.minecraft.nbt.NBTTagCompound;

public abstract class ProductionTile extends MachineTile {
    public final Selections selections = new Selections();
    protected ProductionTile(int slots, int output, int capacity) { super(slots, output, capacity); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); tag.setTag("selections", selections.write()); return tag; }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); selections.read(tag.getCompoundTag("selections")); }
}
