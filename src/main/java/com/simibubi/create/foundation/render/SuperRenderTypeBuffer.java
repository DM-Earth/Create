package com.simibubi.create.foundation.render;

import java.util.SortedMap;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.util.Util;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class SuperRenderTypeBuffer implements VertexConsumerProvider {

	private static final SuperRenderTypeBuffer INSTANCE = new SuperRenderTypeBuffer();

	public static SuperRenderTypeBuffer getInstance() {
		return INSTANCE;
	}

	private SuperRenderTypeBufferPhase earlyBuffer;
	private SuperRenderTypeBufferPhase defaultBuffer;
	private SuperRenderTypeBufferPhase lateBuffer;

	public SuperRenderTypeBuffer() {
		earlyBuffer = new SuperRenderTypeBufferPhase();
		defaultBuffer = new SuperRenderTypeBufferPhase();
		lateBuffer = new SuperRenderTypeBufferPhase();
	}

	public VertexConsumer getEarlyBuffer(RenderLayer type) {
		return earlyBuffer.bufferSource.getBuffer(type);
	}

	@Override
	public VertexConsumer getBuffer(RenderLayer type) {
		return defaultBuffer.bufferSource.getBuffer(type);
	}

	public VertexConsumer getLateBuffer(RenderLayer type) {
		return lateBuffer.bufferSource.getBuffer(type);
	}

	public void draw() {
		earlyBuffer.bufferSource.draw();
		defaultBuffer.bufferSource.draw();
		lateBuffer.bufferSource.draw();
	}

	public void draw(RenderLayer type) {
		earlyBuffer.bufferSource.draw(type);
		defaultBuffer.bufferSource.draw(type);
		lateBuffer.bufferSource.draw(type);
	}

	private static class SuperRenderTypeBufferPhase {

		// Visible clones from RenderBuffers
		private final BlockBufferBuilderStorage fixedBufferPack = new BlockBufferBuilderStorage();
		private final SortedMap<RenderLayer, BufferBuilder> fixedBuffers = Util.make(new Object2ObjectLinkedOpenHashMap<>(), map -> {
				map.put(TexturedRenderLayers.getEntitySolid(), fixedBufferPack.get(RenderLayer.getSolid()));
				map.put(TexturedRenderLayers.getEntityCutout(), fixedBufferPack.get(RenderLayer.getCutout()));
				map.put(TexturedRenderLayers.getBannerPatterns(), fixedBufferPack.get(RenderLayer.getCutoutMipped()));
				map.put(TexturedRenderLayers.getEntityTranslucentCull(), fixedBufferPack.get(RenderLayer.getTranslucent()));
				put(map, TexturedRenderLayers.getShieldPatterns());
				put(map, TexturedRenderLayers.getBeds());
				put(map, TexturedRenderLayers.getShulkerBoxes());
				put(map, TexturedRenderLayers.getSign());
				put(map, TexturedRenderLayers.getChest());
				put(map, RenderLayer.getTranslucentNoCrumbling());
				put(map, RenderLayer.getArmorGlint());
				put(map, RenderLayer.getArmorEntityGlint());
				put(map, RenderLayer.getGlint());
				put(map, RenderLayer.getDirectGlint());
				put(map, RenderLayer.getGlintTranslucent());
				put(map, RenderLayer.getEntityGlint());
				put(map, RenderLayer.getDirectEntityGlint());
				put(map, RenderLayer.getWaterMask());
				put(map, RenderTypes.getOutlineSolid());
				ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach((p_173062_) -> {
					put(map, p_173062_);
				});
			});
		private final VertexConsumerProvider.Immediate bufferSource = VertexConsumerProvider.immediate(fixedBuffers, new BufferBuilder(256));

		private static void put(Object2ObjectLinkedOpenHashMap<RenderLayer, BufferBuilder> map, RenderLayer type) {
			map.put(type, new BufferBuilder(type.getExpectedBufferSize()));
		}

	}

}
