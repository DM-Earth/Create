package com.simibubi.create.content.contraptions.behaviour;

import java.util.function.UnaryOperator;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.utility.VecHelper;

public class MovementContext {

	public Vec3d position;
	public Vec3d motion;
	public Vec3d relativeMotion;
	public UnaryOperator<Vec3d> rotation;

	public World world;
	public BlockState state;
	public BlockPos localPos;
	public NbtCompound blockEntityData;

	public boolean stall;
	public boolean disabled;
	public boolean firstMovement;
	public NbtCompound data;
	public Contraption contraption;
	public Object temporaryData;
	
	private FilterItemStack filter;

	public MovementContext(World world, StructureBlockInfo info, Contraption contraption) {
		this.world = world;
		this.state = info.state();
		this.blockEntityData = info.nbt();
		this.contraption = contraption;
		localPos = info.pos();

		disabled = false;
		firstMovement = true;
		motion = Vec3d.ZERO;
		relativeMotion = Vec3d.ZERO;
		rotation = v -> v;
		position = null;
		data = new NbtCompound();
		stall = false;
		filter = null;
	}

	public float getAnimationSpeed() {
		int modifier = 1000;
		double length = -motion.length();
		if (disabled)
			return 0;
		if (world.isClient && contraption.stalled)
			return 700;
		if (Math.abs(length) < 1 / 512f)
			return 0;
		return (((int) (length * modifier + 100 * Math.signum(length))) / 100) * 100;
	}

	public static MovementContext readNBT(World world, StructureBlockInfo info, NbtCompound nbt, Contraption contraption) {
		MovementContext context = new MovementContext(world, info, contraption);
		context.motion = VecHelper.readNBT(nbt.getList("Motion", NbtElement.DOUBLE_TYPE));
		context.relativeMotion = VecHelper.readNBT(nbt.getList("RelativeMotion", NbtElement.DOUBLE_TYPE));
		if (nbt.contains("Position"))
			context.position = VecHelper.readNBT(nbt.getList("Position", NbtElement.DOUBLE_TYPE));
		context.stall = nbt.getBoolean("Stall");
		context.firstMovement = nbt.getBoolean("FirstMovement");
		context.data = nbt.getCompound("Data");
		return context;
	}

	public NbtCompound writeToNBT(NbtCompound nbt) {
		nbt.put("Motion", VecHelper.writeNBT(motion));
		nbt.put("RelativeMotion", VecHelper.writeNBT(relativeMotion));
		if (position != null)
			nbt.put("Position", VecHelper.writeNBT(position));
		nbt.putBoolean("Stall", stall);
		nbt.putBoolean("FirstMovement", firstMovement);
		nbt.put("Data", data.copy());
		return nbt;
	}
	
	public FilterItemStack getFilterFromBE() {
		if (filter != null)
			return filter;
		return filter = FilterItemStack.of(blockEntityData.getCompound("Filter"));
	}

}
