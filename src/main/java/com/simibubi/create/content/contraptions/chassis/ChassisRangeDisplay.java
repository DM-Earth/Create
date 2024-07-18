package com.simibubi.create.content.contraptions.chassis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.AllTags;
import com.simibubi.create.CreateClient;

public class ChassisRangeDisplay {

	private static final int DISPLAY_TIME = 200;
	private static GroupEntry lastHoveredGroup = null;

	private static class Entry {
		ChassisBlockEntity be;
		int timer;

		public Entry(ChassisBlockEntity be) {
			this.be = be;
			timer = DISPLAY_TIME;
			CreateClient.OUTLINER.showCluster(getOutlineKey(), createSelection(be))
				.colored(0xFFFFFF)
				.disableLineNormals()
				.lineWidth(1 / 16f)
				.withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
		}

		protected Object getOutlineKey() {
			return Pair.of(be.getPos(), 1);
		}

		protected Set<BlockPos> createSelection(ChassisBlockEntity chassis) {
			Set<BlockPos> positions = new HashSet<>();
			List<BlockPos> includedBlockPositions = chassis.getIncludedBlockPositions(null, true);
			if (includedBlockPositions == null)
				return Collections.emptySet();
			positions.addAll(includedBlockPositions);
			return positions;
		}

	}

	private static class GroupEntry extends Entry {

		List<ChassisBlockEntity> includedBEs;

		public GroupEntry(ChassisBlockEntity be) {
			super(be);
		}

		@Override
		protected Object getOutlineKey() {
			return this;
		}

		@Override
		protected Set<BlockPos> createSelection(ChassisBlockEntity chassis) {
			Set<BlockPos> list = new HashSet<>();
			includedBEs = be.collectChassisGroup();
			if (includedBEs == null)
				return list;
			for (ChassisBlockEntity chassisBlockEntity : includedBEs)
				list.addAll(super.createSelection(chassisBlockEntity));
			return list;
		}

	}

	static Map<BlockPos, Entry> entries = new HashMap<>();
	static List<GroupEntry> groupEntries = new ArrayList<>();

	public static void tick() {
		PlayerEntity player = MinecraftClient.getInstance().player;
		World world = MinecraftClient.getInstance().world;
		boolean hasWrench = AllItems.WRENCH.isIn(player.getMainHandStack());

		for (Iterator<BlockPos> iterator = entries.keySet()
			.iterator(); iterator.hasNext();) {
			BlockPos pos = iterator.next();
			Entry entry = entries.get(pos);
			if (tickEntry(entry, hasWrench))
				iterator.remove();
			CreateClient.OUTLINER.keep(entry.getOutlineKey());
		}

		for (Iterator<GroupEntry> iterator = groupEntries.iterator(); iterator.hasNext();) {
			GroupEntry group = iterator.next();
			if (tickEntry(group, hasWrench)) {
				iterator.remove();
				if (group == lastHoveredGroup)
					lastHoveredGroup = null;
			}
			CreateClient.OUTLINER.keep(group.getOutlineKey());
		}

		if (!hasWrench)
			return;

		HitResult over = MinecraftClient.getInstance().crosshairTarget;
		if (!(over instanceof BlockHitResult))
			return;
		BlockHitResult ray = (BlockHitResult) over;
		BlockPos pos = ray.getBlockPos();
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null || blockEntity.isRemoved())
			return;
		if (!(blockEntity instanceof ChassisBlockEntity))
			return;

		boolean ctrl = AllKeys.ctrlDown();
		ChassisBlockEntity chassisBlockEntity = (ChassisBlockEntity) blockEntity;

		if (ctrl) {
			GroupEntry existingGroupForPos = getExistingGroupForPos(pos);
			if (existingGroupForPos != null) {
				for (ChassisBlockEntity included : existingGroupForPos.includedBEs)
					entries.remove(included.getPos());
				existingGroupForPos.timer = DISPLAY_TIME;
				return;
			}
		}

		if (!entries.containsKey(pos) || ctrl)
			display(chassisBlockEntity);
		else {
			if (!ctrl)
				entries.get(pos).timer = DISPLAY_TIME;
		}
	}

	private static boolean tickEntry(Entry entry, boolean hasWrench) {
		ChassisBlockEntity chassisBlockEntity = entry.be;
		World beWorld = chassisBlockEntity.getWorld();
		World world = MinecraftClient.getInstance().world;

		if (chassisBlockEntity.isRemoved() || beWorld == null || beWorld != world
			|| !world.canSetBlock(chassisBlockEntity.getPos())) {
			return true;
		}

		if (!hasWrench && entry.timer > 20) {
			entry.timer = 20;
			return false;
		}

		entry.timer--;
		if (entry.timer == 0)
			return true;
		return false;
	}

	public static void display(ChassisBlockEntity chassis) {

		// Display a group and kill any selections of its contained chassis blocks
		if (AllKeys.ctrlDown()) {
			GroupEntry hoveredGroup = new GroupEntry(chassis);

			for (ChassisBlockEntity included : hoveredGroup.includedBEs)
				CreateClient.OUTLINER.remove(Pair.of(included.getPos(), 1));

			groupEntries.forEach(entry -> CreateClient.OUTLINER.remove(entry.getOutlineKey()));
			groupEntries.clear();
			entries.clear();
			groupEntries.add(hoveredGroup);
			return;
		}

		// Display an individual chassis and kill any group selections that contained it
		BlockPos pos = chassis.getPos();
		GroupEntry entry = getExistingGroupForPos(pos);
		if (entry != null)
			CreateClient.OUTLINER.remove(entry.getOutlineKey());

		groupEntries.clear();
		entries.clear();
		entries.put(pos, new Entry(chassis));

	}

	private static GroupEntry getExistingGroupForPos(BlockPos pos) {
		for (GroupEntry groupEntry : groupEntries)
			for (ChassisBlockEntity chassis : groupEntry.includedBEs)
				if (pos.equals(chassis.getPos()))
					return groupEntry;
		return null;
	}

}
