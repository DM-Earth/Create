package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Couple;
import com.tterrag.registrate.fabric.EnvExecutor;

import io.github.fabricators_of_create.porting_lib.item.UseFirstBehaviorItem;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LinkedControllerItem extends Item implements NamedScreenHandlerFactory, UseFirstBehaviorItem {

	public LinkedControllerItem(Settings properties) {
		super(properties);
	}

	@Override
	public ActionResult onItemUseFirst(ItemStack stack, ItemUsageContext ctx) {
		PlayerEntity player = ctx.getPlayer();
		if (player == null)
			return ActionResult.PASS;
		World world = ctx.getWorld();
		BlockPos pos = ctx.getBlockPos();
		BlockState hitState = world.getBlockState(pos);

		if (player.canModifyBlocks()) {
			if (player.isSneaking()) {
				if (AllBlocks.LECTERN_CONTROLLER.has(hitState)) {
					if (!world.isClient)
						AllBlocks.LECTERN_CONTROLLER.get().withBlockEntityDo(world, pos, be ->
								be.swapControllers(stack, player, ctx.getHand(), hitState));
					return ActionResult.SUCCESS;
				}
			} else {
				if (AllBlocks.REDSTONE_LINK.has(hitState)) {
					if (world.isClient)
						EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> this.toggleBindMode(ctx.getBlockPos()));
					player.getItemCooldownManager()
							.set(this, 2);
					return ActionResult.SUCCESS;
				}

				if (hitState.isOf(Blocks.LECTERN) && !hitState.get(LecternBlock.HAS_BOOK)) {
					if (!world.isClient) {
						ItemStack lecternStack = player.isCreative() ? stack.copy() : stack.split(1);
						AllBlocks.LECTERN_CONTROLLER.get().replaceLectern(hitState, world, pos, lecternStack);
					}
					return ActionResult.SUCCESS;
				}

				if (AllBlocks.LECTERN_CONTROLLER.has(hitState))
					return ActionResult.PASS;
			}
		}

		return use(world, player, ctx.getHand()).getResult();
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack heldItem = player.getStackInHand(hand);

		if (player.isSneaking() && hand == Hand.MAIN_HAND) {
			if (!world.isClient && player instanceof ServerPlayerEntity && player.canModifyBlocks())
				NetworkHooks.openScreen((ServerPlayerEntity) player, this, buf -> {
					buf.writeItemStack(heldItem);
				});
			return TypedActionResult.success(heldItem);
		}

		if (!player.isSneaking()) {
			if (world.isClient)
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::toggleActive);
			player.getItemCooldownManager()
				.set(this, 2);
		}

		return TypedActionResult.pass(heldItem);
	}

	@Environment(EnvType.CLIENT)
	private void toggleBindMode(BlockPos pos) {
		LinkedControllerClientHandler.toggleBindMode(pos);
	}

	@Environment(EnvType.CLIENT)
	private void toggleActive() {
		LinkedControllerClientHandler.toggle();
	}

	public static ItemStackHandler getFrequencyItems(ItemStack stack) {
		ItemStackHandler newInv = new ItemStackHandler(12);
		if (AllItems.LINKED_CONTROLLER.get() != stack.getItem())
			throw new IllegalArgumentException("Cannot get frequency items from non-controller: " + stack);
		NbtCompound invNBT = stack.getOrCreateSubNbt("Items");
		if (!invNBT.isEmpty())
			newInv.deserializeNBT(invNBT);
		return newInv;
	}

	public static Couple<RedstoneLinkNetworkHandler.Frequency> toFrequency(ItemStack controller, int slot) {
		ItemStackHandler frequencyItems = getFrequencyItems(controller);
		return Couple.create(Frequency.of(frequencyItems.getStackInSlot(slot * 2)),
			Frequency.of(frequencyItems.getStackInSlot(slot * 2 + 1)));
	}

	@Override
	public ScreenHandler createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		if (AdventureUtil.isAdventure(player))
			return null;
		ItemStack heldItem = player.getMainHandStack();
		return LinkedControllerMenu.create(id, inv, heldItem);
	}

	@Override
	public Text getDisplayName() {
		return getName();
	}

//	@Override
//	@Environment(EnvType.CLIENT)
//	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
//		consumer.accept(SimpleCustomRenderer.create(this, new LinkedControllerItemRenderer()));
//	}

}
