package com.simibubi.create.foundation.blockEntity.behaviour.edgeInteraction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.crafter.CrafterHelper;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;

public class EdgeInteractionRenderer {

	public static void tick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		HitResult target = mc.crosshairTarget;
		if (target == null || !(target instanceof BlockHitResult))
			return;

		BlockHitResult result = (BlockHitResult) target;
		ClientWorld world = mc.world;
		BlockPos pos = result.getBlockPos();
		PlayerEntity player = mc.player;
		ItemStack heldItem = player.getMainHandStack();

		if (player.isSneaking())
			return;
		EdgeInteractionBehaviour behaviour = BlockEntityBehaviour.get(world, pos, EdgeInteractionBehaviour.TYPE);
		if (behaviour == null)
			return;
		if (!behaviour.requiredPredicate.test(heldItem.getItem()) && heldItem.getItem() != heldItem.getItem())
			return;

		Direction face = result.getSide();
		List<Direction> connectiveSides = EdgeInteractionHandler.getConnectiveSides(world, pos, face, behaviour);
		if (connectiveSides.isEmpty())
			return;

		Direction closestEdge = connectiveSides.get(0);
		double bestDistance = Double.MAX_VALUE;
		Vec3d center = VecHelper.getCenterOf(pos);
		for (Direction direction : connectiveSides) {
			double distance = Vec3d.of(direction.getVector())
				.subtract(target.getPos()
					.subtract(center))
				.length();
			if (distance > bestDistance)
				continue;
			bestDistance = distance;
			closestEdge = direction;
		}

		Box bb = EdgeInteractionHandler.getBB(pos, closestEdge);
		boolean hit = bb.contains(target.getPos());
		Vec3d offset = Vec3d.of(closestEdge.getVector())
			.multiply(.5)
			.add(Vec3d.of(face.getVector())
				.multiply(.469))
			.add(VecHelper.CENTER_OF_ORIGIN);

		ValueBox box = new ValueBox(Components.immutableEmpty(), bb, pos).passive(!hit)
			.transform(new EdgeValueBoxTransform(offset))
			.wideOutline();
		CreateClient.OUTLINER.showValueBox("edge", box)
			.highlightFace(face);

		if (!hit)
			return;

		List<MutableText> tip = new ArrayList<>();
		tip.add(Lang.translateDirect("logistics.crafter.connected"));
		tip.add(Lang.translateDirect(CrafterHelper.areCraftersConnected(world, pos, pos.offset(closestEdge))
			? "logistics.crafter.click_to_separate"
			: "logistics.crafter.click_to_merge"));
		CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
	}

	static class EdgeValueBoxTransform extends ValueBoxTransform.Sided {

		private Vec3d add;

		public EdgeValueBoxTransform(Vec3d add) {
			this.add = add;
		}

		@Override
		protected Vec3d getSouthLocation() {
			return Vec3d.ZERO;
		}

		@Override
		public Vec3d getLocalOffset(BlockState state) {
			return add;
		}

		@Override
		public void rotate(BlockState state, MatrixStack ms) {
			super.rotate(state, ms);
		}

	}

}
