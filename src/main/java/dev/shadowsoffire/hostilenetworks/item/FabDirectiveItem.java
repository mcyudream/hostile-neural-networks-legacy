package dev.shadowsoffire.hostilenetworks.item;

import dev.shadowsoffire.hostilenetworks.data.ModelStack;
import dev.shadowsoffire.hostilenetworks.tile.ProductionTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public final class FabDirectiveItem extends InfoItem {
    public FabDirectiveItem() { super("directive"); setMaxStackSize(1); }
    @Override public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float x, float y, float z) {
        if (!(world.getTileEntity(pos) instanceof ProductionTile machine)) return EnumActionResult.PASS;
        if (!world.isRemote) {
            ItemStack stack = player.getHeldItem(hand);
            if (player.isSneaking()) {
                ModelStack.tag(stack).setTag("selections", machine.selections.write());
                player.sendStatusMessage(new TextComponentTranslation("hnn.directive.copied"), true);
            } else if (stack.hasTagCompound() && stack.getTagCompound().hasKey("selections", 10)) {
                machine.selections.read(stack.getTagCompound().getCompoundTag("selections")); machine.settingsChanged();
                player.sendStatusMessage(new TextComponentTranslation("hnn.directive.pasted"), true);
            }
        }
        return EnumActionResult.SUCCESS;
    }
}
