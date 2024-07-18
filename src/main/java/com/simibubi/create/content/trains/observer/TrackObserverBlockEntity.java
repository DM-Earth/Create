package com.simibubi.create.content.trains.observer;

import java.util.List;

import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.content.contraptions.ITransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.Lang;

public class TrackObserverBlockEntity extends SmartBlockEntity implements ITransformableBlockEntity {

	public TrackTargetingBehaviour<TrackObserver> edgePoint;

	private FilteringBehaviour filtering;

	public TrackObserverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(edgePoint = new TrackTargetingBehaviour<>(this, EdgePointType.OBSERVER));
		behaviours.add(filtering = createFilter().withCallback(this::onFilterChanged));
		filtering.setLabel(Lang.translateDirect("logistics.train_observer.cargo_filter"));
	}

	private void onFilterChanged(ItemStack newFilter) {
		if (world.isClient())
			return;
		TrackObserver observer = getObserver();
		if (observer != null)
			observer.setFilterAndNotify(world, newFilter);
	}

	@Override
	public void tick() {
		super.tick();

		if (world.isClient())
			return;

		boolean shouldBePowered = false;
		TrackObserver observer = getObserver();
		if (observer != null)
			shouldBePowered = observer.isActivated();
		if (isBlockPowered() == shouldBePowered)
			return;

		BlockState blockState = getCachedState();
		if (blockState.contains(TrackObserverBlock.POWERED))
			world.setBlockState(pos, blockState.with(TrackObserverBlock.POWERED, shouldBePowered), 3);
		DisplayLinkBlock.notifyGatherers(world, pos);
	}

	@Nullable
	public TrackObserver getObserver() {
		return edgePoint.getEdgePoint();
	}
	
	public ItemStack getFilter() {
		return filtering.getFilter();
	}

	public boolean isBlockPowered() {
		return getCachedState().getOrEmpty(TrackObserverBlock.POWERED)
			.orElse(false);
	}

	@Override
	protected Box createRenderBoundingBox() {
		return new Box(pos, edgePoint.getGlobalPosition()).expand(2);
	}

	@Override
	public void transform(StructureTransform transform) {
		edgePoint.transform(transform);
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
				return new Vec3d(0.5, 15.5 / 16d, 0.5);
			}

		});
	}

}
