package com.simibubi.create.content.kinetics.simpleRelays;

import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.equipment.wrench.IWrenchableWithBracket;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

public abstract class AbstractSimpleShaftBlock extends AbstractShaftBlock implements IWrenchableWithBracket {

	public AbstractSimpleShaftBlock(Settings properties) {
		super(properties);
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		return IWrenchableWithBracket.super.onWrenched(state, context);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state != newState && !isMoving)
			removeBracket(world, pos, true).ifPresent(stack -> Block.dropStack(world, pos, stack));
		super.onStateReplaced(state, world, pos, newState, isMoving);
	}

	@Override
	public Optional<ItemStack> removeBracket(BlockView world, BlockPos pos, boolean inOnReplacedContext) {
		BracketedBlockEntityBehaviour behaviour = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);
		if (behaviour == null)
			return Optional.empty();
		BlockState bracket = behaviour.removeBracket(inOnReplacedContext);
		if (bracket == null)
			return Optional.empty();
		return Optional.of(new ItemStack(bracket.getBlock()));
	}

	@Override
	public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.BRACKETED_KINETIC.get();
	}

}
