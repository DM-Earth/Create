package com.simibubi.create.content.schematics;

import java.util.Iterator;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.RegisteredObjects;

public class SchematicAndQuillItem extends Item {

	public SchematicAndQuillItem(Settings properties) {
		super(properties);
	}

	public static void replaceStructureVoidWithAir(NbtCompound nbt) {
		String air = RegisteredObjects.getKeyOrThrow(Blocks.AIR)
			.toString();
		String structureVoid = RegisteredObjects.getKeyOrThrow(Blocks.STRUCTURE_VOID)
			.toString();

		NBTHelper.iterateCompoundList(nbt.getList("palette", 10), c -> {
			if (c.contains("Name") && c.getString("Name")
				.equals(structureVoid)) {
				c.putString("Name", air);
			}
		});
	}

	public static void clampGlueBoxes(World level, Box aabb, NbtCompound nbt) {
		NbtList listtag = nbt.getList("entities", 10)
			.copy();

		for (Iterator<NbtElement> iterator = listtag.iterator(); iterator.hasNext();) {
			NbtElement tag = iterator.next();
			if (!(tag instanceof NbtCompound compoundtag))
				continue;
			if (compoundtag.contains("nbt") && new Identifier(compoundtag.getCompound("nbt")
				.getString("id")).equals(AllEntityTypes.SUPER_GLUE.getId())) {
				iterator.remove();
			}
		}

		for (SuperGlueEntity entity : SuperGlueEntity.collectCropped(level, aabb)) {
			Vec3d vec3 = new Vec3d(entity.getX() - aabb.minX, entity.getY() - aabb.minY, entity.getZ() - aabb.minZ);
			NbtCompound compoundtag = new NbtCompound();
			entity.saveNbt(compoundtag);
			BlockPos blockpos = BlockPos.ofFloored(vec3);

			NbtCompound entityTag = new NbtCompound();
			entityTag.put("pos", newDoubleList(vec3.x, vec3.y, vec3.z));
			entityTag.put("blockPos", newIntegerList(blockpos.getX(), blockpos.getY(), blockpos.getZ()));
			entityTag.put("nbt", compoundtag.copy());
			listtag.add(entityTag);
		}

		nbt.put("entities", listtag);
	}

	private static NbtList newIntegerList(int... pValues) {
		NbtList listtag = new NbtList();
		for (int i : pValues)
			listtag.add(NbtInt.of(i));
		return listtag;
	}

	private static NbtList newDoubleList(double... pValues) {
		NbtList listtag = new NbtList();
		for (double d0 : pValues)
			listtag.add(NbtDouble.of(d0));
		return listtag;
	}

}
