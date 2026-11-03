package dev.shadowsoffire.hostilenetworks;

import dev.shadowsoffire.hostilenetworks.gui.*;
import dev.shadowsoffire.hostilenetworks.tile.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class CommonProxy implements IGuiHandler {
    public void preInit() {
        ForgeChunkManager.setForcedChunkLoadingCallback(HostileNetworks.INSTANCE, (tickets, world) -> {
            // Shell tickets are ephemeral. Rebuild them only when a naturally loaded controller validates its shell.
            for (ForgeChunkManager.Ticket ticket : tickets) ForgeChunkManager.releaseTicket(ticket);
        });
    }
    public void init() {}
    public void receiveRegistry(String json, net.minecraft.nbt.NBTTagCompound config) {}
    @Override public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == 1 || id == 2) {
            EnumHand hand = id == 1 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
            return player.getHeldItem(hand).getItem() == Hostile.item("deep_learner") ? new LearnerContainer(player, hand) : null;
        }
        return world.getTileEntity(new BlockPos(x, y, z)) instanceof MachineTile machine ? new MachineContainer(player, machine) : null;
    }
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) { return null; }
}
