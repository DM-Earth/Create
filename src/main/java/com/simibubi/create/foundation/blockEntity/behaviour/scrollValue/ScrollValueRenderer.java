package com.simibubi.create.foundation.blockEntity.behaviour.scrollValue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
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
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllTags;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.IconValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.TextValueBox;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

public class ScrollValueRenderer {

	public static void tick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		HitResult target = mc.crosshairTarget;
		if (target == null || !(target instanceof BlockHitResult))
			return;

		BlockHitResult result = (BlockHitResult) target;
		ClientWorld world = mc.world;
		BlockPos pos = result.getBlockPos();
		Direction face = result.getSide();

		ScrollValueBehaviour behaviour = BlockEntityBehaviour.get(world, pos, ScrollValueBehaviour.TYPE);
		if (behaviour == null)
			return;
		if (!behaviour.isActive()) {
			CreateClient.OUTLINER.remove(pos);
			return;
		}
		ItemStack mainhandItem = mc.player.getStackInHand(Hand.MAIN_HAND);
		boolean clipboard = AllBlocks.CLIPBOARD.isIn(mainhandItem);
		if (behaviour.needsWrench && !AllItems.WRENCH.isIn(mainhandItem) && !clipboard)
			return;
		boolean highlight = behaviour.testHit(target.getPos()) && !clipboard;

		if (behaviour instanceof BulkScrollValueBehaviour && AllKeys.ctrlDown()) {
			BulkScrollValueBehaviour bulkScrolling = (BulkScrollValueBehaviour) behaviour;
			for (SmartBlockEntity smartBlockEntity : bulkScrolling.getBulk()) {
				ScrollValueBehaviour other = smartBlockEntity.getBehaviour(ScrollValueBehaviour.TYPE);
				if (other != null)
					addBox(world, smartBlockEntity.getPos(), face, other, highlight);
			}
		} else
			addBox(world, pos, face, behaviour, highlight);

		if (!highlight)
			return;

		List<MutableText> tip = new ArrayList<>();
		tip.add(behaviour.label.copy());
		tip.add(Lang.translateDirect("gui.value_settings.hold_to_edit"));
		CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
	}

	protected static void addBox(ClientWorld world, BlockPos pos, Direction face, ScrollValueBehaviour behaviour,
		boolean highlight) {
		Box bb = new Box(Vec3d.ZERO, Vec3d.ZERO).expand(.5f)
			.shrink(0, 0, -.5f)
			.offset(0, 0, -.125f);
		Text label = behaviour.label;
		ValueBox box;

		if (behaviour instanceof ScrollOptionBehaviour) {
			box = new IconValueBox(label, ((ScrollOptionBehaviour<?>) behaviour).getIconForSelected(), bb, pos);
		} else {
			box = new TextValueBox(label, bb, pos, Components.literal(behaviour.formatValue()));
		}

		if (!AdventureUtil.isAdventure(MinecraftClient.getInstance().player))
			box.passive(!highlight)
			.wideOutline();

		CreateClient.OUTLINER.showValueBox(pos, box.transform(behaviour.slotPositioning))
			.highlightFace(face);
	}

}
