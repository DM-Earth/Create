package com.simibubi.create.content.contraptions.actors.contraptionControls;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class ContraptionControlsBlock extends ControlsBlock implements IBE<ContraptionControlsBlockEntity> {

	public ContraptionControlsBlock(Settings pProperties) {
		super(pProperties);
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {
		return onBlockEntityUse(pLevel, pPos, cte -> {
			cte.pressButton();
			if (!pLevel.isClient()) {
				cte.disabled = !cte.disabled;
				cte.notifyUpdate();
				ContraptionControlsBlockEntity.sendStatus(pPlayer, cte.filtering.getFilter(), !cte.disabled);
				AllSoundEvents.CONTROLLER_CLICK.play(cte.getWorld(), null, cte.getPos(), 1,
					cte.disabled ? 0.8f : 1.5f);
			}
			return ActionResult.SUCCESS;
		});
	}

	@Override
	public void neighborUpdate(BlockState pState, World pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos,
		boolean pIsMoving) {
		withBlockEntityDo(pLevel, pPos, ContraptionControlsBlockEntity::updatePoweredState);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		return AllShapes.CONTRAPTION_CONTROLS.get(pState.get(FACING));
	}

	@Override
	public Class<ContraptionControlsBlockEntity> getBlockEntityClass() {
		return ContraptionControlsBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ContraptionControlsBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.CONTRAPTION_CONTROLS.get();
	}

}
