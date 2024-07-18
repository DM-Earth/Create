package com.simibubi.create.content.contraptions.actors.seat;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Optional;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTags.AllEntityTags;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.github.fabricators_of_create.porting_lib.util.TagUtil;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SeatBlock extends Block implements ProperWaterloggedBlock {

	protected final DyeColor color;

	public SeatBlock(Settings properties, DyeColor color) {
		super(properties);
		this.color = color;
		setDefaultState(getDefaultState().with(WATERLOGGED, false));
		LandPathNodeTypesRegistry.register(this, PathNodeType.RAIL, null);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(WATERLOGGED));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		return withWater(super.getPlacementState(pContext), pContext);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pDirection, BlockState pNeighborState,
		WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return pState;
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	public void onLandedUpon(World p_152426_, BlockState p_152427_, BlockPos p_152428_, Entity p_152429_, float p_152430_) {
		super.onLandedUpon(p_152426_, p_152427_, p_152428_, p_152429_, p_152430_ * 0.5F);
	}

	@Override
	public void onEntityLand(BlockView reader, Entity entity) {
		BlockPos pos = entity.getBlockPos();
		if (entity instanceof PlayerEntity || !(entity instanceof LivingEntity) || !canBePickedUp(entity)
			|| isSeatOccupied(entity.getWorld(), pos)) {
			if (entity.bypassesLandingEffects()) {
				super.onEntityLand(reader, entity);
				return;
			}

			Vec3d vec3 = entity.getVelocity();
			if (vec3.y < 0.0D) {
				double d0 = entity instanceof LivingEntity ? 1.0D : 0.8D;
				entity.setVelocity(vec3.x, -vec3.y * (double) 0.66F * d0, vec3.z);
			}

			return;
		}
		if (reader.getBlockState(pos)
			.getBlock() != this)
			return;
		sitDown(entity.getWorld(), pos, entity);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState p_220053_1_, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		return AllShapes.SEAT;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState p_220071_1_, BlockView p_220071_2_, BlockPos p_220071_3_,
		ShapeContext ctx) {
		if (ctx instanceof EntityShapeContext ecc && ecc.getEntity() instanceof PlayerEntity)
			return AllShapes.SEAT_COLLISION_PLAYERS;
		return AllShapes.SEAT_COLLISION;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult p_225533_6_) {
		if (player.isSneaking())
			return ActionResult.PASS;

		ItemStack heldItem = player.getStackInHand(hand);
		DyeColor color = TagUtil.getColorFromStack(heldItem);
		if (color != null && color != this.color) {
			if (world.isClient)
				return ActionResult.SUCCESS;
			BlockState newState = BlockHelper.copyProperties(state, AllBlocks.SEATS.get(color)
				.getDefaultState());
			world.setBlockState(pos, newState);
			return ActionResult.SUCCESS;
		}

		List<SeatEntity> seats = world.getNonSpectatingEntities(SeatEntity.class, new Box(pos));
		if (!seats.isEmpty()) {
			SeatEntity seatEntity = seats.get(0);
			List<Entity> passengers = seatEntity.getPassengerList();
			if (!passengers.isEmpty() && !canReplaceSeatedEntity(passengers.get(0), player))
				return ActionResult.PASS;
			if (!world.isClient) {
				seatEntity.removeAllPassengers();
				player.startRiding(seatEntity);
			}
			return ActionResult.SUCCESS;
		}

		if (world.isClient)
			return ActionResult.SUCCESS;
		sitDown(world, pos, getLeashed(world, player).or(player));
		return ActionResult.SUCCESS;
	}

	public static boolean canReplaceSeatedEntity(Entity seated, PlayerEntity player) {
		return !(seated instanceof PlayerEntity) && !AdventureUtil.isAdventure(player);
	}

	public static boolean isSeatOccupied(World world, BlockPos pos) {
		return !world.getNonSpectatingEntities(SeatEntity.class, new Box(pos))
			.isEmpty();
	}

	public static Optional<Entity> getLeashed(World level, PlayerEntity player) {
		List<Entity> entities = player.getWorld().getOtherEntities((Entity) null, player.getBoundingBox()
			.expand(10), e -> true);
		for (Entity e : entities)
			if (e instanceof MobEntity mob && mob.getHoldingEntity() == player && SeatBlock.canBePickedUp(e))
				return Optional.of(mob);
		return Optional.absent();
	}

	public static boolean canBePickedUp(Entity passenger) {
		if (passenger instanceof ShulkerEntity)
			return false;
		if (passenger instanceof PlayerEntity)
			return false;
		if (AllEntityTags.IGNORE_SEAT.matches(passenger))
			return false;
		if (!AllConfigs.server().logistics.seatHostileMobs.get() && !passenger.getType()
			.getSpawnGroup()
			.isPeaceful())
			return false;
		return passenger instanceof LivingEntity;
	}

	public static void sitDown(World world, BlockPos pos, Entity entity) {
		if (world.isClient)
			return;
		SeatEntity seat = new SeatEntity(world, pos);
		seat.setPosition(pos.getX() + .5f, pos.getY(), pos.getZ() + .5f);
		world.spawnEntity(seat);
		entity.startRiding(seat, true);
		if (entity instanceof TameableEntity ta)
			ta.setInSittingPose(true);
	}

	public DyeColor getColor() {
		return color;
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

}
