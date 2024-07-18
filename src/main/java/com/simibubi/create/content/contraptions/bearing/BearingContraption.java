package com.simibubi.create.content.contraptions.bearing;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionType;
import com.simibubi.create.content.contraptions.render.ContraptionLighter;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class BearingContraption extends Contraption {

	protected int sailBlocks;
	protected Direction facing;

	private boolean isWindmill;

	public BearingContraption() {}

	public BearingContraption(boolean isWindmill, Direction facing) {
		this.isWindmill = isWindmill;
		this.facing = facing;
	}

	@Override
	public boolean assemble(World world, BlockPos pos) throws AssemblyException {
		BlockPos offset = pos.offset(facing);
		if (!searchMovedStructure(world, offset, null))
			return false;
		startMoving(world);
		expandBoundsAroundAxis(facing.getAxis());
		if (isWindmill && sailBlocks < AllConfigs.server().kinetics.minimumWindmillSails.get())
			throw AssemblyException.notEnoughSails(sailBlocks);
		if (blocks.isEmpty())
			return false;
		return true;
	}

	@Override
	public ContraptionType getType() {
		return ContraptionType.BEARING;
	}

	@Override
	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return pos.equals(anchor.offset(facing.getOpposite()));
	}

	@Override
	public void addBlock(BlockPos pos, Pair<StructureBlockInfo, BlockEntity> capture) {
		BlockPos localPos = pos.subtract(anchor);
		if (!getBlocks().containsKey(localPos) && AllBlockTags.WINDMILL_SAILS.matches(getSailBlock(capture)))
			sailBlocks++;
		super.addBlock(pos, capture);
	}

	private BlockState getSailBlock(Pair<StructureBlockInfo, BlockEntity> capture) {
		BlockState state = capture.getKey().state();
		if (AllBlocks.COPYCAT_PANEL.has(state) && capture.getRight() instanceof CopycatBlockEntity cbe)
			return cbe.getMaterial();
		return state;
	}

	@Override
	public NbtCompound writeNBT(boolean spawnPacket) {
		NbtCompound tag = super.writeNBT(spawnPacket);
		tag.putInt("Sails", sailBlocks);
		tag.putInt("Facing", facing.getId());
		return tag;
	}

	@Override
	public void readNBT(World world, NbtCompound tag, boolean spawnData) {
		sailBlocks = tag.getInt("Sails");
		facing = Direction.byId(tag.getInt("Facing"));
		super.readNBT(world, tag, spawnData);
	}

	public int getSailBlocks() {
		return sailBlocks;
	}

	public Direction getFacing() {
		return facing;
	}

	@Override
	public boolean canBeStabilized(Direction facing, BlockPos localPos) {
		if (facing.getOpposite() == this.facing && BlockPos.ORIGIN.equals(localPos))
			return false;
		return facing.getAxis() == this.facing.getAxis();
	}

	@Environment(EnvType.CLIENT)
	@Override
	public ContraptionLighter<?> makeLighter() {
		return new AnchoredLighter(this);
	}
}
