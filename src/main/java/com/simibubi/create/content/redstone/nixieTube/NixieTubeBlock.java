package com.simibubi.create.content.redstone.nixieTube;

import static net.minecraft.state.property.Properties.WATERLOGGED;

import java.util.List;
import java.util.function.BiConsumer;

import com.simibubi.create.foundation.utility.AdventureUtil;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import io.github.fabricators_of_create.porting_lib.util.TagUtil;
import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class NixieTubeBlock extends DoubleFaceAttachedBlock
	implements IBE<NixieTubeBlockEntity>, IWrenchable, Waterloggable, ISpecialBlockItemRequirement, BlockPickInteractionAware, ConnectableRedstoneBlock {

	protected final DyeColor color;

	public NixieTubeBlock(Settings properties, DyeColor color) {
		super(properties);
		this.color = color;
		setDefaultState(getDefaultState().with(FACE, DoubleAttachFace.FLOOR)
			.with(WATERLOGGED, false));
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {

		if (player.isSneaking() || AdventureUtil.isAdventure(player))
			return ActionResult.PASS;

		ItemStack heldItem = player.getStackInHand(hand);
		NixieTubeBlockEntity nixie = getBlockEntity(world, pos);

		if (nixie == null)
			return ActionResult.PASS;
		if (heldItem.isEmpty()) {
			if (nixie.reactsToRedstone())
				return ActionResult.PASS;
			nixie.clearCustomText();
			updateDisplayedRedstoneValue(state, world, pos);
			return ActionResult.SUCCESS;
		}

		boolean display =
			heldItem.getItem() == Items.NAME_TAG && heldItem.hasCustomName() || AllBlocks.CLIPBOARD.isIn(heldItem);
		DyeColor dye = TagUtil.getColorFromStack(heldItem);

		if (!display && dye == null)
			return ActionResult.PASS;

		NbtCompound tag = heldItem.getSubNbt("display");
		String tagElement = tag != null && tag.contains("Name", NbtElement.STRING_TYPE) ? tag.getString("Name") : null;

		if (AllBlocks.CLIPBOARD.isIn(heldItem)) {
			List<ClipboardEntry> entries = ClipboardEntry.getLastViewedEntries(heldItem);
			for (int i = 0; i < entries.size();) {
				tagElement = Text.Serializer.toJson(entries.get(i).text);
				break;
			}
		}

		if (world.isClient)
			return ActionResult.SUCCESS;

		String tagUsed = tagElement;
		walkNixies(world, pos, (currentPos, rowPosition) -> {
			if (display)
				withBlockEntityDo(world, currentPos, be -> be.displayCustomText(tagUsed, rowPosition));
			if (dye != null)
				world.setBlockState(currentPos, withColor(state, dye));
		});

		return ActionResult.SUCCESS;
	}

	public static void walkNixies(WorldAccess world, BlockPos start, BiConsumer<BlockPos, Integer> callback) {
		BlockState state = world.getBlockState(start);
		if (!(state.getBlock() instanceof NixieTubeBlock))
			return;

		BlockPos currentPos = start;
		Direction left = state.get(FACING)
			.getOpposite();

		if (state.get(FACE) == DoubleAttachFace.WALL)
			left = Direction.UP;
		if (state.get(FACE) == DoubleAttachFace.WALL_REVERSED)
			left = Direction.DOWN;

		Direction right = left.getOpposite();

		while (true) {
			BlockPos nextPos = currentPos.offset(left);
			if (!areNixieBlocksEqual(world.getBlockState(nextPos), state))
				break;
			currentPos = nextPos;
		}

		int index = 0;

		while (true) {
			final int rowPosition = index;
			callback.accept(currentPos, rowPosition);
			BlockPos nextPos = currentPos.offset(right);
			if (!areNixieBlocksEqual(world.getBlockState(nextPos), state))
				break;
			currentPos = nextPos;
			index++;
		}
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(FACE, FACING, WATERLOGGED));
	}

	@Override
	public void onStateReplaced(BlockState p_196243_1_, World p_196243_2_, BlockPos p_196243_3_, BlockState p_196243_4_,
		boolean p_196243_5_) {
		if (!(p_196243_4_.getBlock() instanceof NixieTubeBlock))
			p_196243_2_.removeBlockEntity(p_196243_3_);
	}

	@Override
	public ItemStack getPickStack(BlockView p_185473_1_, BlockPos p_185473_2_, BlockState p_185473_3_) {
		return AllBlocks.ORANGE_NIXIE_TUBE.asStack();
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		return new ItemRequirement(ItemUseType.CONSUME, AllBlocks.ORANGE_NIXIE_TUBE.get()
			.asItem());
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		Direction facing = pState.get(FACING);
		switch (pState.get(FACE)) {
		case CEILING:
			return AllShapes.NIXIE_TUBE_CEILING.get(facing.rotateYClockwise()
				.getAxis());
		case FLOOR:
			return AllShapes.NIXIE_TUBE.get(facing.rotateYClockwise()
				.getAxis());
		default:
			return AllShapes.NIXIE_TUBE_WALL.get(facing);
		}
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockView view, BlockPos pos, @Nullable PlayerEntity player, @Nullable HitResult result) {
		if (color != DyeColor.ORANGE)
			return AllBlocks.ORANGE_NIXIE_TUBE.get()
					.getPickedStack(state, view, pos, player, result);
		return new ItemStack(AllBlocks.NIXIE_TUBES.get(color).get());
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : Fluids.EMPTY.getDefaultState();
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbourState, WorldAccess world,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.get(WATERLOGGED))
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		return state;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState state = super.getPlacementState(context);
		if (state == null)
			return null;
		if (state.get(FACE) != DoubleAttachFace.WALL && state.get(FACE) != DoubleAttachFace.WALL_REVERSED)
			state = state.with(FACING, state.get(FACING)
				.rotateYClockwise());
		return state.with(WATERLOGGED, Boolean.valueOf(context.getWorld()
			.getFluidState(context.getBlockPos())
			.getFluid() == Fluids.WATER));
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block p_220069_4_, BlockPos p_220069_5_,
		boolean p_220069_6_) {
		if (worldIn.isClient)
			return;
		if (!worldIn.getBlockTickScheduler()
			.isTicking(pos, this))
			worldIn.scheduleBlockTick(pos, this, 0);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random r) {
		updateDisplayedRedstoneValue(state, worldIn, pos);
	}

	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (state.getBlock() == oldState.getBlock() || isMoving)
			return;
		updateDisplayedRedstoneValue(state, worldIn, pos);
	}

	private void updateDisplayedRedstoneValue(BlockState state, World worldIn, BlockPos pos) {
		if (worldIn.isClient)
			return;
		withBlockEntityDo(worldIn, pos, be -> {
			if (be.reactsToRedstone())
				be.updateRedstoneStrength(getPower(worldIn, pos));
		});
	}

	static boolean isValidBlock(BlockView world, BlockPos pos, boolean above) {
		BlockState state = world.getBlockState(pos.up(above ? 1 : -1));
		return !state.getOutlineShape(world, pos)
			.isEmpty();
	}

	private int getPower(World worldIn, BlockPos pos) {
		int power = 0;
		for (Direction direction : Iterate.directions)
			power = Math.max(worldIn.getEmittedRedstonePower(pos.offset(direction), direction), power);
		for (Direction direction : Iterate.directions)
			power = Math.max(worldIn.getEmittedRedstonePower(pos.offset(direction), Direction.UP), power);
		return power;
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockView world, BlockPos pos, Direction side) {
		return side != null;
	}

	@Override
	public Class<NixieTubeBlockEntity> getBlockEntityClass() {
		return NixieTubeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends NixieTubeBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.NIXIE_TUBE.get();
	}

	public DyeColor getColor() {
		return color;
	}

	public static boolean areNixieBlocksEqual(BlockState blockState, BlockState otherState) {
		if (!(blockState.getBlock() instanceof NixieTubeBlock))
			return false;
		if (!(otherState.getBlock() instanceof NixieTubeBlock))
			return false;
		return withColor(blockState, DyeColor.WHITE) == withColor(otherState, DyeColor.WHITE);
	}

	public static BlockState withColor(BlockState state, DyeColor color) {
		return (color == DyeColor.ORANGE ? AllBlocks.ORANGE_NIXIE_TUBE : AllBlocks.NIXIE_TUBES.get(color))
			.getDefaultState()
			.with(FACING, state.get(FACING))
			.with(WATERLOGGED, state.get(WATERLOGGED))
			.with(FACE, state.get(FACE));
	}

	public static DyeColor colorOf(BlockState blockState) {
		return blockState.getBlock() instanceof NixieTubeBlock ? ((NixieTubeBlock) blockState.getBlock()).color
			: DyeColor.ORANGE;
	}

	public static Direction getFacing(BlockState sideState) {
		return getConnectedDirection(sideState);
	}

}
