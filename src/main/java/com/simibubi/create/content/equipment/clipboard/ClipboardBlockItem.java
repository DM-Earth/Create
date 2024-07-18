package com.simibubi.create.content.equipment.clipboard;

import javax.annotation.Nonnull;

import com.simibubi.create.foundation.gui.ScreenOpener;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ClipboardBlockItem extends BlockItem {

	public ClipboardBlockItem(Block pBlock, Settings pProperties) {
		super(pBlock, pProperties);
	}

	@Nonnull
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		if (player == null)
			return ActionResult.PASS;
		if (player.isSneaking())
			return super.useOnBlock(context);
		return use(context.getWorld(), player, context.getHand()).getResult();
	}

	@Override
	protected boolean postPlacement(BlockPos pPos, World pLevel, PlayerEntity pPlayer, ItemStack pStack,
		BlockState pState) {
		if (pLevel.isClient())
			return false;
		if (!(pLevel.getBlockEntity(pPos) instanceof ClipboardBlockEntity cbe))
			return false;
		cbe.dataContainer = ItemHandlerHelper.copyStackWithSize(pStack, 1);
		cbe.notifyUpdate();
		return true;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack heldItem = player.getStackInHand(hand);
		if (hand == Hand.OFF_HAND)
			return TypedActionResult.pass(heldItem);

		player.getItemCooldownManager()
			.set(heldItem.getItem(), 10);
		if (world.isClient)
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> openScreen(player, heldItem));
		NbtCompound tag = heldItem.getOrCreateNbt();
		tag.putInt("Type", ClipboardOverrides.ClipboardType.EDITING.ordinal());
		heldItem.setNbt(tag);

		return TypedActionResult.success(heldItem);
	}

	@Environment(EnvType.CLIENT)
	private void openScreen(PlayerEntity player, ItemStack stack) {
		if (MinecraftClient.getInstance().player == player)
			ScreenOpener.open(new ClipboardScreen(player.getInventory().selectedSlot, stack, null));
	}

	public void registerModelOverrides() {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> ClipboardOverrides.registerModelOverridesClient(this));
	}

}
