package com.simibubi.create.content.schematics.client;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import com.jozufozu.flywheel.core.model.ModelUtil;
import com.jozufozu.flywheel.core.model.ShadeSeparatedBufferedData;
import com.jozufozu.flywheel.core.model.ShadeSeparatingVertexConsumer;
import com.jozufozu.flywheel.fabric.model.LayerFilteringBakedModel;
import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.foundation.render.BlockEntityRenderHelper;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;

public class SchematicRenderer {

	private static final ThreadLocal<ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(ThreadLocalObjects::new);

	private final Map<RenderLayer, SuperByteBuffer> bufferCache = new LinkedHashMap<>();
	private boolean active;
	private boolean changed;
	protected SchematicWorld schematic;
	private BlockPos anchor;

	public SchematicRenderer() {
		changed = false;
	}

	public void display(SchematicWorld world) {
		this.anchor = world.anchor;
		this.schematic = world;
		this.active = true;
		this.changed = true;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void update() {
		changed = true;
	}

	public void tick() {
		if (!active)
			return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null || mc.player == null || !changed)
			return;

		redraw();
		changed = false;
	}

	public void render(MatrixStack ms, SuperRenderTypeBuffer buffers) {
		if (!active)
			return;
		bufferCache.forEach((layer, buffer) -> {
			buffer.renderInto(ms, buffers.getBuffer(layer));
		});
		BlockEntityRenderHelper.renderBlockEntities(schematic, schematic.getRenderedBlockEntities(), ms, buffers);
	}

	protected void redraw() {
		bufferCache.forEach((layer, sbb) -> sbb.delete());
		bufferCache.clear();

		for (RenderLayer layer : RenderLayer.getBlockLayers()) {
			SuperByteBuffer buffer = drawLayer(layer);
			if (!buffer.isEmpty())
				bufferCache.put(layer, buffer);
			else
				buffer.delete();
		}
	}

	protected SuperByteBuffer drawLayer(RenderLayer layer) {
		BlockRenderManager dispatcher = MinecraftClient.getInstance().getBlockRenderManager();
		BlockModelRenderer renderer = dispatcher.getModelRenderer();
		ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();

		MatrixStack poseStack = objects.poseStack;
		Random random = objects.random;
		BlockPos.Mutable mutableBlockPos = objects.mutableBlockPos;
		SchematicWorld renderWorld = schematic;
		renderWorld.renderMode = true;
		BlockBox bounds = renderWorld.getBounds();

		ShadeSeparatingVertexConsumer shadeSeparatingWrapper = objects.shadeSeparatingWrapper;
		BufferBuilder shadedBuilder = objects.shadedBuilder;
		BufferBuilder unshadedBuilder = objects.unshadedBuilder;

		shadedBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
		unshadedBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
		shadeSeparatingWrapper.prepare(shadedBuilder, unshadedBuilder);

		BlockModelRenderer.enableBrightnessCache();
		for (BlockPos localPos : BlockPos.iterate(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(), bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ())) {
			BlockPos pos = mutableBlockPos.set(localPos, anchor);
			BlockState state = renderWorld.getBlockState(pos);

			if (state.getRenderType() == BlockRenderType.MODEL) {
				BakedModel model = dispatcher.getModel(state);
				long seed = state.getRenderingSeed(pos);
				random.setSeed(seed);
				if (model.isVanillaAdapter()) {
					if (RenderLayers.getBlockLayer(state) != layer) {
						continue;
					}
				} else {
					model = LayerFilteringBakedModel.wrap(model, layer);
				}
				model = shadeSeparatingWrapper.wrapModel(model);

				poseStack.push();
				poseStack.translate(localPos.getX(), localPos.getY(), localPos.getZ());

				renderer.render(renderWorld, model, state, pos, poseStack, shadeSeparatingWrapper, true, random,
						seed, OverlayTexture.DEFAULT_UV);

				poseStack.pop();
			}
		}
		BlockModelRenderer.disableBrightnessCache();

		shadeSeparatingWrapper.clear();
		ShadeSeparatedBufferedData bufferedData = ModelUtil.endAndCombine(shadedBuilder, unshadedBuilder);

		renderWorld.renderMode = false;

		SuperByteBuffer sbb = new SuperByteBuffer(bufferedData);
		bufferedData.release();
		return sbb;
	}

	// fabric: calling chunkBufferLayers early causes issues (#612), let the map handle its size on its own
//	private static int getLayerCount() {
//		return RenderType.chunkBufferLayers()
//			.size();
//	}

	private static class ThreadLocalObjects {
		public final MatrixStack poseStack = new MatrixStack();
		public final Random random = Random.createLocal();
		public final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
		public final ShadeSeparatingVertexConsumer shadeSeparatingWrapper = new ShadeSeparatingVertexConsumer();
		public final BufferBuilder shadedBuilder = new BufferBuilder(512);
		public final BufferBuilder unshadedBuilder = new BufferBuilder(512);
	}

}
