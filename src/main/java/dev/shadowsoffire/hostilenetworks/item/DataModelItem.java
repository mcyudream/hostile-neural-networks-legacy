package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;
import dev.shadowsoffire.hostilenetworks.data.*;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.*;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

public final class DataModelItem extends Item {
    public final boolean prediction;
    public DataModelItem(boolean prediction) { this.prediction = prediction; if (!prediction) setMaxStackSize(1); }
    @Override public String getItemStackDisplayName(ItemStack stack) {
        DataModel model = ModelStack.model(stack);
        return I18n.translateToLocalFormatted("item.hostilenetworks." + (prediction ? "prediction" : "data_model") + ".name", model == null ? "?" : model.name());
    }
    @Override public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stacks) {
        if (!isInCreativeTab(tab)) return;
        for (DataModel model : ModelRegistry.models()) stacks.add(ModelStack.create(model, prediction));
    }
    @Override public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        DataModel model = ModelStack.model(stack);
        if (model == null) { list.add(I18n.translateToLocal("hnn.invalid_model")); return; }
        if (prediction) { list.add(I18n.translateToLocal("hnn.info.prediction")); return; }
        ModelTier tier = model.tier(ModelStack.data(stack));
        list.add(I18n.translateToLocalFormatted("hostilenetworks.info.tier", I18n.translateToLocal("hostilenetworks.tier." + tier.id())));
        list.add(I18n.translateToLocalFormatted("hnn.model_data", ModelStack.data(stack), model.required(model.next(tier))));
        list.add(I18n.translateToLocalFormatted("hostilenetworks.gui.accuracy", String.format(java.util.Locale.ROOT, "%.1f%%", model.accuracy(ModelStack.data(stack)) * 100)));
        list.add(I18n.translateToLocalFormatted("hnn.model_cost", model.simCost));
        list.add(I18n.translateToLocalFormatted("hnn.data_per_action", model.gained(tier)));
        if (flag.isAdvanced()) list.add(model.id.toString());
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean changed) { return changed || oldStack.getItem() != newStack.getItem(); }
}
