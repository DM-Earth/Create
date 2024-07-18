package com.simibubi.create.content.trains.track;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.simibubi.create.foundation.model.BakedQuadHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class TrackModel extends ForwardingBakedModel {

	public TrackModel(BakedModel originalModel) {
		wrapped = originalModel;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		if (!(blockView instanceof RenderAttachedBlockView attachmentView
				&& attachmentView.getBlockEntityRenderAttachment(pos) instanceof Double data)) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}

		double angleIn = data;
		double angle = Math.abs(angleIn);
		boolean flip = angleIn < 0;

		TrackShape trackShape = state.get(TrackBlock.SHAPE);
		double hAngle = switch (trackShape) {
		case XO -> 0;
		case PD -> 45;
		case ZO -> 90;
		case ND -> 135;
		default -> 0;
		};

		Vec3d verticalOffset = new Vec3d(0, -0.25, 0);
		Vec3d diagonalRotationPoint =
			(trackShape == TrackShape.ND || trackShape == TrackShape.PD) ? new Vec3d((MathHelper.SQUARE_ROOT_OF_TWO - 1) / 2, 0, 0)
				: Vec3d.ZERO;

		UnaryOperator<Vec3d> transform = v -> {
			v = v.add(verticalOffset);
			v = VecHelper.rotateCentered(v, hAngle, Axis.Y);
			v = v.add(diagonalRotationPoint);
			v = VecHelper.rotate(v, angle, Axis.Z);
			v = v.subtract(diagonalRotationPoint);
			v = VecHelper.rotateCentered(v, -hAngle + (flip ? 180 : 0), Axis.Y);
			v = v.subtract(verticalOffset);
			return v;
		};

		context.pushTransform(quad -> {
			for (int vertex = 0; vertex < 4; vertex++) {
				BakedQuadHelper.setXYZ(quad, vertex, transform.apply(BakedQuadHelper.getXYZ(quad, vertex)));
			}
			return true;
		});
		super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
		context.popTransform();
	}

}
