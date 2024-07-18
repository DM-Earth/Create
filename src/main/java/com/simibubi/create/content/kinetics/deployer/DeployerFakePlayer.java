package com.simibubi.create.content.kinetics.deployer;

import java.util.Collection;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

import javax.annotation.Nullable;

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.util.UsernameCache;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CKinetics;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class DeployerFakePlayer extends FakePlayer {

	private static final ClientConnection NETWORK_MANAGER = new ClientConnection(NetworkSide.CLIENTBOUND);
	public static final UUID fallbackID = UUID.fromString("9e2faded-cafe-4ec2-c314-dad129ae971d");
	Pair<BlockPos, Float> blockBreakingProgress;
	ItemStack spawnedItemEffects;
	public boolean placedTracks;
	public boolean onMinecartContraption;
	private UUID owner;

	public DeployerFakePlayer(ServerWorld world, @Nullable UUID owner) {
		super(world, new DeployerGameProfile(fallbackID, "Deployer", owner));
		// fabric: use the default FakePacketListener
//		connection = new FakePlayNetHandler(world.getServer(), this);
		this.owner = owner;
	}

	@Override
	public OptionalInt openHandledScreen(NamedScreenHandlerFactory menuProvider) {
		return OptionalInt.empty();
	}

	@Override
	public Text getDisplayName() {
		return Lang.translateDirect("block.deployer.damage_source_name");
	}

	@Override
	@Environment(EnvType.CLIENT)
	public float getEyeHeight(EntityPose poseIn) {
		return 0;
	}

	@Override
	public Vec3d getPos() {
		return new Vec3d(getX(), getY(), getZ());
	}

	@Override
	public float getAttackCooldownProgressPerTick() {
		return 1 / 64f;
	}

	@Override
	public boolean canConsume(boolean ignoreHunger) {
		return false;
	}

	@Override
	public ItemStack eatFood(World world, ItemStack stack) {
		stack.decrement(1);
		return stack;
	}

	@Override
	public boolean canHaveStatusEffect(StatusEffectInstance pEffectInstance) {
		return false;
	}

	@Override
	public UUID getUuid() {
		return owner == null ? super.getUuid() : owner;
	}

	public static void deployerHasEyesOnHisFeet(EntityEvents.Size event) {
		if (event.getEntity() instanceof DeployerFakePlayer)
			event.setNewEyeHeight(0);
	}

	public static boolean deployerCollectsDropsFromKilledEntities(LivingEntity target, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit) {
		Entity trueSource = source.getAttacker();
		if (trueSource != null && trueSource instanceof DeployerFakePlayer) {
			DeployerFakePlayer fakePlayer = (DeployerFakePlayer) trueSource;
			drops
				.forEach(stack -> fakePlayer.getInventory()
					.offerOrDrop(stack.getStack()));
			return true;
		}
		return false;
	}

	@Override
	protected boolean isArmorSlot(EquipmentSlot p_217035_) {
		return false;
	}

	@Override
	public void remove(RemovalReason p_150097_) {
		if (blockBreakingProgress != null && !getWorld().isClient)
			getWorld().setBlockBreakingInfo(getId(), blockBreakingProgress.getKey(), -1);
		super.remove(p_150097_);
	}

	public static int deployerKillsDoNotSpawnXP(int i, PlayerEntity player, LivingEntity entity) {
		if (player instanceof DeployerFakePlayer)
			return 0;
		return i;
	}

	public static void entitiesDontRetaliate(LivingEntityEvents.ChangeTarget.ChangeTargetEvent event) {
		if (!(event.getOriginalTarget() instanceof DeployerFakePlayer))
			return;
		LivingEntity entityLiving = (LivingEntity) event.getEntity();
		if (!(entityLiving instanceof MobEntity mob))
			return;

		CKinetics.DeployerAggroSetting setting = AllConfigs.server().kinetics.ignoreDeployerAttacks.get();

		switch (setting) {
		case ALL:
			event.setCanceled(true);
			break;
		case CREEPERS:
			if (mob instanceof CreeperEntity)
				event.setCanceled(true);
			break;
		case NONE:
		default:
		}
	}

	// Credit to Mekanism for this approach. Helps fake players get past claims and
	// protection by other mods
	private static class DeployerGameProfile extends GameProfile {

		private UUID owner;

		public DeployerGameProfile(UUID id, String name, UUID owner) {
			super(id, name);
			this.owner = owner;
		}

		@Override
		public UUID getId() {
			return owner == null ? super.getId() : owner;
		}

		@Override
		public String getName() {
			if (owner == null)
				return super.getName();
			String lastKnownUsername = UsernameCache.getLastKnownUsername(owner);
			return lastKnownUsername == null ? super.getName() : lastKnownUsername;
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o)
				return true;
			if (!(o instanceof GameProfile otherProfile))
				return false;
			return Objects.equals(getId(), otherProfile.getId()) && Objects.equals(getName(), otherProfile.getName());
		}

		@Override
		public int hashCode() {
			UUID id = getId();
			String name = getName();
			int result = id == null ? 0 : id.hashCode();
			result = 31 * result + (name == null ? 0 : name.hashCode());
			return result;
		}
	}

	// fabric: FakePlayNetHandler removed, unused

}
