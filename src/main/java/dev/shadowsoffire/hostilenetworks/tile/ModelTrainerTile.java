package dev.shadowsoffire.hostilenetworks.tile;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import java.util.*;
import net.minecraft.item.ItemStack;

public final class ModelTrainerTile extends ExpansionTile {
    public ModelTrainerTile() { super(3, 3, ExpansionConfig.trainerCapacity); }
    public boolean upgrade(String id) { return inventory.getStackInSlot(1).getItem() == Hostile.item(id) || inventory.getStackInSlot(2).getItem() == Hostile.item(id); }
    @Override public int duration() { return Math.max(1, (int) Math.ceil(ExpansionConfig.trainerDuration * (upgrade("upgrade_speed") ? 1 - ExpansionConfig.speedPercent / 100D : 1))); }
    @Override public int cost() {
        long base = upgrade("upgrade_module_stack") ? ExpansionConfig.tierCost : ExpansionConfig.trainerCost;
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(base * (upgrade("upgrade_speed") ? ExpansionConfig.speedEnergy : 1D)));
    }
    @Override public boolean isModelSlot(int slot) { return slot >= 0 && slot < 3; }
    public boolean allowed(ItemStack stack) {
        List<DataModel> models = PerfectModel.models(stack); DataModel model = ModelStack.model(stack);
        if (stack.getItem() == Hostile.item("data_model") && model != null) {
            ModelTier min = ModelRegistry.tiers().stream().filter(t -> t.id().equals(ExpansionConfig.minimumNormal)).findFirst().orElse(ModelRegistry.tiers().getFirst());
            if (ModelStack.data(stack) < model.required(min)) return false;
            models = List.of(model);
        } else if (models.isEmpty()) return false;
        else if (PerfectModel.tier(stack) < Math.max(0, Arrays.asList(PerfectModel.TIERS).indexOf(ExpansionConfig.minimumExtra))) return false;
        return models.stream().noneMatch(m -> Arrays.asList(ExpansionConfig.trainingBlacklist).contains(m.id.toString()));
    }
    @Override public boolean modelComplete() {
        ItemStack stack = inventory.getStackInSlot(0); DataModel model = ModelStack.model(stack);
        return model != null && stack.getItem() == Hostile.item("data_model") ? model.tier(ModelStack.data(stack)) == ModelRegistry.tiers().getLast()
            : !PerfectModel.models(stack).isEmpty() && PerfectModel.tier(stack) == 4;
    }
    @Override public boolean canExtract(int slot) { return slot == 0 && modelComplete(); }
    @Override public boolean accepts(int slot, ItemStack stack) {
        if (slot == 0) return allowed(stack) && !complete(stack);
        return slot > 0 && slot < 3 && !stack.isEmpty() && List.of("upgrade_speed", "upgrade_module_stack", "upgrade_data_kill").stream().anyMatch(id -> stack.getItem() == Hostile.item(id));
    }
    private boolean complete(ItemStack stack) {
        DataModel model = ModelStack.model(stack);
        return model != null ? model.tier(ModelStack.data(stack)) == ModelRegistry.tiers().getLast() : PerfectModel.tier(stack) == 4;
    }
    @Override protected void tickServer() {
        changedInputs(inputSignature());
        if (!ready()) return;
        ItemStack stack = inventory.getStackInSlot(0);
        if (!allowed(stack)) { progress = 0; failure = "model"; return; }
        if (modelComplete()) { progress = 0; failure = "complete"; return; }
        if (!payTick() || progress < duration()) return;
        int target, gain; DataModel model = ModelStack.model(stack);
        if (model != null) { ModelTier tier = model.tier(ModelStack.data(stack)); target = model.required(model.next(tier)); gain = model.gained(tier); }
        else { int tier = PerfectModel.tier(stack); target = ExpansionConfig.required[tier + 1]; gain = ExpansionConfig.gained[tier]; }
        long data = upgrade("upgrade_module_stack") ? target : Math.min(target, (long) ModelStack.data(stack) + (long) gain * (upgrade("upgrade_data_kill") ? ExpansionConfig.dataMultiplier : 1));
        ModelStack.setData(stack, (int) data); ModelStack.tag(stack).removeTag("awaiting_action"); ModelStack.iteration(stack);
        progress = 0; running = ""; settingsChanged();
    }
}
