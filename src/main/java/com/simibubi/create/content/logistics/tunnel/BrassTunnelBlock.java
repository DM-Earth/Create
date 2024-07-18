package com.simibubi.create.content.logistics.tunnel;

import java.util.List;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;

public class BrassTunnelBlock extends BeltTunnelBlock {

	public BrassTunnelBlock(AbstractBlock.Settings properties) {
		super(properties);
	}

	@Override
	public ActionResult onUse(BlockState p_225533_1_, World world, BlockPos pos, PlayerEntity player,
		Hand p_225533_5_, BlockHitResult p_225533_6_) {
		return onBlockEntityUse(world, pos, be -> {
			if (!(be instanceof BrassTunnelBlockEntity))
				return ActionResult.PASS;
			BrassTunnelBlockEntity bte = (BrassTunnelBlockEntity) be;
			List<ItemStack> stacksOfGroup = bte.grabAllStacksOfGroup(world.isClient);
			if (stacksOfGroup.isEmpty())
				return ActionResult.PASS;
			if (world.isClient)
				return ActionResult.SUCCESS;
			for (ItemStack itemStack : stacksOfGroup) 
				player.getInventory().offerOrDrop(itemStack.copy());
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .2f,
				1f + world.random.nextFloat());
			return ActionResult.SUCCESS;
		});
	}

	@Override
	public BlockEntityType<? extends BeltTunnelBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.BRASS_TUNNEL.get();
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction facing, BlockState facingState, WorldAccess worldIn,
		BlockPos currentPos, BlockPos facingPos) {
		return super.getStateForNeighborUpdate(state, facing, facingState, worldIn, currentPos, facingPos);
	}

	@Override
	public void onStateReplaced(BlockState state, World level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}

}
