package com.simibubi.create.foundation.gui.element;

import javax.annotation.Nullable;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.virtual.VirtualEmptyBlockGetter;
import com.jozufozu.flywheel.fabric.model.DefaultLayerFilteringBakedModel;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.gui.ILightingSettings;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.models.virtual.FixedColorTintingBakedModel;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class GuiGameElement {

	public static GuiRenderBuilder of(ItemStack stack) {
		return new GuiItemRenderBuilder(stack);
	}

	public static GuiRenderBuilder of(ItemConvertible itemProvider) {
		return new GuiItemRenderBuilder(itemProvider);
	}

	public static GuiRenderBuilder of(BlockState state) {
		return new GuiBlockStateRenderBuilder(state);
	}

	public static GuiRenderBuilder of(PartialModel partial) {
		return new GuiBlockPartialRenderBuilder(partial);
	}

	public static GuiRenderBuilder of(Fluid fluid) {
		return new GuiBlockStateRenderBuilder(fluid.getDefaultState()
			.getBlockState()
			.with(FluidBlock.LEVEL, 0));
	}

	public static abstract class GuiRenderBuilder extends RenderElement {
		protected double xLocal, yLocal, zLocal;
		protected double xRot, yRot, zRot;
		protected double scale = 1;
		protected int color = 0xFFFFFF;
		protected Vec3d rotationOffset = Vec3d.ZERO;
		protected ILightingSettings customLighting = null;

		public GuiRenderBuilder atLocal(double x, double y, double z) {
			this.xLocal = x;
			this.yLocal = y;
			this.zLocal = z;
			return this;
		}

		public GuiRenderBuilder rotate(double xRot, double yRot, double zRot) {
			this.xRot = xRot;
			this.yRot = yRot;
			this.zRot = zRot;
			return this;
		}

		public GuiRenderBuilder rotateBlock(double xRot, double yRot, double zRot) {
			return this.rotate(xRot, yRot, zRot)
				.withRotationOffset(VecHelper.getCenterOf(BlockPos.ORIGIN));
		}

		public GuiRenderBuilder scale(double scale) {
			this.scale = scale;
			return this;
		}

		public GuiRenderBuilder color(int color) {
			this.color = color;
			return this;
		}

		public GuiRenderBuilder withRotationOffset(Vec3d offset) {
			this.rotationOffset = offset;
			return this;
		}

		public GuiRenderBuilder lighting(ILightingSettings lighting) {
			customLighting = lighting;
			return this;
		}

		protected void prepareMatrix(MatrixStack matrixStack) {
			matrixStack.push();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
			prepareLighting(matrixStack);
		}

		protected void transformMatrix(MatrixStack matrixStack) {
			matrixStack.translate(x, y, z);
			matrixStack.scale((float) scale, (float) scale, (float) scale);
			matrixStack.translate(xLocal, yLocal, zLocal);
			UIRenderHelper.flipForGuiRender(matrixStack);
			matrixStack.translate(rotationOffset.x, rotationOffset.y, rotationOffset.z);
			matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) zRot));
			matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) xRot));
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) yRot));
			matrixStack.translate(-rotationOffset.x, -rotationOffset.y, -rotationOffset.z);
		}

		protected void cleanUpMatrix(MatrixStack matrixStack) {
			matrixStack.pop();
			cleanUpLighting(matrixStack);
		}

		protected void prepareLighting(MatrixStack matrixStack) {
			if (customLighting != null) {
				customLighting.applyLighting();
			} else {
				DiffuseLighting.enableGuiDepthLighting();
			}
		}

		protected void cleanUpLighting(MatrixStack matrixStack) {
			if (customLighting != null) {
				DiffuseLighting.enableGuiDepthLighting();
			}
		}
	}

	private static class GuiBlockModelRenderBuilder extends GuiRenderBuilder {

		protected BakedModel blockModel;
		protected BlockState blockState;

		public GuiBlockModelRenderBuilder(BakedModel blockmodel, @Nullable BlockState blockState) {
			this.blockState = blockState == null ? Blocks.AIR.getDefaultState() : blockState;
			this.blockModel = blockmodel;
		}

		@Override
		public void render(DrawContext graphics) {
			MatrixStack matrixStack = graphics.getMatrices();
			prepareMatrix(matrixStack);

			MinecraftClient mc = MinecraftClient.getInstance();
			BlockRenderManager blockRenderer = mc.getBlockRenderManager();
			VertexConsumerProvider.Immediate buffer = mc.getBufferBuilders()
				.getEntityVertexConsumers();
			RenderLayer renderType = blockState.getBlock() == Blocks.AIR ? TexturedRenderLayers.getEntityTranslucentCull()
				: RenderLayers.getEntityBlockLayer(blockState, true);
			VertexConsumer vb = buffer.getBuffer(renderType);

			transformMatrix(matrixStack);

			RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
			renderModel(blockRenderer, buffer, renderType, vb, matrixStack);

			cleanUpMatrix(matrixStack);
		}

		protected void renderModel(BlockRenderManager blockRenderer, VertexConsumerProvider.Immediate buffer,
			RenderLayer renderType, VertexConsumer vb, MatrixStack ms) {
			int color = MinecraftClient.getInstance()
				.getBlockColors()
				.getColor(blockState, null, null, 0);
//			Color rgb = new Color(color == -1 ? this.color : color);
//			blockRenderer.getModelRenderer()
//				.renderModel(ms.last(), vb, blockState, blockModel, rgb.getRedAsFloat(), rgb.getGreenAsFloat(), rgb.getBlueAsFloat(),
//					LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			BakedModel model = blockModel;
			model = DefaultLayerFilteringBakedModel.wrap(model);
			if (color == -1) {
				color = this.color;
			}
			if (color != -1) {
				model = FixedColorTintingBakedModel.wrap(model, color);
			}
			blockRenderer.getModelRenderer()
				.render(VirtualEmptyBlockGetter.FULL_BRIGHT, model, blockState, BlockPos.ORIGIN, ms, vb, false, Random.create(), 42L, OverlayTexture.DEFAULT_UV);
			buffer.draw();
		}

	}

	public static class GuiBlockStateRenderBuilder extends GuiBlockModelRenderBuilder {

		public GuiBlockStateRenderBuilder(BlockState blockstate) {
			super(MinecraftClient.getInstance()
				.getBlockRenderManager()
				.getModel(blockstate), blockstate);
		}

		@Override
		protected void renderModel(BlockRenderManager blockRenderer, VertexConsumerProvider.Immediate buffer,
			RenderLayer renderType, VertexConsumer vb, MatrixStack ms) {
			if (blockState.getBlock() instanceof AbstractFireBlock) {
				DiffuseLighting.disableGuiDepthLighting();
//				blockRenderer.renderSingleBlock(blockState, ms, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
//				buffer.endBatch();
				super.renderModel(blockRenderer, buffer, renderType, buffer.getBuffer(RenderLayers.getEntityBlockLayer(blockState, false)), ms);
				DiffuseLighting.enableGuiDepthLighting();
				return;
			}

			super.renderModel(blockRenderer, buffer, renderType, vb, ms);

			if (blockState.getFluidState()
				.isEmpty())
				return;

			FluidRenderer.renderFluidBox(new FluidStack(blockState.getFluidState()
				.getFluid(), FluidConstants.BUCKET), 0, 0, 0, 1, 1, 1, buffer, ms, LightmapTextureManager.MAX_LIGHT_COORDINATE, false);
			buffer.draw();
		}
	}

	public static class GuiBlockPartialRenderBuilder extends GuiBlockModelRenderBuilder {

		public GuiBlockPartialRenderBuilder(PartialModel partial) {
			super(partial.get(), null);
		}

	}

	public static class GuiItemRenderBuilder extends GuiRenderBuilder {

		private final ItemStack stack;

		public GuiItemRenderBuilder(ItemStack stack) {
			this.stack = stack;
		}

		public GuiItemRenderBuilder(ItemConvertible provider) {
			this(new ItemStack(provider));
		}

		@Override
		public void render(DrawContext graphics) {
			MatrixStack matrixStack = graphics.getMatrices();
			prepareMatrix(matrixStack);
			transformMatrix(matrixStack);
			renderItemIntoGUI(matrixStack, stack, customLighting == null);
			cleanUpMatrix(matrixStack);
		}

		public static void renderItemIntoGUI(MatrixStack matrixStack, ItemStack stack, boolean useDefaultLighting) {
			ItemRenderer renderer = MinecraftClient.getInstance().getItemRenderer();
			BakedModel bakedModel = renderer.getModel(stack, null, null, 0);

			MinecraftClient.getInstance().getTextureManager().getTexture(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).setFilter(false, false);
			RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
			RenderSystem.enableBlend();
			RenderSystem.enableCull();
			RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			matrixStack.push();
			matrixStack.translate(0, 0, 100.0F);
			matrixStack.translate(8.0F, -8.0F, 0.0F);
			matrixStack.scale(16.0F, 16.0F, 16.0F);
			VertexConsumerProvider.Immediate buffer = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
			boolean flatLighting = !bakedModel.isSideLit();
			if (useDefaultLighting && flatLighting) {
				DiffuseLighting.disableGuiDepthLighting();
			}

			renderer.renderItem(stack, ModelTransformationMode.GUI, false, matrixStack, buffer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, bakedModel);
			RenderSystem.disableDepthTest();
			buffer.draw();

			RenderSystem.enableDepthTest();
			if (useDefaultLighting && flatLighting) {
				DiffuseLighting.enableGuiDepthLighting();
			}

			matrixStack.pop();
		}

	}

}
