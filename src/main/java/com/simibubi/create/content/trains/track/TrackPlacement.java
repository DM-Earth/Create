package com.simibubi.create.content.trains.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jozufozu.flywheel.util.Color;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.AllTags;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TrackPlacement {

	public static class PlacementInfo {

		public PlacementInfo(TrackMaterial material) {
			this.trackMaterial = material;
		}

		BezierConnection curve = null;
		boolean valid = false;
		int end1Extent = 0;
		int end2Extent = 0;
		String message = null;

		public int requiredTracks = 0;
		public boolean hasRequiredTracks = false;

		public int requiredPavement = 0;
		public boolean hasRequiredPavement = false;
		public final TrackMaterial trackMaterial;

		// for visualisation
		Vec3d end1;
		Vec3d end2;
		Vec3d normal1;
		Vec3d normal2;
		Vec3d axis1;
		Vec3d axis2;
		BlockPos pos1;
		BlockPos pos2;

		public PlacementInfo withMessage(String message) {
			this.message = "track." + message;
			return this;
		}

		public PlacementInfo tooJumbly() {
			curve = null;
			return this;
		}
	}

	public static PlacementInfo cached;

	static BlockPos hoveringPos;
	static boolean hoveringMaxed;
	static int hoveringAngle;
	static ItemStack lastItem;

	static int extraTipWarmup;

	public static PlacementInfo tryConnect(World level, PlayerEntity player, BlockPos pos2, BlockState state2,
		ItemStack stack, boolean girder, boolean maximiseTurn) {
		Vec3d lookVec = player.getRotationVector();
		int lookAngle = (int) (22.5 + AngleHelper.deg(MathHelper.atan2(lookVec.z, lookVec.x)) % 360) / 8;
		int maxLength = AllConfigs.server().trains.maxTrackPlacementLength.get();

		if (level.isClient && cached != null && pos2.equals(hoveringPos) && stack.equals(lastItem)
			&& hoveringMaxed == maximiseTurn && lookAngle == hoveringAngle)
			return cached;

		PlacementInfo info = new PlacementInfo(TrackMaterial.fromItem(stack.getItem()));
		hoveringMaxed = maximiseTurn;
		hoveringAngle = lookAngle;
		hoveringPos = pos2;
		lastItem = stack;
		cached = info;

		ITrackBlock track = (ITrackBlock) state2.getBlock();
		Pair<Vec3d, AxisDirection> nearestTrackAxis = track.getNearestTrackAxis(level, pos2, state2, lookVec);
		Vec3d axis2 = nearestTrackAxis.getFirst()
			.multiply(nearestTrackAxis.getSecond() == AxisDirection.POSITIVE ? -1 : 1);
		Vec3d normal2 = track.getUpNormal(level, pos2, state2)
			.normalize();
		Vec3d normedAxis2 = axis2.normalize();
		Vec3d end2 = track.getCurveStart(level, pos2, state2, axis2);

		NbtCompound itemTag = stack.getNbt();
		NbtCompound selectionTag = itemTag.getCompound("ConnectingFrom");
		BlockPos pos1 = NbtHelper.toBlockPos(selectionTag.getCompound("Pos"));
		Vec3d axis1 = VecHelper.readNBT(selectionTag.getList("Axis", NbtElement.DOUBLE_TYPE));
		Vec3d normedAxis1 = axis1.normalize();
		Vec3d end1 = VecHelper.readNBT(selectionTag.getList("End", NbtElement.DOUBLE_TYPE));
		Vec3d normal1 = VecHelper.readNBT(selectionTag.getList("Normal", NbtElement.DOUBLE_TYPE));
		boolean front1 = selectionTag.getBoolean("Front");
		BlockState state1 = level.getBlockState(pos1);

		if (level.isClient) {
			info.end1 = end1;
			info.end2 = end2;
			info.normal1 = normal1;
			info.normal2 = normal2;
			info.axis1 = axis1;
			info.axis2 = axis2;
		}

		if (pos1.equals(pos2))
			return info.withMessage("second_point");
		if (pos1.getSquaredDistance(pos2) > maxLength * maxLength)
			return info.withMessage("too_far")
				.tooJumbly();
		if (!state1.contains(TrackBlock.HAS_BE))
			return info.withMessage("original_missing");
		if (level.getBlockEntity(pos2) instanceof TrackBlockEntity tbe && tbe.isTilted())
			return info.withMessage("turn_start");

		if (axis1.dotProduct(end2.subtract(end1)) < 0) {
			axis1 = axis1.multiply(-1);
			normedAxis1 = normedAxis1.multiply(-1);
			front1 = !front1;
			end1 = track.getCurveStart(level, pos1, state1, axis1);
			if (level.isClient) {
				info.end1 = end1;
				info.axis1 = axis1;
			}
		}

		double[] intersect = VecHelper.intersect(end1, end2, normedAxis1, normedAxis2, Axis.Y);
		boolean parallel = intersect == null;
		boolean skipCurve = false;

		if ((parallel && normedAxis1.dotProduct(normedAxis2) > 0) || (!parallel && (intersect[0] < 0 || intersect[1] < 0))) {
			axis2 = axis2.multiply(-1);
			normedAxis2 = normedAxis2.multiply(-1);
			end2 = track.getCurveStart(level, pos2, state2, axis2);
			if (level.isClient) {
				info.end2 = end2;
				info.axis2 = axis2;
			}
		}

		Vec3d cross2 = normedAxis2.crossProduct(new Vec3d(0, 1, 0));

		double a1 = MathHelper.atan2(normedAxis2.z, normedAxis2.x);
		double a2 = MathHelper.atan2(normedAxis1.z, normedAxis1.x);
		double angle = a1 - a2;
		double ascend = end2.subtract(end1).y;
		double absAscend = Math.abs(ascend);
		boolean slope = !normal1.equals(normal2);

		if (level.isClient) {
			Vec3d offset1 = axis1.multiply(info.end1Extent);
			Vec3d offset2 = axis2.multiply(info.end2Extent);
			BlockPos targetPos1 = pos1.add(BlockPos.ofFloored(offset1));
			BlockPos targetPos2 = pos2.add(BlockPos.ofFloored(offset2));
			info.curve = new BezierConnection(Couple.create(targetPos1, targetPos2),
				Couple.create(end1.add(offset1), end2.add(offset2)), Couple.create(normedAxis1, normedAxis2),
				Couple.create(normal1, normal2), true, girder, TrackMaterial.fromItem(stack.getItem()));
		}

		// S curve or Straight

		double dist = 0;

		if (parallel) {
			double[] sTest = VecHelper.intersect(end1, end2, normedAxis1, cross2, Axis.Y);
			if (sTest != null) {
				double t = Math.abs(sTest[0]);
				double u = Math.abs(sTest[1]);

				skipCurve = MathHelper.approximatelyEquals(u, 0);

				if (!skipCurve && sTest[0] < 0)
					return info.withMessage("perpendicular")
						.tooJumbly();

				if (skipCurve) {
					dist = VecHelper.getCenterOf(pos1)
						.distanceTo(VecHelper.getCenterOf(pos2));
					info.end1Extent = (int) Math.round((dist + 1) / axis1.length());

				} else {
					if (!MathHelper.approximatelyEquals(ascend, 0) || normedAxis1.y != 0)
						return info.withMessage("ascending_s_curve");

					double targetT = u <= 1 ? 3 : u * 2;

					if (t < targetT)
						return info.withMessage("too_sharp");

					// This is for standardising s curve sizes
					if (t > targetT) {
						int correction = (int) ((t - targetT) / axis1.length());
						info.end1Extent = maximiseTurn ? 0 : correction / 2 + (correction % 2);
						info.end2Extent = maximiseTurn ? 0 : correction / 2;
					}
				}
			}
		}

		// Slope

		if (slope) {
			if (!skipCurve)
				return info.withMessage("slope_turn");
			if (MathHelper.approximatelyEquals(normal1.dotProduct(normal2), 0))
				return info.withMessage("opposing_slopes");
			if ((axis1.y < 0 || axis2.y > 0) && ascend > 0)
				return info.withMessage("leave_slope_ascending");
			if ((axis1.y > 0 || axis2.y < 0) && ascend < 0)
				return info.withMessage("leave_slope_descending");

			skipCurve = false;
			info.end1Extent = 0;
			info.end2Extent = 0;

			Axis plane = MathHelper.approximatelyEquals(axis1.x, 0) ? Axis.X : Axis.Z;
			intersect = VecHelper.intersect(end1, end2, normedAxis1, normedAxis2, plane);
			double dist1 = Math.abs(intersect[0] / axis1.length());
			double dist2 = Math.abs(intersect[1] / axis2.length());

			if (dist1 > dist2)
				info.end1Extent = (int) Math.round(dist1 - dist2);
			if (dist2 > dist1)
				info.end2Extent = (int) Math.round(dist2 - dist1);

			double turnSize = Math.min(dist1, dist2);
			if (intersect[0] < 0 || intersect[1] < 0)
				return info.withMessage("too_sharp")
					.tooJumbly();
			if (turnSize < 2)
				return info.withMessage("too_sharp");

			// This is for standardising curve sizes
			if (turnSize > 2 && !maximiseTurn) {
				info.end1Extent += turnSize - 2;
				info.end2Extent += turnSize - 2;
				turnSize = 2;
			}
		}

		// Straight ascend

		if (skipCurve && !MathHelper.approximatelyEquals(ascend, 0)) {
			int hDistance = info.end1Extent;
			if (axis1.y == 0 || !MathHelper.approximatelyEquals(absAscend + 1, dist / axis1.length())) {

				if (axis1.y != 0 && axis1.y == -axis2.y)
					return info.withMessage("ascending_s_curve");

				info.end1Extent = 0;
				double minHDistance = Math.max(absAscend < 4 ? absAscend * 4 : absAscend * 3, 6) / axis1.length();
				if (hDistance < minHDistance)
					return info.withMessage("too_steep");
				if (hDistance > minHDistance) {
					int correction = (int) (hDistance - minHDistance);
					info.end1Extent = maximiseTurn ? 0 : correction / 2 + (correction % 2);
					info.end2Extent = maximiseTurn ? 0 : correction / 2;
				}

				skipCurve = false;
			}
		}

		// Turn

		if (!parallel) {
			float absAngle = Math.abs(AngleHelper.deg(angle));
			if (absAngle < 60 || absAngle > 300)
				return info.withMessage("turn_90")
					.tooJumbly();

			intersect = VecHelper.intersect(end1, end2, normedAxis1, normedAxis2, Axis.Y);
			double dist1 = Math.abs(intersect[0]);
			double dist2 = Math.abs(intersect[1]);
			float ex1 = 0;
			float ex2 = 0;

			if (dist1 > dist2)
				ex1 = (float) ((dist1 - dist2) / axis1.length());
			if (dist2 > dist1)
				ex2 = (float) ((dist2 - dist1) / axis2.length());

			double turnSize = Math.min(dist1, dist2) - .1d;
			boolean ninety = (absAngle + .25f) % 90 < 1;

			if (intersect[0] < 0 || intersect[1] < 0)
				return info.withMessage("too_sharp")
					.tooJumbly();

			double minTurnSize = ninety ? 7 : 3.25;
			double turnSizeToFitAscend =
				minTurnSize + (ninety ? Math.max(0, absAscend - 3) * 2f : Math.max(0, absAscend - 1.5f) * 1.5f);

			if (turnSize < minTurnSize)
				return info.withMessage("too_sharp");
			if (turnSize < turnSizeToFitAscend)
				return info.withMessage("too_steep");

			// This is for standardising curve sizes
			if (!maximiseTurn) {
				ex1 += (turnSize - turnSizeToFitAscend) / axis1.length();
				ex2 += (turnSize - turnSizeToFitAscend) / axis2.length();
			}
			info.end1Extent = MathHelper.floor(ex1);
			info.end2Extent = MathHelper.floor(ex2);
			turnSize = turnSizeToFitAscend;
		}

		Vec3d offset1 = axis1.multiply(info.end1Extent);
		Vec3d offset2 = axis2.multiply(info.end2Extent);
		BlockPos targetPos1 = pos1.add(BlockPos.ofFloored(offset1));
		BlockPos targetPos2 = pos2.add(BlockPos.ofFloored(offset2));

		info.curve = skipCurve ? null
			: new BezierConnection(Couple.create(targetPos1, targetPos2),
				Couple.create(end1.add(offset1), end2.add(offset2)), Couple.create(normedAxis1, normedAxis2),
				Couple.create(normal1, normal2), true, girder, TrackMaterial.fromItem(stack.getItem()));

		info.valid = true;

		info.pos1 = pos1;
		info.pos2 = pos2;
		info.axis1 = axis1;
		info.axis2 = axis2;

		placeTracks(level, info, state1, state2, targetPos1, targetPos2, true);

		ItemStack offhandItem = player.getOffHandStack()
			.copy();
		boolean shouldPave = offhandItem.getItem() instanceof BlockItem;
		if (shouldPave) {
			BlockItem paveItem = (BlockItem) offhandItem.getItem();
			paveTracks(level, info, paveItem, true);
			info.hasRequiredPavement = true;
		}

		info.hasRequiredTracks = true;

		if (!player.isCreative()) {
			for (boolean simulate : Iterate.trueAndFalse) {
				if (level.isClient && !simulate)
					break;

				int tracks = info.requiredTracks;
				int pavement = info.requiredPavement;
				int foundTracks = 0;
				int foundPavement = 0;

				PlayerInventory inv = player.getInventory();
				int size = inv.main.size();
				for (int j = 0; j <= size + 1; j++) {
					int i = j;
					boolean offhand = j == size + 1;
					if (j == size)
						i = inv.selectedSlot;
					else if (offhand)
						i = 0;
					else if (j == inv.selectedSlot)
						continue;

					ItemStack stackInSlot = (offhand ? inv.offHand : inv.main).get(i);
					boolean isTrack = AllTags.AllBlockTags.TRACKS.matches(stackInSlot) && stackInSlot.isOf(stack.getItem());
					if (!isTrack && (!shouldPave || offhandItem.getItem() != stackInSlot.getItem()))
						continue;
					if (isTrack ? foundTracks >= tracks : foundPavement >= pavement)
						continue;

					int count = stackInSlot.getCount();

					if (!simulate) {
						int remainingItems =
							count - Math.min(isTrack ? tracks - foundTracks : pavement - foundPavement, count);
						if (i == inv.selectedSlot)
							stackInSlot.setNbt(null);
						ItemStack newItem = ItemHandlerHelper.copyStackWithSize(stackInSlot, remainingItems);
						if (offhand)
							player.setStackInHand(Hand.OFF_HAND, newItem);
						else
							inv.setStack(i, newItem);
					}

					if (isTrack)
						foundTracks += count;
					else
						foundPavement += count;
				}

				if (simulate && foundTracks < tracks) {
					info.valid = false;
					info.tooJumbly();
					info.hasRequiredTracks = false;
					return info.withMessage("not_enough_tracks");
				}

				if (simulate && foundPavement < pavement) {
					info.valid = false;
					info.tooJumbly();
					info.hasRequiredPavement = false;
					return info.withMessage("not_enough_pavement");
				}
			}
		}

		if (level.isClient())
			return info;
		if (shouldPave) {
			BlockItem paveItem = (BlockItem) offhandItem.getItem();
			paveTracks(level, info, paveItem, false);
		}
		return placeTracks(level, info, state1, state2, targetPos1, targetPos2, false);
	}

	private static void paveTracks(World level, PlacementInfo info, BlockItem blockItem, boolean simulate) {
		Block block = blockItem.getBlock();
		info.requiredPavement = 0;
		if (block == null || block instanceof BlockEntityProvider || block.getDefaultState()
			.getCollisionShape(level, info.pos1)
			.isEmpty())
			return;

		Set<BlockPos> visited = new HashSet<>();

		for (boolean first : Iterate.trueAndFalse) {
			int extent = (first ? info.end1Extent : info.end2Extent) + (info.curve != null ? 1 : 0);
			Vec3d axis = first ? info.axis1 : info.axis2;
			BlockPos pavePos = first ? info.pos1 : info.pos2;
			info.requiredPavement +=
				TrackPaver.paveStraight(level, pavePos.down(), axis, extent, block, simulate, visited);
		}

		if (info.curve != null)
			info.requiredPavement += TrackPaver.paveCurve(level, info.curve, block, simulate, visited);
	}

	private static PlacementInfo placeTracks(World level, PlacementInfo info, BlockState state1, BlockState state2,
		BlockPos targetPos1, BlockPos targetPos2, boolean simulate) {
		info.requiredTracks = 0;

		for (boolean first : Iterate.trueAndFalse) {
			int extent = first ? info.end1Extent : info.end2Extent;
			Vec3d axis = first ? info.axis1 : info.axis2;
			BlockPos pos = first ? info.pos1 : info.pos2;
			BlockState state = first ? state1 : state2;
			if (state.contains(TrackBlock.HAS_BE) && !simulate)
				state = state.with(TrackBlock.HAS_BE, false);

			switch (state.get(TrackBlock.SHAPE)) {
			case TE, TW:
				state = state.with(TrackBlock.SHAPE, TrackShape.XO);
				break;
			case TN, TS:
				state = state.with(TrackBlock.SHAPE, TrackShape.ZO);
				break;
			default:
				break;
			}

			for (int i = 0; i < (info.curve != null ? extent + 1 : extent); i++) {
				Vec3d offset = axis.multiply(i);
				BlockPos offsetPos = pos.add(BlockPos.ofFloored(offset));
				BlockState stateAtPos = level.getBlockState(offsetPos);
				// copy over all shared properties from the shaped state to the correct track material block
				BlockState toPlace = BlockHelper.copyProperties(state, info.trackMaterial.getBlock().getDefaultState());

				boolean canPlace = stateAtPos.isReplaceable();
				if (canPlace)
					info.requiredTracks++;
				if (simulate)
					continue;

				if (stateAtPos.getBlock() instanceof ITrackBlock trackAtPos) {
					toPlace = trackAtPos.overlay(level, offsetPos, stateAtPos, toPlace);
					canPlace = true;
				}

				if (canPlace)
					level.setBlockState(offsetPos, ProperWaterloggedBlock.withWater(level, toPlace, offsetPos), 3);
			}
		}

		if (info.curve == null)
			return info;

		if (!simulate) {
			BlockState onto = info.trackMaterial.getBlock().getDefaultState();
			BlockState stateAtPos = level.getBlockState(targetPos1);
			level.setBlockState(targetPos1, ProperWaterloggedBlock.withWater(level,
					(AllTags.AllBlockTags.TRACKS.matches(stateAtPos) ? stateAtPos : BlockHelper.copyProperties(state1, onto))
							.with(TrackBlock.HAS_BE, true), targetPos1), 3);

			stateAtPos = level.getBlockState(targetPos2);
			level.setBlockState(targetPos2, ProperWaterloggedBlock.withWater(level,
					(AllTags.AllBlockTags.TRACKS.matches(stateAtPos) ? stateAtPos : BlockHelper.copyProperties(state2, onto))
							.with(TrackBlock.HAS_BE, true), targetPos2), 3);
		}

		BlockEntity te1 = level.getBlockEntity(targetPos1);
		BlockEntity te2 = level.getBlockEntity(targetPos2);
		int requiredTracksForTurn = (info.curve.getSegmentCount() + 1) / 2;

		if (!(te1 instanceof TrackBlockEntity) || !(te2 instanceof TrackBlockEntity)) {
			info.requiredTracks += requiredTracksForTurn;
			return info;
		}

		TrackBlockEntity tte1 = (TrackBlockEntity) te1;
		TrackBlockEntity tte2 = (TrackBlockEntity) te2;

		if (!tte1.getConnections()
			.containsKey(tte2.getPos()))
			info.requiredTracks += requiredTracksForTurn;

		if (simulate)
			return info;

		tte1.addConnection(info.curve);
		tte2.addConnection(info.curve.secondary());
		tte1.tilt.tryApplySmoothing();
		tte2.tilt.tryApplySmoothing();
		return info;
	}

	static LerpedFloat animation = LerpedFloat.linear()
		.startWithValue(0);
	static int lastLineCount = 0;

	static BlockPos hintPos;
	static int hintAngle;
	static Couple<List<BlockPos>> hints;

	@Environment(EnvType.CLIENT)
	public static void clientTick() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		ItemStack stack = player.getMainHandStack();
		HitResult hitResult = MinecraftClient.getInstance().crosshairTarget;
		int restoreWarmup = extraTipWarmup;
		extraTipWarmup = 0;

		if (hitResult == null)
			return;
		if (hitResult.getType() != Type.BLOCK)
			return;

		Hand hand = Hand.MAIN_HAND;
		if (!AllTags.AllBlockTags.TRACKS.matches(stack)) {
			stack = player.getOffHandStack();
			hand = Hand.OFF_HAND;
			if (!AllTags.AllBlockTags.TRACKS.matches(stack))
				return;
		}

		if (!stack.hasGlint())
			return;

		TrackBlockItem blockItem = (TrackBlockItem) stack.getItem();
		World level = player.getWorld();
		BlockHitResult bhr = (BlockHitResult) hitResult;
		BlockPos pos = bhr.getBlockPos();
		BlockState hitState = level.getBlockState(pos);
		if (!(hitState.getBlock() instanceof TrackBlock) && !hitState.isReplaceable()) {
			pos = pos.offset(bhr.getSide());
			hitState = blockItem.getPlacementState(new ItemUsageContext(player, hand, bhr));
			if (hitState == null)
				return;
		}

		if (!(hitState.getBlock() instanceof TrackBlock))
			return;

		extraTipWarmup = restoreWarmup;
		boolean maxTurns = MinecraftClient.getInstance().options.sprintKey.isPressed();
		PlacementInfo info = tryConnect(level, player, pos, hitState, stack, false, maxTurns);
		if (extraTipWarmup < 20)
			extraTipWarmup++;
		if (!info.valid || !hoveringMaxed && (info.end1Extent == 0 || info.end2Extent == 0))
			extraTipWarmup = 0;

		if (!player.isCreative() && (info.valid || !info.hasRequiredTracks || !info.hasRequiredPavement))
			BlueprintOverlayRenderer.displayTrackRequirements(info, player.getOffHandStack());

		if (info.valid)
			player.sendMessage(Lang.translateDirect("track.valid_connection")
				.formatted(Formatting.GREEN), true);
		else if (info.message != null)
			player.sendMessage(Lang.translateDirect(info.message)
				.formatted(info.message.equals("track.second_point") ? Formatting.WHITE : Formatting.RED),
				true);

		if (bhr.getSide() == Direction.UP) {
			Vec3d lookVec = player.getRotationVector();
			int lookAngle = (int) (22.5 + AngleHelper.deg(MathHelper.atan2(lookVec.z, lookVec.x)) % 360) / 8;

			if (!pos.equals(hintPos) || lookAngle != hintAngle) {
				hints = Couple.create(ArrayList::new);
				hintAngle = lookAngle;
				hintPos = pos;

				for (int xOffset = -2; xOffset <= 2; xOffset++) {
					for (int zOffset = -2; zOffset <= 2; zOffset++) {
						BlockPos offset = pos.add(xOffset, 0, zOffset);
						PlacementInfo adjInfo = tryConnect(level, player, offset, hitState, stack, false, maxTurns);
						hints.get(adjInfo.valid)
							.add(offset.down());
					}
				}
			}

			if (hints != null && !hints.either(Collection::isEmpty)) {
				CreateClient.OUTLINER.showCluster("track_valid", hints.getFirst())
					.withFaceTexture(AllSpecialTextures.THIN_CHECKERED)
					.colored(0x95CD41)
					.lineWidth(0);
				CreateClient.OUTLINER.showCluster("track_invalid", hints.getSecond())
					.withFaceTexture(AllSpecialTextures.THIN_CHECKERED)
					.colored(0xEA5C2B)
					.lineWidth(0);
			}
		}

		animation.chase(info.valid ? 1 : 0, 0.25, Chaser.EXP);
		animation.tickChaser();

		if (!info.valid) {
			info.end1Extent = 0;
			info.end2Extent = 0;
		}

		int color = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue());
		Vec3d up = new Vec3d(0, 4 / 16f, 0);

		{
			Vec3d v1 = info.end1;
			Vec3d a1 = info.axis1.normalize();
			Vec3d n1 = info.normal1.crossProduct(a1)
				.multiply(15 / 16f);
			Vec3d o1 = a1.multiply(0.125f);
			Vec3d ex1 =
				a1.multiply((info.end1Extent - (info.curve == null && info.end1Extent > 0 ? 2 : 0)) * info.axis1.length());
			line(1, v1.add(n1)
				.add(up), o1, ex1);
			line(2, v1.subtract(n1)
				.add(up), o1, ex1);

			Vec3d v2 = info.end2;
			Vec3d a2 = info.axis2.normalize();
			Vec3d n2 = info.normal2.crossProduct(a2)
				.multiply(15 / 16f);
			Vec3d o2 = a2.multiply(0.125f);
			Vec3d ex2 = a2.multiply(info.end2Extent * info.axis2.length());
			line(3, v2.add(n2)
				.add(up), o2, ex2);
			line(4, v2.subtract(n2)
				.add(up), o2, ex2);
		}

		BezierConnection bc = info.curve;
		if (bc == null)
			return;

		Vec3d previous1 = null;
		Vec3d previous2 = null;
		int railcolor = color;
		int segCount = bc.getSegmentCount();

		float s = animation.getValue() * 7 / 8f + 1 / 8f;
		float lw = animation.getValue() * 1 / 16f + 1 / 16f;
		Vec3d end1 = bc.starts.getFirst();
		Vec3d end2 = bc.starts.getSecond();
		Vec3d finish1 = end1.add(bc.axes.getFirst()
			.multiply(bc.getHandleLength()));
		Vec3d finish2 = end2.add(bc.axes.getSecond()
			.multiply(bc.getHandleLength()));
		String key = "curve";

		for (int i = 0; i <= segCount; i++) {
			float t = i / (float) segCount;
			Vec3d result = VecHelper.bezier(end1, end2, finish1, finish2, t);
			Vec3d derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t)
				.normalize();
			Vec3d normal = bc.getNormal(t)
				.crossProduct(derivative)
				.multiply(15 / 16f);
			Vec3d rail1 = result.add(normal)
				.add(up);
			Vec3d rail2 = result.subtract(normal)
				.add(up);

			if (previous1 != null) {
				Vec3d middle1 = rail1.add(previous1)
					.multiply(0.5f);
				Vec3d middle2 = rail2.add(previous2)
					.multiply(0.5f);
				CreateClient.OUTLINER
					.showLine(Pair.of(key, i * 2), VecHelper.lerp(s, middle1, previous1),
						VecHelper.lerp(s, middle1, rail1))
					.colored(railcolor)
					.disableLineNormals()
					.lineWidth(lw);
				CreateClient.OUTLINER
					.showLine(Pair.of(key, i * 2 + 1), VecHelper.lerp(s, middle2, previous2),
						VecHelper.lerp(s, middle2, rail2))
					.colored(railcolor)
					.disableLineNormals()
					.lineWidth(lw);
			}

			previous1 = rail1;
			previous2 = rail2;
		}

		for (int i = segCount + 1; i <= lastLineCount; i++) {
			CreateClient.OUTLINER.remove(Pair.of(key, i * 2));
			CreateClient.OUTLINER.remove(Pair.of(key, i * 2 + 1));
		}

		lastLineCount = segCount;
	}

	@Environment(EnvType.CLIENT)
	private static void line(int id, Vec3d v1, Vec3d o1, Vec3d ex) {
		int color = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue());
		CreateClient.OUTLINER.showLine(Pair.of("start", id), v1.subtract(o1), v1.add(ex))
			.lineWidth(1 / 8f)
			.disableLineNormals()
			.colored(color);
	}

}
