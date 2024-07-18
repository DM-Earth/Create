package com.simibubi.create.content.equipment.zapper;

import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.StairShape;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.foundation.utility.BlockHelper;

public class ZapperInteractionHandler {

	public static ActionResult leftClickingBlocksWithTheZapperSelectsTheBlock(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
		if (world.isClient)
			return ActionResult.PASS;
		ItemStack heldItem = player
			.getMainHandStack();
		if (heldItem.getItem() instanceof ZapperItem && trySelect(heldItem, player)) {
//			event.setCancellationResult(InteractionResult.FAIL);
//			event.setCanceled(true);
			return ActionResult.FAIL;
		}
		return ActionResult.PASS;
	}

	public static boolean trySelect(ItemStack stack, PlayerEntity player) {
		if (player.isSneaking())
			return false;

		Vec3d start = player.getPos()
			.add(0, player.getStandingEyeHeight(), 0);
		Vec3d range = player.getRotationVector()
			.multiply(getRange(stack));
		BlockHitResult raytrace = player.getWorld()
			.raycast(new RaycastContext(start, start.add(range), ShapeType.OUTLINE, FluidHandling.NONE, player));
		BlockPos pos = raytrace.getBlockPos();
		if (pos == null)
			return false;

		player.getWorld().setBlockBreakingInfo(player.getId(), pos, -1);
		BlockState newState = player.getWorld().getBlockState(pos);

		if (BlockHelper.getRequiredItem(newState)
			.isEmpty())
			return false;
		if (newState.hasBlockEntity() && !AllBlockTags.SAFE_NBT.matches(newState))
			return false;
		if (newState.contains(Properties.DOUBLE_BLOCK_HALF))
			return false;
		if (newState.contains(Properties.ATTACHED))
			return false;
		if (newState.contains(Properties.HANGING))
			return false;
		if (newState.contains(Properties.BED_PART))
			return false;
		if (newState.contains(Properties.STAIR_SHAPE))
			newState = newState.with(Properties.STAIR_SHAPE, StairShape.STRAIGHT);
		if (newState.contains(Properties.PERSISTENT))
			newState = newState.with(Properties.PERSISTENT, true);
		if (newState.contains(Properties.WATERLOGGED))
			newState = newState.with(Properties.WATERLOGGED, false);

		NbtCompound data = null;
		BlockEntity blockEntity = player.getWorld().getBlockEntity(pos);
		if (blockEntity != null) {
			data = blockEntity.createNbtWithIdentifyingData();
			data.remove("x");
			data.remove("y");
			data.remove("z");
			data.remove("id");
		}
		NbtCompound tag = stack.getOrCreateNbt();
		if (tag.contains("BlockUsed") && NbtHelper.toBlockState(player.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), stack.getNbt()
			.getCompound("BlockUsed")) == newState && Objects.equals(data, tag.get("BlockData"))) {
			return false;
		}

		tag.put("BlockUsed", NbtHelper.fromBlockState(newState));
		if (data == null)
			tag.remove("BlockData");
		else
			tag.put("BlockData", data);

		AllSoundEvents.CONFIRM.playOnServer(player.getWorld(), player.getBlockPos());
		return true;
	}

	public static int getRange(ItemStack stack) {
		if (stack.getItem() instanceof ZapperItem)
			return ((ZapperItem) stack.getItem()).getZappingRange(stack);
		return 0;
	}
}
