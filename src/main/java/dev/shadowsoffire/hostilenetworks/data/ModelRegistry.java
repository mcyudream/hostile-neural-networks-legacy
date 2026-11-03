package dev.shadowsoffire.hostilenetworks.data;

import com.google.gson.*;
import dev.shadowsoffire.hostilenetworks.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;

/** 1.12 replacement for the upstream datapack registries, including atomic reload and server synchronization. */
public final class ModelRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<ResourceLocation, DataModel> models = Map.of();
    private static List<ModelTier> tiers = List.of(new ModelTier("faulty", 0, 1, 0, false, 0x555555));
    private static JsonObject synchronizedData = new JsonObject();
    public static Collection<DataModel> models() { return models.values(); }
    public static List<ModelTier> tiers() { return tiers; }
    public static DataModel get(String id) {
        try { return models.get(id.indexOf(':') < 0 ? HostileNetworks.id(id) : new ResourceLocation(id)); } catch (RuntimeException e) { return null; }
    }
    private static JsonObject read(Reader reader) { return new JsonParser().parse(reader).getAsJsonObject(); }
    private static JsonObject asset(String name) {
        try (InputStream stream = ModelRegistry.class.getResourceAsStream("/assets/hostilenetworks/" + name)) {
            if (stream == null) throw new IllegalArgumentException("Missing built-in " + name);
            return read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public static synchronized void load() {
        JsonObject index = asset("data-index.json"), rawTiers = new JsonObject(), rawModels = new JsonObject();
        for (JsonElement e : index.getAsJsonArray("model_tiers")) {
            String file = e.getAsString(); rawTiers.add(file.substring(0, file.length() - 5), asset("model_tiers/" + file));
        }
        for (JsonElement e : index.getAsJsonArray("data_models")) {
            String file = e.getAsString(); rawModels.add("hostilenetworks:" + file.substring(0, file.length() - 5), asset("data_models/" + file));
        }
        Map<String, JsonObject> customTiers = external("model_tiers"), customModels = external("data_models");
        customTiers.forEach((id, json) -> {
            if (!id.startsWith("hostilenetworks:")) throw new IllegalArgumentException("Tiers must use the hostilenetworks namespace: " + id);
            rawTiers.add(id.substring(id.indexOf(':') + 1), json);
        });
        customModels.forEach(rawModels::add);
        List<ModelTier> loadedTiers = parseTiers(rawTiers);
        Map<ResourceLocation, DataModel> loaded = new TreeMap<>();
        JsonArray issues = new JsonArray(); JsonObject accepted = new JsonObject();
        for (Map.Entry<String, JsonElement> e : rawModels.entrySet()) {
            JsonObject json = e.getValue().getAsJsonObject();
            if (!conditions(json)) continue;
            try {
                DataModel model = new DataModel(new ResourceLocation(e.getKey()), json);
                validateOverrides(model, loadedTiers);
                loaded.put(model.id, model); accepted.add(e.getKey(), json);
            } catch (RuntimeException error) {
                if (customModels.containsKey(e.getKey())) throw new IllegalArgumentException("Invalid custom model " + e.getKey(), error);
                JsonObject issue = new JsonObject(); issue.addProperty("model", e.getKey()); issue.addProperty("reason", error.getMessage()); issues.add(issue);
                HostileNetworks.LOGGER.warn("Skipping built-in model {}: {}", e.getKey(), error.getMessage());
            }
        }
        JsonObject root = new JsonObject(); root.add("tiers", rawTiers); root.add("models", accepted);
        tiers = List.copyOf(loadedTiers); models = Collections.unmodifiableMap(loaded); synchronizedData = root;
        JsonObject report = new JsonObject(); report.addProperty("loaded", loaded.size()); report.add("skipped", issues);
        try { Files.writeString(HostileConfig.directory.toPath().resolve("model-report.json"), GSON.toJson(report), StandardCharsets.UTF_8); }
        catch (IOException error) { HostileNetworks.LOGGER.warn("Cannot write model report", error); }
        HostileNetworks.LOGGER.info("Loaded {} HNN data models and {} model tiers", loaded.size(), tiers.size());
    }
    public static JsonObject snapshot() { return synchronizedData.deepCopy(); }
    public static synchronized void receive(JsonObject root) {
        List<ModelTier> loadedTiers = parseTiers(root.getAsJsonObject("tiers"));
        Map<ResourceLocation, DataModel> loaded = new TreeMap<>();
        for (var e : root.getAsJsonObject("models").entrySet()) {
            DataModel model = new DataModel(new ResourceLocation(e.getKey()), e.getValue().getAsJsonObject());
            validateOverrides(model, loadedTiers); loaded.put(model.id, model);
        }
        models = Collections.unmodifiableMap(loaded); tiers = List.copyOf(loadedTiers);
    }
    private static List<ModelTier> parseTiers(JsonObject json) {
        List<ModelTier> result = new ArrayList<>();
        json.entrySet().forEach(e -> result.add(ModelTier.parse(e.getKey(), e.getValue().getAsJsonObject())));
        result.sort(Comparator.comparingInt(ModelTier::requiredData));
        if (result.isEmpty() || result.getFirst().requiredData() != 0 || result.stream().noneMatch(ModelTier::canSim))
            throw new IllegalArgumentException("Tiers require a zero-data tier and a simulatable tier");
        int last = -1;
        for (ModelTier tier : result) {
            if (tier.requiredData() <= last) throw new IllegalArgumentException("Duplicate tier data thresholds");
            last = tier.requiredData();
        }
        return result;
    }
    private static void validateOverrides(DataModel model, List<ModelTier> values) {
        int last = -1;
        for (ModelTier tier : values) {
            int required = model.required(tier);
            if (required <= last || last == -1 && required != 0 || model.gained(tier) < 0)
                throw new IllegalArgumentException("Invalid required_data/data_gained overrides");
            last = required;
        }
    }
    private static Map<String, JsonObject> external(String directory) {
        Path root = HostileConfig.directory.toPath().resolve(directory);
        Map<String, JsonObject> definitions = new TreeMap<>();
        try {
            Files.createDirectories(root);
            try (var files = Files.walk(root)) {
                for (Path path : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                    String relative = root.relativize(path).toString().replace('\\', '/');
                    int slash = relative.indexOf('/');
                    String id = slash == -1 ? "hostilenetworks:" + relative : relative.substring(0, slash) + ':' + relative.substring(slash + 1);
                    id = id.substring(0, id.length() - 5);
                    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { definitions.put(id, read(reader)); }
                }
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return definitions;
    }
    private static boolean conditions(JsonObject json) {
        if (!json.has("neoforge:conditions")) return true;
        for (JsonElement e : json.getAsJsonArray("neoforge:conditions")) {
            JsonObject c = e.getAsJsonObject();
            switch (c.get("type").getAsString()) {
                case "neoforge:mod_loaded" -> { if (!Loader.isModLoaded(c.get("modid").getAsString())) return false; }
                case "hostilenetworks:block_data_models_enabled" -> { if (!HostileConfig.enableBlockDataModels) return false; }
                default -> throw new IllegalArgumentException("Unknown model condition: " + c);
            }
        }
        return true;
    }
    public static ItemStack stack(JsonElement element) {
        if (element == null || element.isJsonNull()) return ItemStack.EMPTY;
        JsonObject json = element.isJsonPrimitive() ? new JsonObject() : element.getAsJsonObject();
        String id = element.isJsonPrimitive() ? element.getAsString() : json.get(json.has("id") ? "id" : "item").getAsString();
        if (id.equals("minecraft:air")) return ItemStack.EMPTY;
        Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
        if (item == null || item == net.minecraft.init.Items.AIR) {
            if (json.has("optional") && json.get("optional").getAsBoolean()) return ItemStack.EMPTY;
            throw new IllegalArgumentException("Missing item " + id);
        }
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        if (count < 1 || count > 4096) throw new IllegalArgumentException("Stack count must be 1..4096");
        ItemStack stack = new ItemStack(item, count, json.has("data") ? json.get("data").getAsInt() : 0);
        if (json.has("nbt")) try { stack.setTagCompound(JsonToNBT.getTagFromJson(json.get("nbt").getAsString())); }
        catch (NBTException e) { throw new IllegalArgumentException(e); }
        return stack;
    }
    public static List<ItemStack> ingredient(JsonElement element) {
        List<ItemStack> result = new ArrayList<>();
        if (element.isJsonArray()) for (JsonElement e : element.getAsJsonArray()) result.addAll(ingredient(e));
        else if (element.isJsonObject() && element.getAsJsonObject().has("ore")) result.addAll(OreDictionary.getOres(element.getAsJsonObject().get("ore").getAsString()));
        else { ItemStack stack = stack(element); if (!stack.isEmpty()) result.add(stack); }
        return result;
    }
}
