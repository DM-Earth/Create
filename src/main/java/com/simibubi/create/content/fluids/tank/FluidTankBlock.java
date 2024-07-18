package com.simibubi.create.content.fluids.tank;

import java.util.function.Consumer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity.CreativeSmartFluidTank;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.FluidHelper.FluidExchange;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.block.CustomSoundTypeBlock;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class FluidTankBlock extends Block implements IWrenchable, IBE<FluidTankBlockEntity>, CustomSoundTypeBlock {

	public static final BooleanProperty TOP = BooleanProperty.of("top");
	public static final BooleanProperty BOTTOM = BooleanProperty.of("bottom");
	public static final EnumProperty<Shape> SHAPE = EnumProperty.of("shape", Shape.class);
	public static final IntProperty LIGHT_LEVEL = IntProperty.of("light_level", 0, 15);

	private boolean creative;

	public static FluidTankBlock regular(Settings p_i48440_1_) {
		return new FluidTankBlock(p_i48440_1_, false);
	}

	public static FluidTankBlock creative(Settings p_i48440_1_) {
		return new FluidTankBlock(p_i48440_1_, true);
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	protected FluidTankBlock(Settings p_i48440_1_, boolean creative) {
		super(setLightFunction(p_i48440_1_));
		this.creative = creative;
		setDefaultState(getDefaultState().with(TOP, true)
			.with(BOTTOM, true)
			.with(SHAPE, Shape.WINDOW)
			.with(LIGHT_LEVEL, 0));
	}

	private static Settings setLightFunction(Settings properties) {
		return properties.luminance(state -> state.get(LIGHT_LEVEL));
	}

	public static boolean isTank(BlockState state) {
		return state.getBlock() instanceof FluidTankBlock;
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved) {
		if (oldState.getBlock() == state.getBlock())
			return;
		if (moved)
			return;
		// fabric: see comment in FluidTankItem
		Consumer<FluidTankBlockEntity> consumer = FluidTankItem.IS_PLACING_NBT
				? FluidTankBlockEntity::queueConnectivityUpdate
				: FluidTankBlockEntity::updateConnectivity;
		withBlockEntityDo(world, pos, consumer);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> p_206840_1_) {
		p_206840_1_.add(TOP, BOTTOM, SHAPE, LIGHT_LEVEL);
	}

	// Handled via LIGHT_LEVEL state property
//	@Override
//	public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
//		FluidTankBlockEntity tankAt = ConnectivityHandler.partAt(getBlockEntityType(), world, pos);
//		if (tankAt == null)
//			return 0;
//		FluidTankBlockEntity controllerBE = tankAt.getControllerBE();
//		if (controllerBE == null || !controllerBE.window)
//			return 0;
//		return tankAt.luminosity;
//	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		withBlockEntityDo(context.getWorld(), context.getBlockPos(), FluidTankBlockEntity::toggleWindows);
		return ActionResult.SUCCESS;
	}

	static final VoxelShape CAMPFIRE_SMOKE_CLIP = Block.createCuboidShape(0, 4, 0, 16, 16, 16);

	@Override
	public VoxelShape getCollisionShape(BlockState pState, BlockView pLevel, BlockPos pPos,
										ShapeContext pContext) {
		if (pContext == ShapeContext.absent())
			return CAMPFIRE_SMOKE_CLIP;
		return pState.getOutlineShape(pLevel, pPos);
	}

	@Override
	public VoxelShape getSidesShape(BlockState pState, BlockView pReader, BlockPos pPos) {
		return VoxelShapes.fullCube();
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pDirection, BlockState pNeighborState,
		WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		if (pDirection == Direction.DOWN && pNeighborState.getBlock() != this)
			withBlockEntityDo(pLevel, pCurrentPos, FluidTankBlockEntity::updateBoilerTemperature);
		return pState;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {
		ItemStack heldItem = player.getStackInHand(hand);
		boolean onClient = world.isClient;

		if (heldItem.isEmpty())
			return ActionResult.PASS;
		if (!player.isCreative() && !creative)
			return ActionResult.PASS;

		FluidExchange exchange = null;
		FluidTankBlockEntity be = ConnectivityHandler.partAt(getBlockEntityType(), world, pos);
		if (be == null)
			return ActionResult.FAIL;

		Direction direction = ray.getSide();
		Storage<FluidVariant> fluidTank = be.getFluidStorage(direction);
		if (fluidTank == null)
			return ActionResult.PASS;

		FluidStack prevFluidInTank = TransferUtil.firstCopyOrEmpty(fluidTank);

		if (FluidHelper.tryEmptyItemIntoBE(world, player, hand, heldItem, be, direction))
			exchange = FluidExchange.ITEM_TO_TANK;
		else if (FluidHelper.tryFillItemFromBE(world, player, hand, heldItem, be, direction))
			exchange = FluidExchange.TANK_TO_ITEM;

		if (exchange == null) {
			if (GenericItemEmptying.canItemBeEmptied(world, heldItem)
				|| GenericItemFilling.canItemBeFilled(world, heldItem))
				return ActionResult.SUCCESS;
			return ActionResult.PASS;
		}

		SoundEvent soundevent = null;
		BlockState fluidState = null;
		FluidStack fluidInTank = TransferUtil.firstOrEmpty(fluidTank);

		if (exchange == FluidExchange.ITEM_TO_TANK) {
			if (creative && !onClient) {
				FluidStack fluidInItem = GenericItemEmptying.emptyItem(world, heldItem, true)
					.getFirst();
				if (!fluidInItem.isEmpty() && fluidTank instanceof CreativeSmartFluidTank)
					((CreativeSmartFluidTank) fluidTank).setContainedFluid(fluidInItem);
			}

			Fluid fluid = fluidInTank.getFluid();
			fluidState = fluid.getDefaultState()
				.getBlockState();
			soundevent = FluidVariantAttributes.getEmptySound(FluidVariant.of(fluid));
		}

		if (exchange == FluidExchange.TANK_TO_ITEM) {
			if (creative && !onClient)
				if (fluidTank instanceof CreativeSmartFluidTank)
					((CreativeSmartFluidTank) fluidTank).setContainedFluid(FluidStack.EMPTY);

			Fluid fluid = prevFluidInTank.getFluid();
			fluidState = fluid.getDefaultState()
				.getBlockState();
			soundevent = FluidVariantAttributes.getFillSound(FluidVariant.of(fluid));
		}

		if (soundevent != null && !onClient) {
			float pitch = MathHelper
				.clamp(1 - (1f * fluidInTank.getAmount() / (FluidTankBlockEntity.getCapacityMultiplier() * 16)), 0, 1);
			pitch /= 1.5f;
			pitch += .5f;
			pitch += (world.random.nextFloat() - .5f) / 4f;
			world.playSound(null, pos, soundevent, SoundCategory.BLOCKS, .5f, pitch);
		}

		if (!fluidInTank.isFluidEqual(prevFluidInTank)) {
			if (be instanceof FluidTankBlockEntity) {
				FluidTankBlockEntity controllerBE = ((FluidTankBlockEntity) be).getControllerBE();
				if (controllerBE != null) {
					if (fluidState != null && onClient) {
						BlockStateParticleEffect blockParticleData =
							new BlockStateParticleEffect(ParticleTypes.BLOCK, fluidState);
						float level = (float) fluidInTank.getAmount() / TransferUtil.firstCapacity(fluidTank);

						boolean reversed = FluidVariantAttributes.isLighterThanAir(fluidInTank.getType());
						if (reversed)
							level = 1 - level;

						Vec3d vec = ray.getPos();
						vec = new Vec3d(vec.x, controllerBE.getPos()
							.getY() + level * (controllerBE.height - .5f) + .25f, vec.z);
						Vec3d motion = player.getPos()
							.subtract(vec)
							.multiply(1 / 20f);
						vec = vec.add(motion);
						world.addParticle(blockParticleData, vec.x, vec.y, vec.z, motion.x, motion.y, motion.z);
						return ActionResult.SUCCESS;
					}

					controllerBE.sendDataImmediately();
					controllerBE.markDirty();
				}
			}
		}

		return ActionResult.SUCCESS;
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (!(be instanceof FluidTankBlockEntity))
				return;
			FluidTankBlockEntity tankBE = (FluidTankBlockEntity) be;
			world.removeBlockEntity(pos);
			ConnectivityHandler.splitMulti(tankBE);
		}
	}

	@Override
	public Class<FluidTankBlockEntity> getBlockEntityClass() {
		return FluidTankBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
		return creative ? AllBlockEntityTypes.CREATIVE_FLUID_TANK.get() : AllBlockEntityTypes.FLUID_TANK.get();
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		if (mirror == BlockMirror.NONE)
			return state;
		boolean x = mirror == BlockMirror.FRONT_BACK;
		switch (state.get(SHAPE)) {
		case WINDOW_NE:
			return state.with(SHAPE, x ? Shape.WINDOW_NW : Shape.WINDOW_SE);
		case WINDOW_NW:
			return state.with(SHAPE, x ? Shape.WINDOW_NE : Shape.WINDOW_SW);
		case WINDOW_SE:
			return state.with(SHAPE, x ? Shape.WINDOW_SW : Shape.WINDOW_NE);
		case WINDOW_SW:
			return state.with(SHAPE, x ? Shape.WINDOW_SE : Shape.WINDOW_NW);
		default:
			return state;
		}
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		for (int i = 0; i < rotation.ordinal(); i++)
			state = rotateOnce(state);
		return state;
	}

	private BlockState rotateOnce(BlockState state) {
		switch (state.get(SHAPE)) {
		case WINDOW_NE:
			return state.with(SHAPE, Shape.WINDOW_SE);
		case WINDOW_NW:
			return state.with(SHAPE, Shape.WINDOW_NE);
		case WINDOW_SE:
			return state.with(SHAPE, Shape.WINDOW_SW);
		case WINDOW_SW:
			return state.with(SHAPE, Shape.WINDOW_NW);
		default:
			return state;
		}
	}

	public enum Shape implements StringIdentifiable {
		PLAIN, WINDOW, WINDOW_NW, WINDOW_SW, WINDOW_NE, WINDOW_SE;

		@Override
		public String asString() {
			return Lang.asId(name());
		}
	}

	// Tanks are less noisy when placed in batch
	public static final BlockSoundGroup SILENCED_METAL =
		new BlockSoundGroup(0.1F, 1.5F, SoundEvents.BLOCK_METAL_BREAK, SoundEvents.BLOCK_METAL_STEP,
			SoundEvents.BLOCK_METAL_PLACE, SoundEvents.BLOCK_METAL_HIT, SoundEvents.BLOCK_METAL_FALL);

	@Override
	public BlockSoundGroup getSoundType(BlockState state, WorldView world, BlockPos pos, Entity entity) {
		BlockSoundGroup soundType = getSoundGroup(state);
		if (entity != null && entity.getCustomData()
			.contains("SilenceTankSound"))
			return SILENCED_METAL;
		return soundType;
	}

	@Override
	public boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState blockState, World worldIn, BlockPos pos) {
		return getBlockEntityOptional(worldIn, pos).map(FluidTankBlockEntity::getControllerBE)
			.map(be -> ComparatorUtil.fractionToRedstoneLevel(be.getFillState()))
			.orElse(0);
	}

	public static void updateBoilerState(BlockState pState, World pLevel, BlockPos tankPos) {
		BlockState tankState = pLevel.getBlockState(tankPos);
		if (!(tankState.getBlock()instanceof FluidTankBlock tank))
			return;
		FluidTankBlockEntity tankBE = tank.getBlockEntity(pLevel, tankPos);
		if (tankBE == null)
			return;
		FluidTankBlockEntity controllerBE = tankBE.getControllerBE();
		if (controllerBE == null)
			return;
		controllerBE.updateBoilerState();
	}

}
