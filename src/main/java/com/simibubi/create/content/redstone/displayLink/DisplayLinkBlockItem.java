package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTarget;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.item.BlockUseBypassingItem;
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
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class DisplayLinkBlockItem extends BlockItem implements BlockUseBypassingItem {

	public DisplayLinkBlockItem(Block pBlock, Settings pProperties) {
		super(pBlock, pProperties);
	}

	// fabric: handled by BlockUseBypassingItem
//	public static InteractionResult gathererItemAlwaysPlacesWhenUsed(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
//		ItemStack usedItem = player.getItemInHand(hand);
//		if (usedItem.getItem() instanceof DisplayLinkBlockItem) {
//			if (AllBlocks.DISPLAY_LINK.has(level
//				.getBlockState(hitResult.getBlockPos())))
//				return InteractionResult.PASS;
//			return InteractionResult.FAIL;
//		}
//		return InteractionResult.PASS;
//	}

	@Override
	public boolean shouldBypass(BlockState state, BlockPos pos, World level, PlayerEntity player, Hand hand) {
		ItemStack usedItem = player.getStackInHand(hand);
		if (usedItem.getItem() instanceof DisplayLinkBlockItem) {
			if (!AllBlocks.DISPLAY_LINK.has(state))
				return true;
		}
		return false;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext pContext) {
		ItemStack stack = pContext.getStack();
		BlockPos pos = pContext.getBlockPos();
		World level = pContext.getWorld();
		BlockState state = level.getBlockState(pos);
		PlayerEntity player = pContext.getPlayer();

		if (player == null)
			return ActionResult.FAIL;

		if (player.isSneaking() && stack.hasNbt()) {
			if (level.isClient)
				return ActionResult.SUCCESS;
			player.sendMessage(Lang.translateDirect("display_link.clear"), true);
			stack.setNbt(null);
			return ActionResult.SUCCESS;
		}

		if (!stack.hasNbt()) {
			if (level.isClient)
				return ActionResult.SUCCESS;
			NbtCompound stackTag = stack.getOrCreateNbt();
			stackTag.put("SelectedPos", NbtHelper.fromBlockPos(pos));
			player.sendMessage(Lang.translateDirect("display_link.set"), true);
			stack.setNbt(stackTag);
			return ActionResult.SUCCESS;
		}

		NbtCompound tag = stack.getNbt();
		NbtCompound teTag = new NbtCompound();

		BlockPos selectedPos = NbtHelper.toBlockPos(tag.getCompound("SelectedPos"));
		BlockPos placedPos = pos.offset(pContext.getSide(), state.isReplaceable() ? 0 : 1);

		if (!selectedPos.isWithinDistance(placedPos, AllConfigs.server().logistics.displayLinkRange.get())) {
			player.sendMessage(Lang.translateDirect("display_link.too_far")
				.formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}

		teTag.put("TargetOffset", NbtHelper.fromBlockPos(selectedPos.subtract(placedPos)));
		tag.put("BlockEntityTag", teTag);

		ActionResult useOn = super.useOnBlock(pContext);
		if (level.isClient || useOn == ActionResult.FAIL)
			return useOn;

		ItemStack itemInHand = player.getStackInHand(pContext.getHand());
		if (!itemInHand.isEmpty())
			itemInHand.setNbt(null);
		player.sendMessage(Lang.translateDirect("display_link.success")
			.formatted(Formatting.GREEN), true);
		return useOn;
	}

	private static BlockPos lastShownPos = null;
	private static Box lastShownAABB = null;

	@Environment(EnvType.CLIENT)
	public static void clientTick() {
		PlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null)
			return;
		ItemStack heldItemMainhand = player.getMainHandStack();
		if (!(heldItemMainhand.getItem() instanceof DisplayLinkBlockItem))
			return;
		if (!heldItemMainhand.hasNbt())
			return;
		NbtCompound stackTag = heldItemMainhand.getOrCreateNbt();
		if (!stackTag.contains("SelectedPos"))
			return;

		BlockPos selectedPos = NbtHelper.toBlockPos(stackTag.getCompound("SelectedPos"));

		if (!selectedPos.equals(lastShownPos)) {
			lastShownAABB = getBounds(selectedPos);
			lastShownPos = selectedPos;
		}

		CreateClient.OUTLINER.showAABB("target", lastShownAABB)
			.colored(0xffcb74)
			.lineWidth(1 / 16f);
	}

	@Environment(EnvType.CLIENT)
	private static Box getBounds(BlockPos pos) {
		World world = MinecraftClient.getInstance().world;
		DisplayTarget target = AllDisplayBehaviours.targetOf(world, pos);

		if (target != null)
			return target.getMultiblockBounds(world, pos);

		BlockState state = world.getBlockState(pos);
		VoxelShape shape = state.getOutlineShape(world, pos);
		return shape.isEmpty() ? new Box(BlockPos.ORIGIN)
			: shape.getBoundingBox()
				.offset(pos);
	}

}
