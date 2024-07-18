package com.simibubi.create.content.kinetics.waterwheel;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;

public class WaterWheelBlockEntity extends GeneratingKineticBlockEntity {

	public static final Map<Axis, Set<BlockPos>> SMALL_OFFSETS = new EnumMap<>(Axis.class);
	public static final Map<Axis, Set<BlockPos>> LARGE_OFFSETS = new EnumMap<>(Axis.class);

	static {
		for (Axis axis : Iterate.axes) {
			HashSet<BlockPos> offsets = new HashSet<>();
			for (Direction d : Iterate.directions)
				if (d.getAxis() != axis)
					offsets.add(BlockPos.ORIGIN.offset(d));
			SMALL_OFFSETS.put(axis, offsets);

			offsets = new HashSet<>();
			for (Direction d : Iterate.directions) {
				if (d.getAxis() == axis)
					continue;
				BlockPos centralOffset = BlockPos.ORIGIN.offset(d, 2);
				offsets.add(centralOffset);
				for (Direction d2 : Iterate.directions) {
					if (d2.getAxis() == axis)
						continue;
					if (d2.getAxis() == d.getAxis())
						continue;
					offsets.add(centralOffset.offset(d2));
				}
			}
			LARGE_OFFSETS.put(axis, offsets);
		}
	}

	public int flowScore;
	public BlockState material;

	public WaterWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		material = Blocks.SPRUCE_PLANKS.getDefaultState();
		setLazyTickRate(60);
	}

	protected int getSize() {
		return 1;
	}

	protected Set<BlockPos> getOffsetsToCheck() {
		return (getSize() == 1 ? SMALL_OFFSETS : LARGE_OFFSETS).get(getAxis());
	}

	public ActionResult applyMaterialIfValid(ItemStack stack) {
		if (!(stack.getItem()instanceof BlockItem blockItem))
			return ActionResult.PASS;
		BlockState material = blockItem.getBlock()
			.getDefaultState();
		if (material == this.material)
			return ActionResult.PASS;
		if (!material.isIn(BlockTags.PLANKS))
			return ActionResult.PASS;
		if (world.isClient() && !isVirtual())
			return ActionResult.SUCCESS;
		this.material = material;
		notifyUpdate();
		world.syncWorldEvent(2001, pos, Block.getRawIdFromState(material));
		return ActionResult.SUCCESS;
	}

	protected Axis getAxis() {
		Axis axis = Axis.X;
		BlockState blockState = getCachedState();
		if (blockState.getBlock()instanceof IRotate irotate)
			axis = irotate.getRotationAxis(blockState);
		return axis;
	}

	@Override
	public void lazyTick() {
		super.lazyTick();

		// Water can change flow direction without notifying neighbours
		determineAndApplyFlowScore();
	}

	public void determineAndApplyFlowScore() {
		Vec3d wheelPlane =
			Vec3d.of(new Vec3i(1, 1, 1).subtract(Direction.get(AxisDirection.POSITIVE, getAxis())
				.getVector()));

		int flowScore = 0;
		boolean lava = false;
		for (BlockPos blockPos : getOffsetsToCheck()) {
			BlockPos targetPos = blockPos.add(pos);
			Vec3d flowAtPos = getFlowVectorAtPosition(targetPos).multiply(wheelPlane);
			lava |= FluidHelper.isLava(world.getFluidState(targetPos)
				.getFluid());

			if (flowAtPos.lengthSquared() == 0)
				continue;

			flowAtPos = flowAtPos.normalize();
			Vec3d normal = Vec3d.of(blockPos)
				.normalize();

			Vec3d positiveMotion = VecHelper.rotate(normal, 90, getAxis());
			double dot = flowAtPos.dotProduct(positiveMotion);
			if (Math.abs(dot) > .5)
				flowScore += Math.signum(dot);
		}

		if (flowScore != 0 && !world.isClient())
			award(lava ? AllAdvancements.LAVA_WHEEL : AllAdvancements.WATER_WHEEL);

		setFlowScoreAndUpdate(flowScore);
	}

	public Vec3d getFlowVectorAtPosition(BlockPos pos) {
		FluidState fluid = world.getFluidState(pos);
		Vec3d vec = fluid.getVelocity(world, pos);
		BlockState blockState = world.getBlockState(pos);
		if (blockState.getBlock() == Blocks.BUBBLE_COLUMN)
			vec = new Vec3d(0, blockState.get(BubbleColumnBlock.DRAG) ? -1 : 1, 0);
		return vec;
	}

	public void setFlowScoreAndUpdate(int score) {
		if (flowScore == score)
			return;
		flowScore = score;
		updateGeneratedRotation();
		markDirty();
	}

	private void redraw() {
		if (!isVirtual())
			requestModelDataUpdate();
		if (hasWorld()) {
			world.updateListeners(getPos(), getCachedState(), getCachedState(), 16);
			world.getChunkManager()
				.getLightingProvider()
				.checkBlock(pos);
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.LAVA_WHEEL, AllAdvancements.WATER_WHEEL);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		flowScore = compound.getInt("FlowScore");

		BlockState prevMaterial = material;
		if (!compound.contains("Material"))
			return;

		material = NbtHelper.toBlockState(blockHolderGetter(), compound.getCompound("Material"));
		if (material.isAir())
			material = Blocks.SPRUCE_PLANKS.getDefaultState();

		if (clientPacket && prevMaterial != material)
			redraw();
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.putInt("FlowScore", flowScore);
		compound.put("Material", NbtHelper.fromBlockState(material));
	}

	@Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).expand(getSize());
	}

	@Override
	public float getGeneratedSpeed() {
		return MathHelper.clamp(flowScore, -1, 1) * 8 / getSize();
	}

}
