package com.simibubi.create.content.processing.burner;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class LitBlazeBurnerBlock extends Block implements IWrenchable, BlockPickInteractionAware {

//	public static final ToolAction EXTINGUISH_FLAME_ACTION = ToolAction.get(Create.asResource("extinguish_flame").toString());

	public static final EnumProperty<FlameType> FLAME_TYPE = EnumProperty.of("flame_type", FlameType.class);

	public LitBlazeBurnerBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(FLAME_TYPE, FlameType.REGULAR));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(FLAME_TYPE);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult blockRayTraceResult) {
		ItemStack heldItem = player.getStackInHand(hand);

		if (heldItem.getItem() instanceof ShovelItem) {// || heldItem.getItem().canPerformAction(heldItem, EXTINGUISH_FLAME_ACTION)) {
			world.playSound(player, pos, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 0.5f, 2);
			if (world.isClient)
				return ActionResult.SUCCESS;
			heldItem.damage(1, player, p -> p.sendToolBreakStatus(hand));
			world.setBlockState(pos, AllBlocks.BLAZE_BURNER.getDefaultState());
			return ActionResult.SUCCESS;
		}

		if (state.get(FLAME_TYPE) == FlameType.REGULAR) {
			if (heldItem.isIn(ItemTags.SOUL_FIRE_BASE_BLOCKS)) {
				world.playSound(player, pos, SoundEvents.BLOCK_SOUL_SAND_PLACE, SoundCategory.BLOCKS, 1.0f, world.random.nextFloat() * 0.4F + 0.8F);
				if (world.isClient)
					return ActionResult.SUCCESS;
				world.setBlockState(pos, getDefaultState().with(FLAME_TYPE, FlameType.SOUL));
				return ActionResult.SUCCESS;
			}
		}

		return ActionResult.PASS;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView reader, BlockPos pos, ShapeContext context) {
		return AllBlocks.BLAZE_BURNER.get()
			.getOutlineShape(state, reader, pos, context);
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockView view, BlockPos pos, @Nullable PlayerEntity player, @Nullable HitResult result) {
		return AllItems.EMPTY_BLAZE_BURNER.asStack();
	}

	@Environment(EnvType.CLIENT)
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		world.addImportantParticle(ParticleTypes.LARGE_SMOKE, true,
			(double) pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double) (random.nextBoolean() ? 1 : -1),
			(double) pos.getY() + random.nextDouble() + random.nextDouble(),
			(double) pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double) (random.nextBoolean() ? 1 : -1), 0.0D,
			0.07D, 0.0D);

		if (random.nextInt(10) == 0) {
			world.playSound((double) ((float) pos.getX() + 0.5F), (double) ((float) pos.getY() + 0.5F),
				(double) ((float) pos.getZ() + 0.5F), SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS,
				0.25F + random.nextFloat() * .25f, random.nextFloat() * 0.7F + 0.6F, false);
		}

		if (state.get(FLAME_TYPE) == FlameType.SOUL) {
			if (random.nextInt(8) == 0) {
				world.addParticle(ParticleTypes.SOUL,
					pos.getX() + 0.5F + random.nextDouble() / 4 * (random.nextBoolean() ? 1 : -1),
					pos.getY() + 0.3F + random.nextDouble() / 2,
					pos.getZ() + 0.5F + random.nextDouble() / 4 * (random.nextBoolean() ? 1 : -1),
					0.0, random.nextDouble() * 0.04 + 0.04, 0.0);
			}
			return;
		}

		if (random.nextInt(5) == 0) {
			for (int i = 0; i < random.nextInt(1) + 1; ++i) {
				world.addParticle(ParticleTypes.LAVA, (double) ((float) pos.getX() + 0.5F),
					(double) ((float) pos.getY() + 0.5F), (double) ((float) pos.getZ() + 0.5F),
					(double) (random.nextFloat() / 2.0F), 5.0E-5D, (double) (random.nextFloat() / 2.0F));
			}
		}
	}

	@Override
	public boolean hasComparatorOutput(BlockState p_149740_1_) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState state, World p_180641_2_, BlockPos p_180641_3_) {
		return state.get(FLAME_TYPE) == FlameType.REGULAR ? 1 : 2;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView reader, BlockPos pos,
		ShapeContext context) {
		return AllBlocks.BLAZE_BURNER.get()
			.getCollisionShape(state, reader, pos, context);
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	public static int getLight(BlockState state) {
		if (state.get(FLAME_TYPE) == FlameType.SOUL)
			return 9;
		else
			return 12;
	}

	public enum FlameType implements StringIdentifiable {

		REGULAR, SOUL;

		@Override
		public String asString() {
			return Lang.asId(name());
		}

	}

}
