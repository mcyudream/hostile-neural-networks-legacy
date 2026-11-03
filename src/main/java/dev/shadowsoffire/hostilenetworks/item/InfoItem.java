package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;
import net.minecraft.item.*;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.translation.I18n;

public class InfoItem extends Item {
    private final String info;
    public InfoItem(String info) { this.info = info; }
    @Override public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(I18n.translateToLocal("hnn.info." + info));
    }
}
