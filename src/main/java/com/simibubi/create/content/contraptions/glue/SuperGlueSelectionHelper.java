package com.simibubi.create.content.contraptions.glue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.simibubi.create.content.contraptions.BlockMovementChecks;
import com.simibubi.create.foundation.utility.Iterate;

public class SuperGlueSelectionHelper {

	public static Set<BlockPos> searchGlueGroup(World level, BlockPos startPos, BlockPos endPos, boolean includeOther) {
		if (endPos == null || startPos == null)
			return null;

		Box bb = SuperGlueEntity.span(startPos, endPos);

		List<BlockPos> frontier = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		Set<BlockPos> attached = new HashSet<>();
		Set<SuperGlueEntity> cachedOther = new HashSet<>();

		visited.add(startPos);
		frontier.add(startPos);

		while (!frontier.isEmpty()) {
			BlockPos currentPos = frontier.remove(0);
			attached.add(currentPos);

			for (Direction d : Iterate.directions) {
				BlockPos offset = currentPos.offset(d);
				boolean gluePresent = includeOther && SuperGlueEntity.isGlued(level, currentPos, d, cachedOther);
				boolean alreadySticky = includeOther && SuperGlueEntity.isSideSticky(level, currentPos, d)
					|| SuperGlueEntity.isSideSticky(level, offset, d.getOpposite());

				if (!alreadySticky && !gluePresent && !bb.contains(Vec3d.ofCenter(offset)))
					continue;
				if (!BlockMovementChecks.isMovementNecessary(level.getBlockState(offset), level, offset))
					continue;
				if (!SuperGlueEntity.isValidFace(level, currentPos, d)
					|| !SuperGlueEntity.isValidFace(level, offset, d.getOpposite()))
					continue;

				if (visited.add(offset))
					frontier.add(offset);
			}
		}

		if (attached.size() < 2 && attached.contains(endPos))
			return null;

		return attached;
	}

	public static boolean collectGlueFromInventory(PlayerEntity player, int requiredAmount, boolean simulate) {
		if (player.getAbilities().creativeMode)
			return true;
		if (requiredAmount == 0)
			return true;

		DefaultedList<ItemStack> items = player.getInventory().main;
		for (int i = -1; i < items.size(); i++) {
			int slot = i == -1 ? player.getInventory().selectedSlot : i;
			ItemStack stack = items.get(slot);
			if (stack.isEmpty())
				continue;
			if (!stack.isDamageable())
				continue;
			if (!(stack.getItem() instanceof SuperGlueItem))
				continue;

			int charges = Math.min(requiredAmount, stack.getMaxDamage() - stack.getDamage());

			if (!simulate)
				stack.damage(charges, player, i == -1 ? SuperGlueItem::onBroken : $ -> {
				});

			requiredAmount -= charges;
			if (requiredAmount <= 0)
				return true;
		}

		return false;
	}

}
