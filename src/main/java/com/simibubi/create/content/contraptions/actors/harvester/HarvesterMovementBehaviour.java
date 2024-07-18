package com.simibubi.create.content.contraptions.actors.harvester;

import javax.annotation.Nullable;
import net.minecraft.block.AbstractPlantPartBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import io.github.fabricators_of_create.porting_lib.common.util.IPlantable;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorInstance;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class HarvesterMovementBehaviour implements MovementBehaviour {

	@Override
	public boolean isActive(MovementContext context) {
		return MovementBehaviour.super.isActive(context)
			&& !VecHelper.isVecPointingTowards(context.relativeMotion, context.state.get(HarvesterBlock.FACING)
				.getOpposite());
	}

	@Override
	public boolean hasSpecialInstancedRendering() {
		return true;
	}

	@Nullable
	@Override
	public ActorInstance createInstance(MaterialManager materialManager, VirtualRenderWorld simulationWorld,
		MovementContext context) {
		return new HarvesterActorInstance(materialManager, simulationWorld, context);
	}

	@Override
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffers) {
        if (!ContraptionRenderDispatcher.canInstance())
			HarvesterRenderer.renderInContraption(context, renderWorld, matrices, buffers);
	}

	@Override
	public Vec3d getActiveAreaOffset(MovementContext context) {
		return Vec3d.of(context.state.get(HarvesterBlock.FACING)
			.getVector())
			.multiply(.45);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		World world = context.world;
		BlockState stateVisited = world.getBlockState(pos);
		boolean notCropButCuttable = false;

		if (world.isClient)
			return;

		if (!isValidCrop(world, pos, stateVisited)) {
			if (isValidOther(world, pos, stateVisited))
				notCropButCuttable = true;
			else
				return;
		}

		ItemStack item = ItemStack.EMPTY;
		float effectChance = 1;

		if (stateVisited.isIn(BlockTags.LEAVES)) {
			item = new ItemStack(Items.SHEARS);
			effectChance = .45f;
		}

		MutableBoolean seedSubtracted = new MutableBoolean(notCropButCuttable);
		BlockState state = stateVisited;
		BlockHelper.destroyBlockAs(world, pos, null, item, effectChance, stack -> {
			if (AllConfigs.server().kinetics.harvesterReplants.get() && !seedSubtracted.getValue()
				&& ItemHelper.sameItem(stack, new ItemStack(state.getBlock()))) {
				stack.decrement(1);
				seedSubtracted.setTrue();
			}
			if (!stack.isEmpty()) // fabric: guard shrinking above
				dropItem(context, stack);
		});

		BlockState cutCrop = cutCrop(world, pos, stateVisited);
		world.setBlockState(pos, cutCrop.canPlaceAt(world, pos) ? cutCrop : Blocks.AIR.getDefaultState());
	}

	public boolean isValidCrop(World world, BlockPos pos, BlockState state) {
		boolean harvestPartial = AllConfigs.server().kinetics.harvestPartiallyGrown.get();
		boolean replant = AllConfigs.server().kinetics.harvesterReplants.get();

		if (state.getBlock() instanceof CropBlock) {
			CropBlock crop = (CropBlock) state.getBlock();
			if (harvestPartial)
				return state != crop.withAge(0) || !replant;
			return crop.isMature(state);
		}

		if (state.getCollisionShape(world, pos)
			.isEmpty() || state.getBlock() instanceof CocoaBlock) {
			for (Property<?> property : state.getProperties()) {
				if (!(property instanceof IntProperty))
					continue;
				IntProperty ageProperty = (IntProperty) property;
				if (!property.getName()
					.equals(Properties.AGE_1.getName()))
					continue;
				int age = state.get(ageProperty)
					.intValue();
				if (state.getBlock() instanceof SweetBerryBushBlock && age <= 1 && replant)
					continue;
				if (age == 0 && replant || !harvestPartial && (ageProperty.getValues()
					.size() - 1 != age))
					continue;
				return true;
			}
		}

		return false;
	}

	public boolean isValidOther(World world, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof CropBlock)
			return false;
		if (state.getBlock() instanceof SugarCaneBlock)
			return true;
		if (state.isIn(BlockTags.LEAVES))
			return true;
		if (state.getBlock() instanceof CocoaBlock)
			return state.get(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;

		if (state.getCollisionShape(world, pos)
			.isEmpty()) {
			if (state.getBlock() instanceof AbstractPlantPartBlock)
				return true;

			for (Property<?> property : state.getProperties()) {
				if (!(property instanceof IntProperty))
					continue;
				if (!property.getName()
					.equals(Properties.AGE_1.getName()))
					continue;
				return false;
			}

			if (state.getBlock() instanceof IPlantable)
				return true;
		}

		return false;
	}

	private BlockState cutCrop(World world, BlockPos pos, BlockState state) {
		if (!AllConfigs.server().kinetics.harvesterReplants.get()) {
			if (state.getFluidState()
				.isEmpty())
				return Blocks.AIR.getDefaultState();
			return state.getFluidState()
				.getBlockState();
		}

		Block block = state.getBlock();
		if (block instanceof CropBlock) {
			CropBlock crop = (CropBlock) block;
			return crop.withAge(0);
		}
		if (block == Blocks.SWEET_BERRY_BUSH) {
			return state.with(Properties.AGE_3, Integer.valueOf(1));
		}
		if (block == Blocks.SUGAR_CANE || block instanceof AbstractPlantPartBlock) {
			if (state.getFluidState()
				.isEmpty())
				return Blocks.AIR.getDefaultState();
			return state.getFluidState()
				.getBlockState();
		}
		if (state.getCollisionShape(world, pos)
			.isEmpty() || block instanceof CocoaBlock) {
			for (Property<?> property : state.getProperties()) {
				if (!(property instanceof IntProperty))
					continue;
				if (!property.getName()
					.equals(Properties.AGE_1.getName()))
					continue;
				return state.with((IntProperty) property, Integer.valueOf(0));
			}
		}

		if (state.getFluidState()
			.isEmpty())
			return Blocks.AIR.getDefaultState();
		return state.getFluidState()
			.getBlockState();
	}

}
