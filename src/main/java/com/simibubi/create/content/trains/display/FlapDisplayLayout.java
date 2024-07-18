package com.simibubi.create.content.trains.display;

import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.apache.commons.lang3.mutable.MutableInt;

import com.simibubi.create.foundation.utility.NBTHelper;

public class FlapDisplayLayout {

	List<FlapDisplaySection> sections;
	String layoutKey;

	public FlapDisplayLayout(int maxCharCount) {
		loadDefault(maxCharCount);
	}

	public void loadDefault(int maxCharCount) {
		configure("Default", Arrays
			.asList(new FlapDisplaySection(maxCharCount * FlapDisplaySection.MONOSPACE, "alphabet", false, false)));
	}

	public boolean isLayout(String key) {
		return layoutKey.equals(key);
	}

	public void configure(String layoutKey, List<FlapDisplaySection> sections) {
		this.layoutKey = layoutKey;
		this.sections = sections;
	}

	public NbtCompound write() {
		NbtCompound tag = new NbtCompound();
		tag.putString("Key", layoutKey);
		tag.put("Sections", NBTHelper.writeCompoundList(sections, FlapDisplaySection::write));
		return tag;
	};

	public void read(NbtCompound tag) {
		String prevKey = layoutKey;
		layoutKey = tag.getString("Key");
		NbtList sectionsTag = tag.getList("Sections", NbtElement.COMPOUND_TYPE);

		if (!prevKey.equals(layoutKey)) {
			sections = NBTHelper.readCompoundList(sectionsTag, FlapDisplaySection::load);
			return;
		}

		MutableInt index = new MutableInt(0);
		NBTHelper.iterateCompoundList(sectionsTag, nbt -> sections.get(index.getAndIncrement())
			.update(nbt));
	}

	public List<FlapDisplaySection> getSections() {
		return sections;
	}

}
