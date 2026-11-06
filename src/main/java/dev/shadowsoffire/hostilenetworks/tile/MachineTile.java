package dev.shadowsoffire.hostilenetworks.tile;

import java.util.*;
import dev.shadowsoffire.hostilenetworks.api.MachineController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.*;
import net.minecraftforge.items.*;

public abstract class MachineTile extends TileEntity implements ITickable {
    public final ItemStackHandler inventory;
    public final EnergyStore energy;
    public final List<ItemStack> pending = new ArrayList<>();
    public int redstone;
    public String failure = "none";
    protected final int outputStart;
    protected int age;
    private final IItemHandler external;
    private BlockPos controller;

    public boolean isExternallyControlled() { return controller != null; }
    public MachineController controller() {
        if (controller == null || world == null || !world.isBlockLoaded(controller)) return null;
        return world.getTileEntity(controller) instanceof MachineController c && c.controls(this) ? c : null;
    }
    public boolean claimController(BlockPos owner) {
        if (world == null || world.isRemote || controller != null && !controller.equals(owner)
            || !world.isBlockLoaded(owner) || !(world.getTileEntity(owner) instanceof MachineController c) || !c.controls(this)) return false;
        controller = owner.toImmutable(); markDirty(); return true;
    }
    public void releaseController(BlockPos owner) {
        if (owner.equals(controller)) { controller = null; settingsChanged(); }
    }

    protected MachineTile(int slots, int outputStart, int capacity) {
        this.outputStart = outputStart;
        inventory = new ItemStackHandler(slots) {
            @Override public int getSlotLimit(int slot) { return MachineTile.this.slotLimit(slot); }
            @Override protected void onContentsChanged(int slot) { MachineTile.this.markDirty(); }
        };
        energy = new EnergyStore(capacity);
        external = new IItemHandler() {
            @Override public int getSlots() { return inventory.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
            @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot, stack); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return !isExternallyControlled() && accepts(slot, stack) ? inventory.insertItem(slot, stack, simulate) : stack; }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return !isExternallyControlled() && canExtract(slot) ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY; }
        };
    }
    public abstract boolean accepts(int slot, ItemStack stack);
    public boolean isModelSlot(int slot) { return false; }
    public int slotLimit(int slot) { return isModelSlot(slot) ? 1 : 64; }
    public boolean canExtract(int slot) { return slot >= outputStart; }
    public int getOutputStart() { return outputStart; }
    public boolean canUse(EntityPlayer player) { return !isExternallyControlled() && world != null && world.getTileEntity(pos) == this && player.getDistanceSq(pos) <= 64; }
    public boolean enabled() { return redstone == 0 || (world.isBlockPowered(pos) == (redstone == 2)); }
    public void settingsChanged() { markDirty(); if (world != null) world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3); }
    public boolean canFit(List<ItemStack> outputs) {
        ItemStackHandler trial = new ItemStackHandler(inventory.getSlots()); trial.deserializeNBT(inventory.serializeNBT());
        for (ItemStack original : outputs) {
            ItemStack remainder = original.copy();
            for (int i = outputStart; i < inventory.getSlots() && !remainder.isEmpty(); i++) remainder = trial.insertItem(i, remainder, false);
            if (!remainder.isEmpty()) return false;
        }
        return true;
    }
    public void flushPending() {
        boolean changed = false;
        for (int index = 0; index < pending.size();) {
            ItemStack stack = pending.get(index); int before = stack.getCount();
            for (int slot = outputStart; slot < inventory.getSlots() && !stack.isEmpty(); slot++) stack = inventory.insertItem(slot, stack, false);
            if (stack.getCount() != before) changed = true;
            if (stack.isEmpty()) pending.remove(index); else { pending.set(index, stack); index++; }
        }
        if (changed) markDirty();
    }
    public void emit(List<ItemStack> outputs) {
        // 1.12 ItemStack NBT stores Count as a signed byte. Split large custom drops
        // before buffering so a save while outputs are full cannot truncate them.
        for (ItemStack stack : outputs) if (!stack.isEmpty()) {
            int remaining = stack.getCount(), limit = Math.min(64, stack.getMaxStackSize());
            while (remaining > 0) {
                ItemStack part = stack.copy(); part.setCount(Math.min(remaining, limit));
                pending.add(part); remaining -= part.getCount();
            }
        }
        flushPending(); markDirty();
    }
    public List<ItemStack> dropsOnBreak() {
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) if (!inventory.getStackInSlot(i).isEmpty()) drops.add(inventory.getStackInSlot(i).copy());
        for (ItemStack stack : pending) drops.add(stack.copy());
        return drops;
    }
    @Override public void update() {
        if (world == null || world.isRemote) return;
        if (controller != null) {
            MachineController active = controller();
            if (active == null) {
                if (!world.isBlockLoaded(controller)) { failure = "controller_unloaded"; return; }
                controller = null; markDirty();
            } else if (!active.canRun(this)) { failure = "controller_paused"; return; }
        }
        age++; tickServer();
    }
    protected abstract void tickServer();
    @Override public boolean hasCapability(Capability<?> capability, EnumFacing side) {
        return capability == CapabilityEnergy.ENERGY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }
    @Override public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == CapabilityEnergy.ENERGY) return CapabilityEnergy.ENERGY.cast(energy);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(external);
        return super.getCapability(capability, side);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("inventory", inventory.serializeNBT()); tag.setInteger("energy", energy.getEnergyStored());
        tag.setInteger("redstone", redstone); tag.setString("failure", failure);
        if (controller != null) tag.setLong("externalController", controller.toLong());
        else tag.removeTag("externalController");
        NBTTagList buffer = new NBTTagList(); for (ItemStack stack : pending) buffer.appendTag(stack.writeToNBT(new NBTTagCompound()));
        tag.setTag("pending", buffer); return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag); inventory.deserializeNBT(tag.getCompoundTag("inventory")); energy.set(tag.getInteger("energy"));
        redstone = Math.clamp(tag.getInteger("redstone"), 0, 2); failure = tag.getString("failure");
        controller = tag.hasKey("externalController", 4) ? BlockPos.fromLong(tag.getLong("externalController")) : null;
        pending.clear(); NBTTagList buffer = tag.getTagList("pending", 10);
        for (int i = 0; i < buffer.tagCount(); i++) { ItemStack stack = new ItemStack(buffer.getCompoundTagAt(i)); if (!stack.isEmpty()) pending.add(stack); }
    }
    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) { readFromNBT(packet.getNbtCompound()); }

    public final class EnergyStore extends EnergyStorage {
        EnergyStore(int capacity) { super(capacity, capacity, 0); }
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(Math.max(0, amount), simulate);
            if (!simulate && received > 0) markDirty(); return received;
        }
        public boolean spend(int amount) {
            if (amount < 0 || energy < amount) return false;
            energy -= amount; markDirty(); return true;
        }
        public void set(int amount) { energy = Math.clamp(amount, 0, capacity); }
        public void resize(int amount) { capacity = Math.max(1, amount); maxReceive = capacity; energy = Math.min(energy, capacity); }
    }
}
