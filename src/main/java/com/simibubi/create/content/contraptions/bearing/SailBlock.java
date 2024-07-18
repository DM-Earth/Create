package com.simibubi.create.content.contraptions.bearing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import io.github.fabricators_of_create.porting_lib.util.TagUtil;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SailBlock extends WrenchableDirectionalBlock implements BlockPickInteractionAware {

	public static SailBlock frame(Settings properties) {
		return new SailBlock(properties, true, null);
	}

	public static SailBlock withCanvas(Settings properties, DyeColor color) {
		return new SailBlock(properties, false, color);
	}

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	protected final boolean frame;
	protected final DyeColor color;

	protected SailBlock(Settings properties, boolean frame, DyeColor color) {
		super(properties);
		this.frame = frame;
		this.color = color;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState state = super.getPlacementState(context);
		return state.with(FACING, state.get(FACING)
			.getOpposite());
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {
		ItemStack heldItem = player.getStackInHand(hand);

		IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
		if (!player.isSneaking() && player.canModifyBlocks()) {
			if (placementHelper.matchesItem(heldItem)) {
				placementHelper.getOffset(player, world, state, pos, ray)
					.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
				return ActionResult.SUCCESS;
			}
		}

		if (heldItem.getItem() instanceof ShearsItem) {
			if (!world.isClient)
				world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1.0f, 1.0f);
				applyDye(state, world, pos, ray.getPos(), null);
			return ActionResult.SUCCESS;
		}

		if (frame)
			return ActionResult.PASS;

		DyeColor color = TagUtil.getColorFromStack(heldItem);

		if (color != null) {
			if (!world.isClient)
				world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1.0f, 1.1f - world.random.nextFloat() * .2f);
				applyDye(state, world, pos, ray.getPos(), color);
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	public void applyDye(BlockState state, World world, BlockPos pos, Vec3d hit, @Nullable DyeColor color) {
		BlockState newState =
			(color == null ? AllBlocks.SAIL_FRAME : AllBlocks.DYED_SAILS.get(color)).getDefaultState();
		newState = BlockHelper.copyProperties(state, newState);

		// Dye the block itself
		if (state != newState) {
			world.setBlockState(pos, newState);
			return;
		}

		// Dye all adjacent
		List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, hit, state.get(FACING)
			.getAxis());
		for (Direction d : directions) {
			BlockPos offset = pos.offset(d);
			BlockState adjacentState = world.getBlockState(offset);
			Block block = adjacentState.getBlock();
			if (!(block instanceof SailBlock) || ((SailBlock) block).frame)
				continue;
			if (state.get(FACING) != adjacentState.get(FACING))
				continue;
			if (state == adjacentState)
				continue;
			world.setBlockState(offset, newState);
			return;
		}

		// Dye all the things
		List<BlockPos> frontier = new ArrayList<>();
		frontier.add(pos);
		Set<BlockPos> visited = new HashSet<>();
		int timeout = 100;
		while (!frontier.isEmpty()) {
			if (timeout-- < 0)
				break;

			BlockPos currentPos = frontier.remove(0);
			visited.add(currentPos);

			for (Direction d : Iterate.directions) {
				if (d.getAxis() == state.get(FACING)
					.getAxis())
					continue;
				BlockPos offset = currentPos.offset(d);
				if (visited.contains(offset))
					continue;
				BlockState adjacentState = world.getBlockState(offset);
				Block block = adjacentState.getBlock();
				if (!(block instanceof SailBlock) || ((SailBlock) block).frame && color != null)
					continue;
				if (adjacentState.get(FACING) != state.get(FACING))
					continue;
				if (state != adjacentState)
					world.setBlockState(offset, newState);
				frontier.add(offset);
				visited.add(offset);
			}
		}
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		return (frame ? AllShapes.SAIL_FRAME : AllShapes.SAIL).get(state.get(FACING));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView p_220071_2_, BlockPos p_220071_3_,
		ShapeContext p_220071_4_) {
		if (frame)
			return AllShapes.SAIL_FRAME_COLLISION.get(state.get(FACING));
		return getOutlineShape(state, p_220071_2_, p_220071_3_, p_220071_4_);
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockView view, BlockPos pos,
		@Nullable PlayerEntity player, @Nullable HitResult result) {
		ItemStack pickBlock = new ItemStack(this);
		if (pickBlock.isEmpty())
			return AllBlocks.SAIL.get()
					.getPickedStack(state, view, pos, player, result);
		return pickBlock;
	}

	@Override
	public void onLandedUpon(World p_152426_, BlockState p_152427_, BlockPos p_152428_, Entity p_152429_, float p_152430_) {
		if (frame)
			super.onLandedUpon(p_152426_, p_152427_, p_152428_, p_152429_, p_152430_);
		super.onLandedUpon(p_152426_, p_152427_, p_152428_, p_152429_, 0);
	}

	public void onEntityLand(BlockView p_176216_1_, Entity p_176216_2_) {
		if (frame || p_176216_2_.bypassesLandingEffects()) {
			super.onEntityLand(p_176216_1_, p_176216_2_);
		} else {
			this.bounce(p_176216_2_);
		}
	}

	private void bounce(Entity p_226860_1_) {
		Vec3d Vector3d = p_226860_1_.getVelocity();
		if (Vector3d.y < 0.0D) {
			double d0 = p_226860_1_ instanceof LivingEntity ? 1.0D : 0.8D;
			p_226860_1_.setVelocity(Vector3d.x, -Vector3d.y * (double) 0.26F * d0, Vector3d.z);
		}

	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	public boolean isFrame() {
		return frame;
	}

	public DyeColor getColor() {
		return color;
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {
		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> AllBlocks.SAIL.isIn(i) || AllBlocks.SAIL_FRAME.isIn(i);
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return s -> s.getBlock() instanceof SailBlock;
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getPos(),
				state.get(SailBlock.FACING)
					.getAxis(),
				dir -> world.getBlockState(pos.offset(dir))
					.isReplaceable());

			if (directions.isEmpty())
				return PlacementOffset.fail();
			else {
				return PlacementOffset.success(pos.offset(directions.get(0)),
					s -> s.with(FACING, state.get(FACING)));
			}
		}
	}
}
