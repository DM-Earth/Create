package com.simibubi.create.foundation.utility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import com.simibubi.create.AllBlockEntityTypes;

public final class NBTProcessors {

	private static final Map<BlockEntityType<?>, UnaryOperator<NbtCompound>> processors = new HashMap<>();
	private static final Map<BlockEntityType<?>, UnaryOperator<NbtCompound>> survivalProcessors = new HashMap<>();

	public static synchronized void addProcessor(BlockEntityType<?> type, UnaryOperator<NbtCompound> processor) {
		processors.put(type, processor);
	}

	public static synchronized void addSurvivalProcessor(BlockEntityType<?> type,
		UnaryOperator<NbtCompound> processor) {
		survivalProcessors.put(type, processor);
	}

	static {
		String[] signSides = new String[] { "front_text", "back_text" };
		UnaryOperator<NbtCompound> signProcessor = data -> {
			for (String side : signSides) {
				if (data.contains(side, NbtElement.COMPOUND_TYPE)) {
					NbtCompound sideData = data.getCompound(side);
					if (sideData.contains("messages", NbtElement.LIST_TYPE)) {
						NbtList messages = sideData.getList("messages", NbtElement.STRING_TYPE);
						for (int i = 0; i < messages.size(); i++) {
							String string = messages.getString(i);
							if (textComponentHasClickEvent(string)) {
								return null;
							}
						}
					}
				}
			}
			return data;
		};
		addProcessor(BlockEntityType.SIGN, signProcessor);
		addProcessor(BlockEntityType.HANGING_SIGN, signProcessor);

		addProcessor(BlockEntityType.LECTERN, data -> {
			if (!data.contains("Book", NbtElement.COMPOUND_TYPE))
				return data;
			NbtCompound book = data.getCompound("Book");

			if (!book.contains("tag", NbtElement.COMPOUND_TYPE))
				return data;
			NbtCompound tag = book.getCompound("tag");

			if (!tag.contains("pages", NbtElement.LIST_TYPE))
				return data;
			NbtList pages = tag.getList("pages", NbtElement.STRING_TYPE);

			for (NbtElement inbt : pages) {
				if (textComponentHasClickEvent(inbt.asString()))
					return null;
			}
			return data;
		});
		addProcessor(AllBlockEntityTypes.CREATIVE_CRATE.get(), itemProcessor("Filter"));
		addProcessor(AllBlockEntityTypes.PLACARD.get(), itemProcessor("Item"));
	}

	public static UnaryOperator<NbtCompound> itemProcessor(String tagKey) {
		return data -> {
			NbtCompound compound = data.getCompound(tagKey);
			if (!compound.contains("tag", 10))
				return data;
			NbtCompound itemTag = compound.getCompound("tag");
			if (itemTag == null)
				return data;
			HashSet<String> keys = new HashSet<>(itemTag.getKeys());
			for (String key : keys)
				if (isUnsafeItemNBTKey(key))
					itemTag.remove(key);
			if (itemTag.isEmpty())
				compound.remove("tag");
			return data;
		};
	}

	public static ItemStack withUnsafeNBTDiscarded(ItemStack stack) {
		if (stack.getNbt() == null)
			return stack;
		ItemStack copy = stack.copy();
		stack.getNbt()
			.getKeys()
			.stream()
			.filter(NBTProcessors::isUnsafeItemNBTKey)
			.forEach(copy::removeSubNbt);
		return copy;
	}

	public static boolean isUnsafeItemNBTKey(String name) {
		if (name.equals(EnchantedBookItem.STORED_ENCHANTMENTS_KEY))
			return false;
		if (name.equals("Enchantments"))
			return false;
		if (name.contains("Potion"))
			return false;
		if (name.contains("Damage"))
			return false;
		if (name.equals("display"))
			return false;
		return true;
	}

	public static boolean textComponentHasClickEvent(String json) {
		Text component = Text.Serializer.fromJson(json.isEmpty() ? "\"\"" : json);
		return component != null && component.getStyle() != null && component.getStyle()
			.getClickEvent() != null;
	}

	private NBTProcessors() {}

	@Nullable
	public static NbtCompound process(BlockEntity blockEntity, NbtCompound compound, boolean survival) {
		if (compound == null)
			return null;
		BlockEntityType<?> type = blockEntity.getType();
		if (survival && survivalProcessors.containsKey(type))
			compound = survivalProcessors.get(type)
				.apply(compound);
		if (compound != null && processors.containsKey(type))
			return processors.get(type)
				.apply(compound);
		if (blockEntity instanceof MobSpawnerBlockEntity)
			return compound;
		if (blockEntity.copyItemDataRequiresOperator())
			return null;
		return compound;
	}

}
