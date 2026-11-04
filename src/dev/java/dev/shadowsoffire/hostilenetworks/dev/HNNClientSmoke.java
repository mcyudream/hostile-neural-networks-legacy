package dev.shadowsoffire.hostilenetworks.dev;

import com.google.gson.*;
import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.client.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.Language;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Independent dev-only mod, never included in the distributed HNN jar. */
@Mod(modid = "hnn_verification", name = "HNN verification", version = "1", clientSideOnly = true,
    dependencies = "required-after:hostile-neural-networks-legacy", acceptableRemoteVersions = "*")
public final class HNNClientSmoke {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final JsonArray checks = new JsonArray();
    private MachineChecks machines;
    private Path output;
    private int phase, ticks, next, exit = Integer.MAX_VALUE;
    private long trainingStart, inferenceStart, fabStart;
    private String capture;
    private CompletableFuture<Void> pending;
    private Language originalLanguage;
    private java.util.List<String> integrationPreviews;
    private int integrationPreview;
    private net.minecraft.item.ItemStack originalLearner;
    private void check(boolean valid, String name) { if (!valid) throw new IllegalStateException(name); synchronized (checks) { checks.add(name); } }
    @Mod.EventHandler public void init(FMLInitializationEvent e) throws Exception {
        if (!Boolean.getBoolean("hnn.dev.smoke")) return;
        output = mc.gameDir.toPath().resolve("hnn-smoke"); Files.createDirectories(output);
        mc.gameSettings.pauseOnLostFocus = false; mc.gameSettings.renderDistanceChunks = 3; mc.gameSettings.guiScale = 2;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        MinecraftForge.EVENT_BUS.register(this); report("STARTING", null);
    }
    private WorldServer world() { return mc.getIntegratedServer().getWorld(0); }
    private EntityPlayerMP player() { return mc.getIntegratedServer().getPlayerList().getPlayers().getFirst(); }
    private void server(Runnable work) {
        pending = new CompletableFuture<>();
        mc.getIntegratedServer().addScheduledTask(() -> { try { work.run(); pending.complete(null); } catch (Throwable error) { pending.completeExceptionally(error); } });
    }
    private boolean ready() { if (pending == null || !pending.isDone()) return false; pending.join(); return true; }
    private WorldSettings settings() { WorldSettings s = new WorldSettings(1729, GameType.CREATIVE, false, false, WorldType.FLAT); s.enableCommands(); return s; }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (++ticks >= exit) { mc.shutdown(); return; }
        if (ticks < next || capture != null || exit != Integer.MAX_VALUE) return;
        try {
            if (ticks > 4000) throw new IllegalStateException("Timeout in phase " + phase);
            switch (phase) {
                case 0 -> {
                    if (!(mc.currentScreen instanceof GuiMainMenu)) return;
                    check(Loader.isModLoaded("hostile-neural-networks-legacy") && Loader.isModLoaded("cleanroom"), "Cleanroom discovers HNN and the isolated verification mod");
                    String target = System.getProperty("hnn.dev.cleanroomTarget");
                    if (target != null) check(target.equals(com.cleanroommc.common.CleanroomVersion.getVersion()), "Runtime Cleanroom version equals the exact requested baseline " + target);
                    check(HostileNetworks.class.getResource("HostileNetworks.class").getProtocol().equals("jar"), "HNN runs from the packaged installation jar");
                    mc.launchIntegratedServer("hnn-smoke", "HNN verification", settings()); phase++;
                }
                case 1 -> {
                    if (mc.world == null || mc.player == null || mc.getIntegratedServer().getPlayerList().getPlayers().isEmpty()) return;
                    server(() -> { machines = new MachineChecks(world(), player(), this::check); machines.setup(); trainingStart = world().getTotalWorldTime(); }); phase++;
                }
                case 2 -> {
                    if (!ready()) return;
                    mc.displayGuiScreen(null); capture = "deep-learner-hud.png"; phase = 22;
                }
                case 22 -> {
                    server(() -> player().openGui(HostileNetworks.INSTANCE, 1, world(), 0, 0, 0)); phase = 3; next = ticks + 20;
                }
                case 3 -> {
                    if (!ready() || !(mc.currentScreen instanceof LearnerScreen)) return;
                    if (Loader.isModLoaded("jei")) JeiSmoke.verifyArea(this::check, "learner entity panel");
                    check(true, "Deep Learner opens through the real client/server GUI protocol"); capture = "deep-learner.png"; phase++;
                }
                case 4 -> {
                    mc.gameSettings.guiScale = 4; resize(); capture = "deep-learner-large-scale.png";
                    check(mc.currentScreen.width >= 338 && mc.currentScreen.height >= 235, "Responsive learner keeps the upstream layout inside the logical viewport at GUI scale four"); phase++;
                }
                case 5 -> {
                    originalLanguage = mc.getLanguageManager().getCurrentLanguage(); mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguage("zh_cn"));
                    mc.gameSettings.language = "zh_cn"; mc.refreshResources(); resize(); capture = "deep-learner-zh-cn.png"; phase++;
                }
                case 6 -> {
                    mc.getLanguageManager().setCurrentLanguage(originalLanguage); mc.gameSettings.language = originalLanguage.getLanguageCode(); mc.refreshResources();
                    mc.player.closeScreen(); mc.gameSettings.guiScale = 2;
                    server(() -> player().openGui(HostileNetworks.INSTANCE, 0, world(), 0, 4, 0)); phase++; next = ticks + 15;
                }
                case 7 -> {
                    if (!ready() || !(mc.currentScreen instanceof MachineScreen)) return;
                    if (Loader.isModLoaded("jei")) JeiSmoke.verifyArea(this::check, "simulation tabs");
                    capture = "simulation-training.png"; phase = 70;
                }
                case 70 -> {
                    server(() -> { player().setPositionAndUpdate(15, 5, 10); player().openGui(HostileNetworks.INSTANCE, 0, world(), 15, 4, 12); });
                    phase++; next = ticks + 10;
                }
                case 71 -> { if (!ready() || !(mc.currentScreen instanceof MachineScreen)) return; capture = "data-center.png"; phase = 710; }
                case 710 -> { clickUi(16, 28, 1); phase = 712; next = ticks + 5; }
                case 712 -> { capture = "data-center-config.png"; phase = 711; }
                case 711 -> {
                    check(((dev.shadowsoffire.hostilenetworks.gui.MachineContainer) mc.player.openContainer).configurationView, "Right-clicking a Data Center slot opens the original-style fabrication settings page");
                    var hidden = mc.player.openContainer.inventorySlots.stream().noneMatch(net.minecraft.inventory.Slot::isEnabled);
                    check(hidden, "Data Center settings hide all inventory slots and prevent invisible slot interaction");
                    if (Loader.isModLoaded("jei")) JeiSmoke.verifyArea(this::check, "Data Center fabrication settings");
                    clickUi(-13, 9, 0, true); phase = 72;
                }
                case 72 -> {
                    mc.player.closeScreen();
                    server(() -> {
                        player().connection.setPlayerLocation(15, 6, 6, 0, 0);
                        player().capabilities.isFlying = true; player().sendPlayerAbilities();
                    }); phase++; next = ticks + 10;
                }
                case 73 -> { if (!ready()) return; capture = "data-center-world.png"; phase++; }
                case 74 -> {
                    server(() -> { player().setPositionAndUpdate(1, 5, -4); player().openGui(HostileNetworks.INSTANCE, 0, world(), 0, 4, 0); }); phase = 8;
                }
                case 8 -> {
                    if (world().getTotalWorldTime() - trainingStart < 315) return;
                    server(() -> { machines.afterTraining(); inferenceStart = world().getTotalWorldTime(); }); phase++;
                }
                case 9 -> {
                    if (!ready()) return;
                    server(machines::fillDuringCycle); phase++; next = ticks + 5;
                }
                case 10 -> {
                    if (!ready()) return;
                    capture = "simulation-inference.png"; phase++;
                }
                case 11 -> {
                    if (world().getTotalWorldTime() - inferenceStart < 255) return;
                    server(() -> { machines.afterInference(); fabStart = world().getTotalWorldTime(); player().openGui(HostileNetworks.INSTANCE, 0, world(), 3, 4, 0); }); phase++; next = ticks + 20;
                }
                case 12 -> {
                    if (!ready()) return;
                    if (Loader.isModLoaded("jei")) JeiSmoke.verifyArea(this::check, "fabricator queue");
                    capture = "fabricator-queue.png"; phase = 120;
                }
                case 120 -> {
                    // The side-tab hitbox must survive a large GUI scale and send a real server packet.
                    mc.gameSettings.guiScale = 4; resize(); clickUi(-13, 9, 0); phase++; next = ticks + 8;
                }
                case 121 -> {
                    server(() -> check(machines.fab().redstone == 1, "Large-scale upstream redstone tab sends its action through the real network handler")); phase++; next = ticks + 5;
                }
                case 122 -> {
                    if (!ready()) return;
                    if (Loader.isModLoaded("jei")) JeiSmoke.verifyArea(this::check, "large-scale fabricator queue");
                    capture = "fabricator-large-scale.png"; clickUi(-13, 9, 0); phase++; next = ticks + 5;
                }
                case 123 -> { clickUi(-13, 9, 0); mc.gameSettings.guiScale = 2; resize(); phase = 13; }
                case 13 -> {
                    if (world().getTotalWorldTime() - fabStart < 180) return;
                    server(machines::afterFabrication); phase++;
                }
                case 14 -> {
                    if (!ready()) return;
                    mc.player.closeScreen();
                    if (Loader.isModLoaded("jei")) { JeiSmoke.verify(this::check); capture = "jei-simulation.png"; phase = 140; }
                    else phase = 141;
                }
                case 140 -> { JeiSmoke.fabrication(); capture = "jei-fabrication.png"; phase++; }
                case 141 -> {
                    if (Loader.isModLoaded("ponder-legacy")) { PonderSmoke.verify(this::check); phase = 142; next = ticks + 50; }
                    else { check(true, "Client completes core gameplay with Ponder absent"); phase = 170; }
                }
                case 142 -> { PonderSmoke.verifyPlayback(this::check, "workflow"); capture = "ponder-workflow.png"; phase = 15; }
                case 15 -> { PonderSmoke.center(); phase++; next = ticks + 140; }
                case 16 -> { PonderSmoke.verifyPlayback(this::check, "Data Center"); capture = "ponder-data-center.png"; phase = 160; }
                case 160 -> { mc.displayGuiScreen(null); phase = 170; }
                case 170 -> {
                    integrationPreviews = IntegrationChecks.loadedModels(); integrationPreview = 0;
                    if (integrationPreviews.isEmpty()) { phase = 17; break; }
                    originalLearner = player().inventory.getStackInSlot(0).copy();
                    previewIntegration(); phase = 171;
                }
                case 171 -> {
                    if (!ready()) return;
                    server(() -> player().openGui(HostileNetworks.INSTANCE, 1, world(), 0, 0, 0)); phase = 172; next = ticks + 20;
                }
                case 172 -> {
                    if (!ready() || !(mc.currentScreen instanceof LearnerScreen screen)) return;
                    var model = dev.shadowsoffire.hostilenetworks.data.ModelRegistry.get(integrationPreviews.get(integrationPreview));
                    var field = LearnerScreen.class.getDeclaredField("entity"); field.setAccessible(true);
                    var entity = (net.minecraft.entity.Entity) field.get(screen);
                    check(entity != null && net.minecraft.entity.EntityList.getKey(entity).equals(model.target), model.id + " renders its real optional-mod entity in the learner GUI");
                    capture = "integration-" + integrationPreviews.get(integrationPreview).replace('/', '-') + ".png"; phase = 173;
                }
                case 173 -> {
                    mc.player.closeScreen();
                    if (++integrationPreview < integrationPreviews.size()) { previewIntegration(); phase = 171; }
                    else { server(() -> player().inventory.setInventorySlotContents(0, originalLearner)); phase = 174; }
                }
                case 174 -> { if (ready()) phase = 17; }
                case 17 -> {
                    server(() -> {
                        simTag = machines.sim().writeToNBT(new net.minecraft.nbt.NBTTagCompound());
                        savedLearner = player().inventory.getStackInSlot(0).writeToNBT(new net.minecraft.nbt.NBTTagCompound());
                        try { world().saveAllChunks(true, null); } catch (net.minecraft.world.MinecraftException error) { throw new IllegalStateException(error); }
                    }); phase++;
                }
                case 18 -> {
                    if (!ready()) return;
                    mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); mc.displayGuiScreen(new GuiMainMenu()); phase++; next = ticks + 20;
                }
                case 19 -> { mc.launchIntegratedServer("hnn-smoke", "HNN verification", settings()); phase++; }
                case 20 -> {
                    if (mc.world == null || mc.player == null || mc.getIntegratedServer().getPlayerList().getPlayers().isEmpty()) return;
                    server(() -> {
                        var tile = (dev.shadowsoffire.hostilenetworks.tile.SimChamberTile) world().getTileEntity(MachineChecks.SIM);
                        check(tile.energy.getEnergyStored() == simTag.getInteger("energy") && tile.inventory.getStackInSlot(3).getCount() == 1, "Machine FE and prediction outputs survive a full world disconnect and reload");
                        check(player().inventory.getStackInSlot(0).writeToNBT(new net.minecraft.nbt.NBTTagCompound()).equals(savedLearner), "Deep Learner model data survives a real player save/reload");
                    }); phase++;
                }
                case 21 -> { if (!ready()) return; report("PASS", null); exit = ticks + 10; }
            }
        } catch (Throwable error) { error.printStackTrace(); try { report("FAIL", error); } catch (Exception ignored) {} exit = ticks + 5; }
    }
    private net.minecraft.nbt.NBTTagCompound simTag, savedLearner;
    private void previewIntegration() {
        server(() -> {
            var model = dev.shadowsoffire.hostilenetworks.data.ModelRegistry.get(integrationPreviews.get(integrationPreview));
            var learner = Hostile.stack("deep_learner");
            dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem.inventory(learner).setStackInSlot(0, machines.model(model.id.toString(), model.required(model.next(dev.shadowsoffire.hostilenetworks.data.ModelRegistry.tiers().getFirst()))));
            player().inventory.setInventorySlotContents(0, learner); player().inventoryContainer.detectAndSendChanges();
        });
    }
    private void clickUi(int x, int y, int button) throws Exception { clickUi(x, y, button, false); }
    private void clickUi(int x, int y, int button, boolean settings) throws Exception {
        var screen = mc.currentScreen;
        var fitField = ResponsiveContainer.class.getDeclaredField("fit"); fitField.setAccessible(true); float scale = fitField.getFloat(screen);
        var container = (net.minecraft.client.gui.inventory.GuiContainer) screen;
        int left = settings ? (screen.width - 282) / 2 + 22 : container.getGuiLeft();
        int top = settings ? (screen.height - 100) / 2 : container.getGuiTop();
        java.lang.reflect.Method click;
        try { click = GuiScreen.class.getDeclaredMethod("mouseClicked", int.class, int.class, int.class); }
        catch (NoSuchMethodException ignored) { click = GuiScreen.class.getDeclaredMethod("func_73864_a", int.class, int.class, int.class); }
        click.setAccessible(true); click.invoke(screen, Math.round((left + x) * scale), Math.round((top + y) * scale), button);
    }
    private void resize() { ScaledResolution resolution = new ScaledResolution(mc); mc.currentScreen.setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight()); }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END || capture == null) return;
        try { ScreenShotHelper.saveScreenshot(output.toFile(), capture, mc.displayWidth, mc.displayHeight, mc.getFramebuffer()); }
        catch (Throwable error) { error.printStackTrace(); try { report("FAIL", error); } catch (Exception ignored) {} exit = ticks + 5; }
        finally { capture = null; next = ticks + 5; }
    }
    private void report(String result, Throwable error) throws Exception {
        JsonObject report = new JsonObject(); report.addProperty("result", result); report.addProperty("phase", phase); report.addProperty("ticks", ticks);
        report.addProperty("ponder", Loader.isModLoaded("ponder-legacy")); report.addProperty("jei", Loader.isModLoaded("jei"));
        report.addProperty("cleanroom", com.cleanroommc.common.CleanroomVersion.getVersion());
        synchronized (checks) { report.add("checks", checks.deepCopy()); }
        if (error != null) { java.io.StringWriter text = new java.io.StringWriter(); error.printStackTrace(new java.io.PrintWriter(text)); report.addProperty("failure", text.toString()); }
        Files.writeString(output.resolve("report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
    }
}
