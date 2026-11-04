package dev.shadowsoffire.hostilenetworks.client;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;

public final class PerfectModelRenderer extends TileEntityItemStackRenderer {
    @Override public void renderByItem(ItemStack stack, float partial) {
        var renderer = Minecraft.getMinecraft().getRenderItem();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(.5, .5, .5);
            renderer.renderItem(Hostile.stack("blank_extra_data_model"), ItemCameraTransforms.TransformType.NONE);
            var models = PerfectModel.models(stack);
            for (int i = 0; i < models.size(); i++) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(i % 2 == 0 ? -.21 : .21, i / 2 == 0 ? .21 : -.21, .08);
                GlStateManager.scale(.42, .42, .42);
                renderer.renderItem(ModelStack.create(models.get(i), false), ItemCameraTransforms.TransformType.NONE);
                GlStateManager.popMatrix();
            }
        } finally { GlStateManager.color(1, 1, 1, 1); GlStateManager.popMatrix(); }
    }
}
