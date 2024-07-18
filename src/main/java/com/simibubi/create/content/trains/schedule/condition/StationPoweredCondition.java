package com.simibubi.create.content.trains.schedule.condition;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class StationPoweredCondition extends ScheduleWaitCondition {
	@Override
	public Pair<ItemStack, Text> getSummary() {
		return Pair.of(ItemStack.EMPTY, Lang.translateDirect("schedule.condition.powered"));
	}
	
	@Override
	public boolean tickCompletion(World level, Train train, NbtCompound context) {
		GlobalStation currentStation = train.getCurrentStation();
		if (currentStation == null)
			return false;
		BlockPos stationPos = currentStation.getBlockEntityPos();
		RegistryKey<World> stationDim = currentStation.getBlockEntityDimension();
		MinecraftServer server = level.getServer();
		if (server == null)
			return false;
		ServerWorld stationLevel = server.getWorld(stationDim);
		if (stationLevel == null || !stationLevel.canSetBlock(stationPos))
			return false;
		return stationLevel.isReceivingRedstonePower(stationPos);
	}

	@Override
	protected void writeAdditional(NbtCompound tag) {}

	@Override
	protected void readAdditional(NbtCompound tag) {}

	@Override
	public Identifier getId() {
		return Create.asResource("powered");
	}

	@Override
	public MutableText getWaitingStatus(World level, Train train, NbtCompound tag) {
		return Lang.translateDirect("schedule.condition.powered.status");
	}
}