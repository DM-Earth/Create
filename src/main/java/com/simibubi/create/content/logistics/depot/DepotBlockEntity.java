package com.simibubi.create.content.logistics.depot;

import java.util.List;

import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

public class DepotBlockEntity extends SmartBlockEntity implements SidedStorageBlockEntity {

	DepotBehaviour depotBehaviour;

	public DepotBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(depotBehaviour = new DepotBehaviour(this));
		depotBehaviour.addSubBehaviours(behaviours);
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction direction) {
		return depotBehaviour.itemHandler;
	}

	public ItemStack getHeldItem() {
		return depotBehaviour.getHeldItemStack();
	}
}
