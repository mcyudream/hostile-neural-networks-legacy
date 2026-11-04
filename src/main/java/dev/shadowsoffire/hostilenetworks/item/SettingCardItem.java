package dev.shadowsoffire.hostilenetworks.item;

import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.tile.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/** Settings are copied separately from inventories, energy and in-flight operations. */
public final class SettingCardItem extends InfoItem {
    public SettingCardItem() { super("setting_card"); setMaxStackSize(1); }
    private static String family(MachineTile tile) {
        if (tile instanceof LootFabTile || tile instanceof UltimateFabTile) return "fabricator";
        if (tile instanceof UltimateSimTile) return "ultimate_simulator";
        return tile.getClass().getName();
    }
    public static NBTTagCompound copy(MachineTile tile) {
        NBTTagCompound n = new NBTTagCompound(); n.setString("family", family(tile)); n.setInteger("redstone", tile.redstone);
        if (tile instanceof ProductionTile p) n.setTag("selections", p.selections.write());
        if (tile instanceof SimChamberTile sim) n.setInteger("mode", sim.mode);
        if (tile instanceof UltimateSimTile sim) n.setBoolean("extractModel", sim.extractModel);
        if (tile instanceof SimChamberTile || tile instanceof UltimateSimTile) {
            ItemStack model = tile.inventory.getStackInSlot(0);
            if (!model.isEmpty()) n.setTag("model", model.writeToNBT(new NBTTagCompound()));
        }
        return n;
    }
    public static boolean paste(MachineTile tile, NBTTagCompound n, EntityPlayer player) {
        if (!family(tile).equals(n.getString("family"))) return false;
        if (tile instanceof SimChamberTile sim && sim.progress > 0 || tile instanceof ExpansionTile extra && extra.progress > 0) return false;
        ItemStack wanted = new ItemStack(n.getCompoundTag("model"));
        if (!wanted.isEmpty()) {
            ItemStack current = tile.inventory.getStackInSlot(0);
            if (!sameModel(wanted, current)) {
                int found = -1;
                for (int i = 0; i < player.inventory.mainInventory.size(); i++)
                    if (sameModel(wanted, player.inventory.getStackInSlot(i)) && tile.accepts(0, player.inventory.getStackInSlot(i))) { found = i; break; }
                if (found < 0) return false;
                // Transfer the real stack, preserving its data and all other NBT.
                ItemStack replacement = player.inventory.decrStackSize(found, 1);
                tile.inventory.setStackInSlot(0, replacement);
                if (!current.isEmpty() && !player.inventory.addItemStackToInventory(current)) player.dropItem(current, false);
                player.inventory.markDirty();
            }
        }
        tile.redstone = Math.clamp(n.getInteger("redstone"), 0, 2);
        if (tile instanceof ProductionTile p) p.selections.read(n.getCompoundTag("selections"));
        if (tile instanceof SimChamberTile sim) sim.mode = Math.clamp(n.getInteger("mode"), 0, 1);
        if (tile instanceof UltimateSimTile sim) sim.extractModel = n.getBoolean("extractModel");
        tile.settingsChanged(); return true;
    }
    private static boolean sameModel(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) return false;
        DataModel normal = ModelStack.model(a);
        return normal != null ? normal == ModelStack.model(b) : !PerfectModel.models(a).isEmpty() && PerfectModel.signature(a).equals(PerfectModel.signature(b));
    }
    @Override public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!(world.getTileEntity(pos) instanceof MachineTile tile)) return EnumActionResult.PASS;
        if (!world.isBlockModifiable(player, pos) || !player.canPlayerEdit(pos, side, player.getHeldItem(hand))) return EnumActionResult.FAIL;
        if (!world.isRemote) {
            var tag = ModelStack.tag(player.getHeldItem(hand)); String key;
            if (player.isSneaking()) { tag.setTag("settings", copy(tile)); key = "copied"; }
            else key = paste(tile, tag.getCompoundTag("settings"), player) ? "pasted" : "failed";
            player.sendStatusMessage(new TextComponentTranslation("hnn.settings." + key), true);
        }
        return EnumActionResult.SUCCESS;
    }
    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        var hit = rayTrace(world, player, false);
        if (player.isSneaking() && !world.isRemote && (hit == null || hit.typeOfHit == net.minecraft.util.math.RayTraceResult.Type.MISS)) {
            ModelStack.tag(player.getHeldItem(hand)).removeTag("settings");
            player.sendStatusMessage(new TextComponentTranslation("hnn.settings.cleared"), true);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
}
