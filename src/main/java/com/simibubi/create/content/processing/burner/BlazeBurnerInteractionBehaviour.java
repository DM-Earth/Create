package com.simibubi.create.content.processing.burner;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleItem;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BlazeBurnerInteractionBehaviour extends MovingInteractionBehaviour {

	@Override
	public boolean handlePlayerInteraction(PlayerEntity player, Hand activeHand, BlockPos localPos,
		AbstractContraptionEntity contraptionEntity) {
		if (AdventureUtil.isAdventure(player))
			return false;
		ItemStack itemInHand = player.getStackInHand(activeHand);

		if (!(contraptionEntity instanceof CarriageContraptionEntity carriageEntity))
			return false;
		if (activeHand == Hand.OFF_HAND)
			return false;
		Contraption contraption = carriageEntity.getContraption();
		if (!(contraption instanceof CarriageContraption carriageContraption))
			return false;

		StructureBlockInfo info = carriageContraption.getBlocks()
			.get(localPos);
		if (info == null || !info.state().contains(BlazeBurnerBlock.HEAT_LEVEL)
			|| info.state().get(BlazeBurnerBlock.HEAT_LEVEL) == HeatLevel.NONE)
			return false;

		Direction assemblyDirection = carriageContraption.getAssemblyDirection();
		for (Direction direction : Iterate.directionsInAxis(assemblyDirection.getAxis())) {
			if (!carriageContraption.inControl(localPos, direction))
				continue;

			Train train = carriageEntity.getCarriage().train;
			if (train == null)
				return false;
			if (player.getWorld().isClient)
				return true;

			if (train.runtime.getSchedule() != null) {
				if (train.runtime.paused && !train.runtime.completed) {
					train.runtime.paused = false;
					AllSoundEvents.CONFIRM.playOnServer(player.getWorld(), player.getBlockPos(), 1, 1);
					player.sendMessage(Lang.translateDirect("schedule.continued"), true);
					return true;
				}

				if (!itemInHand.isEmpty()) {
					AllSoundEvents.DENY.playOnServer(player.getWorld(), player.getBlockPos(), 1, 1);
					player.sendMessage(Lang.translateDirect("schedule.remove_with_empty_hand"), true);
					return true;
				}

				AllSoundEvents.playItemPickup(player);
				player.sendMessage(Lang.translateDirect(
					train.runtime.isAutoSchedule ? "schedule.auto_removed_from_train" : "schedule.removed_from_train"),
					true);
				player.setStackInHand(activeHand, train.runtime.returnSchedule());
				return true;
			}

			if (!AllItems.SCHEDULE.isIn(itemInHand))
				return true;

			Schedule schedule = ScheduleItem.getSchedule(itemInHand);
			if (schedule == null)
				return false;

			if (schedule.entries.isEmpty()) {
				AllSoundEvents.DENY.playOnServer(player.getWorld(), player.getBlockPos(), 1, 1);
				player.sendMessage(Lang.translateDirect("schedule.no_stops"), true);
				return true;
			}

			train.runtime.setSchedule(schedule, false);
			AllAdvancements.CONDUCTOR.awardTo(player);
			AllSoundEvents.CONFIRM.playOnServer(player.getWorld(), player.getBlockPos(), 1, 1);
			player.sendMessage(Lang.translateDirect("schedule.applied_to_train")
				.formatted(Formatting.GREEN), true);
			itemInHand.decrement(1);
			player.setStackInHand(activeHand, itemInHand.isEmpty() ? ItemStack.EMPTY : itemInHand);
			return true;
		}

		player.sendMessage(Lang.translateDirect("schedule.non_controlling_seat"), true);
		AllSoundEvents.DENY.playOnServer(player.getWorld(), player.getBlockPos(), 1, 1);
		return true;
	}

}
