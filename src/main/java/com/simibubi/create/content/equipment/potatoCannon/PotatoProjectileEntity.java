package com.simibubi.create.content.equipment.potatoCannon;

import io.github.fabricators_of_create.porting_lib.entity.IEntityAdditionalSpawnData;

import io.github.fabricators_of_create.porting_lib.entity.PortingLibEntity;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.AllEnchantments;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.particle.AirParticleData;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PotatoProjectileEntity extends ExplosiveProjectileEntity implements IEntityAdditionalSpawnData {

	protected PotatoCannonProjectileType type;
	protected ItemStack stack = ItemStack.EMPTY;

	protected Entity stuckEntity;
	protected Vec3d stuckOffset;
	protected PotatoProjectileRenderMode stuckRenderer;
	protected double stuckFallSpeed;

	protected float additionalDamageMult = 1;
	protected float additionalKnockback = 0;
	protected float recoveryChance = 0;

	public PotatoProjectileEntity(EntityType<? extends ExplosiveProjectileEntity> type, World world) {
		super(type, world);
	}

	public ItemStack getItem() {
		return stack;
	}

	public void setItem(ItemStack stack) {
		this.stack = stack;
	}

	public PotatoCannonProjectileType getProjectileType() {
		if (type == null)
			type = PotatoProjectileTypeManager.getTypeForStack(stack)
				.orElse(BuiltinPotatoProjectileTypes.FALLBACK);
		return type;
	}

	public void setEnchantmentEffectsFromCannon(ItemStack cannon) {
		int power = EnchantmentHelper.getLevel(Enchantments.POWER, cannon);
		int punch = EnchantmentHelper.getLevel(Enchantments.PUNCH, cannon);
		int flame = EnchantmentHelper.getLevel(Enchantments.FLAME, cannon);
		int recovery = EnchantmentHelper.getLevel(AllEnchantments.POTATO_RECOVERY.get(), cannon);

		if (power > 0)
			additionalDamageMult = 1 + power * .2f;
		if (punch > 0)
			additionalKnockback = punch * .5f;
		if (flame > 0)
			setOnFireFor(100);
		if (recovery > 0)
			recoveryChance = .125f + recovery * .125f;
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		stack = ItemStack.fromNbt(nbt.getCompound("Item"));
		additionalDamageMult = nbt.getFloat("AdditionalDamage");
		additionalKnockback = nbt.getFloat("AdditionalKnockback");
		recoveryChance = nbt.getFloat("Recovery");
		super.readCustomDataFromNbt(nbt);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.put("Item", NBTSerializer.serializeNBT(stack));
		nbt.putFloat("AdditionalDamage", additionalDamageMult);
		nbt.putFloat("AdditionalKnockback", additionalKnockback);
		nbt.putFloat("Recovery", recoveryChance);
		super.writeCustomDataToNbt(nbt);
	}

	public Entity getStuckEntity() {
		if (stuckEntity == null)
			return null;
		if (!stuckEntity.isAlive())
			return null;
		return stuckEntity;
	}

	public void setStuckEntity(Entity stuckEntity) {
		this.stuckEntity = stuckEntity;
		this.stuckOffset = getPos().subtract(stuckEntity.getPos());
		this.stuckRenderer = new PotatoProjectileRenderMode.StuckToEntity(stuckOffset);
		this.stuckFallSpeed = 0.0;
		setVelocity(Vec3d.ZERO);
	}

	public PotatoProjectileRenderMode getRenderMode() {
		if (getStuckEntity() != null)
			return stuckRenderer;

		return getProjectileType().getRenderMode();
	}

	public void tick() {
		PotatoCannonProjectileType projectileType = getProjectileType();

		Entity stuckEntity = getStuckEntity();
		if (stuckEntity != null) {
			if (getY() < stuckEntity.getY() - 0.1) {
				pop(getPos());
				kill();
			} else {
				stuckFallSpeed += 0.007 * projectileType.getGravityMultiplier();
				stuckOffset = stuckOffset.add(0, -stuckFallSpeed, 0);
				Vec3d pos = stuckEntity.getPos()
					.add(stuckOffset);
				setPosition(pos.x, pos.y, pos.z);
			}
		} else {
			setVelocity(getVelocity().add(0, -0.05 * projectileType.getGravityMultiplier(), 0)
				.multiply(projectileType.getDrag()));
		}

		super.tick();
	}

	@Override
	protected float getDrag() {
		return 1;
	}

	@Override
	protected ParticleEffect getParticleType() {
		return new AirParticleData(1, 10);
	}

	@Override
	protected boolean isBurning() {
		return false;
	}

	@Override
	protected void onEntityHit(EntityHitResult ray) {
		super.onEntityHit(ray);

		if (getStuckEntity() != null)
			return;

		Vec3d hit = ray.getPos();
		Entity target = ray.getEntity();
		PotatoCannonProjectileType projectileType = getProjectileType();
		float damage = projectileType.getDamage() * additionalDamageMult;
		float knockback = projectileType.getKnockback() + additionalKnockback;
		Entity owner = this.getOwner();

		if (!target.isAlive())
			return;
		if (owner instanceof LivingEntity)
			((LivingEntity) owner).onAttacking(target);

		if (target instanceof PotatoProjectileEntity ppe) {
			if (age < 10 && target.age < 10)
				return;
			if (ppe.getProjectileType() != getProjectileType()) {
				if (owner instanceof PlayerEntity p)
					AllAdvancements.POTATO_CANNON_COLLIDE.awardTo(p);
				if (ppe.getOwner() instanceof PlayerEntity p)
					AllAdvancements.POTATO_CANNON_COLLIDE.awardTo(p);
			}
		}

		pop(hit);

		if (target instanceof WitherEntity && ((WitherEntity) target).shouldRenderOverlay())
			return;
		if (projectileType.preEntityHit(ray))
			return;

		boolean targetIsEnderman = target.getType() == EntityType.ENDERMAN;
		int k = target.getFireTicks();
		if (this.isOnFire() && !targetIsEnderman)
			target.setOnFireFor(5);

		boolean onServer = !getWorld().isClient;
		if (onServer && !target.damage(causePotatoDamage(), damage)) {
			target.setFireTicks(k);
			kill();
			return;
		}

		if (targetIsEnderman)
			return;

		if (!projectileType.onEntityHit(ray) && onServer)
			if (random.nextDouble() <= recoveryChance)
				recoverItem();

		if (!(target instanceof LivingEntity)) {
			playHitSound(getWorld(), getPos());
			kill();
			return;
		}

		LivingEntity livingentity = (LivingEntity) target;

		if (type.getReloadTicks() < 10)
			livingentity.timeUntilRegen = type.getReloadTicks() + 10;

		if (onServer && knockback > 0) {
			Vec3d appliedMotion = this.getVelocity()
				.multiply(1.0D, 0.0D, 1.0D)
				.normalize()
				.multiply(knockback * 0.6);
			if (appliedMotion.lengthSquared() > 0.0D)
				livingentity.addVelocity(appliedMotion.x, 0.1D, appliedMotion.z);
		}

		if (onServer && owner instanceof LivingEntity) {
			EnchantmentHelper.onUserDamaged(livingentity, owner);
			EnchantmentHelper.onTargetDamaged((LivingEntity) owner, livingentity);
		}

		if (livingentity != owner && livingentity instanceof PlayerEntity && owner instanceof ServerPlayerEntity
			&& !this.isSilent()) {
			((ServerPlayerEntity) owner).networkHandler
				.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.PROJECTILE_HIT_PLAYER, 0.0F));
		}

		if (onServer && owner instanceof ServerPlayerEntity) {
			ServerPlayerEntity serverplayerentity = (ServerPlayerEntity) owner;
			if (!target.isAlive() && target.getType()
				.getSpawnGroup() == SpawnGroup.MONSTER || (target instanceof PlayerEntity && target != owner))
				AllAdvancements.POTATO_CANNON.awardTo(serverplayerentity);
		}

		if (type.isSticky() && target.isAlive()) {
			setStuckEntity(target);
		} else {
			kill();
		}

	}

	private void recoverItem() {
		if (!stack.isEmpty())
			dropStack(ItemHandlerHelper.copyStackWithSize(stack, 1));
	}

	public static void playHitSound(World world, Vec3d location) {
		AllSoundEvents.POTATO_HIT.playOnServer(world, BlockPos.ofFloored(location));
	}

	public static void playLaunchSound(World world, Vec3d location, float pitch) {
		AllSoundEvents.FWOOMP.playAt(world, location, 1, pitch, true);
	}

	@Override
	protected void onBlockHit(BlockHitResult ray) {
		Vec3d hit = ray.getPos();
		pop(hit);
		if (!getProjectileType().onBlockHit(getWorld(), ray) && !getWorld().isClient)
			if (random.nextDouble() <= recoveryChance)
				recoverItem();
		super.onBlockHit(ray);
		kill();
	}

	@Override
	public boolean damage(@NotNull DamageSource source, float amt) {
		if (source.isIn(DamageTypeTags.IS_FIRE))
			return false;
		if (this.isInvulnerableTo(source))
			return false;
		pop(getPos());
		kill();
		return true;
	}

	private void pop(Vec3d hit) {
		if (!stack.isEmpty()) {
			for (int i = 0; i < 7; i++) {
				Vec3d m = VecHelper.offsetRandomly(Vec3d.ZERO, this.random, .25f);
				getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, stack), hit.x, hit.y, hit.z, m.x, m.y,
					m.z);
			}
		}
		if (!getWorld().isClient)
			playHitSound(getWorld(), getPos());
	}

	private DamageSource causePotatoDamage() {
		return CreateDamageSources.potatoCannon(getWorld(), getOwner(), this);
	}

	@SuppressWarnings("unchecked")
	public static FabricEntityTypeBuilder<?> build(FabricEntityTypeBuilder<?> builder) {
//		EntityType.Builder<PotatoProjectileEntity> entityBuilder = (EntityType.Builder<PotatoProjectileEntity>) builder;
		return builder.dimensions(EntityDimensions.fixed(0.25f, 0.25f));
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		return PortingLibEntity.getEntitySpawningPacket(this);
	}

	@Override
	public void writeSpawnData(PacketByteBuf buffer) {
		NbtCompound compound = new NbtCompound();
		writeCustomDataToNbt(compound);
		buffer.writeNbt(compound);
	}

	@Override
	public void readSpawnData(PacketByteBuf additionalData) {
		readCustomDataFromNbt(additionalData.readNbt());
	}

}
