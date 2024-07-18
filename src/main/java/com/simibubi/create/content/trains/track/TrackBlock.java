package com.simibubi.create.content.trains.track;

import static com.simibubi.create.AllShapes.TRACK_ASC;
import static com.simibubi.create.AllShapes.TRACK_CROSS;
import static com.simibubi.create.AllShapes.TRACK_CROSS_DIAG;
import static com.simibubi.create.AllShapes.TRACK_CROSS_DIAG_ORTHO;
import static com.simibubi.create.AllShapes.TRACK_CROSS_ORTHO_DIAG;
import static com.simibubi.create.AllShapes.TRACK_DIAG;
import static com.simibubi.create.AllShapes.TRACK_ORTHO;
import static com.simibubi.create.AllShapes.TRACK_ORTHO_LONG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicates;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.decoration.girder.GirderBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.trains.CubeParticleData;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import com.simibubi.create.foundation.block.render.ReducedDestroyEffects;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.QueryableTickScheduler;

public class TrackBlock extends Block implements IBE<TrackBlockEntity>, IWrenchable, ITrackBlock, ISpecialBlockItemRequirement, ProperWaterloggedBlock, ReducedDestroyEffects, MultiPosDestructionHandler {

	public static final EnumProperty<TrackShape> SHAPE = EnumProperty.of("shape", TrackShape.class);
	public static final BooleanProperty HAS_BE = BooleanProperty.of("turn");

	protected final TrackMaterial material;

	public TrackBlock(Settings p_49795_, TrackMaterial material) {
		super(p_49795_);
		setDefaultState(getDefaultState().with(SHAPE, TrackShape.ZO)
			.with(HAS_BE, false)
			.with(WATERLOGGED, false));
		this.material = material;
		LandPathNodeTypesRegistry.register(this, PathNodeType.RAIL, null);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> p_49915_) {
		super.appendProperties(p_49915_.add(SHAPE, HAS_BE, WATERLOGGED));
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return fluidState(state);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState stateForPlacement = withWater(super.getPlacementState(ctx), ctx);

		if (ctx.getPlayer() == null)
			return stateForPlacement;

		Vec3d lookAngle = ctx.getPlayer()
			.getRotationVector();
		lookAngle = lookAngle.multiply(1, 0, 1);
		if (MathHelper.approximatelyEquals(lookAngle.length(), 0))
			lookAngle = VecHelper.rotate(new Vec3d(0, 0, 1), -ctx.getPlayer()
				.getYaw(), Axis.Y);

		lookAngle = lookAngle.normalize();

		TrackShape best = TrackShape.ZO;
		double bestValue = Float.MAX_VALUE;
		for (TrackShape shape : TrackShape.values()) {
			if (shape.isJunction() || shape.isPortal())
				continue;
			Vec3d axis = shape.getAxes()
				.get(0);
			double distance = Math.min(axis.squaredDistanceTo(lookAngle), axis.normalize()
				.multiply(-1)
				.squaredDistanceTo(lookAngle));
			if (distance > bestValue)
				continue;
			bestValue = distance;
			best = shape;
		}

		World level = ctx.getWorld();
		Vec3d bestAxis = best.getAxes()
			.get(0);
		if (bestAxis.lengthSquared() == 1)
			for (boolean neg : Iterate.trueAndFalse) {
				BlockPos offset = ctx.getBlockPos()
					.add(BlockPos.ofFloored(bestAxis.multiply(neg ? -1 : 1)));

				if (level.getBlockState(offset)
					.isSideSolidFullSquare(level, offset, Direction.UP)
					&& !level.getBlockState(offset.up())
						.isSideSolidFullSquare(level, offset, Direction.DOWN)) {
					if (best == TrackShape.XO)
						best = neg ? TrackShape.AW : TrackShape.AE;
					if (best == TrackShape.ZO)
						best = neg ? TrackShape.AN : TrackShape.AS;
				}
			}

		return stateForPlacement.with(SHAPE, best);
	}

	@Override
	public void onBreak(World pLevel, BlockPos pPos, BlockState pState, PlayerEntity pPlayer) {
		super.onBreak(pLevel, pPos, pState, pPlayer);
		if (pLevel.isClient())
			return;
		if (!pPlayer.isCreative())
			return;
		withBlockEntityDo(pLevel, pPos, be -> {
			be.cancelDrops = true;
			be.removeInboundConnections(true);
		});
	}

	@Override
	public void onBlockAdded(BlockState pState, World pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
		if (pOldState.getBlock() == this && pState.with(HAS_BE, true) == pOldState.with(HAS_BE, true))
			return;
		if (pLevel.isClient)
			return;
		QueryableTickScheduler<Block> blockTicks = pLevel.getBlockTickScheduler();
		if (!blockTicks.isQueued(pPos, this))
			pLevel.scheduleBlockTick(pPos, this, 1);
		updateGirders(pState, pLevel, pPos, blockTicks);
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		withBlockEntityDo(pLevel, pPos, TrackBlockEntity::validateConnections);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld level, BlockPos pos, Random p_60465_) {
		TrackPropagator.onRailAdded(level, pos, state);
		withBlockEntityDo(level, pos, tbe -> tbe.tilt.undoSmoothing());
		if (!state.get(SHAPE)
			.isPortal())
			connectToPortal(level, pos, state);
	}

	protected void connectToPortal(ServerWorld level, BlockPos pos, BlockState state) {
		TrackShape shape = state.get(TrackBlock.SHAPE);
		Axis portalTest = shape == TrackShape.XO ? Axis.X : shape == TrackShape.ZO ? Axis.Z : null;
		if (portalTest == null)
			return;

		boolean pop = false;
		String fail = null;
		BlockPos failPos = null;

		for (Direction d : Iterate.directionsInAxis(portalTest)) {
			BlockPos portalPos = pos.offset(d);
			BlockState portalState = level.getBlockState(portalPos);
			if (!AllPortalTracks.isSupportedPortal(portalState))
				continue;

			pop = true;
			Pair<ServerWorld, BlockFace> otherSide = AllPortalTracks.getOtherSide(level, new BlockFace(pos, d));
			if (otherSide == null) {
				fail = "missing";
				continue;
			}

			ServerWorld otherLevel = otherSide.getFirst();
			BlockFace otherTrack = otherSide.getSecond();
			BlockPos otherTrackPos = otherTrack.getPos();
			BlockState existing = otherLevel.getBlockState(otherTrackPos);
			if (!existing.isReplaceable()) {
				fail = "blocked";
				failPos = otherTrackPos;
				continue;
			}

			level.setBlockState(pos, state.with(SHAPE, TrackShape.asPortal(d))
				.with(HAS_BE, true), 3);
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof TrackBlockEntity tbe)
				tbe.bind(otherLevel.getRegistryKey(), otherTrackPos);

			otherLevel.setBlockState(otherTrackPos, state.with(SHAPE, TrackShape.asPortal(otherTrack.getFace()))
				.with(HAS_BE, true), 3);
			BlockEntity otherBE = otherLevel.getBlockEntity(otherTrackPos);
			if (otherBE instanceof TrackBlockEntity tbe)
				tbe.bind(level.getRegistryKey(), pos);

			pop = false;
		}

		if (!pop)
			return;

		level.breakBlock(pos, true);

		if (fail == null)
			return;
		PlayerEntity player = level.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, Predicates.alwaysTrue());
		if (player == null)
			return;
		player.sendMessage(Components.literal("<!> ")
			.append(Lang.translateDirect("portal_track.failed"))
			.formatted(Formatting.GOLD), false);
		MutableText component = failPos != null
			? Lang.translateDirect("portal_track." + fail, failPos.getX(), failPos.getY(), failPos.getZ())
			: Lang.translateDirect("portal_track." + fail);
		player.sendMessage(Components.literal(" - ")
			.formatted(Formatting.GRAY)
			.append(component.styled(st -> st.withColor(0xFFD3B4))), false);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction pDirection, BlockState pNeighborState,
		WorldAccess level, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		updateWater(level, state, pCurrentPos);
		TrackShape shape = state.get(SHAPE);
		if (!shape.isPortal())
			return state;

		for (Direction d : Iterate.horizontalDirections) {
			if (TrackShape.asPortal(d) != state.get(SHAPE))
				continue;
			if (pDirection != d)
				continue;

			BlockPos portalPos = pCurrentPos.offset(d);
			BlockState portalState = level.getBlockState(portalPos);
			if (!AllPortalTracks.isSupportedPortal(portalState))
				return Blocks.AIR.getDefaultState();
		}

		return state;
	}

	@Override
	public int getYOffsetAt(BlockView world, BlockPos pos, BlockState state, Vec3d end) {
		return getBlockEntityOptional(world, pos).map(tbe -> tbe.tilt.getYOffsetForAxisEnd(end))
			.orElse(0);
	}

	@Override
	public Collection<DiscoveredLocation> getConnected(BlockView worldIn, BlockPos pos, BlockState state,
		boolean linear, TrackNodeLocation connectedTo) {
		Collection<DiscoveredLocation> list;
		BlockView world = connectedTo != null && worldIn instanceof ServerWorld sl ? sl.getServer()
			.getWorld(connectedTo.dimension) : worldIn;

		if (getTrackAxes(world, pos, state).size() > 1) {
			Vec3d center = Vec3d.ofBottomCenter(pos)
				.add(0, getElevationAtCenter(world, pos, state), 0);
			TrackShape shape = state.get(TrackBlock.SHAPE);
			list = new ArrayList<>();
			for (Vec3d axis : getTrackAxes(world, pos, state))
				for (boolean fromCenter : Iterate.trueAndFalse)
					ITrackBlock.addToListIfConnected(connectedTo, list,
						(d, b) -> axis.multiply(b ? 0 : fromCenter ? -d : d)
							.add(center),
						b -> shape.getNormal(), b -> world instanceof World l ? l.getRegistryKey() : World.OVERWORLD, v -> 0,
						axis, null, (b, v) -> ITrackBlock.getMaterialSimple(world, v));
		} else
			list = ITrackBlock.super.getConnected(world, pos, state, linear, connectedTo);

		if (!state.get(HAS_BE))
			return list;
		if (linear)
			return list;

		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (!(blockEntity instanceof TrackBlockEntity trackBE))
			return list;

		Map<BlockPos, BezierConnection> connections = trackBE.getConnections();
		connections.forEach((connectedPos, bc) -> ITrackBlock.addToListIfConnected(connectedTo, list,
			(d, b) -> d == 1 ? Vec3d.of(bc.tePositions.get(b)) : bc.starts.get(b), bc.normals::get,
			b -> world instanceof World l ? l.getRegistryKey() : World.OVERWORLD, bc::yOffsetAt, null, bc,
			(b, v) -> ITrackBlock.getMaterialSimple(world, v, bc.getMaterial())));

		if (trackBE.boundLocation == null || !(world instanceof ServerWorld level))
			return list;

		RegistryKey<World> otherDim = trackBE.boundLocation.getFirst();
		ServerWorld otherLevel = level.getServer()
			.getWorld(otherDim);
		if (otherLevel == null)
			return list;
		BlockPos boundPos = trackBE.boundLocation.getSecond();
		BlockState boundState = otherLevel.getBlockState(boundPos);
		if (!AllTags.AllBlockTags.TRACKS.matches(boundState))
			return list;

		Vec3d center = Vec3d.ofBottomCenter(pos)
			.add(0, getElevationAtCenter(world, pos, state), 0);
		Vec3d boundCenter = Vec3d.ofBottomCenter(boundPos)
			.add(0, getElevationAtCenter(otherLevel, boundPos, boundState), 0);
		TrackShape shape = state.get(TrackBlock.SHAPE);
		TrackShape boundShape = boundState.get(TrackBlock.SHAPE);
		Vec3d boundAxis = getTrackAxes(otherLevel, boundPos, boundState).get(0);

		getTrackAxes(world, pos, state).forEach(axis -> {
			ITrackBlock.addToListIfConnected(connectedTo, list, (d, b) -> (b ? axis : boundAxis).multiply(d)
				.add(b ? center : boundCenter), b -> (b ? shape : boundShape).getNormal(),
				b -> b ? level.getRegistryKey() : otherLevel.getRegistryKey(), v -> 0, axis, null,
				(b, v) -> ITrackBlock.getMaterialSimple(b ? level : otherLevel, v));
		});

		return list;
	}

	public void randomDisplayTick(BlockState pState, World pLevel, BlockPos pPos, Random pRand) {
		if (!pState.get(SHAPE)
			.isPortal())
			return;
		Vec3d v = Vec3d.of(pPos)
			.subtract(.125f, 0, .125f);
		CubeParticleData data =
			new CubeParticleData(1, pRand.nextFloat(), 1, .0125f + .0625f * pRand.nextFloat(), 30, false);
		pLevel.addParticle(data, v.x + pRand.nextFloat() * 1.5f, v.y + .25f, v.z + pRand.nextFloat() * 1.5f, 0.0D,
			0.04D, 0.0D);
	}

	@Override
	public void onStateReplaced(BlockState pState, World pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		boolean removeBE = false;
		if (pState.get(HAS_BE) && (!pState.isOf(pNewState.getBlock()) || !pNewState.get(HAS_BE))) {
			BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
			if (blockEntity instanceof TrackBlockEntity tbe && !pLevel.isClient) {
				tbe.cancelDrops |= pNewState.getBlock() == this;
				tbe.removeInboundConnections(true);
			}
			removeBE = true;
		}

		if (pNewState.getBlock() != this || pState.with(HAS_BE, true) != pNewState.with(HAS_BE, true))
			TrackPropagator.onRailRemoved(pLevel, pPos, pState);
		if (removeBE)
			pLevel.removeBlockEntity(pPos);
		if (!pLevel.isClient)
			updateGirders(pState, pLevel, pPos, pLevel.getBlockTickScheduler());
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult hit) {

		if (world.isClient)
			return ActionResult.SUCCESS;
		for (Entry<BlockPos, BlockBox> entry : StationBlockEntity.assemblyAreas.get(world)
			.entrySet()) {
			if (!entry.getValue()
				.contains(pos))
				continue;
			if (world.getBlockEntity(entry.getKey()) instanceof StationBlockEntity station)
				if (station.trackClicked(player, hand, this, state, pos))
					return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	private void updateGirders(BlockState pState, World pLevel, BlockPos pPos, QueryableTickScheduler<Block> blockTicks) {
		for (Vec3d vec3 : getTrackAxes(pLevel, pPos, pState)) {
			if (vec3.length() > 1 || vec3.y != 0)
				continue;
			for (int side : Iterate.positiveAndNegative) {
				BlockPos girderPos = pPos.down()
					.add(BlockPos.ofFloored(vec3.z * side, 0, vec3.x * side));
				BlockState girderState = pLevel.getBlockState(girderPos);
				if (girderState.getBlock() instanceof GirderBlock girderBlock
					&& !blockTicks.isQueued(girderPos, girderBlock))
					pLevel.scheduleBlockTick(girderPos, girderBlock, 1);
			}
		}
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView reader, BlockPos pos) {
		return reader.getBlockState(pos.down())
			.getBlock() != this;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView p_60556_, BlockPos p_60557_, ShapeContext p_60558_) {
		return getFullShape(state);
	}

	@Override
	public VoxelShape getRaycastShape(BlockState state, BlockView pLevel, BlockPos pPos) {
		return getFullShape(state);
	}

	private VoxelShape getFullShape(BlockState state) {
		switch (state.get(SHAPE)) {
		case AE:
			return TRACK_ASC.get(Direction.EAST);
		case AW:
			return TRACK_ASC.get(Direction.WEST);
		case AN:
			return TRACK_ASC.get(Direction.NORTH);
		case AS:
			return TRACK_ASC.get(Direction.SOUTH);
		case CR_D:
			return TRACK_CROSS_DIAG;
		case CR_NDX:
			return TRACK_CROSS_ORTHO_DIAG.get(Direction.SOUTH);
		case CR_NDZ:
			return TRACK_CROSS_DIAG_ORTHO.get(Direction.SOUTH);
		case CR_O:
			return TRACK_CROSS;
		case CR_PDX:
			return TRACK_CROSS_DIAG_ORTHO.get(Direction.EAST);
		case CR_PDZ:
			return TRACK_CROSS_ORTHO_DIAG.get(Direction.EAST);
		case ND:
			return TRACK_DIAG.get(Direction.SOUTH);
		case PD:
			return TRACK_DIAG.get(Direction.EAST);
		case XO:
			return TRACK_ORTHO.get(Direction.EAST);
		case ZO:
			return TRACK_ORTHO.get(Direction.SOUTH);
		case TE:
			return TRACK_ORTHO_LONG.get(Direction.EAST);
		case TW:
			return TRACK_ORTHO_LONG.get(Direction.WEST);
		case TS:
			return TRACK_ORTHO_LONG.get(Direction.SOUTH);
		case TN:
			return TRACK_ORTHO_LONG.get(Direction.NORTH);
		case NONE:
		default:
		}
		return AllShapes.TRACK_FALLBACK;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState pState, BlockView pLevel, BlockPos pPos,
		ShapeContext pContext) {
		switch (pState.get(SHAPE)) {
		case AE, AW, AN, AS:
			return VoxelShapes.empty();
		default:
			return AllShapes.TRACK_COLLISION;
		}
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos p_153215_, BlockState state) {
		if (!state.get(HAS_BE))
			return null;
		return AllBlockEntityTypes.TRACK.create(p_153215_, state);
	}

	@Override
	public Class<TrackBlockEntity> getBlockEntityClass() {
		return TrackBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TrackBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.TRACK.get();
	}

	@Override
	public Vec3d getUpNormal(BlockView world, BlockPos pos, BlockState state) {
		return state.get(SHAPE)
			.getNormal();
	}

	@Override
	public List<Vec3d> getTrackAxes(BlockView world, BlockPos pos, BlockState state) {
		return state.get(SHAPE)
			.getAxes();
	}

	@Override
	public Vec3d getCurveStart(BlockView world, BlockPos pos, BlockState state, Vec3d axis) {
		boolean vertical = axis.y != 0;
		return VecHelper.getCenterOf(pos)
			.add(0, (vertical ? 0 : -.5f), 0)
			.add(axis.multiply(.5));
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		World level = context.getWorld();
		if (!level.isClient && !player.isCreative() && state.get(HAS_BE)) {
			BlockEntity blockEntity = level.getBlockEntity(context.getBlockPos());
			if (blockEntity instanceof TrackBlockEntity trackBE) {
				trackBE.cancelDrops = true;
				trackBE.connections.values()
					.forEach(bc -> bc.addItemsToPlayer(player));
			}
		}

		return IWrenchable.super.onSneakWrenched(state, context);
	}

	@Override
	public BlockState overlay(BlockView world, BlockPos pos, BlockState existing, BlockState placed) {
		if (placed.getBlock() != this)
			return existing;

		TrackShape existingShape = existing.get(SHAPE);
		TrackShape placedShape = placed.get(SHAPE);
		TrackShape combinedShape = null;

		for (boolean flip : Iterate.trueAndFalse) {
			TrackShape s1 = flip ? existingShape : placedShape;
			TrackShape s2 = flip ? placedShape : existingShape;
			if (s1 == TrackShape.XO && s2 == TrackShape.ZO)
				combinedShape = TrackShape.CR_O;
			if (s1 == TrackShape.PD && s2 == TrackShape.ND)
				combinedShape = TrackShape.CR_D;
			if (s1 == TrackShape.XO && s2 == TrackShape.PD)
				combinedShape = TrackShape.CR_PDX;
			if (s1 == TrackShape.ZO && s2 == TrackShape.PD)
				combinedShape = TrackShape.CR_PDZ;
			if (s1 == TrackShape.XO && s2 == TrackShape.ND)
				combinedShape = TrackShape.CR_NDX;
			if (s1 == TrackShape.ZO && s2 == TrackShape.ND)
				combinedShape = TrackShape.CR_NDZ;
		}

		if (combinedShape != null)
			existing = existing.with(SHAPE, combinedShape);
		return existing;
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation pRotation) {
		return state.with(SHAPE, state.get(SHAPE)
			.rotate(pRotation));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror pMirror) {
		return state.with(SHAPE, state.get(SHAPE)
			.mirror(pMirror));
	}

	@Override
	public BlockState getBogeyAnchor(BlockView world, BlockPos pos, BlockState state) {
		return AllBlocks.SMALL_BOGEY.getDefaultState()
			.with(Properties.HORIZONTAL_AXIS, state.get(SHAPE) == TrackShape.XO ? Axis.X : Axis.Z);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public PartialModel prepareAssemblyOverlay(BlockView world, BlockPos pos, BlockState state, Direction direction,
		MatrixStack ms) {
		TransformStack.cast(ms)
			.rotateCentered(Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(direction)));
		return AllPartialModels.TRACK_ASSEMBLING_OVERLAY;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public PartialModel prepareTrackOverlay(BlockView world, BlockPos pos, BlockState state,
		BezierTrackPointLocation bezierPoint, AxisDirection direction, MatrixStack ms, RenderedTrackOverlayType type) {
		TransformStack msr = TransformStack.cast(ms);

		Vec3d axis = null;
		Vec3d diff = null;
		Vec3d normal = null;
		Vec3d offset = null;

		if (bezierPoint != null && world.getBlockEntity(pos) instanceof TrackBlockEntity trackBE) {
			BezierConnection bc = trackBE.connections.get(bezierPoint.curveTarget());
			if (bc != null) {
				double length = MathHelper.floor(bc.getLength() * 2);
				int seg = bezierPoint.segment() + 1;
				double t = seg / length;
				double tpre = (seg - 1) / length;
				double tpost = (seg + 1) / length;

				offset = bc.getPosition(t);
				normal = bc.getNormal(t);
				diff = bc.getPosition(tpost)
					.subtract(bc.getPosition(tpre))
					.normalize();

				msr.translate(offset.subtract(Vec3d.ofBottomCenter(pos)));
				msr.translate(0, -4 / 16f, 0);
			} else
				return null;
		}

		if (normal == null) {
			axis = state.get(SHAPE)
				.getAxes()
				.get(0);
			diff = axis.multiply(direction.offset())
				.normalize();
			normal = getUpNormal(world, pos, state);
		}

		Vec3d angles = TrackRenderer.getModelAngles(normal, diff);

		msr.centre()
			.rotateYRadians(angles.y)
			.rotateXRadians(angles.x)
			.unCentre();

		if (axis != null)
			msr.translate(0, axis.y != 0 ? 7 / 16f : 0, axis.y != 0 ? direction.offset() * 2.5f / 16f : 0);
		else {
			msr.translate(0, 4 / 16f, 0);
			if (direction == AxisDirection.NEGATIVE)
				msr.rotateCentered(Direction.UP, MathHelper.PI);
		}

		if (bezierPoint == null && world.getBlockEntity(pos) instanceof TrackBlockEntity trackTE
			&& trackTE.isTilted()) {
			double yOffset = 0;
			for (BezierConnection bc : trackTE.connections.values())
				yOffset += bc.starts.getFirst().y - pos.getY();
			msr.centre()
				.rotateX(-direction.offset() * trackTE.tilt.smoothingAngle.get())
				.unCentre()
				.translate(0, yOffset / 2, 0);
		}

		return switch (type) {
		case DUAL_SIGNAL -> AllPartialModels.TRACK_SIGNAL_DUAL_OVERLAY;
		case OBSERVER -> AllPartialModels.TRACK_OBSERVER_OVERLAY;
		case SIGNAL -> AllPartialModels.TRACK_SIGNAL_OVERLAY;
		case STATION -> AllPartialModels.TRACK_STATION_OVERLAY;
		};
	}

	@Override
	public boolean trackEquals(BlockState state1, BlockState state2) {
		return state1.getBlock() == this && state2.getBlock() == this
			&& state1.with(HAS_BE, false) == state2.with(HAS_BE, false);
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		int sameTypeTrackAmount = 1;
		Object2IntMap<TrackMaterial> otherTrackAmounts = new Object2IntArrayMap<>();
		int girderAmount = 0;

		if (be instanceof TrackBlockEntity track) {
			for (BezierConnection bezierConnection : track.getConnections()
				.values()) {
				if (!bezierConnection.isPrimary())
					continue;
				TrackMaterial material = bezierConnection.getMaterial();
				if (material == getMaterial()) {
					sameTypeTrackAmount += bezierConnection.getTrackItemCost();
				} else {
					otherTrackAmounts.put(material, otherTrackAmounts.getOrDefault(material, 0) + 1);
				}
				girderAmount += bezierConnection.getGirderItemCost();
			}
		}

		List<ItemStack> stacks = new ArrayList<>();
		while (sameTypeTrackAmount > 0) {
			stacks.add(new ItemStack(state.getBlock(), Math.min(sameTypeTrackAmount, 64)));
			sameTypeTrackAmount -= 64;
		}
		for (TrackMaterial material : otherTrackAmounts.keySet()) {
			int amt = otherTrackAmounts.getOrDefault(material, 0);
			while (amt > 0) {
				stacks.add(material.asStack(Math.min(amt, 64)));
				amt -= 64;
			}
		}
		while (girderAmount > 0) {
			stacks.add(AllBlocks.METAL_GIRDER.asStack(Math.min(girderAmount, 64)));
			girderAmount -= 64;
		}

		return new ItemRequirement(ItemUseType.CONSUME, stacks);
	}

	@Override
	public TrackMaterial getMaterial() {
		return material;
	}

//	public static class RenderProperties extends ReducedDestroyEffects implements MultiPosDestructionHandler {
		@Override
		@Nullable
		@Environment(EnvType.CLIENT)
		public Set<BlockPos> getExtraPositions(ClientWorld level, BlockPos pos, BlockState blockState,
											   int progress) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof TrackBlockEntity track) {
				return new HashSet<>(track.connections.keySet());
			}
			return null;
		}
//	}

}
