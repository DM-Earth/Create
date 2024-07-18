package com.simibubi.create.content.kinetics.gearbox;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

public class GearboxBlock extends RotatedPillarKineticBlock implements IBE<GearboxBlockEntity>, BlockPickInteractionAware {

	public GearboxBlock(Settings properties) {
		super(properties);
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
		if (state.get(AXIS).isVertical())
			return super.getDroppedStacks(state, builder);
		return Arrays.asList(new ItemStack(AllItems.VERTICAL_GEARBOX.get()));
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockView view, BlockPos pos, @Nullable PlayerEntity player, @Nullable HitResult result) {
		if (state.get(AXIS).isVertical())
			return super.getPickStack(view, pos, state);
		return new ItemStack(AllItems.VERTICAL_GEARBOX.get());
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return getDefaultState().with(AXIS, Axis.Y);
	}

	// IRotate:

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() != state.get(AXIS);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(AXIS);
	}

	@Override
	public Class<GearboxBlockEntity> getBlockEntityClass() {
		return GearboxBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends GearboxBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.GEARBOX.get();
	}
}
