package com.simibubi.create.content.legacy;

import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.BeaconBlockEntityAccessor;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CRecipes;

import io.github.fabricators_of_create.porting_lib.block.LightEmissiveBlock;
import io.github.fabricators_of_create.porting_lib.item.CustomMaxCountItem;
import io.github.fabricators_of_create.porting_lib.item.EntityTickListenerItem;

public class ChromaticCompoundItem extends Item implements CustomMaxCountItem, EntityTickListenerItem {

	public ChromaticCompoundItem(Settings properties) {
		super(properties);
	}

	public int getLight(ItemStack stack) {
		return stack.getOrCreateNbt()
			.getInt("CollectingLight");
	}

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return getLight(stack) > 0;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		return Math.round(13.0F * getLight(stack) / AllConfigs.server().recipes.lightSourceCountForRefinedRadiance.get());
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		return Color.mixColors(0x413c69, 0xFFFFFF,
			getLight(stack) / (float) AllConfigs.server().recipes.lightSourceCountForRefinedRadiance.get());
	}

	@Override
	public int getItemStackLimit(ItemStack stack) {
		return isItemBarVisible(stack) ? 1 : 16;
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		World world = entity.getWorld();
		NbtCompound itemData = entity.getStack()
			.getOrCreateNbt();
		Vec3d positionVec = entity.getPos();
		CRecipes config = AllConfigs.server().recipes;

		if (world.isClient) {
			int light = itemData.getInt("CollectingLight");
			if (world.random.nextInt(config.lightSourceCountForRefinedRadiance.get() + 20) < light) {
				Vec3d start = VecHelper.offsetRandomly(positionVec, world.random, 3);
				Vec3d motion = positionVec.subtract(start)
					.normalize()
					.multiply(.2f);
				world.addParticle(ParticleTypes.END_ROD, start.x, start.y, start.z, motion.x, motion.y, motion.z);
			}
			return false;
		}

		double y = entity.getY();
		double yMotion = entity.getVelocity().y;
		int minHeight = world.getBottomY();
		NbtCompound data = entity.getCustomData();

		// Convert to Shadow steel if in void
		if (y < minHeight && y - yMotion < -10 + minHeight && config.enableShadowSteelRecipe.get()) {
			ItemStack newStack = AllItems.SHADOW_STEEL.asStack();
			newStack.setCount(stack.getCount());
			data.putBoolean("JustCreated", true);
			entity.setStack(newStack);
		}

		if (!config.enableRefinedRadianceRecipe.get())
			return false;

		// Convert to Refined Radiance if eaten enough light sources
		if (itemData.getInt("CollectingLight") >= config.lightSourceCountForRefinedRadiance.get()) {
			ItemStack newStack = AllItems.REFINED_RADIANCE.asStack();
			ItemEntity newEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), newStack);
			newEntity.setVelocity(entity.getVelocity());
			newEntity.getCustomData()
				.putBoolean("JustCreated", true);
			itemData.remove("CollectingLight");
			world.spawnEntity(newEntity);

			stack.split(1);
			entity.setStack(stack);
			if (stack.isEmpty())
				entity.discard();
			return false;
		}

		// Is inside beacon beam?
		boolean isOverBeacon = false;
		int entityX = MathHelper.floor(entity.getX());
		int entityZ = MathHelper.floor(entity.getZ());
		int localWorldHeight = world.getTopY(Heightmap.Type.WORLD_SURFACE, entityX, entityZ);

		BlockPos.Mutable testPos =
			new BlockPos.Mutable(entityX, Math.min(MathHelper.floor(entity.getY()), localWorldHeight), entityZ);

		while (testPos.getY() > 0) {
			testPos.move(Direction.DOWN);
			BlockState state = world.getBlockState(testPos);
			if (state.getOpacity(world, testPos) >= 15 && state.getBlock() != Blocks.BEDROCK)
				break;
			if (state.getBlock() == Blocks.BEACON) {
				BlockEntity be = world.getBlockEntity(testPos);

				if (!(be instanceof BeaconBlockEntity))
					break;

				BeaconBlockEntity bte = (BeaconBlockEntity) be;

				if (!((BeaconBlockEntityAccessor) bte).port_lib$getBeamSections().isEmpty())
					isOverBeacon = true;

				break;
			}
		}

		if (isOverBeacon) {
			ItemStack newStack = AllItems.REFINED_RADIANCE.asStack();
			newStack.setCount(stack.getCount());
			data.putBoolean("JustCreated", true);
			entity.setStack(newStack);
			return false;
		}

		// Find a light source and eat it.
		net.minecraft.util.math.random.Random r = world.random;
		int range = 3;
		float rate = 1 / 2f;
		if (r.nextFloat() > rate)
			return false;

		BlockPos randomOffset = BlockPos.ofFloored(VecHelper.offsetRandomly(positionVec, r, range));
		BlockState state = world.getBlockState(randomOffset);

		TransportedItemStackHandlerBehaviour behaviour =
			BlockEntityBehaviour.get(world, randomOffset, TransportedItemStackHandlerBehaviour.TYPE);

		// Find a placed light source
		if (behaviour == null) {
			if (checkLight(stack, entity, world, itemData, positionVec, randomOffset, state))
				world.breakBlock(randomOffset, false);
			return false;
		}

		// Find a light source from a depot/belt (chunk rebuild safe)
		MutableBoolean success = new MutableBoolean(false);
		behaviour.handleProcessingOnAllItems(ts -> {

			ItemStack heldStack = ts.stack;
			if (!(heldStack.getItem() instanceof BlockItem))
				return TransportedResult.doNothing();

			BlockItem blockItem = (BlockItem) heldStack.getItem();
			if (blockItem.getBlock() == null)
				return TransportedResult.doNothing();

			BlockState stateToCheck = blockItem.getBlock()
				.getDefaultState();

			if (!success.getValue()
				&& checkLight(stack, entity, world, itemData, positionVec, randomOffset, stateToCheck)) {
				success.setTrue();
				if (ts.stack.getCount() == 1)
					return TransportedResult.removeItem();
				TransportedItemStack left = ts.copy();
				left.stack.decrement(1);
				return TransportedResult.convertTo(left);
			}

			return TransportedResult.doNothing();

		});
		return false;
	}

	public boolean checkLight(ItemStack stack, ItemEntity entity, World world, NbtCompound itemData, Vec3d positionVec,
		BlockPos randomOffset, BlockState state) {
		if(state.getBlock() instanceof LightEmissiveBlock lightEmissiveBlock && lightEmissiveBlock.getLightEmission(state, world, randomOffset) == 0)
			return false;
		else if (state.getLuminance() == 0)
			return false;
		if (state.getHardness(world, randomOffset) == -1)
			return false;
		if (state.getBlock() == Blocks.BEACON)
			return false;

		RaycastContext context = new RaycastContext(positionVec.add(new Vec3d(0, 0.5, 0)), VecHelper.getCenterOf(randomOffset),
			ShapeType.COLLIDER, FluidHandling.NONE, entity);
		if (!randomOffset.equals(world.raycast(context)
			.getBlockPos()))
			return false;

		ItemStack newStack = stack.split(1);
		newStack.getOrCreateNbt()
			.putInt("CollectingLight", itemData.getInt("CollectingLight") + 1);
		ItemEntity newEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), newStack);
		newEntity.setVelocity(entity.getVelocity());
		newEntity.setToDefaultPickupDelay();
		world.spawnEntity(newEntity);
//		entity.lifespan = 6000;
		if (stack.isEmpty())
			entity.discard();
		return true;
	}

}
