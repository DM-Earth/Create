package com.simibubi.create.content.logistics.depot;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.util.transform.Rotate;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.jozufozu.flywheel.util.transform.Translate;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.LongAttached;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public class EjectorRenderer extends ShaftRenderer<EjectorBlockEntity> {

	static final Vec3d pivot = VecHelper.voxelSpace(0, 11.25, 0.75);

	public EjectorRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public boolean rendersOutsideBoundingBox(EjectorBlockEntity p_188185_1_) {
		return true;
	}

	@Override
	protected void renderSafe(EjectorBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		VertexConsumer vertexBuilder = buffer.getBuffer(RenderLayer.getSolid());
		float lidProgress = be.getLidProgress(partialTicks);
		float angle = lidProgress * 70;

		if (!Backend.canUseInstancing(be.getWorld())) {
			SuperByteBuffer model = CachedBufferer.partial(AllPartialModels.EJECTOR_TOP, be.getCachedState());
			applyLidAngle(be, angle, model);
			model.light(light)
					.renderInto(ms, vertexBuilder);
		}

		TransformStack msr = TransformStack.cast(ms);

		float maxTime =
				(float) (be.earlyTarget != null ? be.earlyTargetTime : be.launcher.getTotalFlyingTicks());
		for (LongAttached<ItemStack> LongAttached : be.launchedItems) {
			float time = LongAttached.getFirst() + partialTicks;
			if (time > maxTime)
				continue;

			ms.push();
			Vec3d launchedItemLocation = be.getLaunchedItemLocation(time);
			msr.translate(launchedItemLocation.subtract(Vec3d.of(be.getPos())));
			Vec3d itemRotOffset = VecHelper.voxelSpace(0, 3, 0);
			msr.translate(itemRotOffset);
			msr.rotateY(AngleHelper.horizontalAngle(be.getFacing()));
			msr.rotateX(time * 40);
			msr.translateBack(itemRotOffset);
			MinecraftClient.getInstance()
				.getItemRenderer()
				.renderItem(LongAttached.getValue(), ModelTransformationMode.GROUND, light, overlay, ms, buffer, be.getWorld(), 0);
			ms.pop();
		}

		DepotBehaviour behaviour = be.getBehaviour(DepotBehaviour.TYPE);
		if (behaviour == null || behaviour.isEmpty())
			return;

		ms.push();
		applyLidAngle(be, angle, msr);
		msr.centre()
			.rotateY(-180 - AngleHelper.horizontalAngle(be.getCachedState()
				.get(EjectorBlock.HORIZONTAL_FACING)))
			.unCentre();
		DepotRenderer.renderItemsOf(be, partialTicks, ms, buffer, light, overlay, behaviour);
		ms.pop();
	}

	static <T extends Translate<T> & Rotate<T>> void applyLidAngle(KineticBlockEntity be, float angle, T tr) {
		applyLidAngle(be, pivot, angle, tr);
	}

	static <T extends Translate<T> & Rotate<T>> void applyLidAngle(KineticBlockEntity be, Vec3d rotationOffset, float angle, T tr) {
		tr.centre()
			.rotateY(180 + AngleHelper.horizontalAngle(be.getCachedState()
				.get(EjectorBlock.HORIZONTAL_FACING)))
			.unCentre()
			.translate(rotationOffset)
			.rotateX(-angle)
			.translateBack(rotationOffset);
	}

}
