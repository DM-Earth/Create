package com.simibubi.create.content.redstone.smartObserver;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SmartObserverBlock extends DirectedDirectionalBlock implements IBE<SmartObserverBlockEntity>, ConnectableRedstoneBlock {

	public static final BooleanProperty POWERED = Properties.POWERED;

	public SmartObserverBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(POWERED, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(POWERED));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState state = getDefaultState();

		Direction preferredFacing = null;
		for (Direction face : context.getPlacementDirections()) {
			BlockPos offsetPos = context.getBlockPos()
				.offset(face);
			World world = context.getWorld();
			boolean canDetect = false;
			BlockEntity blockEntity = world.getBlockEntity(offsetPos);

			if (BlockEntityBehaviour.get(blockEntity, TransportedItemStackHandlerBehaviour.TYPE) != null)
				canDetect = true;
			else if (BlockEntityBehaviour.get(blockEntity, FluidTransportBehaviour.TYPE) != null)
				canDetect = true;
			else if (TransferUtil.getItemStorage(world, offsetPos, face.getOpposite()) != null
					|| TransferUtil.getFluidStorage(world, offsetPos, face.getOpposite()) != null)
				canDetect = true;
			else if (blockEntity instanceof FunnelBlockEntity)
				canDetect = true;

			if (canDetect) {
				preferredFacing = face;
				break;
			}
		}

		if (preferredFacing == null) {
			Direction facing = context.getPlayerLookDirection();
			preferredFacing = context.getPlayer() != null && context.getPlayer()
				.isSneaking() ? facing : facing.getOpposite();
		}

		if (preferredFacing.getAxis() == Axis.Y) {
			state = state.with(TARGET, preferredFacing == Direction.UP ? WallMountLocation.CEILING : WallMountLocation.FLOOR);
			preferredFacing = context.getHorizontalPlayerFacing();
		}

		return state.with(FACING, preferredFacing);
	}

	@Override
	public boolean emitsRedstonePower(BlockState state) {
		return state.get(POWERED);
	}

	@Override
	public int getWeakRedstonePower(BlockState blockState, BlockView blockAccess, BlockPos pos, Direction side) {
		return emitsRedstonePower(blockState) && (side == null || side != getTargetDirection(blockState)
			.getOpposite()) ? 15 : 0;
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
		worldIn.setBlockState(pos, state.with(POWERED, false), 2);
		worldIn.updateNeighborsAlways(pos, this);
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockView world, BlockPos pos, Direction side) {
		return side != state.get(FACING)
			.getOpposite();
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, worldIn, pos, newState);
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		InvManipulationBehaviour behaviour = BlockEntityBehaviour.get(worldIn, pos, InvManipulationBehaviour.TYPE);
		if (behaviour != null)
			behaviour.onNeighborChanged(fromPos);
	}

	public void onFunnelTransfer(World world, BlockPos funnelPos, ItemStack transferred) {
		for (Direction direction : Iterate.directions) {
			BlockPos detectorPos = funnelPos.offset(direction);
			BlockState detectorState = world.getBlockState(detectorPos);
			if (!AllBlocks.SMART_OBSERVER.has(detectorState))
				continue;
			if (SmartObserverBlock.getTargetDirection(detectorState) != direction.getOpposite())
				continue;
			withBlockEntityDo(world, detectorPos, be -> {
				FilteringBehaviour filteringBehaviour = BlockEntityBehaviour.get(be, FilteringBehaviour.TYPE);
				if (filteringBehaviour == null)
					return;
				if (!filteringBehaviour.test(transferred))
					return;
				be.activate(4);
			});
		}
	}

	@Override
	public Class<SmartObserverBlockEntity> getBlockEntityClass() {
		return SmartObserverBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SmartObserverBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SMART_OBSERVER.get();
	}

}
