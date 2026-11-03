package dev.shadowsoffire.hostilenetworks.tile;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.*;
import net.minecraftforge.common.ForgeChunkManager;

public final class DataCenterTile extends ProductionTile {
    public final int[] progress = new int[25], successes = new int[25];
    public final String[] failures = new String[25], runningModels = new String[25];
    public boolean shellValid;
    public DataCenterShell.Layout layout;
    private final Set<BlockPos> ports = new HashSet<>();
    private ForgeChunkManager.Ticket ticket;
    private final Set<ChunkPos> forced = new HashSet<>();
    public DataCenterTile() {
        super(45, 29, HostileConfig.dataCenterPowerCap);
        Arrays.fill(failures, "model"); Arrays.fill(runningModels, "");
    }
    @Override public boolean isModelSlot(int slot) { return slot < 25; }
    @Override public boolean accepts(int slot, ItemStack stack) {
        if (slot < 25) return stack.getItem() == Hostile.item("data_model");
        if (slot >= 29) return false;
        for (int i = 0; i < 25; i++) { DataModel model = ModelStack.model(inventory.getStackInSlot(i)); if (model != null && model.accepts(stack)) return true; }
        return stack.getItem() == Hostile.item("prediction_matrix");
    }
    public void recheck() {
        DataCenterShell.Layout found = DataCenterShell.find(world, pos); boolean old = shellValid;
        shellValid = found != null; layout = found;
        Set<BlockPos> current = new HashSet<>();
        if (found != null) for (BlockPos p : BlockPos.getAllInBox(found.min(), found.max())) {
            if (found.portPosition(p) && world.getTileEntity(p) instanceof IOPortTile port) { port.claim(pos); current.add(p.toImmutable()); }
        }
        for (BlockPos oldPort : ports) if (!current.contains(oldPort) && world.isBlockLoaded(oldPort) && world.getTileEntity(oldPort) instanceof IOPortTile port) port.release(pos);
        ports.clear(); ports.addAll(current); updateTickets();
        if (old != shellValid) settingsChanged();
    }
    private void updateTickets() {
        Set<ChunkPos> needed = new HashSet<>();
        if (layout != null) for (int x = layout.min().getX() >> 4; x <= layout.max().getX() >> 4; x++)
            for (int z = layout.min().getZ() >> 4; z <= layout.max().getZ() >> 4; z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                if (!chunk.equals(new ChunkPos(pos))) needed.add(chunk);
            }
        if (needed.isEmpty()) { releaseTickets(); return; }
        if (ticket == null) ticket = ForgeChunkManager.requestTicket(HostileNetworks.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
        if (ticket == null) return;
        for (ChunkPos chunk : new HashSet<>(forced)) if (!needed.contains(chunk)) { ForgeChunkManager.unforceChunk(ticket, chunk); forced.remove(chunk); }
        for (ChunkPos chunk : needed) if (forced.add(chunk)) ForgeChunkManager.forceChunk(ticket, chunk);
    }
    private void releaseTickets() { if (ticket != null) { ForgeChunkManager.releaseTicket(ticket); ticket = null; } forced.clear(); }
    @Override public void invalidate() { releasePorts(); releaseTickets(); super.invalidate(); }
    @Override public void onChunkUnload() { releaseTickets(); super.onChunkUnload(); }
    public void releasePorts() {
        if (world != null && !world.isRemote) for (BlockPos p : ports) if (world.isBlockLoaded(p) && world.getTileEntity(p) instanceof IOPortTile port) port.release(pos);
        ports.clear();
    }
    @Override protected void tickServer() {
        if (age == 1 || age % 20 == 0) recheck();
        flushPending();
        if (!shellValid) { failure = "shell_broken"; Arrays.fill(failures, failure); return; }
        if (!enabled()) { failure = "redstone"; Arrays.fill(failures, failure); return; }
        if (!pending.isEmpty()) { failure = "buffering"; Arrays.fill(failures, failure); return; }
        failure = "none";
        for (int i = 0; i < 25; i++) tickModel(i);
        if (age % 20 == 0) settingsChanged();
    }
    private void tickModel(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot); DataModel model = ModelStack.model(stack);
        if (model == null) { progress[slot] = 0; failures[slot] = "model"; return; }
        if (!model.id.toString().equals(runningModels[slot])) progress[slot] = 0;
        ModelTier tier = model.tier(ModelStack.data(stack));
        if (!tier.canSim() || tier.accuracy() < 1) { progress[slot] = 0; failures[slot] = "not_self_aware"; return; }
        int cost = (int) Math.min(Integer.MAX_VALUE, Math.ceil(model.simCost * (double) HostileConfig.dataCenterCost));
        if (progress[slot] == 0) {
            if (selections.current(model) < 0) { failures[slot] = "no_selection"; return; }
            int inputSlot = -1;
            for (int i = 25; i < 29; i++) if (model.accepts(inventory.getStackInSlot(i))) { inputSlot = i; break; }
            if (inputSlot < 0) { failures[slot] = "input"; return; }
            if (energy.getEnergyStored() < cost) { failures[slot] = "energy"; return; }
            if (!canFit(List.of(model.baseDrop, model.drops.get(selections.current(model))))) { failures[slot] = "output"; return; }
            successes[slot] = model.roll(ModelStack.data(stack), world.rand); inventory.extractItem(inputSlot, 1, false);
            runningModels[slot] = model.id.toString(); progress[slot] = 300; failures[slot] = "none"; markDirty(); return;
        }
        // An edited/cleared selection pauses a paid cycle until a valid output is selected.
        if (progress[slot] == 1 && selections.current(model) < 0) { failures[slot] = "no_selection"; return; }
        if (!energy.spend(cost)) { failures[slot] = "energy_mid_cycle"; return; }
        failures[slot] = "none";
        if (--progress[slot] == 0) {
            List<ItemStack> outputs = new ArrayList<>(); outputs.add(model.baseDrop);
            for (int n = 0; n < successes[slot]; n++) {
                int selection = selections.current(model); if (selection < 0) break;
                outputs.add(model.drops.get(selection)); selections.get(model).advance();
            }
            emit(outputs); ModelStack.iteration(stack); settingsChanged();
        }
    }
    @Override public AxisAlignedBB getRenderBoundingBox() { return layout == null ? super.getRenderBoundingBox() : new AxisAlignedBB(layout.min(), layout.max().add(1, 1, 1)); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag); tag.setIntArray("progress", progress); tag.setIntArray("successes", successes); tag.setBoolean("shellValid", shellValid);
        NBTTagList states = new NBTTagList(), models = new NBTTagList();
        for (int i = 0; i < 25; i++) { states.appendTag(new NBTTagString(failures[i])); models.appendTag(new NBTTagString(runningModels[i])); }
        tag.setTag("failures", states); tag.setTag("runningModels", models);
        if (layout != null) tag.setLong("shellMin", layout.min().toLong()); return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag); int[] p = tag.getIntArray("progress"), s = tag.getIntArray("successes");
        NBTTagList f = tag.getTagList("failures", 8), m = tag.getTagList("runningModels", 8);
        for (int i = 0; i < 25; i++) {
            progress[i] = i < p.length ? Math.clamp(p[i], 0, 300) : 0; successes[i] = i < s.length ? Math.clamp(s[i], 0, 64) : 0;
            failures[i] = i < f.tagCount() ? f.getStringTagAt(i) : "model"; runningModels[i] = i < m.tagCount() ? m.getStringTagAt(i) : "";
        }
        shellValid = tag.getBoolean("shellValid"); layout = tag.hasKey("shellMin") ? new DataCenterShell.Layout(BlockPos.fromLong(tag.getLong("shellMin")), pos) : null;
    }
}
