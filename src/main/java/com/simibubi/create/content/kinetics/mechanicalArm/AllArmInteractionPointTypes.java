package com.simibubi.create.content.kinetics.mechanicalArm;

import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.logistics.chute.AbstractChuteBlock;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock.Shape;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.recipe.CampfireCookingRecipe;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public class AllArmInteractionPointTypes {

	public static final BasinType BASIN = register("basin", BasinType::new);
	public static final BeltType BELT = register("belt", BeltType::new);
	public static final BlazeBurnerType BLAZE_BURNER = register("blaze_burner", BlazeBurnerType::new);
	public static final ChuteType CHUTE = register("chute", ChuteType::new);
	public static final CrafterType CRAFTER = register("crafter", CrafterType::new);
	public static final CrushingWheelsType CRUSHING_WHEELS = register("crushing_wheels", CrushingWheelsType::new);
	public static final DeployerType DEPLOYER = register("deployer", DeployerType::new);
	public static final DepotType DEPOT = register("depot", DepotType::new);
	public static final FunnelType FUNNEL = register("funnel", FunnelType::new);
	public static final MillstoneType MILLSTONE = register("millstone", MillstoneType::new);
	public static final SawType SAW = register("saw", SawType::new);

	public static final CampfireType CAMPFIRE = register("campfire", CampfireType::new);
	public static final ComposterType COMPOSTER = register("composter", ComposterType::new);
	public static final JukeboxType JUKEBOX = register("jukebox", JukeboxType::new);
	public static final RespawnAnchorType RESPAWN_ANCHOR = register("respawn_anchor", RespawnAnchorType::new);

	private static <T extends ArmInteractionPointType> T register(String id, Function<Identifier, T> factory) {
		T type = factory.apply(Create.asResource(id));
		ArmInteractionPointType.register(type);
		return type;
	}

	public static void register() {}

	//

	public static class BasinType extends ArmInteractionPointType {
		public BasinType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return AllBlocks.BASIN.has(state);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new ArmInteractionPoint(this, level, pos, state);
		}
	}

	public static class BeltType extends ArmInteractionPointType {
		public BeltType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return AllBlocks.BELT.has(state) && !(level.getBlockState(pos.up())
				.getBlock() instanceof BeltTunnelBlock);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new BeltPoint(this, level, pos, state);
		}
	}

	public static class BlazeBurnerType extends ArmInteractionPointType {
		public BlazeBurnerType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return AllBlocks.BLAZE_BURNER.has(state);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new BlazeBurnerPoint(this, level, pos, state);
		}
	}

	public static class ChuteType extends ArmInteractionPointType {
		public ChuteType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return AbstractChuteBlock.isChute(state);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new TopFaceArmInteractionPoint(this, level, pos, state);
		}
	}

	public static class CrafterType extends ArmInteractionPointType {
		public CrafterType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return AllBlocks.MECHANICAL_CRAFTER.has(state);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new CrafterPoint(this, level, pos, state);
		}
	}

	public static class CrushingWheelsType extends ArmInteractionPointType {
		public CrushingWheelsType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return AllBlocks.CRUSHING_WHEEL_CONTROLLER.has(state);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new TopFaceArmInteractionPoint(this, level, pos, state);
		}
	}

	public static class DeployerType extends ArmInteractionPointType {
		public DeployerType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return AllBlocks.DEPLOYER.has(state);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new DeployerPoint(this, level, pos, state);
		}
	}

	public static class DepotType extends ArmInteractionPointType {
		public DepotType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return AllBlocks.DEPOT.has(state) || AllBlocks.WEIGHTED_EJECTOR.has(state)
				|| AllBlocks.TRACK_STATION.has(state);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new DepotPoint(this, level, pos, state);
		}
	}

	public static class FunnelType extends ArmInteractionPointType {
		public FunnelType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return state.getBlock() instanceof AbstractFunnelBlock
				&& !(state.contains(FunnelBlock.EXTRACTING) && state.get(FunnelBlock.EXTRACTING))
				&& !(state.contains(BeltFunnelBlock.SHAPE)
					&& state.get(BeltFunnelBlock.SHAPE) == Shape.PUSHING);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new FunnelPoint(this, level, pos, state);
		}
	}

	public static class MillstoneType extends ArmInteractionPointType {
		public MillstoneType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return AllBlocks.MILLSTONE.has(state);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new ArmInteractionPoint(this, level, pos, state);
		}
	}

	public static class SawType extends ArmInteractionPointType {
		public SawType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return AllBlocks.MECHANICAL_SAW.has(state) && state.get(SawBlock.FACING) == Direction.UP
				&& ((KineticBlockEntity) level.getBlockEntity(pos)).getSpeed() != 0;
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new DepotPoint(this, level, pos, state);
		}
	}

	public static class CampfireType extends ArmInteractionPointType {
		public CampfireType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return state.getBlock() instanceof CampfireBlock;
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new CampfirePoint(this, level, pos, state);
		}
	}

	public static class ComposterType extends ArmInteractionPointType {
		public ComposterType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return state.isOf(Blocks.COMPOSTER);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new ComposterPoint(this, level, pos, state);
		}
	}

	public static class JukeboxType extends ArmInteractionPointType {
		public JukeboxType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return state.isOf(Blocks.JUKEBOX);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new JukeboxPoint(this, level, pos, state);
		}
	}

	public static class RespawnAnchorType extends ArmInteractionPointType {
		public RespawnAnchorType(Identifier id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(World level, BlockPos pos, BlockState state) {
			return state.isOf(Blocks.RESPAWN_ANCHOR);
		}

		@Override
		public ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state) {
			return new RespawnAnchorPoint(this, level, pos, state);
		}
	}

	//

	public static class DepositOnlyArmInteractionPoint extends ArmInteractionPoint {
		public DepositOnlyArmInteractionPoint(ArmInteractionPointType type, World level, BlockPos pos,
			BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		public void cycleMode() {}

		@Override
		public ItemStack extract(int amount, TransactionContext ctx) {
			return ItemStack.EMPTY;
		}
	}

	public static class TopFaceArmInteractionPoint extends ArmInteractionPoint {
		public TopFaceArmInteractionPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		protected Vec3d getInteractionPositionVector() {
			return Vec3d.of(pos)
				.add(.5f, 1, .5f);
		}
	}

	public static class BeltPoint extends DepotPoint {
		public BeltPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		public void keepAlive() {
			super.keepAlive();
			BeltBlockEntity beltBE = BeltHelper.getSegmentBE(level, pos);
			if (beltBE == null)
				return;
			TransportedItemStackHandlerBehaviour transport =
				beltBE.getBehaviour(TransportedItemStackHandlerBehaviour.TYPE);
			if (transport == null)
				return;
			MutableBoolean found = new MutableBoolean(false);
			transport.handleProcessingOnAllItems(tis -> {
				if (found.isTrue())
					return TransportedResult.doNothing();
				tis.lockedExternally = true;
				found.setTrue();
				return TransportedResult.doNothing();
			});
		}
	}

	public static class BlazeBurnerPoint extends DepositOnlyArmInteractionPoint {
		public BlazeBurnerPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		public ItemStack insert(ItemStack stack, TransactionContext ctx) {
			ItemStack input = stack.copy();
			TypedActionResult<ItemStack> res =
				BlazeBurnerBlock.tryInsert(cachedState, level, pos, input, false, false, ctx);
			ItemStack remainder = res.getValue();
			if (input.isEmpty()) {
				return remainder;
			} else {
				TransactionCallback.onSuccess(ctx, () ->
						ItemScatterer.spawn(level, pos.getX(), pos.getY(), pos.getZ(), remainder));
				return input;
			}
		}
	}

	public static class CrafterPoint extends ArmInteractionPoint {
		public CrafterPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		protected Direction getInteractionDirection() {
			return cachedState.getOrEmpty(MechanicalCrafterBlock.HORIZONTAL_FACING)
				.orElse(Direction.SOUTH)
				.getOpposite();
		}

		@Override
		protected Vec3d getInteractionPositionVector() {
			return super.getInteractionPositionVector().add(Vec3d.of(getInteractionDirection().getVector())
				.multiply(.5f));
		}

		@Override
		public void updateCachedState() {
			BlockState oldState = cachedState;
			super.updateCachedState();
			if (oldState != cachedState)
				cachedAngles = null;
		}

		@Override
		public ItemStack extract(int amount, TransactionContext ctx) {
			BlockEntity be = level.getBlockEntity(pos);
			if (!(be instanceof MechanicalCrafterBlockEntity))
				return ItemStack.EMPTY;
			MechanicalCrafterBlockEntity crafter = (MechanicalCrafterBlockEntity) be;
			SmartInventory inventory = crafter.getInventory();
			inventory.allowExtraction();
			ItemStack extract = super.extract(amount, ctx);
			inventory.forbidExtraction();
			return extract;
		}
	}

	public static class DeployerPoint extends ArmInteractionPoint {
		public DeployerPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		protected Direction getInteractionDirection() {
			return cachedState.getOrEmpty(DeployerBlock.FACING)
				.orElse(Direction.UP)
				.getOpposite();
		}

		@Override
		protected Vec3d getInteractionPositionVector() {
			return super.getInteractionPositionVector().add(Vec3d.of(getInteractionDirection().getVector())
				.multiply(.65f));
		}

		@Override
		public void updateCachedState() {
			BlockState oldState = cachedState;
			super.updateCachedState();
			if (oldState != cachedState)
				cachedAngles = null;
		}
	}

	public static class DepotPoint extends ArmInteractionPoint {
		public DepotPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		protected Vec3d getInteractionPositionVector() {
			return Vec3d.of(pos)
				.add(.5f, 14 / 16f, .5f);
		}
	}

	public static class FunnelPoint extends DepositOnlyArmInteractionPoint {
		public FunnelPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		protected Vec3d getInteractionPositionVector() {
			Direction funnelFacing = FunnelBlock.getFunnelFacing(cachedState);
			Vec3i normal = funnelFacing != null ? funnelFacing.getVector() : Vec3i.ZERO;
			return VecHelper.getCenterOf(pos)
				.add(Vec3d.of(normal)
					.multiply(-.15f));
		}

		@Override
		protected Direction getInteractionDirection() {
			Direction funnelFacing = FunnelBlock.getFunnelFacing(cachedState);
			return funnelFacing != null ? funnelFacing.getOpposite() : Direction.UP;
		}

		@Override
		public void updateCachedState() {
			BlockState oldState = cachedState;
			super.updateCachedState();
			if (oldState != cachedState)
				cachedAngles = null;
		}

		@Override
		public ItemStack insert(ItemStack stack, TransactionContext ctx) {
			FilteringBehaviour filtering = BlockEntityBehaviour.get(level, pos, FilteringBehaviour.TYPE);
			InvManipulationBehaviour inserter = BlockEntityBehaviour.get(level, pos, InvManipulationBehaviour.TYPE);
			if (cachedState.getOrEmpty(Properties.POWERED)
				.orElse(false))
				return stack;
			if (inserter == null)
				return stack;
			if (filtering != null && !filtering.test(stack))
				return stack;

			// fabric: this is already wrapped in a transaction, no need to simulate
			ItemStack insert = inserter.insert(stack);
			if (insert.getCount() != stack.getCount()) {
				BlockEntity blockEntity = level.getBlockEntity(pos);
				if (blockEntity instanceof FunnelBlockEntity) {
					FunnelBlockEntity funnelBlockEntity = (FunnelBlockEntity) blockEntity;
					TransactionCallback.onSuccess(ctx, () -> {
						funnelBlockEntity.onTransfer(stack);
						if (funnelBlockEntity.hasFlap())
							funnelBlockEntity.flap(true);
					});
				}
			}
			return insert;
		}
	}

	public static class CampfirePoint extends DepositOnlyArmInteractionPoint {
		public CampfirePoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		public ItemStack insert(ItemStack stack, TransactionContext ctx) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (!(blockEntity instanceof CampfireBlockEntity campfireBE))
				return stack;
			Optional<CampfireCookingRecipe> recipe = campfireBE.getRecipeFor(stack);
			if (recipe.isEmpty())
				return stack;
			boolean hasSpace = false;
			for (ItemStack campfireStack : campfireBE.getItemsBeingCooked()) {
				if (campfireStack.isEmpty()) {
					hasSpace = true;
					break;
				}
			}
			if (!hasSpace)
				return stack;
			ItemStack inserted = ItemHandlerHelper.copyStackWithSize(stack, 1);
			TransactionCallback.onSuccess(ctx, () -> campfireBE.addItem(null, inserted, recipe.get().getCookTime()));
			ItemStack remainder = stack.copy();
			remainder.decrement(1);
			return remainder;
		}
	}

	public static class ComposterPoint extends ArmInteractionPoint {
		public ComposterPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		protected Vec3d getInteractionPositionVector() {
			return Vec3d.of(pos)
				.add(.5f, 13 / 16f, .5f);
		}

		// fabric: should not be needed since FAPI handles wrapping Containers
//		@Override
//		public void updateCachedState() {
//			BlockState oldState = cachedState;
//			super.updateCachedState();
//			if (oldState != cachedState)
//				cachedHandler.invalidate();
//		}
//
//		@Nullable
//		@Override
//		protected IItemHandler getHandler() {
//			return null;
//		}
//
//		protected WorldlyContainer getContainer() {
//			ComposterBlock composterBlock = (ComposterBlock) Blocks.COMPOSTER;
//			return composterBlock.getContainer(cachedState, level, pos);
//		}
//
//		@Override
//		public ItemStack insert(ItemStack stack, boolean simulate) {
//			IItemHandler handler = new SidedInvWrapper(getContainer(), Direction.UP);
//			return ItemHandlerHelper.insertItem(handler, stack, simulate);
//		}
//
//		@Override
//		public ItemStack extract(int slot, int amount, boolean simulate) {
//			IItemHandler handler = new SidedInvWrapper(getContainer(), Direction.DOWN);
//			return handler.extractItem(slot, amount, simulate);
//		}
//
//		@Override
//		public int getSlotCount() {
//			return 2;
//		}
	}

	public static class JukeboxPoint extends TopFaceArmInteractionPoint {
		public JukeboxPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		public ItemStack insert(ItemStack stack, TransactionContext ctx) {
			Item item = stack.getItem();
			if (!(item instanceof MusicDiscItem))
				return stack;
			if (cachedState.getOrEmpty(JukeboxBlock.HAS_RECORD)
				.orElse(true))
				return stack;
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (!(blockEntity instanceof JukeboxBlockEntity jukeboxBE))
				return stack;
			if (!jukeboxBE.getStack()
				.isEmpty())
				return stack;
			ItemStack remainder = stack.copy();
			ItemStack toInsert = remainder.split(1);
			level.updateSnapshots(ctx);
			level.setBlockState(pos, cachedState.with(JukeboxBlock.HAS_RECORD, true), 2);
			TransactionCallback.onSuccess(ctx, () -> {
				jukeboxBE.setStack(toInsert);
				level.syncWorldEvent(null, 1010, pos, Item.getRawId(item));
			});
			return remainder;
		}

		@Override
		public ItemStack extract(int amount, TransactionContext ctx) {
			if (!cachedState.getOrEmpty(JukeboxBlock.HAS_RECORD)
				.orElse(false))
				return ItemStack.EMPTY;
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (!(blockEntity instanceof JukeboxBlockEntity jukeboxBE))
				return ItemStack.EMPTY;
			ItemStack record = jukeboxBE.getStack();
			if (record.isEmpty())
				return ItemStack.EMPTY;
			level.updateSnapshots(ctx);
			level.setBlockState(pos, cachedState.with(JukeboxBlock.HAS_RECORD, false), 2);
			TransactionCallback.onSuccess(ctx, () -> {
				level.syncWorldEvent(1010, pos, 0);
				jukeboxBE.clear();
			});
			return record;
		}
	}

	public static class RespawnAnchorPoint extends DepositOnlyArmInteractionPoint {
		public RespawnAnchorPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
			super(type, level, pos, state);
		}

		@Override
		protected Vec3d getInteractionPositionVector() {
			return Vec3d.of(pos)
				.add(.5f, 1, .5f);
		}

		@Override
		public ItemStack insert(ItemStack stack, TransactionContext ctx) {
			if (!stack.isOf(Items.GLOWSTONE))
				return stack;
			if (cachedState.getOrEmpty(RespawnAnchorBlock.CHARGES)
				.orElse(4) == 4)
				return stack;
			TransactionCallback.onSuccess(ctx, () -> RespawnAnchorBlock.charge(null, level, pos, cachedState));
			ItemStack remainder = stack.copy();
			remainder.decrement(1);
			return remainder;
		}
	}

}
