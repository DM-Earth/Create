package com.simibubi.create.content.logistics.chute;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import com.simibubi.create.foundation.particle.AirParticleData;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.block.CustomRenderBoundingBoxBlockEntity;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.ItemStackUtil;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import io.github.fabricators_of_create.porting_lib.util.StorageProvider;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
/*
 * Commented Code: Chutes create air streams and act similarly to encased fans
 * (Unfinished)
 */
public class ChuteBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, CustomRenderBoundingBoxBlockEntity, SidedStorageBlockEntity { // , IAirCurrentSource {

	// public AirCurrent airCurrent;

	float pull;
	float push;

	ItemStack item;
	LerpedFloat itemPosition;
	ChuteItemHandler itemHandler;
	boolean canPickUpItems;

	float bottomPullDistance;
	float beltBelowOffset;
	TransportedItemStackHandlerBehaviour beltBelow;
	boolean updateAirFlow;
	int airCurrentUpdateCooldown;
	int entitySearchCooldown;

	VersionedInventoryTrackerBehaviour invVersionTracker;

	StorageProvider<ItemVariant> capAbove;
	StorageProvider<ItemVariant> capBelow;

	public ChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		item = ItemStack.EMPTY;
		itemPosition = LerpedFloat.linear();
		itemHandler = new ChuteItemHandler(this);
		canPickUpItems = false;

		bottomPullDistance = 0;
		// airCurrent = new AirCurrent(this);
		updateAirFlow = true;
	}

	@Override
	public void setWorld(World level) {
		super.setWorld(level);
		capAbove = StorageProvider.createForItems(level, pos.up());
		capBelow = StorageProvider.createForItems(level, pos.down());
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen((d) -> canDirectlyInsertCached()));
		behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
		registerAwardables(behaviours, AllAdvancements.CHUTE);
	}

	// Cached per-tick, useful when a lot of items are waiting on top of it
	public boolean canDirectlyInsertCached() {
		return canPickUpItems;
	}

	private boolean canDirectlyInsert() {
		BlockState blockState = getCachedState();
		BlockState blockStateAbove = world.getBlockState(pos.up());
		if (!AbstractChuteBlock.isChute(blockState))
			return false;
		if (AbstractChuteBlock.getChuteFacing(blockStateAbove) == Direction.DOWN)
			return false;
		if (getItemMotion() > 0 && getInputChutes().isEmpty())
			return false;
		return AbstractChuteBlock.isOpenChute(blockState);
	}

	@Override
	public void initialize() {
		super.initialize();
		onAdded();
	}

	@Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).stretch(0, -3, 0);
	}

	@Override
	public void tick() {
		super.tick();

		if (!world.isClient)
			canPickUpItems = canDirectlyInsert();

		boolean clientSide = world != null && world.isClient && !isVirtual();
		float itemMotion = getItemMotion();
		if (itemMotion != 0 && world != null && world.isClient)
			spawnParticles(itemMotion);
		tickAirStreams(itemMotion);

		if (item.isEmpty() && !clientSide) {
			if (itemMotion < 0)
				handleInputFromAbove();
			if (itemMotion > 0)
				handleInputFromBelow();
			return;
		}

		float nextOffset = itemPosition.getValue() + itemMotion;

		if (itemMotion < 0) {
			if (nextOffset < .5f) {
				if (!handleDownwardOutput(true))
					nextOffset = .5f;
				else if (nextOffset < 0) {
					handleDownwardOutput(clientSide);
					nextOffset = itemPosition.getValue();
				}
			}
		} else if (itemMotion > 0) {
			if (nextOffset > .5f) {
				if (!handleUpwardOutput(true))
					nextOffset = .5f;
				else if (nextOffset > 1) {
					handleUpwardOutput(clientSide);
					nextOffset = itemPosition.getValue();
				}
			}
		}

		itemPosition.setValue(nextOffset);
	}

	private void updateAirFlow(float itemSpeed) {
		updateAirFlow = false;
		// airCurrent.rebuild();
		if (itemSpeed > 0 && world != null && !world.isClient) {
			float speed = pull - push;
			beltBelow = null;

			float maxPullDistance;
			if (speed >= 128)
				maxPullDistance = 3;
			else if (speed >= 64)
				maxPullDistance = 2;
			else if (speed >= 32)
				maxPullDistance = 1;
			else
				maxPullDistance = MathHelper.lerp(speed / 32, 0, 1);

			if (AbstractChuteBlock.isChute(world.getBlockState(pos.down())))
				maxPullDistance = 0;
			float flowLimit = maxPullDistance;
			if (flowLimit > 0)
				flowLimit = AirCurrent.getFlowLimit(world, pos, maxPullDistance, Direction.DOWN);

			for (int i = 1; i <= flowLimit + 1; i++) {
				TransportedItemStackHandlerBehaviour behaviour =
					BlockEntityBehaviour.get(world, pos.down(i), TransportedItemStackHandlerBehaviour.TYPE);
				if (behaviour == null)
					continue;
				beltBelow = behaviour;
				beltBelowOffset = i - 1;
				break;
			}
			this.bottomPullDistance = Math.max(0, flowLimit);
		}
		sendData();
	}

	private void findEntities(float itemSpeed) {
		// if (getSpeed() != 0)
		// airCurrent.findEntities();
		if (bottomPullDistance <= 0 && !getItem().isEmpty() || itemSpeed <= 0 || world == null || world.isClient)
			return;
		if (!canCollectItemsFromBelow())
			return;
		Vec3d center = VecHelper.getCenterOf(pos);
		Box searchArea = new Box(center.add(0, -bottomPullDistance - 0.5, 0), center.add(0, -0.5, 0)).expand(.45f);
		for (ItemEntity itemEntity : world.getNonSpectatingEntities(ItemEntity.class, searchArea)) {
			if (!itemEntity.isAlive())
				continue;
			ItemStack entityItem = itemEntity.getStack();
			if (!canAcceptItem(entityItem))
				continue;
			setItem(entityItem.copy(), (float) (itemEntity.getBoundingBox()
				.getCenter().y - pos.getY()));
			itemEntity.discard();
			break;
		}
	}

	private void extractFromBelt(float itemSpeed) {
		if (itemSpeed <= 0 || world == null || world.isClient)
			return;
		if (getItem().isEmpty() && beltBelow != null) {
			beltBelow.handleCenteredProcessingOnAllItems(.5f, ts -> {
				if (canAcceptItem(ts.stack)) {
					setItem(ts.stack.copy(), -beltBelowOffset);
					return TransportedResult.removeItem();
				}
				return TransportedResult.doNothing();
			});
		}
	}

	private void tickAirStreams(float itemSpeed) {
		if (!world.isClient && airCurrentUpdateCooldown-- <= 0) {
			airCurrentUpdateCooldown = AllConfigs.server().kinetics.fanBlockCheckRate.get();
			updateAirFlow = true;
		}

		if (updateAirFlow) {
			updateAirFlow(itemSpeed);
		}

		if (entitySearchCooldown-- <= 0 && item.isEmpty()) {
			entitySearchCooldown = 5;
			findEntities(itemSpeed);
		}

		extractFromBelt(itemSpeed);
		// if (getSpeed() != 0)
		// airCurrent.tick();
	}

	public void blockBelowChanged() {
		updateAirFlow = true;
	}

	private void spawnParticles(float itemMotion) {
		// todo: reduce the amount of particles
		if (world == null)
			return;
		BlockState blockState = getCachedState();
		boolean up = itemMotion > 0;
		float absMotion = up ? itemMotion : -itemMotion;
		if (blockState == null || !AbstractChuteBlock.isChute(blockState))
			return;
		if (push == 0 && pull == 0)
			return;

		if (up && AbstractChuteBlock.isOpenChute(blockState)
			&& BlockHelper.noCollisionInSpace(world, pos.up()))
			spawnAirFlow(1, 2, absMotion, .5f);

		if (AbstractChuteBlock.getChuteFacing(blockState) != Direction.DOWN)
			return;

		if (AbstractChuteBlock.isTransparentChute(blockState))
			spawnAirFlow(up ? 0 : 1, up ? 1 : 0, absMotion, 1);

		if (!up && BlockHelper.noCollisionInSpace(world, pos.down()))
			spawnAirFlow(0, -1, absMotion, .5f);

		if (up && canCollectItemsFromBelow() && bottomPullDistance > 0) {
			spawnAirFlow(-bottomPullDistance, 0, absMotion, 2);
			spawnAirFlow(-bottomPullDistance, 0, absMotion, 2);
		}
	}

	private void spawnAirFlow(float verticalStart, float verticalEnd, float motion, float drag) {
		if (world == null)
			return;
		AirParticleData airParticleData = new AirParticleData(drag, motion);
		Vec3d origin = Vec3d.of(pos);
		float xOff = Create.RANDOM.nextFloat() * .5f + .25f;
		float zOff = Create.RANDOM.nextFloat() * .5f + .25f;
		Vec3d v = origin.add(xOff, verticalStart, zOff);
		Vec3d d = origin.add(xOff, verticalEnd, zOff)
			.subtract(v);
		if (Create.RANDOM.nextFloat() < 2 * motion)
			world.addImportantParticle(airParticleData, v.x, v.y, v.z, d.x, d.y, d.z);
	}

	private void handleInputFromAbove() {
		Storage<ItemVariant> storage = grabCapability(Direction.UP);
		handleInput(storage, 1);
	}

	private void handleInputFromBelow() {
		Storage<ItemVariant> storage = grabCapability(Direction.DOWN);
		handleInput(storage, 0);
	}

	private void handleInput(@Nullable Storage<ItemVariant> inv, float startLocation) {
		if (inv == null)
			return;
		if (invVersionTracker.stillWaiting(inv))
			return;
		Predicate<ItemStack> canAccept = this::canAcceptItem;
		int count = getExtractionAmount();
		ExtractionCountMode mode = getExtractionMode();
		if (mode == ExtractionCountMode.UPTO || !ItemHelper.extract(inv, canAccept, mode, count, true)
			.isEmpty()) {
			ItemStack extracted = ItemHelper.extract(inv, canAccept, mode, count, false);
			if (!extracted.isEmpty()) {
				setItem(extracted, startLocation);
				return;
			}
		}
		invVersionTracker.awaitNewVersion(inv);
	}

	private boolean handleDownwardOutput(boolean simulate) {
		BlockState blockState = getCachedState();
		ChuteBlockEntity targetChute = getTargetChute(blockState);
		Direction direction = AbstractChuteBlock.getChuteFacing(blockState);

		if (world == null || direction == null || !this.canOutputItems())
			return false;
		Storage<ItemVariant> inv = grabCapability(Direction.DOWN);
		if (inv != null) {
			if (world.isClient && !isVirtual())
				return false;

			if (invVersionTracker.stillWaiting(inv))
				return false;

			try (Transaction t = TransferUtil.getTransaction()) {
				long inserted = inv.insert(ItemVariant.of(item), item.getCount(), t);
				if (inserted != 0 && !simulate) t.commit();
				ItemStack held = getItem();
				if (!simulate) {
					ItemStack newStack = held.copy();
					newStack.decrement(ItemHelper.truncateLong(inserted));
					setItem(newStack, itemPosition.getValue(0));
				}
				if (inserted != 0)
					return true;
			}

			// awaitNewVersion and getVersion cannot be called during a transaction and will throw an IllegalStateException,
			// so we call this outside of the transaction
			invVersionTracker.awaitNewVersion(inv);
			if (direction == Direction.DOWN)
				return false;
		}

		if (targetChute != null) {
			boolean canInsert = targetChute.canAcceptItem(item);
			if (!simulate && canInsert) {
				targetChute.setItem(item, direction == Direction.DOWN ? 1 : .51f);
				setItem(ItemStack.EMPTY);
			}
			return canInsert;
		}

		// Diagonal chutes cannot drop items
		if (direction.getAxis()
				.isHorizontal())
			return false;

		if (FunnelBlock.getFunnelFacing(world.getBlockState(pos.down())) == Direction.DOWN)
			return false;
		if (Block.hasTopRim(world, pos.down()))
			return false;

		if (!simulate) {
			Vec3d dropVec = VecHelper.getCenterOf(pos)
					.add(0, -12 / 16f, 0);
			ItemEntity dropped = new ItemEntity(world, dropVec.x, dropVec.y, dropVec.z, item.copy());
			dropped.setToDefaultPickupDelay();
			dropped.setVelocity(0, -.25f, 0);
			world.spawnEntity(dropped);
			setItem(ItemStack.EMPTY);
		}

		return true;
	}

	private boolean handleUpwardOutput(boolean simulate) {
		BlockState stateAbove = world.getBlockState(pos.up());

		if (world == null || !this.canOutputItems())
			return false;

		if (AbstractChuteBlock.isOpenChute(getCachedState())) {
			Storage<ItemVariant> inv = grabCapability(Direction.UP);
			if (inv != null) {
				if (world.isClient && !isVirtual() && !ChuteBlock.isChute(stateAbove))
					return false;

				if (invVersionTracker.stillWaiting(inv))
					return false;

				try (Transaction t = TransferUtil.getTransaction()) {
					long inserted = inv.insert(ItemVariant.of(item), item.getCount(), t);
					if (!simulate) {
						item = item.copy();
						item.decrement(ItemHelper.truncateLong(inserted));
						itemHandler.update();
						sendData();
						t.commit();
					}
					if (inserted != 0)
						return true;
				}

				// awaitNewVersion and getVersion cannot be called during a transaction and will throw an IllegalStateException,
				// so we call this outside of the transaction
				invVersionTracker.awaitNewVersion(inv);
				return false;
			}
		}

		ChuteBlockEntity bestOutput = null;
		List<ChuteBlockEntity> inputChutes = getInputChutes();
		for (ChuteBlockEntity targetChute : inputChutes) {
			if (!targetChute.canAcceptItem(item))
				continue;
			float itemMotion = targetChute.getItemMotion();
			if (itemMotion < 0)
				continue;
			if (bestOutput == null || bestOutput.getItemMotion() < itemMotion) {
				bestOutput = targetChute;
			}
		}

		if (bestOutput != null) {
			if (!simulate) {
				bestOutput.setItem(item, 0);
				setItem(ItemStack.EMPTY);
			}
			return true;
		}

		if (FunnelBlock.getFunnelFacing(world.getBlockState(pos.up())) == Direction.UP)
			return false;
		if (BlockHelper.hasBlockSolidSide(stateAbove, world, pos.up(), Direction.DOWN))
			return false;
		if (!inputChutes.isEmpty())
			return false;

		if (!simulate) {
			Vec3d dropVec = VecHelper.getCenterOf(pos)
					.add(0, 8 / 16f, 0);
			ItemEntity dropped = new ItemEntity(world, dropVec.x, dropVec.y, dropVec.z, item.copy());
			dropped.setToDefaultPickupDelay();
			dropped.setVelocity(0, getItemMotion() * 2, 0);
			world.spawnEntity(dropped);
			setItem(ItemStack.EMPTY);
		}
		return true;
	}

	protected boolean canAcceptItem(ItemStack stack) {
		return item.isEmpty();
	}

	protected int getExtractionAmount() {
		return 16;
	}

	protected ExtractionCountMode getExtractionMode() {
		return ExtractionCountMode.UPTO;
	}

	protected boolean canCollectItemsFromBelow() {
		return true;
	}

	protected boolean canOutputItems() {
		return true;
	}

	@Nullable
	private Storage<ItemVariant> grabCapability(Direction side) {
		if (world == null)
			return null;
		StorageProvider<ItemVariant> provider = side == Direction.UP ? capAbove : capBelow;
		BlockEntity be = provider.findBlockEntity();
		if (be instanceof ChuteBlockEntity) {
			if (side != Direction.DOWN || !(be instanceof SmartChuteBlockEntity) || getItemMotion() > 0)
				return null;
		}
		return provider.get(side.getOpposite());
	}

	public void setItem(ItemStack stack) {
		setItem(stack, getItemMotion() < 0 ? 1 : 0);
	}

	public void setItem(ItemStack stack, float insertionPos) {
		item = stack;
		itemPosition.startWithValue(insertionPos);
		itemHandler.update();
		invVersionTracker.reset();
		if (!world.isClient) {
			notifyUpdate();
			award(AllAdvancements.CHUTE);
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.put("Item", NBTSerializer.serializeNBT(item));
		compound.putFloat("ItemPosition", itemPosition.getValue());
		compound.putFloat("Pull", pull);
		compound.putFloat("Push", push);
		compound.putFloat("BottomAirFlowDistance", bottomPullDistance);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		ItemStack previousItem = item;
		item = ItemStack.fromNbt(compound.getCompound("Item"));
		itemHandler.update();
		itemPosition.startWithValue(compound.getFloat("ItemPosition"));
		pull = compound.getFloat("Pull");
		push = compound.getFloat("Push");
		bottomPullDistance = compound.getFloat("BottomAirFlowDistance");
		super.read(compound, clientPacket);
//		if (clientPacket)
//			airCurrent.rebuild();

		if (hasWorld() && world != null && world.isClient && !ItemStack.areEqual(previousItem, item) && !item.isEmpty()) {
			if (world.random.nextInt(3) != 0)
				return;
			Vec3d p = VecHelper.getCenterOf(pos);
			p = VecHelper.offsetRandomly(p, world.random, .5f);
			Vec3d m = Vec3d.ZERO;
			world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, item), p.x, p.y, p.z, m.x, m.y, m.z);
		}
	}

	public float getItemMotion() {
		// Chutes per second
		final float fanSpeedModifier = 1 / 64f;
		final float maxItemSpeed = 20f;
		final float gravity = 4f;

		float motion = (push + pull) * fanSpeedModifier;
		return (MathHelper.clamp(motion, -maxItemSpeed, maxItemSpeed) + (motion <= 0 ? -gravity : 0)) / 20f;
	}

	@Override
	public void destroy() {
		super.destroy();
		ChuteBlockEntity targetChute = getTargetChute(getCachedState());
		List<ChuteBlockEntity> inputChutes = getInputChutes();
		if (!item.isEmpty() && world != null)
			ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), item);
		markRemoved();
		if (targetChute != null) {
			targetChute.updatePull();
			targetChute.propagatePush();
		}
		inputChutes.forEach(c -> c.updatePush(inputChutes.size()));
	}

	public void onAdded() {
		refreshBlockState();
		updatePull();
		ChuteBlockEntity targetChute = getTargetChute(getCachedState());
		if (targetChute != null)
			targetChute.propagatePush();
		else
			updatePush(1);
	}

	public void updatePull() {
		float totalPull = calculatePull();
		if (pull == totalPull)
			return;
		pull = totalPull;
		updateAirFlow = true;
		sendData();
		ChuteBlockEntity targetChute = getTargetChute(getCachedState());
		if (targetChute != null)
			targetChute.updatePull();
	}

	public void updatePush(int branchCount) {
		float totalPush = calculatePush(branchCount);
		if (push == totalPush)
			return;
		updateAirFlow = true;
		push = totalPush;
		sendData();
		propagatePush();
	}

	public void propagatePush() {
		List<ChuteBlockEntity> inputs = getInputChutes();
		inputs.forEach(c -> c.updatePush(inputs.size()));
	}

	protected float calculatePull() {
		BlockState blockStateAbove = world.getBlockState(pos.up());
		if (AllBlocks.ENCASED_FAN.has(blockStateAbove)
			&& blockStateAbove.get(EncasedFanBlock.FACING) == Direction.DOWN) {
			BlockEntity be = world.getBlockEntity(pos.up());
			if (be instanceof EncasedFanBlockEntity && !be.isRemoved()) {
				EncasedFanBlockEntity fan = (EncasedFanBlockEntity) be;
				return fan.getSpeed();
			}
		}

		float totalPull = 0;
		for (Direction d : Iterate.directions) {
			ChuteBlockEntity inputChute = getInputChute(d);
			if (inputChute == null)
				continue;
			totalPull += inputChute.pull;
		}
		return totalPull;
	}

	protected float calculatePush(int branchCount) {
		if (world == null)
			return 0;
		BlockState blockStateBelow = world.getBlockState(pos.down());
		if (AllBlocks.ENCASED_FAN.has(blockStateBelow)
			&& blockStateBelow.get(EncasedFanBlock.FACING) == Direction.UP) {
			BlockEntity be = world.getBlockEntity(pos.down());
			if (be instanceof EncasedFanBlockEntity && !be.isRemoved()) {
				EncasedFanBlockEntity fan = (EncasedFanBlockEntity) be;
				return fan.getSpeed();
			}
		}

		ChuteBlockEntity targetChute = getTargetChute(getCachedState());
		if (targetChute == null)
			return 0;
		return targetChute.push / branchCount;
	}

	@Nullable
	private ChuteBlockEntity getTargetChute(BlockState state) {
		if (world == null)
			return null;
		Direction targetDirection = AbstractChuteBlock.getChuteFacing(state);
		if (targetDirection == null)
			return null;
		BlockPos chutePos = pos.down();
		if (targetDirection.getAxis()
			.isHorizontal())
			chutePos = chutePos.offset(targetDirection.getOpposite());
		BlockState chuteState = world.getBlockState(chutePos);
		if (!AbstractChuteBlock.isChute(chuteState))
			return null;
		BlockEntity be = world.getBlockEntity(chutePos);
		if (be instanceof ChuteBlockEntity)
			return (ChuteBlockEntity) be;
		return null;
	}

	private List<ChuteBlockEntity> getInputChutes() {
		List<ChuteBlockEntity> inputs = new LinkedList<>();
		for (Direction d : Iterate.directions) {
			ChuteBlockEntity inputChute = getInputChute(d);
			if (inputChute == null)
				continue;
			inputs.add(inputChute);
		}
		return inputs;
	}

	@Nullable
	private ChuteBlockEntity getInputChute(Direction direction) {
		if (world == null || direction == Direction.DOWN)
			return null;
		direction = direction.getOpposite();
		BlockPos chutePos = pos.up();
		if (direction.getAxis()
			.isHorizontal())
			chutePos = chutePos.offset(direction);
		BlockState chuteState = world.getBlockState(chutePos);
		Direction chuteFacing = AbstractChuteBlock.getChuteFacing(chuteState);
		if (chuteFacing != direction)
			return null;
		BlockEntity be = world.getBlockEntity(chutePos);
		if (be instanceof ChuteBlockEntity && !be.isRemoved())
			return (ChuteBlockEntity) be;
		return null;
	}

	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		boolean downward = getItemMotion() < 0;
		Lang.translate("tooltip.chute.header")
			.forGoggles(tooltip);

		if (pull == 0 && push == 0)
			Lang.translate("tooltip.chute.no_fans_attached")
				.style(Formatting.GRAY)
				.forGoggles(tooltip);
		if (pull != 0)
			Lang.translate("tooltip.chute.fans_" + (pull > 0 ? "pull_up" : "push_down"))
				.style(Formatting.GRAY)
				.forGoggles(tooltip);
		if (push != 0)
			Lang.translate("tooltip.chute.fans_" + (push > 0 ? "push_up" : "pull_down"))
				.style(Formatting.GRAY)
				.forGoggles(tooltip);

		Lang.text("-> ")
			.add(Lang.translate("tooltip.chute.items_move_" + (downward ? "down" : "up")))
			.style(Formatting.YELLOW)
			.forGoggles(tooltip);
		if (!item.isEmpty())
			Lang.translate("tooltip.chute.contains", Components.translatable(item.getTranslationKey())
				.getString(), item.getCount())
				.style(Formatting.GREEN)
				.forGoggles(tooltip);

		return true;
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		return itemHandler;
	}

	public ItemStack getItem() {
		return item;
	}

	// @Override
	// @Nullable
	// public AirCurrent getAirCurrent() {
	// return airCurrent;
	// }
	//
	// @Nullable
	// @Override
	// public World getAirCurrentWorld() {
	// return world;
	// }
	//
	// @Override
	// public BlockPos getAirCurrentPos() {
	// return pos;
	// }
	//
	// @Override
	// public float getSpeed() {
	// if (getBlockState().get(ChuteBlock.SHAPE) == Shape.NORMAL &&
	// getBlockState().get(ChuteBlock.FACING) != Direction.DOWN)
	// return 0;
	// return pull + push;
	// }
	//
	// @Override
	// @Nullable
	// public Direction getAirFlowDirection() {
	// float speed = getSpeed();
	// if (speed == 0)
	// return null;
	// return speed > 0 ? Direction.UP : Direction.DOWN;
	// }
	//
	// @Override
	// public boolean isSourceRemoved() {
	// return removed;
	// }
	//
	// @Override
	// public Direction getAirflowOriginSide() {
	// return world != null && !(world.getBlockEntity(pos.down()) instanceof
	// IAirCurrentSource)
	// && getBlockState().get(ChuteBlock.FACING) == Direction.DOWN ? Direction.DOWN
	// : Direction.UP;
	// }
}
