package dev.shadowsoffire.hostilenetworks;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraft.nbt.NBTTagCompound;

public final class HostileConfig {
    public static File directory;
    public static int simPowerCap, fabPowerCap, fabPowerCost, dataCenterPowerCap, simModelUpgrade;
    public static boolean rightClickToAttune, actionUpgradesModel, continuousAccuracy, enableBlockDataModels;
    public static float dataCenterCost;
    public static String[] floorBlocks, wallBlocks;
    public static void load(File configDir) {
        directory = new File(configDir, "hostilenetworks");
        directory.mkdirs();
        Configuration cfg = new Configuration(new File(configDir, "hostilenetworks.cfg"));
        cfg.load();
        simPowerCap = cfg.getInt("simPowerCap", "power", 2000000, 1, Integer.MAX_VALUE, "Simulation Chamber capacity (FE)");
        fabPowerCap = cfg.getInt("fabPowerCap", "power", 1000000, 1, Integer.MAX_VALUE, "Loot Fabricator capacity (FE)");
        fabPowerCost = cfg.getInt("fabPowerCost", "power", 256, 0, Integer.MAX_VALUE, "Fabrication FE/t");
        dataCenterPowerCap = cfg.getInt("dataCenterPowerCap", "power", 8000000, 1, Integer.MAX_VALUE, "Data Center capacity (FE)");
        dataCenterCost = cfg.getFloat("dataCenterCost", "power", 1.5f, 0.01f, 100f, "Data Center per-model simulation cost multiplier");
        rightClickToAttune = cfg.getBoolean("rightClickToAttune", "models", true, "Attune a framework by right clicking its target");
        actionUpgradesModel = cfg.getBoolean("actionUpgradesModel", "models", true, "Kills and mining grant model data");
        continuousAccuracy = cfg.getBoolean("continuousAccuracy", "models", true, "Interpolate accuracy between tiers");
        simModelUpgrade = cfg.getInt("simModelUpgrade", "models", 1, 0, 2, "Training: 0 disabled; 1 enabled; 2 stop at tier boundaries until a world action");
        enableBlockDataModels = cfg.getBoolean("enableBlockDataModels", "models", false, "Load experimental ore data models");
        floorBlocks = cfg.getStringList("floorBlocks", "data_center", new String[]{"minecraft:obsidian"}, "Valid floor blocks, optionally id@metadata");
        wallBlocks = cfg.getStringList("wallBlocks", "data_center", new String[]{"minecraft:stained_glass@15", "hostilenetworks:data_center_io_port"}, "Valid walls and ceiling");
        if (cfg.hasChanged()) cfg.save();
    }
    public static NBTTagCompound syncTag() {
        NBTTagCompound n = new NBTTagCompound();
        n.setInteger("sim", simPowerCap); n.setInteger("fab", fabPowerCap); n.setInteger("fabCost", fabPowerCost);
        n.setInteger("dc", dataCenterPowerCap); n.setFloat("dcCost", dataCenterCost); n.setInteger("training", simModelUpgrade);
        n.setBoolean("attune", rightClickToAttune); n.setBoolean("actions", actionUpgradesModel); n.setBoolean("accuracy", continuousAccuracy);
        return n;
    }
    public static void readSync(NBTTagCompound n) {
        simPowerCap = n.getInteger("sim"); fabPowerCap = n.getInteger("fab"); fabPowerCost = n.getInteger("fabCost");
        dataCenterPowerCap = n.getInteger("dc"); dataCenterCost = n.getFloat("dcCost"); simModelUpgrade = n.getInteger("training");
        rightClickToAttune = n.getBoolean("attune"); actionUpgradesModel = n.getBoolean("actions"); continuousAccuracy = n.getBoolean("accuracy");
    }
}
