package dev.shadowsoffire.hostilenetworks.data;

import dev.shadowsoffire.hostilenetworks.Hostile;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class ModelStack {
    public static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }
    public static DataModel model(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound() || stack.getItem() != Hostile.item("data_model") && stack.getItem() != Hostile.item("prediction")) return null;
        return ModelRegistry.get(stack.getTagCompound().getString("model"));
    }
    public static int data(ItemStack stack) { return stack.hasTagCompound() ? Math.max(0, stack.getTagCompound().getInteger("data")) : 0; }
    public static void setData(ItemStack stack, int data) { tag(stack).setInteger("data", Math.max(0, data)); }
    public static int iterations(ItemStack stack) { return stack.hasTagCompound() ? Math.max(0, stack.getTagCompound().getInteger("iterations")) : 0; }
    public static void iteration(ItemStack stack) { tag(stack).setInteger("iterations", iterations(stack) == Integer.MAX_VALUE ? Integer.MAX_VALUE : iterations(stack) + 1); }
    public static ItemStack create(DataModel model, boolean prediction) {
        ItemStack stack = Hostile.stack(prediction ? "prediction" : "data_model");
        tag(stack).setString("model", model.id.toString()); return stack;
    }
    public static boolean atBoundary(ItemStack stack) { return stack.hasTagCompound() && stack.getTagCompound().getBoolean("awaiting_action"); }
    public static void gain(ItemStack stack, int amount, boolean action) {
        setData(stack, (int) Math.min(Integer.MAX_VALUE, (long) data(stack) + Math.max(0, amount)));
        if (action) tag(stack).removeTag("awaiting_action");
    }
}
