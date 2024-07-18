package com.simibubi.create.foundation.blockEntity.behaviour.filtering;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;

import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FilteringBehaviour extends BlockEntityBehaviour implements ValueSettingsBehaviour {

	public static final BehaviourType<FilteringBehaviour> TYPE = new BehaviourType<>();

	public MutableText customLabel;
	ValueBoxTransform slotPositioning;
	boolean showCount;

	private FilterItemStack filter;
	public int count;
	public boolean upTo;
	private Predicate<ItemStack> predicate;
	private Consumer<ItemStack> callback;
	private Supplier<Boolean> isActive;
	private Supplier<Boolean> showCountPredicate;

	boolean recipeFilter;
	boolean fluidFilter;

	public FilteringBehaviour(SmartBlockEntity be, ValueBoxTransform slot) {
		super(be);
		filter = FilterItemStack.empty();
		slotPositioning = slot;
		showCount = false;
		callback = stack -> {
		};
		predicate = stack -> true;
		isActive = () -> true;
		count = 64;
		showCountPredicate = () -> showCount;
		recipeFilter = false;
		fluidFilter = false;
		upTo = true;
	}

	@Override
	public boolean isSafeNBT() {
		return true;
	}

	@Override
	public void write(NbtCompound nbt, boolean clientPacket) {
		nbt.put("Filter", NBTSerializer.serializeNBT(getFilter()));
		nbt.putInt("FilterAmount", count);
		nbt.putBoolean("UpTo", upTo);
		super.write(nbt, clientPacket);
	}

	@Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		filter = FilterItemStack.of(nbt.getCompound("Filter"));
		count = nbt.getInt("FilterAmount");
		upTo = nbt.getBoolean("UpTo");

		// Migrate from previous behaviour
		if (count == 0) {
			upTo = true;
			count = filter.item()
				.getMaxCount();
		}

		super.read(nbt, clientPacket);
	}

	public FilteringBehaviour withCallback(Consumer<ItemStack> filterCallback) {
		callback = filterCallback;
		return this;
	}

	public FilteringBehaviour withPredicate(Predicate<ItemStack> filterPredicate) {
		predicate = filterPredicate;
		return this;
	}

	public FilteringBehaviour forRecipes() {
		recipeFilter = true;
		return this;
	}

	public FilteringBehaviour forFluids() {
		fluidFilter = true;
		return this;
	}

	public FilteringBehaviour onlyActiveWhen(Supplier<Boolean> condition) {
		isActive = condition;
		return this;
	}

	public FilteringBehaviour showCountWhen(Supplier<Boolean> condition) {
		showCountPredicate = condition;
		return this;
	}

	public FilteringBehaviour showCount() {
		showCount = true;
		return this;
	}

	public boolean setFilter(Direction face, ItemStack stack) {
		return setFilter(stack);
	}

	public void setLabel(MutableText label) {
		this.customLabel = label;
	}

	public boolean setFilter(ItemStack stack) {
		ItemStack filter = stack.copy();
		if (!filter.isEmpty() && !predicate.test(filter))
			return false;
		this.filter = FilterItemStack.of(filter);
		if (!upTo)
			count = Math.min(count, stack.getMaxCount());
		callback.accept(filter);
		blockEntity.markDirty();
		blockEntity.sendData();
		return true;
	}

	@Override
	public void setValueSettings(PlayerEntity player, ValueSettings settings, boolean ctrlDown) {
		if (getValueSettings().equals(settings))
			return;
		count = MathHelper.clamp(settings.value(), 1, filter.item()
			.getMaxCount());
		upTo = settings.row() == 0;
		blockEntity.markDirty();
		blockEntity.sendData();
		playFeedbackSound(this);
	}

	@Override
	public ValueSettings getValueSettings() {
		return new ValueSettings(upTo ? 0 : 1, count == 0 ? filter.item()
			.getMaxCount() : count);
	}

	@Override
	public void destroy() {
		if (filter.isFilterItem()) {
			Vec3d pos = VecHelper.getCenterOf(getPos());
			World world = getWorld();
			world.spawnEntity(new ItemEntity(world, pos.x, pos.y, pos.z, filter.item()
				.copy()));
		}
		super.destroy();
	}

	@Override
	public ItemRequirement getRequiredItems() {
		if (filter.isFilterItem())
			return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, filter.item());

		return ItemRequirement.NONE;
	}

	public ItemStack getFilter(Direction side) {
		return getFilter();
	}

	public ItemStack getFilter() {
		return filter.item();
	}

	public boolean isCountVisible() {
		return showCountPredicate.get() && filter.item()
			.getMaxCount() > 1;
	}

	public boolean test(ItemStack stack) {
		return !isActive() || filter.test(blockEntity.getWorld(), stack);
	}

	public boolean test(FluidStack stack) {
		return !isActive() || filter.test(blockEntity.getWorld(), stack);
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	@Override
	public boolean testHit(Vec3d hit) {
		BlockState state = blockEntity.getCachedState();
		Vec3d localHit = hit.subtract(Vec3d.of(blockEntity.getPos()));
		return slotPositioning.testHit(state, localHit);
	}

	public int getAmount() {
		return count;
	}

	public boolean anyAmount() {
		return count == 0;
	}

	@Override
	public boolean acceptsValueSettings() {
		return isCountVisible();
	}

	@Override
	public boolean isActive() {
		return isActive.get();
	}

	@Override
	public ValueBoxTransform getSlotPositioning() {
		return slotPositioning;
	}

	@Override
	public ValueSettingsBoard createBoard(PlayerEntity player, BlockHitResult hitResult) {
		ItemStack filter = getFilter(hitResult.getSide());
		int maxAmount = (filter.getItem() instanceof FilterItem) ? 64 : filter.getMaxCount();
		return new ValueSettingsBoard(Lang.translateDirect("logistics.filter.extracted_amount"), maxAmount, 16,
			Lang.translatedOptions("logistics.filter", "up_to", "exactly"),
			new ValueSettingsFormatter(this::formatValue));
	}

	public MutableText formatValue(ValueSettings value) {
		if (value.row() == 0 && value.value() == filter.item()
			.getMaxCount())
			return Lang.translateDirect("logistics.filter.any_amount_short");
		return Components.literal(((value.row() == 0) ? "\u2264" : "=") + Math.max(1, value.value()));
	}

	@Override
	public void onShortInteract(PlayerEntity player, Hand hand, Direction side) {
		World level = getWorld();
		BlockPos pos = getPos();
		ItemStack itemInHand = player.getStackInHand(hand);
		ItemStack toApply = itemInHand.copy();

		if (AllItems.WRENCH.isIn(toApply))
			return;
		if (AllBlocks.MECHANICAL_ARM.isIn(toApply))
			return;
		if (level.isClient())
			return;

		if (getFilter(side).getItem() instanceof FilterItem) {
			if (!player.isCreative() || ItemHelper
				.extract(PlayerInventoryStorage.of(player),
					stack -> ItemHandlerHelper.canItemStacksStack(stack, getFilter(side)), true)
				.isEmpty())
				player.getInventory()
					.offerOrDrop(getFilter(side).copy());
		}

		if (toApply.getItem() instanceof FilterItem)
			toApply.setCount(1);

		if (!setFilter(side, toApply)) {
			player.sendMessage(Lang.translateDirect("logistics.filter.invalid_item"), true);
			AllSoundEvents.DENY.playOnServer(player.getWorld(), player.getBlockPos(), 1, 1);
			return;
		}

		if (!player.isCreative()) {
			if (toApply.getItem() instanceof FilterItem) {
				if (itemInHand.getCount() == 1)
					player.setStackInHand(hand, ItemStack.EMPTY);
				else
					itemInHand.decrement(1);
			}
		}

		level.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, .25f, .1f);
	}

	public MutableText getLabel() {
		if (customLabel != null)
			return customLabel;
		return Lang.translateDirect(
			recipeFilter ? "logistics.recipe_filter" : fluidFilter ? "logistics.fluid_filter" : "logistics.filter");
	}

	@Override
	public String getClipboardKey() {
		return "Filtering";
	}

	@Override
	public boolean writeToClipboard(NbtCompound tag, Direction side) {
		ValueSettingsBehaviour.super.writeToClipboard(tag, side);
		ItemStack filter = getFilter(side);
		tag.put("Filter", NBTSerializer.serializeNBT(filter));
		return true;
	}

	@Override
	public boolean readFromClipboard(NbtCompound tag, PlayerEntity player, Direction side, boolean simulate) {
		boolean upstreamResult = ValueSettingsBehaviour.super.readFromClipboard(tag, player, side, simulate);
		if (!tag.contains("Filter"))
			return upstreamResult;
		if (simulate)
			return true;
		if (getWorld().isClient)
			return true;

		ItemStack refund = ItemStack.EMPTY;
		if (getFilter(side).getItem() instanceof FilterItem && !player.isCreative())
			refund = getFilter(side).copy();

		ItemStack copied = ItemStack.fromNbt(tag.getCompound("Filter"));

		if (copied.getItem() instanceof FilterItem filterType && !player.isCreative()) {
			PlayerInventoryStorage inv = PlayerInventoryStorage.of(player);

			for (boolean preferStacksWithoutData : Iterate.trueAndFalse) {
				if (refund.getItem() != filterType && ItemHelper
					.extract(inv, stack -> stack.getItem() == filterType && preferStacksWithoutData != stack.hasNbt(),
						1, false)
					.isEmpty())
					continue;

				if (!refund.isEmpty() && refund.getItem() != filterType)
					player.getInventory()
						.offerOrDrop(refund);

				setFilter(side, copied);
				return true;
			}

			player.sendMessage(Lang
				.translate("logistics.filter.requires_item_in_inventory", copied.getName()
					.copy()
					.formatted(Formatting.WHITE))
				.style(Formatting.RED)
				.component(), true);
			AllSoundEvents.DENY.playOnServer(player.getWorld(), player.getBlockPos(), 1, 1);
			return false;
		}

		if (!refund.isEmpty())
			player.getInventory()
				.offerOrDrop(refund);

		return setFilter(side, copied);
	}
}
