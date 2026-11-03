package dev.shadowsoffire.hostilenetworks;

import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.network.Network;
import java.util.*;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public final class HostileCommand extends CommandBase {
    @Override public String getName() { return "hostilenetworks"; }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getUsage(ICommandSender sender) { return "/hostilenetworks reload | give_model <player> <model> [tier|data]"; }
    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1 && args[0].equals("reload")) {
            try { ModelRegistry.load(); } catch (RuntimeException error) { throw new CommandException("HNN reload failed: %s", error.getMessage()); }
            server.getPlayerList().getPlayers().forEach(Network::sync); sender.sendMessage(new TextComponentString("HNN: " + ModelRegistry.models().size() + " models loaded")); return;
        }
        if (args.length >= 3 && args[0].equals("give_model")) {
            EntityPlayerMP player = getPlayer(server, sender, args[1]); DataModel model = ModelRegistry.get(args[2]);
            if (model == null) throw new CommandException("Unknown model: %s", args[2]);
            ItemStack stack = ModelStack.create(model, false);
            if (args.length > 3) {
                ModelTier tier = ModelRegistry.tiers().stream().filter(t -> t.id().equals(args[3])).findFirst().orElse(null);
                ModelStack.setData(stack, tier == null ? parseInt(args[3], 0) : model.required(tier));
            }
            if (!player.inventory.addItemStackToInventory(stack)) player.dropItem(stack, false); return;
        }
        throw new WrongUsageException(getUsage(sender));
    }
    @Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos target) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "reload", "give_model");
        if (args.length == 2) return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        if (args.length == 3) return getListOfStringsMatchingLastWord(args, ModelRegistry.models().stream().map(m -> m.id.toString()).toList());
        if (args.length == 4) return getListOfStringsMatchingLastWord(args, ModelRegistry.tiers().stream().map(ModelTier::id).toList());
        return List.of();
    }
}
