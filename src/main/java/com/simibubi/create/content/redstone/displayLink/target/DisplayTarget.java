package com.simibubi.create.content.redstone.displayLink.target;

import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.WorldAccess;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.displayLink.DisplayBehaviour;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.foundation.utility.Lang;

public abstract class DisplayTarget extends DisplayBehaviour {

	public abstract void acceptText(int line, List<MutableText> text, DisplayLinkContext context);

	public abstract DisplayTargetStats provideStats(DisplayLinkContext context);

	public Box getMultiblockBounds(WorldAccess level, BlockPos pos) {
		VoxelShape shape = level.getBlockState(pos)
			.getOutlineShape(level, pos);
		if (shape.isEmpty())
			return new Box(pos);
		return shape.getBoundingBox()
			.offset(pos);
	}

	public Text getLineOptionText(int line) {
		return Lang.translateDirect("display_target.line", line + 1);
	}

	public static void reserve(int line, BlockEntity target, DisplayLinkContext context) {
		if (line == 0)
			return;

		NbtCompound tag = target.getCustomData();
		NbtCompound compound = tag.getCompound("DisplayLink");
		compound.putLong("Line" + line, context.blockEntity()
			.getPos()
			.asLong());
		tag.put("DisplayLink", compound);
	}

	public boolean isReserved(int line, BlockEntity target, DisplayLinkContext context) {
		NbtCompound tag = target.getCustomData();
		NbtCompound compound = tag.getCompound("DisplayLink");

		if (!compound.contains("Line" + line))
			return false;

		long l = compound.getLong("Line" + line);
		BlockPos reserved = BlockPos.fromLong(l);

		if (!reserved.equals(context.blockEntity()
			.getPos()) && AllBlocks.DISPLAY_LINK.has(target.getWorld()
				.getBlockState(reserved)))
			return true;

		compound.remove("Line" + line);
		if (compound.isEmpty())
			tag.remove("DisplayLink");
		return false;
	}

}
