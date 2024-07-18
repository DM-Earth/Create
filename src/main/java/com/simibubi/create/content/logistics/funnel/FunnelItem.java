package com.simibubi.create.content.logistics.funnel;

import io.github.fabricators_of_create.porting_lib.item.BlockUseBypassingItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.apache.logging.log4j.core.Filter.Result;

public class FunnelItem extends BlockItem implements BlockUseBypassingItem {

	public FunnelItem(Block p_i48527_1_, Settings p_i48527_2_) {
		super(p_i48527_1_, p_i48527_2_);
	}

	// fabric: handled by BlockUseBypassingItem
//	public static void funnelItemAlwaysPlacesWhenUsed(PlayerInteractEvent.RightClickBlock event) {
//		if (event.getItemStack()
//				.getItem() instanceof FunnelItem)
//			event.setUseBlock(Result.DENY);
//	}

	@Override
	protected BlockState getPlacementState(ItemPlacementContext ctx) {
		World world = ctx.getWorld();
		BlockPos pos = ctx.getBlockPos();
		BlockState state = super.getPlacementState(ctx);
		if (state == null)
			return state;
		if (!(state.getBlock() instanceof FunnelBlock))
			return state;
		if (state.get(FunnelBlock.FACING)
			.getAxis()
			.isVertical())
			return state;

		Direction direction = state.get(FunnelBlock.FACING);
		FunnelBlock block = (FunnelBlock) getBlock();
		Block beltFunnelBlock = block.getEquivalentBeltFunnel(world, pos, state)
			.getBlock();
		BlockState equivalentBeltFunnel = beltFunnelBlock.getPlacementState(ctx)
			.with(BeltFunnelBlock.HORIZONTAL_FACING, direction);
		if (BeltFunnelBlock.isOnValidBelt(equivalentBeltFunnel, world, pos))
			return equivalentBeltFunnel;

		return state;
	}

	@Override
	public boolean shouldBypass(BlockState state, BlockPos pos, World level, PlayerEntity player, Hand hand) {
		return true;
	}
}
