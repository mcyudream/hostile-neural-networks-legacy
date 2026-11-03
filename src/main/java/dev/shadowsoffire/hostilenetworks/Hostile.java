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

public final class Hostile {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final CreativeTabs TAB = new CreativeTabs("hostilenetworks") {
        @Override public ItemStack createIcon() { return new ItemStack(ITEMS.get("deep_learner")); }
    };
    public static Item item(String id) { return ITEMS.get(id); }
    public static ItemStack stack(String id) { return new ItemStack(item(id)); }
    @SubscribeEvent public static void blocks(RegistryEvent.Register<Block> event) {
        for (String name : List.of("sim_chamber", "loot_fabricator", "data_center", "data_center_io_port")) {
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
        for (String name : List.of("prediction_matrix", "overworld_prediction", "nether_prediction", "end_prediction", "twilight_prediction"))
            register(event, name, new Item());
    }
    private static void register(RegistryEvent.Register<Item> event, String name, Item item) {
        item.setRegistryName(HostileNetworks.id(name)).setTranslationKey("hostilenetworks." + name).setCreativeTab(TAB);
        ITEMS.put(name, item); event.getRegistry().register(item);
    }
}
