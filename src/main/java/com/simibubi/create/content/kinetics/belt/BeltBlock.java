package com.simibubi.create.content.kinetics.belt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import io.github.fabricators_of_create.porting_lib.tags.Tags;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.ITransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.armor.DivingBootsItem;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltSlicer.Feedback;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler.TransportedEntityInfo;
import com.simibubi.create.content.kinetics.belt.transport.BeltTunnelInteractionHandler;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import com.simibubi.create.foundation.block.render.ReducedDestroyEffects;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.util.TagUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
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
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.chunk.DebugChunkGenerator;

public class BeltBlock extends HorizontalKineticBlock
	implements IBE<BeltBlockEntity>, ISpecialBlockItemRequirement, ITransformableBlock, ProperWaterloggedBlock,
		BlockPickInteractionAware, ReducedDestroyEffects, MultiPosDestructionHandler {

	public static final Property<BeltSlope> SLOPE = EnumProperty.of("slope", BeltSlope.class);
	public static final Property<BeltPart> PART = EnumProperty.of("part", BeltPart.class);
	public static final BooleanProperty CASING = BooleanProperty.of("casing");

	public BeltBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(SLOPE, BeltSlope.HORIZONTAL)
			.with(PART, BeltPart.START)
			.with(CASING, false)
			.with(WATERLOGGED, false));
		LandPathNodeTypesRegistry.register(this, PathNodeType.RAIL, null);
	}

	@Override
	protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
		return super.areStatesKineticallyEquivalent(oldState, newState)
			&& oldState.get(PART) == newState.get(PART);
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		if (face.getAxis() != getRotationAxis(state))
			return false;
		return getBlockEntityOptional(world, pos).map(BeltBlockEntity::hasPulley)
			.orElse(false);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		if (state.get(SLOPE) == BeltSlope.SIDEWAYS)
			return Axis.Y;
		return state.get(HORIZONTAL_FACING)
			.rotateYClockwise()
			.getAxis();
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockView world, BlockPos pos,
		PlayerEntity player, HitResult target) {
		return AllItems.BELT_CONNECTOR.asStack();
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
		List<ItemStack> drops = super.getDroppedStacks(state, builder);
		BlockEntity blockEntity = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
		if (blockEntity instanceof BeltBlockEntity && ((BeltBlockEntity) blockEntity).hasPulley())
			drops.addAll(AllBlocks.SHAFT.getDefaultState()
				.getDroppedStacks(builder));
		return drops;
	}

	@Override
	public void onStacksDropped(BlockState state, ServerWorld worldIn, BlockPos pos, ItemStack p_220062_4_, boolean b) {
		BeltBlockEntity controllerBE = BeltHelper.getControllerBE(worldIn, pos);
		if (controllerBE != null)
			controllerBE.getInventory()
				.ejectAll();
	}

//	@Override
//	public boolean isFlammable(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
//		return false;
//	}

	@Override
	public void onEntityLand(BlockView worldIn, Entity entityIn) {
		super.onEntityLand(worldIn, entityIn);
		BlockPos entityPosition = entityIn.getBlockPos();
		BlockPos beltPos = null;

		if (AllBlocks.BELT.has(worldIn.getBlockState(entityPosition)))
			beltPos = entityPosition;
		else if (AllBlocks.BELT.has(worldIn.getBlockState(entityPosition.down())))
			beltPos = entityPosition.down();
		if (beltPos == null)
			return;
		if (!(worldIn instanceof World))
			return;

		onEntityCollision(worldIn.getBlockState(beltPos), (World) worldIn, beltPos, entityIn);
	}

	@Override
	public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
		if (!canTransportObjects(state))
			return;
		if (entityIn instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) entityIn;
			if (player.isSneaking())
				return;
			if (player.getAbilities().flying)
				return;
		}

		if (DivingBootsItem.isWornBy(entityIn))
			return;

		BeltBlockEntity belt = BeltHelper.getSegmentBE(worldIn, pos);
		if (belt == null)
			return;
		if (entityIn instanceof ItemEntity && entityIn.isAlive()) {
			if (worldIn.isClient)
				return;
			if (entityIn.getVelocity().y > 0)
				return;
			if (!entityIn.isAlive())
				return;
			if (BeltTunnelInteractionHandler.getTunnelOnPosition(worldIn, pos) != null)
				return;
			withBlockEntityDo(worldIn, pos, be -> {
				ItemEntity itemEntity = (ItemEntity) entityIn;
				Storage<ItemVariant> handler = be.getItemStorage(null);
				if (handler == null)
					return;
				ItemStack inEntity = itemEntity.getStack();
				try (Transaction t = TransferUtil.getTransaction()) {
					long inserted = handler.insert(ItemVariant.of(inEntity), inEntity.getCount(), t);
					if (inserted == 0)
						return;
					if (inEntity.getCount() == inserted) {
						itemEntity.discard();
					} else {
						inEntity.decrement((int) inserted);
					}
					t.commit();
				}
			});
			return;
		}

		BeltBlockEntity controller = BeltHelper.getControllerBE(worldIn, pos);
		if (controller == null || controller.passengers == null)
			return;
		if (controller.passengers.containsKey(entityIn)) {
			TransportedEntityInfo info = controller.passengers.get(entityIn);
			if (info.getTicksSinceLastCollision() != 0 || pos.equals(entityIn.getBlockPos()))
				info.refresh(pos, state);
		} else {
			controller.passengers.put(entityIn, new TransportedEntityInfo(pos, state));
			entityIn.setOnGround(true);
		}
	}

	public static boolean canTransportObjects(BlockState state) {
		if (!AllBlocks.BELT.has(state))
			return false;
		BeltSlope slope = state.get(SLOPE);
		return slope != BeltSlope.VERTICAL && slope != BeltSlope.SIDEWAYS;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (player.isSneaking() || !player.canModifyBlocks())
			return ActionResult.PASS;
		ItemStack heldItem = player.getStackInHand(handIn);

		boolean isWrench = AllItems.WRENCH.isIn(heldItem);
		boolean isConnector = AllItems.BELT_CONNECTOR.isIn(heldItem);
		boolean isShaft = AllBlocks.SHAFT.isIn(heldItem);
		boolean isDye = heldItem.isIn(Tags.Items.DYES);
		boolean hasWater = GenericItemEmptying.emptyItem(world, heldItem, true)
			.getFirst()
			.getFluid()
			.matchesType(Fluids.WATER);
		boolean isHand = heldItem.isEmpty() && handIn == Hand.MAIN_HAND;

		if (isDye || hasWater)
			return onBlockEntityUse(world, pos,
				be -> be.applyColor(TagUtil.getColorFromStack(heldItem)) ? ActionResult.SUCCESS : ActionResult.PASS);

		if (isConnector)
			return BeltSlicer.useConnector(state, world, pos, player, handIn, hit, new Feedback());
		if (isWrench)
			return BeltSlicer.useWrench(state, world, pos, player, handIn, hit, new Feedback());

		BeltBlockEntity belt = BeltHelper.getSegmentBE(world, pos);
		if (belt == null)
			return ActionResult.PASS;

		if (isHand) {
			BeltBlockEntity controllerBelt = belt.getControllerBE();
			if (controllerBelt == null)
				return ActionResult.PASS;
			if (world.isClient)
				return ActionResult.SUCCESS;
			MutableBoolean success = new MutableBoolean(false);
			controllerBelt.getInventory()
				.applyToEachWithin(belt.index + .5f, .55f, (transportedItemStack) -> {
					player.getInventory()
						.offerOrDrop(transportedItemStack.stack);
					success.setTrue();
					return TransportedResult.removeItem();
				});
			if (success.isTrue())
				world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .2f,
					1f + Create.RANDOM.nextFloat());
		}

		if (isShaft) {
			if (state.get(PART) != BeltPart.MIDDLE)
				return ActionResult.PASS;
			if (world.isClient)
				return ActionResult.SUCCESS;
			if (!player.isCreative())
				heldItem.decrement(1);
			KineticBlockEntity.switchToBlockState(world, pos, state.with(PART, BeltPart.PULLEY));
			return ActionResult.SUCCESS;
		}

		if (AllBlocks.BRASS_CASING.isIn(heldItem)) {
			withBlockEntityDo(world, pos, be -> be.setCasingType(CasingType.BRASS));
			updateCoverProperty(world, pos, world.getBlockState(pos));
			return ActionResult.SUCCESS;
		}

		if (AllBlocks.ANDESITE_CASING.isIn(heldItem)) {
			withBlockEntityDo(world, pos, be -> be.setCasingType(CasingType.ANDESITE));
			updateCoverProperty(world, pos, world.getBlockState(pos));
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();
		BlockPos pos = context.getBlockPos();

		if (state.get(CASING)) {
			if (world.isClient)
				return ActionResult.SUCCESS;
			withBlockEntityDo(world, pos, be -> be.setCasingType(CasingType.NONE));
			return ActionResult.SUCCESS;
		}

		if (state.get(PART) == BeltPart.PULLEY) {
			if (world.isClient)
				return ActionResult.SUCCESS;
			KineticBlockEntity.switchToBlockState(world, pos, state.with(PART, BeltPart.MIDDLE));
			if (player != null && !player.isCreative())
				player.getInventory()
					.offerOrDrop(AllBlocks.SHAFT.asStack());
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(SLOPE, PART, CASING, WATERLOGGED);
		super.appendProperties(builder);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return BeltShapes.getShape(state);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		if (state.getBlock() != this)
			return VoxelShapes.empty();

		VoxelShape shape = getOutlineShape(state, worldIn, pos, context);
		if (!(context instanceof EntityShapeContext))
			return shape;

		return getBlockEntityOptional(worldIn, pos).map(be -> {
			Entity entity = ((EntityShapeContext) context).getEntity();
			if (entity == null)
				return shape;

			BeltBlockEntity controller = be.getControllerBE();
			if (controller == null)
				return shape;
			if (controller.passengers == null || !controller.passengers.containsKey(entity))
				return BeltShapes.getCollisionShape(state);
			return shape;

		})
			.orElse(shape);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return state.get(CASING) ? BlockRenderType.MODEL : BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	public static void initBelt(World world, BlockPos pos) {
		if (world.isClient)
			return;
		if (world instanceof ServerWorld && ((ServerWorld) world).getChunkManager()
			.getChunkGenerator() instanceof DebugChunkGenerator)
			return;

		BlockState state = world.getBlockState(pos);
		if (!AllBlocks.BELT.has(state))
			return;
		// Find controller
		int limit = 1000;
		BlockPos currentPos = pos;
		while (limit-- > 0) {
			BlockState currentState = world.getBlockState(currentPos);
			if (!AllBlocks.BELT.has(currentState)) {
				world.breakBlock(pos, true);
				return;
			}
			BlockPos nextSegmentPosition = nextSegmentPosition(currentState, currentPos, false);
			if (nextSegmentPosition == null)
				break;
			if (!world.canSetBlock(nextSegmentPosition))
				return;
			currentPos = nextSegmentPosition;
		}

		// Init belts
		int index = 0;
		List<BlockPos> beltChain = getBeltChain(world, currentPos);
		if (beltChain.size() < 2) {
			world.breakBlock(currentPos, true);
			return;
		}

		for (BlockPos beltPos : beltChain) {
			BlockEntity blockEntity = world.getBlockEntity(beltPos);
			BlockState currentState = world.getBlockState(beltPos);

			if (blockEntity instanceof BeltBlockEntity && AllBlocks.BELT.has(currentState)) {
				BeltBlockEntity be = (BeltBlockEntity) blockEntity;
				be.setController(currentPos);
				be.beltLength = beltChain.size();
				be.index = index;
				be.attachKinetics();
				be.markDirty();
				be.sendData();

				if (be.isController() && !canTransportObjects(currentState))
					be.getInventory()
						.ejectAll();
			} else {
				world.breakBlock(currentPos, true);
				return;
			}
			index++;
		}

	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onStateReplaced(state, world, pos, newState, isMoving);

		if (world.isClient)
			return;
		if (state.getBlock() == newState.getBlock())
			return;
		if (isMoving)
			return;

		// Destroy chain
		for (boolean forward : Iterate.trueAndFalse) {
			BlockPos currentPos = nextSegmentPosition(state, pos, forward);
			if (currentPos == null)
				continue;
			BlockState currentState = world.getBlockState(currentPos);
			if (!AllBlocks.BELT.has(currentState))
				continue;

			boolean hasPulley = false;
			BlockEntity blockEntity = world.getBlockEntity(currentPos);
			if (blockEntity instanceof BeltBlockEntity) {
				BeltBlockEntity belt = (BeltBlockEntity) blockEntity;
				if (belt.isController())
					belt.getInventory()
						.ejectAll();

				hasPulley = belt.hasPulley();
			}

			world.removeBlockEntity(currentPos);
			BlockState shaftState = AllBlocks.SHAFT.getDefaultState()
				.with(Properties.AXIS, getRotationAxis(currentState));
			world.setBlockState(currentPos, ProperWaterloggedBlock.withWater(world,
				hasPulley ? shaftState : Blocks.AIR.getDefaultState(), currentPos), 3);
			world.syncWorldEvent(2001, currentPos, Block.getRawIdFromState(currentState));
		}
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction side, BlockState p_196271_3_, WorldAccess world,
		BlockPos pos, BlockPos p_196271_6_) {
		updateWater(world, state, pos);
		if (side.getAxis()
			.isHorizontal())
			updateTunnelConnections(world, pos.up());
		if (side == Direction.UP)
			updateCoverProperty(world, pos, state);
		return state;
	}

	public void updateCoverProperty(WorldAccess world, BlockPos pos, BlockState state) {
		if (world.isClient())
			return;
		if (state.get(CASING) && state.get(SLOPE) == BeltSlope.HORIZONTAL)
			withBlockEntityDo(world, pos, bbe -> bbe.setCovered(isBlockCoveringBelt(world, pos.up())));
	}

	public static boolean isBlockCoveringBelt(WorldAccess world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		VoxelShape collisionShape = blockState.getCollisionShape(world, pos);
		if (collisionShape.isEmpty())
			return false;
		Box bounds = collisionShape.getBoundingBox();
		if (bounds.getXLength() < .5f || bounds.getZLength() < .5f)
			return false;
		if (bounds.minY > 0)
			return false;
		if (AllBlocks.CRUSHING_WHEEL_CONTROLLER.has(blockState))
			return false;
		if (FunnelBlock.isFunnel(blockState) && FunnelBlock.getFunnelFacing(blockState) != Direction.UP)
			return false;
		if (blockState.getBlock() instanceof BeltTunnelBlock)
			return false;
		return true;
	}

	private void updateTunnelConnections(WorldAccess world, BlockPos pos) {
		Block tunnelBlock = world.getBlockState(pos)
			.getBlock();
		if (tunnelBlock instanceof BeltTunnelBlock)
			((BeltTunnelBlock) tunnelBlock).updateTunnel(world, pos);
	}

	public static List<BlockPos> getBeltChain(World world, BlockPos controllerPos) {
		List<BlockPos> positions = new LinkedList<>();

		BlockState blockState = world.getBlockState(controllerPos);
		if (!AllBlocks.BELT.has(blockState))
			return positions;

		int limit = 1000;
		BlockPos current = controllerPos;
		while (limit-- > 0 && current != null) {
			BlockState state = world.getBlockState(current);
			if (!AllBlocks.BELT.has(state))
				break;
			positions.add(current);
			current = nextSegmentPosition(state, current, true);
		}

		return positions;
	}

	public static BlockPos nextSegmentPosition(BlockState state, BlockPos pos, boolean forward) {
		Direction direction = state.get(HORIZONTAL_FACING);
		BeltSlope slope = state.get(SLOPE);
		BeltPart part = state.get(PART);

		int offset = forward ? 1 : -1;

		if (part == BeltPart.END && forward || part == BeltPart.START && !forward)
			return null;
		if (slope == BeltSlope.VERTICAL)
			return pos.up(direction.getDirection() == AxisDirection.POSITIVE ? offset : -offset);
		pos = pos.offset(direction, offset);
		if (slope != BeltSlope.HORIZONTAL && slope != BeltSlope.SIDEWAYS)
			return pos.up(slope == BeltSlope.UPWARD ? offset : -offset);
		return pos;
	}

	@Override
	public Class<BeltBlockEntity> getBlockEntityClass() {
		return BeltBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends BeltBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.BELT.get();
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		List<ItemStack> required = new ArrayList<>();
		if (state.get(PART) != BeltPart.MIDDLE)
			required.add(AllBlocks.SHAFT.asStack());
		if (state.get(PART) == BeltPart.START)
			required.add(AllItems.BELT_CONNECTOR.asStack());
		if (required.isEmpty())
			return ItemRequirement.NONE;
		return new ItemRequirement(ItemUseType.CONSUME, required);
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rot) {
		BlockState rotate = super.rotate(state, rot);

		if (state.get(SLOPE) != BeltSlope.VERTICAL)
			return rotate;
		if (state.get(HORIZONTAL_FACING)
			.getDirection() != rotate.get(HORIZONTAL_FACING)
				.getDirection()) {
			if (state.get(PART) == BeltPart.START)
				return rotate.with(PART, BeltPart.END);
			if (state.get(PART) == BeltPart.END)
				return rotate.with(PART, BeltPart.START);
		}

		return rotate;
	}

	public BlockState transform(BlockState state, StructureTransform transform) {
		if (transform.mirror != null) {
			state = mirror(state, transform.mirror);
		}

		if (transform.rotationAxis == Direction.Axis.Y) {
			return rotate(state, transform.rotation);
		}
		return transformInner(state, transform);
	}

	protected BlockState transformInner(BlockState state, StructureTransform transform) {
		boolean halfTurn = transform.rotation == BlockRotation.CLOCKWISE_180;

		Direction initialDirection = state.get(HORIZONTAL_FACING);
		boolean diagonal =
			state.get(SLOPE) == BeltSlope.DOWNWARD || state.get(SLOPE) == BeltSlope.UPWARD;

		if (!diagonal) {
			for (int i = 0; i < transform.rotation.ordinal(); i++) {
				Direction direction = state.get(HORIZONTAL_FACING);
				BeltSlope slope = state.get(SLOPE);
				boolean vertical = slope == BeltSlope.VERTICAL;
				boolean horizontal = slope == BeltSlope.HORIZONTAL;
				boolean sideways = slope == BeltSlope.SIDEWAYS;

				Direction newDirection = direction.getOpposite();
				BeltSlope newSlope = BeltSlope.VERTICAL;

				if (vertical) {
					if (direction.getAxis() == transform.rotationAxis) {
						newDirection = direction.rotateYCounterclockwise();
						newSlope = BeltSlope.SIDEWAYS;
					} else {
						newSlope = BeltSlope.HORIZONTAL;
						newDirection = direction;
						if (direction.getAxis() == Axis.Z)
							newDirection = direction.getOpposite();
					}
				}

				if (sideways) {
					newDirection = direction;
					if (direction.getAxis() == transform.rotationAxis)
						newSlope = BeltSlope.HORIZONTAL;
					else
						newDirection = direction.rotateYCounterclockwise();
				}

				if (horizontal) {
					newDirection = direction;
					if (direction.getAxis() == transform.rotationAxis)
						newSlope = BeltSlope.SIDEWAYS;
					else if (direction.getAxis() != Axis.Z)
						newDirection = direction.getOpposite();
				}

				state = state.with(HORIZONTAL_FACING, newDirection);
				state = state.with(SLOPE, newSlope);
			}

		} else if (initialDirection.getAxis() != transform.rotationAxis) {
			for (int i = 0; i < transform.rotation.ordinal(); i++) {
				Direction direction = state.get(HORIZONTAL_FACING);
				Direction newDirection = direction.getOpposite();
				BeltSlope slope = state.get(SLOPE);
				boolean upward = slope == BeltSlope.UPWARD;
				boolean downward = slope == BeltSlope.DOWNWARD;

				// Rotate diagonal
				if (direction.getDirection() == AxisDirection.POSITIVE ^ downward ^ direction.getAxis() == Axis.Z) {
					state = state.with(SLOPE, upward ? BeltSlope.DOWNWARD : BeltSlope.UPWARD);
				} else {
					state = state.with(HORIZONTAL_FACING, newDirection);
				}
			}

		} else if (halfTurn) {
			Direction direction = state.get(HORIZONTAL_FACING);
			Direction newDirection = direction.getOpposite();
			BeltSlope slope = state.get(SLOPE);
			boolean vertical = slope == BeltSlope.VERTICAL;

			if (diagonal) {
				state = state.with(SLOPE, slope == BeltSlope.UPWARD ? BeltSlope.DOWNWARD
					: slope == BeltSlope.DOWNWARD ? BeltSlope.UPWARD : slope);
			} else if (vertical) {
				state = state.with(HORIZONTAL_FACING, newDirection);
			}
		}

		return state;
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

//	public static class RenderProperties extends ReducedDestroyEffects implements MultiPosDestructionHandler {
		@Override
		@Environment(EnvType.CLIENT)
		public Set<BlockPos> getExtraPositions(ClientWorld level, BlockPos pos, BlockState blockState, int progress) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof BeltBlockEntity belt) {
				return new HashSet<>(BeltBlock.getBeltChain(level, belt.getController()));
			}
			return null;
		}
//	}

}
