package com.simibubi.create.content.logistics.vault;

import org.jetbrains.annotations.ApiStatus.Internal;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankItem;
import com.simibubi.create.foundation.utility.VecHelper;
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
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.world.World;

public class ItemVaultItem extends BlockItem {
	// fabric: see comment in FluidTankItem
	@Internal
	public static boolean IS_PLACING_NBT = false;

	public ItemVaultItem(Block p_i48527_1_, Settings p_i48527_2_) {
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
			nbt.remove("Length");
			nbt.remove("Size");
			nbt.remove("Controller");
			nbt.remove("LastKnownPos");
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
		ItemStack stack = ctx.getStack();
		World world = ctx.getWorld();
		BlockPos pos = ctx.getBlockPos();
		BlockPos placedOnPos = pos.offset(face.getOpposite());
		BlockState placedOnState = world.getBlockState(placedOnPos);

		if (!ItemVaultBlock.isVault(placedOnState))
			return;
		ItemVaultBlockEntity tankAt = ConnectivityHandler.partAt(AllBlockEntityTypes.ITEM_VAULT.get(), world, placedOnPos);
		if (tankAt == null)
			return;
		ItemVaultBlockEntity controllerBE = tankAt.getControllerBE();
		if (controllerBE == null)
			return;

		int width = controllerBE.radius;
		if (width == 1)
			return;

		int tanksToPlace = 0;
		Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(placedOnState);
		if (vaultBlockAxis == null)
			return;
		if (face.getAxis() != vaultBlockAxis)
			return;

		Direction vaultFacing = Direction.from(vaultBlockAxis, AxisDirection.POSITIVE);
		BlockPos startPos = face == vaultFacing.getOpposite() ? controllerBE.getPos()
			.offset(vaultFacing.getOpposite())
			: controllerBE.getPos()
				.offset(vaultFacing, controllerBE.length);

		if (VecHelper.getCoordinate(startPos, vaultBlockAxis) != VecHelper.getCoordinate(pos, vaultBlockAxis))
			return;

		for (int xOffset = 0; xOffset < width; xOffset++) {
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockPos offsetPos = vaultBlockAxis == Axis.X ? startPos.add(0, xOffset, zOffset)
					: startPos.add(xOffset, zOffset, 0);
				BlockState blockState = world.getBlockState(offsetPos);
				if (ItemVaultBlock.isVault(blockState))
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
				BlockPos offsetPos = vaultBlockAxis == Axis.X ? startPos.add(0, xOffset, zOffset)
					: startPos.add(xOffset, zOffset, 0);
				BlockState blockState = world.getBlockState(offsetPos);
				if (ItemVaultBlock.isVault(blockState))
					continue;
				ItemPlacementContext context = ItemPlacementContext.offset(ctx, offsetPos, face);
				player.getCustomData()
					.putBoolean("SilenceVaultSound", true);
				IS_PLACING_NBT = FluidTankItem.checkPlacingNbt(context);
				super.place(context);
				IS_PLACING_NBT = false;
				player.getCustomData()
					.remove("SilenceVaultSound");
			}
		}
	}

}
