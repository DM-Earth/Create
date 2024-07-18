package com.simibubi.create.content.redstone.displayLink.target;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlock;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldAccess;

public class NixieTubeDisplayTarget extends SingleLineDisplayTarget {

	@Override
	protected void acceptLine(MutableText text, DisplayLinkContext context) {
		String tagElement = Text.Serializer.toJson(text);
		NixieTubeBlock.walkNixies(context.level(), context.getTargetPos(), (currentPos, rowPosition) -> {
			BlockEntity blockEntity = context.level()
				.getBlockEntity(currentPos);
			if (blockEntity instanceof NixieTubeBlockEntity nixie)
				nixie.displayCustomText(tagElement, rowPosition);
		});
	}

	@Override
	protected int getWidth(DisplayLinkContext context) {
		MutableInt count = new MutableInt(0);
		NixieTubeBlock.walkNixies(context.level(), context.getTargetPos(), (currentPos, rowPosition) -> count.add(2));
		return count.intValue();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Box getMultiblockBounds(WorldAccess level, BlockPos pos) {
		MutableObject<BlockPos> start = new MutableObject<>(null);
		MutableObject<BlockPos> end = new MutableObject<>(null);
		NixieTubeBlock.walkNixies(level, pos, (currentPos, rowPosition) -> {
			end.setValue(currentPos);
			if (start.getValue() == null)
				start.setValue(currentPos);
		});

		BlockPos diffToCurrent = start.getValue()
			.subtract(pos);
		BlockPos diff = end.getValue()
			.subtract(start.getValue());

		return super.getMultiblockBounds(level, pos).offset(diffToCurrent)
			.stretch(Vec3d.of(diff));
	}

}
