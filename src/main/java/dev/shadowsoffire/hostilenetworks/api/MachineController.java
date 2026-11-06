package dev.shadowsoffire.hostilenetworks.api;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.tile.MachineTile;

/** Optional server-side automation contract. The controller must be a loaded tile in the same world.
 * Materials and FE are still processed by the actual HNN machine. Never force-load the controller.
 */
public interface MachineController {
    boolean controls(MachineTile machine);
    boolean canRun(MachineTile machine);
    default int selectedDrop(MachineTile machine, DataModel model) { return -1; }
}
