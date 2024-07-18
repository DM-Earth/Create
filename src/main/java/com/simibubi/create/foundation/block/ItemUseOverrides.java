package com.simibubi.create.foundation.block;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.simibubi.create.foundation.utility.VecHelper;

public class ItemUseOverrides {

	private static final Set<Identifier> OVERRIDES = new HashSet<>();

	public static void addBlock(Block block) {
		OVERRIDES.add(RegisteredObjects.getKeyOrThrow(block));
	}

	public static ActionResult onBlockActivated(PlayerEntity player, World world, Hand hand, BlockHitResult traceResult) {
		if (AllItems.WRENCH.isIn(player.getStackInHand(hand)))
			return ActionResult.PASS;

		if (player.isSpectator())
			return ActionResult.PASS;

		BlockPos pos = traceResult.getBlockPos();

		BlockState state = world
				.getBlockState(pos);
		Identifier id = RegisteredObjects.getKeyOrThrow(state.getBlock());

		if (!OVERRIDES.contains(id))
			return ActionResult.PASS;

		BlockHitResult blockTrace =
				new BlockHitResult(VecHelper.getCenterOf(pos), traceResult.getSide(), pos, true);
		ActionResult result = state.onUse(world, player, hand, blockTrace);

		if (!result.isAccepted())
			return ActionResult.PASS;

		return result;
	}

}
