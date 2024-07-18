package com.simibubi.create.content.contraptions.chassis;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import io.github.fabricators_of_create.porting_lib.block.CustomLandingEffectsBlock;
import io.github.fabricators_of_create.porting_lib.block.CustomRunningEffectsBlock;
import io.github.fabricators_of_create.porting_lib.block.WeakPowerCheckingBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;

public class StickerBlock extends WrenchableDirectionalBlock implements IBE<StickerBlockEntity>, CustomRunningEffectsBlock,
		CustomLandingEffectsBlock, WeakPowerCheckingBlock {

	public static final BooleanProperty POWERED = Properties.POWERED;
	public static final BooleanProperty EXTENDED = Properties.EXTENDED;

	public StickerBlock(Settings p_i48415_1_) {
		super(p_i48415_1_);
		setDefaultState(getDefaultState().with(POWERED, false)
			.with(EXTENDED, false));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		Direction nearestLookingDirection = context.getPlayerLookDirection();
		boolean shouldPower = context.getWorld()
			.isReceivingRedstonePower(context.getBlockPos());
		Direction facing = context.getPlayer() != null && context.getPlayer()
			.isSneaking() ? nearestLookingDirection : nearestLookingDirection.getOpposite();

		return getDefaultState().with(FACING, facing)
			.with(POWERED, shouldPower);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(POWERED, EXTENDED));
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		if (worldIn.isClient)
			return;

		boolean previouslyPowered = state.get(POWERED);
		if (previouslyPowered != worldIn.isReceivingRedstonePower(pos)) {
			state = state.cycle(POWERED);
			if (state.get(POWERED))
				state = state.cycle(EXTENDED);
			worldIn.setBlockState(pos, state, 2);
		}
	}

	@Override
    public boolean shouldCheckWeakPower(BlockState state, RedstoneView level, BlockPos pos, Direction side) {
        return false;
    }

	@Override
	public Class<StickerBlockEntity> getBlockEntityClass() {
		return StickerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends StickerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.STICKER.get();
	}

	// Slime block stuff

	private boolean isUprightSticker(BlockView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		return AllBlocks.STICKER.has(blockState) && blockState.get(FACING) == Direction.UP;
	}

	@Override
	public void onLandedUpon(World p_152426_, BlockState p_152427_, BlockPos p_152428_, Entity p_152429_, float p_152430_) {
		if (!isUprightSticker(p_152426_, p_152428_) || p_152429_.bypassesLandingEffects())
			super.onLandedUpon(p_152426_, p_152427_, p_152428_, p_152429_, p_152430_);
		p_152429_.handleFallDamage(p_152430_, 1.0F, p_152426_.getDamageSources().fall());
	}

	@Override
	public void onEntityLand(BlockView p_176216_1_, Entity p_176216_2_) {
		if (!isUprightSticker(p_176216_1_, p_176216_2_.getBlockPos()
			.down()) || p_176216_2_.bypassesLandingEffects()) {
			super.onEntityLand(p_176216_1_, p_176216_2_);
		} else {
			this.bounceUp(p_176216_2_);
		}
	}

	private void bounceUp(Entity p_226946_1_) {
		Vec3d Vector3d = p_226946_1_.getVelocity();
		if (Vector3d.y < 0.0D) {
			double d0 = p_226946_1_ instanceof LivingEntity ? 1.0D : 0.8D;
			p_226946_1_.setVelocity(Vector3d.x, -Vector3d.y * d0, Vector3d.z);
		}
	}

	@Override
	public void onSteppedOn(World p_152431_, BlockPos p_152432_, BlockState p_152433_, Entity p_152434_) {
		double d0 = Math.abs(p_152434_.getVelocity().y);
		if (d0 < 0.1D && !p_152434_.bypassesSteppingEffects() && isUprightSticker(p_152431_, p_152432_)) {
			double d1 = 0.4D + d0 * 0.2D;
			p_152434_.setVelocity(p_152434_.getVelocity()
				.multiply(d1, 1.0D, d1));
		}
		super.onSteppedOn(p_152431_, p_152432_, p_152433_, p_152434_);
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerWorld worldserver, BlockPos pos, BlockState state2,
		LivingEntity entity, int numberOfParticles) {
		if (isUprightSticker(worldserver, pos)) {
			worldserver.spawnParticles(
				new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.getDefaultState()), entity.getX(),
				entity.getY(), entity.getZ(), numberOfParticles, 0.0D, 0.0D, 0.0D, (double) 0.15F);
			return true;
		}
		return false;
	}

	@Override
	public boolean addRunningEffects(BlockState state, World world, BlockPos pos, Entity entity) {
		if (state.get(FACING) == Direction.UP) {
			Vec3d Vector3d = entity.getVelocity();
			world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.getDefaultState()).setSourcePos(pos),
				entity.getX() + ((double) world.random.nextFloat() - 0.5D) * (double) entity.getWidth(),
				entity.getY() + 0.1D,
				entity.getZ() + ((double) world.random.nextFloat() - 0.5D) * (double) entity.getWidth(),
				Vector3d.x * -4.0D, 1.5D, Vector3d.z * -4.0D);
			return true;
		}
		return false;
	}

}
