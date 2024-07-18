package com.simibubi.create.content.equipment.toolbox;

import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.ISyncPersistentData.PersistentDataPacket;
import com.simibubi.create.foundation.utility.WorldAttached;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class ToolboxHandler {

	public static final WorldAttached<WeakHashMap<BlockPos, ToolboxBlockEntity>> toolboxes =
		new WorldAttached<>(w -> new WeakHashMap<>());

	public static void onLoad(ToolboxBlockEntity be) {
		toolboxes.get(be.getWorld())
			.put(be.getPos(), be);
	}

	public static void onUnload(ToolboxBlockEntity be) {
		toolboxes.get(be.getWorld())
			.remove(be.getPos());
	}

	static int validationTimer = 20;

	public static void entityTick(Entity entity, World world) {
		if (world.isClient)
			return;
		if (!(world instanceof ServerWorld))
			return;
		if (!(entity instanceof ServerPlayerEntity))
			return;
		if (entity.age % validationTimer != 0)
			return;

		ServerPlayerEntity player = (ServerPlayerEntity) entity;
		if (!player.getCustomData()
			.contains("CreateToolboxData"))
			return;

		boolean sendData = false;
		NbtCompound compound = player.getCustomData()
			.getCompound("CreateToolboxData");
		for (int i = 0; i < 9; i++) {
			String key = String.valueOf(i);
			if (!compound.contains(key))
				continue;

			NbtCompound data = compound.getCompound(key);
			BlockPos pos = NbtHelper.toBlockPos(data.getCompound("Pos"));
			int slot = data.getInt("Slot");

			if (!world.canSetBlock(pos))
				continue;
			if (!(world.getBlockState(pos)
				.getBlock() instanceof ToolboxBlock)) {
				compound.remove(key);
				sendData = true;
				continue;
			}

			BlockEntity prevBlockEntity = world.getBlockEntity(pos);
			if (prevBlockEntity instanceof ToolboxBlockEntity)
				((ToolboxBlockEntity) prevBlockEntity).connectPlayer(slot, player, i);
		}

		if (sendData)
			syncData(player);
	}

	public static void playerLogin(PlayerEntity player) {
		if (!(player instanceof ServerPlayerEntity))
			return;
		if (player.getCustomData()
			.contains("CreateToolboxData")
			&& !player.getCustomData()
				.getCompound("CreateToolboxData")
				.isEmpty()) {
			syncData(player);
		}
	}

	public static void syncData(PlayerEntity player) {
		AllPackets.getChannel().sendToClient(new PersistentDataPacket(player), (ServerPlayerEntity) player);
	}

	public static List<ToolboxBlockEntity> getNearest(WorldAccess world, PlayerEntity player, int maxAmount) {
		Vec3d location = player.getPos();
		double maxRange = getMaxRange(player);
		return toolboxes.get(world)
			.keySet()
			.stream()
			.filter(p -> distance(location, p) < maxRange * maxRange)
			.sorted((p1, p2) -> Double.compare(distance(location, p1), distance(location, p2)))
			.limit(maxAmount)
			.map(toolboxes.get(world)::get)
			.filter(ToolboxBlockEntity::isFullyInitialized)
			.collect(Collectors.toList());
	}

	public static void unequip(PlayerEntity player, int hotbarSlot, boolean keepItems) {
		NbtCompound compound = player.getCustomData()
			.getCompound("CreateToolboxData");
		World world = player.getWorld();
		String key = String.valueOf(hotbarSlot);
		if (!compound.contains(key))
			return;

		NbtCompound prevData = compound.getCompound(key);
		BlockPos prevPos = NbtHelper.toBlockPos(prevData.getCompound("Pos"));
		int prevSlot = prevData.getInt("Slot");

		BlockEntity prevBlockEntity = world.getBlockEntity(prevPos);
		if (prevBlockEntity instanceof ToolboxBlockEntity) {
			ToolboxBlockEntity toolbox = (ToolboxBlockEntity) prevBlockEntity;
			toolbox.unequip(prevSlot, player, hotbarSlot, keepItems || !ToolboxHandler.withinRange(player, toolbox));
		}
		compound.remove(key);
	}

	public static boolean withinRange(PlayerEntity player, ToolboxBlockEntity box) {
		if (player.getWorld() != box.getWorld())
			return false;
		double maxRange = getMaxRange(player);
		return distance(player.getPos(), box.getPos()) < maxRange * maxRange;
	}

	public static double distance(Vec3d location, BlockPos p) {
		return location.squaredDistanceTo(p.getX() + 0.5f, p.getY(), p.getZ() + 0.5f);
	}

	public static double getMaxRange(PlayerEntity player) {
		return AllConfigs.server().equipment.toolboxRange.get()
			.doubleValue();
	}

}
