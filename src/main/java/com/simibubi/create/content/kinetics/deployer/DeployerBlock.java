package com.simibubi.create.content.kinetics.deployer;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.processing.AssemblyOperatorUseContext;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.utility.AdventureUtil;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DeployerBlock extends DirectionalAxisKineticBlock implements IBE<DeployerBlockEntity> {

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public DeployerBlock(Settings properties) {
		super(properties);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.DEPLOYER_INTERACTION.get(state.get(FACING));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.CASING_12PX.get(state.get(FACING));
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		Vec3d normal = Vec3d.of(state.get(FACING)
			.getVector());
		Vec3d location = context.getHitPos()
			.subtract(Vec3d.ofCenter(context.getBlockPos())
				.subtract(normal.multiply(.5)))
			.multiply(normal);
		if (location.length() > .75f) {
			if (!context.getWorld().isClient)
				withBlockEntityDo(context.getWorld(), context.getBlockPos(), DeployerBlockEntity::changeMode);
			return ActionResult.SUCCESS;
		}
		return super.onWrenched(state, context);
	}

	@Override
	public void onPlaced(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.onPlaced(worldIn, pos, state, placer, stack);
		if (placer instanceof ServerPlayerEntity)
			withBlockEntityDo(worldIn, pos, dbe -> dbe.owner = placer.getUuid());
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!isMoving && !state.isOf(newState.getBlock()))
			withBlockEntityDo(worldIn, pos, DeployerBlockEntity::discardPlayer);
		super.onStateReplaced(state, worldIn, pos, newState, isMoving);
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;
		ItemStack heldByPlayer = player.getStackInHand(handIn)
			.copy();

		IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
		if (!player.isSneaking() && player.canModifyBlocks()) {
			if (placementHelper.matchesItem(heldByPlayer) && placementHelper.getOffset(player, worldIn, state, pos, hit)
				.placeInWorld(worldIn, (BlockItem) heldByPlayer.getItem(), player, handIn, hit)
				.isAccepted())
				return ActionResult.SUCCESS;
		}

		if (AllItems.WRENCH.isIn(heldByPlayer))
			return ActionResult.PASS;

		Vec3d normal = Vec3d.of(state.get(FACING)
			.getVector());
		Vec3d location = hit.getPos()
			.subtract(Vec3d.ofCenter(pos)
				.subtract(normal.multiply(.5)))
			.multiply(normal);
		if (location.length() < .75f)
			return ActionResult.PASS;
		if (worldIn.isClient)
			return ActionResult.SUCCESS;

		withBlockEntityDo(worldIn, pos, be -> {
			ItemStack heldByDeployer = be.player.getMainHandStack()
				.copy();
			if (heldByDeployer.isEmpty() && heldByPlayer.isEmpty())
				return;

			player.setStackInHand(handIn, heldByDeployer);
			be.player.setStackInHand(Hand.MAIN_HAND, heldByPlayer);
			be.sendData();
		});

		return ActionResult.SUCCESS;
	}

	@Override
	public Class<DeployerBlockEntity> getBlockEntityClass() {
		return DeployerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DeployerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.DEPLOYER.get();
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onBlockAdded(state, world, pos, oldState, isMoving);
		withBlockEntityDo(world, pos, DeployerBlockEntity::redstoneUpdate);
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block p_220069_4_, BlockPos p_220069_5_,
		boolean p_220069_6_) {
		withBlockEntityDo(world, pos, DeployerBlockEntity::redstoneUpdate);
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	protected Direction getFacingForPlacement(ItemPlacementContext context) {
		if (context instanceof AssemblyOperatorUseContext)
			return Direction.DOWN;
		else
			return super.getFacingForPlacement(context);
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.DEPLOYER::isIn;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return AllBlocks.DEPLOYER::has;
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getPos(),
				state.get(FACING)
					.getAxis(),
				dir -> world.getBlockState(pos.offset(dir))
					.isReplaceable());

			if (directions.isEmpty())
				return PlacementOffset.fail();
			else {
				return PlacementOffset.success(pos.offset(directions.get(0)),
					s -> s.with(FACING, state.get(FACING))
						.with(AXIS_ALONG_FIRST_COORDINATE, state.get(AXIS_ALONG_FIRST_COORDINATE)));
			}
		}

	}

}
