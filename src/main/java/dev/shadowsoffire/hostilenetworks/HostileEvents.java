package dev.shadowsoffire.hostilenetworks;

import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.hostilenetworks.network.Network;
import java.util.*;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public final class HostileEvents {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.player instanceof EntityPlayerMP p) Network.sync(p);
    }
    @SubscribeEvent public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!HostileConfig.rightClickToAttune || event.getItemStack().getItem() != Hostile.item("blank_data_model")) return;
        if (Arrays.asList(ExpansionConfig.attuneBlacklist).contains(String.valueOf(EntityList.getKey(event.getTarget())))) return;
        if (!event.getWorld().isRemote) attune(event.getEntityPlayer(), event.getHand(), ModelRegistry.models().stream().filter(m -> m.attunes(event.getTarget())).toList());
        event.setCanceled(true); event.setCancellationResult(EnumActionResult.SUCCESS);
    }
    @SubscribeEvent public static void blockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!HostileConfig.rightClickToAttune || event.getItemStack().getItem() != Hostile.item("blank_data_model")) return;
        if (!event.getWorld().isRemote) attune(event.getEntityPlayer(), event.getHand(), ModelRegistry.models().stream().filter(m -> m.attunes(event.getWorld(), event.getPos())).toList());
        event.setCanceled(true); event.setCancellationResult(EnumActionResult.SUCCESS);
    }
    private static void attune(EntityPlayer player, EnumHand hand, List<DataModel> matches) {
        if (matches.size() != 1) {
            player.sendStatusMessage(new TextComponentTranslation(matches.isEmpty() ? "hostilenetworks.msg.no_model" : "hnn.multiple_models"), true); return;
        }
        ItemStack held = player.getHeldItem(hand), result = ModelStack.create(matches.getFirst(), false);
        if (held.getCount() == 1) player.setHeldItem(hand, result);
        else { held.shrink(1); if (!player.inventory.addItemStackToInventory(result)) player.dropItem(result, false); }
        player.inventoryContainer.detectAndSendChanges();
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (!HostileConfig.actionUpgradesModel || event.getEntityLiving().world.isRemote) return;
        EntityPlayer player = event.getSource().getTrueSource() instanceof EntityPlayer p ? p : event.getEntityLiving().getAttackingEntity() instanceof EntityPlayer p ? p : null;
        if (player != null) update(player, m -> !m.blockModel && m.matches(EntityList.getKey(event.getEntityLiving())));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void mine(BlockEvent.BreakEvent event) {
        if (!HostileConfig.actionUpgradesModel || event.getWorld().isRemote || event.getPlayer().isCreative()) return;
        boolean silk = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.SILK_TOUCH, event.getPlayer().getHeldItemMainhand()) > 0;
        update(event.getPlayer(), m -> m.blockModel && m.matches(event.getState().getBlock().getRegistryName()) && (!m.noSilkTouch || !silk));
    }
    private static void update(EntityPlayer player, java.util.function.Predicate<DataModel> match) {
        for (ItemStack learner : player.inventory.mainInventory) updateLearner(learner, match);
        for (ItemStack learner : player.inventory.offHandInventory) updateLearner(learner, match);
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }
    public static void updateLearner(ItemStack learner, java.util.function.Predicate<DataModel> match) {
        if (learner.getItem() != Hostile.item("deep_learner")) return;
        var inventory = DeepLearnerItem.inventory(learner);
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot); DataModel model = ModelStack.model(stack);
            if (LearningModel.perfect(stack)) {
                if (PerfectModel.models(stack).stream().noneMatch(match)) continue;
                PerfectModel.gain(stack);
            } else {
                if (model == null || !match.test(model)) continue;
                ModelStack.gain(stack, model.gained(model.tier(ModelStack.data(stack))), true);
            }
            inventory.setStackInSlot(slot, stack);
        }
    }
}
