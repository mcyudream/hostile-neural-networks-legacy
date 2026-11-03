package dev.shadowsoffire.hostilenetworks.client;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class DataModelRenderer extends TileEntityItemStackRenderer {
    private final Map<ResourceLocation, Entity> entities = new HashMap<>();
    private net.minecraft.world.World world;
    @Override public void renderByItem(ItemStack stack, float partial) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(.5, .5, .5);
            mc.getRenderItem().renderItem(Hostile.stack("blank_data_model"), ItemCameraTransforms.TransformType.NONE);
            DataModel model = ModelStack.model(stack); if (model == null || mc.world == null) return;
            if (mc.world != world) { entities.clear(); world = mc.world; }
            GlStateManager.translate(0, -.25, .08);
            if (model.blockModel) {
                GlStateManager.translate(0, .25, .02); GlStateManager.scale(.4, .4, .4);
                mc.getRenderItem().renderItem(new ItemStack(net.minecraft.block.Block.REGISTRY.getObject(model.target)), ItemCameraTransforms.TransformType.GUI);
            } else {
                Entity entity = entities.computeIfAbsent(model.target, key -> EntityList.createEntityByIDFromName(key, mc.world));
                if (entity == null) return;
                float scale = .58f / Math.max(1, Math.max(entity.width, entity.height));
                GlStateManager.scale(scale, scale, scale); GlStateManager.rotate(180, 0, 1, 0);
                mc.getRenderManager().renderEntity(entity, 0, 0, 0, 0, 0, false);
            }
        } finally { GlStateManager.color(1, 1, 1, 1); GlStateManager.popMatrix(); }
    }
}
