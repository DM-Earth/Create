package com.simibubi.create.content.contraptions;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BellBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.DyedCarpetBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.contraptions.actors.AttachedActorBlock;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterBlock;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlock;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.SailBlock;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.content.contraptions.chassis.StickerBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.content.contraptions.pulley.PulleyBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.fan.NozzleBlock;
import com.simibubi.create.content.logistics.vault.ItemVaultBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.station.StationBlock;
import com.simibubi.create.content.trains.track.ITrackBlock;

public class BlockMovementChecks {

	private static final List<MovementNecessaryCheck> MOVEMENT_NECESSARY_CHECKS = new ArrayList<>();
	private static final List<MovementAllowedCheck> MOVEMENT_ALLOWED_CHECKS = new ArrayList<>();
	private static final List<BrittleCheck> BRITTLE_CHECKS = new ArrayList<>();
	private static final List<AttachedCheck> ATTACHED_CHECKS = new ArrayList<>();
	private static final List<NotSupportiveCheck> NOT_SUPPORTIVE_CHECKS = new ArrayList<>();

	// Registration
	// Add new checks to the front instead of the end

	public static void registerMovementNecessaryCheck(MovementNecessaryCheck check) {
		MOVEMENT_NECESSARY_CHECKS.add(0, check);
	}

	public static void registerMovementAllowedCheck(MovementAllowedCheck check) {
		MOVEMENT_ALLOWED_CHECKS.add(0, check);
	}

	public static void registerBrittleCheck(BrittleCheck check) {
		BRITTLE_CHECKS.add(0, check);
	}

	public static void registerAttachedCheck(AttachedCheck check) {
		ATTACHED_CHECKS.add(0, check);
	}

	public static void registerNotSupportiveCheck(NotSupportiveCheck check) {
		NOT_SUPPORTIVE_CHECKS.add(0, check);
	}

	public static void registerAllChecks(AllChecks checks) {
		registerMovementNecessaryCheck(checks);
		registerMovementAllowedCheck(checks);
		registerBrittleCheck(checks);
		registerAttachedCheck(checks);
		registerNotSupportiveCheck(checks);
	}

	// Actual check methods

	public static boolean isMovementNecessary(BlockState state, World world, BlockPos pos) {
		for (MovementNecessaryCheck check : MOVEMENT_NECESSARY_CHECKS) {
			CheckResult result = check.isMovementNecessary(state, world, pos);
			if (result != CheckResult.PASS) {
				return result.toBoolean();
			}
		}
		return isMovementNecessaryFallback(state, world, pos);
	}

	public static boolean isMovementAllowed(BlockState state, World world, BlockPos pos) {
		for (MovementAllowedCheck check : MOVEMENT_ALLOWED_CHECKS) {
			CheckResult result = check.isMovementAllowed(state, world, pos);
			if (result != CheckResult.PASS) {
				return result.toBoolean();
			}
		}
		return isMovementAllowedFallback(state, world, pos);
	}

	/**
	 * Brittle blocks will be collected first, as they may break when other blocks
	 * are removed before them
	 */
	public static boolean isBrittle(BlockState state) {
		for (BrittleCheck check : BRITTLE_CHECKS) {
			CheckResult result = check.isBrittle(state);
			if (result != CheckResult.PASS) {
				return result.toBoolean();
			}
		}
		return isBrittleFallback(state);
	}

	/**
	 * Attached blocks will move if blocks they are attached to are moved
	 */
	public static boolean isBlockAttachedTowards(BlockState state, World world, BlockPos pos, Direction direction) {
		for (AttachedCheck check : ATTACHED_CHECKS) {
			CheckResult result = check.isBlockAttachedTowards(state, world, pos, direction);
			if (result != CheckResult.PASS) {
				return result.toBoolean();
			}
		}
		return isBlockAttachedTowardsFallback(state, world, pos, direction);
	}

	/**
	 * Non-Supportive blocks will not continue a chain of blocks picked up by e.g. a
	 * piston
	 */
	public static boolean isNotSupportive(BlockState state, Direction facing) {
		for (NotSupportiveCheck check : NOT_SUPPORTIVE_CHECKS) {
			CheckResult result = check.isNotSupportive(state, facing);
			if (result != CheckResult.PASS) {
				return result.toBoolean();
			}
		}
		return isNotSupportiveFallback(state, facing);
	}

	// Fallback checks

	private static boolean isMovementNecessaryFallback(BlockState state, World world, BlockPos pos) {
		if (isBrittle(state))
			return true;
		if (AllBlockTags.MOVABLE_EMPTY_COLLIDER.matches(state))
			return true;
		if (state.getCollisionShape(world, pos)
			.isEmpty())
			return false;
		if (state.isReplaceable())
			return false;
		return true;
	}

	private static boolean isMovementAllowedFallback(BlockState state, World world, BlockPos pos) {
		Block block = state.getBlock();
		if (block instanceof AbstractChassisBlock)
			return true;
		if (state.getHardness(world, pos) == -1)
			return false;
		if (AllBlockTags.RELOCATION_NOT_SUPPORTED.matches(state))
			return false;
		if (AllBlockTags.NON_MOVABLE.matches(state))
			return false;
		if (ContraptionMovementSetting.get(state.getBlock()) == ContraptionMovementSetting.UNMOVABLE)
			return false;

		// Move controllers only when they aren't moving
		if (block instanceof MechanicalPistonBlock && state.get(MechanicalPistonBlock.STATE) != PistonState.MOVING)
			return true;
		if (block instanceof MechanicalBearingBlock) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof MechanicalBearingBlockEntity)
				return !((MechanicalBearingBlockEntity) be).isRunning();
		}
		if (block instanceof ClockworkBearingBlock) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof ClockworkBearingBlockEntity)
				return !((ClockworkBearingBlockEntity) be).isRunning();
		}
		if (block instanceof PulleyBlock) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof PulleyBlockEntity)
				return !((PulleyBlockEntity) be).running;
		}

		if (AllBlocks.BELT.has(state))
			return true;
		if (state.getBlock() instanceof GrindstoneBlock)
			return true;
		if (state.getBlock() instanceof ITrackBlock)
			return false;
		if (state.getBlock() instanceof StationBlock)
			return false;
		return state.getPistonBehavior() != PistonBehavior.BLOCK;
	}

	private static boolean isBrittleFallback(BlockState state) {
		Block block = state.getBlock();
		if (state.contains(Properties.HANGING))
			return true;

		if (block instanceof LadderBlock)
			return true;
		if (block instanceof TorchBlock)
			return true;
		if (block instanceof AbstractSignBlock)
			return true;
		if (block instanceof AbstractPressurePlateBlock)
			return true;
		if (block instanceof WallMountedBlock && !(block instanceof GrindstoneBlock))
			return true;
		if (block instanceof CartAssemblerBlock)
			return false;
		if (block instanceof AbstractRailBlock)
			return true;
		if (block instanceof AbstractRedstoneGateBlock)
			return true;
		if (block instanceof RedstoneWireBlock)
			return true;
		if (block instanceof DyedCarpetBlock)
			return true;
		if (block instanceof WhistleBlock)
			return true;
		if (block instanceof WhistleExtenderBlock)
			return true;
		return AllBlockTags.BRITTLE.matches(state);
	}

	private static boolean isBlockAttachedTowardsFallback(BlockState state, World world, BlockPos pos,
		Direction direction) {
		Block block = state.getBlock();
		if (block instanceof LadderBlock)
			return state.get(LadderBlock.FACING) == direction.getOpposite();
		if (block instanceof WallTorchBlock)
			return state.get(WallTorchBlock.FACING) == direction.getOpposite();
		if (block instanceof WallSignBlock)
			return state.get(WallSignBlock.FACING) == direction.getOpposite();
		if (block instanceof SignBlock)
			return direction == Direction.DOWN;
		if (block instanceof AbstractPressurePlateBlock)
			return direction == Direction.DOWN;
		if (block instanceof DoorBlock) {
			if (state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER && direction == Direction.UP)
				return true;
			return direction == Direction.DOWN;
		}
		if (block instanceof BedBlock) {
			Direction facing = state.get(BedBlock.FACING);
			if (state.get(BedBlock.PART) == BedPart.HEAD)
				facing = facing.getOpposite();
			return direction == facing;
		}
		if (block instanceof RedstoneLinkBlock)
			return direction.getOpposite() == state.get(RedstoneLinkBlock.FACING);
		if (block instanceof FlowerPotBlock)
			return direction == Direction.DOWN;
		if (block instanceof AbstractRedstoneGateBlock)
			return direction == Direction.DOWN;
		if (block instanceof RedstoneWireBlock)
			return direction == Direction.DOWN;
		if (block instanceof DyedCarpetBlock)
			return direction == Direction.DOWN;
		if (block instanceof WallRedstoneTorchBlock)
			return state.get(WallRedstoneTorchBlock.FACING) == direction.getOpposite();
		if (block instanceof TorchBlock)
			return direction == Direction.DOWN;
		if (block instanceof WallMountedBlock) {
			WallMountLocation attachFace = state.get(WallMountedBlock.FACE);
			if (attachFace == WallMountLocation.CEILING)
				return direction == Direction.UP;
			if (attachFace == WallMountLocation.FLOOR)
				return direction == Direction.DOWN;
			if (attachFace == WallMountLocation.WALL)
				return direction.getOpposite() == state.get(WallMountedBlock.FACING);
		}
		if (state.contains(Properties.HANGING))
			return direction == (state.get(Properties.HANGING) ? Direction.UP : Direction.DOWN);
		if (block instanceof AbstractRailBlock)
			return direction == Direction.DOWN;
		if (block instanceof AttachedActorBlock)
			return direction == state.get(HarvesterBlock.FACING)
				.getOpposite();
		if (block instanceof HandCrankBlock)
			return direction == state.get(HandCrankBlock.FACING)
				.getOpposite();
		if (block instanceof NozzleBlock)
			return direction == state.get(NozzleBlock.FACING)
				.getOpposite();
		if (block instanceof BellBlock) {
			Attachment attachment = state.get(Properties.ATTACHMENT);
			if (attachment == Attachment.FLOOR)
				return direction == Direction.DOWN;
			if (attachment == Attachment.CEILING)
				return direction == Direction.UP;
			return direction == state.get(HorizontalFacingBlock.FACING);
		}
		if (state.getBlock() instanceof SailBlock)
			return direction.getAxis() != state.get(SailBlock.FACING)
				.getAxis();
		if (state.getBlock() instanceof FluidTankBlock)
			return ConnectivityHandler.isConnected(world, pos, pos.offset(direction));
		if (state.getBlock() instanceof ItemVaultBlock)
			return ConnectivityHandler.isConnected(world, pos, pos.offset(direction));
		if (AllBlocks.STICKER.has(state) && state.get(StickerBlock.EXTENDED)) {
			return direction == state.get(StickerBlock.FACING)
				&& !isNotSupportive(world.getBlockState(pos.offset(direction)), direction.getOpposite());
		}
		if (block instanceof AbstractBogeyBlock<?> bogey)
			return bogey.getStickySurfaces(world, pos, state)
				.contains(direction);
		if (block instanceof WhistleBlock)
			return direction == (state.get(WhistleBlock.WALL) ? state.get(WhistleBlock.FACING)
				: Direction.DOWN);
		if (block instanceof WhistleExtenderBlock)
			return direction == Direction.DOWN;
		return false;
	}

	private static boolean isNotSupportiveFallback(BlockState state, Direction facing) {
		if (AllBlocks.MECHANICAL_DRILL.has(state))
			return state.get(Properties.FACING) == facing;
		if (AllBlocks.MECHANICAL_BEARING.has(state))
			return state.get(Properties.FACING) == facing;

		if (AllBlocks.CART_ASSEMBLER.has(state))
			return Direction.DOWN == facing;
		if (AllBlocks.MECHANICAL_SAW.has(state))
			return state.get(Properties.FACING) == facing;
		if (AllBlocks.PORTABLE_STORAGE_INTERFACE.has(state))
			return state.get(PortableStorageInterfaceBlock.FACING) == facing;
		if (state.getBlock() instanceof AttachedActorBlock && !AllBlocks.MECHANICAL_ROLLER.has(state))
			return state.get(Properties.HORIZONTAL_FACING) == facing;
		if (AllBlocks.ROPE_PULLEY.has(state))
			return facing == Direction.DOWN;
		if (state.getBlock() instanceof DyedCarpetBlock)
			return facing == Direction.UP;
		if (state.getBlock() instanceof SailBlock)
			return facing.getAxis() == state.get(SailBlock.FACING)
				.getAxis();
		if (AllBlocks.PISTON_EXTENSION_POLE.has(state))
			return facing.getAxis() != state.get(Properties.FACING)
				.getAxis();
		if (AllBlocks.MECHANICAL_PISTON_HEAD.has(state))
			return facing.getAxis() != state.get(Properties.FACING)
				.getAxis();
		if (AllBlocks.STICKER.has(state) && !state.get(StickerBlock.EXTENDED))
			return facing == state.get(StickerBlock.FACING);
		if (state.getBlock() instanceof SlidingDoorBlock)
			return false;
		return isBrittle(state);
	}

	// Check classes

	public static interface MovementNecessaryCheck {
		public CheckResult isMovementNecessary(BlockState state, World world, BlockPos pos);
	}

	public static interface MovementAllowedCheck {
		public CheckResult isMovementAllowed(BlockState state, World world, BlockPos pos);
	}

	public static interface BrittleCheck {
		/**
		 * Brittle blocks will be collected first, as they may break when other blocks
		 * are removed before them
		 */
		public CheckResult isBrittle(BlockState state);
	}

	public static interface AttachedCheck {
		/**
		 * Attached blocks will move if blocks they are attached to are moved
		 */
		public CheckResult isBlockAttachedTowards(BlockState state, World world, BlockPos pos, Direction direction);
	}

	public static interface NotSupportiveCheck {
		/**
		 * Non-Supportive blocks will not continue a chain of blocks picked up by e.g. a
		 * piston
		 */
		public CheckResult isNotSupportive(BlockState state, Direction direction);
	}

	public static interface AllChecks
		extends MovementNecessaryCheck, MovementAllowedCheck, BrittleCheck, AttachedCheck, NotSupportiveCheck {
	}

	public static enum CheckResult {
		SUCCESS, FAIL, PASS;

		public Boolean toBoolean() {
			return this == PASS ? null : (this == SUCCESS ? true : false);
		}

		public static CheckResult of(boolean b) {
			return b ? SUCCESS : FAIL;
		}

		public static CheckResult of(Boolean b) {
			return b == null ? PASS : (b ? SUCCESS : FAIL);
		}
	}

}
