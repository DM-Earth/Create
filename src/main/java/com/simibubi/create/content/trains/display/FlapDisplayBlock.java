package com.simibubi.create.content.trains.display;

import static net.minecraft.state.property.Properties.WATERLOGGED;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.tick.QueryableTickScheduler;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.util.TagUtil;

public class FlapDisplayBlock extends HorizontalKineticBlock
	implements IBE<FlapDisplayBlockEntity>, IWrenchable, ICogWheel, Waterloggable {

	public static final BooleanProperty UP = BooleanProperty.of("up");
	public static final BooleanProperty DOWN = BooleanProperty.of("down");

	public FlapDisplayBlock(Settings p_49795_) {
		super(p_49795_);
		setDefaultState(getDefaultState().with(UP, false)
			.with(DOWN, false)
			.with(WATERLOGGED, false));
	}

	@Override
	protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
		return super.areStatesKineticallyEquivalent(oldState, newState);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(HORIZONTAL_FACING)
			.getAxis();
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(UP, DOWN, WATERLOGGED));
	}

	@Override
	public SpeedLevel getMinimumRequiredSpeedLevel() {
		return SpeedLevel.MEDIUM;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		Direction face = context.getSide();
		BlockPos clickedPos = context.getBlockPos();
		BlockPos placedOnPos = clickedPos.offset(face.getOpposite());
		World level = context.getWorld();
		BlockState blockState = level.getBlockState(placedOnPos);
		BlockState stateForPlacement = getDefaultState();
		FluidState ifluidstate = context.getWorld()
			.getFluidState(context.getBlockPos());

		if ((blockState.getBlock() != this) || (context.getPlayer() != null && context.getPlayer()
			.isSneaking()))
			stateForPlacement = super.getPlacementState(context);
		else {
			Direction otherFacing = blockState.get(HORIZONTAL_FACING);
			stateForPlacement = stateForPlacement.with(HORIZONTAL_FACING, otherFacing);
		}

		return updateColumn(level, clickedPos,
			stateForPlacement.with(WATERLOGGED, Boolean.valueOf(ifluidstate.getFluid() == Fluids.WATER)), true);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {

		if (player.isSneaking() || AdventureUtil.isAdventure(player))
			return ActionResult.PASS;

		ItemStack heldItem = player.getStackInHand(hand);

		IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
		if (placementHelper.matchesItem(heldItem))
			return placementHelper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

		FlapDisplayBlockEntity flapBE = getBlockEntity(world, pos);

		if (flapBE == null)
			return ActionResult.PASS;
		flapBE = flapBE.getController();
		if (flapBE == null)
			return ActionResult.PASS;

		double yCoord = ray.getPos()
			.add(Vec3d.of(ray.getSide()
				.getOpposite()
				.getVector())
				.multiply(.125f)).y;

		int lineIndex = flapBE.getLineIndexAt(yCoord);

		if (heldItem.isEmpty()) {
			if (!flapBE.isSpeedRequirementFulfilled())
				return ActionResult.PASS;
			flapBE.applyTextManually(lineIndex, null);
			return ActionResult.SUCCESS;
		}

		if (heldItem.getItem() == Items.GLOW_INK_SAC) {
			if (!world.isClient) {
				world.playSound(null, pos, SoundEvents.ITEM_INK_SAC_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
				flapBE.setGlowing(lineIndex);
			}
			return ActionResult.SUCCESS;
		}

		boolean display =
			heldItem.getItem() == Items.NAME_TAG && heldItem.hasCustomName() || AllBlocks.CLIPBOARD.isIn(heldItem);
		DyeColor dye = TagUtil.getColorFromStack(heldItem);

		if (!display && dye == null)
			return ActionResult.PASS;
		if (dye == null && !flapBE.isSpeedRequirementFulfilled())
			return ActionResult.PASS;
		if (world.isClient)
			return ActionResult.SUCCESS;

		NbtCompound tag = heldItem.getSubNbt("display");
		String tagElement = tag != null && tag.contains("Name", NbtElement.STRING_TYPE) ? tag.getString("Name") : null;

		if (display) {
			if (AllBlocks.CLIPBOARD.isIn(heldItem)) {
				List<ClipboardEntry> entries = ClipboardEntry.getLastViewedEntries(heldItem);
				int line = lineIndex;
				for (int i = 0; i < entries.size(); i++) {
					for (String string : entries.get(i).text.getString()
						.split("\n"))
						flapBE.applyTextManually(line++, Text.Serializer.toJson(Components.literal(string)));
				}
				return ActionResult.SUCCESS;
			}

			flapBE.applyTextManually(lineIndex, tagElement);
		}
		if (dye != null) {
			world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
			flapBE.setColour(lineIndex, dye);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		return AllShapes.FLAP_DISPLAY.get(pState.get(HORIZONTAL_FACING));
	}

	@Override
	public Class<FlapDisplayBlockEntity> getBlockEntityClass() {
		return FlapDisplayBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FlapDisplayBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.FLAP_DISPLAY.get();
	}

	@Override
	public float getParticleTargetRadius() {
		return .85f;
	}

	@Override
	public float getParticleInitialRadius() {
		return .75f;
	}

	private BlockState updateColumn(World level, BlockPos pos, BlockState state, boolean present) {
		Mutable currentPos = new Mutable();
		Axis axis = getConnectionAxis(state);

		for (Direction connection : Iterate.directionsInAxis(Axis.Y)) {
			boolean connect = true;

			Move: for (Direction movement : Iterate.directionsInAxis(axis)) {
				currentPos.set(pos);
				for (int i = 0; i < 1000; i++) {
					if (!level.canSetBlock(currentPos))
						break;

					BlockState other1 = currentPos.equals(pos) ? state : level.getBlockState(currentPos);
					BlockState other2 = level.getBlockState(currentPos.offset(connection));
					boolean col1 = canConnect(state, other1);
					boolean col2 = canConnect(state, other2);
					currentPos.move(movement);

					if (!col1 && !col2)
						break;
					if (col1 && col2)
						continue;

					connect = false;
					break Move;
				}
			}
			state = setConnection(state, connection, connect);
		}
		return state;
	}

	@Override
	public void onBlockAdded(BlockState pState, World pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
		super.onBlockAdded(pState, pLevel, pPos, pOldState, pIsMoving);
		if (pOldState.getBlock() == this)
			return;
		QueryableTickScheduler<Block> blockTicks = pLevel.getBlockTickScheduler();
		if (!blockTicks.isQueued(pPos, this))
			pLevel.scheduleBlockTick(pPos, this, 1);
	}

	@Override
	public void scheduledTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom) {
		if (pState.getBlock() != this)
			return;
		BlockPos belowPos =
			pPos.offset(Direction.from(getConnectionAxis(pState), AxisDirection.NEGATIVE));
		BlockState belowState = pLevel.getBlockState(belowPos);
		if (!canConnect(pState, belowState))
			KineticBlockEntity.switchToBlockState(pLevel, pPos, updateColumn(pLevel, pPos, pState, true));
		withBlockEntityDo(pLevel, pPos, FlapDisplayBlockEntity::updateControllerStatus);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction pDirection, BlockState pNeighborState,
		WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		return updatedShapeInner(state, pDirection, pNeighborState, pLevel, pCurrentPos);
	}

	private BlockState updatedShapeInner(BlockState state, Direction pDirection, BlockState pNeighborState,
		WorldAccess pLevel, BlockPos pCurrentPos) {
		if (state.get(Properties.WATERLOGGED))
			pLevel.scheduleFluidTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickRate(pLevel));
		if (!canConnect(state, pNeighborState))
			return setConnection(state, pDirection, false);
		if (pDirection.getAxis() == getConnectionAxis(state))
			return getStateWithProperties(pNeighborState).with(WATERLOGGED, state.get(WATERLOGGED));
		return setConnection(state, pDirection, getConnection(pNeighborState, pDirection.getOpposite()));
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false)
			: Fluids.EMPTY.getDefaultState();
	}

	protected boolean canConnect(BlockState state, BlockState other) {
		return other.getBlock() == this && state.get(HORIZONTAL_FACING) == other.get(HORIZONTAL_FACING);
	}

	protected Axis getConnectionAxis(BlockState state) {
		return state.get(HORIZONTAL_FACING)
			.rotateYClockwise()
			.getAxis();
	}

	public static boolean getConnection(BlockState state, Direction side) {
		BooleanProperty property = side == Direction.DOWN ? DOWN : side == Direction.UP ? UP : null;
		return property != null && state.get(property);
	}

	public static BlockState setConnection(BlockState state, Direction side, boolean connect) {
		BooleanProperty property = side == Direction.DOWN ? DOWN : side == Direction.UP ? UP : null;
		if (property != null)
			state = state.with(property, connect);
		return state;
	}

	@Override
	public void onStateReplaced(BlockState pState, World pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		super.onStateReplaced(pState, pLevel, pPos, pNewState, pIsMoving);
		if (pIsMoving || pNewState.getBlock() == this)
			return;
		for (Direction d : Iterate.directionsInAxis(getConnectionAxis(pState))) {
			BlockPos relative = pPos.offset(d);
			BlockState adjacent = pLevel.getBlockState(relative);
			if (canConnect(pState, adjacent))
				KineticBlockEntity.switchToBlockState(pLevel, relative,
					updateColumn(pLevel, relative, adjacent, false));
		}
	}

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {
		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.DISPLAY_BOARD::isIn;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return AllBlocks.DISPLAY_BOARD::has;
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getPos(),
				state.get(FlapDisplayBlock.HORIZONTAL_FACING)
					.getAxis(),
				dir -> world.getBlockState(pos.offset(dir))
					.isReplaceable());

			return directions.isEmpty() ? PlacementOffset.fail()
				: PlacementOffset.success(pos.offset(directions.get(0)), s -> AllBlocks.DISPLAY_BOARD.get()
					.updateColumn(world, pos.offset(directions.get(0)),
						s.with(HORIZONTAL_FACING, state.get(FlapDisplayBlock.HORIZONTAL_FACING)), true));
		}
	}

}
