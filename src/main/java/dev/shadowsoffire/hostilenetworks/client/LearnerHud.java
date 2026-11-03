package dev.shadowsoffire.hostilenetworks.client;

import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Original 28-pixel HUD rows, model cards and tier progress, using the upstream assets. */
final class LearnerHud extends Gui {
    private static final ResourceLocation BAR = new ResourceLocation("hostilenetworks", "textures/gui/deep_learner_hud.png");
    private static final ResourceLocation BACKGROUND = new ResourceLocation("hostilenetworks", "textures/gui/sprites/dl_hud_bg.png");
    static void render(ItemStack learner) {
        var inventory = DeepLearnerItem.inventory(learner);
        List<ItemStack> models = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) if (ModelStack.model(inventory.getStackInSlot(i)) != null) models.add(inventory.getStackInSlot(i));
        if (models.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix(); GlStateManager.color(1, 1, 1, 1); GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.getTextureManager().bindTexture(BACKGROUND);
        int height = 5 + 28 * models.size();
        // Preserve the one-pixel frame of the upstream nine-slice sprite.
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            int x = col == 0 ? 0 : col == 1 ? 1 : 112;
            int v = row == 0 ? 0 : row == 1 ? 1 : 99;
            int y = row == 0 ? 0 : row == 1 ? 1 : height - 1;
            drawScaledCustomSizeModalRect(3 + x, 3 + y, x, v, col == 1 ? 111 : 1, row == 1 ? 98 : 1,
                col == 1 ? 111 : 1, row == 1 ? height - 2 : 1, 113, 100);
        }
        for (int i = 0; i < models.size(); i++) {
            ItemStack stack = models.get(i); DataModel model = ModelStack.model(stack);
            int data = ModelStack.data(stack), y = 6 + i * 28;
            ModelTier tier = model.tier(data), next = model.next(tier);
            int filled = next == tier ? 87 : (int) Math.ceil(87D * (data - model.required(tier)) / Math.max(1, model.required(next) - model.required(tier)));
            mc.getTextureManager().bindTexture(BAR); GlStateManager.color(1, 1, 1, 1);
            drawModalRectWithCustomSizedTexture(24, y + 11, 0, 0, 89, 12, 256, 256);
            drawModalRectWithCustomSizedTexture(25, y + 12, 0, 12, Math.clamp(filled, 0, 87), 10, 256, 256);
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 6, y + 9);
            RenderHelper.disableStandardItemLighting();
            String tierName = I18n.format("hostilenetworks.tier." + tier.id());
            mc.fontRenderer.drawStringWithShadow(tierName + I18n.format("hostilenetworks.hud.model"), 8, y, tier.color());
            if (next != tier) {
                int remaining = Math.max(0, model.required(next) - data), gained = model.gained(tier);
                int actions = gained == 0 ? remaining : (remaining + gained - 1) / gained;
                mc.fontRenderer.drawStringWithShadow(I18n.format("hostilenetworks.hud.kills", actions), 27, y + 13, 0xFFFFFF);
            }
        }
        GlStateManager.color(1, 1, 1, 1); GlStateManager.popMatrix();
    }
}
