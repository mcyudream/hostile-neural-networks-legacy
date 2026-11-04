package dev.shadowsoffire.hostilenetworks.tile;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import java.util.*;
import net.minecraft.item.ItemStack;

public final class MergerTile extends ExpansionTile {
    public MergerTile() { super(6, 5, ExpansionConfig.mergerCapacity); }
    @Override public int duration() { return ExpansionConfig.mergerDuration; }
    @Override public int cost() { return ExpansionConfig.mergerCost; }
    @Override public boolean isModelSlot(int slot) { return slot < 4; }
    @Override public boolean accepts(int slot, ItemStack stack) {
        DataModel model = ModelStack.model(stack);
        if (slot < 4) return stack.getItem() == Hostile.item("data_model") && model != null && model.tier(ModelStack.data(stack)) == ModelRegistry.tiers().getLast();
        return slot == 4 && stack.getItem() == Hostile.item("blank_extra_data_model");
    }
    @Override protected void tickServer() {
        changedInputs(inputSignature());
        if (!ready()) return;
        List<DataModel> models = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!accepts(i, stack)) { progress = 0; failure = "merge_models"; return; }
            models.add(ModelStack.model(stack));
        }
        if (!accepts(4, inventory.getStackInSlot(4))) { failure = "input"; return; }
        ItemStack result = PerfectModel.create(models);
        if (!canFit(List.of(result))) { failure = "output"; return; }
        if (payTick() && progress >= duration()) {
            for (int i = 0; i < 5; i++) inventory.extractItem(i, 1, false);
            emit(List.of(result)); progress = 0; running = ""; settingsChanged();
        }
    }
}
