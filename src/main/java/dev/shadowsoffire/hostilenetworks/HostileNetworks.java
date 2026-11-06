package dev.shadowsoffire.hostilenetworks;

import dev.shadowsoffire.hostilenetworks.data.ModelRegistry;
import dev.shadowsoffire.hostilenetworks.network.Network;
import dev.shadowsoffire.hostilenetworks.tile.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = HostileNetworks.LEGACY_MOD_ID, name = "Hostile Neural Networks Legacy", version = "6.5.1-cleanroom.4",
    acceptedMinecraftVersions = "[1.12.2]", dependencies = "required-after:cleanroom@[0.4.0,);after:ponder-legacy;after:jei;after:draconicevolution;after:thermalfoundation;after:tconstruct;after:bloodmagic")
public final class HostileNetworks {
    /** Loader identity; resource and saved registry namespaces remain unchanged. */
    public static final String LEGACY_MOD_ID = "hostile-neural-networks-legacy";
    public static final String MODID = "hostilenetworks";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    @Mod.Instance(HostileNetworks.LEGACY_MOD_ID) public static HostileNetworks INSTANCE;
    @SidedProxy(clientSide = "dev.shadowsoffire.hostilenetworks.client.ClientProxy", serverSide = "dev.shadowsoffire.hostilenetworks.CommonProxy")
    public static CommonProxy proxy;
    public static ResourceLocation id(String name) { return new ResourceLocation(MODID, name); }

    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        HostileConfig.load(event.getModConfigurationDirectory());
        MinecraftForge.EVENT_BUS.register(Hostile.class);
        MinecraftForge.EVENT_BUS.register(HostileEvents.class);
        GameRegistry.registerTileEntity(SimChamberTile.class, id("sim_chamber"));
        GameRegistry.registerTileEntity(LootFabTile.class, id("loot_fabricator"));
        GameRegistry.registerTileEntity(DataCenterTile.class, id("data_center"));
        GameRegistry.registerTileEntity(IOPortTile.class, id("data_center_io_port"));
        GameRegistry.registerTileEntity(MergerTile.class, id("merger_camera"));
        GameRegistry.registerTileEntity(ModelTrainerTile.class, id("simulator_modeling"));
        GameRegistry.registerTileEntity(UltimateSimTile.class, id("ultimate_sim_chamber"));
        GameRegistry.registerTileEntity(UltimateFabTile.class, id("ultimate_loot_fabricator"));
        if (net.minecraftforge.fml.common.Loader.isModLoaded("bloodmagic")) GameRegistry.registerTileEntity(DigitalAgonizerTile.class, id("digital_agonizer"));
        Network.register();
        proxy.preInit();
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
        proxy.init();
    }
    @Mod.EventHandler public void postInit(FMLPostInitializationEvent event) { ModelRegistry.load(); }
    @Mod.EventHandler public void serverStarting(FMLServerStartingEvent event) {
        ModelRegistry.load();
        event.registerServerCommand(new HostileCommand());
    }
}
