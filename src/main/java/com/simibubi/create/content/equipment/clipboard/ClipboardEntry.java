package com.simibubi.create.content.equipment.clipboard;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import com.simibubi.create.foundation.utility.NBTHelper;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;

public class ClipboardEntry {

	public boolean checked;
	public MutableText text;
	public ItemStack icon;

	public ClipboardEntry(boolean checked, MutableText text) {
		this.checked = checked;
		this.text = text;
		this.icon = ItemStack.EMPTY;
	}

	public ClipboardEntry displayItem(ItemStack icon) {
		this.icon = icon;
		return this;
	}

	public static List<List<ClipboardEntry>> readAll(ItemStack clipboardItem) {
		NbtCompound tag = clipboardItem.getNbt();
		if (tag == null)
			return new ArrayList<>();
		return NBTHelper.readCompoundList(tag.getList("Pages", NbtElement.COMPOUND_TYPE), pageTag -> NBTHelper
			.readCompoundList(pageTag.getList("Entries", NbtElement.COMPOUND_TYPE), ClipboardEntry::readNBT));
	}

	public static List<ClipboardEntry> getLastViewedEntries(ItemStack heldItem) {
		List<List<ClipboardEntry>> pages = ClipboardEntry.readAll(heldItem);
		if (pages.isEmpty())
			return new ArrayList<>();
		int page = heldItem.getNbt() == null ? 0
			: Math.min(heldItem.getNbt()
				.getInt("PreviouslyOpenedPage"), pages.size() - 1);
		List<ClipboardEntry> entries = pages.get(page);
		return entries;
	}

	public static void saveAll(List<List<ClipboardEntry>> entries, ItemStack clipboardItem) {
		NbtCompound tag = clipboardItem.getOrCreateNbt();
		tag.put("Pages", NBTHelper.writeCompoundList(entries, list -> {
			NbtCompound pageTag = new NbtCompound();
			pageTag.put("Entries", NBTHelper.writeCompoundList(list, ClipboardEntry::writeNBT));
			return pageTag;
		}));
	}

	public NbtCompound writeNBT() {
		NbtCompound nbt = new NbtCompound();
		nbt.putBoolean("Checked", checked);
		nbt.putString("Text", Text.Serializer.toJson(text));
		if (icon.isEmpty())
			return nbt;
		nbt.put("Icon", NBTSerializer.serializeNBT(icon));
		return nbt;
	}

	public static ClipboardEntry readNBT(NbtCompound tag) {
		ClipboardEntry clipboardEntry =
			new ClipboardEntry(tag.getBoolean("Checked"), Text.Serializer.fromJson(tag.getString("Text")));
		if (tag.contains("Icon"))
			clipboardEntry.displayItem(ItemStack.fromNbt(tag.getCompound("Icon")));
		return clipboardEntry;
	}

}
