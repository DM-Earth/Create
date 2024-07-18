package com.simibubi.create.content.decoration.copycat;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.ITransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.redstone.RoseQuartzLampBlock;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockEntityItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.IPartialSafeNBT;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class CopycatBlockEntity extends SmartBlockEntity
	implements ISpecialBlockEntityItemRequirement, ITransformableBlockEntity, IPartialSafeNBT, RenderAttachmentBlockEntity {

	private BlockState material;
	private ItemStack consumedItem;

	public CopycatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		material = AllBlocks.COPYCAT_BASE.getDefaultState();
		consumedItem = ItemStack.EMPTY;
	}

	public BlockState getMaterial() {
		return material;
	}

	public boolean hasCustomMaterial() {
		return !AllBlocks.COPYCAT_BASE.has(getMaterial());
	}

	public void setMaterial(BlockState blockState) {
		BlockState wrapperState = getCachedState();

		if (!material.isOf(blockState.getBlock()))
			for (Direction side : Iterate.directions) {
				BlockPos neighbour = pos.offset(side);
				BlockState neighbourState = world.getBlockState(neighbour);
				if (neighbourState != wrapperState)
					continue;
				if (!(world.getBlockEntity(neighbour)instanceof CopycatBlockEntity cbe))
					continue;
				BlockState otherMaterial = cbe.getMaterial();
				if (!otherMaterial.isOf(blockState.getBlock()))
					continue;
				blockState = otherMaterial;
				break;
			}

		material = blockState;
		if (!world.isClient()) {
			notifyUpdate();
			return;
		}
		redraw();
	}

	public boolean cycleMaterial() {
		if (material.contains(TrapdoorBlock.HALF) && material.getOrEmpty(TrapdoorBlock.OPEN)
			.orElse(false))
			setMaterial(material.cycle(TrapdoorBlock.HALF));
		else if (material.contains(Properties.FACING))
			setMaterial(material.cycle(Properties.FACING));
		else if (material.contains(Properties.HORIZONTAL_FACING))
			setMaterial(material.with(Properties.HORIZONTAL_FACING,
				material.get(Properties.HORIZONTAL_FACING)
					.rotateYClockwise()));
		else if (material.contains(Properties.AXIS))
			setMaterial(material.cycle(Properties.AXIS));
		else if (material.contains(Properties.HORIZONTAL_AXIS))
			setMaterial(material.cycle(Properties.HORIZONTAL_AXIS));
		else if (material.contains(Properties.LIT))
			setMaterial(material.cycle(Properties.LIT));
		else if (material.contains(RoseQuartzLampBlock.POWERING))
			setMaterial(material.cycle(RoseQuartzLampBlock.POWERING));
		else
			return false;

		return true;
	}

	public ItemStack getConsumedItem() {
		return consumedItem;
	}

	public void setConsumedItem(ItemStack stack) {
		consumedItem = ItemHandlerHelper.copyStackWithSize(stack, 1);
		markDirty();
	}

	private void redraw() {
		// fabric: no need for requestModelDataUpdate
		if (hasWorld()) {
			world.updateListeners(getPos(), getCachedState(), getCachedState(), 16);
			world.getChunkManager()
				.getLightingProvider()
				.checkBlock(pos);
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public ItemRequirement getRequiredItems(BlockState state) {
		if (consumedItem.isEmpty())
			return ItemRequirement.NONE;
		return new ItemRequirement(ItemUseType.CONSUME, consumedItem);
	}

	@Override
	public void transform(StructureTransform transform) {
		material = transform.apply(material);
		notifyUpdate();
	}

	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		super.read(tag, clientPacket);

		consumedItem = ItemStack.fromNbt(tag.getCompound("Item"));

		BlockState prevMaterial = material;
		if (!tag.contains("Material")) {
			consumedItem = ItemStack.EMPTY;
			return;
		}

		material = NbtHelper.toBlockState(blockHolderGetter(), tag.getCompound("Material"));

		// Validate Material
		if (material != null && !clientPacket) {
			BlockState blockState = getCachedState();
			if (blockState == null)
				return;
			if (!(blockState.getBlock() instanceof CopycatBlock cb))
				return;
			BlockState acceptedBlockState = cb.getAcceptedBlockState(world, pos, consumedItem, null);
			if (acceptedBlockState != null && material.isOf(acceptedBlockState.getBlock()))
				return;
			consumedItem = ItemStack.EMPTY;
			material = AllBlocks.COPYCAT_BASE.getDefaultState();
		}

		if (clientPacket && prevMaterial != material)
			redraw();
	}

	@Override
	public void writeSafe(NbtCompound tag) {
		super.writeSafe(tag);

		ItemStack stackWithoutNBT = consumedItem.copy();
		stackWithoutNBT.setNbt(null);

		write(tag, stackWithoutNBT, material);
	}

	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		write(tag, consumedItem, material);
	}

	protected void write(NbtCompound tag, ItemStack stack, BlockState material) {
		tag.put("Item", NBTSerializer.serializeNBT(stack));
		tag.put("Material", NbtHelper.fromBlockState(material));
	}

	@Override
	public BlockState getRenderAttachmentData() {
		return material;
	}

}
