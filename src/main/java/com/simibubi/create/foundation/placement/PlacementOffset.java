package com.simibubi.create.foundation.placement;

import java.util.function.Function;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public class PlacementOffset {

	private final boolean success;
	private Vec3i pos;
	private Function<BlockState, BlockState> stateTransform;
	private BlockState ghostState;

	private PlacementOffset(boolean success) {
		this.success = success;
		this.pos = BlockPos.ORIGIN;
		this.stateTransform = Function.identity();
		this.ghostState = null;
	}

	public static PlacementOffset fail() {
		return new PlacementOffset(false);
	}

	public static PlacementOffset success() {
		return new PlacementOffset(true);
	}

	public static PlacementOffset success(Vec3i pos) {
		return success().at(pos);
	}

	public static PlacementOffset success(Vec3i pos, Function<BlockState, BlockState> transform) {
		return success().at(pos).withTransform(transform);
	}

	public PlacementOffset at(Vec3i pos) {
		this.pos = pos;
		return this;
	}

	public PlacementOffset withTransform(Function<BlockState, BlockState> stateTransform) {
		this.stateTransform = stateTransform;
		return this;
	}

	public PlacementOffset withGhostState(BlockState ghostState) {
		this.ghostState = ghostState;
		return this;
	}

	public boolean isSuccessful() {
		return success;
	}

	public Vec3i getPos() {
		return pos;
	}

	public BlockPos getBlockPos() {
		if (pos instanceof BlockPos)
			return (BlockPos) pos;

		return new BlockPos(pos);
	}

	public Function<BlockState, BlockState> getTransform() {
		return stateTransform;
	}

	public boolean hasGhostState() {
		return ghostState != null;
	}

	public BlockState getGhostState() {
		return ghostState;
	}

	public boolean isReplaceable(World world) {
		if (!success)
			return false;

		return world.getBlockState(new BlockPos(pos)).isReplaceable();
	}

	public ActionResult placeInWorld(World world, BlockItem blockItem, PlayerEntity player, Hand hand, BlockHitResult ray) {

		if (!isReplaceable(world))
			return ActionResult.PASS;

		if (world.isClient)
			return ActionResult.SUCCESS;

		ItemUsageContext context = new ItemUsageContext(player, hand, ray);
		BlockPos newPos = new BlockPos(pos);
		ItemStack stackBefore = player.getStackInHand(hand)
			.copy();

		if (!world.canPlayerModifyAt(player, newPos))
			return ActionResult.PASS;

		BlockState state = stateTransform.apply(blockItem.getBlock().getDefaultState());
		if (state.contains(Properties.WATERLOGGED)) {
			FluidState fluidState = world.getFluidState(newPos);
			state = state.with(Properties.WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
		}

//		BlockSnapshot snapshot = BlockSnapshot.create(world.dimension(), world, newPos);
		world.setBlockState(newPos, state);

//		BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(snapshot, IPlacementHelper.ID, player);
//		if (MinecraftForge.EVENT_BUS.post(event)) {
//			snapshot.restore(true, false);
//			return InteractionResult.FAIL;
//		}

		BlockState newState = world.getBlockState(newPos);
		BlockSoundGroup soundtype = newState.getSoundGroup();
		world.playSound(null, newPos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

		player.incrementStat(Stats.USED.getOrCreateStat(blockItem));
		newState.getBlock()
			.onPlaced(world, newPos, newState, player, stackBefore);

		if (player instanceof ServerPlayerEntity)
			Criteria.PLACED_BLOCK.trigger((ServerPlayerEntity) player, newPos, context.getStack());

		if (!player.isCreative())
			context.getStack().decrement(1);

		return ActionResult.SUCCESS;
	}
}
