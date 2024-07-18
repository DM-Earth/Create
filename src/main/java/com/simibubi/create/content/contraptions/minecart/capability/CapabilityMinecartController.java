package com.simibubi.create.content.contraptions.minecart.capability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.minecart.CouplingHandler;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.WorldAttached;
import com.simibubi.create.foundation.utility.fabric.AbstractMinecartExtensions;

import io.github.fabricators_of_create.porting_lib.core.util.INBTSerializable;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

public class CapabilityMinecartController implements INBTSerializable<NbtCompound> {

	/* Global map of loaded carts */

	public static WorldAttached<Map<UUID, MinecartController>> loadedMinecartsByUUID;
	public static WorldAttached<Set<UUID>> loadedMinecartsWithCoupling;
	static WorldAttached<List<AbstractMinecartEntity>> queuedAdditions;
	static WorldAttached<List<UUID>> queuedUnloads;

// fabric: we just manually deal with removal to avoid the weirdness of adding 5 listeners but only ever calling accept on one.
//	/**
//	 * This callback wrapper ensures that the listeners map in the controller
//	 * capability only ever contains one instance
//	 */
//	public static class MinecartRemovalListener implements NonNullConsumer<LazyOptional<MinecartController>> {
//
//		private Level world;
//		private AbstractMinecart cart;
//
//		public MinecartRemovalListener(Level world, AbstractMinecart cart) {
//			this.world = world;
//			this.cart = cart;
//		}
//
//		@Override
//		public boolean equals(Object obj) {
//			return obj instanceof MinecartRemovalListener;
//		}
//
//		@Override
//		public int hashCode() {
//			return 100;
//		}
//
//		@Override
//		public void accept(LazyOptional<MinecartController> t) {
//			onCartRemoved(world, cart);
//		}
//
//	}

	static {
		loadedMinecartsByUUID = new WorldAttached<>($ -> new HashMap<>());
		loadedMinecartsWithCoupling = new WorldAttached<>($ -> new HashSet<>());
		queuedAdditions = new WorldAttached<>($ -> ObjectLists.synchronize(new ObjectArrayList<>()));
		queuedUnloads = new WorldAttached<>($ -> ObjectLists.synchronize(new ObjectArrayList<>()));
	}

	public static void tick(World world) {
		List<UUID> toRemove = new ArrayList<>();
		Map<UUID, MinecartController> carts = loadedMinecartsByUUID.get(world);
		List<AbstractMinecartEntity> queued = queuedAdditions.get(world);
		List<UUID> queuedRemovals = queuedUnloads.get(world);
		Set<UUID> cartsWithCoupling = loadedMinecartsWithCoupling.get(world);
		Set<UUID> keySet = carts.keySet();

		keySet.removeAll(queuedRemovals);
		cartsWithCoupling.removeAll(queuedRemovals);

		for (AbstractMinecartEntity cart : queued) {
			UUID uniqueID = cart.getUuid();

			if (world.isClient && carts.containsKey(uniqueID)) {
				MinecartController minecartController = carts.get(uniqueID);
				if (minecartController != null) {
					AbstractMinecartEntity minecartEntity = minecartController.cart();
					if (minecartEntity != null && minecartEntity.getId() != cart.getId())
						continue; // Away with you, Fake Entities!
				}
			}

			cartsWithCoupling.remove(uniqueID);

			MinecartController controller = cart.create$getController();
//			capability.addListener(new MinecartRemovalListener(world, cart)); // fabric: handled via AbstractMinecartMixin
			carts.put(uniqueID, controller);

			if (controller.isLeadingCoupling())
				cartsWithCoupling.add(uniqueID);
			if (!world.isClient && controller != null)
				controller.sendData();
		}

		queuedRemovals.clear();
		queued.clear();

		for (Entry<UUID, MinecartController> entry : carts.entrySet()) {
			MinecartController controller = entry.getValue();
			if (controller != null) {
				if (controller.isPresent()) {
					controller.tick();
					continue;
				}
			}
			toRemove.add(entry.getKey());
		}

		cartsWithCoupling.removeAll(toRemove);
		keySet.removeAll(toRemove);
	}

	public static void onChunkUnloaded(World world, WorldChunk chunk) {
		ChunkPos chunkPos = chunk
			.getPos();
		Map<UUID, MinecartController> carts = loadedMinecartsByUUID.get(world);
		for (MinecartController minecartController : carts.values()) {
			if (minecartController == null)
				continue;
			if (!minecartController.isPresent())
				continue;
			AbstractMinecartEntity cart = minecartController.cart();
			if (cart.getChunkPos()
				.equals(chunkPos))
				queuedUnloads.get(world)
					.add(cart.getUuid());
		}
	}

	public static void onCartRemoved(World world, AbstractMinecartEntity entity) {
		Map<UUID, MinecartController> carts = loadedMinecartsByUUID.get(world);
		List<UUID> unloads = queuedUnloads.get(world);
		UUID uniqueID = entity.getUuid();
		if (!carts.containsKey(uniqueID) || unloads.contains(uniqueID))
			return;
		if (world.isClient)
			return;
		handleKilledMinecart(world, carts.get(uniqueID), entity.getPos());
	}

	protected static void handleKilledMinecart(World world, MinecartController controller, Vec3d removedPos) {
		if (controller == null)
			return;
		for (boolean forward : Iterate.trueAndFalse) {
			MinecartController next = CouplingHandler.getNextInCouplingChain(world, controller, forward);
			if (next == null || next == MinecartController.EMPTY)
				continue;

			next.removeConnection(!forward);
			if (controller.hasContraptionCoupling(forward))
				continue;
			AbstractMinecartEntity cart = next.cart();
			if (cart == null)
				continue;

			Vec3d itemPos = cart.getPos()
				.add(removedPos)
				.multiply(.5f);
			ItemEntity itemEntity =
				new ItemEntity(world, itemPos.x, itemPos.y, itemPos.z, AllItems.MINECART_COUPLING.asStack());
			itemEntity.setToDefaultPickupDelay();
			world.spawnEntity(itemEntity);
		}
	}

	@Nullable
	public static MinecartController getIfPresent(World world, UUID cartId) {
		Map<UUID, MinecartController> carts = loadedMinecartsByUUID.get(world);
		if (carts == null)
			return null;
		if (!carts.containsKey(cartId))
			return null;
		return carts.get(cartId);
	}

	public static void attach(AbstractMinecartEntity entity) {
		queuedAdditions.get(entity.getEntityWorld())
			.add(entity);
	}

	public static void startTracking(Entity entity) {
		if (!(entity instanceof AbstractMinecartEntity cart))
			return;
		cart.create$getController().sendData();
	}
}
