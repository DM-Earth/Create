package com.simibubi.create.content.equipment.wrench;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags.AllItemTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public class WrenchEventHandler {
	public static ActionResult useOwnWrenchLogicForCreateBlocks(PlayerEntity player, World world, Hand hand, BlockHitResult hitVec) {
		ItemStack itemStack = player.getStackInHand(hand);

		// fabric: note - mayBuild handles spectator check
		if (!player.canModifyBlocks())
			return ActionResult.PASS;
		if (itemStack.isEmpty())
			return ActionResult.PASS;
		if (AllItems.WRENCH.isIn(itemStack))
			return ActionResult.PASS;
		if (!AllItemTags.WRENCH.matches(itemStack.getItem()))
			return ActionResult.PASS;

		BlockState state = world
			.getBlockState(hitVec.getBlockPos());
		Block block = state.getBlock();

		if (!(block instanceof IWrenchable))
			return ActionResult.PASS;

		ItemUsageContext context = new ItemUsageContext(player, hand, hitVec);
		IWrenchable actor = (IWrenchable) block;

		return player.isSneaking() ? actor.onSneakWrenched(state, context) : actor.onWrenched(state, context);
	}

}
