package com.simibubi.create.content.redstone.displayLink;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.redstone.displayLink.source.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTarget;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

public class DisplayLinkBlockEntity extends SmartBlockEntity {

	protected BlockPos targetOffset;

	public DisplaySource activeSource;
	private NbtCompound sourceConfig;

	public DisplayTarget activeTarget;
	public int targetLine;

	public LerpedFloat glow;
	private boolean sendPulse;

	public int refreshTicks;
	public AbstractComputerBehaviour computerBehaviour;

	public DisplayLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		targetOffset = BlockPos.ORIGIN;
		sourceConfig = new NbtCompound();
		targetLine = 0;
		glow = LerpedFloat.linear()
			.startWithValue(0);
		glow.chase(0, 0.5f, Chaser.EXP);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(computerBehaviour = ComputerCraftProxy.behaviour(this));
		registerAwardables(behaviours, AllAdvancements.DISPLAY_LINK, AllAdvancements.DISPLAY_BOARD);
	}

	@Override
	public void tick() {
		super.tick();

		if (isVirtual()) {
			glow.tickChaser();
			return;
		}

		if (activeSource == null)
			return;
		if (world.isClient) {
			glow.tickChaser();
			return;
		}

		refreshTicks++;
		if (refreshTicks < activeSource.getPassiveRefreshTicks() || !activeSource.shouldPassiveReset())
			return;
		tickSource();
	}

	public void tickSource() {
		refreshTicks = 0;
		if (getCachedState().getOrEmpty(DisplayLinkBlock.POWERED)
			.orElse(true))
			return;
		if (!world.isClient)
			updateGatheredData();
	}

	public void onNoLongerPowered() {
		if (activeSource == null)
			return;
		refreshTicks = 0;
		activeSource.onSignalReset(new DisplayLinkContext(world, this));
		updateGatheredData();
	}

	public void updateGatheredData() {
		BlockPos sourcePosition = getSourcePosition();
		BlockPos targetPosition = getTargetPosition();

		if (!world.canSetBlock(targetPosition) || !world.canSetBlock(sourcePosition))
			return;

		DisplayTarget target = AllDisplayBehaviours.targetOf(world, targetPosition);
		List<DisplaySource> sources = AllDisplayBehaviours.sourcesOf(world, sourcePosition);
		boolean notify = false;

		if (activeTarget != target) {
			activeTarget = target;
			notify = true;
		}

		if (activeSource != null && !sources.contains(activeSource)) {
			activeSource = null;
			sourceConfig = new NbtCompound();
			notify = true;
		}

		if (notify)
			notifyUpdate();
		if (activeSource == null || activeTarget == null)
			return;

		DisplayLinkContext context = new DisplayLinkContext(world, this);
		activeSource.transferData(context, activeTarget, targetLine);
		sendPulse = true;
		sendData();

		award(AllAdvancements.DISPLAY_LINK);
	}

	@Override
	public void writeSafe(NbtCompound tag) {
		super.writeSafe(tag);
		writeGatheredData(tag);
	}

	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		writeGatheredData(tag);
		if (clientPacket && activeTarget != null)
			tag.putString("TargetType", activeTarget.id.toString());
		if (clientPacket && sendPulse) {
			sendPulse = false;
			NBTHelper.putMarker(tag, "Pulse");
		}
	}

	private void writeGatheredData(NbtCompound tag) {
		tag.put("TargetOffset", NbtHelper.fromBlockPos(targetOffset));
		tag.putInt("TargetLine", targetLine);

		if (activeSource != null) {
			NbtCompound data = sourceConfig.copy();
			data.putString("Id", activeSource.id.toString());
			tag.put("Source", data);
		}
	}

	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		targetOffset = NbtHelper.toBlockPos(tag.getCompound("TargetOffset"));
		targetLine = tag.getInt("TargetLine");

		if (clientPacket && tag.contains("TargetType"))
			activeTarget = AllDisplayBehaviours.getTarget(new Identifier(tag.getString("TargetType")));
		if (clientPacket && tag.contains("Pulse"))
			glow.setValue(2);

		if (!tag.contains("Source"))
			return;

		NbtCompound data = tag.getCompound("Source");
		activeSource = AllDisplayBehaviours.getSource(new Identifier(data.getString("Id")));
		sourceConfig = new NbtCompound();
		if (activeSource != null)
			sourceConfig = data.copy();
	}

	public void target(BlockPos targetPosition) {
		this.targetOffset = targetPosition.subtract(pos);
	}

	public BlockPos getSourcePosition() {
		return pos.offset(getDirection());
	}

	public NbtCompound getSourceConfig() {
		return sourceConfig;
	}

	public void setSourceConfig(NbtCompound sourceConfig) {
		this.sourceConfig = sourceConfig;
	}

	public Direction getDirection() {
		return getCachedState().getOrEmpty(DisplayLinkBlock.FACING)
			.orElse(Direction.UP)
			.getOpposite();
	}

	public BlockPos getTargetPosition() {
		return pos.add(targetOffset);
	}

}
