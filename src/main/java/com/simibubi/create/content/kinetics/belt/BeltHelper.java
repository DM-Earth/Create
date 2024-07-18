package com.simibubi.create.content.kinetics.belt;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;

public class BeltHelper {

	public static Map<Item, Boolean> uprightCache = new Object2BooleanOpenHashMap<>();
	public static final IdentifiableResourceReloadListener LISTENER = new SimpleSynchronousResourceReloadListener() {
		@Override
		public Identifier getFabricId() {
			return Create.asResource("belt_helper");
		}

		@Override
		public void reload(ResourceManager resourceManager) {
			uprightCache.clear();
		}
	};

	public static boolean isItemUpright(ItemStack stack) {
		return uprightCache.computeIfAbsent(stack.getItem(),
			item -> ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM) != null
				|| AllItemTags.UPRIGHT_ON_BELT.matches(stack));
	}

	public static BeltBlockEntity getSegmentBE(WorldAccess world, BlockPos pos) {
		if (world instanceof World l && !l.canSetBlock(pos))
			return null;
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (!(blockEntity instanceof BeltBlockEntity))
			return null;
		return (BeltBlockEntity) blockEntity;
	}

	public static BeltBlockEntity getControllerBE(WorldAccess world, BlockPos pos) {
		BeltBlockEntity segment = getSegmentBE(world, pos);
		if (segment == null)
			return null;
		BlockPos controllerPos = segment.controller;
		if (controllerPos == null)
			return null;
		return getSegmentBE(world, controllerPos);
	}

	public static BeltBlockEntity getBeltForOffset(BeltBlockEntity controller, float offset) {
		return getBeltAtSegment(controller, (int) Math.floor(offset));
	}

	public static BeltBlockEntity getBeltAtSegment(BeltBlockEntity controller, int segment) {
		BlockPos pos = getPositionForOffset(controller, segment);
		BlockEntity be = controller.getWorld()
				.getBlockEntity(pos);
		if (be == null || !(be instanceof BeltBlockEntity))
			return null;
		return (BeltBlockEntity) be;
	}

	public static BlockPos getPositionForOffset(BeltBlockEntity controller, int offset) {
		BlockPos pos = controller.getPos();
		Vec3i vec = controller.getBeltFacing()
				.getVector();
		BeltSlope slope = controller.getCachedState()
				.get(BeltBlock.SLOPE);
		int verticality = slope == BeltSlope.DOWNWARD ? -1 : slope == BeltSlope.UPWARD ? 1 : 0;

		return pos.add(offset * vec.getX(), MathHelper.clamp(offset, 0, controller.beltLength - 1) * verticality,
				offset * vec.getZ());
	}

	public static Vec3d getVectorForOffset(BeltBlockEntity controller, float offset) {
		BeltSlope slope = controller.getCachedState()
				.get(BeltBlock.SLOPE);
		int verticality = slope == BeltSlope.DOWNWARD ? -1 : slope == BeltSlope.UPWARD ? 1 : 0;
		float verticalMovement = verticality;
		if (offset < .5)
			verticalMovement = 0;
		verticalMovement = verticalMovement * (Math.min(offset, controller.beltLength - .5f) - .5f);
		Vec3d vec = VecHelper.getCenterOf(controller.getPos());
		Vec3d horizontalMovement = Vec3d.of(controller.getBeltFacing()
						.getVector())
				.multiply(offset - .5f);

		if (slope == BeltSlope.VERTICAL)
			horizontalMovement = Vec3d.ZERO;

		vec = vec.add(horizontalMovement)
				.add(0, verticalMovement, 0);
		return vec;
	}

	public static Vec3d getBeltVector(BlockState state) {
		BeltSlope slope = state.get(BeltBlock.SLOPE);
		int verticality = slope == BeltSlope.DOWNWARD ? -1 : slope == BeltSlope.UPWARD ? 1 : 0;
		Vec3d horizontalMovement = Vec3d.of(state.get(BeltBlock.HORIZONTAL_FACING)
				.getVector());
		if (slope == BeltSlope.VERTICAL)
			return new Vec3d(0, state.get(BeltBlock.HORIZONTAL_FACING)
					.getDirection()
					.offset(), 0);
		return new Vec3d(0, verticality, 0).add(horizontalMovement);
	}

}