package dev.shadowsoffire.hostilenetworks.client;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.gui.MachineContainer;
import dev.shadowsoffire.hostilenetworks.network.Network;
import dev.shadowsoffire.hostilenetworks.tile.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Extra HNN's original texture regions and slot coordinates, adapted to GuiContainer. */
public final class ExpansionScreen extends ResponsiveContainer {
    public final ExpansionTile machine;
    private int page;
    private static ResourceLocation extra(String name) { return new ResourceLocation("extrahnn", "textures/gui/" + name + ".png"); }
    private final ResourceLocation base;
    public ExpansionScreen(EntityPlayer player, ExpansionTile tile) {
        super(new MachineContainer(player, tile), MachineContainer.width(tile), MachineContainer.height(tile)); machine = tile;
        base = extra(tile instanceof MergerTile ? "merger_camera" : tile instanceof UltimateSimTile ? "ultimate_sim_chamber" : tile instanceof UltimateFabTile ? "ultimate_loot_fabricator" : "simulation_modeling");
        overflowLeft = tile instanceof ModelTrainerTile || tile instanceof DigitalAgonizerTile ? 38 : tile instanceof UltimateSimTile ? 14 : 22;
        overflowRight = tile instanceof UltimateFabTile ? 62 : tile instanceof UltimateSimTile ? 14 : 0;
    }
    private DataModel model() { return ModelStack.model(machine.inventory.getStackInSlot(0)); }
    private int rx() { return machine instanceof UltimateSimTile ? 228 : machine instanceof ModelTrainerTile || machine instanceof DigitalAgonizerTile ? -36 : -22; }
    private void send(int action, int value) { Network.CHANNEL.sendToServer(new Network.Action(inventorySlots.windowId, action, 0, value)); }
    @Override public void initGui() {
        super.initGui(); buttonList.clear();
        if (machine instanceof UltimateFabTile) {
            addButton(new Arrow(0, guiLeft + 13, guiTop + 90, 29, 12, "fab_left"));
            addButton(new Arrow(1, guiLeft + 46, guiTop + 90, 29, 12, "fab_right"));
        }
    }
    @Override public void updateScreen() {
        super.updateScreen(); DataModel m = model();
        page = m == null ? 0 : Math.clamp(page, 0, Math.max(0, (m.drops.size() - 1) / 9));
        for (GuiButton b : buttonList) b.visible = m != null && (b.id == 0 ? page > 0 : (page + 1) * 9 < m.drops.size());
    }
    @Override protected void actionPerformed(GuiButton b) { page = Math.max(0, page + (b.id == 0 ? -1 : 1)); }
    private void control(int x, int y, ItemStack icon) {
        blit(texture("loot_fabricator"), guiLeft + x, guiTop + y, 31, 83, 18, 18); item(icon, guiLeft + x + 1, guiTop + y + 1);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial, int mx, int my) {
        drawDefaultBackground(); int l = guiLeft, t = guiTop;
        double energy = machine.energy.getEnergyStored() / (double) Math.max(1, machine.energy.getMaxEnergyStored());
        if (machine instanceof MergerTile) {
            blit(base, l, t, 0, 0, 216, 152); blit(base, l + 203, t + 59, 216, 0, 7, 87 - (int) Math.ceil(87 * energy));
            blit(PLAYER, l + 20, t + 153, 0, 0, 176, 90);
            if (machine.progress > 0) {
                double f = machine.progress / (double) machine.duration();
                for (int i = 0; i < 4; i++) { int x = new int[]{70, 95, 119, 144}[i], y = i == 0 || i == 3 ? 28 : 26; drawRect(l + x, t + y, l + x + 2, t + y + (int) (16 * f), 0xFF00FFC0); }
            }
        } else if (machine instanceof UltimateSimTile) {
            blit(base, l + 8, t, 0, 0, 216, 166); blit(base, l - 14, t, 216, 0, 18, 18);
            blit(base, l + 211, t + 73, 234, 0, 7, 87 - (int) Math.ceil(87 * energy));
            ItemStack stack = machine.inventory.getStackInSlot(0);
            double data = LearningModel.valid(stack) ? LearningModel.fraction(stack) : 0;
            blit(base, l + 14, t + 73, 234, 0, 7, 87 - (int) Math.ceil(87 * data));
            blit(PLAYER, l + 28, t + 167, 0, 0, 176, 90);
            control(228, 24, new ItemStack(machine.extractModel ? Items.HOPPER_MINECART : Items.MINECART));
        } else if (machine instanceof UltimateFabTile) {
            blit(base, l, t, 0, 0, 212, 119);
            int h = (int) (53 * energy); blit(base, l + 6, t + 86 - h, 0, 119, 7, h);
            h = Math.min(35, machine.progress * 35 / machine.duration()); blit(base, l + 84, t + 70 - h, 7, 119, 6, h);
            blit(PLAYER, l + 18, t + 120, 0, 0, 176, 90); fabrication(mx - l, my - t);
        } else {
            blit(base, l + 8, t, 0, 0, 216, 97); blit(base, l - 14, t, 223, 0, 18, 18);
            if (machine instanceof ModelTrainerTile) {
                blit(base, l - 14, t + 20, 223, 18, 18, 18); blit(base, l - 14, t + 38, 223, 18, 18, 18);
            } else blit(base, l - 14, t + 24, 223, 18, 18, 18);
            blit(base, l + 211, t + 5, 216, 0, 7, 87 - (int) Math.ceil(87 * energy));
            blit(PLAYER, l + 28, t + 98, 0, 0, 176, 90);
        }
        blit(texture("loot_fabricator"), l + rx(), t, 31, 83, 18, 18);
        String glyph = machine.redstone == 0 ? "items/redstone_dust" : machine.redstone == 1 ? "blocks/redstone_torch_off" : "blocks/redstone_torch_on";
        mc.getTextureManager().bindTexture(new ResourceLocation("minecraft", "textures/" + glyph + ".png"));
        drawModalRectWithCustomSizedTexture(l + rx() + 1, t + 1, 0, 0, 16, 16, 16, 16);
        GlStateManager.color(1, 1, 1, 1);
    }
    private void fabrication(int mx, int my) {
        DataModel m = model(); if (m == null) return;
        var state = machine.selections.get(m); int selected = machine.selections.current(m);
        for (int i = 0; i < 9 && page * 9 + i < m.drops.size(); i++) {
            int x = 17 + i % 3 * 19, y = 32 + i / 3 * 19;
            if (!state.queue && selected == page * 9 + i || over(mx, my, x, y, 16, 16)) blit(base, guiLeft + x - 1, guiTop + y - 1, selected == page * 9 + i && !state.queue ? 31 : 13, 119, 18, 18);
            item(m.drops.get(page * 9 + i), guiLeft + x, guiTop + y);
        }
        if (selected >= 0) item(m.drops.get(selected), guiLeft + 79, guiTop + 15);
        control(216, 0, new ItemStack(state.queue ? Items.KNOWLEDGE_BOOK : Items.ITEM_FRAME));
        if (state.queue) {
            control(236, 0, new ItemStack(net.minecraft.init.Blocks.BARRIER));
            int visible = Math.min(state.drops.size(), state.drops.size() > 9 ? 8 : 9);
            for (int i = 0; i < 9; i++) {
                int x = guiLeft + 216 + i % 3 * 18, y = guiTop + 30 + i / 3 * 18;
                blit(base, x, y, i == state.cursor && i < visible ? 31 : 13, 119, 18, 18);
                if (i < visible && state.drops.get(i) < m.drops.size()) item(m.drops.get(state.drops.get(i)), x + 1, y + 1);
                else if (i == 8 && state.drops.size() > 9) text("+" + (state.drops.size() - 8), x + 1, y + 5, 17, WHITE);
            }
        }
    }
    @Override protected void drawGuiContainerForegroundLayer(int mx, int my) {
        ItemStack s = machine.inventory.getStackInSlot(0); int percent = Math.min(100, machine.progress * 100 / machine.duration());
        if (machine instanceof UltimateFabTile) {
            text(tr("hnn.extra.batch", ((UltimateFabTile) machine).batchLimit()), 17, 8, 60, AQUA); return;
        }
        if (machine instanceof UltimateSimTile) {
            var models = PerfectModel.models(s);
            // Match the upstream compact target list without intruding on output slots.
            GlStateManager.pushMatrix(); GlStateManager.scale(.7, .7, 1);
            text(tr("hnn.extra.targets"), 25, 6, 180, AQUA);
            for (int i = 0; i < models.size(); i++) text("- " + models.get(i).name(), 25, 19 + i * 13, 180, LIME);
            if (!models.isEmpty()) { text(tr(LearningModel.tierKey(s)), 25, 72, 180, WHITE); text(tr("hostilenetworks.gui.accuracy", String.format(Locale.ROOT, "%.1f%%", PerfectModel.accuracy(s) * 100)), 25, 85, 180, WHITE); }
            GlStateManager.popMatrix();
            wrapped(tr("hnn.status." + machine.failure), 29, 76, 174, WHITE, 4); text(percent + "%", 181, 149, 30, AQUA);
        } else if (machine instanceof MergerTile) {
            wrapped(tr("hnn.status." + machine.failure), 8, 61, 188, WHITE, 5); text(percent + "%", 177, 135, 32, AQUA);
        } else if (machine instanceof DigitalAgonizerTile a) {
            text(tr("tile.hostilenetworks.digital_agonizer.name"), 20, 9, 184, AQUA);
            text(a.altar == null ? tr("hnn.status.altar_missing") : tr("hnn.extra.altar", a.altar.getX(), a.altar.getY(), a.altar.getZ()), 20, 23, 184, WHITE);
            text(tr("hnn.extra.lp", a.altarBlood, a.altarCapacity), 20, 36, 184, WHITE);
            text(tr("hnn.extra.lp_cycle", a.predictedEssence(), a.charges), 20, 49, 184, WHITE);
            text(tr("hnn.status." + a.failure), 20, 63, 184, WHITE); text(percent + "%", 20, 77, 184, AQUA);
        } else {
            text(tr("tile.hostilenetworks.simulator_modeling.name"), 20, 9, 184, AQUA);
            if (LearningModel.valid(s)) {
                text(tr("hostilenetworks.gui.tier", tr(LearningModel.tierKey(s))), 20, 23, 184, WHITE);
                text(tr("hnn.model_data", ModelStack.data(s), LearningModel.required(s, true)), 20, 36, 184, WHITE);
                text(tr("hnn.extra.gain", LearningModel.gained(s) * (((ModelTrainerTile) machine).upgrade("upgrade_data_kill") ? ExpansionConfig.dataMultiplier : 1)), 20, 49, 184, WHITE);
            }
            text(tr("hnn.status." + machine.failure), 20, 63, 184, WHITE); text(percent + "%", 20, 77, 184, AQUA);
        }
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        int mx = Math.round(x / fit) - guiLeft, my = Math.round(y / fit) - guiTop;
        if (button == 0) {
            if (over(mx, my, rx(), 0, 18, 18)) { send(0, 0); return; }
            if (machine instanceof UltimateSimTile && over(mx, my, 228, 24, 18, 18)) { send(6, 0); return; }
            DataModel m = model();
            if (machine instanceof UltimateFabTile && m != null) {
                var state = machine.selections.get(m);
                if (over(mx, my, 216, 0, 18, 18)) { send(3, 0); return; }
                if (state.queue && over(mx, my, 236, 0, 18, 18)) { send(4, 0); return; }
                if (over(mx, my, 79, 15, 16, 16)) { send(state.queue ? 5 : 4, state.cursor); return; }
                for (int i = 0; i < 9; i++) if (page * 9 + i < m.drops.size() && over(mx, my, 17 + i % 3 * 19, 32 + i / 3 * 19, 16, 16)) { send(2, page * 9 + i); return; }
                int visible = state.drops.size() > 9 ? 8 : state.drops.size();
                if (state.queue) for (int i = 0; i < visible; i++) if (over(mx, my, 216 + i % 3 * 18, 30 + i / 3 * 18, 18, 18)) { send(5, i); return; }
            }
        }
        super.mouseClicked(x, y, button);
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partial) {
        super.drawScreen(mouseX, mouseY, partial);
        int x = Math.round(mouseX / fit) - guiLeft, y = Math.round(mouseY / fit) - guiTop; List<String> lines = new ArrayList<>();
        if (over(x, y, rx(), 0, 18, 18)) lines.add(tr("hnn.redstone." + machine.redstone));
        int ex = machine instanceof MergerTile ? 203 : machine instanceof UltimateFabTile ? 6 : 211;
        int ey = machine instanceof MergerTile ? 59 : machine instanceof UltimateFabTile ? 33 : machine instanceof UltimateSimTile ? 73 : 5;
        if (over(x, y, ex, ey, 7, machine instanceof UltimateFabTile ? 53 : 87)) { lines.add(tr("hostilenetworks.gui.energy", machine.energy.getEnergyStored(), machine.energy.getMaxEnergyStored())); lines.add(tr("hnn.model_cost", machine.cost())); }
        if (machine instanceof UltimateSimTile && over(x, y, 228, 24, 18, 18)) lines.add(tr("hnn.extra.extract." + machine.extractModel));
        if (machine instanceof UltimateFabTile && model() != null) {
            var m = model(); var state = machine.selections.get(m);
            if (over(x, y, 216, 0, 18, 18)) lines.add(tr(state.queue ? "hnn.queue" : "hnn.fixed"));
            if (state.queue && over(x, y, 236, 0, 18, 18)) lines.add(tr("hnn.clear_queue"));
            for (int i = 0; i < 9 && page * 9 + i < m.drops.size(); i++) if (over(x, y, 17 + i % 3 * 19, 32 + i / 3 * 19, 16, 16)) { lines.add(m.drops.get(page * 9 + i).getDisplayName()); if (state.queue) lines.add(tr("hostilenetworks.gui.queue_add")); }
            if (over(x, y, 79, 15, 16, 16)) lines.add(tr(state.queue ? "hostilenetworks.gui.queue_clear_current" : "hostilenetworks.gui.clear"));
            int visible = state.drops.size() > 9 ? 8 : state.drops.size();
            if (state.queue) for (int i = 0; i < visible; i++) if (over(x, y, 216 + i % 3 * 18, 30 + i / 3 * 18, 18, 18)) { if (state.drops.get(i) < m.drops.size()) lines.add(m.drops.get(state.drops.get(i)).getDisplayName()); lines.add(tr("hostilenetworks.gui.queue_remove")); }
        }
        if (!lines.isEmpty()) tip(lines, mouseX, mouseY);
    }
}
