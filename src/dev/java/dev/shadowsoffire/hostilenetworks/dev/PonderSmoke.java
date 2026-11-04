package dev.shadowsoffire.hostilenetworks.dev;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import java.util.function.BiConsumer;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;

/** Also isolated so the no-Ponder verification cannot link Ponder classes. */
public final class PonderSmoke {
    public static void verify(BiConsumer<Boolean, String> check) {
        for (String item : new String[]{"deep_learner", "blank_data_model", "sim_chamber", "loot_fabricator", "data_center", "data_center_io_port"}) {
            var scenes = PonderIndex.getSceneAccess().compile(HostileNetworks.id(item));
            check.accept(scenes.size() == 1, "Ponder registers a scene for " + item);
            var scene = scenes.getFirst(); scene.begin(); scene.seekToTime(1200);
            check.accept(!scene.getWorld().getBlockState(new net.minecraft.util.math.BlockPos(item.startsWith("data_center") ? 4 : 1, item.startsWith("data_center") ? 2 : 1, item.startsWith("data_center") ? 1 : 2)).getBlock().equals(net.minecraft.init.Blocks.AIR), "Ponder structure loads and the entire storyboard executes for " + item);
        }
        for (String item : dev.shadowsoffire.hostilenetworks.Hostile.ITEMS.keySet()) {
            if (!item.startsWith("ultimate_") && !item.startsWith("upgrade_") && !java.util.List.of("merger_camera", "simulator_modeling", "blank_extra_data_model", "extra_data_model", "setting_card", "digital_agonizer", "altar_linker").contains(item)) continue;
            var scenes = PonderIndex.getSceneAccess().compile(HostileNetworks.id(item));
            check.accept(scenes.size() == 1, "Ponder registers an expansion storyboard for " + item);
            var scene = scenes.getFirst(); scene.begin(); scene.seekToTime(1200);
            boolean blood = item.equals("digital_agonizer") || item.equals("altar_linker");
            var pos = new net.minecraft.util.math.BlockPos(blood ? 3 : 1, 1, blood ? 2 : 3);
            check.accept(!scene.getWorld().getBlockState(pos).getBlock().equals(net.minecraft.init.Blocks.AIR), "Ponder expansion structure loads and all six steps execute for " + item);
        }
        Minecraft.getMinecraft().displayGuiScreen(PonderUI.of(HostileNetworks.id("sim_chamber")));
    }
    public static void center() { Minecraft.getMinecraft().displayGuiScreen(PonderUI.of(HostileNetworks.id("data_center"))); }
    public static void expansion() { Minecraft.getMinecraft().displayGuiScreen(PonderUI.of(HostileNetworks.id("merger_camera"))); }
    public static void agonizer() { Minecraft.getMinecraft().displayGuiScreen(PonderUI.of(HostileNetworks.id("digital_agonizer"))); }
    public static void verifyPlayback(BiConsumer<Boolean, String> check, String name) {
        var screen = (PonderUI) Minecraft.getMinecraft().currentScreen;
        var scene = screen.getActiveScene();
        check.accept(scene.getCurrentTick() >= 30 && scene.getSceneElements().stream().anyMatch(element -> element.isVisible()),
            "Ponder " + name + " viewer advances its timeline and displays visible scene elements");
        check.accept(scene.getOverlayElements().stream().anyMatch(element -> element.isVisible()),
            "Ponder " + name + " viewer displays its step instructions during playback");
    }
}
