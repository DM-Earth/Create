package com.simibubi.create.content.contraptions.render;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.render.SuperByteBufferCache;
import com.simibubi.create.foundation.utility.Pair;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.world.WorldAccess;

public class SBBContraptionManager extends ContraptionRenderingWorld<ContraptionRenderInfo> {
	public static final SuperByteBufferCache.Compartment<Pair<Contraption, RenderLayer>> CONTRAPTION = new SuperByteBufferCache.Compartment<>();

	public SBBContraptionManager(WorldAccess world) {
		super(world);
	}

	@Override
	public void renderLayer(RenderLayerEvent event) {
		super.renderLayer(event);
		RenderLayer type = event.getType();
		VertexConsumer consumer = event.buffers.getEntityVertexConsumers()
				.getBuffer(type);
		visible.forEach(info -> renderContraptionLayerSBB(info, type, consumer));

		event.buffers.getEntityVertexConsumers().draw(type);
	}

	@Override
	public boolean invalidate(Contraption contraption) {
		for (RenderLayer chunkBufferLayer : RenderLayer.getBlockLayers()) {
			CreateClient.BUFFER_CACHE.invalidate(CONTRAPTION, Pair.of(contraption, chunkBufferLayer));
		}
		return super.invalidate(contraption);
	}

	@Override
	protected ContraptionRenderInfo create(Contraption c) {
		VirtualRenderWorld renderWorld = ContraptionRenderDispatcher.setupRenderWorld(world, c);
		return new ContraptionRenderInfo(c, renderWorld);
	}

	private void renderContraptionLayerSBB(ContraptionRenderInfo renderInfo, RenderLayer layer, VertexConsumer consumer) {
		if (!renderInfo.isVisible()) return;

		SuperByteBuffer contraptionBuffer = CreateClient.BUFFER_CACHE.get(CONTRAPTION, Pair.of(renderInfo.contraption, layer), () -> ContraptionRenderDispatcher.buildStructureBuffer(renderInfo.renderWorld, renderInfo.contraption, layer));

		if (!contraptionBuffer.isEmpty()) {
			ContraptionMatrices matrices = renderInfo.getMatrices();

			contraptionBuffer.transform(matrices.getModel())
					.light(matrices.getWorld())
					.hybridLight()
					.renderInto(matrices.getViewProjection(), consumer);
		}

	}
}
