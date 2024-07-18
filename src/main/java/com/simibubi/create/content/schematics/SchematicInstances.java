package com.simibubi.create.content.schematics;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.foundation.utility.WorldAttached;

public class SchematicInstances {

	private static final WorldAttached<Cache<Integer, SchematicWorld>> LOADED_SCHEMATICS = new WorldAttached<>($ -> CacheBuilder.newBuilder()
			.expireAfterAccess(5, TimeUnit.MINUTES)
			.build());

	@Nullable
	public static SchematicWorld get(World world, ItemStack schematic) {
		Cache<Integer, SchematicWorld> map = LOADED_SCHEMATICS.get(world);
		int hash = getHash(schematic);
		SchematicWorld ifPresent = map.getIfPresent(hash);
		if (ifPresent != null)
			return ifPresent;
		SchematicWorld loadWorld = loadWorld(world, schematic);
		if (loadWorld == null)
			return null;
		map.put(hash, loadWorld);
		return loadWorld;
	}

	private static SchematicWorld loadWorld(World wrapped, ItemStack schematic) {
		if (schematic == null || !schematic.hasNbt())
			return null;
		if (!schematic.getNbt()
			.getBoolean("Deployed"))
			return null;

		StructureTemplate activeTemplate =
			SchematicItem.loadSchematic(wrapped.createCommandRegistryWrapper(RegistryKeys.BLOCK), schematic);

		if (activeTemplate.getSize()
			.equals(Vec3i.ZERO))
			return null;

		BlockPos anchor = NbtHelper.toBlockPos(schematic.getNbt()
			.getCompound("Anchor"));
		SchematicWorld world = new SchematicWorld(anchor, wrapped);
		StructurePlacementData settings = SchematicItem.getSettings(schematic);
		activeTemplate.place(world, anchor, anchor, settings, wrapped.getRandom(), Block.NOTIFY_LISTENERS);

		StructureTransform transform = new StructureTransform(settings.getPosition(), Direction.Axis.Y,
			settings.getRotation(), settings.getMirror());
		for (BlockEntity be : world.getBlockEntities())
			transform.apply(be);

		return world;
	}

	public static void clearHash(ItemStack schematic) {
		if (schematic == null || !schematic.hasNbt())
			return;
		schematic.getNbt()
			.remove("SchematicHash");
	}

	public static int getHash(ItemStack schematic) {
		if (schematic == null || !schematic.hasNbt())
			return -1;
		NbtCompound tag = schematic.getNbt();
		if (!tag.contains("SchematicHash"))
			tag.putInt("SchematicHash", tag.toString()
				.hashCode());
		return tag.getInt("SchematicHash");
	}

}
