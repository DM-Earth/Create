package com.simibubi.create.content.kinetics.belt.item;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class BeltConnectorItem extends BlockItem {

	public BeltConnectorItem(Settings properties) {
		super(AllBlocks.BELT.get(), properties);
	}

	@Override
	public String getTranslationKey() {
		return getOrCreateTranslationKey();
	}


	@Nonnull
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity playerEntity = context.getPlayer();
		if (playerEntity != null && playerEntity.isSneaking()) {
			context.getStack()
					.setNbt(null);
			return ActionResult.SUCCESS;
		}

		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		boolean validAxis = validateAxis(world, pos);

		if (world.isClient)
			return validAxis ? ActionResult.SUCCESS : ActionResult.FAIL;

		NbtCompound tag = context.getStack()
				.getOrCreateNbt();
		BlockPos firstPulley = null;

		// Remove first if no longer existant or valid
		if (tag.contains("FirstPulley")) {
			firstPulley = NbtHelper.toBlockPos(tag.getCompound("FirstPulley"));
			if (!validateAxis(world, firstPulley) || !firstPulley.isWithinDistance(pos, maxLength() * 2)) {
				tag.remove("FirstPulley");
				context.getStack()
						.setNbt(tag);
			}
		}

		if (!validAxis || playerEntity == null)
			return ActionResult.FAIL;

		if (tag.contains("FirstPulley")) {

			if (!canConnect(world, firstPulley, pos))
				return ActionResult.FAIL;

			if (firstPulley != null && !firstPulley.equals(pos)) {
				createBelts(world, firstPulley, pos);
				AllAdvancements.BELT.awardTo(playerEntity);
				if (!playerEntity.isCreative())
					context.getStack()
							.decrement(1);
			}

			if (!context.getStack()
					.isEmpty()) {
				context.getStack()
						.setNbt(null);
				playerEntity.getItemCooldownManager()
						.set(this, 5);
			}
			return ActionResult.SUCCESS;
		}

		tag.put("FirstPulley", NbtHelper.fromBlockPos(pos));
		context.getStack()
				.setNbt(tag);
		playerEntity.getItemCooldownManager()
				.set(this, 5);
		return ActionResult.SUCCESS;
	}

	public static void createBelts(World world, BlockPos start, BlockPos end) {
		world.playSound(null, BlockPos.ofFloored(VecHelper.getCenterOf(start.add(end))
				.multiply(.5f)), SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 0.5F, 1F);

		BeltSlope slope = getSlopeBetween(start, end);
		Direction facing = getFacingFromTo(start, end);

		BlockPos diff = end.subtract(start);
		if (diff.getX() == diff.getZ())
			facing = Direction.get(facing.getDirection(), world.getBlockState(start)
					.get(Properties.AXIS) == Axis.X ? Axis.Z : Axis.X);

		List<BlockPos> beltsToCreate = getBeltChainBetween(start, end, slope, facing);
		BlockState beltBlock = AllBlocks.BELT.getDefaultState();
		boolean failed = false;

		for (BlockPos pos : beltsToCreate) {
			BlockState existingBlock = world.getBlockState(pos);
			if (existingBlock.getHardness(world, pos) == -1) {
				failed = true;
				break;
			}

			BeltPart part = pos.equals(start) ? BeltPart.START : pos.equals(end) ? BeltPart.END : BeltPart.MIDDLE;
			BlockState shaftState = world.getBlockState(pos);
			boolean pulley = ShaftBlock.isShaft(shaftState);
			if (part == BeltPart.MIDDLE && pulley)
				part = BeltPart.PULLEY;
			if (pulley && shaftState.get(AbstractSimpleShaftBlock.AXIS) == Axis.Y)
				slope = BeltSlope.SIDEWAYS;

			if (!existingBlock.isReplaceable())
				world.breakBlock(pos, false);

			KineticBlockEntity.switchToBlockState(world, pos,
				ProperWaterloggedBlock.withWater(world, beltBlock.with(BeltBlock.SLOPE, slope)
					.with(BeltBlock.PART, part)
					.with(BeltBlock.HORIZONTAL_FACING, facing), pos));
		}

		if (!failed)
			return;

		for (BlockPos pos : beltsToCreate)
			if (AllBlocks.BELT.has(world.getBlockState(pos)))
				world.breakBlock(pos, false);
	}

	private static Direction getFacingFromTo(BlockPos start, BlockPos end) {
		Axis beltAxis = start.getX() == end.getX() ? Axis.Z : Axis.X;
		BlockPos diff = end.subtract(start);
		AxisDirection axisDirection = AxisDirection.POSITIVE;

		if (diff.getX() == 0 && diff.getZ() == 0)
			axisDirection = diff.getY() > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;
		else
			axisDirection =
					beltAxis.choose(diff.getX(), 0, diff.getZ()) > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;

		return Direction.get(axisDirection, beltAxis);
	}

	private static BeltSlope getSlopeBetween(BlockPos start, BlockPos end) {
		BlockPos diff = end.subtract(start);

		if (diff.getY() != 0) {
			if (diff.getZ() != 0 || diff.getX() != 0)
				return diff.getY() > 0 ? BeltSlope.UPWARD : BeltSlope.DOWNWARD;
			return BeltSlope.VERTICAL;
		}
		return BeltSlope.HORIZONTAL;
	}

	private static List<BlockPos> getBeltChainBetween(BlockPos start, BlockPos end, BeltSlope slope,
													  Direction direction) {
		List<BlockPos> positions = new LinkedList<>();
		int limit = 1000;
		BlockPos current = start;

		do {
			positions.add(current);

			if (slope == BeltSlope.VERTICAL) {
				current = current.up(direction.getDirection() == AxisDirection.POSITIVE ? 1 : -1);
				continue;
			}

			current = current.offset(direction);
			if (slope != BeltSlope.HORIZONTAL)
				current = current.up(slope == BeltSlope.UPWARD ? 1 : -1);

		} while (!current.equals(end) && limit-- > 0);

		positions.add(end);
		return positions;
	}

	public static boolean canConnect(World world, BlockPos first, BlockPos second) {
		if (!world.canSetBlock(first) || !world.canSetBlock(second))
			return false;
		if (!second.isWithinDistance(first, maxLength()))
			return false;

		BlockPos diff = second.subtract(first);
		Axis shaftAxis = world.getBlockState(first)
				.get(Properties.AXIS);

		int x = diff.getX();
		int y = diff.getY();
		int z = diff.getZ();
		int sames = ((Math.abs(x) == Math.abs(y)) ? 1 : 0) + ((Math.abs(y) == Math.abs(z)) ? 1 : 0)
				+ ((Math.abs(z) == Math.abs(x)) ? 1 : 0);

		if (shaftAxis.choose(x, y, z) != 0)
			return false;
		if (sames != 1)
			return false;
		if (shaftAxis != world.getBlockState(second)
				.get(Properties.AXIS))
			return false;
		if (shaftAxis == Axis.Y && x != 0 && z != 0)
			return false;

		BlockEntity blockEntity = world.getBlockEntity(first);
		BlockEntity blockEntity2 = world.getBlockEntity(second);

		if (!(blockEntity instanceof KineticBlockEntity))
			return false;
		if (!(blockEntity2 instanceof KineticBlockEntity))
			return false;

		float speed1 = ((KineticBlockEntity) blockEntity).getTheoreticalSpeed();
		float speed2 = ((KineticBlockEntity) blockEntity2).getTheoreticalSpeed();
		if (Math.signum(speed1) != Math.signum(speed2) && speed1 != 0 && speed2 != 0)
			return false;

		BlockPos step = BlockPos.ofFloored(Math.signum(diff.getX()), Math.signum(diff.getY()), Math.signum(diff.getZ()));
		int limit = 1000;
		for (BlockPos currentPos = first.add(step); !currentPos.equals(second) && limit-- > 0; currentPos =
				currentPos.add(step)) {
			BlockState blockState = world.getBlockState(currentPos);
			if (ShaftBlock.isShaft(blockState) && blockState.get(AbstractSimpleShaftBlock.AXIS) == shaftAxis)
				continue;
			if (!blockState.isReplaceable())
				return false;
		}

		return true;

	}

	public static Integer maxLength() {
		return AllConfigs.server().kinetics.maxBeltLength.get();
	}

	public static boolean validateAxis(World world, BlockPos pos) {
		if (!world.canSetBlock(pos))
			return false;
		if (!ShaftBlock.isShaft(world.getBlockState(pos)))
			return false;
		return true;
	}

}
