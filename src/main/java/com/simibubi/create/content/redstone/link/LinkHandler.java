package com.simibubi.create.content.redstone.link;

import java.util.Arrays;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.RaycastHelper;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LinkHandler {

	public static ActionResult onBlockActivated(PlayerEntity player, World world, Hand hand, BlockHitResult blockRayTraceResult) {
		BlockPos pos = blockRayTraceResult.getBlockPos();
		if (player.isSneaking() || player.isSpectator())
			return ActionResult.PASS;

		LinkBehaviour behaviour = BlockEntityBehaviour.get(world, pos, LinkBehaviour.TYPE);
		if (behaviour == null)
			return ActionResult.PASS;
		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;

		ItemStack heldItem = player.getStackInHand(hand);
		BlockHitResult ray = RaycastHelper.rayTraceRange(world, player, 10);
		if (ray == null)
			return ActionResult.PASS;
		if (AllItems.LINKED_CONTROLLER.isIn(heldItem))
			return ActionResult.PASS;
		if (AllItems.WRENCH.isIn(heldItem))
			return ActionResult.PASS;

		boolean fakePlayer = player instanceof FakePlayer;
		boolean fakePlayerChoice = false;

		if (fakePlayer) {
			BlockState blockState = world.getBlockState(pos);
			Vec3d localHit = ray.getPos()
				.subtract(Vec3d.of(pos))
				.add(Vec3d.of(ray.getSide()
					.getVector())
					.multiply(.25f));
			fakePlayerChoice = localHit.squaredDistanceTo(behaviour.firstSlot.getLocalOffset(blockState)) > localHit
				.squaredDistanceTo(behaviour.secondSlot.getLocalOffset(blockState));
		}

		for (boolean first : Arrays.asList(false, true)) {
			if (behaviour.testHit(first, ray.getPos()) || fakePlayer && fakePlayerChoice == first) {
				if (!world.isClient)
					behaviour.setFrequency(first, heldItem);
				world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, .25f, .1f);
				return ActionResult.SUCCESS;
			}
		}
		return ActionResult.PASS;
	}

}
