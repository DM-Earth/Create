package com.simibubi.create.content.trains.entity;

import java.util.Objects;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;

public class CarriageContraptionEntityRenderer extends ContraptionEntityRenderer<CarriageContraptionEntity> {

	public CarriageContraptionEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(CarriageContraptionEntity entity, Frustum clippingHelper, double cameraX,
		double cameraY, double cameraZ) {
		Carriage carriage = entity.getCarriage();
		if (carriage != null)
			for (CarriageBogey bogey : carriage.bogeys)
				if (bogey != null)
					bogey.couplingAnchors.replace(v -> null);
		return super.shouldRender(entity, clippingHelper, cameraX, cameraY, cameraZ);
	}

	@Override
	public void render(CarriageContraptionEntity entity, float yaw, float partialTicks, MatrixStack ms,
		VertexConsumerProvider buffers, int overlay) {
		if (!entity.validForRender || entity.firstPositionUpdate)
			return;

		super.render(entity, yaw, partialTicks, ms, buffers, overlay);

		Carriage carriage = entity.getCarriage();
		if (carriage == null)
			return;

		Vec3d position = entity.getLerpedPos(partialTicks);

		float viewYRot = entity.getYaw(partialTicks);
		float viewXRot = entity.getPitch(partialTicks);
		int bogeySpacing = carriage.bogeySpacing;

		carriage.bogeys.forEach(bogey -> {
			if (bogey == null)
				return;

			BlockPos bogeyPos = bogey.isLeading ? BlockPos.ORIGIN
				: BlockPos.ORIGIN.offset(entity.getInitialOrientation()
					.rotateYCounterclockwise(), bogeySpacing);

			if (!Backend.canUseInstancing(entity.getWorld()) && !entity.getContraption()
				.isHiddenInPortal(bogeyPos)) {

				ms.push();
				translateBogey(ms, bogey, bogeySpacing, viewYRot, viewXRot, partialTicks);

				int light = getBogeyLightCoords(entity, bogey, partialTicks);

				bogey.type.render(null, bogey.wheelAngle.getValue(partialTicks), ms, partialTicks, buffers, light,
					overlay, bogey.getStyle(), bogey.bogeyData);

				ms.pop();
			}

			bogey.updateCouplingAnchor(position, viewXRot, viewYRot, bogeySpacing, partialTicks, bogey.isLeading);
			if (!carriage.isOnTwoBogeys())
				bogey.updateCouplingAnchor(position, viewXRot, viewYRot, bogeySpacing, partialTicks, !bogey.isLeading);

		});
	}

	public static void translateBogey(MatrixStack ms, CarriageBogey bogey, int bogeySpacing, float viewYRot,
		float viewXRot, float partialTicks) {
		boolean selfUpsideDown = bogey.isUpsideDown();
		boolean leadingUpsideDown = bogey.carriage.leadingBogey().isUpsideDown();
		TransformStack.cast(ms)
			.rotateY(viewYRot + 90)
			.rotateX(-viewXRot)
			.rotateY(180)
			.translate(0, 0, bogey.isLeading ? 0 : -bogeySpacing)
			.rotateY(-180)
			.rotateX(viewXRot)
			.rotateY(-viewYRot - 90)
			.rotateY(bogey.yaw.getValue(partialTicks))
			.rotateX(bogey.pitch.getValue(partialTicks))
			.translate(0, .5f, 0)
			.rotateZ(selfUpsideDown ? 180 : 0)
			.translateY(selfUpsideDown != leadingUpsideDown ? 2 : 0);
	}

	public static int getBogeyLightCoords(CarriageContraptionEntity entity, CarriageBogey bogey, float partialTicks) {

		var lightPos = BlockPos.ofFloored(
			Objects.requireNonNullElseGet(bogey.getAnchorPosition(), () -> entity.getClientCameraPosVec(partialTicks)));

		return LightmapTextureManager.pack(entity.getWorld().getLightLevel(LightType.BLOCK, lightPos),
			entity.getWorld().getLightLevel(LightType.SKY, lightPos));
	}

}
