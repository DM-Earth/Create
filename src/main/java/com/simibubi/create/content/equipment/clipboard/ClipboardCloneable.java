package com.simibubi.create.content.equipment.clipboard;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;

public interface ClipboardCloneable {

	public String getClipboardKey();
	
	public boolean writeToClipboard(NbtCompound tag, Direction side);
	
	public boolean readFromClipboard(NbtCompound tag, PlayerEntity player, Direction side, boolean simulate);
	
}
