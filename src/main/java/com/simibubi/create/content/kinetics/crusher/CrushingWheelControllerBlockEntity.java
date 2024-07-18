package com.simibubi.create.content.kinetics.crusher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.simibubi.create.AllDamageTypes;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import io.github.fabricators_of_create.porting_lib.util.ItemStackUtil;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class CrushingWheelControllerBlockEntity extends SmartBlockEntity implements SidedStorageBlockEntity {

	public Entity processingEntity;
	private UUID entityUUID;
	protected boolean searchForEntity;

	public ProcessingInventory inventory;
	public float crushingspeed;

	public CrushingWheelControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inventory = new ProcessingInventory(this::itemInserted) {

			@Override
			public boolean isItemValid(int slot, ItemVariant stack, int amount) {
				return super.isItemValid(slot, stack, amount) && processingEntity == null;
			}

		};
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(this::supportsDirectBeltInput));
	}

	private boolean supportsDirectBeltInput(Direction side) {
		BlockState blockState = getCachedState();
		if (blockState == null)
			return false;
		Direction direction = blockState.get(CrushingWheelControllerBlock.FACING);
		return direction == Direction.DOWN || direction == side;
	}

	@Override
	public void tick() {
		super.tick();
		if (searchForEntity) {
			searchForEntity = false;
			List<Entity> search = world.getOtherEntities((Entity) null, new Box(getPos()),
				e -> entityUUID.equals(e.getUuid()));
			if (search.isEmpty())
				clear();
			else
				processingEntity = search.get(0);
		}

		if (!isOccupied())
			return;
		if (crushingspeed == 0)
			return;

		if (world.isClient)
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> this.tickAudio());

		float speed = crushingspeed * 4;

		Vec3d centerPos = VecHelper.getCenterOf(pos);
		Direction facing = getCachedState().get(CrushingWheelControllerBlock.FACING);
		int offset = facing.getDirection()
			.offset();
		Vec3d outSpeed = new Vec3d((facing.getAxis() == Axis.X ? 0.25D : 0.0D) * offset,
			offset == 1 ? (facing.getAxis() == Axis.Y ? 0.5D : 0.0D) : 0.0D // Increased upwards speed so upwards
																			// crushing wheels shoot out the item
																			// properly.
			, (facing.getAxis() == Axis.Z ? 0.25D : 0.0D) * offset); // No downwards speed, so downwards crushing wheels
																		// drop the items as before.
		Vec3d outPos = centerPos.add((facing.getAxis() == Axis.X ? .55f * offset : 0f),
			(facing.getAxis() == Axis.Y ? .55f * offset : 0f), (facing.getAxis() == Axis.Z ? .55f * offset : 0f));

		if (!hasEntity()) {

			float processingSpeed =
				MathHelper.clamp((speed) / (!inventory.appliedRecipe ? MathHelper.floorLog2(inventory.getStackInSlot(0)
					.getCount()) : 1), .25f, 20);
			inventory.remainingTime -= processingSpeed;
			spawnParticles(inventory.getStackInSlot(0));

			if (world.isClient)
				return;

			if (inventory.remainingTime < 20 && !inventory.appliedRecipe) {
				applyRecipe();
				inventory.appliedRecipe = true;
				world.updateListeners(pos, getCachedState(), getCachedState(), 2 | 16);
				return;
			}

			if (inventory.remainingTime > 0) {
				return;
			}
			inventory.remainingTime = 0;

			// Output Items
			if (facing != Direction.UP) {
				BlockPos nextPos = pos.add(facing.getAxis() == Axis.X ? 1 * offset : 0, -1,
					facing.getAxis() == Axis.Z ? 1 * offset : 0);
				DirectBeltInputBehaviour behaviour =
					BlockEntityBehaviour.get(world, nextPos, DirectBeltInputBehaviour.TYPE);
				if (behaviour != null) {
					boolean changed = false;
					if (!behaviour.canInsertFromSide(facing))
						return;
					for (int slot = 0; slot < inventory.getSlotCount(); slot++) {
						ItemStack stack = inventory.getStackInSlot(slot);
						if (stack.isEmpty())
							continue;
						ItemStack remainder = behaviour.handleInsertion(stack, facing, false);
						if (ItemStack.areEqual(stack, remainder))
							continue;
						inventory.setStackInSlot(slot, remainder);
						changed = true;
					}
					if (changed) {
						markDirty();
						sendData();
					}
					return;
				}
			}

			// Eject Items
			for (int slot = 0; slot < inventory.getSlotCount(); slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				if (stack.isEmpty())
					continue;
				ItemEntity entityIn = new ItemEntity(world, outPos.x, outPos.y, outPos.z, stack);
				entityIn.setVelocity(outSpeed);
				entityIn.getCustomData()
					.put("BypassCrushingWheel", NbtHelper.fromBlockPos(pos));
				world.spawnEntity(entityIn);
			}
			inventory.clear();
			world.updateListeners(pos, getCachedState(), getCachedState(), 2 | 16);

			return;
		}

		if (!processingEntity.isAlive() || !processingEntity.getBoundingBox()
			.intersects(new Box(pos).expand(.5f))) {
			clear();
			return;
		}

		double xMotion = ((pos.getX() + .5f) - processingEntity.getX()) / 2f;
		double zMotion = ((pos.getZ() + .5f) - processingEntity.getZ()) / 2f;
		if (processingEntity.isSneaking())
			xMotion = zMotion = 0;
		double movement = Math.max(-speed / 4f, -.5f) * -offset;
		processingEntity.setVelocity(
			new Vec3d(facing.getAxis() == Axis.X ? movement : xMotion, facing.getAxis() == Axis.Y ? movement : 0f // Do
																														// not
																														// move
																														// entities
																														// upwards
																														// or
																														// downwards
																														// for
																														// horizontal
																														// crushers,
				, facing.getAxis() == Axis.Z ? movement : zMotion)); // Or they'll only get their feet crushed.

		if (world.isClient)
			return;

		if (!(processingEntity instanceof ItemEntity)) {
			Vec3d entityOutPos = outPos.add(facing.getAxis() == Axis.X ? .5f * offset : 0f,
				facing.getAxis() == Axis.Y ? .5f * offset : 0f, facing.getAxis() == Axis.Z ? .5f * offset : 0f);
			int crusherDamage = AllConfigs.server().kinetics.crushingDamage.get();

			if (processingEntity instanceof LivingEntity) {
				if ((((LivingEntity) processingEntity).getHealth() - crusherDamage <= 0) // Takes LivingEntity instances
																							// as exception, so it can
																							// move them before it would
																							// kill them.
					&& (((LivingEntity) processingEntity).hurtTime <= 0)) { // This way it can actually output the items
																			// to the right spot.
					processingEntity.setPosition(entityOutPos.x, entityOutPos.y, entityOutPos.z);
				}
			}
			processingEntity.damage(CreateDamageSources.crush(world), crusherDamage);
			if (!processingEntity.isAlive()) {
				processingEntity.setPosition(entityOutPos.x, entityOutPos.y, entityOutPos.z);
			}
			return;
		}

		ItemEntity itemEntity = (ItemEntity) processingEntity;
		itemEntity.setPickupDelay(20);
		if (facing.getAxis() == Axis.Y) {
			if (processingEntity.getY() * -offset < (centerPos.y - .25f) * -offset) {
				intakeItem(itemEntity);
			}
		} else if (facing.getAxis() == Axis.Z) {
			if (processingEntity.getZ() * -offset < (centerPos.z - .25f) * -offset) {
				intakeItem(itemEntity);
			}
		} else {
			if (processingEntity.getX() * -offset < (centerPos.x - .25f) * -offset) {
				intakeItem(itemEntity);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public void tickAudio() {
		float pitch = MathHelper.clamp((crushingspeed / 256f) + .45f, .85f, 1f);
		if (entityUUID == null && inventory.getStackInSlot(0)
			.isEmpty())
			return;
		SoundScapes.play(AmbienceGroup.CRUSHING, pos, pitch);
	}

	private void intakeItem(ItemEntity itemEntity) {
		inventory.clear();
		inventory.setStackInSlot(0, itemEntity.getStack()
			.copy());
		itemInserted(inventory.getStackInSlot(0));
		itemEntity.discard();
		world.updateListeners(pos, getCachedState(), getCachedState(), 2 | 16);
	}

	protected void spawnParticles(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return;

		ParticleEffect particleData = null;
		if (stack.getItem() instanceof BlockItem)
			particleData = new BlockStateParticleEffect(ParticleTypes.BLOCK, ((BlockItem) stack.getItem()).getBlock()
				.getDefaultState());
		else
			particleData = new ItemStackParticleEffect(ParticleTypes.ITEM, stack);

		Random r = world.random;
		for (int i = 0; i < 4; i++)
			world.addParticle(particleData, pos.getX() + r.nextFloat(), pos.getY() + r.nextFloat(),
				pos.getZ() + r.nextFloat(), 0, 0, 0);
	}

	private void applyRecipe() {
		Optional<ProcessingRecipe<Inventory>> recipe = findRecipe();

		List<ItemStack> list = new ArrayList<>();
		if (recipe.isPresent()) {
			int rolls = inventory.getStackInSlot(0)
				.getCount();
			inventory.clear();
			for (int roll = 0; roll < rolls; roll++) {
				List<ItemStack> rolledResults = recipe.get()
					.rollResults();
				for (int i = 0; i < rolledResults.size(); i++) {
					ItemStack stack = rolledResults.get(i);
					ItemHelper.addToList(stack, list);
				}
			}
			for (int slot = 0; slot < list.size() && slot + 1 < inventory.getSlotCount(); slot++)
				inventory.setStackInSlot(slot + 1, list.get(slot));
		} else {
			inventory.clear();
		}

	}

	public Optional<ProcessingRecipe<Inventory>> findRecipe() {
		Optional<ProcessingRecipe<Inventory>> crushingRecipe = AllRecipeTypes.CRUSHING.find(inventory, world);
		if (!crushingRecipe.isPresent())
			crushingRecipe = AllRecipeTypes.MILLING.find(inventory, world);
		return crushingRecipe;
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		if (hasEntity())
			compound.put("Entity", NbtHelper.fromUuid(entityUUID));
		compound.put("Inventory", NBTSerializer.serializeNBT(inventory));
		compound.putFloat("Speed", crushingspeed);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		if (compound.contains("Entity") && !isOccupied()) {
			entityUUID = NbtHelper.toUuid(NBTHelper.getINBT(compound, "Entity"));
			this.searchForEntity = true;
		}
		crushingspeed = compound.getFloat("Speed");
		inventory.deserializeNBT(compound.getCompound("Inventory"));
	}

	public void startCrushing(Entity entity) {
		processingEntity = entity;
		entityUUID = entity.getUuid();
	}

	private void itemInserted(ItemStack stack) {
		Optional<ProcessingRecipe<Inventory>> recipe = findRecipe();
		inventory.remainingTime = recipe.isPresent() ? recipe.get()
			.getProcessingDuration() : 100;
		inventory.appliedRecipe = false;
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		return inventory;
	}

	public void clear() {
		processingEntity = null;
		entityUUID = null;
	}

	public boolean isOccupied() {
		return hasEntity() || !inventory.isEmpty();
	}

	public boolean hasEntity() {
		return processingEntity != null;
	}

}
