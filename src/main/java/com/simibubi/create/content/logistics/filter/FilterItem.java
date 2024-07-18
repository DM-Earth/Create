package com.simibubi.create.content.logistics.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.logistics.filter.AttributeFilterMenu.WhitelistMode;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class FilterItem extends Item implements NamedScreenHandlerFactory {

	private FilterType type;

	private enum FilterType {
		REGULAR, ATTRIBUTE;
	}

	public static FilterItem regular(Settings properties) {
		return new FilterItem(FilterType.REGULAR, properties);
	}

	public static FilterItem attribute(Settings properties) {
		return new FilterItem(FilterType.ATTRIBUTE, properties);
	}

	private FilterItem(FilterType type, Settings properties) {
		super(properties);
		this.type = type;
	}

	@Nonnull
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (context.getPlayer() == null)
			return ActionResult.PASS;
		return use(context.getWorld(), context.getPlayer(), context.getHand()).getResult();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendTooltip(ItemStack stack, World worldIn, List<Text> tooltip, TooltipContext flagIn) {
		if (!AllKeys.shiftDown()) {
			List<Text> makeSummary = makeSummary(stack);
			if (makeSummary.isEmpty())
				return;
			tooltip.add(Components.literal(" "));
			tooltip.addAll(makeSummary);
		}
	}

	private List<Text> makeSummary(ItemStack filter) {
		List<Text> list = new ArrayList<>();
		if (!filter.hasNbt())
			return list;

		if (type == FilterType.REGULAR) {
			ItemStackHandler filterItems = getFilterItems(filter);
			boolean blacklist = filter.getOrCreateNbt()
				.getBoolean("Blacklist");

			list.add((blacklist ? Lang.translateDirect("gui.filter.deny_list")
				: Lang.translateDirect("gui.filter.allow_list")).formatted(Formatting.GOLD));
			int count = 0;
			for (int i = 0; i < filterItems.getSlotCount(); i++) {
				if (count > 3) {
					list.add(Components.literal("- ...")
						.formatted(Formatting.DARK_GRAY));
					break;
				}

				ItemStack filterStack = filterItems.getStackInSlot(i);
				if (filterStack.isEmpty())
					continue;
				list.add(Components.literal("- ")
					.append(filterStack.getName())
					.formatted(Formatting.GRAY));
				count++;
			}

			if (count == 0)
				return Collections.emptyList();
		}

		if (type == FilterType.ATTRIBUTE) {
			WhitelistMode whitelistMode = WhitelistMode.values()[filter.getOrCreateNbt()
				.getInt("WhitelistMode")];
			list.add((whitelistMode == WhitelistMode.WHITELIST_CONJ
				? Lang.translateDirect("gui.attribute_filter.allow_list_conjunctive")
				: whitelistMode == WhitelistMode.WHITELIST_DISJ
					? Lang.translateDirect("gui.attribute_filter.allow_list_disjunctive")
					: Lang.translateDirect("gui.attribute_filter.deny_list")).formatted(Formatting.GOLD));

			int count = 0;
			NbtList attributes = filter.getOrCreateNbt()
				.getList("MatchedAttributes", NbtElement.COMPOUND_TYPE);
			for (NbtElement inbt : attributes) {
				NbtCompound compound = (NbtCompound) inbt;
				ItemAttribute attribute = ItemAttribute.fromNBT(compound);
				if (attribute == null)
					continue;
				boolean inverted = compound.getBoolean("Inverted");
				if (count > 3) {
					list.add(Components.literal("- ...")
						.formatted(Formatting.DARK_GRAY));
					break;
				}
				list.add(Components.literal("- ")
					.append(attribute.format(inverted)));
				count++;
			}

			if (count == 0)
				return Collections.emptyList();
		}

		return list;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack heldItem = player.getStackInHand(hand);

		if (!player.isSneaking() && hand == Hand.MAIN_HAND) {
			if (!world.isClient && player instanceof ServerPlayerEntity)
				NetworkHooks.openScreen((ServerPlayerEntity) player, this, buf -> {
					buf.writeItemStack(heldItem);
				});
			return TypedActionResult.success(heldItem);
		}
		return TypedActionResult.pass(heldItem);
	}

	@Override
	public ScreenHandler createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		ItemStack heldItem = player.getMainHandStack();
		if (type == FilterType.REGULAR)
			return FilterMenu.create(id, inv, heldItem);
		if (type == FilterType.ATTRIBUTE)
			return AttributeFilterMenu.create(id, inv, heldItem);
		return null;
	}

	@Override
	public Text getDisplayName() {
		return getName();
	}

	public static ItemStackHandler getFilterItems(ItemStack stack) {
		ItemStackHandler newInv = new ItemStackHandler(18);
		if (AllItems.FILTER.get() != stack.getItem())
			throw new IllegalArgumentException("Cannot get filter items from non-filter: " + stack);
		if (!stack.hasNbt())
			return newInv;
		NbtCompound invNBT = stack.getOrCreateSubNbt("Items");
		if (!invNBT.isEmpty())
			newInv.deserializeNBT(invNBT);
		return newInv;
	}

	public static boolean testDirect(ItemStack filter, ItemStack stack, boolean matchNBT) {
		if (matchNBT) {
			return ItemHandlerHelper.canItemStacksStack(filter, stack);
		} else {
			return ItemHelper.sameItem(filter, stack);
		}
	}

}
