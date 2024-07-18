package com.simibubi.create.content.kinetics.mechanicalArm;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class ArmBlock extends KineticBlock implements IBE<ArmBlockEntity>, ICogWheel {

	public static final BooleanProperty CEILING = BooleanProperty.of("ceiling");

	public ArmBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(CEILING, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> p_206840_1_) {
		super.appendProperties(p_206840_1_.add(CEILING));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(CEILING, ctx.getSide() == Direction.DOWN);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		return state.get(CEILING) ? AllShapes.MECHANICAL_ARM_CEILING : AllShapes.MECHANICAL_ARM;
	}
	
	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onBlockAdded(state, world, pos, oldState, isMoving);
		withBlockEntityDo(world, pos, ArmBlockEntity::redstoneUpdate);
	}
	
	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block p_220069_4_,
		BlockPos p_220069_5_, boolean p_220069_6_) {
		withBlockEntityDo(world, pos, ArmBlockEntity::redstoneUpdate);
	}
	
	@Override
	public Axis getRotationAxis(BlockState state) {
		return Axis.Y;
	}

	@Override
	public Class<ArmBlockEntity> getBlockEntityClass() {
		return ArmBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ArmBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.MECHANICAL_ARM.get();
	}
	
	@Override
	public ActionResult onUse(BlockState p_225533_1_, World world, BlockPos pos, PlayerEntity player,
		Hand hand, BlockHitResult p_225533_6_) {
		ItemStack heldItem = player.getStackInHand(hand);

		if (AllItems.GOGGLES.isIn(heldItem)) {
			ActionResult gogglesResult = onBlockEntityUse(world, pos, ate -> {
				if (ate.goggles)
					return ActionResult.PASS;
				ate.goggles = true;
				ate.notifyUpdate();
				return ActionResult.SUCCESS;
			});
			if (gogglesResult.isAccepted())
				return gogglesResult;
		}

		MutableBoolean success = new MutableBoolean(false);
		withBlockEntityDo(world, pos, be -> {
			if (be.heldItem.isEmpty())
				return;
			success.setTrue();
			if (world.isClient)
				return;
			player.getInventory().offerOrDrop(be.heldItem);
			be.heldItem = ItemStack.EMPTY;
			be.phase = Phase.SEARCH_INPUTS;
			be.markDirty();
			be.sendData();
		});
		
		return success.booleanValue() ? ActionResult.SUCCESS : ActionResult.PASS;
	}

}
