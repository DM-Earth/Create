package com.simibubi.create.content.decoration.encasing;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Implement this interface to indicate that this block is encasable.
 */
public interface EncasableBlock {
	/**
	 * This method should be called in the {@link Block#onUse(BlockState, World, BlockPos, PlayerEntity, Hand, BlockHitResult)} method.
	 */
	default ActionResult tryEncase(BlockState state, World level, BlockPos pos, ItemStack heldItem, PlayerEntity player, Hand hand,
		BlockHitResult ray) {
		List<Block> encasedVariants = EncasingRegistry.getVariants(state.getBlock());
		for (Block block : encasedVariants) {
			if (block instanceof EncasedBlock encased) {
				if (encased.getCasing().asItem() != heldItem.getItem())
					continue;

				if (level.isClient)
					return ActionResult.SUCCESS;

				encased.handleEncasing(state, level, pos, heldItem, player, hand, ray);
				return ActionResult.SUCCESS;
			}
		}
		return ActionResult.PASS;
	}
}
