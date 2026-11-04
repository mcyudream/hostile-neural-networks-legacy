package dev.shadowsoffire.hostilenetworks.item;

import dev.shadowsoffire.hostilenetworks.data.*;
import java.util.*;
import net.minecraft.item.*;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.translation.I18n;

public final class PerfectModelItem extends Item {
    public PerfectModelItem() { setMaxStackSize(1); }
    @Override public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
        List<DataModel> models = PerfectModel.models(stack);
        if (models.isEmpty()) { lines.add(I18n.translateToLocal("hnn.invalid_model")); return; }
        lines.add(I18n.translateToLocal("hnn.extra.tier." + PerfectModel.TIERS[PerfectModel.tier(stack)]));
        for (DataModel model : models) lines.add("§b" + model.name());
        lines.add(I18n.translateToLocalFormatted("hostilenetworks.gui.accuracy", String.format(Locale.ROOT, "%.1f%%", PerfectModel.accuracy(stack) * 100)));
        lines.add(I18n.translateToLocalFormatted("hnn.model_cost", PerfectModel.cost(stack)));
        lines.add(I18n.translateToLocalFormatted("hnn.model_data", ModelStack.data(stack), dev.shadowsoffire.hostilenetworks.ExpansionConfig.required[Math.min(4, PerfectModel.tier(stack) + 1)]));
    }
}
