package dev.shadowsoffire.hostilenetworks;

import java.util.*;
import dev.shadowsoffire.hostilenetworks.block.MachineBlock;
import dev.shadowsoffire.hostilenetworks.block.IOPortBlock;
import dev.shadowsoffire.hostilenetworks.item.*;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.*;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

public final class Hostile {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final CreativeTabs TAB = new CreativeTabs("hostilenetworks") {
        @Override public ItemStack createIcon() { return new ItemStack(ITEMS.get("deep_learner")); }
    };
    public static Item item(String id) { return ITEMS.get(id); }
    public static ItemStack stack(String id) { return new ItemStack(item(id)); }
    @SubscribeEvent public static void blocks(RegistryEvent.Register<Block> event) {
        List<String> names = new ArrayList<>(List.of("sim_chamber", "loot_fabricator", "data_center", "data_center_io_port", "merger_camera", "simulator_modeling"));
        for (int v = 1; v <= 4; v++) { names.add("ultimate_sim_chamber_v" + v); names.add("ultimate_loot_fabricator_v" + v); }
        if (Loader.isModLoaded("bloodmagic")) names.add("digital_agonizer");
        for (String name : names) {
            Block block = name.equals("data_center_io_port") ? new IOPortBlock() : new MachineBlock(name);
            block.setRegistryName(HostileNetworks.id(name)).setTranslationKey("hostilenetworks." + name).setCreativeTab(TAB);
            BLOCKS.put(name, block); event.getRegistry().register(block);
        }
    }
    @SubscribeEvent public static void items(RegistryEvent.Register<Item> event) {
        for (Map.Entry<String, Block> e : BLOCKS.entrySet()) register(event, e.getKey(), new ItemBlock(e.getValue()));
        register(event, "deep_learner", new DeepLearnerItem());
        register(event, "data_model", new DataModelItem(false));
        register(event, "prediction", new DataModelItem(true));
        register(event, "blank_data_model", new InfoItem("framework"));
        register(event, "fab_directive", new FabDirectiveItem());
        register(event, "extra_data_model", new PerfectModelItem());
        register(event, "blank_extra_data_model", new InfoItem("perfect_framework"));
        register(event, "setting_card", new SettingCardItem());
        for (String id : List.of("upgrade_speed", "upgrade_module_stack", "upgrade_data_kill")) register(event, id, new InfoItem(id).setMaxStackSize(1));
        if (Loader.isModLoaded("bloodmagic")) register(event, "altar_linker", new AltarLinkerItem());
        for (String name : List.of("prediction_matrix", "overworld_prediction", "nether_prediction", "end_prediction", "twilight_prediction"))
            register(event, name, new Item());
    }
    @SubscribeEvent public static void recipes(RegistryEvent.Register<IRecipe> event) {
        if (!Loader.isModLoaded("bloodmagic")) return;
        Item slate = Item.REGISTRY.getObject(new ResourceLocation("bloodmagic", "slate"));
        GameRegistry.addShapedRecipe(HostileNetworks.id("digital_agonizer"), null, stack("digital_agonizer"),
            "SPS", "MCM", "SRS", 'S', new ItemStack(slate, 1, 2), 'P', stack("prediction_matrix"), 'M', stack("blank_data_model"), 'C', stack("sim_chamber"), 'R', Items.REDSTONE);
        GameRegistry.addShapedRecipe(HostileNetworks.id("altar_linker"), null, stack("altar_linker"),
            " S ", " R ", " G ", 'S', new ItemStack(slate, 1, 0), 'R', Items.REDSTONE, 'G', Items.GOLD_INGOT);
    }
    private static void register(RegistryEvent.Register<Item> event, String name, Item item) {
        item.setRegistryName(HostileNetworks.id(name)).setTranslationKey("hostilenetworks." + name).setCreativeTab(TAB);
        ITEMS.put(name, item); event.getRegistry().register(item);
    }
}
