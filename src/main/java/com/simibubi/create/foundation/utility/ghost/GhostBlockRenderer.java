package com.simibubi.create.foundation.utility.ghost;

import com.jozufozu.flywheel.core.virtual.VirtualEmptyBlockGetter;
import com.jozufozu.flywheel.fabric.model.DefaultLayerFilteringBakedModel;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;

import io.github.fabricators_of_create.porting_lib.models.virtual.FixedLightBakedModel;
import io.github.fabricators_of_create.porting_lib.models.virtual.TranslucentBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public abstract class GhostBlockRenderer {

	private static final GhostBlockRenderer STANDARD = new DefaultGhostBlockRenderer();

	public static GhostBlockRenderer standard() {
		return STANDARD;
	}

	private static final GhostBlockRenderer TRANSPARENT = new TransparentGhostBlockRenderer();

	public static GhostBlockRenderer transparent() {
		return TRANSPARENT;
	}

	public abstract void render(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera, GhostBlockParams params);

	private static class DefaultGhostBlockRenderer extends GhostBlockRenderer {

		@Override
		public void render(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera, GhostBlockParams params) {
			ms.push();

			BlockRenderManager dispatcher = MinecraftClient.getInstance()
				.getBlockRenderManager();

			BakedModel model = dispatcher.getModel(params.state);

			RenderLayer layer = RenderLayers.getEntityBlockLayer(params.state, false);
			VertexConsumer vb = buffer.getEarlyBuffer(layer);

			BlockPos pos = params.pos;
			ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

			model = DefaultLayerFilteringBakedModel.wrap(model);
			dispatcher.getModelRenderer()
					.render(VirtualEmptyBlockGetter.FULL_BRIGHT, model, params.state, pos, ms, vb, false, Random.create(), 42L, OverlayTexture.DEFAULT_UV);

			ms.pop();
		}

	}

	private static class TransparentGhostBlockRenderer extends GhostBlockRenderer {

		@Override
		public void render(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera, GhostBlockParams params) {
			MinecraftClient mc = MinecraftClient.getInstance();
			BlockRenderManager dispatcher = mc.getBlockRenderManager();

			BlockState state = params.state;
			BlockPos pos = params.pos;
			float alpha = params.alphaSupplier.get() * .75f * PlacementHelpers.getCurrentAlpha();

			BakedModel model = dispatcher.getModel(state);
			RenderLayer layer = RenderLayer.getTranslucent();
			VertexConsumer vb = buffer.getEarlyBuffer(layer);

			ms.push();
			ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

			ms.translate(.5, .5, .5);
			ms.scale(.85f, .85f, .85f);
			ms.translate(-.5, -.5, -.5);

			model = DefaultLayerFilteringBakedModel.wrap(model);
			model = FixedLightBakedModel.wrap(model, WorldRenderer.getLightmapCoordinates(mc.world, pos));
			model = TranslucentBakedModel.wrap(model, () -> params.alphaSupplier.get() * .75f * PlacementHelpers.getCurrentAlpha());
			dispatcher.getModelRenderer()
					.render(VirtualEmptyBlockGetter.INSTANCE, model, params.state, pos, ms, vb, false, Random.create(), 42L, OverlayTexture.DEFAULT_UV);

			ms.pop();
		}

	}

}
