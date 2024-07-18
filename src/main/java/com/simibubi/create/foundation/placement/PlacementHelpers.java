package com.simibubi.create.foundation.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import io.github.fabricators_of_create.porting_lib.event.client.OverlayRenderCallback;
import io.github.fabricators_of_create.porting_lib.event.client.OverlayRenderCallback.Types;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class PlacementHelpers {

	private static final List<IPlacementHelper> helpers = new ArrayList<>();
	private static int animationTick = 0;
	private static final LerpedFloat angle = LerpedFloat.angular()
		.chase(0, 0.25f, Chaser.EXP);
	private static BlockPos target = null;
	private static BlockPos lastTarget = null;

	public static int register(IPlacementHelper helper) {
		helpers.add(helper);
		return helpers.size() - 1;
	}

	public static IPlacementHelper get(int id) {
		if (id < 0 || id >= helpers.size())
			throw new ArrayIndexOutOfBoundsException("id " + id + " for placement helper not known");

		return helpers.get(id);
	}

	@Environment(EnvType.CLIENT)
	public static void tick() {
		setTarget(null);
		checkHelpers();

		if (target == null) {
			if (animationTick > 0)
				animationTick = Math.max(animationTick - 2, 0);

			return;
		}

		if (animationTick < 10)
			animationTick++;

	}

	@Environment(EnvType.CLIENT)
	private static void checkHelpers() {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientWorld world = mc.world;

		if (world == null)
			return;

		if (!(mc.crosshairTarget instanceof BlockHitResult))
			return;

		BlockHitResult ray = (BlockHitResult) mc.crosshairTarget;

		if (mc.player == null)
			return;

		if (mc.player.isSneaking())// for now, disable all helpers when sneaking TODO add helpers that respect
										// sneaking but still show position
			return;

		for (Hand hand : Hand.values()) {

			ItemStack heldItem = mc.player.getStackInHand(hand);
			List<IPlacementHelper> filteredForHeldItem = helpers.stream()
				.filter(helper -> helper.matchesItem(heldItem))
				.collect(Collectors.toList());
			if (filteredForHeldItem.isEmpty())
				continue;

			BlockPos pos = ray.getBlockPos();
			BlockState state = world.getBlockState(pos);

			List<IPlacementHelper> filteredForState = filteredForHeldItem.stream()
				.filter(helper -> helper.matchesState(state))
				.collect(Collectors.toList());
			if (filteredForState.isEmpty())
				continue;

			boolean atLeastOneMatch = false;
			for (IPlacementHelper h : filteredForState) {
				PlacementOffset offset = h.getOffset(mc.player, world, state, pos, ray, heldItem);

				if (offset.isSuccessful()) {
					h.renderAt(pos, state, ray, offset);
					setTarget(offset.getBlockPos());
					atLeastOneMatch = true;
					break;
				}

			}

			// at least one helper activated, no need to check the offhand if we are still
			// in the mainhand
			if (atLeastOneMatch)
				return;

		}
	}

	static void setTarget(BlockPos target) {
		PlacementHelpers.target = target;

		if (target == null)
			return;

		if (lastTarget == null) {
			lastTarget = target;
			return;
		}

		if (!lastTarget.equals(target))
			lastTarget = target;
	}

	@Environment(EnvType.CLIENT)
	public static void afterRenderOverlayLayer(DrawContext graphics, float partialTicks, Window res) {
		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;

		if (player != null && animationTick > 0) {
			float screenY = res.getScaledHeight() / 2f;
			float screenX = res.getScaledWidth() / 2f;
			float progress = getCurrentAlpha();

			drawDirectionIndicator(graphics, partialTicks, screenX, screenY, progress);
		}
	}

	public static float getCurrentAlpha() {
		return Math.min(animationTick / 10f/* + event.getPartialTicks() */, 1f);
	}

	@Environment(EnvType.CLIENT)
	private static void drawDirectionIndicator(DrawContext graphics, float partialTicks, float centerX, float centerY,
		float progress) {
		float r = .8f;
		float g = .8f;
		float b = .8f;
		float a = progress * progress;

		Vec3d projTarget = VecHelper.projectToPlayerView(VecHelper.getCenterOf(lastTarget), partialTicks);

		Vec3d target = new Vec3d(projTarget.x, projTarget.y, 0);
		if (projTarget.z > 0)
			target = target.negate();

		Vec3d norm = target.normalize();
		Vec3d ref = new Vec3d(0, 1, 0);
		float targetAngle = AngleHelper.deg(Math.acos(norm.dotProduct(ref)));

		if (norm.x < 0)
			targetAngle = 360 - targetAngle;

		if (animationTick < 10)
			angle.setValue(targetAngle);

		angle.chase(targetAngle, .25f, Chaser.EXP);
		angle.tickChaser();

		float snapSize = 22.5f;
		float snappedAngle = (snapSize * Math.round(angle.getValue(0f) / snapSize)) % 360f;

		float length = 10;

		CClient.PlacementIndicatorSetting mode = AllConfigs.client().placementIndicator.get();
		MatrixStack ms = graphics.getMatrices();
		if (mode == CClient.PlacementIndicatorSetting.TRIANGLE)
			fadedArrow(ms, centerX, centerY, r, g, b, a, length, snappedAngle);
		else if (mode == CClient.PlacementIndicatorSetting.TEXTURE)
			textured(ms, centerX, centerY, a, snappedAngle);
	}

	private static void fadedArrow(MatrixStack ms, float centerX, float centerY, float r, float g, float b, float a,
		float length, float snappedAngle) {
//		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);

		ms.push();
		ms.translate(centerX, centerY, 5);
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle.getValue(0)));
		// RenderSystem.rotatef(snappedAngle, 0, 0, 1);
		double scale = AllConfigs.client().indicatorScale.get();
		ms.scale((float) scale, (float) scale, 1);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

		Matrix4f mat = ms.peek()
			.getPositionMatrix();

		bufferbuilder.vertex(mat, 0, -(10 + length), 0)
			.color(r, g, b, a)
			.next();

		bufferbuilder.vertex(mat, -9, -3, 0)
			.color(r, g, b, 0f)
			.next();
		bufferbuilder.vertex(mat, -6, -6, 0)
			.color(r, g, b, 0f)
			.next();
		bufferbuilder.vertex(mat, -3, -8, 0)
			.color(r, g, b, 0f)
			.next();
		bufferbuilder.vertex(mat, 0, -8.5f, 0)
			.color(r, g, b, 0f)
			.next();
		bufferbuilder.vertex(mat, 3, -8, 0)
			.color(r, g, b, 0f)
			.next();
		bufferbuilder.vertex(mat, 6, -6, 0)
			.color(r, g, b, 0f)
			.next();
		bufferbuilder.vertex(mat, 9, -3, 0)
			.color(r, g, b, 0f)
			.next();

		tessellator.draw();
		RenderSystem.disableBlend();
//		RenderSystem.enableTexture();
		ms.pop();
	}

	@Environment(EnvType.CLIENT)
	public static void textured(MatrixStack ms, float centerX, float centerY, float alpha, float snappedAngle) {
//		RenderSystem.enableTexture();
		AllGuiTextures.PLACEMENT_INDICATOR_SHEET.bind();
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);

		ms.push();
		ms.translate(centerX, centerY, 50);
		float scale = AllConfigs.client().indicatorScale.get()
			.floatValue() * .75f;
		ms.scale(scale, scale, 1);
		ms.scale(12, 12, 1);

		float index = snappedAngle / 22.5f;
		float tex_size = 16f / 256f;

		float tx = 0;
		float ty = index * tex_size;
		float tw = 1f;
		float th = tex_size;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);

		Matrix4f mat = ms.peek()
			.getPositionMatrix();
		buffer.vertex(mat, -1, -1, 0)
			.color(1f, 1f, 1f, alpha)
			.texture(tx, ty)
			.next();
		buffer.vertex(mat, -1, 1, 0)
			.color(1f, 1f, 1f, alpha)
			.texture(tx, ty + th)
			.next();
		buffer.vertex(mat, 1, 1, 0)
			.color(1f, 1f, 1f, alpha)
			.texture(tx + tw, ty + th)
			.next();
		buffer.vertex(mat, 1, -1, 0)
			.color(1f, 1f, 1f, alpha)
			.texture(tx + tw, ty)
			.next();

		tessellator.draw();

		RenderSystem.disableBlend();
		ms.pop();
	}

}
