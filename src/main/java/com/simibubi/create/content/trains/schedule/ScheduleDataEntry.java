package com.simibubi.create.content.trains.schedule;

import net.minecraft.nbt.NbtCompound;

public abstract class ScheduleDataEntry implements IScheduleInput {

	protected NbtCompound data;

	public ScheduleDataEntry() {
		data = new NbtCompound();
	}

	@Override
	public NbtCompound getData() {
		return data;
	}

	@Override
	public void setData(NbtCompound data) {
		this.data = data;
		readAdditional(data);
	}

	protected void writeAdditional(NbtCompound tag) {};

	protected void readAdditional(NbtCompound tag) {};

	protected <T> T enumData(String key, Class<T> enumClass) {
		T[] enumConstants = enumClass.getEnumConstants();
		return enumConstants[data.getInt(key) % enumConstants.length];
	}

	protected String textData(String key) {
		return data.getString(key);
	}

	protected int intData(String key) {
		return data.getInt(key);
	}

}
