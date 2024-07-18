package com.simibubi.create.content.schematics.cannon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.google.common.collect.Sets;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MaterialChecklist {

	public static final int MAX_ENTRIES_PER_PAGE = 5;
	public static final int MAX_ENTRIES_PER_CLIPBOARD_PAGE = 7;

	public Object2IntMap<Item> gathered = new Object2IntArrayMap<>();
	public Object2IntMap<Item> required = new Object2IntArrayMap<>();
	public Object2IntMap<Item> damageRequired = new Object2IntArrayMap<>();
	public boolean blocksNotLoaded;

	public void warnBlockNotLoaded() {
		blocksNotLoaded = true;
	}

	public void require(ItemRequirement requirement) {
		if (requirement.isEmpty())
			return;
		if (requirement.isInvalid())
			return;

		for (ItemRequirement.StackRequirement stack : requirement.getRequiredItems()) {
			if (stack.usage == ItemUseType.DAMAGE)
				putOrIncrement(damageRequired, stack.stack);
			if (stack.usage == ItemUseType.CONSUME)
				putOrIncrement(required, stack.stack);
		}
	}

	private void putOrIncrement(Object2IntMap<Item> map, ItemStack stack) {
		Item item = stack.getItem();
		if (item == Items.AIR)
			return;
		if (map.containsKey(item))
			map.put(item, map.getInt(item) + stack.getCount());
		else
			map.put(item, stack.getCount());
	}

	public void collect(ItemStack stack) {
		Item item = stack.getItem();
		if (required.containsKey(item) || damageRequired.containsKey(item))
			if (gathered.containsKey(item))
				gathered.put(item, gathered.getInt(item) + stack.getCount());
			else
				gathered.put(item, stack.getCount());
	}

	public void collect(StorageView<ItemVariant> view) {
		if (view.isResourceBlank())
			return;
		int amount = TransferUtil.truncateLong(view.getAmount());
		ItemStack stack = view.getResource().toStack(amount);
		collect(stack);
	}

	public ItemStack createWrittenBook() {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

		NbtCompound tag = book.getOrCreateNbt();
		NbtList pages = new NbtList();

		int itemsWritten = 0;
		MutableText textComponent;

		if (blocksNotLoaded) {
			textComponent = Components.literal("\n" + Formatting.RED);
			textComponent = textComponent.append(Lang.translateDirect("materialChecklist.blocksNotLoaded"));
			pages.add(NbtString.of(Text.Serializer.toJson(textComponent)));
		}

		List<Item> keys = new ArrayList<>(Sets.union(required.keySet(), damageRequired.keySet()));
		Collections.sort(keys, (item1, item2) -> {
			Locale locale = Locale.ENGLISH;
			String name1 = item1.getName()
				.getString()
				.toLowerCase(locale);
			String name2 = item2.getName()
				.getString()
				.toLowerCase(locale);
			return name1.compareTo(name2);
		});

		textComponent = Components.empty();
		List<Item> completed = new ArrayList<>();
		for (Item item : keys) {
			int amount = getRequiredAmount(item);
			if (gathered.containsKey(item))
				amount -= gathered.getInt(item);

			if (amount <= 0) {
				completed.add(item);
				continue;
			}

			if (itemsWritten == MAX_ENTRIES_PER_PAGE) {
				itemsWritten = 0;
				textComponent.append(Components.literal("\n >>>")
					.formatted(Formatting.BLUE));
				pages.add(NbtString.of(Text.Serializer.toJson(textComponent)));
				textComponent = Components.empty();
			}

			itemsWritten++;
			textComponent.append(entry(new ItemStack(item), amount, true, true));
		}

		for (Item item : completed) {
			if (itemsWritten == MAX_ENTRIES_PER_PAGE) {
				itemsWritten = 0;
				textComponent.append(Components.literal("\n >>>")
					.formatted(Formatting.DARK_GREEN));
				pages.add(NbtString.of(Text.Serializer.toJson(textComponent)));
				textComponent = Components.empty();
			}

			itemsWritten++;
			textComponent.append(entry(new ItemStack(item), getRequiredAmount(item), false, true));
		}

		pages.add(NbtString.of(Text.Serializer.toJson(textComponent)));

		tag.put("pages", pages);
		tag.putBoolean("readonly", true);
		tag.putString("author", "Schematicannon");
		tag.putString("title", Formatting.BLUE + "Material Checklist");
		textComponent = Lang.translateDirect("materialChecklist")
			.setStyle(Style.EMPTY.withColor(Formatting.BLUE)
				.withItalic(Boolean.FALSE));
		book.getOrCreateSubNbt("display")
			.putString("Name", Text.Serializer.toJson(textComponent));
		book.setNbt(tag);

		return book;
	}

	public ItemStack createWrittenClipboard() {
		ItemStack clipboard = AllBlocks.CLIPBOARD.asStack();
		NbtCompound tag = clipboard.getOrCreateNbt();
		int itemsWritten = 0;

		List<List<ClipboardEntry>> pages = new ArrayList<>();
		List<ClipboardEntry> currentPage = new ArrayList<>();

		if (blocksNotLoaded) {
			currentPage.add(new ClipboardEntry(false, Lang.translateDirect("materialChecklist.blocksNotLoaded")
				.formatted(Formatting.RED)));
		}

		List<Item> keys = new ArrayList<>(Sets.union(required.keySet(), damageRequired.keySet()));
		Collections.sort(keys, (item1, item2) -> {
			Locale locale = Locale.ENGLISH;
			String name1 = item1.getName()
				.getString()
				.toLowerCase(locale);
			String name2 = item2.getName()
				.getString()
				.toLowerCase(locale);
			return name1.compareTo(name2);
		});

		List<Item> completed = new ArrayList<>();
		for (Item item : keys) {
			int amount = getRequiredAmount(item);
			if (gathered.containsKey(item))
				amount -= gathered.getInt(item);

			if (amount <= 0) {
				completed.add(item);
				continue;
			}

			if (itemsWritten == MAX_ENTRIES_PER_CLIPBOARD_PAGE) {
				itemsWritten = 0;
				currentPage.add(new ClipboardEntry(false, Components.literal(">>>")
					.formatted(Formatting.DARK_GRAY)));
				pages.add(currentPage);
				currentPage = new ArrayList<>();
			}

			itemsWritten++;
			currentPage.add(new ClipboardEntry(false, entry(new ItemStack(item), amount, true, false))
				.displayItem(new ItemStack(item)));
		}

		for (Item item : completed) {
			if (itemsWritten == MAX_ENTRIES_PER_CLIPBOARD_PAGE) {
				itemsWritten = 0;
				currentPage.add(new ClipboardEntry(true, Components.literal(">>>")
					.formatted(Formatting.DARK_GREEN)));
				pages.add(currentPage);
				currentPage = new ArrayList<>();
			}

			itemsWritten++;
			currentPage.add(new ClipboardEntry(true, entry(new ItemStack(item), getRequiredAmount(item), false, false))
				.displayItem(new ItemStack(item)));
		}

		pages.add(currentPage);
		ClipboardEntry.saveAll(pages, clipboard);
		ClipboardOverrides.switchTo(ClipboardType.WRITTEN, clipboard);
		clipboard.getOrCreateSubNbt("display")
			.putString("Name", Text.Serializer.toJson(Lang.translateDirect("materialChecklist")
				.setStyle(Style.EMPTY.withItalic(Boolean.FALSE))));
		tag.putBoolean("Readonly", true);
		clipboard.setNbt(tag);
		return clipboard;
	}

	public int getRequiredAmount(Item item) {
		int amount = required.getOrDefault(item, 0);
		if (damageRequired.containsKey(item))
			amount += Math.ceil(damageRequired.getInt(item) / (float) new ItemStack(item).getMaxDamage());
		return amount;
	}

	private MutableText entry(ItemStack item, int amount, boolean unfinished, boolean forBook) {
		int stacks = amount / 64;
		int remainder = amount % 64;
		MutableText tc = Components.empty();
		tc.append(Components.translatable(item.getTranslationKey())
			.setStyle(Style.EMPTY
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(item)))));

		if (!unfinished && forBook)
			tc.append(" \u2714");
		if (!unfinished || forBook)
			tc.formatted(unfinished ? Formatting.BLUE : Formatting.DARK_GREEN);
		return tc.append(Components.literal("\n" + " x" + amount)
			.formatted(Formatting.BLACK))
			.append(Components.literal(" | " + stacks + "\u25A4 +" + remainder + (forBook ? "\n" : ""))
				.formatted(Formatting.GRAY));
	}

}
