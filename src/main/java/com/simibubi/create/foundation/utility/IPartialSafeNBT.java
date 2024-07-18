package com.simibubi.create.foundation.utility;

import net.minecraft.nbt.NbtCompound;

public interface IPartialSafeNBT {
	/** This method always runs on the logical server. */
	public void writeSafe(NbtCompound compound);
}
