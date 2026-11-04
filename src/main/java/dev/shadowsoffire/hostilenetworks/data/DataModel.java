package dev.shadowsoffire.hostilenetworks.data;

import java.util.*;
import com.google.gson.*;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

/** An immutable definition. Runtime item data remains on its source ItemStack. */
public final class DataModel {
    public final ResourceLocation id, target;
    public final boolean blockModel;
    public final List<ResourceLocation> variants;
    public final JsonObject json;
    public final int simCost, color;
    public final List<ItemStack> inputs, drops;
    public final ItemStack baseDrop;
    public final boolean noSilkTouch;

    DataModel(ResourceLocation id, JsonObject json) {
        this.id = id; this.json = json.deepCopy();
        blockModel = json.has("block");
        JsonElement targetJson = json.get(blockModel ? "block" : "entity");
        target = new ResourceLocation(targetJson.isJsonObject() ? targetJson.getAsJsonObject().get("block").getAsString() : targetJson.getAsString());
        if (blockModel ? !Block.REGISTRY.containsKey(target) : !EntityList.isRegistered(target))
            throw new IllegalArgumentException("Missing target " + target);
        List<ResourceLocation> other = new ArrayList<>();
        if (json.has("variants")) for (JsonElement v : json.getAsJsonArray("variants")) {
            ResourceLocation key = new ResourceLocation(v.isJsonObject() ? v.getAsJsonObject().get("block").getAsString() : v.getAsString());
            if (blockModel ? Block.REGISTRY.containsKey(key) : EntityList.isRegistered(key)) other.add(key);
        }
        variants = List.copyOf(other);
        simCost = json.get("sim_cost").getAsInt();
        if (simCost < 0 || simCost > Integer.MAX_VALUE / 100) throw new IllegalArgumentException("Invalid sim_cost");
        color = ModelTier.color(json.get("name_color").getAsString());
        inputs = ModelRegistry.ingredient(json.get("input"));
        if (inputs.isEmpty()) throw new IllegalArgumentException("No available simulation input");
        baseDrop = ModelRegistry.stack(json.get("base_drop"));
        List<ItemStack> results = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("fabricator_drops")) {
            ItemStack stack = ModelRegistry.stack(e);
            if (!stack.isEmpty()) results.add(stack);
        }
        drops = List.copyOf(results);
        noSilkTouch = json.has("upgrade_conditions");
        if (noSilkTouch) for (JsonElement e : json.getAsJsonArray("upgrade_conditions")) {
            JsonObject condition = e.getAsJsonObject();
            if (!condition.has("condition") || !condition.get("condition").getAsString().equals("minecraft:reference")
                || !condition.get("name").getAsString().equals("hostilenetworks:not_silk_touch"))
                throw new IllegalArgumentException("Unsupported upgrade condition; use hostilenetworks:not_silk_touch");
        }
        if (json.has("attunement")) validatePredicate(json.getAsJsonObject("attunement").getAsJsonObject("predicate"));
    }
    private static void validatePredicate(JsonObject p) {
        if (p == null) return;
        for (String key : p.keySet()) if (!List.of("nbt", "state", "type", "flags").contains(key))
            throw new IllegalArgumentException("Unsupported attunement predicate: " + key);
    }
    public boolean matches(ResourceLocation key) { return target.equals(key) || variants.contains(key); }
    public boolean attunes(Entity entity) {
        if (entity instanceof MultiPartEntityPart part && part.parent instanceof Entity parent) entity = parent;
        if (blockModel || !matches(EntityList.getKey(entity))) return false;
        if (!json.has("attunement")) return true;
        JsonObject attune = json.getAsJsonObject("attunement");
        if (!attune.get("attunable").getAsBoolean()) return false;
        JsonObject predicate = attune.getAsJsonObject("predicate");
        if (predicate == null) return true;
        NBTTagCompound nbt = new NBTTagCompound(); entity.writeToNBT(nbt);
        if (!matchesNBT(predicate, nbt)) return false;
        if (predicate.has("type") && !predicate.get("type").getAsString().equals(EntityList.getKey(entity).toString())) return false;
        if (predicate.has("flags")) {
            JsonObject flags = predicate.getAsJsonObject("flags");
            for (String flag : flags.keySet()) {
                boolean actual = switch (flag) {
                    case "is_on_fire" -> entity.isBurning();
                    case "is_sneaking" -> entity.isSneaking();
                    case "is_sprinting" -> entity.isSprinting();
                    case "is_invisible" -> entity.isInvisible();
                    case "is_baby" -> entity instanceof net.minecraft.entity.EntityAgeable a && a.isChild();
                    default -> throw new IllegalArgumentException("Unsupported entity flag " + flag);
                };
                if (actual != flags.get(flag).getAsBoolean()) return false;
            }
        }
        return true;
    }
    public boolean attunes(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!blockModel || !matches(state.getBlock().getRegistryName())) return false;
        if (!json.has("attunement")) return true;
        JsonObject attune = json.getAsJsonObject("attunement");
        if (!attune.get("attunable").getAsBoolean()) return false;
        JsonObject p = attune.getAsJsonObject("predicate");
        if (p == null) return true;
        if (p.has("state")) for (String key : p.getAsJsonObject("state").keySet()) {
            var property = state.getBlock().getBlockState().getProperty(key);
            if (property == null || !state.getValue(property).toString().equals(p.getAsJsonObject("state").get(key).getAsString())) return false;
        }
        return !p.has("nbt") || world.getTileEntity(pos) != null && matchesNBT(p, world.getTileEntity(pos).writeToNBT(new NBTTagCompound()));
    }
    private static boolean matchesNBT(JsonObject predicate, NBTTagCompound actual) {
        if (!predicate.has("nbt")) return true;
        try { return NBTUtil.areNBTEquals(JsonToNBT.getTagFromJson(predicate.get("nbt").getAsString()), actual, true); }
        catch (NBTException e) { throw new IllegalArgumentException(e); }
    }
    public boolean accepts(ItemStack stack) {
        for (ItemStack input : inputs) if (net.minecraftforge.oredict.OreDictionary.itemMatches(input, stack, false)
            && (!input.hasTagCompound() || ItemStack.areItemStackTagsEqual(input, stack))) return true;
        return false;
    }
    public int required(ModelTier tier) { return override("required_data", tier.id(), tier.requiredData()); }
    public int gained(ModelTier tier) { return override(json.has("data_gained") ? "data_gained" : "data_per_kill", tier.id(), tier.dataGained()); }
    private int override(String key, String tier, int fallback) {
        JsonObject overrides = json.has(key) ? json.getAsJsonObject(key) : null;
        return overrides != null && overrides.has(tier) ? overrides.get(tier).getAsInt() : fallback;
    }
    public ModelTier tier(int data) {
        ModelTier result = ModelRegistry.tiers().getFirst();
        for (ModelTier t : ModelRegistry.tiers()) if (data >= required(t)) result = t;
        return result;
    }
    public ModelTier next(ModelTier tier) {
        List<ModelTier> tiers = ModelRegistry.tiers();
        return tiers.get(Math.min(tiers.size() - 1, Math.max(0, tiers.indexOf(tier)) + 1));
    }
    public float accuracy(int data) {
        ModelTier t = tier(data), next = next(t);
        if (!HostileConfig.continuousAccuracy || t == next) return t.accuracy();
        float progress = Math.clamp((data - required(t)) / (float) (required(next) - required(t)), 0f, 1f);
        return t.accuracy() + progress * (next.accuracy() - t.accuracy());
    }
    public int roll(int data, Random random) {
        float accuracy = accuracy(data);
        return (int) accuracy + (random.nextFloat() < accuracy % 1 ? 1 : 0);
    }
    public String name() {
        if (json.has("name")) {
            ITextComponent component = ITextComponent.Serializer.jsonToComponent(json.get("name").toString());
            if (component != null) return component.getUnformattedText();
        }
        if (blockModel) return Block.REGISTRY.getObject(target).getLocalizedName();
        String translation = EntityList.getTranslationName(target);
        return new net.minecraft.util.text.TextComponentTranslation("entity." + translation + ".name").getUnformattedText();
    }
}
