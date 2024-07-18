package com.simibubi.create.content.kinetics.simpleRelays.encased;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class EncasedShaftBlock extends AbstractEncasedShaftBlock
	implements IBE<KineticBlockEntity>, ISpecialBlockItemRequirement, BlockPickInteractionAware, EncasedBlock {

	private final Supplier<Block> casing;

	public EncasedShaftBlock(Settings properties, Supplier<Block> casing) {
		super(properties);
		this.casing = casing;
	}

	@Override
	public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
		if (context.getWorld().isClient)
			return ActionResult.SUCCESS;
		context.getWorld()
			.syncWorldEvent(2001, context.getBlockPos(), Block.getRawIdFromState(state));
		KineticBlockEntity.switchToBlockState(context.getWorld(), context.getBlockPos(),
			AllBlocks.SHAFT.getDefaultState()
				.with(AXIS, state.get(AXIS)));
		return ActionResult.SUCCESS;
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockView view, BlockPos pos, @Nullable PlayerEntity player, @Nullable HitResult target) {
		if (target instanceof BlockHitResult)
			return ((BlockHitResult) target).getSide()
				.getAxis() == getRotationAxis(state) ? AllBlocks.SHAFT.asStack() : getCasing().asItem().getDefaultStack();
		return ItemStack.EMPTY;
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		return ItemRequirement.of(AllBlocks.SHAFT.getDefaultState(), be);
	}

	@Override
	public Class<KineticBlockEntity> getBlockEntityClass() {
		return KineticBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ENCASED_SHAFT.get();
	}

	@Override
	public Block getCasing() {
		return casing.get();
	}

	@Override
	public void handleEncasing(BlockState state, World level, BlockPos pos, ItemStack heldItem, PlayerEntity player, Hand hand,
	    BlockHitResult ray) {
		KineticBlockEntity.switchToBlockState(level, pos, getDefaultState()
				.with(RotatedPillarKineticBlock.AXIS, state.get(RotatedPillarKineticBlock.AXIS)));
	}
}
