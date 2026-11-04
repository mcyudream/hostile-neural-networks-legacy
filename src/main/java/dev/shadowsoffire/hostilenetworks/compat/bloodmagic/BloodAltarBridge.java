package dev.shadowsoffire.hostilenetworks.compat.bloodmagic;

import java.lang.reflect.Method;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

/** Only initialized after Blood Magic is loaded. Names are Blood Magic's public, unmapped API. */
public final class BloodAltarBridge {
    private static final Class<?> ALTAR;
    private static final Method BLOOD, CAPACITY, MULTIPLIER, FILL;
    static {
        try {
            ALTAR = Class.forName("WayofTime.bloodmagic.tile.TileAltar");
            BLOOD = ALTAR.getMethod("getCurrentBlood"); CAPACITY = ALTAR.getMethod("getCapacity");
            MULTIPLIER = ALTAR.getMethod("getSacrificeMultiplier");
            FILL = Class.forName("WayofTime.bloodmagic.altar.BloodAltar").getMethod("fillMainTank", int.class);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported Blood Magic altar API", e); }
    }
    public static boolean isAltar(TileEntity tile) { return ALTAR.isInstance(tile); }
    private static Number invoke(Method method, Object target, Object... args) {
        try { return (Number) method.invoke(target, args); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Blood Magic altar invocation failed", e); }
    }
    public static int stored(TileEntity tile) { return invoke(BLOOD, tile).intValue(); }
    public static int capacity(TileEntity tile) { return invoke(CAPACITY, tile).intValue(); }
    public static double multiplier(TileEntity tile) { return Math.max(1, 1D + invoke(MULTIPLIER, tile).doubleValue()); }
    public static int fill(TileEntity tile, int amount) {
        Object tank = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        if (tank == null) return 0;
        int accepted = invoke(FILL, tank, amount).intValue();
        if (accepted > 0) tile.markDirty();
        return accepted;
    }
}
