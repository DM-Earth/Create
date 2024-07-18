package com.simibubi.create.content.redstone.link;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class LinkRenderer {

	public static void tick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		HitResult target = mc.crosshairTarget;
		if (target == null || !(target instanceof BlockHitResult))
			return;

		BlockHitResult result = (BlockHitResult) target;
		ClientWorld world = mc.world;
		BlockPos pos = result.getBlockPos();

		LinkBehaviour behaviour = BlockEntityBehaviour.get(world, pos, LinkBehaviour.TYPE);
		if (behaviour == null)
			return;

		Text freq1 = Lang.translateDirect("logistics.firstFrequency");
		Text freq2 = Lang.translateDirect("logistics.secondFrequency");

		for (boolean first : Iterate.trueAndFalse) {
			Box bb = new Box(Vec3d.ZERO, Vec3d.ZERO).expand(.25f);
			Text label = first ? freq1 : freq2;
			boolean hit = behaviour.testHit(first, target.getPos());
			ValueBoxTransform transform = first ? behaviour.firstSlot : behaviour.secondSlot;

			ValueBox box = new ValueBox(label, bb, pos).passive(!hit);
			boolean empty = behaviour.getNetworkKey()
				.get(first)
				.getStack()
				.isEmpty();

			if (!empty)
				box.wideOutline();

			CreateClient.OUTLINER.showValueBox(Pair.of(Boolean.valueOf(first), pos), box.transform(transform))
				.highlightFace(result.getSide());

			if (!hit)
				continue;

			List<MutableText> tip = new ArrayList<>();
			tip.add(label.copy());
			tip.add(
				Lang.translateDirect(empty ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace"));
			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
		}
	}

	public static void renderOnBlockEntity(SmartBlockEntity be, float partialTicks, MatrixStack ms,
		VertexConsumerProvider buffer, int light, int overlay) {

		if (be == null || be.isRemoved())
			return;

		Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
		float max = AllConfigs.client().filterItemRenderDistance.getF();
		if (!be.isVirtual() && cameraEntity != null && cameraEntity.getPos()
			.squaredDistanceTo(VecHelper.getCenterOf(be.getPos())) > (max * max))
			return;

		LinkBehaviour behaviour = be.getBehaviour(LinkBehaviour.TYPE);
		if (behaviour == null)
			return;

		for (boolean first : Iterate.trueAndFalse) {
			ValueBoxTransform transform = first ? behaviour.firstSlot : behaviour.secondSlot;
			ItemStack stack = first ? behaviour.frequencyFirst.getStack() : behaviour.frequencyLast.getStack();

			ms.push();
			transform.transform(be.getCachedState(), ms);
			ValueBoxRenderer.renderItemIntoValueBox(stack, ms, buffer, light, overlay);
			ms.pop();
		}

	}

}
