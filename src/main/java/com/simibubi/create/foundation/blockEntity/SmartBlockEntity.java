package com.simibubi.create.foundation.blockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import com.simibubi.create.api.event.BlockEntityBehaviourEvent;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockEntityItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.IInteractionChecker;
import com.simibubi.create.foundation.utility.IPartialSafeNBT;

import io.github.fabricators_of_create.porting_lib.block.ChunkUnloadListeningBlockEntity;

public abstract class SmartBlockEntity extends CachedRenderBBBlockEntity
	implements IPartialSafeNBT, IInteractionChecker, ChunkUnloadListeningBlockEntity, ISpecialBlockEntityItemRequirement {

	private final Map<BehaviourType<?>, BlockEntityBehaviour> behaviours = new HashMap<>();
	private boolean initialized = false;
	private boolean firstNbtRead = true;
	protected int lazyTickRate;
	protected int lazyTickCounter;
	private boolean chunkUnloaded;

	// Used for simulating this BE in a client-only setting
	private boolean virtualMode;

	public SmartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);

		setLazyTickRate(10);

		ArrayList<BlockEntityBehaviour> list = new ArrayList<>();
		addBehaviours(list);
		list.forEach(b -> behaviours.put(b.getType(), b));
	}

	public abstract void addBehaviours(List<BlockEntityBehaviour> behaviours);

	/**
	 * Gets called just before reading block entity data for behaviours. Register
	 * anything here that depends on your custom BE data.
	 */
	public void addBehavioursDeferred(List<BlockEntityBehaviour> behaviours) {}

	public void initialize() {
		if (firstNbtRead) {
			firstNbtRead = false;
			BlockEntityBehaviourEvent.EVENT.invoker().manageBehaviors(new BlockEntityBehaviourEvent(this, behaviours));
		}

		forEachBehaviour(BlockEntityBehaviour::initialize);
		lazyTick();
	}

	public void tick() {
		if (!initialized && hasWorld()) {
			initialize();
			initialized = true;
		}

		if (lazyTickCounter-- <= 0) {
			lazyTickCounter = lazyTickRate;
			lazyTick();
		}

		forEachBehaviour(BlockEntityBehaviour::tick);
	}

	public void lazyTick() {}

	/**
	 * Hook only these in future subclasses of STE
	 */
	protected void write(NbtCompound tag, boolean clientPacket) {
		super.writeNbt(tag);
		forEachBehaviour(tb -> tb.write(tag, clientPacket));
	}

	@Override
	public void writeSafe(NbtCompound tag) {
		super.writeNbt(tag);
		forEachBehaviour(tb -> {
			if (tb.isSafeNBT())
				tb.write(tag, false);
		});
	}

	/**
	 * Hook only these in future subclasses of STE
	 */
	protected void read(NbtCompound tag, boolean clientPacket) {
		if (firstNbtRead) {
			firstNbtRead = false;
			ArrayList<BlockEntityBehaviour> list = new ArrayList<>();
			addBehavioursDeferred(list);
			list.forEach(b -> behaviours.put(b.getType(), b));
			BlockEntityBehaviourEvent.EVENT.invoker().manageBehaviors(new BlockEntityBehaviourEvent(this, behaviours));
		}
		super.readNbt(tag);
		forEachBehaviour(tb -> tb.read(tag, clientPacket));
	}

	@Override
	public final void readNbt(NbtCompound tag) {
		read(tag, false);
	}

	@Override
	public void onChunkUnloaded() {
		ChunkUnloadListeningBlockEntity.super.onChunkUnloaded();
		chunkUnloaded = true;
	}

	@Override
	public final void markRemoved() {
		super.markRemoved();
		if (!chunkUnloaded)
			remove();
		invalidate();
	}

	/**
	 * Block destroyed or Chunk unloaded. Usually invalidates capabilities
	 */
	public void invalidate() {
		forEachBehaviour(BlockEntityBehaviour::unload);
	}

	/**
	 * Block destroyed or picked up by a contraption. Usually detaches kinetics
	 */
	public void remove() {}

	/**
	 * Block destroyed or replaced. Requires Block to call IBE::onRemove
	 */
	public void destroy() {
		forEachBehaviour(BlockEntityBehaviour::destroy);
	}

	@Override
	public final void writeNbt(NbtCompound tag) {
		write(tag, false);
	}

	@Override
	public final void readClient(NbtCompound tag) {
		read(tag, true);
	}

	@Override
	public final NbtCompound writeClient(NbtCompound tag) {
		write(tag, true);
		return tag;
	}

	@SuppressWarnings("unchecked")
	public <T extends BlockEntityBehaviour> T getBehaviour(BehaviourType<T> type) {
		return (T) behaviours.get(type);
	}

	public void forEachBehaviour(Consumer<BlockEntityBehaviour> action) {
		getAllBehaviours().forEach(action);
	}

	public Collection<BlockEntityBehaviour> getAllBehaviours() {
		return behaviours.values();
	}

	protected void attachBehaviourLate(BlockEntityBehaviour behaviour) {
		behaviours.put(behaviour.getType(), behaviour);
		behaviour.initialize();
	}

	public ItemRequirement getRequiredItems(BlockState state) {
		return getAllBehaviours().stream()
			.reduce(ItemRequirement.NONE, (r, b) -> r.union(b.getRequiredItems()), (r, r1) -> r.union(r1));
	}

	protected void removeBehaviour(BehaviourType<?> type) {
		BlockEntityBehaviour remove = behaviours.remove(type);
		if (remove != null) {
			remove.unload();
		}
	}

	public void setLazyTickRate(int slowTickRate) {
		this.lazyTickRate = slowTickRate;
		this.lazyTickCounter = slowTickRate;
	}

	public void markVirtual() {
		virtualMode = true;
	}

	public boolean isVirtual() {
		return virtualMode;
	}

	public boolean isChunkUnloaded() {
		return chunkUnloaded;
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		if (world == null || world.getBlockEntity(pos) != this)
			return false;
		return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D,
			pos.getZ() + 0.5D) <= 64.0D;
	}

	public void sendToMenu(PacketByteBuf buffer) {
		buffer.writeBlockPos(getPos());
		buffer.writeNbt(toInitialChunkDataNbt());
	}

	@SuppressWarnings("deprecation")
	public void refreshBlockState() {
		setCachedState(getWorld().getBlockState(getPos()));
	}

//	protected boolean isItemHandlerCap(Capability<?> cap) {
//		return cap == ForgeCapabilities.ITEM_HANDLER;
//	}
//
//	protected boolean isFluidHandlerCap(Capability<?> cap) {
//		return cap == ForgeCapabilities.FLUID_HANDLER;
//	}

	public void registerAwardables(List<BlockEntityBehaviour> behaviours, CreateAdvancement... advancements) {
		for (BlockEntityBehaviour behaviour : behaviours) {
			if (behaviour instanceof AdvancementBehaviour ab) {
				ab.add(advancements);
				return;
			}
		}
		behaviours.add(new AdvancementBehaviour(this, advancements));
	}

	public void award(CreateAdvancement advancement) {
		AdvancementBehaviour behaviour = getBehaviour(AdvancementBehaviour.TYPE);
		if (behaviour != null)
			behaviour.awardPlayer(advancement);
	}

	public void awardIfNear(CreateAdvancement advancement, int range) {
		AdvancementBehaviour behaviour = getBehaviour(AdvancementBehaviour.TYPE);
		if (behaviour != null)
			behaviour.awardPlayerIfNear(advancement, range);
	}
}
