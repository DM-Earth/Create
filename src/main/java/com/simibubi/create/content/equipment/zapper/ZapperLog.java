package com.simibubi.create.content.equipment.zapper;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ZapperLog {

	private World activeWorld;
	private List<List<StructureBlockInfo>> log = new LinkedList<>();
//	private int redoIndex;

	/*
	 * Undo and redo operations applied by tools what information is necessary?
	 *
	 * For survival mode: does undo have the required blocks
	 *
	 * For creative mode: what data did removed TEs have
	 *
	 * When undo: remove added blocks (added -> air) replace replaced blocks (added
	 * -> before) add removed blocks (air -> before)
	 *
	 */

	public void record(World world, List<BlockPos> positions) {
//		if (maxLogLength() == 0)
//			return;
		if (world != activeWorld)
			log.clear();
		activeWorld = world;

		List<StructureBlockInfo> blocks = positions.stream().map(pos -> {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			return new StructureBlockInfo(pos, world.getBlockState(pos), blockEntity == null ? null : blockEntity.createNbtWithIdentifyingData());
		}).collect(Collectors.toList());

		log.add(0, blocks);
//		redoIndex = 0;

//		if (maxLogLength() < log.size())
//			log.remove(log.size() - 1);
	}

//	protected Integer maxLogLength() {
//		return AllConfigs.SERVER.curiosities.zapperUndoLogLength.get();
//	}

	public void undo() {

	}

	public void redo() {

	}

}
