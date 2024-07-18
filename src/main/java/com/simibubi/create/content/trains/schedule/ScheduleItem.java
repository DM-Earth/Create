package com.simibubi.create.content.trains.schedule;

import java.util.List;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ScheduleItem extends Item implements NamedScreenHandlerFactory {

	public ScheduleItem(Settings pProperties) {
		super(pProperties);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (context.getPlayer() == null)
			return ActionResult.PASS;
		return use(context.getWorld(), context.getPlayer(), context.getHand()).getResult();
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack heldItem = player.getStackInHand(hand);

		if (!player.isSneaking() && hand == Hand.MAIN_HAND) {
			if (!world.isClient && player instanceof ServerPlayerEntity)
				NetworkHooks.openScreen((ServerPlayerEntity) player, this, buf -> {
					buf.writeItemStack(heldItem);
				});
			return TypedActionResult.success(heldItem);
		}
		return TypedActionResult.pass(heldItem);
	}

	public ActionResult handScheduleTo(ItemStack pStack, PlayerEntity pPlayer, LivingEntity pInteractionTarget,
		Hand pUsedHand) {
		ActionResult pass = ActionResult.PASS;

		Schedule schedule = getSchedule(pStack);
		if (schedule == null)
			return pass;
		if (pInteractionTarget == null)
			return pass;
		Entity rootVehicle = pInteractionTarget.getRootVehicle();
		if (!(rootVehicle instanceof CarriageContraptionEntity))
			return pass;
		if (pPlayer.getWorld().isClient)
			return ActionResult.SUCCESS;

		CarriageContraptionEntity entity = (CarriageContraptionEntity) rootVehicle;
		Contraption contraption = entity.getContraption();
		if (contraption instanceof CarriageContraption cc) {

			Train train = entity.getCarriage().train;
			if (train == null)
				return ActionResult.SUCCESS;

			Integer seatIndex = contraption.getSeatMapping()
				.get(pInteractionTarget.getUuid());
			if (seatIndex == null)
				return ActionResult.SUCCESS;
			BlockPos seatPos = contraption.getSeats()
				.get(seatIndex);
			Couple<Boolean> directions = cc.conductorSeats.get(seatPos);
			if (directions == null) {
				pPlayer.sendMessage(Lang.translateDirect("schedule.non_controlling_seat"), true);
				AllSoundEvents.DENY.playOnServer(pPlayer.getWorld(), pPlayer.getBlockPos(), 1, 1);
				return ActionResult.SUCCESS;
			}

			if (train.runtime.getSchedule() != null) {
				AllSoundEvents.DENY.playOnServer(pPlayer.getWorld(), pPlayer.getBlockPos(), 1, 1);
				pPlayer.sendMessage(Lang.translateDirect("schedule.remove_with_empty_hand"), true);
				return ActionResult.SUCCESS;
			}

			if (schedule.entries.isEmpty()) {
				AllSoundEvents.DENY.playOnServer(pPlayer.getWorld(), pPlayer.getBlockPos(), 1, 1);
				pPlayer.sendMessage(Lang.translateDirect("schedule.no_stops"), true);
				return ActionResult.SUCCESS;
			}

			train.runtime.setSchedule(schedule, false);
			AllAdvancements.CONDUCTOR.awardTo(pPlayer);
			AllSoundEvents.CONFIRM.playOnServer(pPlayer.getWorld(), pPlayer.getBlockPos(), 1, 1);
			pPlayer.sendMessage(Lang.translateDirect("schedule.applied_to_train")
				.formatted(Formatting.GREEN), true);
			pStack.decrement(1);
			pPlayer.setStackInHand(pUsedHand, pStack.isEmpty() ? ItemStack.EMPTY : pStack);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendTooltip(ItemStack stack, World worldIn, List<Text> tooltip, TooltipContext flagIn) {
		Schedule schedule = getSchedule(stack);
		if (schedule == null || schedule.entries.isEmpty())
			return;

		MutableText caret = Components.literal("> ").formatted(Formatting.GRAY);
		MutableText arrow = Components.literal("-> ").formatted(Formatting.GRAY);

		List<ScheduleEntry> entries = schedule.entries;
		for (int i = 0; i < entries.size(); i++) {
			boolean current = i == schedule.savedProgress && schedule.entries.size() > 1;
			ScheduleEntry entry = entries.get(i);
			if (!(entry.instruction instanceof DestinationInstruction destination))
				continue;
			Formatting format = current ? Formatting.YELLOW : Formatting.GOLD;
			MutableText prefix = current ? arrow : caret;
			tooltip.add(prefix.copy()
				.append(Components.literal(destination.getFilter()).formatted(format)));
		}
	}

	public static Schedule getSchedule(ItemStack pStack) {
		if (!pStack.hasNbt())
			return null;
		if (!pStack.getNbt()
			.contains("Schedule"))
			return null;
		return Schedule.fromTag(pStack.getSubNbt("Schedule"));
	}

	@Override
	public ScreenHandler createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		ItemStack heldItem = player.getMainHandStack();
		return new ScheduleMenu(AllMenuTypes.SCHEDULE.get(), id, inv, heldItem);
	}

	@Override
	public Text getDisplayName() {
		return getName();
	}

}
