package com.simibubi.create.content.contraptions.elevator;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn.ColumnCoords;
import com.simibubi.create.content.decoration.slidingDoor.DoorControlBehaviour;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.NBTHelper;

public class ElevatorContactBlockEntity extends SmartBlockEntity {

	public DoorControlBehaviour doorControls;
	public ColumnCoords columnCoords;
	public boolean activateBlock;

	public String shortName;
	public String longName;

	public String lastReportedCurrentFloor;

	private int yTargetFromNBT = Integer.MIN_VALUE;
	private boolean deferNameGenerator;

	public ElevatorContactBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		shortName = "";
		longName = "";
		deferNameGenerator = false;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(doorControls = new DoorControlBehaviour(this));
	}

	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		super.write(tag, clientPacket);

		tag.putString("ShortName", shortName);
		tag.putString("LongName", longName);

		if (lastReportedCurrentFloor != null)
			tag.putString("LastReportedCurrentFloor", lastReportedCurrentFloor);

		if (clientPacket)
			return;
		tag.putBoolean("Activate", activateBlock);
		if (columnCoords == null)
			return;

		ElevatorColumn column = ElevatorColumn.get(world, columnCoords);
		if (column == null)
			return;
		tag.putInt("ColumnTarget", column.getTargetedYLevel());
		if (column.isActive())
			NBTHelper.putMarker(tag, "ColumnActive");
	}

	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		super.read(tag, clientPacket);

		shortName = tag.getString("ShortName");
		longName = tag.getString("LongName");

		if (tag.contains("LastReportedCurrentFloor"))
			lastReportedCurrentFloor = tag.getString("LastReportedCurrentFloor");

		if (clientPacket)
			return;
		activateBlock = tag.getBoolean("Activate");
		if (!tag.contains("ColumnTarget"))
			return;

		int target = tag.getInt("ColumnTarget");
		boolean active = tag.contains("ColumnActive");

		if (columnCoords == null) {
			yTargetFromNBT = target;
			return;
		}

		ElevatorColumn column = ElevatorColumn.getOrCreate(world, columnCoords);
		column.target(target);
		column.setActive(active);
	}

	public void updateDisplayedFloor(String floor) {
		if (floor.equals(lastReportedCurrentFloor))
			return;
		lastReportedCurrentFloor = floor;
		DisplayLinkBlock.notifyGatherers(world, pos);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (world.isClient())
			return;
		columnCoords = ElevatorContactBlock.getColumnCoords(world, pos);
		if (columnCoords == null)
			return;
		ElevatorColumn column = ElevatorColumn.getOrCreate(world, columnCoords);
		column.add(pos);
		if (shortName.isBlank())
			deferNameGenerator = true;
		if (yTargetFromNBT == Integer.MIN_VALUE)
			return;
		column.target(yTargetFromNBT);
		yTargetFromNBT = Integer.MIN_VALUE;
	}

	@Override
	public void tick() {
		super.tick();
		if (!deferNameGenerator)
			return;
		if (columnCoords != null)
			ElevatorColumn.getOrCreate(world, columnCoords)
				.initNames(world);
		deferNameGenerator = false;
	}

	@Override
	public void invalidate() {
		if (columnCoords != null) {
			ElevatorColumn column = ElevatorColumn.get(world, columnCoords);
			if (column != null)
				column.remove(pos);
		}
		super.invalidate();
	}

	public void updateName(String shortName, String longName) {
		this.shortName = shortName;
		this.longName = longName;
		this.deferNameGenerator = false;
		notifyUpdate();

		ElevatorColumn column = ElevatorColumn.get(world, columnCoords);
		if (column != null)
			column.namesChanged();
	}

	public Couple<String> getNames() {
		return Couple.create(shortName, longName);
	}

}
