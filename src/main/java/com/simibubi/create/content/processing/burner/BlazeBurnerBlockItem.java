package com.simibubi.create.content.processing.burner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.collection.Weighted.Present;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.MobSpawnerEntry;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllEntityTags;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.BaseSpawnerAccessor;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlazeBurnerBlockItem extends BlockItem {

	private final boolean capturedBlaze;

	public static BlazeBurnerBlockItem empty(Settings properties) {
		return new BlazeBurnerBlockItem(AllBlocks.BLAZE_BURNER.get(), properties, false);
	}

	public static BlazeBurnerBlockItem withBlaze(Block block, Settings properties) {
		return new BlazeBurnerBlockItem(block, properties, true);
	}

	@Override
	public void appendBlocks(Map<Block, Item> p_195946_1_, Item p_195946_2_) {
		if (!hasCapturedBlaze())
			return;
		super.appendBlocks(p_195946_1_, p_195946_2_);
	}

	private BlazeBurnerBlockItem(Block block, Settings properties, boolean capturedBlaze) {
		super(block, properties);
		this.capturedBlaze = capturedBlaze;
	}

	@Override
	public String getTranslationKey() {
		return hasCapturedBlaze() ? super.getTranslationKey() : "item.create." + RegisteredObjects.getKeyOrThrow(this).getPath();
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (hasCapturedBlaze())
			return super.useOnBlock(context);

		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockEntity be = world.getBlockEntity(pos);
		PlayerEntity player = context.getPlayer();

		if (!(be instanceof MobSpawnerBlockEntity))
			return super.useOnBlock(context);

		MobSpawnerLogic spawner = ((MobSpawnerBlockEntity) be).getLogic();

		List<MobSpawnerEntry> possibleSpawns = ((BaseSpawnerAccessor) spawner).port_lib$getSpawnPotentials().getEntries()
			.stream()
			.map(Present::getData)
			.toList();

		if (possibleSpawns.isEmpty()) {
			possibleSpawns = new ArrayList<>();
			possibleSpawns.add(((BaseSpawnerAccessor) spawner).port_lib$getNextSpawnData());
		}

		for (MobSpawnerEntry e : possibleSpawns) {
			Optional<EntityType<?>> optionalEntity = EntityType.fromNbt(e.entity());
			if (optionalEntity.isEmpty() || !AllEntityTags.BLAZE_BURNER_CAPTURABLE.matches(optionalEntity.get()))
				continue;

			spawnCaptureEffects(world, VecHelper.getCenterOf(pos));
			if (world.isClient || player == null)
				return ActionResult.SUCCESS;

			giveBurnerItemTo(player, context.getStack(), context.getHand());
			return ActionResult.SUCCESS;
		}

		return super.useOnBlock(context);
	}

	@Override
	public ActionResult useOnEntity(ItemStack heldItem, PlayerEntity player, LivingEntity entity,
		Hand hand) {
		if (hasCapturedBlaze())
			return ActionResult.PASS;
		if (!AllEntityTags.BLAZE_BURNER_CAPTURABLE.matches(entity))
			return ActionResult.PASS;

		World world = player.getWorld();
		spawnCaptureEffects(world, entity.getPos());
		if (world.isClient)
			return ActionResult.FAIL;

		giveBurnerItemTo(player, heldItem, hand);
		entity.discard();
		return ActionResult.FAIL;
	}

	protected void giveBurnerItemTo(PlayerEntity player, ItemStack heldItem, Hand hand) {
		ItemStack filled = AllBlocks.BLAZE_BURNER.asStack();
		if (!player.isCreative())
			heldItem.decrement(1);
		if (heldItem.isEmpty()) {
			player.setStackInHand(hand, filled);
			return;
		}
		player.getInventory()
			.offerOrDrop(filled);
	}

	private void spawnCaptureEffects(World world, Vec3d vec) {
		if (world.isClient) {
			for (int i = 0; i < 40; i++) {
				Vec3d motion = VecHelper.offsetRandomly(Vec3d.ZERO, world.random, .125f);
				world.addParticle(ParticleTypes.FLAME, vec.x, vec.y, vec.z, motion.x, motion.y, motion.z);
				Vec3d circle = motion.multiply(1, 0, 1)
					.normalize()
					.multiply(.5f);
				world.addParticle(ParticleTypes.SMOKE, circle.x, vec.y, circle.z, 0, -0.125, 0);
			}
			return;
		}

		BlockPos soundPos = BlockPos.ofFloored(vec);
		world.playSound(null, soundPos, SoundEvents.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, .25f, .75f);
		world.playSound(null, soundPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, .5f, .75f);
	}

	public boolean hasCapturedBlaze() {
		return capturedBlaze;
	}

}
