package com.simibubi.create.content.decoration.copycat;

import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import com.simibubi.create.foundation.utility.AdventureUtil;

import io.github.fabricators_of_create.porting_lib.block.CustomFrictionBlock;
import io.github.fabricators_of_create.porting_lib.block.CustomLandingEffectsBlock;
import io.github.fabricators_of_create.porting_lib.block.CustomRunningEffectsBlock;
import io.github.fabricators_of_create.porting_lib.block.CustomSoundTypeBlock;
import io.github.fabricators_of_create.porting_lib.block.ExplosionResistanceBlock;
import io.github.fabricators_of_create.porting_lib.block.LightEmissiveBlock;
import io.github.fabricators_of_create.porting_lib.block.ValidSpawnBlock;
import io.github.fabricators_of_create.porting_lib.enchant.EnchantmentBonusBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnRestriction.Location;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.explosion.Explosion;

public abstract class CopycatBlock extends Block implements IBE<CopycatBlockEntity>, IWrenchable,
		CustomFrictionBlock, CustomSoundTypeBlock, LightEmissiveBlock, ExplosionResistanceBlock,
		BlockPickInteractionAware, CustomLandingEffectsBlock, CustomRunningEffectsBlock, EnchantmentBonusBlock,
		ValidSpawnBlock {

	public CopycatBlock(Settings pProperties) {
		super(pProperties);
	}

	@Nullable
	@Override
	public <S extends BlockEntity> BlockEntityTicker<S> getTicker(World p_153212_, BlockState p_153213_,
		BlockEntityType<S> p_153214_) {
		return null;
	}

	@Override
	public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
		onWrenched(state, context);
		return IWrenchable.super.onSneakWrenched(state, context);
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		return onBlockEntityUse(context.getWorld(), context.getBlockPos(), ufte -> {
			ItemStack consumedItem = ufte.getConsumedItem();
			if (!ufte.hasCustomMaterial())
				return ActionResult.PASS;
			PlayerEntity player = context.getPlayer();
			if (!player.isCreative())
				player.getInventory()
					.offerOrDrop(consumedItem);
			context.getWorld()
				.syncWorldEvent(2001, context.getBlockPos(), Block.getRawIdFromState(ufte.getCachedState()));
			ufte.setMaterial(AllBlocks.COPYCAT_BASE.getDefaultState());
			ufte.setConsumedItem(ItemStack.EMPTY);
			return ActionResult.SUCCESS;
		});
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {

		if (pPlayer == null || AdventureUtil.isAdventure(pPlayer))
			return ActionResult.PASS;

		Direction face = pHit.getSide();
		ItemStack itemInHand = pPlayer.getStackInHand(pHand);
		BlockState materialIn = getAcceptedBlockState(pLevel, pPos, itemInHand, face);

		if (materialIn != null)
			materialIn = prepareMaterial(pLevel, pPos, pState, pPlayer, pHand, pHit, materialIn);
		if (materialIn == null)
			return ActionResult.PASS;

		BlockState material = materialIn;
		return onBlockEntityUse(pLevel, pPos, ufte -> {
			if (ufte.getMaterial()
				.isOf(material.getBlock())) {
				if (!ufte.cycleMaterial())
					return ActionResult.PASS;
				ufte.getWorld()
					.playSound(null, ufte.getPos(), SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, .75f,
						.95f);
				return ActionResult.SUCCESS;
			}
			if (ufte.hasCustomMaterial())
				return ActionResult.PASS;
			if (pLevel.isClient())
				return ActionResult.SUCCESS;

			ufte.setMaterial(material);
			ufte.setConsumedItem(itemInHand);
			ufte.getWorld()
				.playSound(null, ufte.getPos(), material.getSoundGroup()
					.getPlaceSound(), SoundCategory.BLOCKS, 1, .75f);

			if (pPlayer.isCreative())
				return ActionResult.SUCCESS;

			itemInHand.decrement(1);
			if (itemInHand.isEmpty())
				pPlayer.setStackInHand(pHand, ItemStack.EMPTY);
			return ActionResult.SUCCESS;
		});
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		if (pPlacer == null)
			return;
		ItemStack offhandItem = pPlacer.getStackInHand(Hand.OFF_HAND);
		BlockState appliedState =
			getAcceptedBlockState(pLevel, pPos, offhandItem, Direction.getEntityFacingOrder(pPlacer)[0]);

		if (appliedState == null)
			return;
		withBlockEntityDo(pLevel, pPos, ufte -> {
			if (ufte.hasCustomMaterial())
				return;

			ufte.setMaterial(appliedState);
			ufte.setConsumedItem(offhandItem);

			if (pPlacer instanceof PlayerEntity player && player.isCreative())
				return;
			offhandItem.decrement(1);
			if (offhandItem.isEmpty())
				pPlacer.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
		});
	}

	@Nullable
	public BlockState getAcceptedBlockState(World pLevel, BlockPos pPos, ItemStack item, Direction face) {
		if (!(item.getItem() instanceof BlockItem bi))
			return null;

		Block block = bi.getBlock();
		if (block instanceof CopycatBlock)
			return null;

		BlockState appliedState = block.getDefaultState();
		boolean hardCodedAllow = isAcceptedRegardless(appliedState);

		if (!AllBlockTags.COPYCAT_ALLOW.matches(block) && !hardCodedAllow) {

			if (AllBlockTags.COPYCAT_DENY.matches(block))
				return null;
			if (block instanceof BlockEntityProvider)
				return null;
			if (block instanceof StairsBlock)
				return null;

			if (pLevel != null) {
				VoxelShape shape = appliedState.getOutlineShape(pLevel, pPos);
				if (shape.isEmpty() || !shape.getBoundingBox()
					.equals(VoxelShapes.fullCube()
						.getBoundingBox()))
					return null;

				VoxelShape collisionShape = appliedState.getCollisionShape(pLevel, pPos);
				if (collisionShape.isEmpty())
					return null;
			}
		}

		if (face != null) {
			Axis axis = face.getAxis();

			if (appliedState.contains(Properties.FACING))
				appliedState = appliedState.with(Properties.FACING, face);
			if (appliedState.contains(Properties.HORIZONTAL_FACING) && axis != Axis.Y)
				appliedState = appliedState.with(Properties.HORIZONTAL_FACING, face);
			if (appliedState.contains(Properties.AXIS))
				appliedState = appliedState.with(Properties.AXIS, axis);
			if (appliedState.contains(Properties.HORIZONTAL_AXIS) && axis != Axis.Y)
				appliedState = appliedState.with(Properties.HORIZONTAL_AXIS, axis);
		}

		return appliedState;
	}

	public boolean isAcceptedRegardless(BlockState material) {
		return false;
	}

	public BlockState prepareMaterial(World pLevel, BlockPos pPos, BlockState pState, PlayerEntity pPlayer,
		Hand pHand, BlockHitResult pHit, BlockState material) {
		return material;
	}

	@Override
	public void onStateReplaced(BlockState pState, World pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		if (!pState.hasBlockEntity() || pState.getBlock() == pNewState.getBlock())
			return;
		if (!pIsMoving)
			withBlockEntityDo(pLevel, pPos, ufte -> Block.dropStack(pLevel, pPos, ufte.getConsumedItem()));
		pLevel.removeBlockEntity(pPos);
	}

	@Override
	public void onBreak(World pLevel, BlockPos pPos, BlockState pState, PlayerEntity pPlayer) {
		super.onBreak(pLevel, pPos, pState, pPlayer);
		if (pPlayer.isCreative())
			withBlockEntityDo(pLevel, pPos, ufte -> ufte.setConsumedItem(ItemStack.EMPTY));
	}

	@Override
	public Class<CopycatBlockEntity> getBlockEntityClass() {
		return CopycatBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CopycatBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.COPYCAT.get();
	}

	// Connected Textures

	@Override
	@Environment(EnvType.CLIENT)
	public BlockState getAppearance(BlockState state, BlockRenderView level, BlockPos pos, Direction side,
		BlockState queryState, BlockPos queryPos) {

		if (isIgnoredConnectivitySide(level, state, side, pos, queryPos))
			return state;

		return CopycatModel.getMaterial(getMaterial(level, pos));
	}

	public boolean isIgnoredConnectivitySide(BlockRenderView reader, BlockState state, Direction face,
		BlockPos fromPos, BlockPos toPos) {
		return false;
	}

	public abstract boolean canConnectTexturesToward(BlockRenderView reader, BlockPos fromPos, BlockPos toPos,
		BlockState state);

	//

	public static BlockState getMaterial(BlockView reader, BlockPos targetPos) {
		if (reader.getBlockEntity(targetPos) instanceof CopycatBlockEntity cbe)
			return cbe.getMaterial();
		return Blocks.AIR.getDefaultState();
	}

	public boolean canFaceBeOccluded(BlockState state, Direction face) {
		return false;
	}

	public boolean shouldFaceAlwaysRender(BlockState state, Direction face) {
		return false;
	}

	// Wrapped properties

	@Override
	public BlockSoundGroup getSoundType(BlockState state, WorldView level, BlockPos pos, Entity entity) {
		return getMaterial(level, pos).getSoundGroup();
	}

	@Override
	public float getFriction(BlockState state, WorldView level, BlockPos pos, Entity entity) {
		return maybeMaterialAs(
				level, pos, CustomFrictionBlock.class,
				(material, block) -> block.getFriction(material, level, pos, entity),
				material -> material.getBlock().getSlipperiness()
		);
	}

	@Override
	public int getLightEmission(BlockState state, BlockView level, BlockPos pos) {
		return maybeMaterialAs(
				level, pos, LightEmissiveBlock.class,
				(material, block) -> block.getLightEmission(material, level, pos),
				AbstractBlockState::getLuminance
		);
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockView level, BlockPos pos, Explosion explosion) {
		return maybeMaterialAs(
				level, pos, ExplosionResistanceBlock.class,
				(material, block) -> block.getExplosionResistance(material, level, pos, explosion),
				material -> material.getBlock().getBlastResistance()
		);
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockView level, BlockPos pos, @Nullable PlayerEntity player, @Nullable HitResult result) {
		BlockState material = getMaterial(level, pos);
		if (AllBlocks.COPYCAT_BASE.has(material) || player != null && player.isSneaking())
			return new ItemStack(this);
		return maybeMaterialAs(
				level, pos, BlockPickInteractionAware.class,
				(mat, block) -> block.getPickedStack(mat, level, pos, player, result),
				mat -> mat.getBlock().getPickStack(level, pos, mat)
		);
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerWorld level, BlockPos pos, BlockState state2,
		LivingEntity entity, int numberOfParticles) {
		return maybeMaterialAs(
				level, pos, CustomLandingEffectsBlock.class, // duplicate material is not a bug
				(material, block) -> block.addLandingEffects(material, level, pos, material, entity, numberOfParticles),
				material -> false // default to vanilla, true cancels
		);
	}

	@Override
	public boolean addRunningEffects(BlockState state, World level, BlockPos pos, Entity entity) {
		return maybeMaterialAs(
				level, pos, CustomRunningEffectsBlock.class,
				(material, block) -> block.addRunningEffects(material, level, pos, entity),
				material -> false // default to vanilla, true cancels
		);
	}

	@Override
	public float getEnchantPowerBonus(BlockState state, WorldView level, BlockPos pos) {
		return maybeMaterialAs(
				level, pos, EnchantmentBonusBlock.class,
				(material, block) -> block.getEnchantPowerBonus(material, level, pos),
				material -> EnchantmentBonusBlock.super.getEnchantPowerBonus(material, level, pos)
		);
	}

	// fabric: unsupported
//	@Override
//	public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
//		return getMaterial(level, pos).canEntityDestroy(level, pos, entity);
//	}

	@Override
	public boolean isValidSpawn(BlockState state, BlockView level, BlockPos pos, Location type,
		EntityType<?> entityType) {
		return false;
	}

	@Override
	public void onLandedUpon(World pLevel, BlockState pState, BlockPos pPos, Entity pEntity, float p_152430_) {
		BlockState material = getMaterial(pLevel, pPos);
		material.getBlock()
			.onLandedUpon(pLevel, material, pPos, pEntity, p_152430_);
	}

	@Override
	public float calcBlockBreakingDelta(BlockState pState, PlayerEntity pPlayer, BlockView pLevel, BlockPos pPos) {
		return getMaterial(pLevel, pPos).calcBlockBreakingDelta(pPlayer, pLevel, pPos);
	}

	//

	@Environment(EnvType.CLIENT)
	public static BlockColorProvider wrappedColor() {
		return new WrappedBlockColor();
	}

	@Environment(EnvType.CLIENT)
	public static class WrappedBlockColor implements BlockColorProvider {

		@Override
		public int getColor(BlockState pState, @Nullable BlockRenderView pLevel, @Nullable BlockPos pPos,
			int pTintIndex) {
			if (pLevel == null || pPos == null)
				return GrassColors.getColor(0.5D, 1.0D);
			return MinecraftClient.getInstance()
				.getBlockColors()
				.getColor(getMaterial(pLevel, pPos), pLevel, pPos, pTintIndex);
		}

	}


	// fabric: util
	private static <T, R> R maybeMaterialAs(BlockView level, BlockPos pos, Class<T> clazz,
											BiFunction<BlockState, T, R> ifType, Function<BlockState, R> ifNot) {
		BlockState material = getMaterial(level, pos);
		Block block = material.getBlock();
		if (clazz.isInstance(block))
			return ifType.apply(material, clazz.cast(block));
		return ifNot.apply(material);
	}


}
