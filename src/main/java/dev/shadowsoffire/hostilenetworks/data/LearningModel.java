package dev.shadowsoffire.hostilenetworks.data;

import dev.shadowsoffire.hostilenetworks.*;
import net.minecraft.item.ItemStack;

/** Shared presentation of normal and four-model training progress. */
public final class LearningModel {
    public static boolean valid(ItemStack s) { return s.getItem() == Hostile.item("data_model") && ModelStack.model(s) != null || !PerfectModel.models(s).isEmpty(); }
    public static boolean perfect(ItemStack s) { return !PerfectModel.models(s).isEmpty(); }
    public static String tierKey(ItemStack s) { return perfect(s) ? "hnn.extra.tier." + PerfectModel.TIERS[PerfectModel.tier(s)] : "hostilenetworks.tier." + ModelStack.model(s).tier(ModelStack.data(s)).id(); }
    public static boolean complete(ItemStack s) { return perfect(s) ? PerfectModel.tier(s) == 4 : ModelStack.model(s).tier(ModelStack.data(s)) == ModelRegistry.tiers().getLast(); }
    public static int required(ItemStack s, boolean next) {
        if (perfect(s)) return ExpansionConfig.required[Math.min(4, PerfectModel.tier(s) + (next ? 1 : 0))];
        DataModel m = ModelStack.model(s); ModelTier t = m.tier(ModelStack.data(s)); return m.required(next ? m.next(t) : t);
    }
    public static int gained(ItemStack s) { return perfect(s) ? ExpansionConfig.gained[PerfectModel.tier(s)] : ModelStack.model(s).gained(ModelStack.model(s).tier(ModelStack.data(s))); }
    public static float accuracy(ItemStack s) { return perfect(s) ? PerfectModel.accuracy(s) : ModelStack.model(s).accuracy(ModelStack.data(s)); }
    public static double fraction(ItemStack s) { return complete(s) ? 1 : Math.clamp((ModelStack.data(s) - required(s, false)) / (double) Math.max(1, required(s, true) - required(s, false)), 0, 1); }
}
