package dev.shadowsoffire.hostilenetworks.block;

import dev.shadowsoffire.hostilenetworks.tile.IOPortTile;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public final class IOPortBlock extends Block {
    public enum Mode implements IStringSerializable {
        ENERGY, MODELS, INPUTS, OUTPUTS;
        @Override public String getName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final PropertyEnum<Mode> MODE = PropertyEnum.create("mode", Mode.class);
    public IOPortBlock() { super(Material.IRON); setHardness(3); setResistance(8); setDefaultState(blockState.getBaseState().withProperty(MODE, Mode.ENERGY)); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, MODE); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(MODE).ordinal(); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(MODE, Mode.values()[meta & 3]); }
    @Override public int damageDropped(IBlockState state) { return 0; }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new IOPortTile(); }
    @Override public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        if (!w.isRemote) {
            Mode mode = Mode.values()[(s.getValue(MODE).ordinal() + 1) % 4]; w.setBlockState(pos, s.withProperty(MODE, mode), 3);
            p.sendStatusMessage(new TextComponentTranslation("hostilenetworks.io_port.mode." + mode.getName()), true);
        }
        return true;
    }
}
