package dev.shadowsoffire.hostilenetworks.compat.jei;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.client.*;
import java.awt.Rectangle;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.client.resources.I18n;

/** Optional, client-side JEI 4 adapter. Its classes are discovered only by JEI. */
@JEIPlugin
public final class HNNJei implements IModPlugin {
    private static final String SIM = "hostilenetworks.simulation", FAB = "hostilenetworks.fabrication";
    @Override public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new Category(registry.getJeiHelpers().getGuiHelper(), false), new Category(registry.getJeiHelpers().getGuiHelper(), true));
    }
    @Override public void register(IModRegistry registry) {
        // JEI's default GuiContainer bounds omit HNN's side tabs and use unscaled logical pixels.
        registry.addGuiScreenHandler(LearnerScreen.class, HNNJei::properties);
        registry.addGuiScreenHandler(MachineScreen.class, HNNJei::properties);
        List<Recipe> simulations = new ArrayList<>(), fabrications = new ArrayList<>();
        for (DataModel model : ModelRegistry.models()) {
            simulations.add(new Recipe(model, -1));
            for (int i = 0; i < model.drops.size(); i++) fabrications.add(new Recipe(model, i));
        }
        registry.addRecipes(simulations, SIM); registry.addRecipes(fabrications, FAB);
        registry.addRecipeCatalyst(Hostile.stack("sim_chamber"), SIM); registry.addRecipeCatalyst(Hostile.stack("loot_fabricator"), FAB);
        registry.addIngredientInfo(Hostile.stack("data_center"), ItemStack.class, "hnn.ponder.floor", "hnn.ponder.shell", "hnn.ponder.controller", "hnn.ponder.parallel");
        registry.addIngredientInfo(Hostile.stack("data_center_io_port"), ItemStack.class, "hnn.ponder.ports");
    }
    private static IGuiProperties properties(ResponsiveContainer screen) {
        Rectangle area = screen.getVisibleArea();
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return new IGuiProperties() {
            @Override public Class<? extends net.minecraft.client.gui.GuiScreen> getGuiClass() { return screen.getClass(); }
            @Override public int getGuiLeft() { return area.x; }
            @Override public int getGuiTop() { return area.y; }
            @Override public int getGuiXSize() { return area.width; }
            @Override public int getGuiYSize() { return area.height; }
            @Override public int getScreenWidth() { return resolution.getScaledWidth(); }
            @Override public int getScreenHeight() { return resolution.getScaledHeight(); }
        };
    }
    private record Recipe(DataModel model, int drop) implements IRecipeWrapper {
        boolean fabrication() { return drop >= 0; }
        @Override public void getIngredients(IIngredients ingredients) {
            if (fabrication()) { ingredients.setInput(ItemStack.class, ModelStack.create(model, true)); ingredients.setOutput(ItemStack.class, model.drops.get(drop)); }
            else {
                ItemStack stack = ModelStack.create(model, false); ModelStack.setData(stack, model.required(ModelRegistry.tiers().getLast()));
                ingredients.setInputLists(ItemStack.class, List.of(List.of(stack), model.inputs));
                List<ItemStack> outputs = new ArrayList<>(); if (!model.baseDrop.isEmpty()) outputs.add(model.baseDrop); outputs.add(ModelStack.create(model, true));
                ingredients.setOutputs(ItemStack.class, outputs);
            }
        }
        @Override public void drawInfo(Minecraft mc, int width, int height, int mx, int my) {
            mc.fontRenderer.drawString(I18n.format("hnn.model_cost", fabrication() ? HostileConfig.fabPowerCost : model.simCost), 4, 44, 0x555555);
            mc.fontRenderer.drawString(fabrication() ? "60 ticks" : "240 ticks / Inference", 4, 58, 0x555555);
            if (!fabrication()) mc.fontRenderer.drawString(I18n.format("hnn.jei_training"), 4, 72, 0x555555);
        }
    }
    private static final class Category implements IRecipeCategory<Recipe> {
        private final IDrawable background, icon;
        private final boolean fab;
        Category(IGuiHelper helper, boolean fab) {
            this.fab = fab; background = helper.createBlankDrawable(170, 88); icon = helper.createDrawableIngredient(Hostile.stack(fab ? "loot_fabricator" : "sim_chamber"));
        }
        @Override public String getUid() { return fab ? FAB : SIM; }
        @Override public String getTitle() { return I18n.format("block.hostilenetworks." + (fab ? "loot_fabricator" : "sim_chamber")); }
        @Override public String getModName() { return "Hostile Neural Networks"; }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public void setRecipe(IRecipeLayout layout, Recipe recipe, IIngredients ingredients) {
            IGuiItemStackGroup slots = layout.getItemStacks(); int i = 0;
            for (List<ItemStack> stacks : ingredients.getInputs(ItemStack.class)) { slots.init(i, true, 4 + i * 26, 12); slots.set(i++, stacks); }
            int out = 0;
            for (List<ItemStack> stacks : ingredients.getOutputs(ItemStack.class)) { slots.init(i, false, 106 + out++ * 26, 12); slots.set(i++, stacks); }
        }
    }
}
