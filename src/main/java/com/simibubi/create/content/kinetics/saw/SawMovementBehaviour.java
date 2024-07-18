package com.simibubi.create.content.kinetics.saw;

import java.util.Optional;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.utility.AbstractBlockBreakQueue;
import com.simibubi.create.foundation.utility.TreeCutter;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SawMovementBehaviour extends BlockBreakingMovementBehaviour {

	@Override
	public boolean isActive(MovementContext context) {
		return super.isActive(context)
			&& !VecHelper.isVecPointingTowards(context.relativeMotion, context.state.get(SawBlock.FACING)
				.getOpposite());
	}

	@Override
	public Vec3d getActiveAreaOffset(MovementContext context) {
		return Vec3d.of(context.state.get(SawBlock.FACING)
			.getVector())
			.multiply(.65f);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		super.visitNewPosition(context, pos);
		Vec3d facingVec = Vec3d.of(context.state.get(SawBlock.FACING)
			.getVector());
		facingVec = context.rotation.apply(facingVec);

		Direction closestToFacing = Direction.getFacing(facingVec.x, facingVec.y, facingVec.z);
		if (closestToFacing.getAxis()
			.isVertical() && context.data.contains("BreakingPos")) {
			context.data.remove("BreakingPos");
			context.stall = false;
		}
	}

	@Override
	public boolean canBreak(World world, BlockPos breakingPos, BlockState state) {
		return super.canBreak(world, breakingPos, state) && SawBlockEntity.isSawable(state);
	}

	@Override
	protected void onBlockBroken(MovementContext context, BlockPos pos, BlockState brokenState) {
		if (brokenState.isIn(BlockTags.LEAVES))
			return;

		Optional<AbstractBlockBreakQueue> dynamicTree = TreeCutter.findDynamicTree(brokenState.getBlock(), pos);
		if (dynamicTree.isPresent()) {
			dynamicTree.get()
				.destroyBlocks(context.world, null, (stack, dropPos) -> dropItemFromCutTree(context, stack, dropPos));
			return;
		}

		TreeCutter.findTree(context.world, pos)
			.destroyBlocks(context.world, null, (stack, dropPos) -> dropItemFromCutTree(context, stack, dropPos));
	}

	public void dropItemFromCutTree(MovementContext context, BlockPos pos, ItemStack stack) {
		long inserted = TransferUtil.insertItem(context.contraption.getSharedInventory(), stack);
		if (inserted == stack.getCount())
			return;
		long remaining = stack.getCount() - inserted;
		ItemStack remainder = stack.copy();
		remainder.setCount((int) remaining);

		World world = context.world;
		Vec3d dropPos = VecHelper.getCenterOf(pos);
		float distance = context.position == null ? 1 : (float) dropPos.distanceTo(context.position);
		ItemEntity entity = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, remainder);
		entity.setVelocity(context.relativeMotion.multiply(distance / 20f));
		world.spawnEntity(entity);
	}

	@Override
	@Environment(value = EnvType.CLIENT)
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffer) {
		SawRenderer.renderInContraption(context, renderWorld, matrices, buffer);
	}

	@Override
	protected boolean shouldDestroyStartBlock(BlockState stateToBreak) {
		return !TreeCutter.canDynamicTreeCutFrom(stateToBreak.getBlock());
	}

	@Override
	protected DamageSource getDamageSource(World level) {
		return CreateDamageSources.saw(level);
	}
}
