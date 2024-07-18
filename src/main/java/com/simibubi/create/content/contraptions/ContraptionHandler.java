package com.simibubi.create.content.contraptions;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.WorldAttached;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

public class ContraptionHandler {

	/* Global map of loaded contraptions */

	public static WorldAttached<Map<Integer, WeakReference<AbstractContraptionEntity>>> loadedContraptions;
	static WorldAttached<List<AbstractContraptionEntity>> queuedAdditions;

	static {
		loadedContraptions = new WorldAttached<>($ -> new HashMap<>());
		queuedAdditions = new WorldAttached<>($ -> ObjectLists.synchronize(new ObjectArrayList<>()));
	}

	public static void tick(World world) {
		Map<Integer, WeakReference<AbstractContraptionEntity>> map = loadedContraptions.get(world);
		List<AbstractContraptionEntity> queued = queuedAdditions.get(world);

		for (AbstractContraptionEntity contraptionEntity : queued)
			map.put(contraptionEntity.getId(), new WeakReference<>(contraptionEntity));
		queued.clear();

		Collection<WeakReference<AbstractContraptionEntity>> values = map.values();
		for (Iterator<WeakReference<AbstractContraptionEntity>> iterator = values.iterator(); iterator.hasNext();) {
			WeakReference<AbstractContraptionEntity> weakReference = iterator.next();
			AbstractContraptionEntity contraptionEntity = weakReference.get();
			if (contraptionEntity == null || !contraptionEntity.isAliveOrStale()) {
				iterator.remove();
				continue;
			}
			if (!contraptionEntity.isAlive()) {
				contraptionEntity.staleTicks--;
				continue;
			}

			ContraptionCollider.collideEntities(contraptionEntity);
		}
	}

	public static void addSpawnedContraptionsToCollisionList(Entity entity, World world) {
		if (entity instanceof AbstractContraptionEntity)
			queuedAdditions.get(world)
				.add((AbstractContraptionEntity) entity);
	}

	public static void entitiesWhoJustDismountedGetSentToTheRightLocation(LivingEntity entityLiving, World world) {
		if (!world.isClient)
			return;

		NbtCompound data = entityLiving.getCustomData();
		if (!data.contains("ContraptionDismountLocation"))
			return;

		Vec3d position = VecHelper.readNBT(data.getList("ContraptionDismountLocation", NbtElement.DOUBLE_TYPE));
		if (entityLiving.getVehicle() == null)
			entityLiving.updatePositionAndAngles(position.x, position.y, position.z, entityLiving.getYaw(), entityLiving.getPitch());
		data.remove("ContraptionDismountLocation");
		entityLiving.setOnGround(false);
	}

}
