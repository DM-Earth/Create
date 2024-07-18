package com.simibubi.create.content.equipment.symmetryWand.mirror;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.jozufozu.flywheel.core.PartialModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class EmptyMirror extends SymmetryMirror {

	public static enum Align implements StringIdentifiable {
		None("none");

		private final String name;
		private Align(String name) { this.name = name; }
		@Override public String asString() { return name; }
		@Override public String toString() { return name; }
	}

	public EmptyMirror(Vec3d pos) {
		super(pos);
		orientation = Align.None;
	}

	@Override
	protected void setOrientation() {
	}

	@Override
	public void setOrientation(int index) {
		this.orientation = Align.values()[index];
		orientationIndex = index;
	}

	@Override
	public Map<BlockPos, BlockState> process(BlockPos position, BlockState block) {
		return new HashMap<>();
	}

	@Override
	public String typeName() {
		return EMPTY;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public PartialModel getModel() {
		return null;
	}

	@Override
	public List<Text> getAlignToolTips() {
		return ImmutableList.of();
	}

}
