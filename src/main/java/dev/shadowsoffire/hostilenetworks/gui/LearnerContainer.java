package dev.shadowsoffire.hostilenetworks.gui;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.items.*;

public final class LearnerContainer extends Container {
    public final ItemStack learner;
    public final EnumHand hand;
    public final ItemStackHandler models;
    private final int selectedSlot;
    private final boolean serverSide;
    public LearnerContainer(EntityPlayer player, EnumHand hand) {
        this.hand = hand; learner = player.getHeldItem(hand); selectedSlot = player.inventory.currentItem;
        serverSide = !player.world.isRemote;
        models = DeepLearnerItem.inventory(learner);
        for (int i = 0; i < 4; i++) addSlotToContainer(new SlotItemHandler(models, i, 256 + i % 2 * 18, 99 + i / 2 * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player.inventory, col + row * 9 + 9, 89 + col * 18, 153 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player.inventory, col, 89 + col * 18, 211));
    }
    private boolean locked(int index) { return hand == EnumHand.MAIN_HAND && index == 31 + selectedSlot; }
    private void refreshModels() {
        // Kills may update the learner while its non-pausing GUI is open. Read the
        // current item before editing slots, rather than writing an older snapshot.
        if (serverSide && learner.hasTagCompound()) models.deserializeNBT(learner.getTagCompound().getCompoundTag("models"));
    }
    @Override public void detectAndSendChanges() { refreshModels(); super.detectAndSendChanges(); }
    @Override public boolean canInteractWith(EntityPlayer player) {
        return player.getHeldItem(hand) == learner && learner.getItem() == Hostile.item("deep_learner") && (hand != EnumHand.MAIN_HAND || player.inventory.currentItem == selectedSlot);
    }
    @Override public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        refreshModels();
        if (locked(slotId) || clickType == ClickType.SWAP && (dragType == selectedSlot && hand == EnumHand.MAIN_HAND || dragType == 40)) return ItemStack.EMPTY;
        return super.slotClick(slotId, dragType, clickType, player);
    }
    @Override public boolean canMergeSlot(ItemStack stack, Slot slot) { return !locked(slot.slotNumber) && super.canMergeSlot(stack, slot); }
    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        refreshModels();
        if (locked(index)) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index); if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack source = slot.getStack(), old = source.copy();
        if (index < 4) { if (!mergeItemStack(source, 4, inventorySlots.size(), true)) return ItemStack.EMPTY; }
        else if (!dev.shadowsoffire.hostilenetworks.data.LearningModel.valid(source) || !mergeItemStack(source, 0, 4, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged(); slot.onTake(player, source); return old;
    }
}
