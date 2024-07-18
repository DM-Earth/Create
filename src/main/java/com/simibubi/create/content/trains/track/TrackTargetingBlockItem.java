package com.simibubi.create.content.trains.track;

import java.util.List;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.mutable.MutableObject;

import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TrackTargetingBlockItem extends BlockItem {

	private EdgePointType<?> type;

	public static <T extends Block> NonNullBiFunction<? super T, Item.Settings, TrackTargetingBlockItem> ofType(
		EdgePointType<?> type) {
		return (b, p) -> new TrackTargetingBlockItem(b, p, type);
	}

	public TrackTargetingBlockItem(Block pBlock, Settings pProperties, EdgePointType<?> type) {
		super(pBlock, pProperties);
		this.type = type;
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
			player.sendMessage(Lang.translateDirect("track_target.clear"), true);
			stack.setNbt(null);
			AllSoundEvents.CONTROLLER_CLICK.play(level, null, pos, 1, .5f);
			return ActionResult.SUCCESS;
		}

		if (state.getBlock() instanceof ITrackBlock track) {
			if (level.isClient)
				return ActionResult.SUCCESS;

			Vec3d lookAngle = player.getRotationVector();
			boolean front = track.getNearestTrackAxis(level, pos, state, lookAngle)
				.getSecond() == AxisDirection.POSITIVE;
			EdgePointType<?> type = getType(stack);

			MutableObject<OverlapResult> result = new MutableObject<>(null);
			withGraphLocation(level, pos, front, null, type, (overlap, location) -> result.setValue(overlap));

			if (result.getValue().feedback != null) {
				player.sendMessage(Lang.translateDirect(result.getValue().feedback)
					.formatted(Formatting.RED), true);
				AllSoundEvents.DENY.play(level, null, pos, .5f, 1);
				return ActionResult.FAIL;
			}

			NbtCompound stackTag = stack.getOrCreateNbt();
			stackTag.put("SelectedPos", NbtHelper.fromBlockPos(pos));
			stackTag.putBoolean("SelectedDirection", front);
			stackTag.remove("Bezier");
			player.sendMessage(Lang.translateDirect("track_target.set"), true);
			stack.setNbt(stackTag);
			AllSoundEvents.CONTROLLER_CLICK.play(level, null, pos, 1, 1);
			return ActionResult.SUCCESS;
		}

		if (!stack.hasNbt()) {
			player.sendMessage(Lang.translateDirect("track_target.missing")
				.formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}

		NbtCompound tag = stack.getNbt();
		NbtCompound teTag = new NbtCompound();
		teTag.putBoolean("TargetDirection", tag.getBoolean("SelectedDirection"));

		BlockPos selectedPos = NbtHelper.toBlockPos(tag.getCompound("SelectedPos"));
		BlockPos placedPos = pos.offset(pContext.getSide(), state.isReplaceable() ? 0 : 1);

		boolean bezier = tag.contains("Bezier");

		if (!selectedPos.isWithinDistance(placedPos, bezier ? 64 + 16 : 16)) {
			player.sendMessage(Lang.translateDirect("track_target.too_far")
				.formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}

		if (bezier)
			teTag.put("Bezier", tag.getCompound("Bezier"));

		teTag.put("TargetTrack", NbtHelper.fromBlockPos(selectedPos.subtract(placedPos)));
		tag.put("BlockEntityTag", teTag);

		ActionResult useOn = super.useOnBlock(pContext);
		if (level.isClient || useOn == ActionResult.FAIL)
			return useOn;

		ItemStack itemInHand = player.getStackInHand(pContext.getHand());
		if (!itemInHand.isEmpty())
			itemInHand.setNbt(null);
		player.sendMessage(Lang.translateDirect("track_target.success")
			.formatted(Formatting.GREEN), true);
		
		if (type == EdgePointType.SIGNAL)
			AllAdvancements.SIGNAL.awardTo(player);
		
		return useOn;
	}

	public EdgePointType<?> getType(ItemStack stack) {
		return type;
	}

	@Environment(EnvType.CLIENT)
	public boolean useOnCurve(BezierPointSelection selection, ItemStack stack) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		TrackBlockEntity be = selection.blockEntity();
		BezierTrackPointLocation loc = selection.loc();
		boolean front = player.getRotationVector()
			.dotProduct(selection.direction()) < 0;

		AllPackets.getChannel().sendToServer(new CurvedTrackSelectionPacket(be.getPos(), loc.curveTarget(),
			loc.segment(), front, player.getInventory().selectedSlot));
		return true;
	}

	public static enum OverlapResult {

		VALID,
		OCCUPIED("track_target.occupied"),
		JUNCTION("track_target.no_junctions"),
		NO_TRACK("track_target.invalid");

		public String feedback;

		private OverlapResult() {}

		private OverlapResult(String feedback) {
			this.feedback = feedback;
		}

	}

	public static void withGraphLocation(World level, BlockPos pos, boolean front,
		BezierTrackPointLocation targetBezier, EdgePointType<?> type,
		BiConsumer<OverlapResult, TrackGraphLocation> callback) {

		BlockState state = level.getBlockState(pos);

		if (!(state.getBlock() instanceof ITrackBlock track)) {
			callback.accept(OverlapResult.NO_TRACK, null);
			return;
		}

		List<Vec3d> trackAxes = track.getTrackAxes(level, pos, state);
		if (targetBezier == null && trackAxes.size() > 1) {
			callback.accept(OverlapResult.JUNCTION, null);
			return;
		}

		AxisDirection targetDirection = front ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;
		TrackGraphLocation location =
			targetBezier != null ? TrackGraphHelper.getBezierGraphLocationAt(level, pos, targetDirection, targetBezier)
				: TrackGraphHelper.getGraphLocationAt(level, pos, targetDirection, trackAxes.get(0));

		if (location == null) {
			callback.accept(OverlapResult.NO_TRACK, null);
			return;
		}

		Couple<TrackNode> nodes = location.edge.map(location.graph::locateNode);
		TrackEdge edge = location.graph.getConnection(nodes);
		if (edge == null)
			return;

		EdgeData edgeData = edge.getEdgeData();
		double edgePosition = location.position;

		for (TrackEdgePoint edgePoint : edgeData.getPoints()) {
			double otherEdgePosition = edgePoint.getLocationOn(edge);
			double distance = Math.abs(edgePosition - otherEdgePosition);
			if (distance > .75)
				continue;
			if (edgePoint.canCoexistWith(type, front) && distance < .25)
				continue;

			callback.accept(OverlapResult.OCCUPIED, location);
			return;
		}

		callback.accept(OverlapResult.VALID, location);
	}

}
