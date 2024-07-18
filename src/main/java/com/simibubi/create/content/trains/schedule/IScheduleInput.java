package com.simibubi.create.content.trains.schedule;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public interface IScheduleInput {

	public abstract Pair<ItemStack, Text> getSummary();

	public abstract Identifier getId();

	public abstract NbtCompound getData();

	public abstract void setData(NbtCompound data);

	public default int slotsTargeted() {
		return 0;
	}

	public default List<Text> getTitleAs(String type) {
		Identifier id = getId();
		return ImmutableList
			.of(Components.translatable(id.getNamespace() + ".schedule." + type + "." + id.getPath()));
	}

	public default ItemStack getSecondLineIcon() {
		return ItemStack.EMPTY;
	}

	public default void setItem(int slot, ItemStack stack) {}

	public default ItemStack getItem(int slot) {
		return ItemStack.EMPTY;
	}

	@Nullable
	public default List<Text> getSecondLineTooltip(int slot) {
		return null;
	}

	@Environment(EnvType.CLIENT)
	public default void initConfigurationWidgets(ModularGuiLineBuilder builder) {};

	@Environment(EnvType.CLIENT)
	public default boolean renderSpecialIcon(DrawContext graphics, int x, int y) {
		return false;
	}

}
