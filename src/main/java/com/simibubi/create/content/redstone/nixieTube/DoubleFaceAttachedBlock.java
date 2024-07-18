package com.simibubi.create.content.redstone.nixieTube;

import javax.annotation.Nullable;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class DoubleFaceAttachedBlock extends HorizontalFacingBlock {

	public enum DoubleAttachFace implements StringIdentifiable {
		FLOOR("floor"), WALL("wall"), WALL_REVERSED("wall_reversed"), CEILING("ceiling");

		private final String name;

		private DoubleAttachFace(String p_61311_) {
			this.name = p_61311_;
		}

		public String asString() {
			return this.name;
		}

		public int xRot() {
			return this == FLOOR ? 0 : this == CEILING ? 180 : 90;
		}
	}

	public static final EnumProperty<DoubleAttachFace> FACE =
		EnumProperty.of("double_face", DoubleAttachFace.class);

	public DoubleFaceAttachedBlock(AbstractBlock.Settings p_53182_) {
		super(p_53182_);
	}

	@Nullable
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		for (Direction direction : pContext.getPlacementDirections()) {
			BlockState blockstate;
			if (direction.getAxis() == Direction.Axis.Y) {
				blockstate = this.getDefaultState()
					.with(FACE, direction == Direction.UP ? DoubleAttachFace.CEILING : DoubleAttachFace.FLOOR)
					.with(FACING, pContext.getHorizontalPlayerFacing());
			} else {
				Vec3d n = Vec3d.of(direction.rotateYClockwise()
					.getVector());
				DoubleAttachFace face = DoubleAttachFace.WALL;
				if (pContext.getPlayer() != null) {
					Vec3d lookAngle = pContext.getPlayer()
						.getRotationVector();
					if (lookAngle.dotProduct(n) < 0)
						face = DoubleAttachFace.WALL_REVERSED;
				}
				blockstate = this.getDefaultState()
					.with(FACE, face)
					.with(FACING, direction.getOpposite());
			}

			if (blockstate.canPlaceAt(pContext.getWorld(), pContext.getBlockPos())) {
				return blockstate;
			}
		}

		return null;
	}

	protected static Direction getConnectedDirection(BlockState pState) {
		switch ((DoubleAttachFace) pState.get(FACE)) {
		case CEILING:
			return Direction.DOWN;
		case FLOOR:
			return Direction.UP;
		default:
			return pState.get(FACING);
		}
	}
}
