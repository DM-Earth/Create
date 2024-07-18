package com.simibubi.create.content.equipment.symmetryWand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.equipment.symmetryWand.mirror.CrossPlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.EmptyMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.PlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SymmetryWandItem extends Item {

	public static final String SYMMETRY = "symmetry";
	private static final String ENABLE = "enable";

	public SymmetryWandItem(Settings properties) {
		super(properties);
	}

	@Nonnull
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		BlockPos pos = context.getBlockPos();
		if (player == null)
			return ActionResult.PASS;
		player.getItemCooldownManager()
			.set(this, 5);
		ItemStack wand = player.getStackInHand(context.getHand());
		checkNBT(wand);

		// Shift -> open GUI
		if (player.isSneaking()) {
			if (player.getWorld().isClient) {
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
					openWandGUI(wand, context.getHand());
				});
				player.getItemCooldownManager()
					.set(this, 5);
			}
			return ActionResult.SUCCESS;
		}

		if (context.getWorld().isClient || context.getHand() != Hand.MAIN_HAND)
			return ActionResult.SUCCESS;

		NbtCompound compound = wand.getNbt()
			.getCompound(SYMMETRY);
		pos = pos.offset(context.getSide());
		SymmetryMirror previousElement = SymmetryMirror.fromNBT(compound);

		// No Shift -> Make / Move Mirror
		wand.getNbt()
			.putBoolean(ENABLE, true);
		Vec3d pos3d = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
		SymmetryMirror newElement = new PlaneMirror(pos3d);

		if (previousElement instanceof EmptyMirror) {
			newElement.setOrientation(
				(player.getHorizontalFacing() == Direction.NORTH || player.getHorizontalFacing() == Direction.SOUTH)
					? PlaneMirror.Align.XY.ordinal()
					: PlaneMirror.Align.YZ.ordinal());
			newElement.enable = true;
			wand.getNbt()
				.putBoolean(ENABLE, true);

		} else {
			previousElement.setPosition(pos3d);

			if (previousElement instanceof PlaneMirror) {
				previousElement.setOrientation(
					(player.getHorizontalFacing() == Direction.NORTH || player.getHorizontalFacing() == Direction.SOUTH)
						? PlaneMirror.Align.XY.ordinal()
						: PlaneMirror.Align.YZ.ordinal());
			}

			if (previousElement instanceof CrossPlaneMirror) {
				float rotation = player.getHeadYaw();
				float abs = Math.abs(rotation % 90);
				boolean diagonal = abs > 22 && abs < 45 + 22;
				previousElement
					.setOrientation(diagonal ? CrossPlaneMirror.Align.D.ordinal() : CrossPlaneMirror.Align.Y.ordinal());
			}

			newElement = previousElement;
		}

		compound = newElement.writeToNbt();
		wand.getNbt()
			.put(SYMMETRY, compound);

		player.setStackInHand(context.getHand(), wand);
		return ActionResult.SUCCESS;
	}

	@Override
	public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
		ItemStack wand = playerIn.getStackInHand(handIn);
		checkNBT(wand);

		// Shift -> Open GUI
		if (playerIn.isSneaking()) {
			if (worldIn.isClient) {
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
					openWandGUI(playerIn.getStackInHand(handIn), handIn);
				});
				playerIn.getItemCooldownManager()
					.set(this, 5);
			}
			return new TypedActionResult<ItemStack>(ActionResult.SUCCESS, wand);
		}

		// No Shift -> Clear Mirror
		wand.getNbt()
			.putBoolean(ENABLE, false);
		return new TypedActionResult<ItemStack>(ActionResult.SUCCESS, wand);
	}

	@Environment(EnvType.CLIENT)
	private void openWandGUI(ItemStack wand, Hand hand) {
		ScreenOpener.open(new SymmetryWandScreen(wand, hand));
	}

	private static void checkNBT(ItemStack wand) {
		if (!wand.hasNbt() || !wand.getNbt()
			.contains(SYMMETRY)) {
			wand.setNbt(new NbtCompound());
			wand.getNbt()
				.put(SYMMETRY, new EmptyMirror(new Vec3d(0, 0, 0)).writeToNbt());
			wand.getNbt()
				.putBoolean(ENABLE, false);
		}
	}

	public static boolean isEnabled(ItemStack stack) {
		checkNBT(stack);
		NbtCompound tag = stack.getNbt();
		return tag.getBoolean(ENABLE) && !tag.getBoolean("Simulate");
	}

	public static SymmetryMirror getMirror(ItemStack stack) {
		checkNBT(stack);
		return SymmetryMirror.fromNBT(stack.getNbt()
			.getCompound(SYMMETRY));
	}

	public static void configureSettings(ItemStack stack, SymmetryMirror mirror) {
		checkNBT(stack);
		stack.getNbt().put(SYMMETRY, mirror.writeToNbt());
	}

	public static void apply(World world, ItemStack wand, PlayerEntity player, BlockPos pos, BlockState block) {
		checkNBT(wand);
		if (!isEnabled(wand))
			return;
		if (!BlockItem.BLOCK_ITEMS.containsKey(block.getBlock()))
			return;

		Map<BlockPos, BlockState> blockSet = new HashMap<>();
		blockSet.put(pos, block);
		SymmetryMirror symmetry = SymmetryMirror.fromNBT((NbtCompound) wand.getNbt()
			.getCompound(SYMMETRY));

		Vec3d mirrorPos = symmetry.getPosition();
		if (mirrorPos.distanceTo(Vec3d.of(pos)) > AllConfigs.server().equipment.maxSymmetryWandRange.get())
			return;
		if (!player.isCreative() && isHoldingBlock(player, block)
			&& BlockHelper.simulateFindAndRemoveInInventory(block, player, 1) == 0) // fabric: simulate since the first block will already be removed
			return;

		symmetry.process(blockSet);
		BlockPos to = BlockPos.ofFloored(mirrorPos);
		List<BlockPos> targets = new ArrayList<>();
		targets.add(pos);

		for (BlockPos position : blockSet.keySet()) {
			if (position.equals(pos))
				continue;

			if (world.canPlace(block, position, ShapeContext.of(player))) {
				BlockState blockState = blockSet.get(position);
				for (Direction face : Iterate.directions)
					blockState = blockState.getStateForNeighborUpdate(face, world.getBlockState(position.offset(face)), world,
						position, position.offset(face));

				if (player.isCreative()) {
					world.setBlockState(position, blockState);
					targets.add(position);
					continue;
				}

				BlockState toReplace = world.getBlockState(position);
				if (!toReplace.isReplaceable())
					continue;
				if (toReplace.getHardness(world, position) == -1)
					continue;

				if (AllBlocks.CART_ASSEMBLER.has(blockState)) {
					BlockState railBlock = CartAssemblerBlock.getRailBlock(blockState);
					if (BlockHelper.findAndRemoveInInventory(railBlock, player, 1) == 0)
						continue;
					if (BlockHelper.findAndRemoveInInventory(blockState, player, 1) == 0)
						blockState = railBlock;
				} else {
					if (BlockHelper.findAndRemoveInInventory(blockState, player, 1) == 0)
						continue;
				}

//				BlockSnapshot blocksnapshot = BlockSnapshot.create(world.dimension(), world, position);
				BlockState cachedState = world.getBlockState(position);
				FluidState ifluidstate = world.getFluidState(position);
				world.setBlockState(position, ifluidstate.getBlockState(), Block.FORCE_STATE);
				world.setBlockState(position, blockState);

				NbtCompound wandNbt = wand.getOrCreateNbt();
				wandNbt.putBoolean("Simulate", true);
				boolean placeInterrupted = !world.canPlace(cachedState, position, ShapeContext.absent());//ForgeEventFactory.onBlockPlace(player, blocksnapshot, Direction.UP);
				wandNbt.putBoolean("Simulate", false);

				if (placeInterrupted) {
//					blocksnapshot.restore(true, false);
					world.setBlockState(position, cachedState);
					continue;
				}
				targets.add(position);
			}
		}

		AllPackets.getChannel().sendToClientsTrackingAndSelf(new SymmetryEffectPacket(to, targets), player);
	}

	private static boolean isHoldingBlock(PlayerEntity player, BlockState block) {
		ItemStack itemBlock = BlockHelper.getRequiredItem(block);
		return player.isHolding(itemBlock.getItem());
	}

	public static void remove(World world, ItemStack wand, PlayerEntity player, BlockPos pos, BlockState ogBlock) {
		BlockState air = Blocks.AIR.getDefaultState();
		checkNBT(wand);
		if (!isEnabled(wand))
			return;

		Map<BlockPos, BlockState> blockSet = new HashMap<>();
		blockSet.put(pos, air);
		SymmetryMirror symmetry = SymmetryMirror.fromNBT((NbtCompound) wand.getNbt()
			.getCompound(SYMMETRY));

		Vec3d mirrorPos = symmetry.getPosition();
		if (mirrorPos.distanceTo(Vec3d.of(pos)) > AllConfigs.server().equipment.maxSymmetryWandRange.get())
			return;

		symmetry.process(blockSet);

		BlockPos to = BlockPos.ofFloored(mirrorPos);
		List<BlockPos> targets = new ArrayList<>();

		targets.add(pos);
		for (BlockPos position : blockSet.keySet()) {
			if (!player.isCreative() && ogBlock.getBlock() != world.getBlockState(position)
					.getBlock())
				continue;
			if (position.equals(pos))
				continue;

			BlockState blockstate = world.getBlockState(position);
			BlockEntity be = blockstate.hasBlockEntity() ? world.getBlockEntity(position) : null;
			if (!blockstate.isAir()) {
				if (handlePreEvent(world, player, position, blockstate, be))
					continue;
				targets.add(position);
				world.syncWorldEvent(2001, position, Block.getRawIdFromState(blockstate));
				world.setBlockState(position, air, 3);

				if (!player.isCreative()) {
					if (!player.getMainHandStack()
						.isEmpty())
						player.getMainHandStack()
							.postMine(world, blockstate, position, player);
					BlockEntity blockEntity = blockstate.hasBlockEntity() ? world.getBlockEntity(position) : null;
					Block.dropStacks(blockstate, world, pos, blockEntity, player, player.getMainHandStack()); // Add fortune, silk touch and other loot modifiers
				}
				handlePostEvent(world, player, position, blockstate, be);
			}
		}

		AllPackets.getChannel().sendToClientsTrackingAndSelf(new SymmetryEffectPacket(to, targets), player);
	}

	/**
	 * Handling firing events before the wand changes blocks.
	 * @return true if canceled
	 */
	public static boolean handlePreEvent(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity be) {
		if (PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, state, be)) {
			return false;
		}
		PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(world, player, pos, state, be);
		return true;
	}

	public static void handlePostEvent(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity be) {
		PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(world, player, pos, state, be);
	}

//	@Override
//	@Environment(EnvType.CLIENT)
//	public void initializeClient(Consumer<IItemRenderProperties> consumer) {
//		consumer.accept(SimpleCustomRenderer.create(this, new SymmetryWandItemRenderer()));
//	}

}
