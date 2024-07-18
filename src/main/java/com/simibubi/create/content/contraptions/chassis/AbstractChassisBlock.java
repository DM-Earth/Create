package com.simibubi.create.content.contraptions.chassis;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.ITransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public abstract class AbstractChassisBlock extends PillarBlock implements IWrenchable, IBE<ChassisBlockEntity>, ITransformableBlock {

	public AbstractChassisBlock(Settings properties) {
		super(properties);
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (!player.canModifyBlocks())
			return ActionResult.PASS;

		ItemStack heldItem = player.getStackInHand(handIn);
		boolean isSlimeBall = heldItem.isIn(Tags.Items.SLIMEBALLS) || AllItems.SUPER_GLUE.isIn(heldItem);

		BooleanProperty affectedSide = getGlueableSide(state, hit.getSide());
		if (affectedSide == null)
			return ActionResult.PASS;

		if (isSlimeBall && state.get(affectedSide)) {
			for (Direction face : Iterate.directions) {
				BooleanProperty glueableSide = getGlueableSide(state, face);
				if (glueableSide != null && !state.get(glueableSide)
					&& glueAllowedOnSide(worldIn, pos, state, face)) {
					if (worldIn.isClient) {
						Vec3d vec = hit.getPos();
						worldIn.addParticle(ParticleTypes.ITEM_SLIME, vec.x, vec.y, vec.z, 0, 0, 0);
						return ActionResult.SUCCESS;
					}
					AllSoundEvents.SLIME_ADDED.playOnServer(worldIn, pos, .5f, 1);
					state = state.with(glueableSide, true);
				}
			}
			if (!worldIn.isClient)
				worldIn.setBlockState(pos, state);
			return ActionResult.SUCCESS;
		}

		if ((!heldItem.isEmpty() || !player.isSneaking()) && !isSlimeBall)
			return ActionResult.PASS;
		if (state.get(affectedSide) == isSlimeBall)
			return ActionResult.PASS;
		if (!glueAllowedOnSide(worldIn, pos, state, hit.getSide()))
			return ActionResult.PASS;
		if (worldIn.isClient) {
			Vec3d vec = hit.getPos();
			worldIn.addParticle(ParticleTypes.ITEM_SLIME, vec.x, vec.y, vec.z, 0, 0, 0);
			return ActionResult.SUCCESS;
		}

		AllSoundEvents.SLIME_ADDED.playOnServer(worldIn, pos, .5f, 1);
		worldIn.setBlockState(pos, state.with(affectedSide, isSlimeBall));
		return ActionResult.SUCCESS;
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		if (rotation == BlockRotation.NONE)
			return state;

		BlockState rotated = super.rotate(state, rotation);
		for (Direction face : Iterate.directions) {
			BooleanProperty glueableSide = getGlueableSide(rotated, face);
			if (glueableSide != null)
				rotated = rotated.with(glueableSide, false);
		}

		for (Direction face : Iterate.directions) {
			BooleanProperty glueableSide = getGlueableSide(state, face);
			if (glueableSide == null || !state.get(glueableSide))
				continue;
			Direction rotatedFacing = rotation.rotate(face);
			BooleanProperty rotatedGlueableSide = getGlueableSide(rotated, rotatedFacing);
			if (rotatedGlueableSide != null)
				rotated = rotated.with(rotatedGlueableSide, true);
		}

		return rotated;
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirrorIn) {
		if (mirrorIn == BlockMirror.NONE)
			return state;

		BlockState mirrored = state;
		for (Direction face : Iterate.directions) {
			BooleanProperty glueableSide = getGlueableSide(mirrored, face);
			if (glueableSide != null)
				mirrored = mirrored.with(glueableSide, false);
		}

		for (Direction face : Iterate.directions) {
			BooleanProperty glueableSide = getGlueableSide(state, face);
			if (glueableSide == null || !state.get(glueableSide))
				continue;
			Direction mirroredFacing = mirrorIn.apply(face);
			BooleanProperty mirroredGlueableSide = getGlueableSide(mirrored, mirroredFacing);
			if (mirroredGlueableSide != null)
				mirrored = mirrored.with(mirroredGlueableSide, true);
		}

		return mirrored;
	}

	@Override
	public BlockState transform(BlockState state, StructureTransform transform) {
		if (transform.mirror != null) {
			state = mirror(state, transform.mirror);
		}

		if (transform.rotationAxis == Direction.Axis.Y) {
			return rotate(state, transform.rotation);
		}
		return transformInner(state, transform);
	}

	protected BlockState transformInner(BlockState state, StructureTransform transform) {
		if (transform.rotation == BlockRotation.NONE)
			return state;

		BlockState rotated = state.with(AXIS, transform.rotateAxis(state.get(AXIS)));
		AbstractChassisBlock block = (AbstractChassisBlock) state.getBlock();

		for (Direction face : Iterate.directions) {
			BooleanProperty glueableSide = block.getGlueableSide(rotated, face);
			if (glueableSide != null)
				rotated = rotated.with(glueableSide, false);
		}

		for (Direction face : Iterate.directions) {
			BooleanProperty glueableSide = block.getGlueableSide(state, face);
			if (glueableSide == null || !state.get(glueableSide))
				continue;
			Direction rotatedFacing = transform.rotateFacing(face);
			BooleanProperty rotatedGlueableSide = block.getGlueableSide(rotated, rotatedFacing);
			if (rotatedGlueableSide != null)
				rotated = rotated.with(rotatedGlueableSide, true);
		}

		return rotated;
	}

	public abstract BooleanProperty getGlueableSide(BlockState state, Direction face);

	protected boolean glueAllowedOnSide(BlockView world, BlockPos pos, BlockState state, Direction side) {
		return true;
	}

	@Override
	public Class<ChassisBlockEntity> getBlockEntityClass() {
		return ChassisBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ChassisBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.CHASSIS.get();
	}

}
