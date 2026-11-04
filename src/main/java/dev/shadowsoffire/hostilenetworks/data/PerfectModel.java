package dev.shadowsoffire.hostilenetworks.data;

import dev.shadowsoffire.hostilenetworks.*;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;

/** Four ordered model IDs, including duplicates, with independent Extra HNN progress. */
public final class PerfectModel {
    public static final String[] TIERS = {"autonomous", "intelligent", "adaptive", "synthetic", "omnipotent"};
    private static final float[] ACCURACY = {.1f, .3f, .5f, .74f, 1f};
    public static List<DataModel> models(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Hostile.item("extra_data_model") || !stack.hasTagCompound()) return List.of();
        NBTTagList list = stack.getTagCompound().getTagList("models", 8);
        if (list.tagCount() != 4) return List.of();
        List<DataModel> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            DataModel model = ModelRegistry.get(list.getStringTagAt(i));
            if (model == null) return List.of();
            result.add(model);
        }
        return List.copyOf(result);
    }
    public static ItemStack create(List<DataModel> models) {
        if (models.size() != 4 || models.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Four registered models required");
        ItemStack stack = Hostile.stack("extra_data_model"); NBTTagList list = new NBTTagList();
        for (DataModel model : models) list.appendTag(new NBTTagString(model.id.toString()));
        ModelStack.tag(stack).setTag("models", list); return stack;
    }
    public static int tier(ItemStack stack) {
        for (int i = 4; i > 0; i--) if (ModelStack.data(stack) >= ExpansionConfig.required[i]) return i;
        return 0;
    }
    public static float accuracy(ItemStack stack) {
        int tier = tier(stack);
        if (!HostileConfig.continuousAccuracy || tier == 4) return ACCURACY[tier];
        return ACCURACY[tier] + (ACCURACY[tier + 1] - ACCURACY[tier]) * (ModelStack.data(stack) - ExpansionConfig.required[tier])
            / (ExpansionConfig.required[tier + 1] - ExpansionConfig.required[tier]);
    }
    public static String signature(ItemStack stack) {
        return models(stack).stream().map(m -> m.id.toString()).collect(java.util.stream.Collectors.joining(";"));
    }
    public static int cost(ItemStack stack) { return (int) Math.min(Integer.MAX_VALUE, models(stack).stream().mapToLong(m -> m.simCost).sum()); }
    public static void gain(ItemStack stack) { ModelStack.gain(stack, ExpansionConfig.gained[tier(stack)], true); }
}
