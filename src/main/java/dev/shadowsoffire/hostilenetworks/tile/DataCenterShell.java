package dev.shadowsoffire.hostilenetworks.tile;

import java.util.*;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.block.MachineBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Upstream geometry: 7x7x7 shell, 5x5x5 air, controller in the first wall layer above the floor. */
public final class DataCenterShell {
    public record Layout(BlockPos min, BlockPos controller) {
        public BlockPos max() { return min.add(6, 6, 6); }
        public boolean contains(BlockPos p) { return p.getX() >= min.getX() && p.getX() <= min.getX() + 6 && p.getY() >= min.getY() && p.getY() <= min.getY() + 6 && p.getZ() >= min.getZ() && p.getZ() <= min.getZ() + 6; }
        public boolean boundary(BlockPos p) { return contains(p) && (p.getX() == min.getX() || p.getX() == min.getX() + 6 || p.getY() == min.getY() || p.getY() == min.getY() + 6 || p.getZ() == min.getZ() || p.getZ() == min.getZ() + 6); }
        public boolean portPosition(BlockPos p) { return boundary(p) && p.getY() > min.getY() && !p.equals(controller); }
        public BlockPos center() { return min.add(3, 3, 3); }
    }
    public static Layout find(World world, BlockPos controller) {
        IBlockState state = world.getBlockState(controller);
        if (!state.getPropertyKeys().contains(MachineBlock.FACING)) return null;
        EnumFacing inward = state.getValue(MachineBlock.FACING).getOpposite();
        for (int offset = 0; offset < 7; offset++) {
            int x = controller.getX(), z = controller.getZ();
            if (inward.getAxis() == EnumFacing.Axis.Z) { x -= offset; if (inward == EnumFacing.NORTH) z -= 6; }
            else { z -= offset; if (inward == EnumFacing.WEST) x -= 6; }
            Layout candidate = new Layout(new BlockPos(x, controller.getY() - 1, z), controller);
            if (invalid(world, candidate, 1).isEmpty()) return candidate;
        }
        return null;
    }
    public static List<BlockPos> invalid(World world, Layout layout, int limit) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : BlockPos.getAllInBox(layout.min, layout.max())) {
            if (pos.equals(layout.controller)) continue;
            if (!world.isBlockLoaded(pos) || !valid(world, pos, layout)) { result.add(pos.toImmutable()); if (result.size() >= limit) break; }
        }
        return result;
    }
    private static boolean valid(World world, BlockPos pos, Layout layout) {
        if (pos.getY() == layout.min.getY()) return matches(world.getBlockState(pos), HostileConfig.floorBlocks);
        if (layout.boundary(pos)) return matches(world.getBlockState(pos), HostileConfig.wallBlocks);
        return world.isAirBlock(pos);
    }
    private static boolean matches(IBlockState state, String[] allowed) {
        for (String entry : allowed) {
            int split = entry.indexOf('@'); String id = split < 0 ? entry : entry.substring(0, split);
            if (!state.getBlock().getRegistryName().toString().equals(id)) continue;
            if (split < 0 || state.getBlock().getMetaFromState(state) == Integer.parseInt(entry.substring(split + 1))) return true;
        }
        return false;
    }
}
