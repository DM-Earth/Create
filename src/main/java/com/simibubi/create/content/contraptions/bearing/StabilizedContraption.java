package com.simibubi.create.content.contraptions.bearing;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionType;
import com.simibubi.create.content.contraptions.render.ContraptionLighter;
import com.simibubi.create.content.contraptions.render.NonStationaryLighter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class StabilizedContraption extends Contraption {

	private Direction facing;

	public StabilizedContraption() {}

	public StabilizedContraption(Direction facing) {
		this.facing = facing;
	}

	@Override
	public boolean assemble(World world, BlockPos pos) throws AssemblyException {
		BlockPos offset = pos.offset(facing);
		if (!searchMovedStructure(world, offset, null))
			return false;
		startMoving(world);
		if (blocks.isEmpty())
			return false;
		return true;
	}

	@Override
	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return false;
	}

	@Override
	public ContraptionType getType() {
		return ContraptionType.STABILIZED;
	}

	@Override
	public NbtCompound writeNBT(boolean spawnPacket) {
		NbtCompound tag = super.writeNBT(spawnPacket);
		tag.putInt("Facing", facing.getId());
		return tag;
	}

	@Override
	public void readNBT(World world, NbtCompound tag, boolean spawnData) {
		facing = Direction.byId(tag.getInt("Facing"));
		super.readNBT(world, tag, spawnData);
	}

	@Override
	public boolean canBeStabilized(Direction facing, BlockPos localPos) {
		return false;
	}

	public Direction getFacing() {
		return facing;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ContraptionLighter<?> makeLighter() {
		return new NonStationaryLighter<>(this);
	}
}
