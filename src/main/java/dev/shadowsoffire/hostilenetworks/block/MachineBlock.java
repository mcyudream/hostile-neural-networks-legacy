package dev.shadowsoffire.hostilenetworks.block;

import dev.shadowsoffire.hostilenetworks.*;
import dev.shadowsoffire.hostilenetworks.tile.*;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.inventory.InventoryHelper;

public final class MachineBlock extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public final String machine;
    public MachineBlock(String machine) {
        super(Material.IRON); this.machine = machine; setHardness(3); setResistance(8); setHarvestLevel("pickaxe", 0);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex(); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)); }
    @Override public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing f, float x, float y, float z, int meta, EntityLivingBase placer) { return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()); }
    @Override public IBlockState withRotation(IBlockState s, Rotation r) { return s.withProperty(FACING, r.rotate(s.getValue(FACING))); }
    @Override public IBlockState withMirror(IBlockState s, Mirror m) { return withRotation(s, m.toRotation(s.getValue(FACING))); }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) {
        if (machine.startsWith("ultimate_sim_chamber_v")) return new UltimateSimTile(machine.charAt(machine.length() - 1) - '1');
        if (machine.startsWith("ultimate_loot_fabricator_v")) return new UltimateFabTile(machine.charAt(machine.length() - 1) - '1');
        return switch (machine) {
            case "sim_chamber" -> new SimChamberTile(); case "loot_fabricator" -> new LootFabTile();
            case "merger_camera" -> new MergerTile(); case "simulator_modeling" -> new ModelTrainerTile();
            case "digital_agonizer" -> new DigitalAgonizerTile(); default -> new DataCenterTile();
        };
    }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    @Override public boolean canProvidePower(IBlockState s) { return false; }
    @Override public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        for (String id : new String[]{"fab_directive", "deep_learner", "setting_card", "altar_linker"})
            if (!p.getHeldItem(hand).isEmpty() && p.getHeldItem(hand).getItem() == Hostile.item(id)) return false;
        if (!w.isRemote) p.openGui(HostileNetworks.INSTANCE, 0, w, pos.getX(), pos.getY(), pos.getZ()); return true;
    }
    @Override public void breakBlock(World w, BlockPos pos, IBlockState state) {
        if (!w.isRemote && w.getTileEntity(pos) instanceof MachineTile machine) {
            for (ItemStack stack : machine.dropsOnBreak()) InventoryHelper.spawnItemStack(w, pos.getX(), pos.getY(), pos.getZ(), stack);
            if (machine instanceof DataCenterTile dc) dc.releasePorts();
        }
        super.breakBlock(w, pos, state);
    }
}
