package com.simibubi.create.compat.emi;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.gui.CustomLightingSettings;
import com.simibubi.create.foundation.gui.ILightingSettings;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import dev.emi.emi.api.widget.WidgetHolder;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

public class CreateEmiAnimations {
	private static final BlockState WHEEL = AllBlocks.CRUSHING_WHEEL.getDefaultState().with(Properties.AXIS, Axis.X);
	public static final ILightingSettings DEFAULT_LIGHTING = CustomLightingSettings.builder()
		.firstLightRotation(12.5f, 45.0f)
		.secondLightRotation(-20.0f, 50.0f)
		.build();


	public static GuiGameElement.GuiRenderBuilder defaultBlockElement(BlockState state) {
		return GuiGameElement.of(state)
				.lighting(DEFAULT_LIGHTING);
	}

	public static GuiGameElement.GuiRenderBuilder defaultBlockElement(PartialModel partial) {
		return GuiGameElement.of(partial)
				.lighting(DEFAULT_LIGHTING);
	}

	public static float getCurrentAngle() {
		return (AnimationTickHolder.getRenderTime() * 4f) % 360;
	}

	public static BlockState shaft(Axis axis) {
		return AllBlocks.SHAFT.getDefaultState().with(Properties.AXIS, axis);
	}

	public static PartialModel cogwheel() {
		return AllPartialModels.SHAFTLESS_COGWHEEL;
	}

	public static GuiGameElement.GuiRenderBuilder blockElement(BlockState state) {
		return defaultBlockElement(state);
	}

	public static GuiGameElement.GuiRenderBuilder blockElement(PartialModel partial) {
		return defaultBlockElement(partial);
	}

	public static void addPress(WidgetHolder widgets, int x, int y, boolean basin) {
		widgets.addDrawable(x, y, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			renderPress(graphics, 0, basin);
		});
	}

	public static void renderPress(DrawContext graphics, int offset, boolean basin) {
		MatrixStack matrices = graphics.getMatrices();
		matrices.translate(0, 0, 200);
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
		int scale = basin ? 23 : 24;

		blockElement(shaft(Axis.Z))
				.rotateBlock(0, 0, getCurrentAngle())
				.scale(scale)
				.render(graphics);

		blockElement(AllBlocks.MECHANICAL_PRESS.getDefaultState())
				.scale(scale)
				.render(graphics);

		blockElement(AllPartialModels.MECHANICAL_PRESS_HEAD)
				.atLocal(0, -getAnimatedHeadOffset(offset), 0)
				.scale(scale)
				.render(graphics);

		if (basin) {
			blockElement(AllBlocks.BASIN.getDefaultState())
					.atLocal(0, 1.65, 0)
					.scale(scale)
					.render(graphics);
		}
	}

	private static float getAnimatedHeadOffset(int offset) {
		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		if (cycle < 10) {
			float progress = cycle / 10;
			return -(progress * progress * progress);
		}
		if (cycle < 15)
			return -1;
		if (cycle < 20)
			return -1 + (1 - ((20 - cycle) / 5));
		return 0;
	}

	public static void addBlazeBurner(WidgetHolder widgets, int x, int y, HeatLevel heatLevel) {
		widgets.addDrawable(x, y, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			MatrixStack matrixStack = graphics.getMatrices();
			matrixStack.translate(0, 0, 200);
			matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
			matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
			int scale = 23;

			float offset = (MathHelper.sin(AnimationTickHolder.getRenderTime() / 16f) + 0.5f) / 16f;

			blockElement(AllBlocks.BLAZE_BURNER.getDefaultState()).atLocal(0, 1.65, 0)
					.scale(scale)
					.render(graphics);

			PartialModel blaze =
					heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_SUPER : AllPartialModels.BLAZE_ACTIVE;
			PartialModel rods2 = heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS_2
					: AllPartialModels.BLAZE_BURNER_RODS_2;

			blockElement(blaze).atLocal(1, 1.8, 1)
					.rotate(0, 180, 0)
					.scale(scale)
					.render(graphics);
			blockElement(rods2).atLocal(1, 1.7 + offset, 1)
					.rotate(0, 180, 0)
					.scale(scale)
					.render(graphics);

			matrixStack.scale(scale, -scale, scale);
			matrixStack.translate(0, -1.8, 0);

			SpriteShiftEntry spriteShift =
					heatLevel == HeatLevel.SEETHING ? AllSpriteShifts.SUPER_BURNER_FLAME : AllSpriteShifts.BURNER_FLAME;

			float spriteWidth = spriteShift.getTarget()
					.getMaxU()
					- spriteShift.getTarget()
					.getMinU();

			float spriteHeight = spriteShift.getTarget()
					.getMaxV()
					- spriteShift.getTarget()
					.getMinV();

			float time = AnimationTickHolder.getRenderTime(MinecraftClient.getInstance().world);
			float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();

			double vScroll = speed * time;
			vScroll = vScroll - Math.floor(vScroll);
			vScroll = vScroll * spriteHeight / 2;

			double uScroll = speed * time / 2;
			uScroll = uScroll - Math.floor(uScroll);
			uScroll = uScroll * spriteWidth / 2;

			MinecraftClient mc = MinecraftClient.getInstance();
			VertexConsumerProvider.Immediate buffer = mc.getBufferBuilders()
					.getEntityVertexConsumers();
			VertexConsumer vb = buffer.getBuffer(RenderLayer.getCutoutMipped());
			CachedBufferer.partial(AllPartialModels.BLAZE_BURNER_FLAME, Blocks.AIR.getDefaultState())
					.shiftUVScrolling(spriteShift, (float) uScroll, (float) vScroll)
					.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
					.renderInto(matrixStack, vb);
		});
	}

	public static void addMixer(WidgetHolder widgets, int x, int y) {
		widgets.addDrawable(x, y, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			MatrixStack matrices = graphics.getMatrices();
			matrices.translate(0, 0, 200);
			matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
			matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
			int scale = 23;

			blockElement(cogwheel())
				.rotateBlock(0, getCurrentAngle() * 2, 0)
				.atLocal(0, 0, 0)
				.scale(scale)
				.render(graphics);

			blockElement(AllBlocks.MECHANICAL_MIXER.getDefaultState())
				.atLocal(0, 0, 0)
				.scale(scale)
				.render(graphics);

			float animation = ((MathHelper.sin(AnimationTickHolder.getRenderTime() / 32f) + 1) / 5) + .5f;

			blockElement(AllPartialModels.MECHANICAL_MIXER_POLE)
				.atLocal(0, animation, 0)
				.scale(scale)
				.render(graphics);

			blockElement(AllPartialModels.MECHANICAL_MIXER_HEAD)
				.rotateBlock(0, getCurrentAngle() * 4, 0)
				.atLocal(0, animation, 0)
				.scale(scale)
				.render(graphics);

			blockElement(AllBlocks.BASIN.getDefaultState())
				.atLocal(0, 1.65, 0)
				.scale(scale)
				.render(graphics);
		});
	}

	public static void addSaw(WidgetHolder widgets, int x, int y) {
		widgets.addDrawable(x, y, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			renderSaw(graphics, 0);
		});
	}

	public static void renderSaw(DrawContext graphics, int offset) {
		MatrixStack matrices = graphics.getMatrices();
		matrices.translate(0, 0, 200);
		matrices.translate(2, 22, 0);
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(22.5f + 90));
		int scale = 25;

		blockElement(shaft(Axis.X))
			.rotateBlock(-getCurrentAngle(), 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.MECHANICAL_SAW.getDefaultState()
			.with(SawBlock.FACING, Direction.UP))
			.rotateBlock(0, 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE)
			.rotateBlock(0, -90, -90)
			.scale(scale)
			.render(graphics);
	}

	public static void addMillstone(WidgetHolder widgets, int x, int y) {
		widgets.addDrawable(x, y, 0, 0, (matrices, mouseX, mouseY, delta) -> {
			int scale = 22;

			blockElement(AllPartialModels.MILLSTONE_COG)
				.rotateBlock(22.5, getCurrentAngle() * 2, 0)
				.scale(scale)
				.render(matrices);

			blockElement(AllBlocks.MILLSTONE.getDefaultState())
				.rotateBlock(22.5, 22.5, 0)
				.scale(scale)
				.render(matrices);
		});
	}

	public static void addCrushingWheels(WidgetHolder widgets, int x, int y) {
		widgets.addDrawable(x, y, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			MatrixStack matrices = graphics.getMatrices();
			matrices.translate(0, 0, 100);
			matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-22.5f));
			int scale = 22;

			blockElement(WHEEL)
					.rotateBlock(0, 90, -getCurrentAngle())
					.scale(scale)
					.render(graphics);

			blockElement(WHEEL)
					.rotateBlock(0, 90, getCurrentAngle())
					.atLocal(2, 0, 0)
					.scale(scale)
					.render(graphics);
		});
	}

	public static void addFan(WidgetHolder widgets, int x, int y, Consumer<DrawContext> renderAttachedBlock) {
		widgets.addDrawable(x, y, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			MatrixStack matrices = graphics.getMatrices();
			matrices.translate(0, 0, 200);
			matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-12.5f));
			matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
			int scale = 24;

			defaultBlockElement(AllPartialModels.ENCASED_FAN_INNER)
				.rotateBlock(180, 0, getCurrentAngle() * 16)
				.scale(scale)
				.render(graphics);

			defaultBlockElement(AllBlocks.ENCASED_FAN.getDefaultState())
				.rotateBlock(0, 180, 0)
				.atLocal(0, 0, 0)
				.scale(scale)
				.render(graphics);

			renderAttachedBlock.accept(graphics);
		});
	}

	public static void addDeployer(WidgetHolder widgets, int x, int y) {
		widgets.addDrawable(x, y, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			renderDeployer(graphics, 0);
		});
	}

	public static void renderDeployer(DrawContext graphics, int offset) {
		MatrixStack matrices = graphics.getMatrices();
		matrices.translate(0, 0, 100);
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
		int scale = 20;

		blockElement(shaft(Axis.Z))
			.rotateBlock(0, 0, getCurrentAngle())
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.DEPLOYER.getDefaultState()
			.with(DeployerBlock.FACING, Direction.DOWN)
			.with(DeployerBlock.AXIS_ALONG_FIRST_COORDINATE, false))
			.scale(scale)
			.render(graphics);

		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		float off = cycle < 10 ? cycle / 10f : cycle < 20 ? (20 - cycle) / 10f : 0;

		matrices.push();

		matrices.translate(0, off * 17, 0);
		blockElement(AllPartialModels.DEPLOYER_POLE)
			.rotateBlock(90, 0, 0)
			.scale(scale)
			.render(graphics);
		blockElement(AllPartialModels.DEPLOYER_HAND_HOLDING)
			.rotateBlock(90, 0, 0)
			.scale(scale)
			.render(graphics);

		matrices.pop();

		blockElement(AllBlocks.DEPOT.getDefaultState())
			.atLocal(0, 2, 0)
			.scale(scale)
			.render(graphics);
	}

	public static void addSpout(WidgetHolder widgets, int x, int y, List<FluidStack> fluids) {
		widgets.addDrawable(x, y, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			renderSpout(graphics, 0, fluids);
		});
	}

	public static void renderSpout(DrawContext graphics, int offset, List<FluidStack> fluids) {
		MatrixStack matrices = graphics.getMatrices();
		matrices.translate(0, 0, 100);
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
		int scale = 20;

		blockElement(AllBlocks.SPOUT.getDefaultState())
			.scale(scale)
			.render(graphics);

		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		float squeeze = cycle < 20 ? MathHelper.sin((float) (cycle / 20f * Math.PI)) : 0;
		squeeze *= 20;

		matrices.push();

		blockElement(AllPartialModels.SPOUT_TOP)
			.scale(scale)
			.render(graphics);
		matrices.translate(0, -3 * squeeze / 32f, 0);
		blockElement(AllPartialModels.SPOUT_MIDDLE)
			.scale(scale)
			.render(graphics);
		matrices.translate(0, -3 * squeeze / 32f, 0);
		blockElement(AllPartialModels.SPOUT_BOTTOM)
			.scale(scale)
			.render(graphics);
		matrices.translate(0, -3 * squeeze / 32f, 0);

		matrices.pop();

		blockElement(AllBlocks.DEPOT.getDefaultState())
			.atLocal(0, 2, 0)
			.scale(scale)
			.render(graphics);

		DEFAULT_LIGHTING.applyLighting();
		VertexConsumerProvider.Immediate buffer = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
		matrices.push();
		UIRenderHelper.flipForGuiRender(matrices);
		matrices.scale(16, 16, 16);
		float from = 3f / 16f;
		float to = 17f / 16f;
		FluidRenderer.renderFluidBox(fluids.get(0), from, from, from, to, to, to, buffer, matrices,
			LightmapTextureManager.MAX_LIGHT_COORDINATE, false);
		matrices.pop();

		float width = 1 / 128f * squeeze;
		matrices.translate(scale / 2f, scale * 1.5f, scale / 2f);
		UIRenderHelper.flipForGuiRender(matrices);
		matrices.scale(16, 16, 16);
		matrices.translate(-0.5f, 0, -0.5f);
		from = -width / 2 + 0.5f;
		to = width / 2 + 0.5f;
		FluidRenderer.renderFluidBox(fluids.get(0), from, 0, from, to, 2, to, buffer, matrices,
			LightmapTextureManager.MAX_LIGHT_COORDINATE, false);
		buffer.draw();
		DiffuseLighting.enableGuiDepthLighting();
	}

	public static void addDrain(WidgetHolder widgets, int x, int y, FluidStack fluid) {
		widgets.addDrawable(x, y, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			MatrixStack matrices = graphics.getMatrices();
			matrices.translate(0, 0, 100);
			matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
			matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
			int scale = 20;

			blockElement(AllBlocks.ITEM_DRAIN.getDefaultState())
				.scale(scale)
				.render(graphics);

			VertexConsumerProvider.Immediate buffer = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
			//MatrixStack ms = new MatrixStack();
			UIRenderHelper.flipForGuiRender(matrices);
			matrices.scale(scale, scale, scale);
			float from = 2 / 16f;
			float to = 1f - from;
			FluidRenderer.renderFluidBox(fluid, from, from, from, to, 3/4f, to, buffer, matrices,
				LightmapTextureManager.MAX_LIGHT_COORDINATE, false);
			buffer.draw();
		});
	}

	public static void addCrafter(WidgetHolder widgets, int x, int y) {
		widgets.addDrawable(x, y, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			MatrixStack matrices = graphics.getMatrices();
			matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
			matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-22.5f));
			int scale = 22;

			blockElement(cogwheel())
				.rotateBlock(90, 0, getCurrentAngle())
				.scale(scale)
				.render(graphics);

			blockElement(AllBlocks.MECHANICAL_CRAFTER.getDefaultState())
				.rotateBlock(0, 180, 0)
				.scale(scale)
				.render(graphics);
		});
	}
}
