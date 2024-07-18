package com.simibubi.create.content.equipment.clipboard;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class ClipboardValueSettingsHandler {

	@Environment(EnvType.CLIENT)
	public static boolean drawCustomBlockSelection(WorldRenderer context, Camera camera, HitResult hitResult, float partialTicks, MatrixStack ms, VertexConsumerProvider buffers) {
		MinecraftClient mc = MinecraftClient.getInstance();
		BlockHitResult target = (BlockHitResult) hitResult;
		BlockPos pos = target.getBlockPos();
		BlockState blockstate = mc.world.getBlockState(pos);

		if (mc.player == null || mc.player.isSpectator())
			return false;
		if (!mc.world.getWorldBorder()
			.contains(pos))
			return false;
		if (!AllBlocks.CLIPBOARD.isIn(mc.player.getMainHandStack()))
			return false;
		if (!(mc.world.getBlockEntity(pos) instanceof SmartBlockEntity smartBE))
			return false;
		if (!smartBE.getAllBehaviours()
			.stream()
			.anyMatch(b -> b instanceof ClipboardCloneable cc
				&& cc.writeToClipboard(new NbtCompound(), target.getSide())))
			return false;

		VoxelShape shape = blockstate.getOutlineShape(mc.world, pos);
		if (shape.isEmpty())
			return false;

		VertexConsumer vb = buffers
			.getBuffer(RenderLayer.getLines());
		Vec3d camPos = camera
			.getPos();

		ms.push();
		ms.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
		TrackBlockOutline.renderShape(shape, ms, vb, true);

		ms.pop();
		return true;
	}

	@Environment(EnvType.CLIENT)
	public static void clientTick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!(mc.crosshairTarget instanceof BlockHitResult target))
			return;
		PlayerEntity player = mc.player; // fabric: keep LocalPlayer out of lambdas
		if (!AllBlocks.CLIPBOARD.isIn(player.getMainHandStack()))
			return;
		BlockPos pos = target.getBlockPos();
		if (!(mc.world.getBlockEntity(pos) instanceof SmartBlockEntity smartBE))
			return;

		NbtCompound tagElement = player.getMainHandStack()
			.getSubNbt("CopiedValues");

		boolean canCopy = smartBE.getAllBehaviours()
			.stream()
			.anyMatch(b -> b instanceof ClipboardCloneable cc
				&& cc.writeToClipboard(new NbtCompound(), target.getSide()))
			|| smartBE instanceof ClipboardCloneable ccbe
				&& ccbe.writeToClipboard(new NbtCompound(), target.getSide());

		boolean canPaste = tagElement != null && (smartBE.getAllBehaviours()
			.stream()
			.anyMatch(b -> b instanceof ClipboardCloneable cc && cc.readFromClipboard(
				tagElement.getCompound(cc.getClipboardKey()), player, target.getSide(), true))
			|| smartBE instanceof ClipboardCloneable ccbe && ccbe.readFromClipboard(
				tagElement.getCompound(ccbe.getClipboardKey()), player, target.getSide(), true));

		if (!canCopy && !canPaste)
			return;

		List<MutableText> tip = new ArrayList<>();
		tip.add(Lang.translateDirect("clipboard.actions"));
		if (canCopy)
			tip.add(Lang.translateDirect("clipboard.to_copy", Components.keybind("key.use")));
		if (canPaste)
			tip.add(Lang.translateDirect("clipboard.to_paste", Components.keybind("key.attack")));

		CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
	}

	public static ActionResult rightClickToCopy(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		return interact(player.getStackInHand(hand), hitResult.getBlockPos(), world, player, hitResult.getSide(), false);
	}

	public static ActionResult leftClickToPaste(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
		return interact(player.getStackInHand(hand), pos, world, player, direction, true);
	}

	private static ActionResult interact(ItemStack itemStack, BlockPos pos, World world, PlayerEntity player, Direction face, boolean paste) {
		if (!AllBlocks.CLIPBOARD.isIn(itemStack))
			return ActionResult.PASS;

		if (player != null && player.isSpectator() || AdventureUtil.isAdventure(player))
			return ActionResult.PASS;
		if (player.isSneaking())
			return ActionResult.PASS;
		if (!(world.getBlockEntity(pos) instanceof SmartBlockEntity smartBE))
			return ActionResult.PASS;
		NbtCompound tag = itemStack.getSubNbt("CopiedValues");
		if (paste && tag == null)
			return ActionResult.PASS;
		if (!paste)
			tag = new NbtCompound();

		boolean anySuccess = false;
		boolean anyValid = false;
		for (BlockEntityBehaviour behaviour : smartBE.getAllBehaviours()) {
			if (!(behaviour instanceof ClipboardCloneable cc))
				continue;
			anyValid = true;
			String clipboardKey = cc.getClipboardKey();
			if (paste) {
				anySuccess |=
					cc.readFromClipboard(tag.getCompound(clipboardKey), player, face, world.isClient());
				continue;
			}
			NbtCompound compoundTag = new NbtCompound();
			boolean success = cc.writeToClipboard(compoundTag, face);
			anySuccess |= success;
			if (success)
				tag.put(clipboardKey, compoundTag);
		}

		if (smartBE instanceof ClipboardCloneable ccbe) {
			anyValid = true;
			String clipboardKey = ccbe.getClipboardKey();
			if (paste) {
				anySuccess |= ccbe.readFromClipboard(tag.getCompound(clipboardKey), player, face,
					world.isClient());
			} else {
				NbtCompound compoundTag = new NbtCompound();
				boolean success = ccbe.writeToClipboard(compoundTag, face);
				anySuccess |= success;
				if (success)
					tag.put(clipboardKey, compoundTag);
			}
		}

		if (!anyValid)
			return ActionResult.PASS;

		if (world.isClient())
			return ActionResult.SUCCESS;
		if (!anySuccess)
			return ActionResult.SUCCESS;

		player.sendMessage(Lang
			.translate(paste ? "clipboard.pasted_to" : "clipboard.copied_from", world.getBlockState(pos)
				.getBlock()
				.getName()
				.formatted(Formatting.WHITE))
			.style(Formatting.GREEN)
			.component(), true);

		if (!paste) {
			ClipboardOverrides.switchTo(ClipboardType.WRITTEN, itemStack);
			itemStack.getOrCreateNbt()
				.put("CopiedValues", tag);
		}
		return ActionResult.SUCCESS;
	}

}
