package dev.shadowsoffire.hostilenetworks.compat.ponder;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.*;
import net.createmod.ponder.api.registration.*;
import net.createmod.ponder.api.scene.*;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.catnip.math.Pointing;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;

/** Loaded exclusively by the client proxy after Loader.isModLoaded("ponder-legacy"). */
public final class HNNPonder implements PonderPlugin {
    public static void register() { PonderIndex.addPlugin(new HNNPonder()); }
    @Override public String getModId() { return HostileNetworks.MODID; }
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (String item : new String[]{"deep_learner", "blank_data_model", "sim_chamber", "loot_fabricator"})
            helper.addStoryBoard(HostileNetworks.id(item), HostileNetworks.id("workflow"), (scene, util) -> workflow(scene, util, item + "_workflow")).identifiedBy(HostileNetworks.id(item + "_workflow"));
        for (String item : new String[]{"data_center", "data_center_io_port"})
            helper.addStoryBoard(HostileNetworks.id(item), HostileNetworks.id("data_center"), (scene, util) -> dataCenter(scene, util, item + "_structure")).identifiedBy(HostileNetworks.id(item + "_structure"));
    }
    private static void workflow(SceneBuilder scene, SceneBuildingUtil util, String id) {
        scene.title(id, I18n.format("hnn.ponder.workflow.title")); scene.configureBasePlate(0, 0, 5); scene.showBasePlate();
        scene.world().showSection(util.select().layersFrom(1), EnumFacing.DOWN); scene.idle(20);
        scene.overlay().showText(90).text(I18n.format("hnn.ponder.attune")).pointAt(util.vector().centerOf(1, 1, 2)).placeNearTarget();
        scene.overlay().showControls(util.vector().topOf(1, 1, 2), Pointing.DOWN, 80).rightClick().withItem(Hostile.stack("blank_data_model")); scene.idle(95);
        scene.overlay().showText(100).text(I18n.format("hnn.ponder.learner")).pointAt(util.vector().centerOf(1, 1, 2)).placeNearTarget();
        scene.overlay().showControls(util.vector().topOf(1, 1, 2), Pointing.DOWN, 90).withItem(Hostile.stack("deep_learner")); scene.idle(105);
        scene.overlay().showText(100).text(I18n.format("hnn.ponder.training")).pointAt(util.vector().centerOf(1, 1, 2)).placeNearTarget();
        scene.overlay().showControls(util.vector().topOf(1, 1, 2), Pointing.DOWN, 90).withItem(Hostile.stack("prediction_matrix")); scene.idle(105);
        scene.overlay().showText(100).text(I18n.format("hnn.ponder.inference")).pointAt(util.vector().centerOf(1, 1, 2)).placeNearTarget();
        DataModel zombie = ModelRegistry.get("hostilenetworks:zombie");
        if (zombie != null) scene.world().createItemEntity(util.vector().topOf(2, 1, 2), new Vec3d(.02, .05, 0), ModelStack.create(zombie, true));
        scene.idle(105);
        scene.overlay().showText(100).text(I18n.format("hnn.ponder.fabrication")).pointAt(util.vector().centerOf(3, 1, 2)).placeNearTarget(); scene.idle(105);
        scene.overlay().showText(90).text(I18n.format("hnn.ponder.automation")).pointAt(util.vector().centerOf(4, 1, 2)).placeNearTarget(); scene.idle(95); scene.markAsFinished();
    }
    private static void dataCenter(SceneBuilder scene, SceneBuildingUtil util, String id) {
        scene.title(id, I18n.format("hnn.ponder.data_center.title")); scene.configureBasePlate(0, 0, 9); scene.showBasePlate();
        scene.world().showSection(util.select().layer(1), EnumFacing.DOWN);
        scene.overlay().showText(90).text(I18n.format("hnn.ponder.floor")).pointAt(util.vector().centerOf(4, 1, 4)).placeNearTarget(); scene.idle(95);
        scene.world().showSection(util.select().fromTo(1, 2, 1, 7, 7, 7), EnumFacing.DOWN);
        scene.overlay().showText(110).text(I18n.format("hnn.ponder.shell")).pointAt(util.vector().centerOf(4, 5, 1)).placeNearTarget(); scene.idle(115);
        scene.overlay().showText(100).text(I18n.format("hnn.ponder.controller")).pointAt(util.vector().centerOf(4, 2, 1)).placeNearTarget(); scene.idle(105);
        scene.overlay().showText(100).text(I18n.format("hnn.ponder.parallel")).pointAt(util.vector().centerOf(4, 2, 1)).placeNearTarget(); scene.idle(105);
        scene.overlay().showText(110).text(I18n.format("hnn.ponder.ports")).pointAt(util.vector().centerOf(6, 2, 1)).placeNearTarget(); scene.idle(115); scene.markAsFinished();
    }
}
