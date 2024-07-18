package com.simibubi.create.content.contraptions.mounted;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.redstone.rail.ControllerRailBlock;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.block.IBE;
import io.github.fabricators_of_create.porting_lib.block.MinecartPassHandlerBlock;
import io.github.fabricators_of_create.porting_lib.block.SlopeCreationCheckingRailBlock;

public class CartAssemblerBlock extends AbstractRailBlock
	implements IBE<CartAssemblerBlockEntity>, IWrenchable, ISpecialBlockItemRequirement, SlopeCreationCheckingRailBlock, MinecartPassHandlerBlock {

	public static final BooleanProperty POWERED = Properties.POWERED;
	public static final BooleanProperty BACKWARDS = BooleanProperty.of("backwards");
	public static final Property<RailShape> RAIL_SHAPE =
		EnumProperty.of("shape", RailShape.class, RailShape.EAST_WEST, RailShape.NORTH_SOUTH);
	public static final Property<CartAssembleRailType> RAIL_TYPE =
		EnumProperty.of("rail_type", CartAssembleRailType.class);

	public CartAssemblerBlock(Settings properties) {
		super(true, properties);
		setDefaultState(getDefaultState().with(POWERED, false)
			.with(BACKWARDS, false)
			.with(RAIL_TYPE, CartAssembleRailType.POWERED_RAIL)
			.with(WATERLOGGED, false));
	}

	public static BlockState createAnchor(BlockState state) {
		Axis axis = state.get(RAIL_SHAPE) == RailShape.NORTH_SOUTH ? Axis.Z : Axis.X;
		return AllBlocks.MINECART_ANCHOR.getDefaultState()
			.with(Properties.HORIZONTAL_AXIS, axis);
	}

	private static Item getRailItem(BlockState state) {
		return state.get(RAIL_TYPE)
			.getItem();
	}

	public static BlockState getRailBlock(BlockState state) {
		AbstractRailBlock railBlock = (AbstractRailBlock) state.get(RAIL_TYPE)
			.getBlock();

		@SuppressWarnings("deprecation")
		BlockState railState = railBlock.getDefaultState()
			.with(railBlock.getShapeProperty(), state.get(RAIL_SHAPE));

		if (railState.contains(ControllerRailBlock.BACKWARDS))
			railState = railState.with(ControllerRailBlock.BACKWARDS, state.get(BACKWARDS));
		return railState;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(RAIL_SHAPE, POWERED, RAIL_TYPE, BACKWARDS, WATERLOGGED);
		super.appendProperties(builder);
	}

	@Override
	public boolean canMakeSlopes(@Nonnull BlockState state, @Nonnull BlockView world, @Nonnull BlockPos pos) {
		return false;
	}

	@Override
	public void onMinecartPass(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos,
		AbstractMinecartEntity cart) {
		if (!canAssembleTo(cart))
			return;
		if (world.isClient)
			return;

		withBlockEntityDo(world, pos, be -> be.assembleNextTick(cart));
	}

	public enum CartAssemblerAction {
		ASSEMBLE, DISASSEMBLE, ASSEMBLE_ACCELERATE, DISASSEMBLE_BRAKE, ASSEMBLE_ACCELERATE_DIRECTIONAL, PASS;

		public boolean shouldAssemble() {
			return this == ASSEMBLE || this == ASSEMBLE_ACCELERATE || this == ASSEMBLE_ACCELERATE_DIRECTIONAL;
		}

		public boolean shouldDisassemble() {
			return this == DISASSEMBLE || this == DISASSEMBLE_BRAKE;
		}
	}

	public static CartAssemblerAction getActionForCart(BlockState state, AbstractMinecartEntity cart) {
		CartAssembleRailType type = state.get(RAIL_TYPE);
		boolean powered = state.get(POWERED);
		switch (type) {
		case ACTIVATOR_RAIL:
			return powered ? CartAssemblerAction.DISASSEMBLE : CartAssemblerAction.PASS;
		case CONTROLLER_RAIL:
			return powered ? CartAssemblerAction.ASSEMBLE_ACCELERATE_DIRECTIONAL
				: CartAssemblerAction.DISASSEMBLE_BRAKE;
		case DETECTOR_RAIL:
			return cart.getPassengerList()
				.isEmpty() ? CartAssemblerAction.ASSEMBLE_ACCELERATE : CartAssemblerAction.DISASSEMBLE;
		case POWERED_RAIL:
			return powered ? CartAssemblerAction.ASSEMBLE_ACCELERATE : CartAssemblerAction.DISASSEMBLE_BRAKE;
		case REGULAR:
			return powered ? CartAssemblerAction.ASSEMBLE : CartAssemblerAction.DISASSEMBLE;
		default:
			return CartAssemblerAction.PASS;
		}
	}

	public static boolean canAssembleTo(AbstractMinecartEntity cart) {
		return cart.getMinecartType() == AbstractMinecartEntity.Type.RIDEABLE || cart instanceof FurnaceMinecartEntity || cart instanceof ChestMinecartEntity;
	}

	@Override
	@Nonnull
	public ActionResult onUse(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, PlayerEntity player,
		@Nonnull Hand hand, @Nonnull BlockHitResult blockRayTraceResult) {

		ItemStack itemStack = player.getStackInHand(hand);
		Item previousItem = getRailItem(state);
		Item heldItem = itemStack.getItem();
		if (heldItem != previousItem) {

			CartAssembleRailType newType = null;
			for (CartAssembleRailType type : CartAssembleRailType.values())
				if (heldItem == type.getItem())
					newType = type;
			if (newType == null)
				return ActionResult.PASS;
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1, 1);
			world.setBlockState(pos, state.with(RAIL_TYPE, newType));

			if (!player.isCreative()) {
				itemStack.decrement(1);
				player.getInventory()
					.offerOrDrop(new ItemStack(previousItem));
			}
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	@Override
	public void neighborUpdate(@Nonnull BlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos,
		@Nonnull Block blockIn, @Nonnull BlockPos fromPos, boolean isMoving) {
		if (worldIn.isClient)
			return;
		boolean previouslyPowered = state.get(POWERED);
		if (previouslyPowered != worldIn.isReceivingRedstonePower(pos))
			worldIn.setBlockState(pos, state.cycle(POWERED), 2);
		super.neighborUpdate(state, worldIn, pos, blockIn, fromPos, isMoving);
	}

	@Override
	@Nonnull
	public Property<RailShape> getShapeProperty() {
		return RAIL_SHAPE;
	}

	@Override
	@Nonnull
	public VoxelShape getOutlineShape(BlockState state, @Nonnull BlockView worldIn, @Nonnull BlockPos pos,
		@Nonnull ShapeContext context) {
		return AllShapes.CART_ASSEMBLER.get(getRailAxis(state));
	}

	protected Axis getRailAxis(BlockState state) {
		return state.get(RAIL_SHAPE) == RailShape.NORTH_SOUTH ? Direction.Axis.Z : Direction.Axis.X;
	}

	@Override
	@Nonnull
	public VoxelShape getCollisionShape(@Nonnull BlockState state, @Nonnull BlockView worldIn, @Nonnull BlockPos pos,
		ShapeContext context) {
		if (context instanceof EntityShapeContext) {
			Entity entity = ((EntityShapeContext) context).getEntity();
			if (entity instanceof AbstractMinecartEntity)
				return VoxelShapes.empty();
			if (entity instanceof PlayerEntity)
				return AllShapes.CART_ASSEMBLER_PLAYER_COLLISION.get(getRailAxis(state));
		}
		return VoxelShapes.fullCube();
	}

	@Override
	public Class<CartAssemblerBlockEntity> getBlockEntityClass() {
		return CartAssemblerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CartAssemblerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.CART_ASSEMBLER.get();
	}

	@Override
	public boolean canPlaceAt(@Nonnull BlockState state, @Nonnull WorldView world, @Nonnull BlockPos pos) {
		return false;
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		ArrayList<ItemStack> requiredItems = new ArrayList<>();
		requiredItems.add(new ItemStack(getRailItem(state)));
		requiredItems.add(new ItemStack(asItem()));
		return new ItemRequirement(ItemUseType.CONSUME, requiredItems);
	}

	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
		List<ItemStack> drops = super.getDroppedStacks(state, builder);
		drops.addAll(getRailBlock(state).getDroppedStacks(builder));
		return drops;
	}

	@SuppressWarnings("deprecation")
	public List<ItemStack> getDropsNoRail(BlockState state, ServerWorld world, BlockPos pos,
		@Nullable BlockEntity p_220077_3_, @Nullable Entity p_220077_4_, ItemStack p_220077_5_) {
		return super.getDroppedStacks(state,
			(new LootContextParameterSet.Builder(world)).add(LootContextParameters.ORIGIN, Vec3d.of(pos))
				.add(LootContextParameters.TOOL, p_220077_5_)
				.addOptional(LootContextParameters.THIS_ENTITY, p_220077_4_)
				.addOptional(LootContextParameters.BLOCK_ENTITY, p_220077_3_));
	}

	@Override
	public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		PlayerEntity player = context.getPlayer();
		if (world.isClient)
			return ActionResult.SUCCESS;
		if (player != null && !player.isCreative())
			getDropsNoRail(state, (ServerWorld) world, pos, world.getBlockEntity(pos), player, context.getStack())
				.forEach(itemStack -> player.getInventory()
					.offerOrDrop(itemStack));
		if (world instanceof ServerWorld)
			state.onStacksDropped((ServerWorld) world, pos, ItemStack.EMPTY, true);
		world.setBlockState(pos, getRailBlock(state));
		return ActionResult.SUCCESS;
	}

	public static class MinecartAnchorBlock extends Block {

		public MinecartAnchorBlock(Settings p_i48440_1_) {
			super(p_i48440_1_);
		}

		@Override
		protected void appendProperties(Builder<Block, BlockState> builder) {
			builder.add(Properties.HORIZONTAL_AXIS);
			super.appendProperties(builder);
		}

		@Override
		@Nonnull
		public VoxelShape getOutlineShape(@Nonnull BlockState p_220053_1_, @Nonnull BlockView p_220053_2_,
			@Nonnull BlockPos p_220053_3_, @Nonnull ShapeContext p_220053_4_) {
			return VoxelShapes.empty();
		}
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		World world = context.getWorld();
		if (world.isClient)
			return ActionResult.SUCCESS;
		BlockPos pos = context.getBlockPos();
		world.setBlockState(pos, rotate(state, BlockRotation.CLOCKWISE_90), 3);
		world.updateNeighborsAlways(pos.down(), this);
		return ActionResult.SUCCESS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		if (rotation == BlockRotation.NONE)
			return state;
		BlockState base = AllBlocks.CONTROLLER_RAIL.getDefaultState()
			.with(ControllerRailBlock.SHAPE, state.get(RAIL_SHAPE))
			.with(ControllerRailBlock.BACKWARDS, state.get(BACKWARDS))
			.rotate(rotation);
		return state.with(RAIL_SHAPE, base.get(ControllerRailBlock.SHAPE))
			.with(BACKWARDS, base.get(ControllerRailBlock.BACKWARDS));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		if (mirror == BlockMirror.NONE)
			return state;
		BlockState base = AllBlocks.CONTROLLER_RAIL.getDefaultState()
			.with(ControllerRailBlock.SHAPE, state.get(RAIL_SHAPE))
			.with(ControllerRailBlock.BACKWARDS, state.get(BACKWARDS))
			.mirror(mirror);
		return state.with(BACKWARDS, base.get(ControllerRailBlock.BACKWARDS));
	}

	public static Direction getHorizontalDirection(BlockState blockState) {
		if (!(blockState.getBlock() instanceof CartAssemblerBlock))
			return Direction.SOUTH;
		Direction pointingTo = getPointingTowards(blockState);
		return blockState.get(BACKWARDS) ? pointingTo.getOpposite() : pointingTo;
	}

	private static Direction getPointingTowards(BlockState state) {
		switch (state.get(RAIL_SHAPE)) {
		case EAST_WEST:
			return Direction.WEST;
		default:
			return Direction.NORTH;
		}
	}
}
