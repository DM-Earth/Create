package com.simibubi.create.content.kinetics.mechanicalArm;

import com.simibubi.create.AllPackets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ArmItem extends BlockItem {

	public ArmItem(Block p_i48527_1_, Settings p_i48527_2_) {
		super(p_i48527_1_, p_i48527_2_);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		World world = ctx.getWorld();
		BlockPos pos = ctx.getBlockPos();
		if (ArmInteractionPoint.isInteractable(world, pos, world.getBlockState(pos)))
			return ActionResult.SUCCESS;
		return super.useOnBlock(ctx);
	}

	@Override
	protected boolean postPlacement(BlockPos pos, World world, PlayerEntity player, ItemStack p_195943_4_,
		BlockState p_195943_5_) {
		if (!world.isClient && player instanceof ServerPlayerEntity sp)
			AllPackets.getChannel()
				.sendToClient(new ArmPlacementPacket.ClientBoundRequest(pos), sp);
		return super.postPlacement(pos, world, player, p_195943_4_, p_195943_5_);
	}

	@Override
	public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity p_195938_4_) {
		return !ArmInteractionPoint.isInteractable(world, pos, state);
	}

}
