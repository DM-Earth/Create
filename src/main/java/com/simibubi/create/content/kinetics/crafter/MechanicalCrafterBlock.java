package com.simibubi.create.content.kinetics.crafter;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler.ConnectedInput;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity.Phase;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pointing;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MechanicalCrafterBlock extends HorizontalKineticBlock
	implements IBE<MechanicalCrafterBlockEntity>, ICogWheel {

	public static final EnumProperty<Pointing> POINTING = EnumProperty.of("pointing", Pointing.class);

	public MechanicalCrafterBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(POINTING, Pointing.UP));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(POINTING));
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(HORIZONTAL_FACING)
			.getAxis();
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		Direction face = context.getSide();
		BlockPos placedOnPos = context.getBlockPos()
			.offset(face.getOpposite());
		BlockState blockState = context.getWorld()
			.getBlockState(placedOnPos);

		if ((blockState.getBlock() != this) || (context.getPlayer() != null && context.getPlayer()
			.isSneaking())) {
			BlockState stateForPlacement = super.getPlacementState(context);
			Direction direction = stateForPlacement.get(HORIZONTAL_FACING);
			if (direction != face)
				stateForPlacement = stateForPlacement.with(POINTING, pointingFromFacing(face, direction));
			return stateForPlacement;
		}

		Direction otherFacing = blockState.get(HORIZONTAL_FACING);
		Pointing pointing = pointingFromFacing(face, otherFacing);
		return getDefaultState().with(HORIZONTAL_FACING, otherFacing)
			.with(POINTING, pointing);
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() == newState.getBlock()) {
			if (getTargetDirection(state) != getTargetDirection(newState)) {
				MechanicalCrafterBlockEntity crafter = CrafterHelper.getCrafter(worldIn, pos);
				if (crafter != null)
					crafter.blockChanged();
			}
		}

		if (state.hasBlockEntity() && !state.isOf(newState.getBlock())) {
			MechanicalCrafterBlockEntity crafter = CrafterHelper.getCrafter(worldIn, pos);
			if (crafter != null) {
				if (crafter.covered)
					Block.dropStack(worldIn, pos, AllItems.CRAFTER_SLOT_COVER.asStack());
				if (!isMoving)
					crafter.ejectWholeGrid();
			}

			for (Direction direction : Iterate.directions) {
				if (direction.getAxis() == state.get(HORIZONTAL_FACING)
					.getAxis())
					continue;

				BlockPos otherPos = pos.offset(direction);
				ConnectedInput thisInput = CrafterHelper.getInput(worldIn, pos);
				ConnectedInput otherInput = CrafterHelper.getInput(worldIn, otherPos);

				if (thisInput == null || otherInput == null)
					continue;
				if (!pos.add(thisInput.data.get(0))
					.equals(otherPos.add(otherInput.data.get(0))))
					continue;

				ConnectedInputHandler.toggleConnection(worldIn, pos, otherPos);
			}
		}

		super.onStateReplaced(state, worldIn, pos, newState, isMoving);
	}

	public static Pointing pointingFromFacing(Direction pointingFace, Direction blockFacing) {
		boolean positive = blockFacing.getDirection() == AxisDirection.POSITIVE;

		Pointing pointing = pointingFace == Direction.DOWN ? Pointing.UP : Pointing.DOWN;
		if (pointingFace == Direction.EAST)
			pointing = positive ? Pointing.LEFT : Pointing.RIGHT;
		if (pointingFace == Direction.WEST)
			pointing = positive ? Pointing.RIGHT : Pointing.LEFT;
		if (pointingFace == Direction.NORTH)
			pointing = positive ? Pointing.LEFT : Pointing.RIGHT;
		if (pointingFace == Direction.SOUTH)
			pointing = positive ? Pointing.RIGHT : Pointing.LEFT;
		return pointing;
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		if (context.getSide() == state.get(HORIZONTAL_FACING)) {
			if (!context.getWorld().isClient)
				KineticBlockEntity.switchToBlockState(context.getWorld(), context.getBlockPos(),
					state.cycle(POINTING));
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		BlockEntity blockEntity = worldIn.getBlockEntity(pos);
		if (!(blockEntity instanceof MechanicalCrafterBlockEntity crafter))
			return ActionResult.PASS;

		ItemStack heldItem = player.getStackInHand(handIn);

		if (AllBlocks.MECHANICAL_ARM.isIn(heldItem))
			return ActionResult.PASS;

		boolean isHand = heldItem.isEmpty() && handIn == Hand.MAIN_HAND;
		boolean wrenched = AllItems.WRENCH.isIn(heldItem);

		if (hit.getSide() == state.get(HORIZONTAL_FACING)) {

			if (crafter.phase != Phase.IDLE && !wrenched) {
				crafter.ejectWholeGrid();
				return ActionResult.SUCCESS;
			}

			if (crafter.phase == Phase.IDLE && !isHand && !wrenched) {
				if (worldIn.isClient)
					return ActionResult.SUCCESS;

				if (AllItems.CRAFTER_SLOT_COVER.isIn(heldItem)) {
					if (crafter.covered)
						return ActionResult.PASS;
					if (!crafter.inventory.isEmpty())
						return ActionResult.PASS;
					crafter.covered = true;
					crafter.markDirty();
					crafter.sendData();
					if (!player.isCreative())
						heldItem.decrement(1);
					return ActionResult.SUCCESS;
				}

				if (heldItem.isEmpty()) // fabric: can't insert empty
					return ActionResult.PASS;
				Storage<ItemVariant> capability = crafter.getItemStorage(null);
				if (capability == null)
					return ActionResult.PASS;
				try (Transaction t = TransferUtil.getTransaction()) {
					long inserted = capability.insert(ItemVariant.of(heldItem), heldItem.getCount(), t);
					if (inserted <= 0)
						return ActionResult.PASS;

					player.setStackInHand(handIn, ItemHandlerHelper.copyStackWithSize(heldItem, (int) (heldItem.getCount() - inserted)));
					t.commit();
					return ActionResult.SUCCESS;
				}
			}

			ItemStack inSlot = crafter.getInventory()
				.getStack(0);
			if (inSlot.isEmpty()) {
				if (crafter.covered && !wrenched) {
					if (worldIn.isClient)
						return ActionResult.SUCCESS;
					crafter.covered = false;
					crafter.markDirty();
					crafter.sendData();
					if (!player.isCreative())
						player.getInventory()
							.offerOrDrop(AllItems.CRAFTER_SLOT_COVER.asStack());
					return ActionResult.SUCCESS;
				}
				return ActionResult.PASS;
			}
			if (!isHand && !ItemHandlerHelper.canItemStacksStack(heldItem, inSlot))
				return ActionResult.PASS;
			if (worldIn.isClient)
				return ActionResult.SUCCESS;
			player.getInventory()
				.offerOrDrop(inSlot);
			crafter.getInventory()
				.setStackInSlot(0, ItemStack.EMPTY);
			crafter.sendData();
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		InvManipulationBehaviour behaviour = BlockEntityBehaviour.get(worldIn, pos, InvManipulationBehaviour.TYPE);
		if (behaviour != null)
			behaviour.onNeighborChanged(fromPos);
	}

	@Override
	public float getParticleTargetRadius() {
		return .85f;
	}

	@Override
	public float getParticleInitialRadius() {
		return .75f;
	}

	public static Direction getTargetDirection(BlockState state) {
		if (!AllBlocks.MECHANICAL_CRAFTER.has(state))
			return Direction.UP;
		Direction facing = state.get(HORIZONTAL_FACING);
		Pointing point = state.get(POINTING);
		Vec3d targetVec = new Vec3d(0, 1, 0);
		targetVec = VecHelper.rotate(targetVec, -point.getXRotation(), Axis.Z);
		targetVec = VecHelper.rotate(targetVec, AngleHelper.horizontalAngle(facing), Axis.Y);
		return Direction.getFacing(targetVec.x, targetVec.y, targetVec.z);
	}

	public static boolean isValidTarget(World world, BlockPos targetPos, BlockState crafterState) {
		BlockState targetState = world.getBlockState(targetPos);
		if (!world.canSetBlock(targetPos))
			return false;
		if (!AllBlocks.MECHANICAL_CRAFTER.has(targetState))
			return false;
		if (crafterState.get(HORIZONTAL_FACING) != targetState.get(HORIZONTAL_FACING))
			return false;
		if (Math.abs(crafterState.get(POINTING)
			.getXRotation()
			- targetState.get(POINTING)
				.getXRotation()) == 180)
			return false;
		return true;
	}

	@Override
	public Class<MechanicalCrafterBlockEntity> getBlockEntityClass() {
		return MechanicalCrafterBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MechanicalCrafterBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.MECHANICAL_CRAFTER.get();
	}

}
