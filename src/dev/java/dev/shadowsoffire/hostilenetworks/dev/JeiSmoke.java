package dev.shadowsoffire.hostilenetworks.dev;

import dev.shadowsoffire.hostilenetworks.data.ModelRegistry;
import java.util.List;
import java.util.function.BiConsumer;
import mezz.jei.api.*;

/** Optional JEI test plugin, never shipped with the mod. */
@JEIPlugin
public final class JeiSmoke implements IModPlugin {
    private static IJeiRuntime runtime;
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    public static void verify(BiConsumer<Boolean, String> check) {
        check.accept(runtime != null, "JEI initializes its optional HNN plugin in the client");
        IRecipeRegistry registry = runtime.getRecipeRegistry();
        var sim = registry.getRecipeCategory("hostilenetworks.simulation");
        var fab = registry.getRecipeCategory("hostilenetworks.fabrication");
        check.accept(sim != null && fab != null, "JEI discovers both simulation and fabrication categories");
        check.accept(registry.getRecipeWrappers(sim).size() == ModelRegistry.models().size(), "JEI simulation recipes cover every loaded model");
        check.accept(registry.getRecipeWrappers(fab).size() == ModelRegistry.models().stream().mapToInt(model -> model.drops.size()).sum(), "JEI fabrication recipes cover every selectable drop");
        runtime.getRecipesGui().showCategories(List.of("hostilenetworks.simulation"));
    }
    public static void verifyArea(BiConsumer<Boolean, String> check, String name) throws ReflectiveOperationException {
        var screen = (dev.shadowsoffire.hostilenetworks.client.ResponsiveContainer) net.minecraft.client.Minecraft.getMinecraft().currentScreen;
        var overlay = runtime.getIngredientListOverlay();
        var field = overlay.getClass().getDeclaredField("displayArea"); field.setAccessible(true);
        var area = (java.awt.Rectangle) field.get(overlay);
        if (area == null || area.intersects(screen.getVisibleArea())) throw new IllegalStateException("JEI overlap for " + name + ": list=" + area + ", HNN=" + screen.getVisibleArea());
        check.accept(true, "JEI item list leaves the " + name + " unobstructed");
    }
    public static void fabrication() { runtime.getRecipesGui().showCategories(List.of("hostilenetworks.fabrication")); }
}
