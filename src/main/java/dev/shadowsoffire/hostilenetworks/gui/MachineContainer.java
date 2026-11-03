package dev.shadowsoffire.hostilenetworks.gui;

import dev.shadowsoffire.hostilenetworks.tile.*;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class MachineContainer extends Container {
    public final MachineTile machine;
    public boolean configurationView;
    public static int width(MachineTile tile) { return tile instanceof SimChamberTile ? 232 : tile instanceof DataCenterTile ? 230 : 176; }
    public static int height(MachineTile tile) { return tile instanceof SimChamberTile ? 235 : tile instanceof DataCenterTile ? 223 : 178; }
    private int ticks;
    public MachineContainer(EntityPlayer player, MachineTile tile) {
        machine = tile;
        for (int i = 0; i < tile.inventory.getSlots(); i++) {
            int x, y;
            if (tile instanceof DataCenterTile) {
                if (i < 25) { x = 9 + i % 5 * 18; y = 21 + i / 5 * 20; }
                else if (i < 29) { x = 132 + (i - 25) * 18; y = 20; }
                else { x = 132 + (i - 29) % 4 * 18; y = 48 + (i - 29) / 4 * 18; }
            } else if (tile instanceof SimChamberTile) {
                x = new int[]{-13, 176, 196, 186}[i]; y = new int[]{1, 7, 7, 27}[i];
            } else { x = i == 0 ? 79 : 100 + (i - 1) % 4 * 18; y = i == 0 ? 62 : 7 + (i - 1) / 4 * 18; }
            final int slot = i;
            addSlotToContainer(new SlotItemHandler(tile.inventory, i, x, y) {
                @Override public boolean isItemValid(ItemStack stack) { return tile.accepts(slot, stack); }
                @Override public boolean isEnabled() { return !configurationView; }
            });
        }
        playerSlots(player, tile instanceof SimChamberTile ? 36 : tile instanceof DataCenterTile ? 35 : 8,
            tile instanceof SimChamberTile ? 153 : tile instanceof DataCenterTile ? 140 : 96);
    }
    private void playerSlots(EntityPlayer player, int x, int y) {
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player.inventory, col + row * 9 + 9, x + col * 18, y + row * 18) {
            @Override public boolean isEnabled() { return !configurationView; }
        });
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player.inventory, col, x + col * 18, y + 58) {
            @Override public boolean isEnabled() { return !configurationView; }
        });
    }
    @Override public boolean canInteractWith(EntityPlayer player) { return machine.canUse(player); }
    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index); if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack source = slot.getStack(), old = source.copy(); int slots = machine.inventory.getSlots();
        if (index < slots) { if (!mergeItemStack(source, slots, inventorySlots.size(), true)) return ItemStack.EMPTY; }
        else {
            boolean moved = false;
            for (int i = 0; i < machine.getOutputStart() && !source.isEmpty(); i++) if (machine.accepts(i, source)) moved |= mergeItemStack(source, i, i + 1, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        slot.onTake(player, source); return old;
    }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (++ticks % 5 == 0) for (IContainerListener listener : listeners) if (listener instanceof EntityPlayerMP player) player.connection.sendPacket(machine.getUpdatePacket());
    }
}
