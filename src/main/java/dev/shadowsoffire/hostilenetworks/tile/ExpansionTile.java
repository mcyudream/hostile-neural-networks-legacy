package dev.shadowsoffire.hostilenetworks.tile;

import dev.shadowsoffire.hostilenetworks.block.MachineBlock;
import net.minecraft.nbt.NBTTagCompound;

/** Shared server state for the integrated expansion machines. */
public abstract class ExpansionTile extends ProductionTile {
    public int progress;
    protected String running = "";
    public boolean extractModel;
    protected ExpansionTile(int slots, int output, int capacity) { super(slots, output, capacity); }
    public abstract int duration();
    public abstract int cost();
    public String machineName() { return world != null && world.getBlockState(pos).getBlock() instanceof MachineBlock block ? block.machine : ""; }
    public String inputSignature() {
        NBTTagCompound tag = new NBTTagCompound();
        for (int i = 0; i < outputStart; i++) tag.setTag("s" + i, inventory.getStackInSlot(i).writeToNBT(new NBTTagCompound()));
        return tag.toString();
    }
    protected void changedInputs(String signature) { if (!running.equals(signature)) { progress = 0; running = signature; markDirty(); } }
    protected boolean ready() {
        flushPending();
        if (!enabled()) { failure = "redstone"; return false; }
        if (!pending.isEmpty()) { failure = "buffering"; return false; }
        return true;
    }
    protected boolean payTick() {
        if (!energy.spend(cost())) { failure = "energy"; return false; }
        failure = "none"; progress++; markDirty(); return true;
    }
    public boolean modelComplete() { return false; }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound n) {
        super.writeToNBT(n); n.setInteger("progress", progress); n.setString("running", running); n.setBoolean("extractModel", extractModel); return n;
    }
    @Override public void readFromNBT(NBTTagCompound n) {
        super.readFromNBT(n); progress = Math.clamp(n.getInteger("progress"), 0, 1000000); running = n.getString("running"); extractModel = n.getBoolean("extractModel");
    }
}
