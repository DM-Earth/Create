package com.simibubi.create.content.logistics.tunnel;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.content.kinetics.belt.transport.ItemHandlerBeltSegment;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock.Shape;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import com.tterrag.registrate.fabric.EnvExecutor;

import io.github.fabricators_of_create.porting_lib.util.StorageProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.world.World;

public class BeltTunnelBlockEntity extends SmartBlockEntity implements SidedStorageBlockEntity {

	public Map<Direction, LerpedFloat> flaps;
	public Set<Direction> sides;

	protected StorageProvider<ItemVariant> belowProvider;
	protected List<Pair<Direction, Boolean>> flapsToSend;

	public BeltTunnelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		flaps = new EnumMap<>(Direction.class);
		sides = new HashSet<>();
		flapsToSend = new LinkedList<>();
	}

	@Override
	public void setWorld(World level) {
		super.setWorld(level);
		belowProvider = StorageProvider.createForItems(level, pos.down()).filter(this::isBeltStorage);
	}

	public boolean isBeltStorage(StorageProvider<ItemVariant> provider, Storage<ItemVariant> storage) {
		return storage instanceof ItemHandlerBeltSegment;
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	protected void writeFlapsAndSides(NbtCompound compound) {
		NbtList flapsNBT = new NbtList();
		for (Direction direction : flaps.keySet())
			flapsNBT.add(NbtInt.of(direction.getId()));
		compound.put("Flaps", flapsNBT);

		NbtList sidesNBT = new NbtList();
		for (Direction direction : sides)
			sidesNBT.add(NbtInt.of(direction.getId()));
		compound.put("Sides", sidesNBT);
	}

	@Override
	public void writeSafe(NbtCompound tag) {
		writeFlapsAndSides(tag);
		super.writeSafe(tag);
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		writeFlapsAndSides(compound);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		Set<Direction> newFlaps = new HashSet<>(6);
		NbtList flapsNBT = compound.getList("Flaps", NbtElement.INT_TYPE);
		for (NbtElement inbt : flapsNBT)
			if (inbt instanceof NbtInt)
				newFlaps.add(Direction.byId(((NbtInt) inbt).intValue()));

		sides.clear();
		NbtList sidesNBT = compound.getList("Sides", NbtElement.INT_TYPE);
		for (NbtElement inbt : sidesNBT)
			if (inbt instanceof NbtInt)
				sides.add(Direction.byId(((NbtInt) inbt).intValue()));

		for (Direction d : Iterate.directions)
			if (!newFlaps.contains(d))
				flaps.remove(d);
			else if (!flaps.containsKey(d))
				flaps.put(d, createChasingFlap());

		// Backwards compat
		if (!compound.contains("Sides") && compound.contains("Flaps"))
			sides.addAll(flaps.keySet());
		super.read(compound, clientPacket);
		if (clientPacket)
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> InstancedRenderDispatcher.enqueueUpdate(this));
	}

	private LerpedFloat createChasingFlap() {
		return LerpedFloat.linear()
			.startWithValue(.25f)
			.chase(0, .05f, Chaser.EXP);
	}

	public void updateTunnelConnections() {
		flaps.clear();
		sides.clear();
		BlockState tunnelState = getCachedState();
		for (Direction direction : Iterate.horizontalDirections) {
			if (direction.getAxis() != tunnelState.get(Properties.HORIZONTAL_AXIS)) {
				boolean positive =
					direction.getDirection() == AxisDirection.POSITIVE ^ direction.getAxis() == Axis.Z;
				Shape shape = tunnelState.get(BeltTunnelBlock.SHAPE);
				if (BeltTunnelBlock.isStraight(tunnelState))
					continue;
				if (positive && shape == Shape.T_LEFT)
					continue;
				if (!positive && shape == Shape.T_RIGHT)
					continue;
			}

			sides.add(direction);

			// Flap might be occluded
			BlockState nextState = world.getBlockState(pos.offset(direction));
			if (nextState.getBlock() instanceof BeltTunnelBlock)
				continue;
			if (nextState.getBlock() instanceof BeltFunnelBlock)
				if (nextState.get(BeltFunnelBlock.SHAPE) == BeltFunnelBlock.Shape.EXTENDED
					&& nextState.get(BeltFunnelBlock.HORIZONTAL_FACING) == direction.getOpposite())
					continue;

			flaps.put(direction, createChasingFlap());
		}
		sendData();
	}

	public void flap(Direction side, boolean inward) {
		if (world.isClient) {
			if (flaps.containsKey(side))
				flaps.get(side)
					.setValue(inward ^ side.getAxis() == Axis.Z ? -1 : 1);
			return;
		}

		flapsToSend.add(Pair.of(side, inward));
	}

	@Override
	public void initialize() {
		super.initialize();
		updateTunnelConnections();
	}

	@Override
	public void tick() {
		super.tick();
		if (!world.isClient) {
			if (!flapsToSend.isEmpty())
				sendFlaps();
			return;
		}
		flaps.forEach((d, value) -> value.tickChaser());
	}

	private void sendFlaps() {
		AllPackets.getChannel().sendToClientsTracking(new TunnelFlapPacket(this, flapsToSend), (ServerWorld) world, getPos());
		flapsToSend.clear();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		if (belowProvider == null)
			return null;
		if (belowProvider.findBlockEntity() instanceof BeltBlockEntity)
			return belowProvider.get(Direction.UP);
		return null;
	}
}
