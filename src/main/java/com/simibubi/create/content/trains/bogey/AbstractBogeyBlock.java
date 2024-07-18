package com.simibubi.create.content.trains.bogey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.RegisteredObjects;

public abstract class AbstractBogeyBlock<T extends AbstractBogeyBlockEntity> extends Block implements IBE<T>, ProperWaterloggedBlock, ISpecialBlockItemRequirement, IWrenchable {
	public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;
	static final List<Identifier> BOGEYS = new ArrayList<>();
	public BogeySizes.BogeySize size;


	public AbstractBogeyBlock(Settings pProperties, BogeySizes.BogeySize size) {
		super(pProperties);
		setDefaultState(getDefaultState().with(WATERLOGGED, false));
		this.size = size;
	}

	public boolean isOnIncompatibleTrack(Carriage carriage, boolean leading) {
		TravellingPoint point = leading ? carriage.getLeadingPoint() : carriage.getTrailingPoint();
		CarriageBogey bogey = leading ? carriage.leadingBogey() : carriage.trailingBogey();
		TrackEdge currentEdge = point.edge;
		if (currentEdge == null)
			return false;
		return currentEdge.getTrackMaterial().trackType != getTrackType(bogey.getStyle());
	}

	public Set<TrackMaterial.TrackType> getValidPathfindingTypes(BogeyStyle style) {
		return ImmutableSet.of(getTrackType(style));
	}

	public abstract TrackMaterial.TrackType getTrackType(BogeyStyle style);

	/**
	 * Only for internal Create use. If you have your own style set, do not call this method
	 */
	@Deprecated
	public static void registerStandardBogey(Identifier block) {
		BOGEYS.add(block);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AXIS, WATERLOGGED);
		super.appendProperties(builder);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pDirection, BlockState pNeighborState,
								  WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return pState;
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	static final EnumSet<Direction> STICKY_X = EnumSet.of(Direction.EAST, Direction.WEST);
	static final EnumSet<Direction> STICKY_Z = EnumSet.of(Direction.SOUTH, Direction.NORTH);

	public EnumSet<Direction> getStickySurfaces(BlockView world, BlockPos pos, BlockState state) {
		return state.get(Properties.HORIZONTAL_AXIS) == Direction.Axis.X ? STICKY_X : STICKY_Z;
	}

	public abstract double getWheelPointSpacing();

	public abstract double getWheelRadius();

	public Vec3d getConnectorAnchorOffset(boolean upsideDown) {
		return getConnectorAnchorOffset();
	}

	/**
	 * This should be implemented, but not called directly
	 */
	protected abstract Vec3d getConnectorAnchorOffset();

	public boolean allowsSingleBogeyCarriage() {
		return true;
	}

	public abstract BogeyStyle getDefaultStyle();

	/**
	 * Legacy system doesn't capture bogey block entities when constructing a train
	 */
	public boolean captureBlockEntityForTrain() {
		return false;
	}

	@Environment(EnvType.CLIENT)
	public void render(@Nullable BlockState state, float wheelAngle, MatrixStack ms, float partialTicks,
		VertexConsumerProvider buffers, int light, int overlay, BogeyStyle style, NbtCompound bogeyData) {
		if (style == null)
			style = getDefaultStyle();

		final Optional<BogeyRenderer.CommonRenderer> commonRenderer
				= style.getInWorldCommonRenderInstance();
		final BogeyRenderer renderer = style.getInWorldRenderInstance(this.getSize());
		if (state != null) {
			ms.translate(.5f, .5f, .5f);
			if (state.get(AXIS) == Direction.Axis.X)
				ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
		}
		ms.translate(0, -1.5 - 1 / 128f, 0);
		VertexConsumer vb = buffers.getBuffer(RenderLayer.getCutoutMipped());
		if (bogeyData == null)
			bogeyData = new NbtCompound();
		renderer.render(bogeyData, wheelAngle, ms, light, vb, state == null);
		NbtCompound finalBogeyData = bogeyData;
		commonRenderer.ifPresent(common ->
				common.render(finalBogeyData, wheelAngle, ms, light, vb, state == null));
	}

	public BogeySizes.BogeySize getSize() {
		return this.size;
	}

	public Direction getBogeyUpDirection() {
		return Direction.UP;
	}

	public boolean isTrackAxisAlongFirstCoordinate(BlockState state) {
		return state.get(AXIS) == Direction.Axis.X;
	}

	@Nullable
	public BlockState getMatchingBogey(Direction upDirection, boolean axisAlongFirst) {
		if (upDirection != Direction.UP)
			return null;
		return getDefaultState().with(AXIS, axisAlongFirst ? Direction.Axis.X : Direction.Axis.Z);
	}

	@Override
	public final ActionResult onUse(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand,
								 BlockHitResult hit) {
		if (level.isClient)
			return ActionResult.PASS;
		ItemStack stack = player.getStackInHand(hand);

		if (!player.isSneaking() && stack.isOf(AllItems.WRENCH.get()) && !player.getItemCooldownManager().isCoolingDown(stack.getItem())
				&& AllBogeyStyles.BOGEY_STYLES.size() > 1) {

			BlockEntity be = level.getBlockEntity(pos);

			if (!(be instanceof AbstractBogeyBlockEntity sbbe))
				return ActionResult.FAIL;

			player.getItemCooldownManager().set(stack.getItem(), 20);
			BogeyStyle currentStyle = sbbe.getStyle();

			BogeySizes.BogeySize size = getSize();

			BogeyStyle style = this.getNextStyle(currentStyle);
			if (style == currentStyle)
				return ActionResult.PASS;

			Set<BogeySizes.BogeySize> validSizes = style.validSizes();

			for (int i = 0; i < BogeySizes.count(); i++) {
				if (validSizes.contains(size)) break;
				size = size.increment();
			}

			sbbe.setBogeyStyle(style);

			NbtCompound defaultData = style.defaultData;
			sbbe.setBogeyData(sbbe.getBogeyData().copyFrom(defaultData));

			if (size == getSize()) {
				player.sendMessage(Lang.translateDirect("bogey.style.updated_style")
						.append(": ").append(style.displayName), true);
			} else {
				NbtCompound oldData = sbbe.getBogeyData();
				level.setBlockState(pos, this.getStateOfSize(sbbe, size), 3);
				BlockEntity newBlockEntity = level.getBlockEntity(pos);
				if (!(newBlockEntity instanceof AbstractBogeyBlockEntity newBlockEntity1))
					return ActionResult.FAIL;
				newBlockEntity1.setBogeyData(oldData);
				player.sendMessage(Lang.translateDirect("bogey.style.updated_style_and_size")
						.append(": ").append(style.displayName), true);
			}

			return ActionResult.CONSUME;
		}

		return onInteractWithBogey(state, level, pos, player, hand, hit);
	}

	// Allows for custom interactions with bogey block to be added simply
	protected ActionResult onInteractWithBogey(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand,
													BlockHitResult hit) {
		return ActionResult.PASS;
	}

	/**
	 * If, instead of using the style-based cycling system you prefer to use separate blocks, return them from this method
	 */
	protected List<Identifier> getBogeyBlockCycle() {
		return BOGEYS;
	}

	@Override
	public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
		Block block = state.getBlock();
		List<Identifier> bogeyCycle = getBogeyBlockCycle();
		int indexOf = bogeyCycle.indexOf(RegisteredObjects.getKeyOrThrow(block));
		if (indexOf == -1)
			return state;
		int index = (indexOf + 1) % bogeyCycle.size();
		Direction bogeyUpDirection = getBogeyUpDirection();
		boolean trackAxisAlongFirstCoordinate = isTrackAxisAlongFirstCoordinate(state);

		while (index != indexOf) {
			Identifier id = bogeyCycle.get(index);
			Block newBlock = Registries.BLOCK.get(id);
			if (newBlock instanceof AbstractBogeyBlock<?> bogey) {
				BlockState matchingBogey = bogey.getMatchingBogey(bogeyUpDirection, trackAxisAlongFirstCoordinate);
				if (matchingBogey != null)
					return copyProperties(state, matchingBogey);
			}
			index = (index + 1) % bogeyCycle.size();
		}

		return state;
	}

	public BlockState getNextSize(World level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof AbstractBogeyBlockEntity sbbe)
			return this.getNextSize(sbbe);
		return level.getBlockState(pos);
	}

	/**
	 * List of BlockState Properties to copy between sizes
	 */
	public List<Property<?>> propertiesToCopy() {
		return ImmutableList.of(WATERLOGGED, AXIS);
	}

	// generic method needed to satisfy Property and BlockState's generic requirements
	private <V extends Comparable<V>> BlockState copyProperty(BlockState source, BlockState target, Property<V> property) {
		if (source.contains(property) && target.contains(property)) {
			return target.with(property, source.get(property));
		}
		return target;
	}

	private BlockState copyProperties(BlockState source, BlockState target) {
		for (Property<?> property : propertiesToCopy())
			target = copyProperty(source, target, property);
		return target;
	}

	public BlockState getNextSize(AbstractBogeyBlockEntity sbte) {
		BogeySizes.BogeySize size = this.getSize();
		BogeyStyle style = sbte.getStyle();
		BlockState nextBlock = style.getNextBlock(size).getDefaultState();
		nextBlock = copyProperties(sbte.getCachedState(), nextBlock);
		return nextBlock;
	}

	public BlockState getStateOfSize(AbstractBogeyBlockEntity sbte, BogeySizes.BogeySize size) {
		BogeyStyle style = sbte.getStyle();
		BlockState state = style.getBlockOfSize(size).getDefaultState();
		return copyProperties(sbte.getCachedState(), state);
	}

	public BogeyStyle getNextStyle(World level, BlockPos pos) {
		BlockEntity te = level.getBlockEntity(pos);
		if (te instanceof AbstractBogeyBlockEntity sbbe)
			return this.getNextStyle(sbbe.getStyle());
		return getDefaultStyle();
	}

	public BogeyStyle getNextStyle(BogeyStyle style) {
		Collection<BogeyStyle> allStyles = style.getCycleGroup().values();
		if (allStyles.size() <= 1)
			return style;
		List<BogeyStyle> list = new ArrayList<>(allStyles);
		return Iterate.cycleValue(list, style);
	}


	@Override
	public @NotNull BlockState rotate(@NotNull BlockState pState, BlockRotation pRotation) {
		return switch (pRotation) {
			case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> pState.cycle(AXIS);
			default -> pState;
		};
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity te) {
		return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, AllBlocks.RAILWAY_CASING.asStack());
	}

	public boolean canBeUpsideDown() {
		return false;
	}

	public boolean isUpsideDown(BlockState state) {
		return false;
	}

	public BlockState getVersion(BlockState base, boolean upsideDown) {
		return base;
	}
}
