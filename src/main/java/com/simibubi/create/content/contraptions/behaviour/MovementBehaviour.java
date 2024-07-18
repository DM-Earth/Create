package com.simibubi.create.content.contraptions.behaviour;

import javax.annotation.Nullable;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.content.contraptions.render.ActorInstance;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface MovementBehaviour {

	default boolean isActive(MovementContext context) {
		return !context.disabled;
	}

	default void tick(MovementContext context) {}

	default void startMoving(MovementContext context) {}

	default void visitNewPosition(MovementContext context, BlockPos pos) {}

	default Vec3d getActiveAreaOffset(MovementContext context) {
		return Vec3d.ZERO;
	}

	@Nullable
	default ItemStack canBeDisabledVia(MovementContext context) {
		Block block = context.state.getBlock();
		if (block == null)
			return null;
		return new ItemStack(block);
	}

	default void onDisabledByControls(MovementContext context) {
		cancelStall(context);
	}

	default boolean mustTickWhileDisabled() {
		return false;
	}

	default void dropItem(MovementContext context, ItemStack stack) {
		ItemStack remainder;
		if (AllConfigs.server().kinetics.moveItemsToStorage.get()) {
			try (Transaction t = TransferUtil.getTransaction()) {
				long inserted = context.contraption.getSharedInventory().insert(ItemVariant.of(stack), stack.getCount(), t);
				remainder = stack.copy();
				remainder.decrement((int) inserted);
				t.commit();
			}
		}
		else
			remainder = stack;
		if (remainder.isEmpty())
			return;

		// Actors might void items if their positions is undefined
		Vec3d vec = context.position;
		if (vec == null)
			return;
		
		ItemEntity itemEntity = new ItemEntity(context.world, vec.x, vec.y, vec.z, remainder);
		itemEntity.setVelocity(context.motion.add(0, 0.5f, 0)
			.multiply(context.world.random.nextFloat() * .3f));
		context.world.spawnEntity(itemEntity);
	}

	default void onSpeedChanged(MovementContext context, Vec3d oldMotion, Vec3d motion) {}

	default void stopMoving(MovementContext context) {}

	default void cancelStall(MovementContext context) {
		context.stall = false;
	}

	default void writeExtraData(MovementContext context) {}

	default boolean renderAsNormalBlockEntity() {
		return false;
	}

	default boolean hasSpecialInstancedRendering() {
		return false;
	}

	@Environment(EnvType.CLIENT)
	default void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffer) {}

	@Environment(EnvType.CLIENT)
	@Nullable
	default ActorInstance createInstance(MaterialManager materialManager, VirtualRenderWorld simulationWorld,
		MovementContext context) {
		return null;
	}
}
