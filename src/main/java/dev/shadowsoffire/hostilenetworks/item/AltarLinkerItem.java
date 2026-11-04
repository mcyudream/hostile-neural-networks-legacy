package dev.shadowsoffire.hostilenetworks.item;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.compat.bloodmagic.BloodAltarBridge;
import dev.shadowsoffire.hostilenetworks.data.ModelStack;
import dev.shadowsoffire.hostilenetworks.tile.DigitalAgonizerTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public final class AltarLinkerItem extends InfoItem {
    public AltarLinkerItem() { super("altar_linker"); setMaxStackSize(1); }
    @Override public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!player.canPlayerEdit(pos, side, player.getHeldItem(hand)) || !world.isBlockModifiable(player, pos)) return EnumActionResult.FAIL;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        var tag = ModelStack.tag(player.getHeldItem(hand));
        String message;
        if (BloodAltarBridge.isAltar(world.getTileEntity(pos))) {
            tag.setLong("altar", pos.toLong()); tag.setInteger("dimension", world.provider.getDimension()); message = "altar_selected";
        } else if (world.getTileEntity(pos) instanceof DigitalAgonizerTile tile && tag.hasKey("altar", 4) && tag.getInteger("dimension") == world.provider.getDimension()) {
            BlockPos target = BlockPos.fromLong(tag.getLong("altar"));
            message = world.isBlockModifiable(player, target) && tile.link(target) ? "altar_linked" : "altar_link_failed";
        } else message = "altar_link_failed";
        player.sendStatusMessage(new TextComponentTranslation("hnn.msg." + message), true); return EnumActionResult.SUCCESS;
    }
}
