package com.simibubi.create.content.kinetics.fan;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class NozzleBlock extends WrenchableDirectionalBlock implements IBE<NozzleBlockEntity> {

	public NozzleBlock(Settings p_i48415_1_) {
		super(p_i48415_1_);
	}
	
	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		return ActionResult.FAIL;
	}
	
	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return getDefaultState().with(FACING, context.getSide());
	}
	
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.NOZZLE.get(state.get(FACING));
	}
	
	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		if (worldIn.isClient)
			return;

		if (fromPos.equals(pos.offset(state.get(FACING).getOpposite())))
			if (!canPlaceAt(state, worldIn, pos)) {
				worldIn.breakBlock(pos, true);
				return;
			}
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView worldIn, BlockPos pos) {
		Direction towardsFan = state.get(FACING).getOpposite();
		BlockEntity be = worldIn.getBlockEntity(pos.offset(towardsFan));
		return be instanceof IAirCurrentSource
				&& ((IAirCurrentSource) be).getAirflowOriginSide() == towardsFan.getOpposite();
	}
	
	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public Class<NozzleBlockEntity> getBlockEntityClass() {
		return NozzleBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends NozzleBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.NOZZLE.get();
	}

}
