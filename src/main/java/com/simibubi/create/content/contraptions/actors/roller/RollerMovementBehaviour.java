package com.simibubi.create.content.contraptions.actors.roller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity.RollingMode;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.pulley.PulleyContraption;
import com.simibubi.create.content.contraptions.render.ActorInstance;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.entity.TravellingPoint.ITrackSelector;
import com.simibubi.create.content.trains.entity.TravellingPoint.SteerDirection;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class RollerMovementBehaviour extends BlockBreakingMovementBehaviour {

	@Override
	public boolean isActive(MovementContext context) {
		return super.isActive(context) && !(context.contraption instanceof PulleyContraption)
			&& VecHelper.isVecPointingTowards(context.relativeMotion, context.state.get(RollerBlock.FACING));
	}

	@Override
	public boolean hasSpecialInstancedRendering() {
		return true;
	}

	@Nullable
	@Override
	public ActorInstance createInstance(MaterialManager materialManager, VirtualRenderWorld simulationWorld,
		MovementContext context) {
		return new RollerActorInstance(materialManager, simulationWorld, context);
	}

	@Override
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffers) {
		if (!ContraptionRenderDispatcher.canInstance())
			RollerRenderer.renderInContraption(context, renderWorld, matrices, buffers);
	}

	@Override
	public Vec3d getActiveAreaOffset(MovementContext context) {
		return Vec3d.of(context.state.get(RollerBlock.FACING)
			.getVector())
			.multiply(.45)
			.subtract(0, 2, 0);
	}

	@Override
	protected float getBlockBreakingSpeed(MovementContext context) {
		return MathHelper.clamp(super.getBlockBreakingSpeed(context) * 1.5f, 1 / 128f, 16f);
	}

	@Override
	public boolean canBreak(World world, BlockPos breakingPos, BlockState state) {
		for (Direction side : Iterate.directions)
			if (world.getBlockState(breakingPos.offset(side))
				.isIn(BlockTags.PORTALS))
				return false;

		return super.canBreak(world, breakingPos, state) && !state.getCollisionShape(world, breakingPos)
			.isEmpty() && !AllBlocks.TRACK.has(state);
	}

	@Override
	protected DamageSource getDamageSource(World level) {
		return CreateDamageSources.roller(level);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		World world = context.world;
		BlockState stateVisited = world.getBlockState(pos);
		if (!stateVisited.isSolidBlock(world, pos))
			damageEntities(context, pos, world);
		if (world.isClient)
			return;

		List<BlockPos> positionsToBreak = getPositionsToBreak(context, pos);
		if (positionsToBreak.isEmpty()) {
			triggerPaver(context, pos);
			return;
		}

		BlockPos argMax = null;
		double max = -1;
		for (BlockPos toBreak : positionsToBreak) {
			float hardness = context.world.getBlockState(toBreak)
				.getHardness(world, toBreak);
			if (hardness < max)
				continue;
			max = hardness;
			argMax = toBreak;
		}

		if (argMax == null) {
			triggerPaver(context, pos);
			return;
		}

		context.data.put("ReferencePos", NbtHelper.fromBlockPos(pos));
		context.data.put("BreakingPos", NbtHelper.fromBlockPos(argMax));
		context.stall = true;
	}

	@Override
	protected void onBlockBroken(MovementContext context, BlockPos pos, BlockState brokenState) {
		super.onBlockBroken(context, pos, brokenState);
		if (!context.data.contains("ReferencePos"))
			return;

		BlockPos referencePos = NbtHelper.toBlockPos(context.data.getCompound("ReferencePos"));
		for (BlockPos otherPos : getPositionsToBreak(context, referencePos))
			if (!otherPos.equals(pos))
				destroyBlock(context, otherPos);

		triggerPaver(context, referencePos);
		context.data.remove("ReferencePos");
	}

	@Override
	protected void destroyBlock(MovementContext context, BlockPos breakingPos) {
		BlockState blockState = context.world.getBlockState(breakingPos);
		boolean noHarvest = blockState.isIn(BlockTags.NEEDS_IRON_TOOL) || blockState.isIn(BlockTags.NEEDS_STONE_TOOL)
			|| blockState.isIn(BlockTags.NEEDS_DIAMOND_TOOL);

		BlockHelper.destroyBlock(context.world, breakingPos, 1f, stack -> {
			if (noHarvest || context.world.random.nextBoolean())
				return;
			this.dropItem(context, stack);
		});

		super.destroyBlock(context, breakingPos);
	}

	RollerTravellingPoint rollerScout = new RollerTravellingPoint();

	protected List<BlockPos> getPositionsToBreak(MovementContext context, BlockPos visitedPos) {
		ArrayList<BlockPos> positions = new ArrayList<>();

		RollingMode mode = getMode(context);
		if (mode != RollingMode.TUNNEL_PAVE)
			return positions;

		int startingY = 1;
		if (!getStateToPaveWith(context).isAir()) {
			FilterItemStack filter = context.getFilterFromBE();
			if (!ItemHelper	
				.extract(context.contraption.getSharedInventory(),
					stack -> filter.test(context.world, stack), 1, true)
				.isEmpty())
				startingY = 0;
		}

		// Train
		PaveTask profileForTracks = createHeightProfileForTracks(context);
		if (profileForTracks != null) {
			for (Couple<Integer> coords : profileForTracks.keys()) {
				float height = profileForTracks.get(coords);
				BlockPos targetPosition = BlockPos.ofFloored(coords.getFirst(), height, coords.getSecond());
				boolean shouldPlaceSlab = height > Math.floor(height) + .45;
				if (startingY == 1 && shouldPlaceSlab && context.world.getBlockState(targetPosition.up())
					.getOrEmpty(SlabBlock.TYPE)
					.orElse(SlabType.DOUBLE) == SlabType.BOTTOM)
					startingY = 2;
				for (int i = startingY; i <= (shouldPlaceSlab ? 3 : 2); i++)
					if (testBreakerTarget(context, targetPosition.up(i), i))
						positions.add(targetPosition.up(i));
			}
			return positions;
		}

		// Otherwise
		for (int i = startingY; i <= 2; i++)
			if (testBreakerTarget(context, visitedPos.up(i), i))
				positions.add(visitedPos.up(i));

		return positions;
	}

	protected boolean testBreakerTarget(MovementContext context, BlockPos target, int columnY) {
		BlockState stateToPaveWith = getStateToPaveWith(context);
		BlockState stateToPaveWithAsSlab = getStateToPaveWithAsSlab(context);
		BlockState stateAbove = context.world.getBlockState(target);
		if (columnY == 0 && stateAbove.isOf(stateToPaveWith.getBlock()))
			return false;
		if (stateToPaveWithAsSlab != null && columnY == 1 && stateAbove.isOf(stateToPaveWithAsSlab.getBlock()))
			return false;
		return canBreak(context.world, target, stateAbove);
	}

	@Nullable
	protected PaveTask createHeightProfileForTracks(MovementContext context) {
		if (context.contraption == null)
			return null;
		if (!(context.contraption.entity instanceof CarriageContraptionEntity cce))
			return null;
		Carriage carriage = cce.getCarriage();
		if (carriage == null)
			return null;
		Train train = carriage.train;
		if (train == null || train.graph == null)
			return null;

		CarriageBogey mainBogey = carriage.bogeys.getFirst();
		TravellingPoint point = mainBogey.trailing();

		rollerScout.node1 = point.node1;
		rollerScout.node2 = point.node2;
		rollerScout.edge = point.edge;
		rollerScout.position = point.position;

		Axis axis = Axis.X;
		StructureBlockInfo info = context.contraption.getBlocks()
			.get(BlockPos.ORIGIN);
		if (info != null && info.state().contains(StandardBogeyBlock.AXIS))
			axis = info.state().get(StandardBogeyBlock.AXIS);

		Direction orientation = cce.getInitialOrientation();
		Direction rollerFacing = context.state.get(RollerBlock.FACING);

		int step = orientation.getDirection()
			.offset();
		double widthWiseOffset = axis.choose(-context.localPos.getZ(), 0, -context.localPos.getX()) * step;
		double lengthWiseOffset = axis.choose(-context.localPos.getX(), 0, context.localPos.getZ()) * step - 1;

		if (rollerFacing == orientation.rotateYClockwise())
			lengthWiseOffset += 1;

		double distanceToTravel = 2;
		PaveTask heightProfile = new PaveTask(widthWiseOffset, widthWiseOffset);
		ITrackSelector steering = rollerScout.steer(SteerDirection.NONE, new Vec3d(0, 1, 0));

		rollerScout.traversalCallback = (edge, coords) -> {
		};
		rollerScout.travel(train.graph, lengthWiseOffset + 1, steering);

		rollerScout.traversalCallback = (edge, coords) -> {
			if (edge == null)
				return;
			if (edge.isInterDimensional())
				return;
			if (edge.node1.getLocation().dimension != context.world.getRegistryKey())
				return;
			TrackPaverV2.pave(heightProfile, train.graph, edge, coords.getFirst(), coords.getSecond());
		};
		rollerScout.travel(train.graph, distanceToTravel, steering);

		for (Couple<Integer> entry : heightProfile.keys())
			heightProfile.put(entry.getFirst(), entry.getSecond(), context.localPos.getY() + heightProfile.get(entry));

		return heightProfile;
	}

	protected void triggerPaver(MovementContext context, BlockPos pos) {
		BlockState stateToPaveWith = getStateToPaveWith(context);
		BlockState stateToPaveWithAsSlab = getStateToPaveWithAsSlab(context);
		RollingMode mode = getMode(context);
		
		if (mode != RollingMode.TUNNEL_PAVE && stateToPaveWith.isAir())
			return;

		Vec3d directionVec = Vec3d.of(context.state.get(RollerBlock.FACING)
			.rotateYClockwise()
			.getVector());
		directionVec = context.rotation.apply(directionVec);
		PaveResult paveResult = PaveResult.PASS;
		int yOffset = 0;

		List<Pair<BlockPos, Boolean>> paveSet = new ArrayList<>();
		PaveTask profileForTracks = createHeightProfileForTracks(context);
		if (profileForTracks == null)
			paveSet.add(Pair.of(pos, false));
		else
			for (Couple<Integer> coords : profileForTracks.keys()) {
				float height = profileForTracks.get(coords);
				boolean shouldPlaceSlab = height > Math.floor(height) + .45;
				BlockPos targetPosition = BlockPos.ofFloored(coords.getFirst(), height, coords.getSecond());
				paveSet.add(Pair.of(targetPosition, shouldPlaceSlab));
			}

		if (paveSet.isEmpty())
			return;

		while (paveResult == PaveResult.PASS) {
			if (yOffset > AllConfigs.server().kinetics.rollerFillDepth.get()) {
				paveResult = PaveResult.FAIL;
				break;
			}

			Set<Pair<BlockPos, Boolean>> currentLayer = new HashSet<>();
			if (mode == RollingMode.WIDE_FILL) {
				for (Pair<BlockPos, Boolean> anchor : paveSet) {
					int radius = (yOffset + 1) / 2;
					for (int i = -radius; i <= radius; i++)
						for (int j = -radius; j <= radius; j++)
							if (BlockPos.ORIGIN.getManhattanDistance(new BlockPos(i, 0, j)) <= radius)
								currentLayer.add(Pair.of(anchor.getFirst()
									.add(i, -yOffset, j), anchor.getSecond()));
				}
			} else
				for (Pair<BlockPos, Boolean> anchor : paveSet)
					currentLayer.add(Pair.of(anchor.getFirst()
						.down(yOffset), anchor.getSecond()));

			boolean completelyBlocked = true;
			boolean anyBlockPlaced = false;

			for (Pair<BlockPos, Boolean> currentPos : currentLayer) {
				if (stateToPaveWithAsSlab != null && yOffset == 0 && currentPos.getSecond())
					tryFill(context, currentPos.getFirst()
						.up(), stateToPaveWithAsSlab);
				paveResult = tryFill(context, currentPos.getFirst(), stateToPaveWith);
				if (paveResult != PaveResult.FAIL)
					completelyBlocked = false;
				if (paveResult == PaveResult.SUCCESS)
					anyBlockPlaced = true;
			}

			if (anyBlockPlaced)
				paveResult = PaveResult.SUCCESS;
			else if (!completelyBlocked || yOffset == 0)
				paveResult = PaveResult.PASS;

			if (paveResult == PaveResult.SUCCESS && stateToPaveWith.getBlock() instanceof FallingBlock)
				paveResult = PaveResult.PASS;
			if (paveResult != PaveResult.PASS)
				break;
			if (mode == RollingMode.TUNNEL_PAVE)
				break;

			yOffset++;
		}

		if (paveResult == PaveResult.SUCCESS) {
			context.data.putInt("WaitingTicks", 2);
			context.data.put("LastPos", NbtHelper.fromBlockPos(pos));
			context.stall = true;
		}
	}

	public static BlockState getStateToPaveWith(ItemStack itemStack) {
		if (itemStack.getItem()instanceof BlockItem bi) {
			BlockState defaultBlockState = bi.getBlock()
				.getDefaultState();
			if (defaultBlockState.contains(SlabBlock.TYPE))
				defaultBlockState = defaultBlockState.with(SlabBlock.TYPE, SlabType.DOUBLE);
			return defaultBlockState;
		}
		return Blocks.AIR.getDefaultState();
	}

	protected BlockState getStateToPaveWith(MovementContext context) {
		return getStateToPaveWith(ItemStack.fromNbt(context.blockEntityData.getCompound("Filter")));
	}

	protected BlockState getStateToPaveWithAsSlab(MovementContext context) {
		BlockState stateToPaveWith = getStateToPaveWith(context);
		if (stateToPaveWith.contains(SlabBlock.TYPE))
			return stateToPaveWith.with(SlabBlock.TYPE, SlabType.BOTTOM);

		Block block = stateToPaveWith.getBlock();
		if (block == null)
			return null;

		Identifier rl = Registries.BLOCK.getId(block);
		String namespace = rl.getNamespace();
		String blockName = rl.getPath();
		int nameLength = blockName.length();

		List<String> possibleSlabLocations = new ArrayList<>();
		possibleSlabLocations.add(blockName + "_slab");

		if (blockName.endsWith("s") && nameLength > 1)
			possibleSlabLocations.add(blockName.substring(0, nameLength - 1) + "_slab");
		if (blockName.endsWith("planks") && nameLength > 7)
			possibleSlabLocations.add(blockName.substring(0, nameLength - 7) + "_slab");

		for (String locationAttempt : possibleSlabLocations) {
			RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, new Identifier(namespace, locationAttempt));
			Optional<Block> result = Registries.BLOCK.getEntry(key)
				.map(slabHolder -> slabHolder.value());
			if (result.isEmpty())
				continue;
			return result.get()
				.getDefaultState();
		}

		return null;
	}

	protected RollingMode getMode(MovementContext context) {
		return RollingMode.values()[context.blockEntityData.getInt("ScrollValue")];
	}

	private final class RollerTravellingPoint extends TravellingPoint {

		public BiConsumer<TrackEdge, Couple<Double>> traversalCallback;

		@Override
		protected Double edgeTraversedFrom(TrackGraph graph, boolean forward, IEdgePointListener edgePointListener,
			ITurnListener turnListener, double prevPos, double totalDistance) {
			double from = forward ? prevPos : position;
			double to = forward ? position : prevPos;
			traversalCallback.accept(edge, Couple.create(from, to));
			return super.edgeTraversedFrom(graph, forward, edgePointListener, turnListener, prevPos, totalDistance);
		}

	}

	private enum PaveResult {
		FAIL, PASS, SUCCESS;
	}

	protected PaveResult tryFill(MovementContext context, BlockPos targetPos, BlockState toPlace) {
		World level = context.world;
		if (!level.canSetBlock(targetPos))
			return PaveResult.FAIL;
		BlockState existing = level.getBlockState(targetPos);
		if (existing.isOf(toPlace.getBlock()))
			return PaveResult.PASS;
		if (!existing.isIn(BlockTags.LEAVES) && !existing.isReplaceable()
			&& !existing.getCollisionShape(level, targetPos)
				.isEmpty())
			return PaveResult.FAIL;

		FilterItemStack filter = context.getFilterFromBE();
		ItemStack held = ItemHelper.extract(context.contraption.getSharedInventory(),
			stack -> filter.test(context.world, stack), 1, false);
		if (held.isEmpty())
			return PaveResult.FAIL;

		level.setBlockState(targetPos, toPlace);
		return PaveResult.SUCCESS;
	}

}
