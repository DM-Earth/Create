package com.simibubi.create.content.kinetics.transmission.sequencer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.ITransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.tterrag.registrate.fabric.EnvExecutor;

import io.github.fabricators_of_create.porting_lib.block.WeakPowerCheckingBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class SequencedGearshiftBlock extends HorizontalAxisKineticBlock implements IBE<SequencedGearshiftBlockEntity>, ITransformableBlock, WeakPowerCheckingBlock {

	public static final BooleanProperty VERTICAL = BooleanProperty.of("vertical");
	public static final IntProperty STATE = IntProperty.of("state", 0, 5);

	public SequencedGearshiftBlock(Settings properties) {
		super(properties);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(STATE, VERTICAL));
	}

	@Override
    public boolean shouldCheckWeakPower(BlockState state, RedstoneView level, BlockPos pos, Direction side) {
        return false;
    }

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		if (worldIn.isClient)
			return;
		if (!worldIn.getBlockTickScheduler()
			.isTicking(pos, this))
			worldIn.scheduleBlockTick(pos, this, 0);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random r) {
		boolean previouslyPowered = state.get(STATE) != 0;
		boolean isPowered = worldIn.isReceivingRedstonePower(pos);
		withBlockEntityDo(worldIn, pos, sgte -> sgte.onRedstoneUpdate(isPowered, previouslyPowered));
	}

	@Override
	protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
		return false;
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		if (state.get(VERTICAL))
			return face.getAxis()
				.isVertical();
		return super.hasShaftTowards(world, pos, state, face);
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;
		ItemStack held = player.getMainHandStack();
		if (AllItems.WRENCH.isIn(held))
			return ActionResult.PASS;
		if (held.getItem() instanceof BlockItem) {
			BlockItem blockItem = (BlockItem) held.getItem();
			if (blockItem.getBlock() instanceof KineticBlock && hasShaftTowards(worldIn, pos, state, hit.getSide()))
				return ActionResult.PASS;
		}

		EnvExecutor.runWhenOn(EnvType.CLIENT,
			() -> () -> withBlockEntityDo(worldIn, pos, be -> this.displayScreen(be, player)));
		return ActionResult.SUCCESS;
	}

	@Environment(value = EnvType.CLIENT)
	protected void displayScreen(SequencedGearshiftBlockEntity be, PlayerEntity player) {
		if (player instanceof ClientPlayerEntity)
			ScreenOpener.open(new SequencedGearshiftScreen(be));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		Axis preferredAxis = RotatedPillarKineticBlock.getPreferredAxis(context);
		if (preferredAxis != null && (context.getPlayer() == null || !context.getPlayer()
			.isSneaking()))
			return withAxis(preferredAxis, context);
		return withAxis(context.getPlayerLookDirection()
			.getAxis(), context);
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		BlockState newState = state;

		if (context.getSide()
			.getAxis() != Axis.Y)
			if (newState.get(HORIZONTAL_AXIS) != context.getSide()
				.getAxis())
				newState = newState.cycle(VERTICAL);

		return super.onWrenched(newState, context);
	}

	private BlockState withAxis(Axis axis, ItemPlacementContext context) {
		BlockState state = getDefaultState().with(VERTICAL, axis.isVertical());
		if (axis.isVertical())
			return state.with(HORIZONTAL_AXIS, context.getHorizontalPlayerFacing()
				.getAxis());
		return state.with(HORIZONTAL_AXIS, axis);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		if (state.get(VERTICAL))
			return Axis.Y;
		return super.getRotationAxis(state);
	}

	@Override
	public Class<SequencedGearshiftBlockEntity> getBlockEntityClass() {
		return SequencedGearshiftBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SequencedGearshiftBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SEQUENCED_GEARSHIFT.get();
	}

	@Override
	public boolean hasComparatorOutput(BlockState p_149740_1_) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
		return state.get(STATE)
			.intValue();
	}

	@Override
	public BlockState transform(BlockState state, StructureTransform transform) {
		if (transform.mirror != null) {
			state = mirror(state, transform.mirror);
		}

		if (transform.rotationAxis == Direction.Axis.Y) {
			return rotate(state, transform.rotation);
		}

		if (transform.rotation.ordinal() % 2 == 1) {
			if (transform.rotationAxis != state.get(HORIZONTAL_AXIS)) {
				return state.cycle(VERTICAL);
			} else if (state.get(VERTICAL)) {
				return state.cycle(VERTICAL).cycle(HORIZONTAL_AXIS);
			}
		}
		return state;
	}

}
