package com.simibubi.create.content.kinetics.crank;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Couple;

import io.github.fabricators_of_create.porting_lib.util.TagUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

@ParametersAreNonnullByDefault
public class ValveHandleBlock extends HandCrankBlock {

	private final DyeColor color;
	private final boolean inCreativeTab;

	public static ValveHandleBlock copper(Settings properties) {
		return new ValveHandleBlock(properties, null, true);
	}

	public static ValveHandleBlock dyed(Settings properties, DyeColor color) {
		return new ValveHandleBlock(properties, color, false);
	}

	private ValveHandleBlock(Settings properties, DyeColor color, boolean inCreativeTab) {
		super(properties);
		this.color = color;
		this.inCreativeTab = inCreativeTab;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.VALVE_HANDLE.get(pState.get(FACING));
	}

	public static ActionResult onBlockActivated(PlayerEntity player, World level, Hand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		BlockState blockState = level.getBlockState(pos);

		if (!(blockState.getBlock() instanceof ValveHandleBlock vhb))
			return ActionResult.PASS;
		if (!player.canModifyBlocks())
			return ActionResult.PASS;
		if (AllItems.WRENCH.isIn(player.getStackInHand(hand)) && player.isSneaking())
			return ActionResult.PASS;

		if (vhb.clicked(level, pos, blockState, player, hand)) {
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	@Override
	public void onStateReplaced(BlockState pState, World pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		if (!(pNewState.getBlock() instanceof ValveHandleBlock))
			super.onStateReplaced(pState, pLevel, pPos, pNewState, pIsMoving);
	}

	public boolean clicked(World level, BlockPos pos, BlockState blockState, PlayerEntity player, Hand hand) {
		ItemStack heldItem = player.getStackInHand(hand);
		DyeColor color = TagUtil.getColorFromStack(heldItem);
		if (color != null && color != this.color) {
			if (!level.isClient)
				level.setBlockState(pos,
					BlockHelper.copyProperties(blockState, AllBlocks.DYED_VALVE_HANDLES.get(color)
						.getDefaultState()));
			return true;
		}

		onBlockEntityUse(level, pos,
			hcbe -> (hcbe instanceof ValveHandleBlockEntity vhbe) && vhbe.activate(player.isSneaking())
				? ActionResult.SUCCESS
				: ActionResult.PASS);
		return true;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult hit) {
		return ActionResult.PASS;
	}

	@Override
	public BlockEntityType<? extends HandCrankBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.VALVE_HANDLE.get();
	}

	@Override
	public int getRotationSpeed() {
		return 32;
	}

	public static Couple<Integer> getSpeedRange() {
		return Couple.create(32, 32);
	}

}
