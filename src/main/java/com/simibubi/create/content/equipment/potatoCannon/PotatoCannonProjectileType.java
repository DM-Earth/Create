package com.simibubi.create.content.equipment.potatoCannon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.WorldAccess;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.simibubi.create.foundation.utility.RegisteredObjects;

public class PotatoCannonProjectileType {

	private List<Supplier<Item>> items = new ArrayList<>();

	private int reloadTicks = 10;
	private int damage = 1;
	private int split = 1;
	private float knockback = 1;
	private float drag = 0.99f;
	private float velocityMultiplier = 1;
	private float gravityMultiplier = 1;
	private float soundPitch = 1;
	private boolean sticky = false;
	private PotatoProjectileRenderMode renderMode = PotatoProjectileRenderMode.Billboard.INSTANCE;

	private Predicate<EntityHitResult> preEntityHit = e -> false; // True if hit should be canceled
	private Predicate<EntityHitResult> onEntityHit = e -> false; // True if shouldn't recover projectile
	private BiPredicate<WorldAccess, BlockHitResult> onBlockHit = (w, ray) -> false;

	protected PotatoCannonProjectileType() {
	}

	public List<Supplier<Item>> getItems() {
		return items;
	}

	public int getReloadTicks() {
		return reloadTicks;
	}

	public int getDamage() {
		return damage;
	}

	public int getSplit() {
		return split;
	}

	public float getKnockback() {
		return knockback;
	}

	public float getDrag() {
		return drag;
	}

	public float getVelocityMultiplier() {
		return velocityMultiplier;
	}

	public float getGravityMultiplier() {
		return gravityMultiplier;
	}

	public float getSoundPitch() {
		return soundPitch;
	}

	public boolean isSticky() {
		return sticky;
	}

	public PotatoProjectileRenderMode getRenderMode() {
		return renderMode;
	}

	public boolean preEntityHit(EntityHitResult ray) {
		return preEntityHit.test(ray);
	}

	public boolean onEntityHit(EntityHitResult ray) {
		return onEntityHit.test(ray);
	}

	public boolean onBlockHit(WorldAccess world, BlockHitResult ray) {
		return onBlockHit.test(world, ray);
	}

	public static PotatoCannonProjectileType fromJson(JsonObject object) {
		PotatoCannonProjectileType type = new PotatoCannonProjectileType();
		try {
			JsonElement itemsElement = object.get("items");
			if (itemsElement != null && itemsElement.isJsonArray()) {
				for (JsonElement element : itemsElement.getAsJsonArray()) {
					if (element.isJsonPrimitive()) {
						JsonPrimitive primitive = element.getAsJsonPrimitive();
						if (primitive.isString()) {
							try {
								Registries.ITEM.getOrEmpty(new Identifier(primitive.getAsString()))
										.ifPresent(item -> type.items.add(() -> item));
							} catch (InvalidIdentifierException e) {
								//
							}
						}
					}
				}
			}

			parseJsonPrimitive(object, "reload_ticks", JsonPrimitive::isNumber, primitive -> type.reloadTicks = primitive.getAsInt());
			parseJsonPrimitive(object, "damage", JsonPrimitive::isNumber, primitive -> type.damage = primitive.getAsInt());
			parseJsonPrimitive(object, "split", JsonPrimitive::isNumber, primitive -> type.split = primitive.getAsInt());
			parseJsonPrimitive(object, "knockback", JsonPrimitive::isNumber, primitive -> type.knockback = primitive.getAsFloat());
			parseJsonPrimitive(object, "drag", JsonPrimitive::isNumber, primitive -> type.drag = primitive.getAsFloat());
			parseJsonPrimitive(object, "velocity_multiplier", JsonPrimitive::isNumber, primitive -> type.velocityMultiplier = primitive.getAsFloat());
			parseJsonPrimitive(object, "gravity_multiplier", JsonPrimitive::isNumber, primitive -> type.gravityMultiplier = primitive.getAsFloat());
			parseJsonPrimitive(object, "sound_pitch", JsonPrimitive::isNumber, primitive -> type.soundPitch = primitive.getAsFloat());
			parseJsonPrimitive(object, "sticky", JsonPrimitive::isBoolean, primitive -> type.sticky = primitive.getAsBoolean());
		} catch (Exception e) {
			//
		}
		return type;
	}

	private static void parseJsonPrimitive(JsonObject object, String key, Predicate<JsonPrimitive> predicate, Consumer<JsonPrimitive> consumer) {
		JsonElement element = object.get(key);
		if (element != null && element.isJsonPrimitive()) {
			JsonPrimitive primitive = element.getAsJsonPrimitive();
			if (predicate.test(primitive)) {
				consumer.accept(primitive);
			}
		}
	}

	public static void toBuffer(PotatoCannonProjectileType type, PacketByteBuf buffer) {
		buffer.writeVarInt(type.items.size());
		for (Supplier<Item> delegate : type.items) {
			buffer.writeIdentifier(RegisteredObjects.getKeyOrThrow(delegate.get()));
		}
		buffer.writeInt(type.reloadTicks);
		buffer.writeInt(type.damage);
		buffer.writeInt(type.split);
		buffer.writeFloat(type.knockback);
		buffer.writeFloat(type.drag);
		buffer.writeFloat(type.velocityMultiplier);
		buffer.writeFloat(type.gravityMultiplier);
		buffer.writeFloat(type.soundPitch);
		buffer.writeBoolean(type.sticky);
	}

	public static PotatoCannonProjectileType fromBuffer(PacketByteBuf buffer) {
		PotatoCannonProjectileType type = new PotatoCannonProjectileType();
		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++) {
			Item item = Registries.ITEM.get(buffer.readIdentifier());
			if (item != null) {
				type.items.add(() -> item);
			}
		}
		type.reloadTicks = buffer.readInt();
		type.damage = buffer.readInt();
		type.split = buffer.readInt();
		type.knockback = buffer.readFloat();
		type.drag = buffer.readFloat();
		type.velocityMultiplier = buffer.readFloat();
		type.gravityMultiplier = buffer.readFloat();
		type.soundPitch = buffer.readFloat();
		type.sticky = buffer.readBoolean();
		return type;
	}

	public static class Builder {

		protected Identifier id;
		protected PotatoCannonProjectileType result;

		public Builder(Identifier id) {
			this.id = id;
			this.result = new PotatoCannonProjectileType();
		}

		public Builder reloadTicks(int reload) {
			result.reloadTicks = reload;
			return this;
		}

		public Builder damage(int damage) {
			result.damage = damage;
			return this;
		}

		public Builder splitInto(int split) {
			result.split = split;
			return this;
		}

		public Builder knockback(float knockback) {
			result.knockback = knockback;
			return this;
		}

		public Builder drag(float drag) {
			result.drag = drag;
			return this;
		}

		public Builder velocity(float velocity) {
			result.velocityMultiplier = velocity;
			return this;
		}

		public Builder gravity(float modifier) {
			result.gravityMultiplier = modifier;
			return this;
		}

		public Builder soundPitch(float pitch) {
			result.soundPitch = pitch;
			return this;
		}

		public Builder sticky() {
			result.sticky = true;
			return this;
		}

		public Builder renderMode(PotatoProjectileRenderMode renderMode) {
			result.renderMode = renderMode;
			return this;
		}

		public Builder renderBillboard() {
			renderMode(PotatoProjectileRenderMode.Billboard.INSTANCE);
			return this;
		}

		public Builder renderTumbling() {
			renderMode(PotatoProjectileRenderMode.Tumble.INSTANCE);
			return this;
		}

		public Builder renderTowardMotion(int spriteAngle, float spin) {
			renderMode(new PotatoProjectileRenderMode.TowardMotion(spriteAngle, spin));
			return this;
		}

		public Builder preEntityHit(Predicate<EntityHitResult> callback) {
			result.preEntityHit = callback;
			return this;
		}

		public Builder onEntityHit(Predicate<EntityHitResult> callback) {
			result.onEntityHit = callback;
			return this;
		}

		public Builder onBlockHit(BiPredicate<WorldAccess, BlockHitResult> callback) {
			result.onBlockHit = callback;
			return this;
		}

		public Builder addItems(ItemConvertible... items) {
			for (ItemConvertible provider : items)
				result.items.add(provider::asItem);
			return this;
		}

		public PotatoCannonProjectileType register() {
			PotatoProjectileTypeManager.registerBuiltinType(id, result);
			return result;
		}

		public PotatoCannonProjectileType registerAndAssign(ItemConvertible... items) {
			addItems(items);
			register();
			return result;
		}

	}

}
