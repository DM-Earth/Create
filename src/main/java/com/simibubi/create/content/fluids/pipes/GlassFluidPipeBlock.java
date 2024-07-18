package com.simibubi.create.content.fluids.pipes;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class GlassFluidPipeBlock extends AxisPipeBlock implements IBE<StraightPipeBlockEntity>, Waterloggable, ISpecialBlockItemRequirement {

	public static final BooleanProperty ALT = BooleanProperty.of("alt");

	public GlassFluidPipeBlock(Settings p_i48339_1_) {
		super(p_i48339_1_);
		setDefaultState(getDefaultState().with(ALT, false).with(Properties.WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> p_206840_1_) {
		super.appendProperties(p_206840_1_.add(ALT, Properties.WATERLOGGED));
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		if (tryRemoveBracket(context))
			return ActionResult.SUCCESS;
		BlockState newState;
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		FluidTransportBehaviour.cacheFlows(world, pos);
		newState = toRegularPipe(world, pos, state).with(Properties.WATERLOGGED, state.get(Properties.WATERLOGGED));
		world.setBlockState(pos, newState, 3);
		FluidTransportBehaviour.loadFlows(world, pos);
		return ActionResult.SUCCESS;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		FluidState ifluidstate = context.getWorld()
			.getFluidState(context.getBlockPos());
		BlockState state = super.getPlacementState(context);
		return state == null ? null : state.with(Properties.WATERLOGGED,
			ifluidstate.getFluid() == Fluids.WATER);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false)
			: Fluids.EMPTY.getDefaultState();
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		return ItemRequirement.of(AllBlocks.FLUID_PIPE.getDefaultState(), be);
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public Class<StraightPipeBlockEntity> getBlockEntityClass() {
		return StraightPipeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends StraightPipeBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.GLASS_FLUID_PIPE.get();
	}

}
