package dev.shadowsoffire.hostilenetworks.client;

import com.google.gson.JsonParser;
import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.hostilenetworks.tile.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.*;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public final class ClientProxy extends CommonProxy {
    private final KeyBinding open = new KeyBinding("hnn.key.open", Keyboard.KEY_U, "itemGroup.hostilenetworks");
    @Override public void preInit() { super.preInit(); MinecraftForge.EVENT_BUS.register(this); }
    @Override public void init() {
        ClientRegistry.registerKeyBinding(open);
        Hostile.item("data_model").setTileEntityItemStackRenderer(new DataModelRenderer());
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, layer) -> {
            DataModel model = ModelStack.model(stack); return model == null ? 0xFFFFFF : model.color;
        }, Hostile.item("prediction"));
        ClientRegistry.bindTileEntitySpecialRenderer(DataCenterTile.class, new DataCenterRenderer());
        if (Loader.isModLoaded("ponder-legacy")) dev.shadowsoffire.hostilenetworks.compat.ponder.HNNPonder.register();
    }
    @SubscribeEvent public void models(ModelRegistryEvent event) {
        Hostile.ITEMS.forEach((id, item) -> ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory")));
    }
    @SubscribeEvent public void input(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!open.isPressed() || mc.player == null || mc.currentScreen != null) return;
        for (int i = 0; i < 9; i++) if (mc.player.inventory.getStackInSlot(i).getItem() == Hostile.item("deep_learner")) {
            mc.player.inventory.currentItem = i; mc.playerController.updateController(); mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND); return;
        }
        if (mc.player.getHeldItemOffhand().getItem() == Hostile.item("deep_learner")) mc.playerController.processRightClick(mc.player, mc.world, EnumHand.OFF_HAND);
    }
    @SubscribeEvent public void hud(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.player == null
            || (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.GuiChat)) || mc.gameSettings.showDebugInfo) return;
        var stack = mc.player.getHeldItemMainhand();
        if (stack.getItem() != Hostile.item("deep_learner")) stack = mc.player.getHeldItemOffhand();
        if (stack.getItem() != Hostile.item("deep_learner")) return;
        LearnerHud.render(stack);
    }
    @Override public void receiveRegistry(String json, NBTTagCompound config) {
        Minecraft.getMinecraft().addScheduledTask(() -> { ModelRegistry.receive(new JsonParser().parse(json).getAsJsonObject()); HostileConfig.readSync(config); });
    }
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == 1 || id == 2) return new LearnerScreen(player, id == 1 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND);
        return world.getTileEntity(new BlockPos(x, y, z)) instanceof MachineTile machine ? new MachineScreen(player, machine) : null;
    }
}
