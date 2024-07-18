package com.simibubi.create.content.contraptions;

import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class AssemblyException extends Exception {

	private static final long serialVersionUID = 1L;
	public final Text component;
	private BlockPos position = null;

	public static void write(NbtCompound compound, AssemblyException exception) {
		if (exception == null)
			return;

		NbtCompound nbt = new NbtCompound();
		nbt.putString("Component", Text.Serializer.toJson(exception.component));
		if (exception.hasPosition())
			nbt.putLong("Position", exception.getPosition()
				.asLong());

		compound.put("LastException", nbt);
	}

	public static AssemblyException read(NbtCompound compound) {
		if (!compound.contains("LastException"))
			return null;

		NbtCompound nbt = compound.getCompound("LastException");
		String string = nbt.getString("Component");
		AssemblyException exception = new AssemblyException(Text.Serializer.fromJson(string));
		if (nbt.contains("Position"))
			exception.position = BlockPos.fromLong(nbt.getLong("Position"));

		return exception;
	}

	public AssemblyException(Text component) {
		this.component = component;
	}

	public AssemblyException(String langKey, Object... objects) {
		this(Lang.translateDirect("gui.assembly.exception." + langKey, objects));
	}

	public static AssemblyException unmovableBlock(BlockPos pos, BlockState state) {
		AssemblyException e = new AssemblyException("unmovableBlock", pos.getX(), pos.getY(), pos.getZ(),
			state.getBlock().getName());
		e.position = pos;
		return e;
	}

	public static AssemblyException unloadedChunk(BlockPos pos) {
		AssemblyException e = new AssemblyException("chunkNotLoaded", pos.getX(), pos.getY(), pos.getZ());
		e.position = pos;
		return e;
	}

	public static AssemblyException structureTooLarge() {
		return new AssemblyException("structureTooLarge", AllConfigs.server().kinetics.maxBlocksMoved.get());
	}

	public static AssemblyException tooManyPistonPoles() {
		return new AssemblyException("tooManyPistonPoles", AllConfigs.server().kinetics.maxPistonPoles.get());
	}

	public static AssemblyException noPistonPoles() {
		return new AssemblyException("noPistonPoles");
	}
	
	public static AssemblyException notEnoughSails(int sails) {
		return new AssemblyException("not_enough_sails", sails, AllConfigs.server().kinetics.minimumWindmillSails.get());
	}

	public boolean hasPosition() {
		return position != null;
	}

	public BlockPos getPosition() {
		return position;
	}
}
