package com.simibubi.create.content.contraptions.gantry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ContraptionType;
import com.simibubi.create.content.contraptions.TranslatingContraption;
import com.simibubi.create.content.contraptions.render.ContraptionLighter;
import com.simibubi.create.content.contraptions.render.NonStationaryLighter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class GantryContraption extends TranslatingContraption {

	protected Direction facing;

	public GantryContraption() {}

	public GantryContraption(Direction facing) {
		this.facing = facing;
	}

	@Override
	public boolean assemble(World world, BlockPos pos) throws AssemblyException {
		if (!searchMovedStructure(world, pos, null))
			return false;
		startMoving(world);
		return true;
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
	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return super.isAnchoringBlockAt(pos.offset(facing));
	}

	@Override
	public ContraptionType getType() {
		return ContraptionType.GANTRY;
	}

	public Direction getFacing() {
		return facing;
	}

	@Override
	protected boolean shouldUpdateAfterMovement(StructureBlockInfo info) {
		return super.shouldUpdateAfterMovement(info) && !AllBlocks.GANTRY_CARRIAGE.has(info.state());
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ContraptionLighter<?> makeLighter() {
		return new NonStationaryLighter<>(this);
	}
}
