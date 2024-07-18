package com.simibubi.create.content.kinetics.mechanicalArm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode;
import com.simibubi.create.foundation.utility.Lang;

public class ArmInteractionPointHandler {

	static List<ArmInteractionPoint> currentSelection = new ArrayList<>();
	static ItemStack currentItem;

	static long lastBlockPos = -1;

	public static ActionResult rightClickingBlocksSelectsThem(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		if (currentItem == null)
			return ActionResult.PASS;
		BlockPos pos = hitResult.getBlockPos();//event.getPos();
//		Level world = event.getWorld();
		if (!world.isClient)
			return ActionResult.PASS;
//		Player player = event.getPlayer();
		if (player != null && player.isSpectator())
			return ActionResult.PASS;

		ArmInteractionPoint selected = getSelected(pos);
		BlockState state = world.getBlockState(pos);

		if (selected == null) {
			ArmInteractionPoint point = ArmInteractionPoint.create(world, pos, state);
			if (point == null)
				return ActionResult.PASS;
			selected = point;
			put(point);
		}

		selected.cycleMode();
		if (player != null) {
			Mode mode = selected.getMode();
			Lang.builder()
				.translate(mode.getTranslationKey(), Lang.blockName(state)
					.style(Formatting.WHITE))
				.color(mode.getColor())
				.sendStatus(player);
		}

//		event.setCanceled(true);
//		event.setCancellationResult(InteractionResult.SUCCESS);
		return ActionResult.SUCCESS;
	}

	public static ActionResult leftClickingBlocksDeselectsThem(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
		if (currentItem == null)
			return ActionResult.PASS;
		if (!world.isClient)
			return ActionResult.PASS;
		if (player.isSpectator())
			return ActionResult.PASS;
//		BlockPos pos = event.getPos();
		if (remove(pos) != null) {
//			event.setCanceled(true);
//			event.setCancellationResult(InteractionResult.SUCCESS);
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	public static void flushSettings(BlockPos pos) {
		if (currentSelection == null)
			return;

		int removed = 0;
		for (Iterator<ArmInteractionPoint> iterator = currentSelection.iterator(); iterator.hasNext();) {
			ArmInteractionPoint point = iterator.next();
			if (point.getPos()
				.isWithinDistance(pos, ArmBlockEntity.getRange()))
				continue;
			iterator.remove();
			removed++;
		}

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (removed > 0) {
			Lang.builder()
				.translate("mechanical_arm.points_outside_range", removed)
				.style(Formatting.RED)
				.sendStatus(player);
		} else {
			int inputs = 0;
			int outputs = 0;
			for (ArmInteractionPoint armInteractionPoint : currentSelection) {
				if (armInteractionPoint.getMode() == Mode.DEPOSIT)
					outputs++;
				else
					inputs++;
			}
			if (inputs + outputs > 0)
				Lang.builder()
					.translate("mechanical_arm.summary", inputs, outputs)
					.style(Formatting.WHITE)
					.sendStatus(player);
		}

		AllPackets.getChannel().sendToServer(new ArmPlacementPacket(currentSelection, pos));
		currentSelection.clear();
		currentItem = null;
	}

	public static void tick() {
		PlayerEntity player = MinecraftClient.getInstance().player;

		if (player == null)
			return;

		ItemStack heldItemMainhand = player.getMainHandStack();
		if (!AllBlocks.MECHANICAL_ARM.isIn(heldItemMainhand)) {
			currentItem = null;
		} else {
			if (heldItemMainhand != currentItem) {
				currentSelection.clear();
				currentItem = heldItemMainhand;
			}

			drawOutlines(currentSelection);
		}

		checkForWrench(heldItemMainhand);
	}

	private static void checkForWrench(ItemStack heldItem) {
		if (!AllItems.WRENCH.isIn(heldItem)) {
			return;
		}

		HitResult objectMouseOver = MinecraftClient.getInstance().crosshairTarget;
		if (!(objectMouseOver instanceof BlockHitResult)) {
			return;
		}

		BlockHitResult result = (BlockHitResult) objectMouseOver;
		BlockPos pos = result.getBlockPos();

		BlockEntity be = MinecraftClient.getInstance().world.getBlockEntity(pos);
		if (!(be instanceof ArmBlockEntity)) {
			lastBlockPos = -1;
			currentSelection.clear();
			return;
		}

		if (lastBlockPos == -1 || lastBlockPos != pos.asLong()) {
			currentSelection.clear();
			ArmBlockEntity arm = (ArmBlockEntity) be;
			arm.inputs.forEach(ArmInteractionPointHandler::put);
			arm.outputs.forEach(ArmInteractionPointHandler::put);
			lastBlockPos = pos.asLong();
		}

		if (lastBlockPos != -1) {
			drawOutlines(currentSelection);
		}
	}

	private static void drawOutlines(Collection<ArmInteractionPoint> selection) {
		for (Iterator<ArmInteractionPoint> iterator = selection.iterator(); iterator.hasNext();) {
			ArmInteractionPoint point = iterator.next();

			if (!point.isValid()) {
				iterator.remove();
				continue;
			}

			World level = point.getLevel();
			BlockPos pos = point.getPos();
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getOutlineShape(level, pos);
			if (shape.isEmpty())
				continue;

			int color = point.getMode()
				.getColor();
			CreateClient.OUTLINER.showAABB(point, shape.getBoundingBox()
				.offset(pos))
				.colored(color)
				.lineWidth(1 / 16f);
		}
	}

	private static void put(ArmInteractionPoint point) {
		currentSelection.add(point);
	}

	private static ArmInteractionPoint remove(BlockPos pos) {
		ArmInteractionPoint result = getSelected(pos);
		if (result != null)
			currentSelection.remove(result);
		return result;
	}

	private static ArmInteractionPoint getSelected(BlockPos pos) {
		for (ArmInteractionPoint point : currentSelection)
			if (point.getPos()
				.equals(pos))
				return point;
		return null;
	}

}
