package com.simibubi.create.content.kinetics.crusher;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.block.CustomRunningEffectsBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class CrushingWheelControllerBlock extends FacingBlock implements IBE<CrushingWheelControllerBlockEntity>, CustomRunningEffectsBlock {

	public CrushingWheelControllerBlock(Settings p_i48440_1_) {
		super(p_i48440_1_);
	}

	public static final BooleanProperty VALID = BooleanProperty.of("valid");

	@Override
	public boolean canReplace(BlockState state, ItemPlacementContext useContext) {
		return false;
	}

	@Override
	public boolean addRunningEffects(BlockState state, World world, BlockPos pos, Entity entity) {
		return true;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(VALID);
		builder.add(FACING);
		super.appendProperties(builder);
	}

	public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
		if (!state.get(VALID))
			return;

		Direction facing = state.get(FACING);
		Axis axis = facing.getAxis();

		checkEntityForProcessing(worldIn, pos, entityIn);

		withBlockEntityDo(worldIn, pos, be -> {
			if (be.processingEntity == entityIn)

				entityIn.slowMovement(state, new Vec3d(axis == Axis.X ? (double) 0.05F : 0.25D,
					axis == Axis.Y ? (double) 0.05F : 0.25D, axis == Axis.Z ? (double) 0.05F : 0.25D));
		});
	}

	public void checkEntityForProcessing(World worldIn, BlockPos pos, Entity entityIn) {
		CrushingWheelControllerBlockEntity be = getBlockEntity(worldIn, pos);
		if (be == null)
			return;
		if (be.crushingspeed == 0)
			return;
//		if (entityIn instanceof ItemEntity)
//			((ItemEntity) entityIn).setPickUpDelay(10);
		NbtCompound data = entityIn.getCustomData();
		if (data.contains("BypassCrushingWheel")) {
			if (pos.equals(NbtHelper.toBlockPos(data.getCompound("BypassCrushingWheel"))))
				return;
		}
		if (be.isOccupied())
			return;
		boolean isPlayer = entityIn instanceof PlayerEntity;
		if (isPlayer && ((PlayerEntity) entityIn).isCreative())
			return;
		if (isPlayer && entityIn.getWorld().getDifficulty() == Difficulty.PEACEFUL)
			return;

		be.startCrushing(entityIn);
	}

	@Override
	public void onEntityLand(BlockView worldIn, Entity entityIn) {
		super.onEntityLand(worldIn, entityIn);
		// Moved to onEntityCollision to allow for omnidirectional input
	}

	@Override
	public void randomDisplayTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		if (!stateIn.get(VALID))
			return;
		if (rand.nextInt(1) != 0)
			return;
		double d0 = (double) ((float) pos.getX() + rand.nextFloat());
		double d1 = (double) ((float) pos.getY() + rand.nextFloat());
		double d2 = (double) ((float) pos.getZ() + rand.nextFloat());
		worldIn.addParticle(ParticleTypes.CRIT, d0, d1, d2, 0.0D, 0.0D, 0.0D);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState stateIn, Direction facing, BlockState facingState, WorldAccess worldIn,
		BlockPos currentPos, BlockPos facingPos) {
		updateSpeed(stateIn, worldIn, currentPos);
		return stateIn;
	}

	public void updateSpeed(BlockState state, WorldAccess world, BlockPos pos) {
		withBlockEntityDo(world, pos, be -> {
			if (!state.get(VALID)) {
				if (be.crushingspeed != 0) {
					be.crushingspeed = 0;
					be.sendData();
				}
				return;
			}

			for (Direction d : Iterate.directions) {
				BlockState neighbour = world.getBlockState(pos.offset(d));
				if (!AllBlocks.CRUSHING_WHEEL.has(neighbour))
					continue;
				if (neighbour.get(Properties.AXIS) == d.getAxis())
					continue;
				BlockEntity adjBE = world.getBlockEntity(pos.offset(d));
				if (!(adjBE instanceof CrushingWheelBlockEntity cwbe))
					continue;
				be.crushingspeed = Math.abs(cwbe.getSpeed() / 50f);
				be.sendData();

				cwbe.award(AllAdvancements.CRUSHING_WHEEL);
				if (cwbe.getSpeed() > 255)
					cwbe.award(AllAdvancements.CRUSHER_MAXED);
				break;
			}
		});
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		VoxelShape standardShape = AllShapes.CRUSHING_WHEEL_CONTROLLER_COLLISION.get(state.get(FACING));

		if (!state.get(VALID))
			return standardShape;
		if (!(context instanceof EntityShapeContext))
			return standardShape;
		Entity entity = ((EntityShapeContext) context).getEntity();
		if (entity == null)
			return standardShape;

		NbtCompound data = entity.getCustomData();
		if (data.contains("BypassCrushingWheel"))
			if (pos.equals(NbtHelper.toBlockPos(data.getCompound("BypassCrushingWheel"))))
				if (state.get(FACING) != Direction.UP) // Allow output items to land on top of the block rather
															// than falling back through.
					return VoxelShapes.empty();

		CrushingWheelControllerBlockEntity be = getBlockEntity(worldIn, pos);
		if (be != null && be.processingEntity == entity)
			return VoxelShapes.empty();

		return standardShape;
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.hasBlockEntity() || state.getBlock() == newState.getBlock())
			return;

		withBlockEntityDo(worldIn, pos, be -> ItemHelper.dropContents(worldIn, pos, be.inventory));
		worldIn.removeBlockEntity(pos);
	}

	@Override
	public Class<CrushingWheelControllerBlockEntity> getBlockEntityClass() {
		return CrushingWheelControllerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CrushingWheelControllerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.CRUSHING_WHEEL_CONTROLLER.get();
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

}
