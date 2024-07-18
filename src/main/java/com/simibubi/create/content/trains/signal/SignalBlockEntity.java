package com.simibubi.create.content.trains.signal;

import java.util.List;

import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import com.simibubi.create.content.contraptions.ITransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.signal.SignalBlock.SignalType;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.NBTHelper;

public class SignalBlockEntity extends SmartBlockEntity implements ITransformableBlockEntity {

	public static enum OverlayState {
		RENDER, SKIP, DUAL
	}

	public static enum SignalState {
		RED, YELLOW, GREEN, INVALID;

		public boolean isRedLight(float renderTime) {
			return this == RED || this == INVALID && renderTime % 40 < 3;
		}

		public boolean isYellowLight(float renderTime) {
			return this == YELLOW;
		}

		public boolean isGreenLight(float renderTime) {
			return this == GREEN;
		}
	}

	public TrackTargetingBehaviour<SignalBoundary> edgePoint;

	private SignalState state;
	private OverlayState overlay;
	private int switchToRedAfterTrainEntered;
	private boolean lastReportedPower;

	public SignalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		this.state = SignalState.INVALID;
		this.overlay = OverlayState.SKIP;
		this.lastReportedPower = false;
	}

	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		NBTHelper.writeEnum(tag, "State", state);
		NBTHelper.writeEnum(tag, "Overlay", overlay);
		tag.putBoolean("Power", lastReportedPower);
	}

	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		state = NBTHelper.readEnum(tag, "State", SignalState.class);
		overlay = NBTHelper.readEnum(tag, "Overlay", OverlayState.class);
		lastReportedPower = tag.getBoolean("Power");
		invalidateRenderBoundingBox();
	}

	@Nullable
	public SignalBoundary getSignal() {
		return edgePoint.getEdgePoint();
	}

	public boolean isPowered() {
		return state == SignalState.RED;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		edgePoint = new TrackTargetingBehaviour<>(this, EdgePointType.SIGNAL);
		behaviours.add(edgePoint);
	}

	@Override
	public void tick() {
		super.tick();
		if (world.isClient)
			return;

		SignalBoundary boundary = getSignal();
		if (boundary == null) {
			enterState(SignalState.INVALID);
			setOverlay(OverlayState.RENDER);
			return;
		}

		BlockState blockState = getCachedState();

		blockState.getOrEmpty(SignalBlock.POWERED).ifPresent(powered -> {
			if (lastReportedPower == powered)
				return;
			lastReportedPower = powered;
			boundary.updateBlockEntityPower(this);
			notifyUpdate();
		});
		
		blockState.getOrEmpty(SignalBlock.TYPE)
			.ifPresent(stateType -> {
				SignalType targetType = boundary.getTypeFor(pos);
				if (stateType != targetType) {
					world.setBlockState(pos, blockState.with(SignalBlock.TYPE, targetType), 3);
					refreshBlockState();
				}
			});

		enterState(boundary.getStateFor(pos));
		setOverlay(boundary.getOverlayFor(pos));
	}

	public boolean getReportedPower() {
		return lastReportedPower;
	}

	public SignalState getState() {
		return state;
	}

	public OverlayState getOverlay() {
		return overlay;
	}

	public void setOverlay(OverlayState state) {
		if (this.overlay == state)
			return;
		this.overlay = state;
		notifyUpdate();
	}

	public void enterState(SignalState state) {
		if (switchToRedAfterTrainEntered > 0)
			switchToRedAfterTrainEntered--;
		if (this.state == state)
			return;
		if (state == SignalState.RED && switchToRedAfterTrainEntered > 0)
			return;
		this.state = state;
		switchToRedAfterTrainEntered = state == SignalState.GREEN || state == SignalState.YELLOW ? 15 : 0;
		notifyUpdate();
	}

	@Override
	protected Box createRenderBoundingBox() {
		return new Box(pos, edgePoint.getGlobalPosition()).expand(2);
	}

	@Override
	public void transform(StructureTransform transform) {
		edgePoint.transform(transform);
	}

}
