package com.simibubi.create.content.kinetics.mechanicalArm;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public class ArmRenderer extends KineticBlockEntityRenderer<ArmBlockEntity> {

	public ArmRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ArmBlockEntity be, float pt, MatrixStack ms, VertexConsumerProvider buffer, int light,
		int overlay) {
		super.renderSafe(be, pt, ms, buffer, light, overlay);

		ItemStack item = be.heldItem;
		boolean hasItem = !item.isEmpty();
		boolean usingFlywheel = Backend.canUseInstancing(be.getWorld());

		if (usingFlywheel && !hasItem)
			return;

		ItemRenderer itemRenderer = MinecraftClient.getInstance()
			.getItemRenderer();

		boolean isBlockItem =
			hasItem && (item.getItem() instanceof BlockItem) && itemRenderer.getModel(item, be.getWorld(), null, 0)
				.hasDepth();

		VertexConsumer builder = buffer.getBuffer(be.goggles ? RenderLayer.getCutout() : RenderLayer.getSolid());
		BlockState blockState = be.getCachedState();

		MatrixStack msLocal = new MatrixStack();
		TransformStack msr = TransformStack.cast(msLocal);

		float baseAngle;
		float lowerArmAngle;
		float upperArmAngle;
		float headAngle;
		int color;
		boolean inverted = blockState.get(ArmBlock.CEILING);

		boolean rave = be.phase == Phase.DANCING && be.getSpeed() != 0;
		if (rave) {
			float renderTick = AnimationTickHolder.getRenderTime(be.getWorld()) + (be.hashCode() % 64);
			baseAngle = (renderTick * 10) % 360;
			lowerArmAngle = MathHelper.lerp((MathHelper.sin(renderTick / 4) + 1) / 2, -45, 15);
			upperArmAngle = MathHelper.lerp((MathHelper.sin(renderTick / 8) + 1) / 4, -45, 95);
			headAngle = -lowerArmAngle;
			color = Color.rainbowColor(AnimationTickHolder.getTicks() * 100)
				.getRGB();
		} else {
			baseAngle = be.baseAngle.getValue(pt);
			lowerArmAngle = be.lowerArmAngle.getValue(pt) - 135;
			upperArmAngle = be.upperArmAngle.getValue(pt) - 90;
			headAngle = be.headAngle.getValue(pt);
			color = 0xFFFFFF;
		}

		msr.centre();

		if (inverted)
			msr.rotateX(180);

		if (usingFlywheel)
			doItemTransforms(msr, baseAngle, lowerArmAngle, upperArmAngle, headAngle);
		else
			renderArm(builder, ms, msLocal, msr, blockState, color, baseAngle, lowerArmAngle, upperArmAngle, headAngle,
				be.goggles, inverted && be.goggles, hasItem, isBlockItem, light);

		if (hasItem) {
			ms.push();
			float itemScale = isBlockItem ? .5f : .625f;
			msr.rotateX(90);
			msLocal.translate(0, isBlockItem ? -9 / 16f : -10 / 16f, 0);
			msLocal.scale(itemScale, itemScale, itemScale);

			ms.peek()
				.getPositionMatrix()
				.mul(msLocal.peek()
					.getPositionMatrix());

			itemRenderer.renderItem(item, ModelTransformationMode.FIXED, light, overlay, ms, buffer, be.getWorld(), 0);
			ms.pop();
		}

	}

	private void renderArm(VertexConsumer builder, MatrixStack ms, MatrixStack msLocal, TransformStack msr,
		BlockState blockState, int color, float baseAngle, float lowerArmAngle, float upperArmAngle, float headAngle,
		boolean goggles, boolean inverted, boolean hasItem, boolean isBlockItem, int light) {
		SuperByteBuffer base = CachedBufferer.partial(AllPartialModels.ARM_BASE, blockState)
			.light(light);
		SuperByteBuffer lowerBody = CachedBufferer.partial(AllPartialModels.ARM_LOWER_BODY, blockState)
			.light(light);
		SuperByteBuffer upperBody = CachedBufferer.partial(AllPartialModels.ARM_UPPER_BODY, blockState)
			.light(light);
		SuperByteBuffer claw = CachedBufferer
			.partial(goggles ? AllPartialModels.ARM_CLAW_BASE_GOGGLES : AllPartialModels.ARM_CLAW_BASE, blockState)
			.light(light);
		SuperByteBuffer upperClawGrip = CachedBufferer.partial(AllPartialModels.ARM_CLAW_GRIP_UPPER,
			blockState)
			.light(light);
		SuperByteBuffer lowerClawGrip = CachedBufferer.partial(AllPartialModels.ARM_CLAW_GRIP_LOWER, blockState)
			.light(light);

		transformBase(msr, baseAngle);
		base.transform(msLocal)
			.renderInto(ms, builder);

		transformLowerArm(msr, lowerArmAngle);
		lowerBody.color(color)
			.transform(msLocal)
			.renderInto(ms, builder);

		transformUpperArm(msr, upperArmAngle);
		upperBody.color(color)
			.transform(msLocal)
			.renderInto(ms, builder);

		transformHead(msr, headAngle);

		if (inverted)
			msr.rotateZ(180);

		claw.transform(msLocal)
			.renderInto(ms, builder);

		if (inverted)
			msr.rotateZ(180);

		for (int flip : Iterate.positiveAndNegative) {
			msLocal.push();
			transformClawHalf(msr, hasItem, isBlockItem, flip);
			(flip > 0 ? lowerClawGrip : upperClawGrip).transform(msLocal)
				.renderInto(ms, builder);
			msLocal.pop();
		}
	}

	private void doItemTransforms(TransformStack msr, float baseAngle, float lowerArmAngle, float upperArmAngle,
		float headAngle) {

		transformBase(msr, baseAngle);
		transformLowerArm(msr, lowerArmAngle);
		transformUpperArm(msr, upperArmAngle);
		transformHead(msr, headAngle);
	}

	public static void transformClawHalf(TransformStack msr, boolean hasItem, boolean isBlockItem, int flip) {
		msr.translate(0, -flip * (hasItem ? isBlockItem ? 3 / 16f : 5 / 64f : 1 / 16f), -6 / 16d);
	}

	public static void transformHead(TransformStack msr, float headAngle) {
		msr.translate(0, 0, -15 / 16d);
		msr.rotateX(headAngle - 45f);
	}

	public static void transformUpperArm(TransformStack msr, float upperArmAngle) {
		msr.translate(0, 0, -14 / 16d);
		msr.rotateX(upperArmAngle - 90);
	}

	public static void transformLowerArm(TransformStack msr, float lowerArmAngle) {
		msr.translate(0, 2 / 16d, 0);
		msr.rotateX(lowerArmAngle + 135);
	}

	public static void transformBase(TransformStack msr, float baseAngle) {
		msr.translate(0, 4 / 16d, 0);
		msr.rotateY(baseAngle);
	}

	@Override
	public boolean rendersOutsideBoundingBox(ArmBlockEntity be) {
		return true;
	}

	@Override
	protected SuperByteBuffer getRotatedModel(ArmBlockEntity be, BlockState state) {
		return CachedBufferer.partial(AllPartialModels.ARM_COG, state);
	}

}
