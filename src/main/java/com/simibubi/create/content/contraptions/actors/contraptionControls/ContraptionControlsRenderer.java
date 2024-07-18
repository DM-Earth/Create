package com.simibubi.create.content.contraptions.actors.contraptionControls;

import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement.ElevatorFloorSelection;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.simibubi.create.foundation.utility.VecHelper;

public class ContraptionControlsRenderer extends SmartBlockEntityRenderer<ContraptionControlsBlockEntity> {

	private static Random r = new Random();

	public ContraptionControlsRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ContraptionControlsBlockEntity blockEntity, float pt, MatrixStack ms,
		VertexConsumerProvider buffer, int light, int overlay) {
		BlockState blockState = blockEntity.getCachedState();
		Direction facing = blockState.get(ContraptionControlsBlock.FACING)
			.getOpposite();
		Vec3d buttonMovementAxis = VecHelper.rotate(new Vec3d(0, 1, -.325), AngleHelper.horizontalAngle(facing), Axis.Y);
		Vec3d buttonMovement = buttonMovementAxis.multiply(-0.07f + -1 / 24f * blockEntity.button.getValue(pt));
		Vec3d buttonOffset = buttonMovementAxis.multiply(0.07f);

		ms.push();
		ms.translate(buttonMovement.x, buttonMovement.y, buttonMovement.z);
		super.renderSafe(blockEntity, pt, ms, buffer, light, overlay);
		ms.translate(buttonOffset.x, buttonOffset.y, buttonOffset.z);

		VertexConsumer vc = buffer.getBuffer(RenderLayer.getSolid());
		CachedBufferer.partialFacing(AllPartialModels.CONTRAPTION_CONTROLS_BUTTON, blockState, facing)
			.light(light)
			.renderInto(ms, vc);

		ms.pop();

		int i = (((int) blockEntity.indicator.getValue(pt) / 45) % 8) + 8;
		CachedBufferer.partialFacing(AllPartialModels.CONTRAPTION_CONTROLS_INDICATOR.get(i % 8), blockState, facing)
			.light(light)
			.renderInto(ms, vc);
	}

	public static void renderInContraption(MovementContext ctx, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffer) {

		if (!(ctx.temporaryData instanceof ElevatorFloorSelection efs))
			return;
		if (!AllBlocks.CONTRAPTION_CONTROLS.has(ctx.state))
			return;

		Entity cameraEntity = MinecraftClient.getInstance()
			.getCameraEntity();
		float playerDistance = (float) (ctx.position == null || cameraEntity == null ? 0
			: ctx.position.squaredDistanceTo(cameraEntity.getEyePos()));

		float flicker = r.nextFloat();
		Couple<Integer> couple = DyeHelper.DYE_TABLE.get(efs.targetYEqualsSelection ? DyeColor.WHITE : DyeColor.ORANGE);
		int brightColor = couple.getFirst();
		int darkColor = couple.getSecond();
		int flickeringBrightColor = Color.mixColors(brightColor, darkColor, flicker / 4);
		TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
		float shadowOffset = .5f;

		String text = efs.currentShortName;
		String description = efs.currentLongName;
		MatrixStack ms = matrices.getViewProjection();
		TransformStack msr = TransformStack.cast(ms);

		ms.push();
		msr.translate(ctx.localPos);
		msr.rotateCentered(Direction.UP,
			AngleHelper.rad(AngleHelper.horizontalAngle(ctx.state.get(ContraptionControlsBlock.FACING))));
		ms.translate(0.275f + 0.125f, 1, 0.5f);
		msr.rotate(Direction.WEST, AngleHelper.rad(67.5f));

		float buttondepth = -.25f;
		if (ctx.contraption.presentBlockEntities.get(ctx.localPos) instanceof ContraptionControlsBlockEntity cbe)
			buttondepth += -1 / 24f * cbe.button.getValue(AnimationTickHolder.getPartialTicks(renderWorld));

		if (!text.isBlank() && playerDistance < 100) {
			int actualWidth = fontRenderer.getWidth(text);
			int width = Math.max(actualWidth, 12);
			float scale = 1 / (5f * (width - .5f));
			float heightCentering = (width - 8f) / 2;

			ms.push();
			ms.translate(0, .15f, buttondepth);
			ms.scale(scale, -scale, scale);
			ms.translate(Math.max(0, width - actualWidth) / 2, heightCentering, 0);
			NixieTubeRenderer.drawInWorldString(ms, buffer, text, flickeringBrightColor);
			ms.translate(shadowOffset, shadowOffset, -1 / 16f);
			NixieTubeRenderer.drawInWorldString(ms, buffer, text, Color.mixColors(darkColor, 0, .35f));
			ms.pop();
		}

		if (!description.isBlank() && playerDistance < 20) {
			int actualWidth = fontRenderer.getWidth(description);
			int width = Math.max(actualWidth, 55);
			float scale = 1 / (3f * (width - .5f));
			float heightCentering = (width - 8f) / 2;

			ms.push();
			ms.translate(-.0635f, 0.06f, buttondepth);
			ms.scale(scale, -scale, scale);
			ms.translate(Math.max(0, width - actualWidth) / 2, heightCentering, 0);
			NixieTubeRenderer.drawInWorldString(ms, buffer, description, flickeringBrightColor);
			ms.pop();
		}

		ms.pop();

	}

}