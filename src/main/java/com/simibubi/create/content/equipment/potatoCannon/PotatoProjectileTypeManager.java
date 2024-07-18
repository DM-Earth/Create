package com.simibubi.create.content.equipment.potatoCannon;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

public class PotatoProjectileTypeManager {

	private static final Map<Identifier, PotatoCannonProjectileType> BUILTIN_TYPE_MAP = new HashMap<>();
	private static final Map<Identifier, PotatoCannonProjectileType> CUSTOM_TYPE_MAP = new HashMap<>();
	private static final Map<Item, PotatoCannonProjectileType> ITEM_TO_TYPE_MAP = new IdentityHashMap<>();

	public static void registerBuiltinType(Identifier id, PotatoCannonProjectileType type) {
		synchronized (BUILTIN_TYPE_MAP) {
			BUILTIN_TYPE_MAP.put(id, type);
		}
	}

	public static PotatoCannonProjectileType getBuiltinType(Identifier id) {
		return BUILTIN_TYPE_MAP.get(id);
	}

	public static PotatoCannonProjectileType getCustomType(Identifier id) {
		return CUSTOM_TYPE_MAP.get(id);
	}

	public static PotatoCannonProjectileType getTypeForItem(Item item) {
		return ITEM_TO_TYPE_MAP.get(item);
	}

	public static Optional<PotatoCannonProjectileType> getTypeForStack(ItemStack item) {
		if (item.isEmpty())
			return Optional.empty();
		return Optional.ofNullable(getTypeForItem(item.getItem()));
	}

	public static void clear() {
		CUSTOM_TYPE_MAP.clear();
		ITEM_TO_TYPE_MAP.clear();
	}

	public static void fillItemMap() {
		for (Map.Entry<Identifier, PotatoCannonProjectileType> entry : BUILTIN_TYPE_MAP.entrySet()) {
			PotatoCannonProjectileType type = entry.getValue();
			for (Supplier<Item> delegate : type.getItems()) {
				ITEM_TO_TYPE_MAP.put(delegate.get(), type);
			}
		}
		for (Map.Entry<Identifier, PotatoCannonProjectileType> entry : CUSTOM_TYPE_MAP.entrySet()) {
			PotatoCannonProjectileType type = entry.getValue();
			for (Supplier<Item> delegate : type.getItems()) {
				ITEM_TO_TYPE_MAP.put(delegate.get(), type);
			}
		}
		ITEM_TO_TYPE_MAP.remove(AllItems.POTATO_CANNON.get());
	}

	public static void toBuffer(PacketByteBuf buffer) {
		buffer.writeVarInt(CUSTOM_TYPE_MAP.size());
		for (Map.Entry<Identifier, PotatoCannonProjectileType> entry : CUSTOM_TYPE_MAP.entrySet()) {
			buffer.writeIdentifier(entry.getKey());
			PotatoCannonProjectileType.toBuffer(entry.getValue(), buffer);
		}
	}

	public static void fromBuffer(PacketByteBuf buffer) {
		clear();

		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++) {
			CUSTOM_TYPE_MAP.put(buffer.readIdentifier(), PotatoCannonProjectileType.fromBuffer(buffer));
		}

		fillItemMap();
	}

	public static void syncTo(ServerPlayerEntity player) {
		AllPackets.getChannel().sendToClient(new SyncPacket(), player);
	}

	public static void syncToAll(List<ServerPlayerEntity> players) {
		AllPackets.getChannel().sendToClients(new SyncPacket(), players);
	}

	public static class ReloadListener extends JsonDataLoader implements IdentifiableResourceReloadListener {

		private static final Gson GSON = new Gson();

		public static final ReloadListener INSTANCE = new ReloadListener();

		protected ReloadListener() {
			super(GSON, "potato_cannon_projectile_types");
		}

		@Override
		protected void apply(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler) {
			clear();

			for (Map.Entry<Identifier, JsonElement> entry : map.entrySet()) {
				JsonElement element = entry.getValue();
				if (element.isJsonObject()) {
					Identifier id = entry.getKey();
					JsonObject object = element.getAsJsonObject();
					PotatoCannonProjectileType type = PotatoCannonProjectileType.fromJson(object);
					CUSTOM_TYPE_MAP.put(id, type);
				}
			}

			fillItemMap();
		}

		@Override
		public Identifier getFabricId() {
			return Create.asResource("potato_cannon_projectile_types");
		}
	}

	public static class SyncPacket extends SimplePacketBase {

		private PacketByteBuf buffer;

		public SyncPacket() {
		}

		public SyncPacket(PacketByteBuf buffer) {
			this.buffer = buffer;
		}

		@Override
		public void write(PacketByteBuf buffer) {
			toBuffer(buffer);
		}

		@Override
		public boolean handle(Context context) {
			buffer.retain();
			context.enqueueWork(() -> {
				try {
					fromBuffer(buffer);
				} finally {
					buffer.release();
				}
			});
			return true;
		}

	}

}
