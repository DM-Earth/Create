package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.AllPackets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EjectorItem extends BlockItem {

	public EjectorItem(Block p_i48527_1_, Settings p_i48527_2_) {
		super(p_i48527_1_, p_i48527_2_);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		PlayerEntity player = ctx.getPlayer();
		if (player != null && player.isSneaking())
			return ActionResult.SUCCESS;
		return super.useOnBlock(ctx);
	}

	@Override
	protected BlockState getPlacementState(ItemPlacementContext p_195945_1_) {
		BlockState stateForPlacement = super.getPlacementState(p_195945_1_);
		return stateForPlacement;
	}

	@Override
	protected boolean postPlacement(BlockPos pos, World world, PlayerEntity player, ItemStack p_195943_4_,
		BlockState p_195943_5_) {
		if (!world.isClient && player instanceof ServerPlayerEntity sp)
			AllPackets.getChannel()
				.sendToClient(new EjectorPlacementPacket.ClientBoundRequest(pos), sp);
		return super.postPlacement(pos, world, player, p_195943_4_, p_195943_5_);
	}

	@Override
	public boolean canMine(BlockState state, World world, BlockPos pos,
		PlayerEntity p_195938_4_) {
		return !p_195938_4_.isSneaking();
	}

}
