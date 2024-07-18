package com.simibubi.create.content.kinetics.gauge;

import org.joml.Vector3f;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.worldWrappers.WrappedWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class GaugeBlock extends DirectionalAxisKineticBlock implements IBE<GaugeBlockEntity> {

	public static final GaugeShaper GAUGE = GaugeShaper.make();
	protected Type type;

	public enum Type implements StringIdentifiable {
		SPEED, STRESS;

		@Override
		public String asString() {
			return Lang.asId(name());
		}
	}

	public static GaugeBlock speed(Settings properties) {
		return new GaugeBlock(properties, Type.SPEED);
	}

	public static GaugeBlock stress(Settings properties) {
		return new GaugeBlock(properties, Type.STRESS);
	}

	protected GaugeBlock(Settings properties, Type type) {
		super(properties);
		this.type = type;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		World world = context.getWorld();
		Direction face = context.getSide();
		BlockPos placedOnPos = context.getBlockPos()
			.offset(context.getSide()
				.getOpposite());
		BlockState placedOnState = world.getBlockState(placedOnPos);
		Block block = placedOnState.getBlock();

		if (block instanceof IRotate && ((IRotate) block).hasShaftTowards(world, placedOnPos, placedOnState, face)) {
			BlockState toPlace = getDefaultState();
			Direction horizontalFacing = context.getHorizontalPlayerFacing();
			Direction nearestLookingDirection = context.getPlayerLookDirection();
			boolean lookPositive = nearestLookingDirection.getDirection() == AxisDirection.POSITIVE;
			if (face.getAxis() == Axis.X) {
				toPlace = toPlace.with(FACING, lookPositive ? Direction.NORTH : Direction.SOUTH)
					.with(AXIS_ALONG_FIRST_COORDINATE, true);
			} else if (face.getAxis() == Axis.Y) {
				toPlace = toPlace.with(FACING, horizontalFacing.getOpposite())
					.with(AXIS_ALONG_FIRST_COORDINATE, horizontalFacing.getAxis() == Axis.X);
			} else {
				toPlace = toPlace.with(FACING, lookPositive ? Direction.WEST : Direction.EAST)
					.with(AXIS_ALONG_FIRST_COORDINATE, false);
			}

			return toPlace;
		}

		return super.getPlacementState(context);
	}

	@Override
	protected Direction getFacingForPlacement(ItemPlacementContext context) {
		return context.getSide();
	}

	@Override
	protected boolean getAxisAlignmentForPlacement(ItemPlacementContext context) {
		return context.getHorizontalPlayerFacing()
			.getAxis() != Axis.X;
	}

	public boolean shouldRenderHeadOnFace(World world, BlockPos pos, BlockState state, Direction face) {
		if (face.getAxis()
			.isVertical())
			return false;
		if (face == state.get(FACING)
			.getOpposite())
			return false;
		if (face.getAxis() == getRotationAxis(state))
			return false;
		if (getRotationAxis(state) == Axis.Y && face != state.get(FACING))
			return false;
		if (!Block.shouldDrawSide(state, world, pos, face, pos.offset(face)) && !(world instanceof WrappedWorld))
			return false;
		return true;
	}

	@Override
	public void randomDisplayTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		BlockEntity be = worldIn.getBlockEntity(pos);
		if (be == null || !(be instanceof GaugeBlockEntity))
			return;
		GaugeBlockEntity gaugeBE = (GaugeBlockEntity) be;
		if (gaugeBE.dialTarget == 0)
			return;
		int color = gaugeBE.color;

		for (Direction face : Iterate.directions) {
			if (!shouldRenderHeadOnFace(worldIn, pos, stateIn, face))
				continue;

			Vector3f rgb = new Color(color).asVectorF();
			Vec3d faceVec = Vec3d.of(face.getVector());
			Direction positiveFacing = Direction.get(AxisDirection.POSITIVE, face.getAxis());
			Vec3d positiveFaceVec = Vec3d.of(positiveFacing.getVector());
			int particleCount = gaugeBE.dialTarget > 1 ? 4 : 1;

			if (particleCount == 1 && rand.nextFloat() > 1 / 4f)
				continue;

			for (int i = 0; i < particleCount; i++) {
				Vec3d mul = VecHelper.offsetRandomly(Vec3d.ZERO, rand, .25f)
					.multiply(new Vec3d(1, 1, 1).subtract(positiveFaceVec))
					.normalize()
					.multiply(.3f);
				Vec3d offset = VecHelper.getCenterOf(pos)
					.add(faceVec.multiply(.55))
					.add(mul);
				worldIn.addParticle(new DustParticleEffect(rgb, 1), offset.x, offset.y, offset.z, mul.x, mul.y, mul.z);
			}

		}

	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return GAUGE.get(state.get(FACING), state.get(AXIS_ALONG_FIRST_COORDINATE));
	}

	@Override
	public boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState blockState, World worldIn, BlockPos pos) {
		BlockEntity be = worldIn.getBlockEntity(pos);
		if (be instanceof GaugeBlockEntity) {
			GaugeBlockEntity gaugeBlockEntity = (GaugeBlockEntity) be;
			return MathHelper.ceil(MathHelper.clamp(gaugeBlockEntity.dialTarget * 14, 0, 15));
		}
		return 0;
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public Class<GaugeBlockEntity> getBlockEntityClass() {
		return GaugeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends GaugeBlockEntity> getBlockEntityType() {
		return type == Type.SPEED ? AllBlockEntityTypes.SPEEDOMETER.get() : AllBlockEntityTypes.STRESSOMETER.get();
	}
}
