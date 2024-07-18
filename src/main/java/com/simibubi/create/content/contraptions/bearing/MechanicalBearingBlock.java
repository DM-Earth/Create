package com.simibubi.create.content.contraptions.bearing;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MechanicalBearingBlock extends BearingBlock implements IBE<MechanicalBearingBlockEntity> {

	public MechanicalBearingBlock(Settings properties) {
		super(properties);
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (!player.canModifyBlocks())
			return ActionResult.FAIL;
		if (player.isSneaking())
			return ActionResult.FAIL;
		if (player.getStackInHand(handIn)
			.isEmpty()) {
			if (worldIn.isClient)
				return ActionResult.SUCCESS;
			withBlockEntityDo(worldIn, pos, be -> {
				if (be.running) {
					be.disassemble();
					return;
				}
				be.assembleNextTick = true;
			});
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	@Override
	public Class<MechanicalBearingBlockEntity> getBlockEntityClass() {
		return MechanicalBearingBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MechanicalBearingBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.MECHANICAL_BEARING.get();
	}

}