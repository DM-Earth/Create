package com.simibubi.create.content.trains.entity;

import java.util.Collection;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

public class CarriageCouplingRenderer {

	public static void renderAll(MatrixStack ms, VertexConsumerProvider buffer, Vec3d camera) {
		Collection<Train> trains = CreateClient.RAILWAYS.trains.values();
		VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());
		BlockState air = Blocks.AIR.getDefaultState();
		float partialTicks = AnimationTickHolder.getPartialTicks();
		World level = MinecraftClient.getInstance().world;

		for (Train train : trains) {
			List<Carriage> carriages = train.carriages;
			for (int i = 0; i < carriages.size() - 1; i++) {
				Carriage carriage = carriages.get(i);
				CarriageContraptionEntity entity = carriage.getDimensional(level).entity.get();
				Carriage carriage2 = carriages.get(i + 1);
				CarriageContraptionEntity entity2 = carriage.getDimensional(level).entity.get();

				if (entity == null || entity2 == null)
					continue;

				CarriageBogey bogey1 = carriage.trailingBogey();
				CarriageBogey bogey2 = carriage2.leadingBogey();
				Vec3d anchor = bogey1.couplingAnchors.getSecond();
				Vec3d anchor2 = bogey2.couplingAnchors.getFirst();

				if (anchor == null || anchor2 == null)
					continue;
				if (!anchor.isInRange(camera, 64))
					continue;

				int lightCoords = getPackedLightCoords(entity, partialTicks);
				int lightCoords2 = getPackedLightCoords(entity2, partialTicks);

				double diffX = anchor2.x - anchor.x;
				double diffY = anchor2.y - anchor.y;
				double diffZ = anchor2.z - anchor.z;
				float yRot = AngleHelper.deg(MathHelper.atan2(diffZ, diffX)) + 90;
				float xRot = AngleHelper.deg(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)));

				Vec3d position = entity.getLerpedPos(partialTicks);
				Vec3d position2 = entity2.getLerpedPos(partialTicks);

				ms.push();

				{
					ms.push();
					ms.translate(anchor.x - camera.x, anchor.y - camera.y, anchor.z - camera.z);
					CachedBufferer.partial(AllPartialModels.TRAIN_COUPLING_HEAD, air)
						.rotateY(-yRot)
						.rotateX(xRot)
						.light(lightCoords)
						.renderInto(ms, vb);

					float margin = 3 / 16f;
					double couplingDistance = train.carriageSpacing.get(i) - 2 * margin
						- bogey1.type.getConnectorAnchorOffset(bogey1.isUpsideDown()).z - bogey2.type.getConnectorAnchorOffset(bogey2.isUpsideDown()).z;
					int couplingSegments = (int) Math.round(couplingDistance * 4);
					double stretch = ((anchor2.distanceTo(anchor) - 2 * margin) * 4) / couplingSegments;
					for (int j = 0; j < couplingSegments; j++) {
						CachedBufferer.partial(AllPartialModels.TRAIN_COUPLING_CABLE, air)
							.rotateY(-yRot + 180)
							.rotateX(-xRot)
							.translate(0, 0, margin + 2 / 16f)
							.scale(1, 1, (float) stretch)
							.translate(0, 0, j / 4f)
							.light(lightCoords)
							.renderInto(ms, vb);
					}
					ms.pop();
				}

				{
					ms.push();
					Vec3d translation = position2.subtract(position)
						.add(anchor2)
						.subtract(camera);
					ms.translate(translation.x, translation.y, translation.z);
					CachedBufferer.partial(AllPartialModels.TRAIN_COUPLING_HEAD, air)
						.rotateY(-yRot + 180)
						.rotateX(-xRot)
						.light(lightCoords2)
						.renderInto(ms, vb);
					ms.pop();
				}

				ms.pop();
			}
		}

	}

	public static int getPackedLightCoords(Entity pEntity, float pPartialTicks) {
		BlockPos blockpos = BlockPos.ofFloored(pEntity.getClientCameraPosVec(pPartialTicks));
		return LightmapTextureManager.pack(getBlockLightLevel(pEntity, blockpos), getSkyLightLevel(pEntity, blockpos));
	}

	protected static int getSkyLightLevel(Entity pEntity, BlockPos pPos) {
		return pEntity.getWorld().getLightLevel(LightType.SKY, pPos);
	}

	protected static int getBlockLightLevel(Entity pEntity, BlockPos pPos) {
		return pEntity.isOnFire() ? 15 : pEntity.getWorld().getLightLevel(LightType.BLOCK, pPos);
	}

}
