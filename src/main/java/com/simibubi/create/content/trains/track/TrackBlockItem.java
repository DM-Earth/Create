package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class TrackBlockItem extends BlockItem {

	public TrackBlockItem(Block pBlock, Settings pProperties) {
		super(pBlock, pProperties);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext pContext) {
		ItemStack stack = pContext.getStack();
		BlockPos pos = pContext.getBlockPos();
		World level = pContext.getWorld();
		BlockState state = level.getBlockState(pos);
		PlayerEntity player = pContext.getPlayer();

		if (player == null)
			return super.useOnBlock(pContext);
		if (pContext.getHand() == Hand.OFF_HAND)
			return super.useOnBlock(pContext);

		Vec3d lookAngle = player.getRotationVector();

		if (!hasGlint(stack)) {
			if (state.getBlock() instanceof TrackBlock track && track.getTrackAxes(level, pos, state)
				.size() > 1) {
				if (!level.isClient)
					player.sendMessage(Lang.translateDirect("track.junction_start")
						.formatted(Formatting.RED), true);
				return ActionResult.SUCCESS;
			}

			if (level.getBlockEntity(pos) instanceof TrackBlockEntity tbe && tbe.isTilted()) {
				if (!level.isClient)
					player.sendMessage(Lang.translateDirect("track.turn_start")
						.formatted(Formatting.RED), true);
				return ActionResult.SUCCESS;
			}

			if (select(level, pos, lookAngle, stack)) {
				level.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 0.75f, 1);
				return ActionResult.SUCCESS;
			}
			return super.useOnBlock(pContext);

		} else if (player.isSneaking()) {
			if (!level.isClient) {
				player.sendMessage(Lang.translateDirect("track.selection_cleared"), true);
				stack.setNbt(null);
			} else
				level.playSound(player, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 0.75f, 1);
			return ActionResult.SUCCESS;
		}

		boolean placing = !(state.getBlock() instanceof ITrackBlock);
		NbtCompound tag = stack.getNbt();
		boolean extend = tag.getBoolean("ExtendCurve");
		tag.remove("ExtendCurve");

		if (placing) {
			if (!state.isReplaceable())
				pos = pos.offset(pContext.getSide());
			state = getPlacementState(pContext);
			if (state == null)
				return ActionResult.FAIL;
		}

		ItemStack offhandItem = player.getOffHandStack();
		boolean hasGirder = AllBlocks.METAL_GIRDER.isIn(offhandItem);
		PlacementInfo info = TrackPlacement.tryConnect(level, player, pos, state, stack, hasGirder, extend);

		if (info.message != null && !level.isClient)
			player.sendMessage(Lang.translateDirect(info.message), true);
		if (!info.valid) {
			AllSoundEvents.DENY.playFrom(player, 1, 1);
			return ActionResult.FAIL;
		}

		if (level.isClient)
			return ActionResult.SUCCESS;

		stack = player.getMainHandStack();
		if (AllTags.AllBlockTags.TRACKS.matches(stack)) {
			stack.setNbt(null);
			player.setStackInHand(pContext.getHand(), stack);
		}

		BlockSoundGroup soundtype = state.getSoundGroup();
		if (soundtype != null)
			level.playSound(null, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS,
				(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

		return ActionResult.SUCCESS;
	}

	public BlockState getPlacementState(ItemUsageContext pContext) {
		return getPlacementState(getPlacementContext(new ItemPlacementContext(pContext)));
	}

	public static boolean select(WorldAccess world, BlockPos pos, Vec3d lookVec, ItemStack heldItem) {
		BlockState blockState = world.getBlockState(pos);
		Block block = blockState.getBlock();
		if (!(block instanceof ITrackBlock))
			return false;

		ITrackBlock track = (ITrackBlock) block;
		Pair<Vec3d, AxisDirection> nearestTrackAxis = track.getNearestTrackAxis(world, pos, blockState, lookVec);
		Vec3d axis = nearestTrackAxis.getFirst()
			.multiply(nearestTrackAxis.getSecond() == AxisDirection.POSITIVE ? -1 : 1);
		Vec3d end = track.getCurveStart(world, pos, blockState, axis);
		Vec3d normal = track.getUpNormal(world, pos, blockState)
			.normalize();

		NbtCompound compoundTag = heldItem.getOrCreateSubNbt("ConnectingFrom");
		compoundTag.put("Pos", NbtHelper.fromBlockPos(pos));
		compoundTag.put("Axis", VecHelper.writeNBT(axis));
		compoundTag.put("Normal", VecHelper.writeNBT(normal));
		compoundTag.put("End", VecHelper.writeNBT(end));
		return true;
	}

	@Environment(EnvType.CLIENT)
	public static ActionResult sendExtenderPacket(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		ItemStack stack = player.getStackInHand(hand);
		if (!AllTags.AllBlockTags.TRACKS.matches(stack) || !stack.hasNbt())
			return ActionResult.PASS;
		if (MinecraftClient.getInstance().options.sprintKey.isPressed())
			AllPackets.getChannel()
				.sendToServer(new PlaceExtendedCurvePacket(hand == Hand.MAIN_HAND, true));
		return ActionResult.PASS;
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return stack.hasNbt() && stack.getNbt()
			.contains("ConnectingFrom");
	}

}
