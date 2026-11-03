package dev.shadowsoffire.hostilenetworks.client;

import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.tile.DataCenterTile;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.*;
import net.minecraft.util.ResourceLocation;

public final class DataCenterRenderer extends TileEntitySpecialRenderer<DataCenterTile> {
    private final Map<ResourceLocation, Entity> entities = new HashMap<>();
    private net.minecraft.world.World cachedWorld;
    @Override public void render(DataCenterTile tile, double x, double y, double z, float partial, int stage, float alpha) {
        if (!tile.shellValid || tile.layout == null) return;
        if (cachedWorld != tile.getWorld()) { entities.clear(); cachedWorld = tile.getWorld(); }
        int shown = 0;
        for (int i = 0; i < 25 && shown < 12; i++) {
            if (tile.progress[i] <= 0 || !"none".equals(tile.failures[i])) continue;
            DataModel model = ModelStack.model(tile.inventory.getStackInSlot(i)); if (model == null || model.blockModel) continue;
            Entity entity = entities.computeIfAbsent(model.target, key -> EntityList.createEntityByIDFromName(key, tile.getWorld()));
            if (entity == null) continue;
            double time = tile.getWorld().getTotalWorldTime() + partial, angle = time / 75 + shown * Math.PI / 6;
            var center = tile.layout.center();
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + center.getX() - tile.getPos().getX() + .5 + Math.cos(angle) * 1.6,
                y + center.getY() - tile.getPos().getY() - .7 + (shown % 3) * .7,
                z + center.getZ() - tile.getPos().getZ() + .5 + Math.sin(angle) * 1.6);
            float scale = .7f / Math.max(1, entity.height); GlStateManager.scale(scale, scale, scale);
            Minecraft.getMinecraft().getRenderManager().renderEntity(entity, 0, 0, 0, (float) (180 - angle * 180 / Math.PI), partial, false);
            GlStateManager.popMatrix(); shown++;
        }
        GlStateManager.color(1, 1, 1, 1);
    }
}
