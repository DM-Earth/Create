package com.simibubi.create.content.equipment.bell;

import java.util.List;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.NBTHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public abstract class AbstractBellBlockEntity extends SmartBlockEntity {

	public static final int RING_DURATION = 74;

	public boolean isRinging;
	public int ringingTicks;
	public Direction ringDirection;

	public AbstractBellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) { }

	public boolean ring(World world, BlockPos pos, Direction direction) {
		isRinging = true;
		ringingTicks = 0;
		ringDirection = direction;
		sendData();
		return true;
	};

	@Override
	public void tick() {
		super.tick();

		if (isRinging) {
			++ringingTicks;
		}

		if (ringingTicks >= RING_DURATION) {
			isRinging = false;
			ringingTicks = 0;
		}
	}
	
	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		if (!clientPacket || ringingTicks != 0 || !isRinging)
			return;
		NBTHelper.writeEnum(tag, "Ringing", ringDirection);
	}
	
	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		if (!clientPacket || !tag.contains("Ringing"))
			return;
		ringDirection = NBTHelper.readEnum(tag, "Ringing", Direction.class);
		ringingTicks = 0;
		isRinging = true;
	}

	@Environment(EnvType.CLIENT)
	public abstract PartialModel getBellModel();

}
