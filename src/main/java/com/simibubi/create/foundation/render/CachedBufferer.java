package com.simibubi.create.foundation.render;

import static net.minecraft.state.property.Properties.FACING;

import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.tuple.Pair;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.render.SuperByteBufferCache.Compartment;
import com.simibubi.create.foundation.utility.AngleHelper;

public class CachedBufferer {

	public static final Compartment<BlockState> GENERIC_BLOCK = new Compartment<>();
	public static final Compartment<PartialModel> PARTIAL = new Compartment<>();
	public static final Compartment<Pair<Direction, PartialModel>> DIRECTIONAL_PARTIAL = new Compartment<>();

	public static SuperByteBuffer block(BlockState toRender) {
		return block(GENERIC_BLOCK, toRender);
	}

	public static SuperByteBuffer block(Compartment<BlockState> compartment, BlockState toRender) {
		return CreateClient.BUFFER_CACHE.get(compartment, toRender, () -> BakedModelRenderHelper.standardBlockRender(toRender));
	}

	public static SuperByteBuffer partial(PartialModel partial, BlockState referenceState) {
		return CreateClient.BUFFER_CACHE.get(PARTIAL, partial,
				() -> BakedModelRenderHelper.standardModelRender(partial.get(), referenceState));
	}

	public static SuperByteBuffer partial(PartialModel partial, BlockState referenceState,
			Supplier<MatrixStack> modelTransform) {
		return CreateClient.BUFFER_CACHE.get(PARTIAL, partial,
				() -> BakedModelRenderHelper.standardModelRender(partial.get(), referenceState, modelTransform.get()));
	}

	public static SuperByteBuffer partialFacing(PartialModel partial, BlockState referenceState) {
		Direction facing = referenceState.get(FACING);
		return partialFacing(partial, referenceState, facing);
	}

	public static SuperByteBuffer partialFacing(PartialModel partial, BlockState referenceState, Direction facing) {
		return partialDirectional(partial, referenceState, facing,
			rotateToFace(facing));
	}

	public static SuperByteBuffer partialFacingVertical(PartialModel partial, BlockState referenceState, Direction facing) {
		return partialDirectional(partial, referenceState, facing,
			rotateToFaceVertical(facing));
	}

	public static SuperByteBuffer partialDirectional(PartialModel partial, BlockState referenceState, Direction dir,
			Supplier<MatrixStack> modelTransform) {
		return CreateClient.BUFFER_CACHE.get(DIRECTIONAL_PARTIAL, Pair.of(dir, partial),
			() -> BakedModelRenderHelper.standardModelRender(partial.get(), referenceState, modelTransform.get()));
	}

	public static Supplier<MatrixStack> rotateToFace(Direction facing) {
		return () -> {
			MatrixStack stack = new MatrixStack();
			TransformStack.cast(stack)
				.centre()
				.rotateY(AngleHelper.horizontalAngle(facing))
				.rotateX(AngleHelper.verticalAngle(facing))
				.unCentre();
			return stack;
		};
	}

	public static Supplier<MatrixStack> rotateToFaceVertical(Direction facing) {
		return () -> {
			MatrixStack stack = new MatrixStack();
			TransformStack.cast(stack)
				.centre()
				.rotateY(AngleHelper.horizontalAngle(facing))
				.rotateX(AngleHelper.verticalAngle(facing) + 90)
				.unCentre();
			return stack;
		};
	}

}
