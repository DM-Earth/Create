package com.simibubi.create.content.contraptions.render;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.gl.error.GlError;
import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.core.model.ShadeSeparatedBufferedData;
import com.jozufozu.flywheel.core.model.WorldModelBuilder;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.jozufozu.flywheel.event.GatherContextEvent;
import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.jozufozu.flywheel.util.WorldAttached;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.render.BlockEntityRenderHelper;
import com.simibubi.create.foundation.render.SuperByteBuffer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class ContraptionRenderDispatcher {

	private static WorldAttached<ContraptionRenderingWorld<?>> WORLDS = new WorldAttached<>(SBBContraptionManager::new);

	/**
	 * Reset a contraption's renderer.
	 *
	 * @param contraption The contraption to invalidate.
	 * @return true if there was a renderer associated with the given contraption.
	 */
	public static boolean invalidate(Contraption contraption) {
		World level = contraption.entity.getWorld();

		return WORLDS.get(level)
			.invalidate(contraption);
	}

	public static void tick(World world) {
		if (MinecraftClient.getInstance()
			.isPaused())
			return;

		WORLDS.get(world)
			.tick();
	}

	public static void beginFrame(BeginFrameEvent event) {
		WORLDS.get(event.getWorld())
			.beginFrame(event);
	}

	public static void renderLayer(RenderLayerEvent event) {
		WORLDS.get(event.getWorld())
			.renderLayer(event);

		GlError.pollAndThrow(() -> "contraption layer: " + event.getLayer());
	}

	public static void onRendererReload(ReloadRenderersEvent event) {
		reset();
	}

	public static void gatherContext(GatherContextEvent e) {
		reset();
	}

	public static void renderFromEntity(AbstractContraptionEntity entity, Contraption contraption,
		VertexConsumerProvider buffers) {
		World world = entity.getWorld();

		ContraptionRenderInfo renderInfo = WORLDS.get(world)
			.getRenderInfo(contraption);
		ContraptionMatrices matrices = renderInfo.getMatrices();

		// something went wrong with the other rendering
		if (!matrices.isReady())
			return;

		VirtualRenderWorld renderWorld = renderInfo.renderWorld;

		renderBlockEntities(world, renderWorld, contraption, matrices, buffers);

		if (buffers instanceof VertexConsumerProvider.Immediate)
			((VertexConsumerProvider.Immediate) buffers).draw();

		renderActors(world, renderWorld, contraption, matrices, buffers);
	}

	public static VirtualRenderWorld setupRenderWorld(World world, Contraption c) {
		ContraptionWorld contraptionWorld = c.getContraptionWorld();

		BlockPos origin = c.anchor;
		int minBuildHeight = contraptionWorld.getBottomY();
		int height = contraptionWorld.getHeight();
		VirtualRenderWorld renderWorld = new VirtualRenderWorld(world, minBuildHeight, height, origin) {
			@Override
			public boolean supportsFlywheel() {
				return canInstance();
			}
		};

		renderWorld.setBlockEntities(c.presentBlockEntities.values());
		for (StructureTemplate.StructureBlockInfo info : c.getBlocks()
			.values())
			// Skip individual lighting updates to prevent lag with large contraptions
			// FIXME 1.20 this '0' used to be Block.UPDATE_SUPPRESS_LIGHT, yet VirtualRenderWorld didn't actually parse the flags at all
			renderWorld.setBlockState(info.pos(), info.state(), 0);

		renderWorld.runLightEngine();
		return renderWorld;
	}

	public static void renderBlockEntities(World world, VirtualRenderWorld renderWorld, Contraption c,
		ContraptionMatrices matrices, VertexConsumerProvider buffer) {
		BlockEntityRenderHelper.renderBlockEntities(world, renderWorld, c.getSpecialRenderedBEs(),
			matrices.getModelViewProjection(), matrices.getLight(), buffer);
	}

	protected static void renderActors(World world, VirtualRenderWorld renderWorld, Contraption c,
		ContraptionMatrices matrices, VertexConsumerProvider buffer) {
		MatrixStack m = matrices.getModel();

		for (Pair<StructureTemplate.StructureBlockInfo, MovementContext> actor : c.getActors()) {
			MovementContext context = actor.getRight();
			if (context == null)
				continue;
			if (context.world == null)
				context.world = world;
			StructureTemplate.StructureBlockInfo blockInfo = actor.getLeft();

			MovementBehaviour movementBehaviour = AllMovementBehaviours.getBehaviour(blockInfo.state());
			if (movementBehaviour != null) {
				if (c.isHiddenInPortal(blockInfo.pos()))
					continue;
				m.push();
				TransformStack.cast(m)
					.translate(blockInfo.pos());
				movementBehaviour.renderInContraption(context, renderWorld, matrices, buffer);
				m.pop();
			}
		}
	}

	public static SuperByteBuffer buildStructureBuffer(VirtualRenderWorld renderWorld, Contraption c,
		RenderLayer layer) {
		Collection<StructureTemplate.StructureBlockInfo> values = c.getRenderedBlocks();
		ShadeSeparatedBufferedData data = new WorldModelBuilder(layer).withRenderWorld(renderWorld)
				.withBlocks(values)
				.build();
		SuperByteBuffer sbb = new SuperByteBuffer(data);
		data.release();
		return sbb;
	}

	public static int getLight(World world, float lx, float ly, float lz) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		float block = 0, sky = 0;
		float offset = 1 / 8f;

		for (float zOffset = offset; zOffset >= -offset; zOffset -= 2 * offset)
			for (float yOffset = offset; yOffset >= -offset; yOffset -= 2 * offset)
				for (float xOffset = offset; xOffset >= -offset; xOffset -= 2 * offset) {
					pos.set(lx + xOffset, ly + yOffset, lz + zOffset);
					block += world.getLightLevel(LightType.BLOCK, pos) / 8f;
					sky += world.getLightLevel(LightType.SKY, pos) / 8f;
				}

		return LightmapTextureManager.pack((int) block, (int) sky);
	}

	public static int getContraptionWorldLight(MovementContext context, VirtualRenderWorld renderWorld) {
		return WorldRenderer.getLightmapCoordinates(renderWorld, context.localPos);
	}

	public static void reset() {
		WORLDS.empty(ContraptionRenderingWorld::delete);

		if (Backend.isOn()) {
			WORLDS = new WorldAttached<>(FlwContraptionManager::new);
		} else {
			WORLDS = new WorldAttached<>(SBBContraptionManager::new);
		}
	}

	public static boolean canInstance() {
		return Backend.getBackendType() == BackendType.INSTANCING;
	}
}