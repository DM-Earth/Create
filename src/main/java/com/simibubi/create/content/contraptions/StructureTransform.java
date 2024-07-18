package com.simibubi.create.content.contraptions;

import static net.minecraft.state.property.Properties.AXIS;
import static net.minecraft.state.property.Properties.FACING;
import static net.minecraft.state.property.Properties.HORIZONTAL_FACING;

import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BellBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;

public class StructureTransform {

	// Assuming structures cannot be rotated around multiple axes at once
	public Axis rotationAxis;
	public BlockPos offset;
	public int angle;
	public BlockRotation rotation;
	public BlockMirror mirror;

	private StructureTransform(BlockPos offset, int angle, Axis axis, BlockRotation rotation, BlockMirror mirror) {
		this.offset = offset;
		this.angle = angle;
		rotationAxis = axis;
		this.rotation = rotation;
		this.mirror = mirror;
	}

	public StructureTransform(BlockPos offset, Axis axis, BlockRotation rotation, BlockMirror mirror) {
		this(offset, rotation == BlockRotation.NONE ? 0 : (4 - rotation.ordinal()) * 90, axis, rotation, mirror);
	}

	public StructureTransform(BlockPos offset, float xRotation, float yRotation, float zRotation) {
		this.offset = offset;
		if (xRotation != 0) {
			rotationAxis = Axis.X;
			angle = Math.round(xRotation / 90) * 90;
		}
		if (yRotation != 0) {
			rotationAxis = Axis.Y;
			angle = Math.round(yRotation / 90) * 90;
		}
		if (zRotation != 0) {
			rotationAxis = Axis.Z;
			angle = Math.round(zRotation / 90) * 90;
		}

		angle %= 360;
		if (angle < -90)
			angle += 360;

		this.rotation = BlockRotation.NONE;
		if (angle == -90 || angle == 270)
			this.rotation = BlockRotation.CLOCKWISE_90;
		if (angle == 90)
			this.rotation = BlockRotation.COUNTERCLOCKWISE_90;
		if (angle == 180)
			this.rotation = BlockRotation.CLOCKWISE_180;

		mirror = BlockMirror.NONE;
	}

	public Vec3d applyWithoutOffsetUncentered(Vec3d localVec) {
		Vec3d vec = localVec;
		if (mirror != null)
			vec = VecHelper.mirror(vec, mirror);
		if (rotationAxis != null)
			vec = VecHelper.rotate(vec, angle, rotationAxis);
		return vec;
	}

	public Vec3d applyWithoutOffset(Vec3d localVec) {
		Vec3d vec = localVec;
		if (mirror != null)
			vec = VecHelper.mirrorCentered(vec, mirror);
		if (rotationAxis != null)
			vec = VecHelper.rotateCentered(vec, angle, rotationAxis);
		return vec;
	}

	public Vec3d apply(Vec3d localVec) {
		return applyWithoutOffset(localVec).add(Vec3d.of(offset));
	}

	public BlockPos applyWithoutOffset(BlockPos localPos) {
		return BlockPos.ofFloored(applyWithoutOffset(VecHelper.getCenterOf(localPos)));
	}

	public BlockPos apply(BlockPos localPos) {
		return applyWithoutOffset(localPos).add(offset);
	}

	public void apply(BlockEntity be) {
		if (be instanceof ITransformableBlockEntity)
			((ITransformableBlockEntity) be).transform(this);
	}

	/**
	 * Vanilla does not support block state rotation around axes other than Y. Add
	 * specific cases here for vanilla block states so that they can react to rotations
	 * around horizontal axes. For Create blocks, implement ITransformableBlock.
	 */
	public BlockState apply(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof ITransformableBlock transformable)
			return transformable.transform(state, this);

		if (mirror != null)
			state = state.mirror(mirror);

		if (rotationAxis == Axis.Y) {
			if (block instanceof BellBlock) {
				if (state.get(Properties.ATTACHMENT) == Attachment.DOUBLE_WALL)
					state = state.with(Properties.ATTACHMENT, Attachment.SINGLE_WALL);
				return state.with(BellBlock.FACING,
					rotation.rotate(state.get(BellBlock.FACING)));
			}

			return state.rotate(rotation);
		}

		if (block instanceof WallMountedBlock) {
			DirectionProperty facingProperty = WallMountedBlock.FACING;
			EnumProperty<WallMountLocation> faceProperty = WallMountedBlock.FACE;
			Direction stateFacing = state.get(facingProperty);
			WallMountLocation stateFace = state.get(faceProperty);
			boolean z = rotationAxis == Axis.Z;
			Direction forcedAxis = z ? Direction.WEST : Direction.SOUTH;

			if (stateFacing.getAxis() == rotationAxis && stateFace == WallMountLocation.WALL)
				return state;

			for (int i = 0; i < rotation.ordinal(); i++) {
				stateFace = state.get(faceProperty);
				stateFacing = state.get(facingProperty);

				boolean b = state.get(faceProperty) == WallMountLocation.CEILING;
				state = state.with(facingProperty, b ? forcedAxis : forcedAxis.getOpposite());

				if (stateFace != WallMountLocation.WALL) {
					state = state.with(faceProperty, WallMountLocation.WALL);
					continue;
				}

				if (stateFacing.getDirection() == (z ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE)) {
					state = state.with(faceProperty, WallMountLocation.FLOOR);
					continue;
				}
				state = state.with(faceProperty, WallMountLocation.CEILING);
			}

			return state;
		}

		boolean halfTurn = rotation == BlockRotation.CLOCKWISE_180;
		if (block instanceof StairsBlock) {
			state = transformStairs(state, halfTurn);
			return state;
		}

		if (state.contains(FACING)) {
			state = state.with(FACING, rotateFacing(state.get(FACING)));
		} else if (state.contains(AXIS)) {
			state = state.with(AXIS, rotateAxis(state.get(AXIS)));
		} else if (halfTurn) {
			if (state.contains(HORIZONTAL_FACING)) {
				Direction stateFacing = state.get(HORIZONTAL_FACING);
				if (stateFacing.getAxis() == rotationAxis)
					return state;
			}

			state = state.rotate(rotation);

			if (state.contains(SlabBlock.TYPE) && state.get(SlabBlock.TYPE) != SlabType.DOUBLE)
				state = state.with(SlabBlock.TYPE,
					state.get(SlabBlock.TYPE) == SlabType.BOTTOM ? SlabType.TOP : SlabType.BOTTOM);
		}

		return state;
	}

	protected BlockState transformStairs(BlockState state, boolean halfTurn) {
		if (state.get(StairsBlock.FACING)
			.getAxis() != rotationAxis) {
			for (int i = 0; i < rotation.ordinal(); i++) {
				Direction direction = state.get(StairsBlock.FACING);
				BlockHalf half = state.get(StairsBlock.HALF);
				if (direction.getDirection() == AxisDirection.POSITIVE ^ half == BlockHalf.BOTTOM
					^ direction.getAxis() == Axis.Z)
					state = state.cycle(StairsBlock.HALF);
				else
					state = state.with(StairsBlock.FACING, direction.getOpposite());
			}
		} else {
			if (halfTurn) {
				state = state.cycle(StairsBlock.HALF);
			}
		}
		return state;
	}

	public Direction mirrorFacing(Direction facing) {
		if (mirror != null)
			return mirror.apply(facing);
		return facing;
	}

	public Axis rotateAxis(Axis axis) {
		Direction facing = Direction.get(AxisDirection.POSITIVE, axis);
		return rotateFacing(facing).getAxis();
	}

	public Direction rotateFacing(Direction facing) {
		for (int i = 0; i < rotation.ordinal(); i++)
			facing = facing.rotateClockwise(rotationAxis);
		return facing;
	}

	public static StructureTransform fromBuffer(PacketByteBuf buffer) {
		BlockPos readBlockPos = buffer.readBlockPos();
		int readAngle = buffer.readInt();
		int axisIndex = buffer.readVarInt();
		int rotationIndex = buffer.readVarInt();
		int mirrorIndex = buffer.readVarInt();
		return new StructureTransform(readBlockPos, readAngle, axisIndex == -1 ? null : Axis.values()[axisIndex],
			rotationIndex == -1 ? null : BlockRotation.values()[rotationIndex],
			mirrorIndex == -1 ? null : BlockMirror.values()[mirrorIndex]);
	}

	public void writeToBuffer(PacketByteBuf buffer) {
		buffer.writeBlockPos(offset);
		buffer.writeInt(angle);
		buffer.writeVarInt(rotationAxis == null ? -1 : rotationAxis.ordinal());
		buffer.writeVarInt(rotation == null ? -1 : rotation.ordinal());
		buffer.writeVarInt(mirror == null ? - 1 : mirror.ordinal());
	}

}
