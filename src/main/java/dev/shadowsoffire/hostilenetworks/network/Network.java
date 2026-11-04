package dev.shadowsoffire.hostilenetworks.network;

import com.google.gson.Gson;
import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.gui.MachineContainer;
import dev.shadowsoffire.hostilenetworks.tile.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.*;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;

public final class Network {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("hnn_cleanroom");
    public static void register() {
        CHANNEL.registerMessage(ActionHandler.class, Action.class, 0, Side.SERVER);
        CHANNEL.registerMessage(RegistryHandler.class, RegistrySync.class, 1, Side.CLIENT);
    }
    public static void sync(EntityPlayerMP player) { CHANNEL.sendTo(new RegistrySync(), player); }
    public static final class Action implements IMessage {
        public int window, action, modelSlot, value;
        public Action() {}
        public Action(int window, int action, int modelSlot, int value) { this.window = window; this.action = action; this.modelSlot = modelSlot; this.value = value; }
        @Override public void fromBytes(ByteBuf b) { window = b.readInt(); action = b.readInt(); modelSlot = b.readInt(); value = b.readInt(); }
        @Override public void toBytes(ByteBuf b) { b.writeInt(window); b.writeInt(action); b.writeInt(modelSlot); b.writeInt(value); }
    }
    public static final class ActionHandler implements IMessageHandler<Action, IMessage> {
        @Override public IMessage onMessage(Action msg, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> handle(msg, player)); return null;
        }
    }
    public static void handle(Action msg, EntityPlayerMP player) {
        if (!(player.openContainer instanceof MachineContainer container) || container.windowId != msg.window || !container.canInteractWith(player)) return;
        MachineTile tile = container.machine;
        if (msg.action == 0) tile.redstone = (tile.redstone + 1) % 3;
        else if (msg.action == 1 && tile instanceof SimChamberTile sim) sim.changeMode();
        else if (msg.action == 6 && tile instanceof UltimateSimTile sim) sim.extractModel = !sim.extractModel;
        else if (tile instanceof ProductionTile production) {
            int max = tile instanceof DataCenterTile ? 25 : 1;
            if (msg.modelSlot < 0 || msg.modelSlot >= max) return;
            DataModel model = ModelStack.model(tile.inventory.getStackInSlot(msg.modelSlot)); if (model == null) return;
            switch (msg.action) {
                case 2 -> production.selections.select(model, msg.value);
                case 3 -> production.selections.cycle(model);
                case 4 -> production.selections.clear(model);
                case 5 -> production.selections.remove(model, msg.value);
                default -> { return; }
            }
        }
        tile.settingsChanged(); container.detectAndSendChanges();
    }
    public static final class RegistrySync implements IMessage {
        String json;
        NBTTagCompound config;
        public RegistrySync() { json = new Gson().toJson(ModelRegistry.snapshot()); config = HostileConfig.syncTag(); }
        @Override public void fromBytes(ByteBuf b) {
            int length = b.readInt();
            if (length < 0 || length > 1048576 || length > b.readableBytes()) throw new IllegalArgumentException("Invalid model registry packet size");
            byte[] bytes = new byte[length]; b.readBytes(bytes); json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8); config = ByteBufUtils.readTag(b);
        }
        @Override public void toBytes(ByteBuf b) {
            byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (bytes.length > 1048576) throw new IllegalArgumentException("Model registry exceeds 1 MiB");
            b.writeInt(bytes.length); b.writeBytes(bytes); ByteBufUtils.writeTag(b, config);
        }
    }
    public static final class RegistryHandler implements IMessageHandler<RegistrySync, IMessage> {
        @Override public IMessage onMessage(RegistrySync message, MessageContext context) { HostileNetworks.proxy.receiveRegistry(message.json, message.config); return null; }
    }
}
