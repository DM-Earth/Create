package com.simibubi.create.content.fluids.tank;

import org.jetbrains.annotations.ApiStatus.Internal;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.connectivity.ConnectivityHandler;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class FluidTankItem extends BlockItem {
	// fabric: (#690) because of ordering differences, we need to delay connection by a tick when placing multiblocks with NBT.
	// If the item has NBT, it needs to be applied to a controller. However, ordering is different on fabric.
	// on forge, the block is placed, the data is set, and the tanks connect.
	// on fabric, the block is placed, the tanks connect, and the data is set.
	// However, now that the tank is not a controller, nothing happens.
	// solution: hacky static state storage. If we're placing NBT, delay connection until next tick.
	@Internal
	public static boolean IS_PLACING_NBT = false;

	public FluidTankItem(Block p_i48527_1_, Settings p_i48527_2_) {
		super(p_i48527_1_, p_i48527_2_);
	}

	@Override
	public ActionResult place(ItemPlacementContext ctx) {
		IS_PLACING_NBT = FluidTankItem.checkPlacingNbt(ctx);
		ActionResult initialResult = super.place(ctx);
		IS_PLACING_NBT = false;
		if (!initialResult.isAccepted())
			return initialResult;
		tryMultiPlace(ctx);
		return initialResult;
	}

	@Override
	protected boolean postPlacement(BlockPos p_195943_1_, World p_195943_2_, PlayerEntity p_195943_3_,
		ItemStack p_195943_4_, BlockState p_195943_5_) {
		MinecraftServer minecraftserver = p_195943_2_.getServer();
		if (minecraftserver == null)
			return false;
		NbtCompound nbt = p_195943_4_.getSubNbt("BlockEntityTag");
		if (nbt != null) {
			nbt.remove("Luminosity");
			nbt.remove("Size");
			nbt.remove("Height");
			nbt.remove("Controller");
			nbt.remove("LastKnownPos");
			if (nbt.contains("TankContent")) {
				FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt.getCompound("TankContent"));
				if (!fluid.isEmpty()) {
					fluid.setAmount(Math.min(FluidTankBlockEntity.getCapacityMultiplier(), fluid.getAmount()));
					nbt.put("TankContent", fluid.writeToNBT(new NbtCompound()));
				}
			}
		}
		return super.postPlacement(p_195943_1_, p_195943_2_, p_195943_3_, p_195943_4_, p_195943_5_);
	}

	private void tryMultiPlace(ItemPlacementContext ctx) {
		PlayerEntity player = ctx.getPlayer();
		if (player == null)
			return;
		if (player.isSneaking())
			return;
		Direction face = ctx.getSide();
		if (!face.getAxis()
			.isVertical())
			return;
		ItemStack stack = ctx.getStack();
		World world = ctx.getWorld();
		BlockPos pos = ctx.getBlockPos();
		BlockPos placedOnPos = pos.offset(face.getOpposite());
		BlockState placedOnState = world.getBlockState(placedOnPos);

		if (!FluidTankBlock.isTank(placedOnState))
			return;
		boolean creative = getBlock().equals(AllBlocks.CREATIVE_FLUID_TANK.get());
		FluidTankBlockEntity tankAt = ConnectivityHandler.partAt(
			creative ? AllBlockEntityTypes.CREATIVE_FLUID_TANK.get() : AllBlockEntityTypes.FLUID_TANK.get(), world, placedOnPos
		);
		if (tankAt == null)
			return;
		FluidTankBlockEntity controllerBE = tankAt.getControllerBE();
		if (controllerBE == null)
			return;

		int width = controllerBE.width;
		if (width == 1)
			return;

		int tanksToPlace = 0;
		BlockPos startPos = face == Direction.DOWN ? controllerBE.getPos()
			.down()
			: controllerBE.getPos()
				.up(controllerBE.height);

		if (startPos.getY() != pos.getY())
			return;

		for (int xOffset = 0; xOffset < width; xOffset++) {
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockPos offsetPos = startPos.add(xOffset, 0, zOffset);
				BlockState blockState = world.getBlockState(offsetPos);
				if (FluidTankBlock.isTank(blockState))
					continue;
				if (!blockState.isReplaceable())
					return;
				tanksToPlace++;
			}
		}

		if (!player.isCreative() && stack.getCount() < tanksToPlace)
			return;

		for (int xOffset = 0; xOffset < width; xOffset++) {
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockPos offsetPos = startPos.add(xOffset, 0, zOffset);
				BlockState blockState = world.getBlockState(offsetPos);
				if (FluidTankBlock.isTank(blockState))
					continue;
				ItemPlacementContext context = ItemPlacementContext.offset(ctx, offsetPos, face);
				player.getCustomData()
					.putBoolean("SilenceTankSound", true);
				IS_PLACING_NBT = checkPlacingNbt(context);
				super.place(context);
				IS_PLACING_NBT = false;
				player.getCustomData()
					.remove("SilenceTankSound");
			}
		}
	}

	public static boolean checkPlacingNbt(ItemPlacementContext ctx) {
		ItemStack item = ctx.getStack();
		return BlockItem.getBlockEntityNbt(item) != null;
	}
}
