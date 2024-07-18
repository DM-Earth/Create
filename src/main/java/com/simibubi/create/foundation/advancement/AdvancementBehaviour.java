package com.simibubi.create.foundation.advancement;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class AdvancementBehaviour extends BlockEntityBehaviour {

	public static final BehaviourType<AdvancementBehaviour> TYPE = new BehaviourType<>();

	private UUID playerId;
	private Set<CreateAdvancement> advancements;

	public AdvancementBehaviour(SmartBlockEntity be, CreateAdvancement... advancements) {
		super(be);
		this.advancements = new HashSet<>();
		add(advancements);
	}

	public void add(CreateAdvancement... advancements) {
		for (CreateAdvancement advancement : advancements)
			this.advancements.add(advancement);
	}

	public boolean isOwnerPresent() {
		return playerId != null;
	}

	public void setPlayer(UUID id) {
		PlayerEntity player = getWorld().getPlayerByUuid(id);
		if (player == null)
			return;
		playerId = id;
		removeAwarded();
		blockEntity.markDirty();
	}

	@Override
	public void initialize() {
		super.initialize();
		removeAwarded();
	}

	private void removeAwarded() {
		PlayerEntity player = getPlayer();
		if (player == null)
			return;
		advancements.removeIf(c -> c.isAlreadyAwardedTo(player));
		if (advancements.isEmpty()) {
			playerId = null;
			blockEntity.markDirty();
		}
	}

	public void awardPlayerIfNear(CreateAdvancement advancement, int maxDistance) {
		PlayerEntity player = getPlayer();
		if (player == null)
			return;
		if (player.squaredDistanceTo(Vec3d.ofCenter(getPos())) > maxDistance * maxDistance)
			return;
		award(advancement, player);
	}

	public void awardPlayer(CreateAdvancement advancement) {
		PlayerEntity player = getPlayer();
		if (player == null)
			return;
		award(advancement, player);
	}

	private void award(CreateAdvancement advancement, PlayerEntity player) {
		if (advancements.contains(advancement))
			advancement.awardTo(player);
		removeAwarded();
	}

	private PlayerEntity getPlayer() {
		if (playerId == null)
			return null;
		return getWorld().getPlayerByUuid(playerId);
	}

	@Override
	public void write(NbtCompound nbt, boolean clientPacket) {
		super.write(nbt, clientPacket);
		if (playerId != null)
			nbt.putUuid("Owner", playerId);
	}

	@Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		super.read(nbt, clientPacket);
		if (nbt.contains("Owner"))
			playerId = nbt.getUuid("Owner");
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	public static void tryAward(BlockView reader, BlockPos pos, CreateAdvancement advancement) {
		AdvancementBehaviour behaviour = BlockEntityBehaviour.get(reader, pos, AdvancementBehaviour.TYPE);
		if (behaviour != null)
			behaviour.awardPlayer(advancement);
	}

	public static void setPlacedBy(World worldIn, BlockPos pos, LivingEntity placer) {
		AdvancementBehaviour behaviour = BlockEntityBehaviour.get(worldIn, pos, TYPE);
		if (behaviour == null)
			return;
		if (placer instanceof ServerPlayerEntity player && !(player instanceof FakePlayer))
			behaviour.setPlayer(placer.getUuid());
	}

}
