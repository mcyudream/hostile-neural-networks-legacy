package dev.shadowsoffire.hostilenetworks;

import net.minecraftforge.common.config.Configuration;
import net.minecraft.nbt.NBTTagCompound;

/** Extra HNN 2.2.5 defaults, synchronized with the existing server configuration. */
public final class ExpansionConfig {
    public static int[] capacity = {10000000, 50000000, 100000000, 200000000};
    public static int[] simDuration = {250, 200, 150, 100}, fabDuration = {50, 40, 30, 20}, fabCost = {4096, 16384, 65536, 262144};
    public static final int[] MULTIPLIER = {1, 4, 8, 16};
    public static int[] required = {0, 1500, 4500, 10750, 22375}, gained = {20, 30, 50, 75, 0};
    public static int mergerCapacity = 100000000, mergerCost = 25000, mergerDuration = 500;
    public static int trainerCapacity = 100000000, trainerCost = 100000, trainerDuration = 100;
    public static int speedPercent = 50, tierCost = 10000000, dataMultiplier = 2;
    public static float speedEnergy = 2;
    public static String minimumNormal = "faulty", minimumExtra = "autonomous";
    public static String[] trainingBlacklist = {}, attuneBlacklist = {};
    public static int agonizerCapacity = 100000, agonizerCost = 128, agonizerDuration = 60, linkRange = 32;
    public static int[] essence = {0, 50, 75, 150, 300};
    public static float[] catalyst = {2.2f, 2.4f, 2.7f, 2.5f};
    public static void load(Configuration cfg) {
        for (int i = 0; i < 4; i++) {
            String c = "extra.v" + (i + 1);
            capacity[i] = cfg.getInt("capacity", c, capacity[i], 1, Integer.MAX_VALUE, "FE capacity");
            simDuration[i] = cfg.getInt("simulationTicks", c, simDuration[i], 1, 1000000, "Ultimate simulation duration");
            fabDuration[i] = cfg.getInt("fabricationTicks", c, fabDuration[i], 1, 1000000, "Ultimate fabrication duration");
            fabCost[i] = cfg.getInt("fabricationCost", c, fabCost[i], 0, Integer.MAX_VALUE, "FE/t per batch");
            required[i + 1] = cfg.getInt("tier" + (i + 1) + "Data", "extra.models", required[i + 1], required[i] + 1, Integer.MAX_VALUE - 4 + i, "Cumulative data for the next perfect-model tier");
            gained[i] = cfg.getInt("tier" + i + "Gain", "extra.models", gained[i], 1, 1000000, "Data gained per matching kill or inference");
            essence[i + 1] = cfg.getInt("tier" + (i + 1) + "Essence", "bloodmagic", essence[i + 1], 1, 1000000, "Base LP per agonizer cycle");
            catalyst[i] = cfg.getFloat("catalyst" + i, "bloodmagic", catalyst[i], 1, 10, "Overworld / Nether / End / Twilight catalyst multiplier (10 operations)");
        }
        mergerCapacity = cfg.getInt("capacity", "extra.merger", mergerCapacity, 1, Integer.MAX_VALUE, "FE capacity");
        mergerCost = cfg.getInt("cost", "extra.merger", mergerCost, 0, Integer.MAX_VALUE, "FE/t");
        mergerDuration = cfg.getInt("duration", "extra.merger", mergerDuration, 1, 1000000, "Ticks per merge");
        trainerCapacity = cfg.getInt("capacity", "extra.trainer", trainerCapacity, 1, Integer.MAX_VALUE, "FE capacity");
        trainerCost = cfg.getInt("cost", "extra.trainer", trainerCost, 0, Integer.MAX_VALUE, "FE/t");
        trainerDuration = cfg.getInt("duration", "extra.trainer", trainerDuration, 1, 1000000, "Ticks per training cycle");
        speedPercent = cfg.getInt("speedPercent", "extra.upgrades", speedPercent, 1, 99, "Duration reduction with speed upgrade");
        speedEnergy = cfg.getFloat("speedEnergy", "extra.upgrades", speedEnergy, 0, 100, "Speed upgrade FE multiplier");
        tierCost = cfg.getInt("tierCost", "extra.upgrades", tierCost, 0, Integer.MAX_VALUE, "Tier upgrade FE/t");
        dataMultiplier = cfg.getInt("dataMultiplier", "extra.upgrades", dataMultiplier, 1, 1000000, "Data upgrade multiplier");
        minimumNormal = cfg.getString("minimumNormal", "extra.trainer", minimumNormal, "Minimum normal model tier ID").toLowerCase(java.util.Locale.ROOT);
        minimumExtra = cfg.getString("minimumPerfect", "extra.trainer", minimumExtra, "Minimum perfect model tier ID").toLowerCase(java.util.Locale.ROOT);
        trainingBlacklist = cfg.getStringList("blacklist", "extra.trainer", trainingBlacklist, "Model IDs excluded from training");
        attuneBlacklist = cfg.getStringList("attuneBlacklist", "extra.models", attuneBlacklist, "Entity registry IDs excluded from framework attunement");
        agonizerCapacity = cfg.getInt("capacity", "bloodmagic", agonizerCapacity, 1, Integer.MAX_VALUE, "Digital agonizer FE capacity");
        agonizerCost = cfg.getInt("cost", "bloodmagic", agonizerCost, 0, Integer.MAX_VALUE, "Agonizer FE/t");
        agonizerDuration = cfg.getInt("duration", "bloodmagic", agonizerDuration, 1, 1000000, "Ticks per LP operation");
        linkRange = cfg.getInt("linkRange", "bloodmagic", linkRange, 1, 256, "Maximum same-dimension altar link distance");
    }
    public static NBTTagCompound write() {
        NBTTagCompound n = new NBTTagCompound();
        n.setIntArray("capacity", capacity); n.setIntArray("simDuration", simDuration); n.setIntArray("fabDuration", fabDuration); n.setIntArray("fabCost", fabCost);
        n.setIntArray("required", required); n.setIntArray("gained", gained); n.setIntArray("essence", essence);
        n.setInteger("mergerCapacity", mergerCapacity); n.setInteger("mergerCost", mergerCost); n.setInteger("mergerDuration", mergerDuration);
        n.setInteger("trainerCapacity", trainerCapacity); n.setInteger("trainerCost", trainerCost); n.setInteger("trainerDuration", trainerDuration);
        n.setInteger("speed", speedPercent); n.setFloat("speedEnergy", speedEnergy); n.setInteger("tierCost", tierCost); n.setInteger("dataMultiplier", dataMultiplier);
        n.setInteger("agonizerCapacity", agonizerCapacity); n.setInteger("agonizerCost", agonizerCost); n.setInteger("agonizerDuration", agonizerDuration); n.setInteger("linkRange", linkRange);
        for (int i = 0; i < 4; i++) n.setFloat("catalyst" + i, catalyst[i]);
        n.setString("minimumNormal", minimumNormal); n.setString("minimumExtra", minimumExtra);
        n.setTag("trainingBlacklist", strings(trainingBlacklist)); n.setTag("attuneBlacklist", strings(attuneBlacklist));
        return n;
    }
    private static net.minecraft.nbt.NBTTagList strings(String[] values) {
        var list = new net.minecraft.nbt.NBTTagList(); for (String value : values) list.appendTag(new net.minecraft.nbt.NBTTagString(value)); return list;
    }
    private static String[] strings(NBTTagCompound n, String key) {
        var list = n.getTagList(key, 8); String[] values = new String[list.tagCount()]; for (int i = 0; i < values.length; i++) values[i] = list.getStringTagAt(i); return values;
    }
    public static void read(NBTTagCompound n) {
        if (!n.hasKey("capacity")) return;
        capacity = n.getIntArray("capacity"); simDuration = n.getIntArray("simDuration"); fabDuration = n.getIntArray("fabDuration"); fabCost = n.getIntArray("fabCost");
        required = n.getIntArray("required"); gained = n.getIntArray("gained"); essence = n.getIntArray("essence");
        mergerCapacity = n.getInteger("mergerCapacity"); mergerCost = n.getInteger("mergerCost"); mergerDuration = n.getInteger("mergerDuration");
        trainerCapacity = n.getInteger("trainerCapacity"); trainerCost = n.getInteger("trainerCost"); trainerDuration = n.getInteger("trainerDuration");
        speedPercent = n.getInteger("speed"); speedEnergy = n.getFloat("speedEnergy"); tierCost = n.getInteger("tierCost"); dataMultiplier = n.getInteger("dataMultiplier");
        agonizerCapacity = n.getInteger("agonizerCapacity"); agonizerCost = n.getInteger("agonizerCost"); agonizerDuration = n.getInteger("agonizerDuration"); linkRange = n.getInteger("linkRange");
        for (int i = 0; i < 4; i++) catalyst[i] = n.getFloat("catalyst" + i);
        minimumNormal = n.getString("minimumNormal"); minimumExtra = n.getString("minimumExtra");
        trainingBlacklist = strings(n, "trainingBlacklist"); attuneBlacklist = strings(n, "attuneBlacklist");
    }
}
