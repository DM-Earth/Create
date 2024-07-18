package com.simibubi.create.content.trains.schedule.condition;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.Contraption.ContraptionInvWrapper;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ItemThresholdCondition extends CargoThresholdCondition {

	private FilterItemStack stack = FilterItemStack.empty();

	@Override
	protected Text getUnit() {
		return Components.literal(inStacks() ? "\u25A4" : "");
	}

	@Override
	protected ItemStack getIcon() {
		return stack.item();
	}

	@Override
	protected boolean test(World level, Train train, NbtCompound context) {
		Ops operator = getOperator();
		long target = getThreshold();
		boolean stacks = inStacks();

		int foundItems = 0;
		for (Carriage carriage : train.carriages) {
			ContraptionInvWrapper items = carriage.storage.getItems();
			try (Transaction t = TransferUtil.getTransaction()) {
				for (StorageView<ItemVariant> view : items.nonEmptyViews()) {
					ItemVariant variant = view.getResource();
					if (!stack.test(level, variant.toStack()))
						continue;

					if (stacks)
						foundItems += view.getAmount() == variant.getItem().getMaxCount() ? 1 : 0;
					else
						foundItems += view.getAmount();
				}
			}
		}

		requestStatusToUpdate(foundItems, context);
		return operator.test(foundItems, target);
	}

	@Override
	protected void writeAdditional(NbtCompound tag) {
		super.writeAdditional(tag);
		tag.put("Item", stack.serializeNBT());
	}

	@Override
	protected void readAdditional(NbtCompound tag) {
		super.readAdditional(tag);
		if (tag.contains("Item"))
			stack = FilterItemStack.of(tag.getCompound("Item"));
	}

	@Override
	public boolean tickCompletion(World level, Train train, NbtCompound context) {
		return super.tickCompletion(level, train, context);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		this.stack = FilterItemStack.of(stack);
	}

	@Override
	public ItemStack getItem(int slot) {
		return stack.item();
	}

	@Override
	public List<Text> getTitleAs(String type) {
		return ImmutableList.of(
			Lang.translateDirect("schedule.condition.threshold.train_holds",
				Lang.translateDirect("schedule.condition.threshold." + Lang.asId(getOperator().name()))),
			Lang.translateDirect("schedule.condition.threshold.x_units_of_item", getThreshold(),
				Lang.translateDirect("schedule.condition.threshold." + (inStacks() ? "stacks" : "items")),
				stack.isEmpty() ? Lang.translateDirect("schedule.condition.threshold.anything")
					: stack.isFilterItem() ? Lang.translateDirect("schedule.condition.threshold.matching_content")
						: stack.item()
							.getName())
				.formatted(Formatting.DARK_AQUA));
	}

	private boolean inStacks() {
		return intData("Measure") == 1;
	}

	@Override
	public Identifier getId() {
		return Create.asResource("item_threshold");
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		super.initConfigurationWidgets(builder);
		builder.addSelectionScrollInput(71, 50, (i, l) -> {
			i.forOptions(ImmutableList.of(Lang.translateDirect("schedule.condition.threshold.items"),
				Lang.translateDirect("schedule.condition.threshold.stacks")))
				.titled(Lang.translateDirect("schedule.condition.threshold.item_measure"));
		}, "Measure");
	}

	@Override
	public MutableText getWaitingStatus(World level, Train train, NbtCompound tag) {
		long lastDisplaySnapshot = getLastDisplaySnapshot(tag);
		if (lastDisplaySnapshot == -1)
			return Components.empty();
		int offset = getOperator() == Ops.LESS ? -1 : getOperator() == Ops.GREATER ? 1 : 0;
		return Lang.translateDirect("schedule.condition.threshold.status", lastDisplaySnapshot,
			Math.max(0, getThreshold() + offset),
			Lang.translateDirect("schedule.condition.threshold." + (inStacks() ? "stacks" : "items")));
	}
}
