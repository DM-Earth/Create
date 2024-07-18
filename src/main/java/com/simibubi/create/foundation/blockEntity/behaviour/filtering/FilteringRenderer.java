package com.simibubi.create.foundation.blockEntity.behaviour.filtering;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.ItemValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class FilteringRenderer {

	public static void tick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		HitResult target = mc.crosshairTarget;
		if (target == null || !(target instanceof BlockHitResult))
			return;

		BlockHitResult result = (BlockHitResult) target;
		ClientWorld world = mc.world;
		BlockPos pos = result.getBlockPos();
		BlockState state = world.getBlockState(pos);

		FilteringBehaviour behaviour = BlockEntityBehaviour.get(world, pos, FilteringBehaviour.TYPE);
		if (mc.player.isSneaking())
			return;

		ItemStack mainhandItem = mc.player.getStackInHand(Hand.MAIN_HAND);
		if (behaviour == null)
			return;
		if (behaviour instanceof SidedFilteringBehaviour) {
			behaviour = ((SidedFilteringBehaviour) behaviour).get(result.getSide());
			if (behaviour == null)
				return;
		}
		if (!behaviour.isActive())
			return;
		if (behaviour.slotPositioning instanceof ValueBoxTransform.Sided)
			((Sided) behaviour.slotPositioning).fromSide(result.getSide());
		if (!behaviour.slotPositioning.shouldRender(state))
			return;

		ItemStack filter = behaviour.getFilter();
		boolean isFilterSlotted = filter.getItem() instanceof FilterItem;
		boolean showCount = behaviour.isCountVisible();
		Text label = behaviour.getLabel();
		boolean hit = behaviour.slotPositioning.testHit(state, target.getPos()
			.subtract(Vec3d.of(pos)));

		Box emptyBB = new Box(Vec3d.ZERO, Vec3d.ZERO);
		Box bb = isFilterSlotted ? emptyBB.expand(.45f, .31f, .2f) : emptyBB.expand(.25f);

		ValueBox box = new ItemValueBox(label, bb, pos, filter, showCount ? behaviour.count : -1, behaviour.upTo);
		box.passive(!hit || AllBlocks.CLIPBOARD.isIn(mainhandItem));

		CreateClient.OUTLINER.showValueBox(Pair.of("filter", pos), box.transform(behaviour.slotPositioning))
			.lineWidth(1 / 64f)
			.withFaceTexture(hit ? AllSpecialTextures.THIN_CHECKERED : null)
			.highlightFace(result.getSide());

		if (!hit)
			return;

		List<MutableText> tip = new ArrayList<>();
		tip.add(label.copy());
		tip.add(Lang
			.translateDirect(filter.isEmpty() ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace"));
		if (showCount)
			tip.add(Lang.translateDirect("logistics.filter.hold_to_set_amount"));

		CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
	}

	public static void renderOnBlockEntity(SmartBlockEntity be, float partialTicks, MatrixStack ms,
		VertexConsumerProvider buffer, int light, int overlay) {

		if (be == null || be.isRemoved())
			return;

		if (!be.isVirtual()) {
			Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
			if (cameraEntity != null && be.getWorld() == cameraEntity.getWorld()) {
				float max = AllConfigs.client().filterItemRenderDistance.getF();
				if (cameraEntity.getPos()
					.squaredDistanceTo(VecHelper.getCenterOf(be.getPos())) > (max * max)) {
					return;
				}
			}
		}

		FilteringBehaviour behaviour = be.getBehaviour(FilteringBehaviour.TYPE);
		if (behaviour == null)
			return;
		if (!behaviour.isActive())
			return;
		if (behaviour.getFilter()
			.isEmpty() && !(behaviour instanceof SidedFilteringBehaviour))
			return;

		ValueBoxTransform slotPositioning = behaviour.slotPositioning;
		BlockState blockState = be.getCachedState();

		if (slotPositioning instanceof ValueBoxTransform.Sided) {
			ValueBoxTransform.Sided sided = (ValueBoxTransform.Sided) slotPositioning;
			Direction side = sided.getSide();
			for (Direction d : Iterate.directions) {
				ItemStack filter = behaviour.getFilter(d);
				if (filter.isEmpty())
					continue;

				sided.fromSide(d);
				if (!slotPositioning.shouldRender(blockState))
					continue;

				ms.push();
				slotPositioning.transform(blockState, ms);
				if (AllBlocks.CONTRAPTION_CONTROLS.has(blockState))
					ValueBoxRenderer.renderFlatItemIntoValueBox(filter, ms, buffer, light, overlay);
				else
					ValueBoxRenderer.renderItemIntoValueBox(filter, ms, buffer, light, overlay);
				ms.pop();
			}
			sided.fromSide(side);
			return;
		} else if (slotPositioning.shouldRender(blockState)) {
			ms.push();
			slotPositioning.transform(blockState, ms);
			ValueBoxRenderer.renderItemIntoValueBox(behaviour.getFilter(), ms, buffer, light, overlay);
			ms.pop();
		}
	}

}
