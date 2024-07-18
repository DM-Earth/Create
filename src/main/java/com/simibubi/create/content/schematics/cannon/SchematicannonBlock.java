package com.simibubi.create.content.schematics.cannon;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;

import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;

public class SchematicannonBlock extends Block implements IBE<SchematicannonBlockEntity> {

	public SchematicannonBlock(Settings properties) {
		super(properties);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.SCHEMATICANNON_SHAPE;
	}

	@Override
	public void onPlaced(World level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
		if (entity != null) {
			withBlockEntityDo(level, pos, be -> {
				be.defaultYaw = (-MathHelper.floor((entity.getYaw() + (entity.isSneaking() ? 180.0F : 0.0F)) * 16.0F / 360.0F + 0.5F) & 15) * 360.0F / 16.0F;
			});
		}
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
			BlockHitResult hit) {
		if (worldIn.isClient)
			return ActionResult.SUCCESS;
		withBlockEntityDo(worldIn, pos,
				be -> NetworkHooks.openScreen((ServerPlayerEntity) player, be, be::sendToMenu));
		return ActionResult.SUCCESS;
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		withBlockEntityDo(worldIn, pos, be -> be.neighbourCheckCooldown = 0);
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.hasBlockEntity() || state.getBlock() == newState.getBlock())
			return;

		withBlockEntityDo(worldIn, pos, be -> ItemHelper.dropContents(worldIn, pos, be.inventory));
		worldIn.removeBlockEntity(pos);
	}

	@Override
	public Class<SchematicannonBlockEntity> getBlockEntityClass() {
		return SchematicannonBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SchematicannonBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SCHEMATICANNON.get();
	}

}
