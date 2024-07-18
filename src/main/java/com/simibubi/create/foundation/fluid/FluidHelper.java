package com.simibubi.create.foundation.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simibubi.create.Create;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class FluidHelper {

	public static enum FluidExchange {
		ITEM_TO_TANK, TANK_TO_ITEM;
	}

	public static boolean isWater(Fluid fluid) {
		return convertToStill(fluid) == Fluids.WATER;
	}

	public static boolean isLava(Fluid fluid) {
		return convertToStill(fluid) == Fluids.LAVA;
	}

	@SuppressWarnings("deprecation")
	public static boolean isTag(Fluid fluid, TagKey<Fluid> tag) {
		return fluid.isIn(tag);
	}

	public static boolean isTag(FluidState fluid, TagKey<Fluid> tag) {
		return fluid.isIn(tag);
	}

	public static boolean isTag(FluidStack fluid, TagKey<Fluid> tag) {
		return isTag(fluid.getFluid(), tag);
	}

	public static SoundEvent getFillSound(FluidStack fluid) {
		return FluidVariantAttributes.getFillSound(fluid.getType());
	}

	public static SoundEvent getEmptySound(FluidStack fluid) {
		return FluidVariantAttributes.getEmptySound(fluid.getType());
	}

	public static boolean hasBlockState(Fluid fluid) {
		return !fluid.getDefaultState().getBlockState().isAir();
	}

	public static FluidStack copyStackWithAmount(FluidStack fs, long amount) {
		if (amount <= 0)
			return FluidStack.EMPTY;
		if (fs.isEmpty())
			return FluidStack.EMPTY;
		FluidStack copy = fs.copy();
		copy.setAmount(amount);
		return copy;
	}

	public static Fluid convertToFlowing(Fluid fluid) {
		if (fluid == Fluids.WATER)
			return Fluids.FLOWING_WATER;
		if (fluid == Fluids.LAVA)
			return Fluids.FLOWING_LAVA;
		if (fluid instanceof FlowableFluid)
			return ((FlowableFluid) fluid).getFlowing();
		return fluid;
	}

	public static Fluid convertToStill(Fluid fluid) {
		if (fluid == Fluids.FLOWING_WATER)
			return Fluids.WATER;
		if (fluid == Fluids.FLOWING_LAVA)
			return Fluids.LAVA;
		if (fluid instanceof FlowableFluid)
			return ((FlowableFluid) fluid).getStill();
		return fluid;
	}

	public static JsonElement serializeFluidStack(FluidStack stack) {
		JsonObject json = new JsonObject();
		json.addProperty("fluid", RegisteredObjects.getKeyOrThrow(stack.getFluid())
			.toString());
		json.addProperty("amount", stack.getAmount());
		if (stack.hasTag())
			json.addProperty("nbt", stack.getTag()
				.toString());
		return json;
	}

	public static FluidStack deserializeFluidStack(JsonObject json) {
		Identifier id = new Identifier(JsonHelper.getString(json, "fluid"));
		Fluid fluid = Registries.FLUID.get(id);
		if (fluid == null)
			throw new JsonSyntaxException("Unknown fluid '" + id + "'");
		int amount = JsonHelper.getInt(json, "amount");
		if (!json.has("nbt"))
			return new FluidStack(fluid, amount);

		try {
			JsonElement element = json.get("nbt");
			NbtCompound nbt = StringNbtReader.parse(
					element.isJsonObject() ? Create.GSON.toJson(element) : JsonHelper.asString(element, "nbt"));
			return new FluidStack(FluidVariant.of(fluid, nbt), amount, nbt);
		} catch (CommandSyntaxException e) {
			throw new JsonSyntaxException("Failed to read NBT", e);
		}
	}

	public static boolean tryEmptyItemIntoBE(World worldIn, PlayerEntity player, Hand handIn, ItemStack heldItem,
		SmartBlockEntity be, Direction side) {
		if (!GenericItemEmptying.canItemBeEmptied(worldIn, heldItem))
			return false;

		Pair<FluidStack, ItemStack> emptyingResult = GenericItemEmptying.emptyItem(worldIn, heldItem, true);

		Storage<FluidVariant> tank = FluidStorage.SIDED.find(worldIn, be.getPos(), null, be, side);
		FluidStack fluidStack = emptyingResult.getFirst();

		if (tank == null)
			return false;
		if (worldIn.isClient)
			return true;

		try (Transaction t = TransferUtil.getTransaction()) {
			long inserted = tank.insert(fluidStack.getType(), fluidStack.getAmount(), t);
			if (inserted != fluidStack.getAmount())
				return false;

			ItemStack copyOfHeld = heldItem.copy();
			emptyingResult = GenericItemEmptying.emptyItem(worldIn, copyOfHeld, false);
			t.commit();

			if (!player.isCreative() && !(be instanceof CreativeFluidTankBlockEntity)) {
				if (copyOfHeld.isEmpty())
					player.setStackInHand(handIn, emptyingResult.getSecond());
				else {
					player.setStackInHand(handIn, copyOfHeld);
					player.getInventory().offerOrDrop(emptyingResult.getSecond());
				}
			}
			return true;
		}
	}

	public static boolean tryFillItemFromBE(World world, PlayerEntity player, Hand handIn, ItemStack heldItem,
		SmartBlockEntity be, Direction side) {
		if (!GenericItemFilling.canItemBeFilled(world, heldItem))
			return false;

		Storage<FluidVariant> tank = FluidStorage.SIDED.find(world, be.getPos(), null, be, side);

		if (tank == null)
			return false;

		try (Transaction t = TransferUtil.getTransaction()) {
			for (FluidStack fluid : TransferUtil.getAllFluids(tank)) {
				if (fluid.isEmpty())
					continue;
				long requiredAmountForItem = GenericItemFilling.getRequiredAmountForItem(world, heldItem, fluid.copy());
				if (requiredAmountForItem == -1)
					continue;
				if (requiredAmountForItem > fluid.getAmount())
					continue;

				if (world.isClient)
					return true;

				if (player.isCreative() || be instanceof CreativeFluidTankBlockEntity)
					heldItem = heldItem.copy();
				ItemStack out = GenericItemFilling.fillItem(world, requiredAmountForItem, heldItem, fluid.copy());

				FluidStack copy = fluid.copy();
				copy.setAmount(requiredAmountForItem);
				tank.extract(copy.getType(), copy.getAmount(), t);
				t.commit();

				if (!player.isCreative())
					player.getInventory().offerOrDrop(out);
				be.notifyUpdate();
				return true;
			}
		}

		return false;
	}

//	@Nullable
//	public static FluidExchange exchange(IFluidHandler fluidTank, IFluidHandlerItem fluidItem, FluidExchange preferred,
//										 int maxAmount) {
//		return exchange(fluidTank, fluidItem, preferred, true, maxAmount);
//	}
//
//	@Nullable
//	public static FluidExchange exchangeAll(IFluidHandler fluidTank, IFluidHandlerItem fluidItem,
//		FluidExchange preferred) {
//		return exchange(fluidTank, fluidItem, preferred, false, Integer.MAX_VALUE);
//	}
//
//	@Nullable
//	private static FluidExchange exchange(IFluidHandler fluidTank, IFluidHandlerItem fluidItem, FluidExchange preferred,
//		boolean singleOp, int maxTransferAmountPerTank) {
//
//		// Locks in the transfer direction of this operation
//		FluidExchange lockedExchange = null;
//
//		for (int tankSlot = 0; tankSlot < fluidTank.getTanks(); tankSlot++) {
//			for (int slot = 0; slot < fluidItem.getTanks(); slot++) {
//
//				FluidStack fluidInTank = fluidTank.getFluidInTank(tankSlot);
//				long tankCapacity = fluidTank.getTankCapacity(tankSlot) - fluidInTank.getAmount();
//				boolean tankEmpty = fluidInTank.isEmpty();
//
//				FluidStack fluidInItem = fluidItem.getFluidInTank(tankSlot);
//				long itemCapacity = fluidItem.getTankCapacity(tankSlot) - fluidInItem.getAmount();
//				boolean itemEmpty = fluidInItem.isEmpty();
//
//				boolean undecided = lockedExchange == null;
//				boolean canMoveToTank = (undecided || lockedExchange == FluidExchange.ITEM_TO_TANK) && tankCapacity > 0;
//				boolean canMoveToItem = (undecided || lockedExchange == FluidExchange.TANK_TO_ITEM) && itemCapacity > 0;
//
//				// Incompatible Liquids
//				if (!tankEmpty && !itemEmpty && !fluidInItem.isFluidEqual(fluidInTank))
//					continue;
//
//				// Transfer liquid to tank
//				if (((tankEmpty || itemCapacity <= 0) && canMoveToTank)
//					|| undecided && preferred == FluidExchange.ITEM_TO_TANK) {
//
//					long amount = fluidTank.fill(
//						fluidItem.drain(Math.min(maxTransferAmountPerTank, tankCapacity), false),
//						false);
//					if (amount > 0) {
//						lockedExchange = FluidExchange.ITEM_TO_TANK;
//						if (singleOp)
//							return lockedExchange;
//						continue;
//					}
//				}
//
//				// Transfer liquid from tank
//				if (((itemEmpty || tankCapacity <= 0) && canMoveToItem)
//					|| undecided && preferred == FluidExchange.TANK_TO_ITEM) {
//
//					long amount = fluidItem.fill(
//						fluidTank.drain(Math.min(maxTransferAmountPerTank, itemCapacity), false),
//						false);
//					if (amount > 0) {
//						lockedExchange = FluidExchange.TANK_TO_ITEM;
//						if (singleOp)
//							return lockedExchange;
//						continue;
//					}
//
//				}
//
//			}
//		}
//
//		return null;
//	}

}
