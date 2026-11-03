package dev.shadowsoffire.hostilenetworks.client;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.gui.LearnerContainer;
import java.util.*;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.*;
import net.minecraft.util.*;

/** Layout and texture regions follow upstream DeepLearnerScreen (338 x 235). */
public final class LearnerScreen extends ResponsiveContainer {
    private static final ResourceLocation BASE = texture("deep_learner");
    public final LearnerContainer learner;
    private int selected, ticks, variant;
    private EntityLivingBase entity;
    private String lastDisplay = "";
    public LearnerScreen(EntityPlayer player, EnumHand hand) {
        super(new LearnerContainer(player, hand), 338, 235); learner = (LearnerContainer) inventorySlots;
        overflowLeft = 41; overflowRight = -41;
    }
    @Override public void initGui() {
        super.initGui(); buttonList.clear();
        addButton(new Arrow(0, guiLeft - 27, guiTop + 105, 24, 24, "left"));
        addButton(new Arrow(1, guiLeft - 1, guiTop + 105, 24, 24, "right"));
    }
    private DataModel selected() { return ModelStack.model(learner.models.getStackInSlot(selected)); }
    private int count() { int count = 0; for (int i = 0; i < 4; i++) if (ModelStack.model(learner.models.getStackInSlot(i)) != null) count++; return count; }
    @Override public void updateScreen() {
        super.updateScreen(); ticks++;
        if (selected() == null && count() > 0) select(1);
        for (GuiButton button : buttonList) button.visible = count() > 1;
        DataModel model = selected();
        if (model != null && ticks % 80 == 0) variant = (variant + 1) % (model.variants.size() + 1);
    }
    private void select(int direction) {
        for (int i = 0; i < 4; i++) {
            selected = Math.floorMod(selected + direction, 4);
            if (selected() != null) break;
        }
        variant = 0; ticks = 0; lastDisplay = "";
    }
    @Override protected void actionPerformed(GuiButton button) { select(button.id == 0 ? -1 : 1); }
    private void updateEntity(DataModel model) {
        if (model == null || model.blockModel) { entity = null; return; }
        ResourceLocation target = variant == 0 || variant > model.variants.size() ? model.target : model.variants.get(variant - 1);
        String key = model.id + ":" + target;
        if (key.equals(lastDisplay)) return;
        lastDisplay = key; Entity created = EntityList.createEntityByIDFromName(target, mc.world);
        entity = created instanceof EntityLivingBase living ? living : null;
        if (entity != null && model.json.has("display") && model.json.getAsJsonObject("display").has("nbt")) {
            try {
                String snbt = model.json.getAsJsonObject("display").get("nbt").toString();
                NBTTagCompound tag = new NBTTagCompound(); entity.writeToNBT(tag); tag.merge(JsonToNBT.getTagFromJson(snbt)); entity.readFromNBT(tag);
                if (model.target.toString().equals("minecraft:sheep")) entity.setCustomNameTag("jeb_");
            } catch (NBTException ignored) { }
        }
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial, int mx, int my) {
        drawDefaultBackground(); blit(BASE, guiLeft + 41, guiTop, 0, 0, 256, 140);
        blit(PLAYER, guiLeft + 81, guiTop + 145, 0, 0, 176, 90);
        DataModel model = selected(); updateEntity(model);
        if (model == null) return;
        blit(BASE, guiLeft - 41, guiTop, 9, 140, 75, 101);
        for (int i = 0; i < 3; i++) blit(BASE, guiLeft + 189, guiTop + 17 + 11 * i, model.blockModel ? 96 : 0, 140 + 9 * i, 9, 9);
        if (model.blockModel) item(new net.minecraft.item.ItemStack(net.minecraft.block.Block.REGISTRY.getObject(model.target)), guiLeft - 12, guiTop + 42);
        else if (entity != null) {
            float scale = model.json.has("display") && model.json.getAsJsonObject("display").has("scale") ? model.json.getAsJsonObject("display").get("scale").getAsFloat() : 1;
            int size = Math.max(1, (int) Math.min(40 * scale, 88 / Math.max(.5f, entity.height)));
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            GuiInventory.drawEntityOnScreen(guiLeft - 4, guiTop + 90, size, (float) Math.sin(ticks / 45D) * 30, 0, entity);
        }
    }
    @Override protected void drawGuiContainerForegroundLayer(int mx, int my) {
        DataModel model = selected();
        if (model == null) {
            int y = 6;
            for (int i = 0; i < 7; i++) y += wrapped(tr("hostilenetworks.gui.learner_empty." + i), 49, y, 200, i == 0 ? AQUA : WHITE, 2);
            return;
        }
        var stack = learner.models.getStackInSlot(selected); ModelTier tier = model.tier(ModelStack.data(stack));
        text(tr("hostilenetworks.gui.name"), 49, 6, 136, AQUA);
        int y = 18 + wrapped(variant > 0 && entity != null ? tr("hostilenetworks.gui.variant", entity.getName()) : model.name(), 49, 18, 136, WHITE, 2);
        text(tr("hostilenetworks.gui.info"), 49, y, 136, AQUA);
        if (model.json.has("trivia")) wrapped(tr(model.json.get("trivia").getAsString()), 49, y + 12, 136, WHITE, Math.max(0, (96 - y) / 12));
        text(tr("hostilenetworks.gui.stats"), 189, 6, 100, AQUA);
        if (entity != null && !model.blockModel) {
            text(String.format(Locale.ROOT, "%.1f", entity.getMaxHealth()), 202, 18, 84, WHITE);
            text(String.valueOf(entity.getTotalArmorValue()), 202, 29, 84, WHITE);
            var attack = entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
            text(attack == null ? "0" : String.format(Locale.ROOT, "%.1f", attack.getAttributeValue()), 202, 40, 84, WHITE);
        }
        text(tr("hostilenetworks.gui.tier", "§f" + tr("hostilenetworks.tier." + tier.id())), 49, 102, 200, WHITE);
        text(tr("hostilenetworks.gui.accuracy", String.format(Locale.ROOT, "%.1f%%", model.accuracy(ModelStack.data(stack)) * 100)), 49, 114, 200, WHITE);
        if (tier == ModelRegistry.tiers().getLast()) text(tr("hostilenetworks.gui.max_tier"), 49, 126, 200, 0xFF5555);
        else if (HostileConfig.actionUpgradesModel) {
            ModelTier next = model.next(tier); int gained = model.gained(tier), remaining = Math.max(0, model.required(next) - ModelStack.data(stack));
            int actions = gained == 0 ? remaining : (remaining + gained - 1) / gained;
            String word = tr("hostilenetworks.gui.action." + (model.blockModel ? "mine" : "kill") + (actions == 1 ? "" : "s"));
            text(tr("hostilenetworks.gui.next_tier", tr("hostilenetworks.tier." + next.id()), actions, word), 49, 126, 200, WHITE);
        }
    }
}
