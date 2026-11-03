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
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

/** Uses the original HNN texture regions, slot positions and off-panel controls. */
public final class MachineScreen extends ResponsiveContainer {
    private static final ResourceLocation SIM = texture("sim_chamber"), FAB = texture("loot_fabricator"), DC = texture("data_center");
    public final MachineTile machine;
    private int selectedModel, page;
    private boolean configuring;
    public MachineScreen(EntityPlayer player, MachineTile tile) {
        super(new MachineContainer(player, tile), MachineContainer.width(tile), MachineContainer.height(tile)); machine = tile;
        overflowLeft = tile instanceof SimChamberTile ? 14 : 22; overflowRight = tile instanceof LootFabTile ? 84 : tile instanceof SimChamberTile ? 14 : 0;
    }
    @Override public java.awt.Rectangle getVisibleArea() {
        return configuring ? displayArea(left() - 22, top() - 12, 282, 112) : super.getVisibleArea();
    }
    @Override public void initGui() { super.initGui(); buttons(); }
    private boolean fabrication() { return machine instanceof LootFabTile || configuring; }
    private int left() { return configuring ? (width - 282) / 2 + 22 : guiLeft; }
    private int top() { return configuring ? (height - 100) / 2 : guiTop; }
    private DataModel selected() { return ModelStack.model(machine.inventory.getStackInSlot(machine instanceof DataCenterTile ? selectedModel : 0)); }
    private Selections.Entry entry(DataModel model) { return ((ProductionTile) machine).selections.get(model); }
    private void send(int action, int value) { Network.CHANNEL.sendToServer(new Network.Action(inventorySlots.windowId, action, selectedModel, value)); }
    private void click(int action, int value) { send(action, value); mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1)); }
    private void buttons() {
        buttonList.clear();
        if (fabrication()) {
            addButton(new Arrow(10, left() + 13, top() + 68, 29, 12, "fab_left"));
            addButton(new Arrow(11, left() + 46, top() + 68, 29, 12, "fab_right"));
        }
    }
    @Override public void updateScreen() {
        super.updateScreen(); DataModel model = selected();
        if (configuring && model == null) configure(false);
        if (fabrication()) for (GuiButton button : buttonList) button.visible = model != null && (button.id == 10 ? page > 0 : (page + 1) * 9 < model.drops.size());
    }
    @Override protected void actionPerformed(GuiButton button) {
        DataModel model = selected();
        if (button.id == 10) page = Math.max(0, page - 1);
        else if (button.id == 11 && model != null && (page + 1) * 9 < model.drops.size()) page++;
    }
    private void configure(boolean value) { configuring = value; ((MachineContainer) inventorySlots).configurationView = value; page = 0; buttons(); }
    @Override protected void keyTyped(char key, int code) throws IOException {
        if (configuring && (code == Keyboard.KEY_ESCAPE || mc.gameSettings.keyBindInventory.isActiveAndMatches(code))) { configure(false); return; }
        super.keyTyped(key, code);
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        int lx = Math.round(x / fit) - left(), ly = Math.round(y / fit) - top();
        if (configuring && over(lx, ly, -22, 0, 18, 18)) { configure(false); return; }
        if (!configuring && button == 0) {
            int rx = machine instanceof SimChamberTile ? 228 : -22;
            if (over(lx, ly, rx, 0, 18, 18)) { click(0, 0); return; }
            if (machine instanceof SimChamberTile && over(lx, ly, 228, 20, 18, 18)) { click(1, 0); return; }
        }
        if (!configuring && button == 1 && machine instanceof DataCenterTile) {
            for (int i = 0; i < 25; i++) if (over(lx, ly, 9 + i % 5 * 18, 21 + i / 5 * 20, 16, 16)
                && ModelStack.model(machine.inventory.getStackInSlot(i)) != null) { selectedModel = i; configure(true); return; }
        }
        DataModel model = selected();
        if (button == 0 && fabrication() && model != null) {
            Selections.Entry state = entry(model);
            if (over(lx, ly, 180, 0, 18, 18)) { click(3, 0); return; }
            if (state.queue && over(lx, ly, 200, 0, 18, 18)) { click(4, 0); return; }
            int current = ((ProductionTile) machine).selections.current(model);
            if (current >= 0 && over(lx, ly, 79, 5, 16, 16)) { click(state.queue ? 5 : 4, state.drops.isEmpty() ? 0 : Math.floorMod(state.cursor, state.drops.size())); return; }
            for (int i = 0; i < 9; i++) if (over(lx, ly, 17 + i % 3 * 19, 9 + i / 3 * 19, 16, 16)) {
                int drop = page * 9 + i; if (drop < model.drops.size()) { click(2, drop); return; }
            }
            int visible = state.drops.size() > 9 ? 8 : state.drops.size();
            if (state.queue) for (int i = 0; i < visible; i++) if (over(lx, ly, 180 + i % 3 * 18, 30 + i / 3 * 18, 18, 18)) { click(5, i); return; }
        }
        if (configuring) {
            for (GuiButton guiButton : buttonList) if (guiButton.mousePressed(mc, Math.round(x / fit), Math.round(y / fit))) { guiButton.playPressSound(mc.getSoundHandler()); actionPerformed(guiButton); }
            return;
        }
        super.mouseClicked(x, y, button);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial, int mx, int my) {
        drawDefaultBackground(); int l = left(), t = top();
        if (machine instanceof SimChamberTile sim) {
            blit(SIM, l + 8, t, 0, 0, 216, 141); blit(SIM, l - 14, t, 0, 141, 18, 18);
            icon(SIM, 0, 141, l + 228, t, redstoneIcon());
            icon(SIM, 0, 141, l + 228, t + 20, sim.mode == 0 ? "items/ender_eye" : "items/experience_bottle");
            int missingEnergy = 87 - (int) Math.ceil(87D * sim.energy.getEnergyStored() / Math.max(1, sim.energy.getMaxEnergyStored()));
            blit(SIM, l + 211, t + 48, 18, 141, 7, missingEnergy);
            DataModel model = selected(); int missingData = 87;
            if (model != null) {
                int data = ModelStack.data(sim.inventory.getStackInSlot(0)); ModelTier tier = model.tier(data), next = model.next(tier);
                missingData = next == tier ? 0 : 87 - (int) Math.ceil(87D * (data - model.required(tier)) / Math.max(1, model.required(next) - model.required(tier)));
            }
            blit(SIM, l + 14, t + 48, 18, 141, 7, Math.clamp(missingData, 0, 87));
            blit(PLAYER, l + 28, t + 145, 0, 0, 176, 90);
        } else if (fabrication()) {
            blit(FAB, l, t, 0, 0, 176, 83);
            if (!configuring) {
                int energy = (int) (53L * machine.energy.getEnergyStored() / Math.max(1, machine.energy.getMaxEnergyStored()));
                blit(FAB, l + 6, t + 63 - energy, 0, 83, 7, energy);
                int progress = ((LootFabTile) machine).progress * 35 / 60; blit(FAB, l + 84, t + 58 - progress, 7, 83, 6, progress);
                blit(PLAYER, l, t + 88, 0, 0, 176, 90); icon(FAB, 31, 83, l - 22, t, redstoneIcon());
            } else blit(DC, l - 22, t, 36, 138, 18, 18);
            drawFabrication(l, t, mx - l, my - t);
        } else if (machine instanceof DataCenterTile dc) {
            blit(DC, l, t, 0, 0, 230, 128);
            int energy = (int) (100L * machine.energy.getEnergyStored() / Math.max(1, machine.energy.getMaxEnergyStored()));
            blit(DC, l + 213, t + 119 - energy, 231, 0, 7, energy);
            for (int i = 0; i < 25; i++) {
                if (dc.failures[i].equals("model")) continue;
                int w = dc.failures[i].equals("none") ? (300 - dc.progress[i]) * 16 / 300 : 16;
                blit(DC, l + 9 + i % 5 * 18, t + 37 + i / 5 * 20, 0, dc.failures[i].equals("none") ? 129 : 131, Math.clamp(w, 0, 16), 2);
            }
            icon(DC, 18, 138, l - 22, t, redstoneIcon()); blit(PLAYER, l + 27, t + 132, 0, 0, 176, 90);
        }
        GlStateManager.color(1, 1, 1, 1);
    }
    private String redstoneIcon() { return machine.redstone == 0 ? "items/redstone_dust" : machine.redstone == 1 ? "blocks/redstone_torch_off" : "blocks/redstone_torch_on"; }
    private void icon(ResourceLocation resource, int u, int v, int x, int y, String glyph) {
        blit(resource, x, y, u, v, 18, 18);
        mc.getTextureManager().bindTexture(new ResourceLocation("minecraft", "textures/" + glyph + ".png"));
        drawModalRectWithCustomSizedTexture(x + 1, y + 1, 0, 0, 16, 16, 16, 16);
    }
    private void drawFabrication(int l, int t, int mx, int my) {
        DataModel model = selected(); if (model == null) return;
        page = Math.clamp(page, 0, Math.max(0, (model.drops.size() - 1) / 9)); Selections.Entry state = entry(model);
        int selection = ((ProductionTile) machine).selections.current(model);
        for (int i = 0; i < 9 && page * 9 + i < model.drops.size(); i++) {
            int x = 17 + i % 3 * 19, y = 9 + i / 3 * 19;
            if (!state.queue && selection == page * 9 + i || over(mx, my, x, y, 16, 16)) blit(FAB, l + x - 1, t + y - 1, !state.queue && selection == page * 9 + i ? 31 : 13, 83, 18, 18);
            item(model.drops.get(page * 9 + i), l + x, t + y);
        }
        if (selection >= 0) item(model.drops.get(selection), l + 79, t + 5);
        icon(FAB, 31, 83, l + 180, t, state.queue ? "items/knowledge_book" : "items/item_frame");
        if (!state.queue) return;
        icon(FAB, 31, 83, l + 200, t, "items/barrier");
        int visible = state.drops.size() > 9 ? 8 : state.drops.size();
        for (int i = 0; i < 9; i++) {
            int x = l + 180 + i % 3 * 18, y = t + 30 + i / 3 * 18;
            blit(FAB, x, y, i < visible && i == state.cursor ? 31 : 13, 83, 18, 18);
            if (i < visible && state.drops.get(i) < model.drops.size()) item(model.drops.get(state.drops.get(i)), x + 1, y + 1);
            else if (i == 8 && state.drops.size() > 9) text("+" + (state.drops.size() - 8), x + 2, y + 5, 16, WHITE);
        }
    }
    @Override protected void drawGuiContainerForegroundLayer(int mx, int my) {
        if (configuring) {
            DataModel model = selected(); int x = left() - guiLeft, y = top() - guiTop;
            if (model != null) { text(model.name(), x + 8, y - 10, 168, AQUA); if (entry(model).queue) text(tr("hostilenetworks.gui.queue_current"), x + 180, y + 20, 80, AQUA); }
        } else if (machine instanceof SimChamberTile sim) {
            DataModel model = selected();
            if (model != null) {
                var stack = sim.inventory.getStackInSlot(0);
                text(tr("hostilenetworks.gui.target", "§a" + model.name()), 18, 9, 154, WHITE);
                text(tr("hostilenetworks.gui.tier", "§f" + tr("hostilenetworks.tier." + model.tier(ModelStack.data(stack)).id())), 18, 21, 154, WHITE);
                text(tr("hostilenetworks.gui.accuracy", String.format(Locale.ROOT, "%.1f%%", model.accuracy(ModelStack.data(stack)) * 100)), 18, 33, 154, WHITE);
            }
            if (sim.progress > 0) text(Math.min(99, (sim.duration() - sim.progress) * 100 / sim.duration()) + "%", 184, 123, 26, AQUA);
            if (!sim.failure.equals("none")) {
                String key = "hostilenetworks.fail." + sim.failure;
                String value = net.minecraft.client.resources.I18n.hasKey(key) ? tr(key, model == null ? "?" : model.inputs.getFirst().getDisplayName()) : tr("hnn.status." + sim.failure);
                wrapped(value, 29, 51, 174, WHITE, 7);
            } else {
                int budget = (int) ((sim.duration() - sim.progress) * .65 * 300 / sim.duration());
                for (int i = 0; i < 7; i++) {
                    String line = tr((sim.mode == 1 ? "hostilenetworks.run.train." : "hostilenetworks.run.") + i, ModelStack.iterations(sim.inventory.getStackInSlot(0)));
                    if (i == 0) line += "v6.5.1";
                    if (i == 5 && sim.mode == 0) line += tr("hostilenetworks.color_text." + (sim.successes > 0 ? "success" : "failed"));
                    text(line.substring(0, Math.clamp(budget, 0, line.length())), 29, 51 + i * 12, 174, WHITE); budget -= line.length();
                }
            }
        } else if (machine instanceof DataCenterTile) {
            text(tr("block.hostilenetworks.data_center"), 8, 6, 118, AQUA); text(tr("hostilenetworks.gui.data_center.io"), 130, 6, 92, AQUA);
        } else {
            DataModel model = selected(); if (model != null && entry(model).queue) text(tr("hostilenetworks.gui.queue_current"), 180, 20, 80, AQUA);
        }
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partial) {
        super.drawScreen(mouseX, mouseY, partial);
        int x = Math.round(mouseX / fit) - left(), y = Math.round(mouseY / fit) - top();
        List<String> lines = new ArrayList<>();
        if (!configuring) {
            int rx = machine instanceof SimChamberTile ? 228 : -22;
            if (over(x, y, rx, 0, 18, 18)) lines.add(tr("hnn.redstone." + machine.redstone));
            if (machine instanceof SimChamberTile sim) {
                if (over(x, y, 228, 20, 18, 18)) {
                    String key = "hostilenetworks.gui.mode." + (sim.mode == 0 ? "inference" : "training");
                    lines.add(tr(key)); lines.add(tr(key + ".desc")); lines.add(tr(key + ".desc2"));
                    if (sim.progress > 0) lines.add(tr("hnn.mode_busy"));
                }
                if (over(x, y, 211, 48, 7, 87)) { lines.add(tr("hostilenetworks.gui.energy", sim.energy.getEnergyStored(), sim.energy.getMaxEnergyStored())); if (selected() != null) lines.add(tr("hnn.model_cost", sim.cost(selected()))); }
                if (over(x, y, 14, 48, 7, 87) && selected() != null) lines.add(tr("hnn.model_data", ModelStack.data(sim.inventory.getStackInSlot(0)), selected().required(selected().next(selected().tier(ModelStack.data(sim.inventory.getStackInSlot(0)))))));
            } else if (machine instanceof DataCenterTile dc) {
                if (over(x, y, 213, 19, 7, 100)) lines.add(tr("hostilenetworks.gui.energy", dc.energy.getEnergyStored(), dc.energy.getMaxEnergyStored()));
                if (!dc.shellValid) lines.add(tr("hnn.shell_invalid"));
                for (int i = 0; i < 25; i++) if (over(x, y, 9 + i % 5 * 18, 21 + i / 5 * 20, 16, 16)) {
                    DataModel model = ModelStack.model(dc.inventory.getStackInSlot(i));
                    if (model != null) { lines.add(model.name()); lines.add(tr("hnn.status." + dc.failures[i])); lines.add(tr("hostilenetworks.gui.data_center.click_to_config")); }
                }
            } else if (over(x, y, 6, 10, 7, 53)) lines.add(tr("hostilenetworks.gui.energy", machine.energy.getEnergyStored(), machine.energy.getMaxEnergyStored()));
        } else if (over(x, y, -22, 0, 18, 18)) lines.add(tr("hostilenetworks.gui.data_center.back"));
        DataModel model = selected();
        if (fabrication() && model != null) {
            Selections.Entry state = entry(model);
            if (over(x, y, 180, 0, 18, 18)) lines.add(tr(state.queue ? "hnn.queue" : "hnn.fixed"));
            if (state.queue && over(x, y, 200, 0, 18, 18)) lines.add(tr("hnn.clear_queue"));
            for (int i = 0; i < 9; i++) if (page * 9 + i < model.drops.size() && over(x, y, 17 + i % 3 * 19, 9 + i / 3 * 19, 16, 16)) {
                lines.add(model.drops.get(page * 9 + i).getDisplayName()); if (state.queue) lines.add(tr("hostilenetworks.gui.queue_add"));
            }
            if (over(x, y, 79, 5, 16, 16) && ((ProductionTile) machine).selections.current(model) >= 0) lines.add(tr(state.queue ? "hostilenetworks.gui.queue_clear_current" : "hostilenetworks.gui.clear"));
            int visible = state.drops.size() > 9 ? 8 : state.drops.size();
            if (state.queue) for (int i = 0; i < visible; i++) if (over(x, y, 180 + i % 3 * 18, 30 + i / 3 * 18, 18, 18) && state.drops.get(i) < model.drops.size()) {
                lines.add(model.drops.get(state.drops.get(i)).getDisplayName()); lines.add(tr("hostilenetworks.gui.queue_remove"));
            }
        }
        if (!lines.isEmpty()) tip(lines, mouseX, mouseY);
    }
}
