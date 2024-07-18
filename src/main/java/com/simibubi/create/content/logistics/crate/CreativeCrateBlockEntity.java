package com.simibubi.create.content.logistics.crate;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

import org.jetbrains.annotations.Nullable;

import com.jozufozu.flywheel.util.transform.TransformStack;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class CreativeCrateBlockEntity extends CrateBlockEntity implements SidedStorageBlockEntity {

	public CreativeCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inv = new BottomlessItemHandler(filtering::getFilter);
	}

	FilteringBehaviour filtering;
	private BottomlessItemHandler inv;

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(filtering = createFilter());
		filtering.setLabel(Lang.translateDirect("logistics.creative_crate.supply"));
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		return inv;
	}

	public FilteringBehaviour createFilter() {
		return new FilteringBehaviour(this, new ValueBoxTransform() {

			@Override
			public void rotate(BlockState state, MatrixStack ms) {
				TransformStack.cast(ms)
					.rotateX(90);
			}

			@Override
			public Vec3d getLocalOffset(BlockState state) {
				return new Vec3d(0.5, 13.5 / 16d, 0.5);
			}

			public float getScale() {
				return super.getScale();
			};

		});
	}

}
