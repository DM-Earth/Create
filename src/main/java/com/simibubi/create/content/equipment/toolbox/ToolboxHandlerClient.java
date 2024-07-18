package com.simibubi.create.content.equipment.toolbox;

import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_HOTBAR_OFF;
import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_HOTBAR_ON;
import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_SELECTED_OFF;
import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_SELECTED_ON;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.fabricmc.fabric.api.entity.EntityPickInteractionAware;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

public class ToolboxHandlerClient {

	static int COOLDOWN = 0;

	public static void clientTick() {
		if (COOLDOWN > 0 && !AllKeys.TOOLBELT.isPressed())
			COOLDOWN--;
	}

	public static boolean onPickItem() {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		if (player == null)
			return false;
		World level = player.getWorld();
		HitResult hitResult = mc.crosshairTarget;

		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS)
			return false;
		if (player.isCreative())
			return false;

		ItemStack result = ItemStack.EMPTY;
		List<ToolboxBlockEntity> toolboxes = ToolboxHandler.getNearest(player.getWorld(), player, 8);

		if (toolboxes.isEmpty())
			return false;

		if (hitResult.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (state.isAir())
				return false;
			Block block = state.getBlock();
			result = block instanceof BlockPickInteractionAware aware
					? aware.getPickedStack(state, level, pos, player, hitResult)
					: block.getPickStack(level, pos, state);
		} else if (hitResult.getType() == HitResult.Type.ENTITY) {
			Entity entity = ((EntityHitResult) hitResult).getEntity();
			result = entity instanceof EntityPickInteractionAware aware
					? aware.getPickedStack(player, hitResult)
					: entity.getPickBlockStack();
		}

		if (result == null || result.isEmpty())
			return false;

		for (ToolboxBlockEntity toolboxBlockEntity : toolboxes) {
			ToolboxInventory inventory = toolboxBlockEntity.inventory;
			try (Transaction t = TransferUtil.getTransaction()) {
				for (int comp = 0; comp < 8; comp++) {
					ItemStack inSlot = inventory.takeFromCompartment(1, comp, t);
					if (inSlot.isEmpty())
						continue;
					if (inSlot.getItem() != result.getItem())
						continue;
					if (!ItemStack.areEqual(inSlot, result))
						continue;

					AllPackets.getChannel().sendToServer(
							new ToolboxEquipPacket(toolboxBlockEntity.getPos(), comp, player.getInventory().selectedSlot));
					return true;
				}
			}
		}

		return false;
	}

	public static void onKeyInput(int key, boolean pressed) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.interactionManager == null || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)
			return;

		if (key != AllKeys.TOOLBELT.getBoundCode() || !pressed)
			return;
		if (COOLDOWN > 0)
			return;
		ClientPlayerEntity player = mc.player;
		if (player == null)
			return;
		World level = player.getWorld();

		List<ToolboxBlockEntity> toolboxes = ToolboxHandler.getNearest(player.getWorld(), player, 8);
		toolboxes.sort(Comparator.comparing(ToolboxBlockEntity::getUniqueId));

		NbtCompound compound = player.getCustomData()
			.getCompound("CreateToolboxData");

		String slotKey = String.valueOf(player.getInventory().selectedSlot);
		boolean equipped = compound.contains(slotKey);

		if (equipped) {
			BlockPos pos = NbtHelper.toBlockPos(compound.getCompound(slotKey)
				.getCompound("Pos"));
			double max = ToolboxHandler.getMaxRange(player);
			boolean canReachToolbox = ToolboxHandler.distance(player.getPos(), pos) < max * max;

			if (canReachToolbox) {
				BlockEntity blockEntity = level.getBlockEntity(pos);
				if (blockEntity instanceof ToolboxBlockEntity) {
					RadialToolboxMenu screen = new RadialToolboxMenu(toolboxes,
						RadialToolboxMenu.State.SELECT_ITEM_UNEQUIP, (ToolboxBlockEntity) blockEntity);
					screen.prevSlot(compound.getCompound(slotKey)
						.getInt("Slot"));
					ScreenOpener.open(screen);
					return;
				}
			}

			ScreenOpener.open(new RadialToolboxMenu(ImmutableList.of(), RadialToolboxMenu.State.DETACH, null));
			return;
		}

		if (toolboxes.isEmpty())
			return;

		if (toolboxes.size() == 1)
			ScreenOpener.open(new RadialToolboxMenu(toolboxes, RadialToolboxMenu.State.SELECT_ITEM, toolboxes.get(0)));
		else
			ScreenOpener.open(new RadialToolboxMenu(toolboxes, RadialToolboxMenu.State.SELECT_BOX, null));
	}

	public static void renderOverlay(DrawContext graphics, float partialTicks, Window window) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)
			return;

		int x = window.getScaledWidth() / 2 - 90;
		int y = window.getScaledHeight() - 23;
		RenderSystem.enableDepthTest();

		PlayerEntity player = mc.player;
		NbtCompound persistentData = player.getCustomData();
		if (!persistentData.contains("CreateToolboxData"))
			return;

		NbtCompound compound = player.getCustomData()
			.getCompound("CreateToolboxData");

		if (compound.isEmpty())
			return;

		MatrixStack poseStack = graphics.getMatrices();
		poseStack.push();
		for (int slot = 0; slot < 9; slot++) {
			String key = String.valueOf(slot);
			if (!compound.contains(key))
				continue;
			BlockPos pos = NbtHelper.toBlockPos(compound.getCompound(key)
				.getCompound("Pos"));
			double max = ToolboxHandler.getMaxRange(player);
			boolean selected = player.getInventory().selectedSlot == slot;
			int offset = selected ? 1 : 0;
			AllGuiTextures texture = ToolboxHandler.distance(player.getPos(), pos) < max * max
				? selected ? TOOLBELT_SELECTED_ON : TOOLBELT_HOTBAR_ON
				: selected ? TOOLBELT_SELECTED_OFF : TOOLBELT_HOTBAR_OFF;
			texture.render(graphics, x + 20 * slot - offset, y + offset - AllConfigs.client().toolboxHotbarOverlayOffset.get());
		}
		poseStack.pop();
	}

}
