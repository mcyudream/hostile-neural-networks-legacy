package dev.shadowsoffire.hostilenetworks.data;

import com.google.gson.JsonObject;
import net.minecraft.util.text.TextFormatting;

public record ModelTier(String id, int requiredData, int dataGained, float accuracy, boolean canSim, int color) {
    public static ModelTier parse(String id, JsonObject json) {
        int required = json.get("required_data").getAsInt();
        int gained = json.get(json.has("data_gained") ? "data_gained" : "data_per_kill").getAsInt();
        float accuracy = json.get("accuracy").getAsFloat();
        if (required < 0 || gained < 0 || !Float.isFinite(accuracy) || accuracy < 0 || accuracy > 64)
            throw new IllegalArgumentException("Invalid tier range: " + id);
        return new ModelTier(id, required, gained, accuracy, !json.has("can_sim") || json.get("can_sim").getAsBoolean(), color(json.get("color").getAsString()));
    }
    public static int color(String value) {
        if (value.equals("rainbow")) return 0x55FFFF;
        if (value.startsWith("#")) return Integer.parseInt(value.substring(1), 16);
        TextFormatting fmt = TextFormatting.getValueByName(value);
        if (fmt == null || !fmt.isColor()) throw new IllegalArgumentException("Unknown color: " + value);
        int index = fmt.getColorIndex();
        int[] rgb = {0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,
                     0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF};
        return rgb[index];
    }
}
