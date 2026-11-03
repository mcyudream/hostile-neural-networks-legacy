package dev.shadowsoffire.hostilenetworks.dev;

import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Dedicated-only verification, loaded from the separate development harness jar. */
@Mod(modid = "hnn_server_verification", name = "HNN dedicated verification", version = "1",
    serverSideOnly = true, dependencies = "required-after:hostile-neural-networks-legacy", acceptableRemoteVersions = "*")
public final class ServerSmoke {
    private final JsonArray checks = new JsonArray();
    private MinecraftServer server;
    private WorldServer world;
    private MachineChecks machines;
    private Path output;
    private int phase, ticks;
    private long started;
    private void check(boolean valid, String description) {
        if (!valid) throw new IllegalStateException(description);
        checks.add(description);
    }
    @Mod.EventHandler public void started(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("hnn.dev.serverSmoke") || !FMLCommonHandler.instance().getSide().isServer()) return;
        server = FMLCommonHandler.instance().getMinecraftServerInstance(); world = server.getWorld(0);
        output = server.getDataDirectory().toPath().resolve("hnn-smoke"); Files.createDirectories(output);
        MinecraftForge.EVENT_BUS.register(this); report("STARTING", null);
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            if (++ticks > 1400) throw new IllegalStateException("Dedicated verification timeout in phase " + phase);
            switch (phase) {
                case 0 -> {
                    check(!world.isRemote && FMLCommonHandler.instance().getSide().isServer(), "HNN initializes on a real dedicated server with no client classes");
                    String target = System.getProperty("hnn.dev.cleanroomTarget");
                    if (target != null) check(target.equals(com.cleanroommc.common.CleanroomVersion.getVersion()), "Runtime Cleanroom version equals the exact requested baseline " + target);
                    world.setSpawnPoint(new net.minecraft.util.math.BlockPos(0, 4, 0));
                    machines = new MachineChecks(world, FakePlayerFactory.getMinecraft(world), this::check);
                    machines.setup(); started = world.getTotalWorldTime(); phase++;
                }
                case 1 -> {
                    if (world.getTotalWorldTime() - started < 315) return;
                    machines.afterTraining(); started = world.getTotalWorldTime(); phase++;
                }
                case 2 -> { machines.fillDuringCycle(); phase++; }
                case 3 -> {
                    if (world.getTotalWorldTime() - started < 255) return;
                    machines.afterInference(); started = world.getTotalWorldTime(); phase++;
                }
                case 4 -> {
                    if (world.getTotalWorldTime() - started < 135) return;
                    machines.afterFabrication();
                    world.saveAllChunks(true, null);
                    check(true, "Dedicated server completes real world ticks and saves the test world");
                    report("PASS", null); MinecraftForge.EVENT_BUS.unregister(this); server.initiateShutdown();
                }
            }
        } catch (Throwable error) {
            error.printStackTrace();
            try { report("FAIL", error); } catch (Exception writeError) { writeError.printStackTrace(); }
            MinecraftForge.EVENT_BUS.unregister(this); server.initiateShutdown();
        }
    }
    private void report(String result, Throwable error) throws Exception {
        JsonObject report = new JsonObject(); report.addProperty("result", result); report.addProperty("phase", phase); report.addProperty("ticks", ticks);
        report.addProperty("ponder", Loader.isModLoaded("ponder-legacy")); report.addProperty("dedicated", true); report.add("checks", checks.deepCopy());
        report.addProperty("cleanroom", com.cleanroommc.common.CleanroomVersion.getVersion());
        if (error != null) {
            java.io.StringWriter trace = new java.io.StringWriter(); error.printStackTrace(new java.io.PrintWriter(trace)); report.addProperty("failure", trace.toString());
        }
        Files.writeString(output.resolve("report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
    }
}
