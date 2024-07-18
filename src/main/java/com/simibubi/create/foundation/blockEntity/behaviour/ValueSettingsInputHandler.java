package com.simibubi.create.foundation.blockEntity.behaviour;

import com.simibubi.create.foundation.utility.AdventureUtil;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import com.simibubi.create.foundation.utility.RaycastHelper;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;

public class ValueSettingsInputHandler {

	public static ActionResult onBlockActivated(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();

		if (!canInteract(player))
			return ActionResult.PASS;
		if (AllBlocks.CLIPBOARD.isIn(player.getMainHandStack()))
			return ActionResult.PASS;
		if (!(world.getBlockEntity(pos)instanceof SmartBlockEntity sbe))
			return ActionResult.PASS;

		MutableBoolean cancelled = new MutableBoolean(false);
		if (world.isClient)
			EnvExecutor.runWhenOn(EnvType.CLIENT,
				() -> () -> CreateClient.VALUE_SETTINGS_HANDLER.cancelIfWarmupAlreadyStarted(pos, cancelled));

		if (cancelled.booleanValue())
			return ActionResult.FAIL;

		for (BlockEntityBehaviour behaviour : sbe.getAllBehaviours()) {
			if (!(behaviour instanceof ValueSettingsBehaviour valueSettingsBehaviour))
				continue;

			BlockHitResult ray = RaycastHelper.rayTraceRange(world, player, 10);
			if (ray == null)
				return ActionResult.PASS;
			if (behaviour instanceof SidedFilteringBehaviour) {
				behaviour = ((SidedFilteringBehaviour) behaviour).get(ray.getSide());
				if (behaviour == null)
					continue;
			}

			if (!valueSettingsBehaviour.isActive())
				continue;
			if (valueSettingsBehaviour.onlyVisibleWithWrench()
				&& !AllItemTags.WRENCH.matches(player.getStackInHand(hand)))
				continue;
			if (valueSettingsBehaviour.getSlotPositioning()instanceof ValueBoxTransform.Sided sidedSlot) {
				if (!sidedSlot.isSideActive(sbe.getCachedState(), ray.getSide()))
					continue;
				sidedSlot.fromSide(ray.getSide());
			}

			boolean fakePlayer = player instanceof FakePlayer;
			if (!valueSettingsBehaviour.testHit(ray.getPos()) && !fakePlayer)
				continue;

			if (!valueSettingsBehaviour.acceptsValueSettings() || fakePlayer) {
				valueSettingsBehaviour.onShortInteract(player, hand, ray.getSide());
				return ActionResult.SUCCESS;
			}

			if (world.isClient) {
				BehaviourType<?> type = behaviour.getType();
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> CreateClient.VALUE_SETTINGS_HANDLER
					.startInteractionWith(pos, type, hand, ray.getSide()));
			}

			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	public static boolean canInteract(PlayerEntity player) {
		return player != null && !player.isSpectator() && !player.isSneaking() && !AdventureUtil.isAdventure(player);
	}

}
