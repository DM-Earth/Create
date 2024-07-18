package com.simibubi.create.content.trains.schedule;

import com.simibubi.create.foundation.utility.AdventureUtil;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ScheduleItemEntityInteraction {

	public static ActionResult interactWithConductor(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {
		if (player == null || entity == null)
			return ActionResult.PASS;
		if (player.isSpectator() || AdventureUtil.isAdventure(player))
			return ActionResult.PASS;

		Entity rootVehicle = entity.getRootVehicle();
		if (!(rootVehicle instanceof CarriageContraptionEntity))
			return ActionResult.PASS;
		if (!(entity instanceof LivingEntity living))
			return ActionResult.PASS;
		if (player.getItemCooldownManager()
			.isCoolingDown(AllItems.SCHEDULE.get()))
			return ActionResult.PASS;

		ItemStack itemStack = player.getStackInHand(hand);
		if (itemStack.getItem()instanceof ScheduleItem si) {
			ActionResult result = si.handScheduleTo(itemStack, player, living, hand);
			if (result.isAccepted()) {
				player.getItemCooldownManager()
					.set(AllItems.SCHEDULE.get(), 5);
				return result;
			}
		}

		if (hand == Hand.OFF_HAND)
			return ActionResult.PASS;

		CarriageContraptionEntity cce = (CarriageContraptionEntity) rootVehicle;
		Contraption contraption = cce.getContraption();
		if (!(contraption instanceof CarriageContraption cc))
			return ActionResult.PASS;

		Train train = cce.getCarriage().train;
		if (train == null)
			return ActionResult.PASS;
		if (train.runtime.getSchedule() == null)
			return ActionResult.PASS;

		Integer seatIndex = contraption.getSeatMapping()
			.get(entity.getUuid());
		if (seatIndex == null)
			return ActionResult.PASS;
		BlockPos seatPos = contraption.getSeats()
			.get(seatIndex);
		Couple<Boolean> directions = cc.conductorSeats.get(seatPos);
		if (directions == null)
			return ActionResult.PASS;

		boolean onServer = !world.isClient;

		if (train.runtime.paused && !train.runtime.completed) {
			if (onServer) {
				train.runtime.paused = false;
				AllSoundEvents.CONFIRM.playOnServer(player.getWorld(), player.getBlockPos(), 1, 1);
				player.sendMessage(Lang.translateDirect("schedule.continued"), true);
			}

			player.getItemCooldownManager()
				.set(AllItems.SCHEDULE.get(), 5);
			return ActionResult.SUCCESS;
		}

		ItemStack itemInHand = player.getStackInHand(hand);
		if (!itemInHand.isEmpty()) {
			if (onServer) {
				AllSoundEvents.DENY.playOnServer(player.getWorld(), player.getBlockPos(), 1, 1);
				player.sendMessage(Lang.translateDirect("schedule.remove_with_empty_hand"), true);
			}
			return ActionResult.SUCCESS;
		}

		if (onServer) {
			AllSoundEvents.playItemPickup(player);
			player.sendMessage(
				Lang.translateDirect(
					train.runtime.isAutoSchedule ? "schedule.auto_removed_from_train" : "schedule.removed_from_train"),
				true);

			player.getInventory()
				.offerOrDrop(train.runtime.returnSchedule());
		}

		player.getItemCooldownManager()
			.set(AllItems.SCHEDULE.get(), 5);
		return ActionResult.SUCCESS;
	}

}
