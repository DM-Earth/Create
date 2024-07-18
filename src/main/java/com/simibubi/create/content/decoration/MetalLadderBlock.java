package com.simibubi.create.content.decoration;

import java.util.function.Predicate;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class MetalLadderBlock extends LadderBlock implements IWrenchable {

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public MetalLadderBlock(Settings p_54345_) {
		super(p_54345_);
	}

//	@Override // fabric: difficult to implement with little to gain
//	@Environment(EnvType.CLIENT)
//	public boolean supportsExternalFaceHiding(BlockState state) {
//		return false;
//	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean isSideInvisible(BlockState pState, BlockState pAdjacentBlockState, Direction pDirection) {
		return pDirection == Direction.UP && pAdjacentBlockState.getBlock() instanceof LadderBlock;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {
		if (player.isSneaking() || !player.canModifyBlocks())
			return ActionResult.PASS;
		ItemStack heldItem = player.getStackInHand(hand);
		IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
		return ActionResult.PASS;
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof MetalLadderBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return s -> s.getBlock() instanceof LadderBlock;
		}

		public int attachedLadders(World world, BlockPos pos, Direction direction) {
			BlockPos checkPos = pos.offset(direction);
			BlockState state = world.getBlockState(checkPos);
			int count = 0;
			while (getStatePredicate().test(state)) {
				count++;
				checkPos = checkPos.offset(direction);
				state = world.getBlockState(checkPos);
			}
			return count;
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			Direction dir = player.getPitch() < 0 ? Direction.UP : Direction.DOWN;

			int range = AllConfigs.server().equipment.placementAssistRange.get();
			if (player != null) {
				EntityAttributeInstance reach = player.getAttributeInstance(ReachEntityAttributes.REACH);
				if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier))
					range += 4;
			}

			int ladders = attachedLadders(world, pos, dir);
			if (ladders >= range)
				return PlacementOffset.fail();

			BlockPos newPos = pos.offset(dir, ladders + 1);
			BlockState newState = world.getBlockState(newPos);

			if (!state.canPlaceAt(world, newPos))
				return PlacementOffset.fail();

			if (newState.isReplaceable())
				return PlacementOffset.success(newPos, bState -> bState.with(FACING, state.get(FACING)));
			return PlacementOffset.fail();
		}

	}

}
