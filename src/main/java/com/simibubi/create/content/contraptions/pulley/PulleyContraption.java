package com.simibubi.create.content.contraptions.pulley;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ContraptionType;
import com.simibubi.create.content.contraptions.TranslatingContraption;
import com.simibubi.create.content.contraptions.render.ContraptionLighter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class PulleyContraption extends TranslatingContraption {

	int initialOffset;
	
	@Override
	public ContraptionType getType() {
		return ContraptionType.PULLEY;
	}

	public PulleyContraption() {}

	public PulleyContraption(int initialOffset) {
		this.initialOffset = initialOffset;
	}

	@Override
	public boolean assemble(World world, BlockPos pos) throws AssemblyException {
		if (!searchMovedStructure(world, pos, null))
			return false;
		startMoving(world);
		return true;
	}

	@Override
	protected boolean isAnchoringBlockAt(BlockPos pos) {
		if (pos.getX() != anchor.getX() || pos.getZ() != anchor.getZ())
			return false;
		int y = pos.getY();
		if (y <= anchor.getY() || y > anchor.getY() + initialOffset + 1)
			return false;
		return true;
	}

	@Override
	public NbtCompound writeNBT(boolean spawnPacket) {
		NbtCompound tag = super.writeNBT(spawnPacket);
		tag.putInt("InitialOffset", initialOffset);
		return tag;
	}

	@Override
	public void readNBT(World world, NbtCompound nbt, boolean spawnData) {
		initialOffset = nbt.getInt("InitialOffset");
		super.readNBT(world, nbt, spawnData);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ContraptionLighter<?> makeLighter() {
		return new PulleyLighter(this);
	}

	public int getInitialOffset() {
		return initialOffset;
	}
}
