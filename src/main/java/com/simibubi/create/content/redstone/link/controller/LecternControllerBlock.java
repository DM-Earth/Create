package com.simibubi.create.content.redstone.link.controller;

import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class LecternControllerBlock extends LecternBlock
	implements IBE<LecternControllerBlockEntity>, ISpecialBlockItemRequirement, BlockPickInteractionAware {

	public LecternControllerBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(HAS_BOOK, true));
	}

	@Override
	public Class<LecternControllerBlockEntity> getBlockEntityClass() {
		return LecternControllerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends LecternControllerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.LECTERN_CONTROLLER.get();
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos p_153573_, BlockState p_153574_) {
		return IBE.super.createBlockEntity(p_153573_, p_153574_);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult hit) {
		if (!player.isSneaking() && LecternControllerBlockEntity.playerInRange(player, world, pos)) {
			if (!world.isClient)
				withBlockEntityDo(world, pos, be -> be.tryStartUsing(player));
			return ActionResult.SUCCESS;
		}

		if (player.isSneaking()) {
			if (!world.isClient)
				replaceWithLectern(state, world, pos);
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.isOf(newState.getBlock())) {
			if (!world.isClient)
				withBlockEntityDo(world, pos, be -> be.dropController(state));

			super.onStateReplaced(state, world, pos, newState, isMoving);
		}
	}

	@Override
	public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
		return 15;
	}

	public void replaceLectern(BlockState lecternState, World world, BlockPos pos, ItemStack controller) {
		world.setBlockState(pos, getDefaultState().with(FACING, lecternState.get(FACING))
			.with(POWERED, lecternState.get(POWERED)));
		withBlockEntityDo(world, pos, be -> be.setController(controller));
	}

	public void replaceWithLectern(BlockState state, World world, BlockPos pos) {
		AllSoundEvents.CONTROLLER_TAKE.playOnServer(world, pos);
		world.setBlockState(pos, Blocks.LECTERN.getDefaultState()
			.with(FACING, state.get(FACING))
			.with(POWERED, state.get(POWERED)));
	}


	@Override
	public ItemStack getPickedStack(BlockState state, BlockView view, BlockPos pos, @Nullable PlayerEntity player, @Nullable HitResult result) {
		return Blocks.LECTERN.getPickStack(view, pos, state);
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		ArrayList<ItemStack> requiredItems = new ArrayList<>();
		requiredItems.add(new ItemStack(Blocks.LECTERN));
		requiredItems.add(new ItemStack(AllItems.LINKED_CONTROLLER.get()));
		return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, requiredItems);
	}
}
