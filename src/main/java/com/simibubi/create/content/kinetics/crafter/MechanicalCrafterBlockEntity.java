package com.simibubi.create.content.kinetics.crafter;

import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler.ConnectedInput;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.edgeInteraction.EdgeInteractionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.Pointing;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MechanicalCrafterBlockEntity extends KineticBlockEntity implements SidedStorageBlockEntity {

	enum Phase {
		IDLE, ACCEPTING, ASSEMBLING, EXPORTING, WAITING, CRAFTING, INSERTING;
	}

	public static class Inventory extends SmartInventory {

		private MechanicalCrafterBlockEntity blockEntity;

		public Inventory(MechanicalCrafterBlockEntity blockEntity) {
			super(1, blockEntity, 1, false);
			this.blockEntity = blockEntity;
			forbidExtraction();
			whenContentsChanged(() -> {
				if (getStackInSlot(0).isEmpty()) // fabric: only has one slot, this is safe
					return;
				if (blockEntity.phase == Phase.IDLE)
					blockEntity.checkCompletedRecipe(false);
			});
		}

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			if (blockEntity.phase != Phase.IDLE)
				return 0;
			if (blockEntity.covered)
				return 0;
			long inserted = super.insert(resource, maxAmount, transaction);
			if (inserted != 0)
				TransactionCallback.onSuccess(transaction, () -> blockEntity.getWorld()
						.playSound(null, blockEntity.getPos(), SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, .25f,
								.5f));
			return inserted;
		}

	}

	protected Inventory inventory;
	protected GroupedItems groupedItems = new GroupedItems();
	protected ConnectedInput input = new ConnectedInput();
	protected boolean reRender;
	protected Phase phase;
	protected int countDown;
	protected boolean covered;
	protected boolean wasPoweredBefore;

	protected GroupedItems groupedItemsBeforeCraft; // for rendering on client
	private InvManipulationBehaviour inserting;
	private EdgeInteractionBehaviour connectivity;

	private ItemStack scriptedResult = ItemStack.EMPTY;

	public MechanicalCrafterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(20);
		phase = Phase.IDLE;
		groupedItemsBeforeCraft = new GroupedItems();
		inventory = new Inventory(this);

		// Does not get serialized due to active checking in tick
		wasPoweredBefore = true;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		inserting = new InvManipulationBehaviour(this, this::getTargetFace);
		connectivity = new EdgeInteractionBehaviour(this, ConnectedInputHandler::toggleConnection)
			.connectivity(ConnectedInputHandler::shouldConnect)
			.require(AllItems.WRENCH.get());
		behaviours.add(inserting);
		behaviours.add(connectivity);
		registerAwardables(behaviours, AllAdvancements.CRAFTER, AllAdvancements.CRAFTER_LAZY);
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (!MathHelper.approximatelyEquals(getSpeed(), 0)) {
			award(AllAdvancements.CRAFTER);
			if (Math.abs(getSpeed()) < 5)
				award(AllAdvancements.CRAFTER_LAZY);
		}
	}

	public void blockChanged() {
		removeBehaviour(InvManipulationBehaviour.TYPE);
		inserting = new InvManipulationBehaviour(this, this::getTargetFace);
		attachBehaviourLate(inserting);
	}

	public BlockFace getTargetFace(World world, BlockPos pos, BlockState state) {
		return new BlockFace(pos, MechanicalCrafterBlock.getTargetDirection(state));
	}

	public Direction getTargetDirection() {
		return MechanicalCrafterBlock.getTargetDirection(getCachedState());
	}

	@Override
	public void writeSafe(NbtCompound compound) {
		super.writeSafe(compound);
		if (input == null)
			return;

		NbtCompound inputNBT = new NbtCompound();
		input.write(inputNBT);
		compound.put("ConnectedInput", inputNBT);
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.put("Inventory", inventory.serializeNBT());

		NbtCompound inputNBT = new NbtCompound();
		input.write(inputNBT);
		compound.put("ConnectedInput", inputNBT);

		NbtCompound groupedItemsNBT = new NbtCompound();
		groupedItems.write(groupedItemsNBT);
		compound.put("GroupedItems", groupedItemsNBT);

		compound.putString("Phase", phase.name());
		compound.putInt("CountDown", countDown);
		compound.putBoolean("Cover", covered);

		super.write(compound, clientPacket);

		if (clientPacket && reRender) {
			compound.putBoolean("Redraw", true);
			reRender = false;
		}
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		Phase phaseBefore = phase;
		GroupedItems before = this.groupedItems;

		inventory.deserializeNBT(compound.getCompound("Inventory"));
		input.read(compound.getCompound("ConnectedInput"));
		groupedItems = GroupedItems.read(compound.getCompound("GroupedItems"));
		phase = Phase.IDLE;
		String name = compound.getString("Phase");
		for (Phase phase : Phase.values())
			if (phase.name()
				.equals(name))
				this.phase = phase;
		countDown = compound.getInt("CountDown");
		covered = compound.getBoolean("Cover");
		super.read(compound, clientPacket);
		if (!clientPacket)
			return;
		if (compound.contains("Redraw"))
			world.updateListeners(getPos(), getCachedState(), getCachedState(), 16);
		if (phaseBefore != phase && phase == Phase.CRAFTING)
			groupedItemsBeforeCraft = before;
		if (phaseBefore == Phase.EXPORTING && phase == Phase.WAITING) {
			Direction facing = getCachedState().get(MechanicalCrafterBlock.HORIZONTAL_FACING);
			Vec3d vec = Vec3d.of(facing.getVector())
				.multiply(.75)
				.add(VecHelper.getCenterOf(pos));
			Direction targetDirection = MechanicalCrafterBlock.getTargetDirection(getCachedState());
			vec = vec.add(Vec3d.of(targetDirection.getVector())
				.multiply(1));
			world.addParticle(ParticleTypes.CRIT, vec.x, vec.y, vec.z, 0, 0, 0);
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	public int getCountDownSpeed() {
		if (getSpeed() == 0)
			return 0;
		return MathHelper.clamp((int) Math.abs(getSpeed()), 4, 250);
	}

	@Override
	public void tick() {
		super.tick();

		if (phase == Phase.ACCEPTING)
			return;

		boolean onClient = world.isClient;
		boolean runLogic = !onClient || isVirtual();

		if (wasPoweredBefore != world.isReceivingRedstonePower(pos)) {
			wasPoweredBefore = world.isReceivingRedstonePower(pos);
			if (wasPoweredBefore) {
				if (!runLogic)
					return;
				checkCompletedRecipe(true);
			}
		}

		if (phase == Phase.ASSEMBLING) {
			countDown -= getCountDownSpeed();
			if (countDown < 0) {
				countDown = 0;
				if (!runLogic)
					return;
				if (RecipeGridHandler.getTargetingCrafter(this) != null) {
					phase = Phase.EXPORTING;
					countDown = 1000;
					sendData();
					return;
				}

				ItemStack result =
					isVirtual() ? scriptedResult : RecipeGridHandler.tryToApplyRecipe(world, groupedItems);

				if (result != null) {
					List<ItemStack> containers = new ArrayList<>();
					groupedItems.grid.values()
						.forEach(stack -> {
							ItemStack remaining = stack.getRecipeRemainder();
							if (!remaining.isEmpty())
								containers.add(remaining);
						});

					if (isVirtual())
						groupedItemsBeforeCraft = groupedItems;

					groupedItems = new GroupedItems(result);
					for (int i = 0; i < containers.size(); i++) {
						ItemStack stack = containers.get(i);
						GroupedItems container = new GroupedItems();
						container.grid.put(Pair.of(i, 0), stack);
						container.mergeOnto(groupedItems, Pointing.LEFT);
					}

					phase = Phase.CRAFTING;
					countDown = 2000;
					sendData();
					return;
				}
				ejectWholeGrid();
				return;
			}
		}

		if (phase == Phase.EXPORTING) {
			countDown -= getCountDownSpeed();

			if (countDown < 0) {
				countDown = 0;
				if (!runLogic)
					return;

				MechanicalCrafterBlockEntity targetingCrafter = RecipeGridHandler.getTargetingCrafter(this);
				if (targetingCrafter == null) {
					ejectWholeGrid();
					return;
				}

				Pointing pointing = getCachedState().get(MechanicalCrafterBlock.POINTING);
				groupedItems.mergeOnto(targetingCrafter.groupedItems, pointing);
				groupedItems = new GroupedItems();

				float pitch = targetingCrafter.groupedItems.grid.size() * 1 / 16f + .5f;
				AllSoundEvents.CRAFTER_CLICK.playOnServer(world, pos, 1, pitch);

				phase = Phase.WAITING;
				countDown = 0;
				sendData();
				targetingCrafter.continueIfAllPrecedingFinished();
				targetingCrafter.sendData();
				return;
			}
		}

		if (phase == Phase.CRAFTING) {

			if (onClient) {
				Direction facing = getCachedState().get(MechanicalCrafterBlock.HORIZONTAL_FACING);
				float progress = countDown / 2000f;
				Vec3d facingVec = Vec3d.of(facing.getVector());
				Vec3d vec = facingVec.multiply(.65)
					.add(VecHelper.getCenterOf(pos));
				Vec3d offset = VecHelper.offsetRandomly(Vec3d.ZERO, world.random, .125f)
					.multiply(VecHelper.axisAlingedPlaneOf(facingVec))
					.normalize()
					.multiply(progress * .5f)
					.add(vec);
				if (progress > .5f)
					world.addParticle(ParticleTypes.CRIT, offset.x, offset.y, offset.z, 0, 0, 0);

				if (!groupedItemsBeforeCraft.grid.isEmpty() && progress < .5f) {
					if (groupedItems.grid.containsKey(Pair.of(0, 0))) {
						ItemStack stack = groupedItems.grid.get(Pair.of(0, 0));
						groupedItemsBeforeCraft = new GroupedItems();

						for (int i = 0; i < 10; i++) {
							Vec3d randVec = VecHelper.offsetRandomly(Vec3d.ZERO, world.random, .125f)
								.multiply(VecHelper.axisAlingedPlaneOf(facingVec))
								.normalize()
								.multiply(.25f);
							Vec3d offset2 = randVec.add(vec);
							randVec = randVec.multiply(.35f);
							world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, stack), offset2.x, offset2.y,
								offset2.z, randVec.x, randVec.y, randVec.z);
						}
					}
				}
			}

			int prev = countDown;
			countDown -= getCountDownSpeed();

			if (countDown < 1000 && prev >= 1000) {
				AllSoundEvents.CRAFTER_CLICK.playOnServer(world, pos, 1, 2);
				AllSoundEvents.CRAFTER_CRAFT.playOnServer(world, pos);
			}

			if (countDown < 0) {
				countDown = 0;
				if (!runLogic)
					return;
				tryInsert();
				return;
			}
		}

		if (phase == Phase.INSERTING) {
			if (runLogic && isTargetingBelt())
				tryInsert();
			return;
		}
	}

	protected boolean isTargetingBelt() {
		DirectBeltInputBehaviour behaviour = getTargetingBelt();
		return behaviour != null && behaviour.canInsertFromSide(getTargetDirection());
	}

	protected DirectBeltInputBehaviour getTargetingBelt() {
		BlockPos targetPos = pos.offset(getTargetDirection());
		return BlockEntityBehaviour.get(world, targetPos, DirectBeltInputBehaviour.TYPE);
	}

	public void tryInsert() {
		if (!inserting.hasInventory() && !isTargetingBelt()) {
			ejectWholeGrid();
			return;
		}

		boolean chagedPhase = phase != Phase.INSERTING;
		final List<Pair<Integer, Integer>> inserted = new LinkedList<>();

		DirectBeltInputBehaviour behaviour = getTargetingBelt();
		for (Entry<Pair<Integer, Integer>, ItemStack> entry : groupedItems.grid.entrySet()) {
			Pair<Integer, Integer> pair = entry.getKey();
			ItemStack stack = entry.getValue();
			BlockFace face = getTargetFace(world, pos, getCachedState());

			ItemStack remainder = behaviour == null ? inserting.insert(stack.copy())
				: behaviour.handleInsertion(stack, face.getFace(), false);
			if (!remainder.isEmpty()) {
				stack.setCount(remainder.getCount());
				continue;
			}

			inserted.add(pair);
		}

		inserted.forEach(groupedItems.grid::remove);
		if (groupedItems.grid.isEmpty())
			ejectWholeGrid();
		else
			phase = Phase.INSERTING;
		if (!inserted.isEmpty() || chagedPhase)
			sendData();
	}

	public void ejectWholeGrid() {
		List<MechanicalCrafterBlockEntity> chain = RecipeGridHandler.getAllCraftersOfChain(this);
		if (chain == null)
			return;
		chain.forEach(MechanicalCrafterBlockEntity::eject);
	}

	public void eject() {
		BlockState blockState = getCachedState();
		boolean present = AllBlocks.MECHANICAL_CRAFTER.has(blockState);
		Vec3d vec = present ? Vec3d.of(blockState.get(HORIZONTAL_FACING)
			.getVector())
			.multiply(.75f) : Vec3d.ZERO;
		Vec3d ejectPos = VecHelper.getCenterOf(pos)
			.add(vec);
		groupedItems.grid.forEach((pair, stack) -> dropItem(ejectPos, stack));
		if (!inventory.getStack(0)
			.isEmpty())
			dropItem(ejectPos, inventory.getStack(0));
		phase = Phase.IDLE;
		groupedItems = new GroupedItems();
		inventory.setStackInSlot(0, ItemStack.EMPTY);
		sendData();
	}

	public void dropItem(Vec3d ejectPos, ItemStack stack) {
		ItemEntity itemEntity = new ItemEntity(world, ejectPos.x, ejectPos.y, ejectPos.z, stack);
		itemEntity.setToDefaultPickupDelay();
		world.spawnEntity(itemEntity);
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (world.isClient && !isVirtual())
			return;
		if (phase == Phase.IDLE && craftingItemPresent())
			checkCompletedRecipe(false);
		if (phase == Phase.INSERTING)
			tryInsert();
	}

	public boolean craftingItemPresent() {
		return !inventory.getStack(0)
			.isEmpty();
	}

	public boolean craftingItemOrCoverPresent() {
		return !inventory.getStack(0)
			.isEmpty() || covered;
	}

	protected void checkCompletedRecipe(boolean poweredStart) {
		if (getSpeed() == 0)
			return;
		if (world.isClient && !isVirtual())
			return;
		List<MechanicalCrafterBlockEntity> chain = RecipeGridHandler.getAllCraftersOfChainIf(this,
			poweredStart ? MechanicalCrafterBlockEntity::craftingItemPresent
				: MechanicalCrafterBlockEntity::craftingItemOrCoverPresent,
			poweredStart);
		if (chain == null)
			return;
		chain.forEach(MechanicalCrafterBlockEntity::begin);
	}

	protected void begin() {
		phase = Phase.ACCEPTING;
		groupedItems = new GroupedItems(inventory.getStack(0));
		inventory.setStackInSlot(0, ItemStack.EMPTY);
		if (RecipeGridHandler.getPrecedingCrafters(this)
			.isEmpty()) {
			phase = Phase.ASSEMBLING;
			countDown = 500;
		}
		sendData();
	}

	protected void continueIfAllPrecedingFinished() {
		List<MechanicalCrafterBlockEntity> preceding = RecipeGridHandler.getPrecedingCrafters(this);
		if (preceding == null) {
			ejectWholeGrid();
			return;
		}

		for (MechanicalCrafterBlockEntity blockEntity : preceding)
			if (blockEntity.phase != Phase.WAITING)
				return;

		phase = Phase.ASSEMBLING;
		countDown = Math.max(100, getCountDownSpeed() + 1);
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		return input.getItemHandler(world, pos);
	}

	public void connectivityChanged() {
		reRender = true;
		sendData();
	}

	public Inventory getInventory() {
		return inventory;
	}

	public void setScriptedResult(ItemStack scriptedResult) {
		this.scriptedResult = scriptedResult;
	}

}
