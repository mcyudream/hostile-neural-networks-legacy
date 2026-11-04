package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;
import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.items.ItemStackHandler;

public final class DeepLearnerItem extends Item {
    public DeepLearnerItem() { setMaxStackSize(1); }
    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        open(player, hand); return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
    @Override public EnumActionResult onItemUse(EntityPlayer p, World w, BlockPos pos, EnumHand hand, EnumFacing facing, float x, float y, float z) {
        open(p, hand); return EnumActionResult.SUCCESS;
    }
    private void open(EntityPlayer p, EnumHand hand) {
        if (!p.world.isRemote) p.openGui(HostileNetworks.INSTANCE, hand == EnumHand.MAIN_HAND ? 1 : 2, p.world, 0, 0, 0);
    }
    public static ItemStackHandler inventory(ItemStack learner) {
        return new ItemStackHandler(4) {
            { if (learner.hasTagCompound() && learner.getTagCompound().hasKey("models", 10)) deserializeNBT(learner.getTagCompound().getCompoundTag("models")); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return LearningModel.valid(stack); }
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override protected void onContentsChanged(int slot) { ModelStack.tag(learner).setTag("models", serializeNBT()); }
        };
    }
    @Override public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(I18n.translateToLocal("hnn.info.learner"));
        ItemStackHandler inv = inventory(stack);
        for (int i = 0; i < 4; i++) {
            ItemStack s = inv.getStackInSlot(i); DataModel model = ModelStack.model(s);
            if (LearningModel.valid(s)) list.add((model == null ? I18n.translateToLocal("item.hostilenetworks.extra_data_model.name") : model.name()) + " — " + I18n.translateToLocal(LearningModel.tierKey(s)));
        }
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean changed) { return changed || oldStack.getItem() != newStack.getItem(); }
}
