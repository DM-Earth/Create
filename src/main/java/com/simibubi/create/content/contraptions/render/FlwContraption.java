package com.simibubi.create.content.contraptions.render;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.gl.GlStateTracker;
import com.jozufozu.flywheel.backend.gl.GlStateTracker.State;
import com.jozufozu.flywheel.backend.instancing.Engine;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import com.jozufozu.flywheel.backend.instancing.SerialTaskEngine;
import com.jozufozu.flywheel.backend.instancing.batching.BatchingEngine;
import com.jozufozu.flywheel.backend.instancing.instancing.InstancingEngine;
import com.jozufozu.flywheel.backend.model.ArrayModelRenderer;
import com.jozufozu.flywheel.core.model.Model;
import com.jozufozu.flywheel.core.model.WorldModelBuilder;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.render.CreateContexts;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

public class FlwContraption extends ContraptionRenderInfo {

	private final ContraptionLighter<?> lighter;

	private final Map<RenderLayer, ArrayModelRenderer> renderLayers = new HashMap<>();

	private final Matrix4f modelViewPartial = new Matrix4f();
	private final ContraptionInstanceWorld instanceWorld;
	private boolean modelViewPartialReady;
	// floats because we upload this to the gpu
	private Box lightBox;

	public FlwContraption(Contraption contraption, VirtualRenderWorld renderWorld) {
		super(contraption, renderWorld);
		this.lighter = contraption.makeLighter();

		instanceWorld = new ContraptionInstanceWorld(this);

		var restoreState = GlStateTracker.getRestoreState();
		buildLayers();
		if (ContraptionRenderDispatcher.canInstance()) {
			buildInstancedBlockEntities();
			buildActors();
		}
		restoreState.restore();
	}

	public ContraptionLighter<?> getLighter() {
		return lighter;
	}

	public void renderStructureLayer(RenderLayer layer, ContraptionProgram shader) {
		ArrayModelRenderer structure = renderLayers.get(layer);
		if (structure != null) {
			setup(shader);
			structure.draw();
		}
	}

	public void renderInstanceLayer(RenderLayerEvent event) {

		event.stack.push();
		float partialTicks = AnimationTickHolder.getPartialTicks();
		AbstractContraptionEntity entity = contraption.entity;
		double x = MathHelper.lerp(partialTicks, entity.lastRenderX, entity.getX());
		double y = MathHelper.lerp(partialTicks, entity.lastRenderY, entity.getY());
		double z = MathHelper.lerp(partialTicks, entity.lastRenderZ, entity.getZ());
		event.stack.translate(x - event.camX, y - event.camY, z - event.camZ);
		ContraptionMatrices.transform(event.stack, getMatrices().getModel());
		instanceWorld.engine.render(SerialTaskEngine.INSTANCE, event);

		event.stack.pop();
	}

	public void beginFrame(BeginFrameEvent event) {
		super.beginFrame(event);

		modelViewPartial.identity();
		modelViewPartialReady = false;

		if (!isVisible()) return;

		instanceWorld.blockEntityInstanceManager.beginFrame(SerialTaskEngine.INSTANCE, event.getCamera());

		Vec3d cameraPos = event.getCameraPos();

		lightBox = lighter.lightVolume.toAABB()
				.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
	}

	@Override
	public void setupMatrices(MatrixStack viewProjection, double camX, double camY, double camZ) {
		super.setupMatrices(viewProjection, camX, camY, camZ);

		if (!modelViewPartialReady) {
			setupModelViewPartial(modelViewPartial, getMatrices().getModel().peek().getPositionMatrix(), contraption.entity, camX, camY, camZ, AnimationTickHolder.getPartialTicks());
			modelViewPartialReady = true;
		}
	}

	void setup(ContraptionProgram shader) {
		if (!modelViewPartialReady || lightBox == null) return;
		shader.bind(modelViewPartial, lightBox);
		lighter.lightVolume.bind();
	}

	public void invalidate() {
		for (ArrayModelRenderer renderer : renderLayers.values()) {
			renderer.delete();
			renderer.getModel().delete();
		}
		renderLayers.clear();

		lighter.delete();

		instanceWorld.delete();
	}

	private void buildLayers() {
		for (ArrayModelRenderer renderer : renderLayers.values()) {
			renderer.delete();
			renderer.getModel().delete();
		}

		renderLayers.clear();

		List<RenderLayer> blockLayers = RenderLayer.getBlockLayers();
		Collection<StructureBlockInfo> renderedBlocks = contraption.getRenderedBlocks();

		for (RenderLayer layer : blockLayers) {
			Model layerModel = new WorldModelBuilder(layer).withRenderWorld(renderWorld)
					.withBlocks(renderedBlocks)
					.toModel(layer + "_" + contraption.entity.getId());
			renderLayers.put(layer, new ArrayModelRenderer(layerModel));
		}
	}

	private void buildInstancedBlockEntities() {
		for (BlockEntity be : contraption.maybeInstancedBlockEntities) {
			if (!InstancedRenderRegistry.canInstance(be.getType())) {
				continue;
			}

			World world = be.getWorld();
			be.setWorld(renderWorld);
			instanceWorld.blockEntityInstanceManager.add(be);
			be.setWorld(world);
		}
	}

	private void buildActors() {
		contraption.getActors().forEach(instanceWorld.blockEntityInstanceManager::createActor);
	}

	public static void setupModelViewPartial(Matrix4f matrix, Matrix4f modelMatrix, AbstractContraptionEntity entity, double camX, double camY, double camZ, float pt) {
		float x = (float) (MathHelper.lerp(pt, entity.lastRenderX, entity.getX()) - camX);
		float y = (float) (MathHelper.lerp(pt, entity.lastRenderY, entity.getY()) - camY);
		float z = (float) (MathHelper.lerp(pt, entity.lastRenderZ, entity.getZ()) - camZ);
		matrix.setTranslation(x, y, z);
		matrix.mul(modelMatrix);
	}

	public void tick() {
		instanceWorld.blockEntityInstanceManager.tick();
	}

	public static class ContraptionInstanceWorld {

		private final Engine engine;
		private final ContraptionInstanceManager blockEntityInstanceManager;

		public ContraptionInstanceWorld(FlwContraption parent) {
			switch (Backend.getBackendType()) {
			case INSTANCING -> {
				InstancingEngine<ContraptionProgram> engine = InstancingEngine.builder(CreateContexts.CWORLD)
						.setGroupFactory(ContraptionGroup.forContraption(parent))
						.setIgnoreOriginCoordinate(true)
						.build();
				blockEntityInstanceManager = new ContraptionInstanceManager(engine, parent.renderWorld, parent.contraption);
				engine.addListener(blockEntityInstanceManager);

				this.engine = engine;
			}
			case BATCHING -> {
				engine = new BatchingEngine();
				blockEntityInstanceManager = new ContraptionInstanceManager(engine, parent.renderWorld, parent.contraption);
			}
			default -> throw new IllegalArgumentException("Unknown engine type");
			}
		}

		public void delete() {
			engine.delete();
			blockEntityInstanceManager.invalidate();
		}
	}
}
