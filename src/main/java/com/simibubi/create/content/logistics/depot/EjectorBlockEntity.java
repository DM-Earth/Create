package com.simibubi.create.content.logistics.depot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LongAttached;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

public class EjectorBlockEntity extends KineticBlockEntity implements SidedStorageBlockEntity {

	List<LongAttached<ItemStack>> launchedItems;
	ScrollValueBehaviour maxStackSize;
	DepotBehaviour depotBehaviour;
	EntityLauncher launcher;
	LerpedFloat lidProgress;
	boolean powered;
	boolean launch;
	State state;

	// item collision
	@Nullable
	Pair<Vec3d, BlockPos> earlyTarget;
	float earlyTargetTime;
	// runtime stuff
	int scanCooldown;
	ItemStack trackedItem;

	public enum State {
		CHARGED, LAUNCHING, RETRACTING;
	}

	public EjectorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
		launcher = new EntityLauncher(1, 0);
		lidProgress = LerpedFloat.linear()
			.startWithValue(1);
		this.state = State.RETRACTING;
		launchedItems = new ArrayList<>();
		powered = false;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.add(depotBehaviour = new DepotBehaviour(this));

		maxStackSize =
			new ScrollValueBehaviour(Lang.translateDirect("weighted_ejector.stack_size"), this, new EjectorSlot())
				.between(0, 64)
				.withFormatter(i -> i == 0 ? "*" : String.valueOf(i));
		behaviours.add(maxStackSize);

		depotBehaviour.maxStackSize = () -> maxStackSize.getValue();
		depotBehaviour.canAcceptItems = () -> state == State.CHARGED;
		depotBehaviour.canFunnelsPullFrom = side -> side != getFacing();
		depotBehaviour.enableMerging();
		depotBehaviour.addSubBehaviours(behaviours);
	}

	@Override
	public void initialize() {
		super.initialize();
		updateSignal();
	}

	public void activate() {
		launch = true;
		nudgeEntities();
	}

	protected boolean cannotLaunch() {
		return state != State.CHARGED && !(world.isClient && state == State.LAUNCHING);
	}

	public void activateDeferred() {
		if (cannotLaunch())
			return;
		Direction facing = getFacing();
		List<Entity> entities =
			world.getNonSpectatingEntities(Entity.class, new Box(pos).expand(-1 / 16f, 0, -1 / 16f));

		// Launch Items
		boolean doLogic = !world.isClient || isVirtual();
		if (doLogic)
			launchItems();

		// Launch Entities
		for (Entity entity : entities) {
			boolean isPlayerEntity = entity instanceof PlayerEntity;
			if (!entity.isAlive())
				continue;
			if (entity instanceof ItemEntity)
				continue;
			if (entity.getPistonBehavior() == PistonBehavior.IGNORE)
				continue;

			entity.setOnGround(false);

			if (isPlayerEntity != world.isClient)
				continue;

			entity.setPosition(pos.getX() + .5f, pos.getY() + 1, pos.getZ() + .5f);
			launcher.applyMotion(entity, facing);

			if (!isPlayerEntity)
				continue;

			PlayerEntity playerEntity = (PlayerEntity) entity;

			if (launcher.getHorizontalDistance() * launcher.getHorizontalDistance()
				+ launcher.getVerticalDistance() * launcher.getVerticalDistance() >= 25 * 25)
				AllPackets.getChannel()
					.sendToServer(new EjectorAwardPacket(pos));

			if (!(playerEntity.getEquippedStack(EquipmentSlot.CHEST)
				.getItem() instanceof ElytraItem))
				continue;

			playerEntity.setPitch(-35);
			playerEntity.setYaw(facing.asRotation());
			playerEntity.setVelocity(playerEntity.getVelocity()
				.multiply(.75f));
			deployElytra(playerEntity);
			AllPackets.getChannel()
				.sendToServer(new EjectorElytraPacket(pos));
		}

		if (doLogic) {
			lidProgress.chase(1, .8f, Chaser.EXP);
			state = State.LAUNCHING;
			if (!world.isClient) {
				world.playSound(null, pos, SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, .35f, 1f);
				world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, .1f, 1.4f);
			}
		}
	}

	public void deployElytra(PlayerEntity playerEntity) {
		EntityHack.setElytraFlying(playerEntity);
	}

	protected void launchItems() {
		ItemStack heldItemStack = depotBehaviour.getHeldItemStack();
		Direction funnelFacing = getFacing().getOpposite();

		if (AbstractFunnelBlock.getFunnelFacing(world.getBlockState(pos.up())) == funnelFacing) {
			DirectBeltInputBehaviour directOutput = getBehaviour(DirectBeltInputBehaviour.TYPE);

			if (depotBehaviour.heldItem != null) {
				ItemStack remainder = directOutput.tryExportingToBeltFunnel(heldItemStack, funnelFacing, false);
				if (remainder == null)
					;
				else if (remainder.isEmpty())
					depotBehaviour.removeHeldItem();
				else if (remainder.getCount() != heldItemStack.getCount())
					depotBehaviour.heldItem.stack = remainder;
			}

			for (Iterator<TransportedItemStack> iterator = depotBehaviour.incoming.iterator(); iterator.hasNext();) {
				TransportedItemStack transportedItemStack = iterator.next();
				ItemStack stack = transportedItemStack.stack;
				ItemStack remainder = directOutput.tryExportingToBeltFunnel(stack, funnelFacing, false);
				if (remainder == null)
					;
				else if (remainder.isEmpty())
					iterator.remove();
				else if (!ItemStack.areItemsEqual(remainder, stack))
					transportedItemStack.stack = remainder;
			}

			ItemStackHandler outputs = depotBehaviour.processingOutputBuffer;
			for (int i = 0; i < outputs.getSlotCount(); i++) {
				ItemStack stack = outputs.getStackInSlot(i);
				if (stack.isEmpty())
					continue;
				ItemStack remainder = directOutput.tryExportingToBeltFunnel(stack, funnelFacing, false);
				if (remainder != null)
					outputs.setStackInSlot(i, remainder);
			}
			return;
		}

		if (!world.isClient)
			for (Direction d : Iterate.directions) {
				BlockState blockState = world.getBlockState(pos.offset(d));
				if (!(blockState.getBlock() instanceof ObserverBlock))
					continue;
				if (blockState.get(ObserverBlock.FACING) != d.getOpposite())
					continue;
				blockState.getStateForNeighborUpdate(d.getOpposite(), blockState, world, pos.offset(d), pos);
			}

		if (depotBehaviour.heldItem != null) {
			addToLaunchedItems(heldItemStack);
			depotBehaviour.removeHeldItem();
		}

		for (TransportedItemStack transportedItemStack : depotBehaviour.incoming)
			addToLaunchedItems(transportedItemStack.stack);
		depotBehaviour.incoming.clear();

		ItemStackHandler outputs = depotBehaviour.processingOutputBuffer;
		try (Transaction t = TransferUtil.getTransaction()) {
			for (StorageView<ItemVariant> view : outputs.nonEmptyViews()) {
				ItemVariant var = view.getResource();
				long extracted = view.extract(view.getResource(), 64, t);
				if (extracted != 0)
					addToLaunchedItems(var.toStack(ItemHelper.truncateLong(extracted)));
			}
			t.commit();
		}
	}

	protected boolean addToLaunchedItems(ItemStack stack) {
		if ((!world.isClient || isVirtual()) && trackedItem == null && scanCooldown == 0) {
			scanCooldown = AllConfigs.server().kinetics.ejectorScanInterval.get();
			trackedItem = stack;
		}
		return launchedItems.add(LongAttached.withZero(stack));
	}

	protected Direction getFacing() {
		BlockState blockState = getCachedState();
		if (!AllBlocks.WEIGHTED_EJECTOR.has(blockState))
			return Direction.UP;
		Direction facing = blockState.get(EjectorBlock.HORIZONTAL_FACING);
		return facing;
	}

	@Override
	public void tick() {
		super.tick();

		boolean doLogic = !world.isClient || isVirtual();
		State prevState = state;
		float totalTime = Math.max(3, (float) launcher.getTotalFlyingTicks());

		if (scanCooldown > 0)
			scanCooldown--;

		if (launch) {
			launch = false;
			activateDeferred();
		}

		for (Iterator<LongAttached<ItemStack>> iterator = launchedItems.iterator(); iterator.hasNext();) {
			LongAttached<ItemStack> LongAttached = iterator.next();
			boolean hit = false;
			if (LongAttached.getSecond() == trackedItem)
				hit = scanTrajectoryForObstacles(LongAttached.getFirst());
			float maxTime = earlyTarget != null ? Math.min(earlyTargetTime, totalTime) : totalTime;
			if (hit || LongAttached.exceeds((int) maxTime)) {
				placeItemAtTarget(doLogic, maxTime, LongAttached);
				iterator.remove();
			}
			LongAttached.increment();
		}

		if (state == State.LAUNCHING) {
			lidProgress.chase(1, .8f, Chaser.EXP);
			lidProgress.tickChaser();
			if (lidProgress.getValue() > 1 - 1 / 16f && doLogic) {
				state = State.RETRACTING;
				lidProgress.setValue(1);
			}
		}

		if (state == State.CHARGED) {
			lidProgress.setValue(0);
			lidProgress.updateChaseSpeed(0);
			if (doLogic)
				ejectIfTriggered();
		}

		if (state == State.RETRACTING) {
			if (lidProgress.getChaseTarget() == 1 && !lidProgress.settled()) {
				lidProgress.tickChaser();
			} else {
				lidProgress.updateChaseTarget(0);
				lidProgress.updateChaseSpeed(0);
				if (lidProgress.getValue() == 0 && doLogic) {
					state = State.CHARGED;
					lidProgress.setValue(0);
					sendData();
				}

				float value = MathHelper.clamp(lidProgress.getValue() - getWindUpSpeed(), 0, 1);
				lidProgress.setValue(value);

				int soundRate = (int) (1 / (getWindUpSpeed() * 5)) + 1;
				float volume = .125f;
				float pitch = 1.5f - lidProgress.getValue();
				if (((int) world.getTime()) % soundRate == 0 && doLogic)
					world.playSound(null, pos, SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_OFF, SoundCategory.BLOCKS,
						volume, pitch);
			}
		}

		if (state != prevState)
			notifyUpdate();
	}

	private boolean scanTrajectoryForObstacles(long time) {
		if (time <= 2)
			return false;

		Vec3d source = getLaunchedItemLocation(time);
		Vec3d target = getLaunchedItemLocation(time + 1);

		BlockHitResult rayTraceBlocks = world.raycast(new RaycastContext(source, target, ShapeType.COLLIDER, FluidHandling.NONE, null));
		boolean miss = rayTraceBlocks.getType() == Type.MISS;

		if (!miss && rayTraceBlocks.getType() == Type.BLOCK) {
			BlockState blockState = world.getBlockState(rayTraceBlocks.getBlockPos());
			if (FunnelBlock.isFunnel(blockState) && blockState.contains(FunnelBlock.EXTRACTING)
				&& blockState.get(FunnelBlock.EXTRACTING))
				miss = true;
		}

		if (miss) {
			if (earlyTarget != null && earlyTargetTime < time + 1) {
				earlyTarget = null;
				earlyTargetTime = 0;
			}
			return false;
		}

		Vec3d vec = rayTraceBlocks.getPos();
		earlyTarget = Pair.of(vec.add(Vec3d.of(rayTraceBlocks.getSide()
			.getVector())
			.multiply(.25f)), rayTraceBlocks.getBlockPos());
		earlyTargetTime = (float) (time + (source.distanceTo(vec) / source.distanceTo(target)));
		sendData();
		return true;
	}

	protected void nudgeEntities() {
		for (Entity entity : world.getNonSpectatingEntities(Entity.class,
			new Box(pos).expand(-1 / 16f, 0, -1 / 16f))) {
			if (!entity.isAlive())
				continue;
			if (entity.getPistonBehavior() == PistonBehavior.IGNORE)
				continue;
			if (!(entity instanceof PlayerEntity))
				entity.setPosition(entity.getX(), entity.getY() + .125f, entity.getZ());
		}
	}

	protected void ejectIfTriggered() {
		if (powered)
			return;
		int presentStackSize = depotBehaviour.getPresentStackSize();
		if (presentStackSize == 0)
			return;
		if (presentStackSize < maxStackSize.getValue())
			return;
		if (depotBehaviour.heldItem != null && depotBehaviour.heldItem.beltPosition < .49f)
			return;

		Direction funnelFacing = getFacing().getOpposite();
		ItemStack held = depotBehaviour.getHeldItemStack();
		if (AbstractFunnelBlock.getFunnelFacing(world.getBlockState(pos.up())) == funnelFacing) {
			DirectBeltInputBehaviour directOutput = getBehaviour(DirectBeltInputBehaviour.TYPE);
			if (depotBehaviour.heldItem != null) {
				ItemStack tryFunnel = directOutput.tryExportingToBeltFunnel(held, funnelFacing, true);
				if (tryFunnel == null || !tryFunnel.isEmpty())
					return;
			}
		}

		DirectBeltInputBehaviour targetOpenInv = getTargetOpenInv();

		// Do not eject if target cannot accept held item
		if (targetOpenInv != null && depotBehaviour.heldItem != null
			&& targetOpenInv.handleInsertion(held, Direction.UP, true)
				.getCount() == held.getCount())
			return;

		activate();
		notifyUpdate();
	}

	protected void placeItemAtTarget(boolean doLogic, float maxTime, LongAttached<ItemStack> LongAttached) {
		if (!doLogic)
			return;
		if (LongAttached.getSecond() == trackedItem)
			trackedItem = null;

		DirectBeltInputBehaviour targetOpenInv = getTargetOpenInv();
		if (targetOpenInv != null) {
			ItemStack remainder = targetOpenInv.handleInsertion(LongAttached.getValue(), Direction.UP, false);
			LongAttached.setSecond(remainder);
		}

		if (LongAttached.getValue()
			.isEmpty())
			return;

		Vec3d ejectVec = earlyTarget != null ? earlyTarget.getFirst() : getLaunchedItemLocation(maxTime);
		Vec3d ejectMotionVec = getLaunchedItemMotion(maxTime);
		ItemEntity item = new ItemEntity(world, ejectVec.x, ejectVec.y, ejectVec.z, LongAttached.getValue());
		item.setVelocity(ejectMotionVec);
		item.setToDefaultPickupDelay();
		world.spawnEntity(item);
	}

	public DirectBeltInputBehaviour getTargetOpenInv() {
		BlockPos targetPos = earlyTarget != null ? earlyTarget.getSecond()
			: pos.up(launcher.getVerticalDistance())
				.offset(getFacing(), Math.max(1, launcher.getHorizontalDistance()));
		return BlockEntityBehaviour.get(world, targetPos, DirectBeltInputBehaviour.TYPE);
	}

	public Vec3d getLaunchedItemLocation(float time) {
		return launcher.getGlobalPos(time, getFacing().getOpposite(), pos);
	}

	public Vec3d getLaunchedItemMotion(float time) {
		return launcher.getGlobalVelocity(time, getFacing().getOpposite(), pos)
			.multiply(.5f);
	}

	@Override
	public void destroy() {
		super.destroy();
		dropFlyingItems();
	}

	public void dropFlyingItems() {
		for (LongAttached<ItemStack> LongAttached : launchedItems) {
			Vec3d ejectVec = getLaunchedItemLocation(LongAttached.getFirst());
			Vec3d ejectMotionVec = getLaunchedItemMotion(LongAttached.getFirst());
			ItemEntity item = new ItemEntity(world, 0, 0, 0, LongAttached.getValue());
			item.setPos(ejectVec.x, ejectVec.y, ejectVec.z);
			item.setVelocity(ejectMotionVec);
			item.setToDefaultPickupDelay();
			world.spawnEntity(item);
		}
		launchedItems.clear();
	}

	public float getWindUpSpeed() {
		int hd = launcher.getHorizontalDistance();
		int vd = launcher.getVerticalDistance();

		float speedFactor = Math.abs(getSpeed()) / 256f;
		float distanceFactor;
		if (hd == 0 && vd == 0)
			distanceFactor = 1;
		else
			distanceFactor = 1 * MathHelper.sqrt(hd * hd + vd * vd);
		return speedFactor / distanceFactor;
	}

	@Override
	protected void write(NbtCompound compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.putInt("HorizontalDistance", launcher.getHorizontalDistance());
		compound.putInt("VerticalDistance", launcher.getVerticalDistance());
		compound.putBoolean("Powered", powered);
		NBTHelper.writeEnum(compound, "State", state);
		compound.put("Lid", lidProgress.writeNBT());
		compound.put("LaunchedItems",
			NBTHelper.writeCompoundList(launchedItems, ia -> ia.serializeNBT(NBTSerializer::serializeNBTCompound)));

		if (earlyTarget != null) {
			compound.put("EarlyTarget", VecHelper.writeNBT(earlyTarget.getFirst()));
			compound.put("EarlyTargetPos", NbtHelper.fromBlockPos(earlyTarget.getSecond()));
			compound.putFloat("EarlyTargetTime", earlyTargetTime);
		}
	}

	@Override
	public void writeSafe(NbtCompound compound) {
		super.writeSafe(compound);
		compound.putInt("HorizontalDistance", launcher.getHorizontalDistance());
		compound.putInt("VerticalDistance", launcher.getVerticalDistance());
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		int horizontalDistance = compound.getInt("HorizontalDistance");
		int verticalDistance = compound.getInt("VerticalDistance");

		if (launcher.getHorizontalDistance() != horizontalDistance
			|| launcher.getVerticalDistance() != verticalDistance) {
			launcher.set(horizontalDistance, verticalDistance);
			launcher.clamp(AllConfigs.server().kinetics.maxEjectorDistance.get());
		}

		powered = compound.getBoolean("Powered");
		state = NBTHelper.readEnum(compound, "State", State.class);
		lidProgress.readNBT(compound.getCompound("Lid"), false);
		launchedItems = NBTHelper.readCompoundList(compound.getList("LaunchedItems", NbtElement.COMPOUND_TYPE),
			nbt -> LongAttached.read(nbt, ItemStack::fromNbt));

		earlyTarget = null;
		earlyTargetTime = 0;
		if (compound.contains("EarlyTarget")) {
			earlyTarget = Pair.of(VecHelper.readNBT(compound.getList("EarlyTarget", NbtElement.DOUBLE_TYPE)),
				NbtHelper.toBlockPos(compound.getCompound("EarlyTargetPos")));
			earlyTargetTime = compound.getFloat("EarlyTargetTime");
		}

		if (compound.contains("ForceAngle"))
			lidProgress.startWithValue(compound.getFloat("ForceAngle"));
	}

	public void updateSignal() {
		boolean shoudPower = world.isReceivingRedstonePower(pos);
		if (shoudPower == powered)
			return;
		powered = shoudPower;
		sendData();
	}

	public void setTarget(int horizontalDistance, int verticalDistance) {
		launcher.set(Math.max(1, horizontalDistance), verticalDistance);
		sendData();
	}

	public BlockPos getTargetPosition() {
		BlockState blockState = getCachedState();
		if (!AllBlocks.WEIGHTED_EJECTOR.has(blockState))
			return pos;
		Direction facing = blockState.get(EjectorBlock.HORIZONTAL_FACING);
		return pos.offset(facing, launcher.getHorizontalDistance())
			.up(launcher.getVerticalDistance());
	}

	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		return depotBehaviour.itemHandler;
	}

	public float getLidProgress(float pt) {
		return lidProgress.getValue(pt);
	}

	public State getState() {
		return state;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Box getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	private static abstract class EntityHack extends Entity {

		public EntityHack(EntityType<?> p_i48580_1_, World p_i48580_2_) {
			super(p_i48580_1_, p_i48580_2_);
		}

		public static void setElytraFlying(Entity e) {
			DataTracker data = e.getDataTracker();
			data.set(FLAGS, (byte) (data.get(FLAGS) | 1 << 7));
		}

	}

	private class EjectorSlot extends ValueBoxTransform.Sided {

		@Override
		public Vec3d getLocalOffset(BlockState state) {
			if (direction != Direction.UP)
				return super.getLocalOffset(state);
			return new Vec3d(.5, 10.5 / 16f, .5).add(VecHelper.rotate(VecHelper.voxelSpace(0, 0, -5), angle(state), Axis.Y));
		}

		@Override
		public void rotate(BlockState state, MatrixStack ms) {
			if (direction != Direction.UP) {
				super.rotate(state, ms);
				return;
			}
			TransformStack.cast(ms)
				.rotateY(angle(state))
				.rotateX(90);
		}

		protected float angle(BlockState state) {
			float horizontalAngle = AllBlocks.WEIGHTED_EJECTOR.has(state)
				? AngleHelper.horizontalAngle(state.get(EjectorBlock.HORIZONTAL_FACING))
				: 0;
			return horizontalAngle;
		}

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			return direction.getAxis() == state.get(EjectorBlock.HORIZONTAL_FACING)
				.getAxis()
				|| direction == Direction.UP && EjectorBlockEntity.this.state != EjectorBlockEntity.State.CHARGED;
		}

		@Override
		protected Vec3d getSouthLocation() {
			return direction == Direction.UP ? Vec3d.ZERO : VecHelper.voxelSpace(8, 6, 15.5);
		}

	}

}
