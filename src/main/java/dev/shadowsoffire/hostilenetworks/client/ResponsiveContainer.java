package dev.shadowsoffire.hostilenetworks.client;

import java.awt.Rectangle;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Upstream texture coordinates remain in logical pixels, including off-panel tabs.
 * GuiScreen already maps mouse events through our virtual width and height;
 * only rendering receives coordinates in the actual scaled viewport.
 */
public abstract class ResponsiveContainer extends GuiContainer {
    protected static final int AQUA = 0x62D8FF, WHITE = 0xFFFFFF, LIME = 0x00FFC0;
    protected static final ResourceLocation PLAYER = texture("default_gui");
    protected float fit = 1;
    protected int overflowLeft, overflowRight;
    protected ResponsiveContainer(Container container, int width, int height) { super(container); xSize = width; ySize = height; }
    protected static ResourceLocation texture(String name) { return new ResourceLocation("hostilenetworks", "textures/gui/" + name + ".png"); }
    @Override public void setWorldAndResolution(Minecraft mc, int width, int height) {
        fit = Math.min(1f, Math.min(width / (float) (xSize + overflowLeft + overflowRight + 12), height / (float) (ySize + 12)));
        super.setWorldAndResolution(mc, Math.round(width / fit), Math.round(height / fit));
    }
    @Override public void initGui() {
        super.initGui(); guiLeft = (width - xSize - overflowLeft - overflowRight) / 2 + overflowLeft;
    }
    /** Actual scaled-screen coordinates, including the controls outside the inventory panel. */
    public Rectangle getVisibleArea() {
        return displayArea(guiLeft - overflowLeft, guiTop, xSize + overflowLeft + overflowRight, ySize);
    }
    protected Rectangle displayArea(int x, int y, int w, int h) {
        int left = (int) Math.floor(x * fit), top = (int) Math.floor(y * fit);
        return new Rectangle(left, top, (int) Math.ceil((x + w) * fit) - left, (int) Math.ceil((y + h) * fit) - top);
    }
    @Override public void drawScreen(int mouseX, int mouseY, float ticks) {
        GlStateManager.pushMatrix(); GlStateManager.scale(fit, fit, 1);
        super.drawScreen(Math.round(mouseX / fit), Math.round(mouseY / fit), ticks);
        renderHoveredToolTip(Math.round(mouseX / fit), Math.round(mouseY / fit)); GlStateManager.popMatrix();
    }
    @Override public void drawDefaultBackground() {
        // Forge posts BackgroundDrawnEvent here. Overlays such as JEI use actual
        // viewport coordinates and must not inherit this container's transform.
        int logicalWidth = width, logicalHeight = height;
        ScaledResolution resolution = new ScaledResolution(mc);
        width = resolution.getScaledWidth(); height = resolution.getScaledHeight();
        GlStateManager.pushMatrix(); GlStateManager.scale(1 / fit, 1 / fit, 1);
        try { super.drawDefaultBackground(); }
        finally { GlStateManager.popMatrix(); width = logicalWidth; height = logicalHeight; }
    }
    protected void blit(ResourceLocation resource, int x, int y, int u, int v, int w, int h) {
        GlStateManager.color(1, 1, 1, 1); GlStateManager.enableBlend(); mc.getTextureManager().bindTexture(resource);
        drawTexturedModalRect(x, y, u, v, w, h);
    }
    protected void item(ItemStack stack, int x, int y) {
        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(stack, x, y); itemRender.renderItemOverlayIntoGUI(fontRenderer, stack, x, y, null);
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting(); GlStateManager.color(1, 1, 1, 1);
    }
    protected boolean over(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    protected void tip(List<String> lines, int mouseX, int mouseY) {
        GlStateManager.pushMatrix(); GlStateManager.scale(fit, fit, 1);
        drawHoveringText(lines, Math.round(mouseX / fit), Math.round(mouseY / fit)); GlStateManager.popMatrix();
    }
    protected String tr(String key, Object... args) { return net.minecraft.client.resources.I18n.format(key, args).replace("\\n", "\n"); }
    protected void text(String value, int x, int y, int maxWidth, int color) { fontRenderer.drawString(fontRenderer.trimStringToWidth(value, maxWidth), x, y, color); }
    protected int wrapped(String value, int x, int y, int maxWidth, int color, int maxLines) {
        int line = 0;
        for (String part : fontRenderer.listFormattedStringToWidth(value, maxWidth)) {
            if (line >= maxLines) break; fontRenderer.drawString(part, x, y + line * 12, color); line++;
        }
        return line * 12;
    }
    protected final class Arrow extends GuiButton {
        private final String sprite;
        Arrow(int id, int x, int y, int w, int h, String sprite) { super(id, x, y, w, h, ""); this.sprite = sprite; }
        @Override public void drawButton(Minecraft mc, int mx, int my, float partial) {
            if (!visible) return; hovered = mx >= x && my >= y && mx < x + width && my < y + height;
            mc.getTextureManager().bindTexture(texture("sprites/widget/" + sprite + (hovered ? "_hovered" : "")));
            GlStateManager.color(1, 1, 1, 1); GlStateManager.enableBlend();
            drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
        }
    }
}
