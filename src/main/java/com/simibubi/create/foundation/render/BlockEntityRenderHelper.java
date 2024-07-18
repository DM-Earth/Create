package com.simibubi.create.foundation.render;

import java.util.Iterator;

import javax.annotation.Nullable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class BlockEntityRenderHelper {

	public static void renderBlockEntities(World world, Iterable<BlockEntity> customRenderBEs, MatrixStack ms,
			VertexConsumerProvider buffer) {
		renderBlockEntities(world, null, customRenderBEs, ms, null, buffer);
	}

	public static void renderBlockEntities(World world, Iterable<BlockEntity> customRenderBEs, MatrixStack ms,
			VertexConsumerProvider buffer, float pt) {
		renderBlockEntities(world, null, customRenderBEs, ms, null, buffer, pt);
	}

	public static void renderBlockEntities(World world, @Nullable VirtualRenderWorld renderWorld,
			Iterable<BlockEntity> customRenderBEs, MatrixStack ms, @Nullable Matrix4f lightTransform, VertexConsumerProvider buffer) {
		renderBlockEntities(world, renderWorld, customRenderBEs, ms, lightTransform, buffer,
			AnimationTickHolder.getPartialTicks());
	}

	public static void renderBlockEntities(World world, @Nullable VirtualRenderWorld renderWorld,
			Iterable<BlockEntity> customRenderBEs, MatrixStack ms, @Nullable Matrix4f lightTransform, VertexConsumerProvider buffer,
			float pt) {
		Iterator<BlockEntity> iterator = customRenderBEs.iterator();
		while (iterator.hasNext()) {
			BlockEntity blockEntity = iterator.next();
			if (Backend.getBackendType() == BackendType.INSTANCING && Backend.isFlywheelWorld(renderWorld) && InstancedRenderRegistry.shouldSkipRender(blockEntity))
				continue;

			BlockEntityRenderer<BlockEntity> renderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(blockEntity);
			if (renderer == null) {
				iterator.remove();
				continue;
			}

			BlockPos pos = blockEntity.getPos();
			ms.push();
			TransformStack.cast(ms)
				.translate(pos);

			try {
				int worldLight = getCombinedLight(world, getLightPos(lightTransform, pos), renderWorld, pos);

				if (renderWorld != null) {
					// Swap the real world for the render world so that the renderer gets contraption-local information
					blockEntity.setWorld(renderWorld);
					renderer.render(blockEntity, pt, ms, buffer, worldLight, OverlayTexture.DEFAULT_UV);
					blockEntity.setWorld(world);
				} else {
					renderer.render(blockEntity, pt, ms, buffer, worldLight, OverlayTexture.DEFAULT_UV);
				}

			} catch (Exception e) {
				iterator.remove();

				String message = "BlockEntity " + RegisteredObjects.getKeyOrThrow(blockEntity.getType())
					.toString() + " could not be rendered virtually.";
				if (AllConfigs.client().explainRenderErrors.get())
					Create.LOGGER.error(message, e);
				else
					Create.LOGGER.error(message);
			}

			ms.pop();
		}
	}

	private static BlockPos getLightPos(@Nullable Matrix4f lightTransform, BlockPos contraptionPos) {
		if (lightTransform != null) {
			Vector4f lightVec = new Vector4f(contraptionPos.getX() + .5f, contraptionPos.getY() + .5f, contraptionPos.getZ() + .5f, 1);
			lightVec.mul(lightTransform);
			return BlockPos.ofFloored(lightVec.x(), lightVec.y(), lightVec.z());
		} else {
			return contraptionPos;
		}
	}

	public static int getCombinedLight(World world, BlockPos worldPos, @Nullable VirtualRenderWorld renderWorld,
			BlockPos renderWorldPos) {
		int worldLight = WorldRenderer.getLightmapCoordinates(world, worldPos);

		if (renderWorld != null) {
			int renderWorldLight = WorldRenderer.getLightmapCoordinates(renderWorld, renderWorldPos);
			return SuperByteBuffer.maxLight(worldLight, renderWorldLight);
		}

		return worldLight;
	}

}
