package dev.shadowsoffire.hostilenetworks.tile;

import java.util.*;
import dev.shadowsoffire.hostilenetworks.data.*;
import net.minecraft.nbt.*;

/** Saved per-model fixed picks and bounded round-robin queues. */
public final class Selections {
    public static final int MAX_QUEUE = 64;
    private final Map<String, Entry> entries = new TreeMap<>();
    public static final class Entry {
        public boolean queue;
        public final List<Integer> drops = new ArrayList<>();
        public int cursor;
        public int current() { return drops.isEmpty() ? -1 : drops.get(Math.floorMod(cursor, drops.size())); }
        public void advance() { if (queue && !drops.isEmpty()) cursor = (cursor + 1) % drops.size(); }
    }
    public Entry get(DataModel model) { return entries.computeIfAbsent(model.id.toString(), key -> new Entry()); }
    public int current(DataModel model) { int result = get(model).current(); return result >= 0 && result < model.drops.size() ? result : -1; }
    public void select(DataModel model, int drop) {
        if (drop < 0 || drop >= model.drops.size()) return;
        Entry entry = get(model);
        if (!entry.queue) { entry.drops.clear(); entry.cursor = 0; }
        if (entry.drops.size() < MAX_QUEUE) entry.drops.add(drop);
    }
    public void cycle(DataModel model) {
        Entry e = get(model); int current = e.current(); e.queue = !e.queue; e.cursor = 0;
        if (!e.queue) { e.drops.clear(); if (current >= 0) e.drops.add(current); }
    }
    public void clear(DataModel model) { Entry e = get(model); e.drops.clear(); e.cursor = 0; }
    public void remove(DataModel model, int index) {
        Entry e = get(model); if (index < 0 || index >= e.drops.size()) return;
        e.drops.remove(index); if (index < e.cursor) e.cursor--; e.cursor = e.drops.isEmpty() ? 0 : Math.floorMod(e.cursor, e.drops.size());
    }
    public NBTTagCompound write() {
        NBTTagCompound result = new NBTTagCompound();
        entries.forEach((model, e) -> { NBTTagCompound n = new NBTTagCompound(); n.setBoolean("queue", e.queue);
            n.setIntArray("drops", e.drops.stream().mapToInt(Integer::intValue).toArray()); n.setInteger("cursor", e.cursor); result.setTag(model, n); });
        return result;
    }
    public void read(NBTTagCompound tag) {
        entries.clear();
        for (String id : tag.getKeySet()) {
            if (entries.size() >= 1024) break;
            NBTTagCompound n = tag.getCompoundTag(id); Entry e = new Entry(); e.queue = n.getBoolean("queue");
            for (int drop : n.getIntArray("drops")) if (e.drops.size() < MAX_QUEUE && drop >= 0) e.drops.add(drop);
            e.cursor = e.drops.isEmpty() ? 0 : Math.floorMod(n.getInteger("cursor"), e.drops.size()); entries.put(id, e);
        }
    }
}
