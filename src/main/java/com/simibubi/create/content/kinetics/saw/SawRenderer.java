package com.simibubi.create.content.kinetics.saw;

import static net.minecraft.state.property.Properties.FACING;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class SawRenderer extends SafeBlockEntityRenderer<SawBlockEntity> {

	public SawRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	protected void renderSafe(SawBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light,
		int overlay) {
		renderBlade(be, ms, buffer, light);
		renderItems(be, partialTicks, ms, buffer, light, overlay);
		FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);

		if (Backend.canUseInstancing(be.getWorld()))
			return;

		renderShaft(be, ms, buffer, light, overlay);
	}

	protected void renderBlade(SawBlockEntity be, MatrixStack ms, VertexConsumerProvider buffer, int light) {
		BlockState blockState = be.getCachedState();
		PartialModel partial;
		float speed = be.getSpeed();
		boolean rotate = false;

		if (SawBlock.isHorizontal(blockState)) {
			if (speed > 0) {
				partial = AllPartialModels.SAW_BLADE_HORIZONTAL_ACTIVE;
			} else if (speed < 0) {
				partial = AllPartialModels.SAW_BLADE_HORIZONTAL_REVERSED;
			} else {
				partial = AllPartialModels.SAW_BLADE_HORIZONTAL_INACTIVE;
			}
		} else {
			if (be.getSpeed() > 0) {
				partial = AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE;
			} else if (speed < 0) {
				partial = AllPartialModels.SAW_BLADE_VERTICAL_REVERSED;
			} else {
				partial = AllPartialModels.SAW_BLADE_VERTICAL_INACTIVE;
			}

			if (blockState.get(SawBlock.AXIS_ALONG_FIRST_COORDINATE))
				rotate = true;
		}

		SuperByteBuffer superBuffer = CachedBufferer.partialFacing(partial, blockState);
		if (rotate) {
			superBuffer.rotateCentered(Direction.UP, AngleHelper.rad(90));
		}
		superBuffer.color(0xFFFFFF)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getCutoutMipped()));
	}

	protected void renderShaft(SawBlockEntity be, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
		KineticBlockEntityRenderer.renderRotatingBuffer(be, getRotatedModel(be), ms,
			buffer.getBuffer(RenderLayer.getSolid()), light);
	}

	protected void renderItems(SawBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		boolean processingMode = be.getCachedState()
			.get(SawBlock.FACING) == Direction.UP;
		if (processingMode && !be.inventory.isEmpty()) {
			boolean alongZ = !be.getCachedState()
				.get(SawBlock.AXIS_ALONG_FIRST_COORDINATE);
			ms.push();

			boolean moving = be.inventory.recipeDuration != 0;
			float offset = moving ? (float) (be.inventory.remainingTime) / be.inventory.recipeDuration : 0;
			float processingSpeed = MathHelper.clamp(Math.abs(be.getSpeed()) / 32, 1, 128);
			if (moving) {
				offset = MathHelper
					.clamp(offset + ((-partialTicks + .5f) * processingSpeed) / be.inventory.recipeDuration, 0.125f, 1f);
				if (!be.inventory.appliedRecipe)
					offset += 1;
				offset /= 2;
			}

			if (be.getSpeed() == 0)
				offset = .5f;
			if (be.getSpeed() < 0 ^ alongZ)
				offset = 1 - offset;

			for (int i = 0; i < be.inventory.getSlotCount(); i++) {
				ItemStack stack = be.inventory.getStackInSlot(i);
				if (stack.isEmpty())
					continue;

				ItemRenderer itemRenderer = MinecraftClient.getInstance()
					.getItemRenderer();
				BakedModel modelWithOverrides = itemRenderer.getModel(stack, be.getWorld(), null, 0);
				boolean blockItem = modelWithOverrides.hasDepth();

				ms.translate(alongZ ? offset : .5, blockItem ? .925f : 13f / 16f, alongZ ? .5 : offset);

				ms.scale(.5f, .5f, .5f);
				if (alongZ)
					ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
				ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
				itemRenderer.renderItem(stack, ModelTransformationMode.FIXED, light, overlay, ms, buffer, be.getWorld(), 0);
				break;
			}

			ms.pop();
		}
	}

	protected SuperByteBuffer getRotatedModel(KineticBlockEntity be) {
		BlockState state = be.getCachedState();
		if (state.get(FACING)
			.getAxis()
			.isHorizontal())
			return CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF,
				state.rotate(BlockRotation.CLOCKWISE_180));
		return CachedBufferer.block(KineticBlockEntityRenderer.KINETIC_BLOCK,
			getRenderedBlockState(be));
	}

	protected BlockState getRenderedBlockState(KineticBlockEntity be) {
		return KineticBlockEntityRenderer.shaft(KineticBlockEntityRenderer.getRotationAxisOf(be));
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffer) {
		BlockState state = context.state;
		Direction facing = state.get(SawBlock.FACING);

		Vec3d facingVec = Vec3d.of(context.state.get(SawBlock.FACING)
			.getVector());
		facingVec = context.rotation.apply(facingVec);

		Direction closestToFacing = Direction.getFacing(facingVec.x, facingVec.y, facingVec.z);

		boolean horizontal = closestToFacing.getAxis()
			.isHorizontal();
		boolean backwards = VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite());
		boolean moving = context.getAnimationSpeed() != 0;
		boolean shouldAnimate =
			(context.contraption.stalled && horizontal) || (!context.contraption.stalled && !backwards && moving);

		SuperByteBuffer superBuffer;
		if (SawBlock.isHorizontal(state)) {
			if (shouldAnimate)
				superBuffer = CachedBufferer.partial(AllPartialModels.SAW_BLADE_HORIZONTAL_ACTIVE, state);
			else
				superBuffer = CachedBufferer.partial(AllPartialModels.SAW_BLADE_HORIZONTAL_INACTIVE, state);
		} else {
			if (shouldAnimate)
				superBuffer = CachedBufferer.partial(AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE, state);
			else
				superBuffer = CachedBufferer.partial(AllPartialModels.SAW_BLADE_VERTICAL_INACTIVE, state);
		}

		superBuffer.transform(matrices.getModel())
			.centre()
			.rotateY(AngleHelper.horizontalAngle(facing))
			.rotateX(AngleHelper.verticalAngle(facing));

		if (!SawBlock.isHorizontal(state)) {
			superBuffer.rotateZ(state.get(SawBlock.AXIS_ALONG_FIRST_COORDINATE) ? 90 : 0);
		}

		superBuffer.unCentre()
			.light(matrices.getWorld(), ContraptionRenderDispatcher.getContraptionWorldLight(context, renderWorld))
			.renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderLayer.getCutoutMipped()));
	}

}
