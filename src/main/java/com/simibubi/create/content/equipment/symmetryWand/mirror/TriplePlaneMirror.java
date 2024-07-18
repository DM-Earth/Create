package com.simibubi.create.content.equipment.symmetryWand.mirror;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.utility.Lang;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class TriplePlaneMirror extends SymmetryMirror {

	public TriplePlaneMirror(Vec3d pos) {
		super(pos);
		orientationIndex = 0;
	}

	@Override
	public Map<BlockPos, BlockState> process(BlockPos position, BlockState block) {
		Map<BlockPos, BlockState> result = new HashMap<>();

		result.put(flipX(position), flipX(block));
		result.put(flipZ(position), flipZ(block));
		result.put(flipX(flipZ(position)), flipX(flipZ(block)));

		result.put(flipD1(position), flipD1(block));
		result.put(flipD1(flipX(position)), flipD1(flipX(block)));
		result.put(flipD1(flipZ(position)), flipD1(flipZ(block)));
		result.put(flipD1(flipX(flipZ(position))), flipD1(flipX(flipZ(block))));

		return result;
	}

	@Override
	public String typeName() {
		return TRIPLE_PLANE;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public PartialModel getModel() {
		return AllPartialModels.SYMMETRY_TRIPLEPLANE;
	}

	@Override
	protected void setOrientation() {
	}

	@Override
	public void setOrientation(int index) {
	}

	@Override
	public StringIdentifiable getOrientation() {
		return CrossPlaneMirror.Align.Y;
	}

	@Override
	public List<Text> getAlignToolTips() {
		return ImmutableList.of(Lang.translateDirect("orientation.horizontal"));
	}

}
