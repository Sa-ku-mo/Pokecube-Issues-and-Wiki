package pokecube.legends.blocks.normalblocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallGateBlock extends FenceGateBlock
{
    public WallGateBlock(Properties properties, SoundEvent openSound, SoundEvent closeSound)
    {
    	super(properties, openSound, closeSound);
	}

    public WallGateBlock(BlockBehaviour.Properties properties, WoodType woodType)
    {
        this(properties.sound(woodType.soundType()), woodType.fenceGateOpen(), woodType.fenceGateClose());
    }

	@Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
    {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }
}
