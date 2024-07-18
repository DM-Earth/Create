package com.simibubi.create.content.equipment.symmetryWand;

import java.util.Random;

import com.jozufozu.flywheel.fabric.model.DefaultLayerFilteringBakedModel;

import org.joml.Vector3f;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.symmetryWand.mirror.EmptyMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SymmetryHandler {

	private static int tickCounter = 0;
	private static boolean handlingSymmetry = false; // fabric: prevent infinite recursion in break event listening

	public static void onBlockPlaced(ItemPlacementContext context, BlockPos pos, BlockState state) {
		if (context.getWorld()
			.isClient())
			return;

		Item held = context.getStack().getItem();
		if (!(held instanceof BlockItem))
			return;

		PlayerEntity player = context.getPlayer();
		if (player == null || AdventureUtil.isAdventure(player))
			return;
		PlayerInventory inv = player.getInventory();
		for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
			if (!inv.getStack(i)
				.isEmpty()
				&& inv.getStack(i)
					.getItem() == AllItems.WAND_OF_SYMMETRY.get()) {
				SymmetryWandItem.apply(player.getWorld(), inv.getStack(i), player, pos, state);
			}
		}
	}

	public static boolean onBlockDestroyed(World world, PlayerEntity player, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		if (handlingSymmetry || AdventureUtil.isAdventure(player))
			return true;

		if (world
			.isClient())
			return true;

		if (player.isSpectator())
			return true;

//		Player player = event.getPlayer();
		PlayerInventory inv = player.getInventory();
		handlingSymmetry = true;
		for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
			if (!inv.getStack(i)
				.isEmpty() && AllItems.WAND_OF_SYMMETRY.isIn(inv.getStack(i))) {
				SymmetryWandItem.remove(player.getWorld(), inv.getStack(i), player, pos, state);
			}
		}
		handlingSymmetry = false;
		return true;
	}

	@Environment(EnvType.CLIENT)
	public static void render(WorldRenderContext context) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create();

		for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
			ItemStack stackInSlot = player.getInventory()
				.getStack(i);
			if (!AllItems.WAND_OF_SYMMETRY.isIn(stackInSlot))
				continue;
			if (!SymmetryWandItem.isEnabled(stackInSlot))
				continue;
			SymmetryMirror mirror = SymmetryWandItem.getMirror(stackInSlot);
			if (mirror instanceof EmptyMirror)
				continue;

			BlockPos pos = BlockPos.ofFloored(mirror.getPosition());

			float yShift = 0;
			double speed = 1 / 16d;
			yShift = MathHelper.sin((float) (AnimationTickHolder.getRenderTime() * speed)) / 5f;

			VertexConsumerProvider.Immediate buffer = mc.getBufferBuilders()
				.getEntityVertexConsumers();
			Camera info = mc.gameRenderer.getCamera();
			Vec3d view = info.getPos();

			MatrixStack ms = context.matrixStack();
			ms.push();
			ms.translate(pos.getX() - view.getX(), pos.getY() - view.getY(), pos.getZ() - view.getZ());
			ms.translate(0, yShift + .2f, 0);
			mirror.applyModelTransform(ms);
			BakedModel model = mirror.getModel()
				.get();
			VertexConsumer builder = buffer.getBuffer(RenderLayer.getSolid());

			model = DefaultLayerFilteringBakedModel.wrap(model);
			mc.getBlockRenderManager()
				.getModelRenderer()
				.render(player.getWorld(), model, Blocks.AIR.getDefaultState(), pos, ms, builder, true,
					random, MathHelper.hashCode(pos), OverlayTexture.DEFAULT_UV);

			ms.pop();
			buffer.draw();
		}
	}

	@Environment(EnvType.CLIENT)
	public static void onClientTick(MinecraftClient client) {
//		if (event.phase == Phase.START)
//			return;
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;

		if (mc.world == null)
			return;
		if (mc.isPaused())
			return;

		tickCounter++;

		if (tickCounter % 10 == 0) {
			for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
				ItemStack stackInSlot = player.getInventory()
					.getStack(i);

				if (stackInSlot != null && AllItems.WAND_OF_SYMMETRY.isIn(stackInSlot)
					&& SymmetryWandItem.isEnabled(stackInSlot)) {

					SymmetryMirror mirror = SymmetryWandItem.getMirror(stackInSlot);
					if (mirror instanceof EmptyMirror)
						continue;

					net.minecraft.util.math.random.Random r = net.minecraft.util.math.random.Random.create();
					double offsetX = (r.nextDouble() - 0.5) * 0.3;
					double offsetZ = (r.nextDouble() - 0.5) * 0.3;

					Vec3d pos = mirror.getPosition()
						.add(0.5 + offsetX, 1 / 4d, 0.5 + offsetZ);
					Vec3d speed = new Vec3d(0, r.nextDouble() * 1 / 8f, 0);
					mc.world.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
				}
			}
		}

	}

	public static void drawEffect(BlockPos from, BlockPos to) {
		double density = 0.8f;
		Vec3d start = Vec3d.of(from)
			.add(0.5, 0.5, 0.5);
		Vec3d end = Vec3d.of(to)
			.add(0.5, 0.5, 0.5);
		Vec3d diff = end.subtract(start);

		Vec3d step = diff.normalize()
			.multiply(density);
		int steps = (int) (diff.length() / step.length());

		net.minecraft.util.math.random.Random r = net.minecraft.util.math.random.Random.create();
		for (int i = 3; i < steps - 1; i++) {
			Vec3d pos = start.add(step.multiply(i));
			Vec3d speed = new Vec3d(0, r.nextDouble() * -40f, 0);

			MinecraftClient.getInstance().world.addParticle(new DustParticleEffect(new Vector3f(1, 1, 1), 1), pos.x, pos.y,
				pos.z, speed.x, speed.y, speed.z);
		}

		Vec3d speed = new Vec3d(0, r.nextDouble() * 1 / 32f, 0);
		Vec3d pos = start.add(step.multiply(2));
		MinecraftClient.getInstance().world.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, speed.x, speed.y,
			speed.z);

		speed = new Vec3d(0, r.nextDouble() * 1 / 32f, 0);
		pos = start.add(step.multiply(steps));
		MinecraftClient.getInstance().world.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, speed.x, speed.y,
			speed.z);
	}

}
