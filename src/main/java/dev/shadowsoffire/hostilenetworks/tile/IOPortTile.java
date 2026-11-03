package dev.shadowsoffire.hostilenetworks.tile;

import dev.shadowsoffire.hostilenetworks.block.IOPortBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.*;
import net.minecraftforge.items.*;

/** Cached pipe references resolve the owner on every operation, so reformed shells cannot keep a stale inventory alive. */
public final class IOPortTile extends TileEntity {
    private BlockPos owner;
    public void claim(BlockPos controller) { if (!controller.equals(owner)) { owner = controller.toImmutable(); markDirty(); } }
    public void release(BlockPos controller) { if (controller.equals(owner)) { owner = null; markDirty(); } }
    public DataCenterTile controller() {
        if (owner == null || world == null || !world.isBlockLoaded(owner) || !(world.getTileEntity(owner) instanceof DataCenterTile dc)) return null;
        return dc.shellValid && dc.layout != null && dc.layout.portPosition(pos) ? dc : null;
    }
    private IOPortBlock.Mode mode() { return world != null && world.getBlockState(pos).getBlock() instanceof IOPortBlock ? world.getBlockState(pos).getValue(IOPortBlock.MODE) : IOPortBlock.Mode.ENERGY; }
    private final IEnergyStorage energy = new IEnergyStorage() {
        private IEnergyStorage delegate() { DataCenterTile dc = controller(); return dc != null && mode() == IOPortBlock.Mode.ENERGY ? dc.energy : null; }
        @Override public int receiveEnergy(int amount, boolean simulate) { var d = delegate(); return d == null ? 0 : d.receiveEnergy(amount, simulate); }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { var d = delegate(); return d == null ? 0 : d.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { var d = delegate(); return d == null ? 0 : d.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return delegate() != null; }
    };
    private final IItemHandler items = new IItemHandler() {
        private int start() { return switch (mode()) { case MODELS -> 0; case INPUTS -> 25; default -> 29; }; }
        @Override public int getSlots() { return switch (mode()) { case MODELS -> 25; case INPUTS -> 4; case OUTPUTS -> 16; default -> 0; }; }
        private DataCenterTile valid(int slot) { return slot >= 0 && slot < getSlots() ? controller() : null; }
        @Override public ItemStack getStackInSlot(int slot) { var dc = valid(slot); return dc == null ? ItemStack.EMPTY : dc.inventory.getStackInSlot(start() + slot); }
        @Override public int getSlotLimit(int slot) { return mode() == IOPortBlock.Mode.MODELS ? 1 : 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { var dc = valid(slot); return dc != null && mode() != IOPortBlock.Mode.OUTPUTS && dc.accepts(start() + slot, stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { var dc = valid(slot); return dc != null && isItemValid(slot, stack) ? dc.inventory.insertItem(start() + slot, stack, simulate) : stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { var dc = valid(slot); return dc != null && mode() != IOPortBlock.Mode.INPUTS ? dc.inventory.extractItem(start() + slot, amount, simulate) : ItemStack.EMPTY; }
    };
    @Override public boolean hasCapability(Capability<?> capability, EnumFacing side) {
        return capability == CapabilityEnergy.ENERGY && mode() == IOPortBlock.Mode.ENERGY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && mode() != IOPortBlock.Mode.ENERGY || super.hasCapability(capability, side);
    }
    @Override public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == CapabilityEnergy.ENERGY && mode() == IOPortBlock.Mode.ENERGY) return CapabilityEnergy.ENERGY.cast(energy);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && mode() != IOPortBlock.Mode.ENERGY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(items);
        return super.getCapability(capability, side);
    }
    @Override public boolean shouldRefresh(net.minecraft.world.World w, BlockPos p, net.minecraft.block.state.IBlockState old, net.minecraft.block.state.IBlockState next) { return old.getBlock() != next.getBlock(); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); if (owner != null) tag.setLong("owner", owner.toLong()); return tag; }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); owner = tag.hasKey("owner") ? BlockPos.fromLong(tag.getLong("owner")) : null; }
}
