package com.simibubi.create.content.redstone.link.controller;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import com.simibubi.create.foundation.utility.fabric.ReachUtil;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LecternControllerBlockEntity extends SmartBlockEntity {

	private ItemStack controller;
	private UUID user;
	private UUID prevUser;	// used only on client
	private boolean deactivatedThisTick;	// used only on server

	public LecternControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) { }

	@Override
	protected void write(NbtCompound compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.put("Controller", controller.writeNbt(new NbtCompound()));
		if (user != null)
			compound.putUuid("User", user);
	}

	@Override
	public void writeSafe(NbtCompound compound) {
		super.writeSafe(compound);
		compound.put("Controller", controller.writeNbt(new NbtCompound()));
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		controller = ItemStack.fromNbt(compound.getCompound("Controller"));
		user = compound.containsUuid("User") ? compound.getUuid("User") : null;
	}

	public ItemStack getController() {
		return controller;
	}

	public boolean hasUser() { return user != null; }

	public boolean isUsedBy(PlayerEntity player) {
		return hasUser() && user.equals(player.getUuid());
	}

	public void tryStartUsing(PlayerEntity player) {
		if (!deactivatedThisTick && !hasUser() && !playerIsUsingLectern(player) && playerInRange(player, world, pos))
			startUsing(player);
	}

	public void tryStopUsing(PlayerEntity player) {
		if (isUsedBy(player))
			stopUsing(player);
	}

	private void startUsing(PlayerEntity player) {
		user = player.getUuid();
		player.getCustomData().putBoolean("IsUsingLecternController", true);
		sendData();
	}

	private void stopUsing(PlayerEntity player) {
		user = null;
		if (player != null)
			player.getCustomData().remove("IsUsingLecternController");
		deactivatedThisTick = true;
		sendData();
	}

	public static boolean playerIsUsingLectern(PlayerEntity player) {
		return player.getCustomData().contains("IsUsingLecternController");
	}

	@Override
	public void tick() {
		super.tick();

		if (world.isClient) {
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::tryToggleActive);
			prevUser = user;
		}

		if (!world.isClient) {
			deactivatedThisTick = false;

			if (!(world instanceof ServerWorld))
				return;
			if (user == null)
				return;

			Entity entity = ((ServerWorld) world).getEntity(user);
			if (!(entity instanceof PlayerEntity)) {
				stopUsing(null);
				return;
			}

			PlayerEntity player = (PlayerEntity) entity;
			if (!playerInRange(player, world, pos) || !playerIsUsingLectern(player))
				stopUsing(player);
		}
	}

	@Environment(EnvType.CLIENT)
	private void tryToggleActive() {
		if (user == null && MinecraftClient.getInstance().player.getUuid().equals(prevUser)) {
			LinkedControllerClientHandler.deactivateInLectern();
		} else if (prevUser == null && MinecraftClient.getInstance().player.getUuid().equals(user)) {
			LinkedControllerClientHandler.activateInLectern(pos);
		}
	}

	public void setController(ItemStack newController) {
		controller = newController;
		if (newController != null) {
			AllSoundEvents.CONTROLLER_PUT.playOnServer(world, pos);
		}
	}

	public void swapControllers(ItemStack stack, PlayerEntity player, Hand hand, BlockState state) {
		ItemStack newController = stack.copy();
		stack.setCount(0);
		if (player.getStackInHand(hand).isEmpty()) {
			player.setStackInHand(hand, controller);
		} else {
			dropController(state);
		}
		setController(newController);
	}

	public void dropController(BlockState state) {
		Entity playerEntity = ((ServerWorld) world).getEntity(user);
		if (playerEntity instanceof PlayerEntity)
			stopUsing((PlayerEntity) playerEntity);

		Direction dir = state.get(LecternControllerBlock.FACING);
		double x = pos.getX() + 0.5 + 0.25*dir.getOffsetX();
		double y = pos.getY() + 1;
		double z = pos.getZ() + 0.5 + 0.25*dir.getOffsetZ();
		ItemEntity itementity = new ItemEntity(world, x, y, z, controller.copy());
		itementity.setToDefaultPickupDelay();
		world.spawnEntity(itementity);
		controller = null;
	}

	public static boolean playerInRange(PlayerEntity player, World world, BlockPos pos) {
		//double modifier = world.isRemote ? 0 : 1.0;
		double reach = 0.4* ReachUtil.reach(player);// + modifier;
		return player.squaredDistanceTo(Vec3d.ofCenter(pos)) < reach*reach;
	}

}
