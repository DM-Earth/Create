package com.simibubi.create.foundation.damageTypes;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageEffects;
import net.minecraft.entity.damage.DamageRecord;
import net.minecraft.entity.damage.DamageScaling;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DeathMessageType;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;

public class DamageTypeBuilder {
	protected final RegistryKey<DamageType> key;

	protected String msgId;
	protected DamageScaling scaling;
	protected float exhaustion = 0.0f;
	protected DamageEffects effects;
	protected DeathMessageType deathMessageType;

	public DamageTypeBuilder(RegistryKey<DamageType> key) {
		this.key = key;
	}

	/**
	 * Set the message ID. this is used for death message lang keys.
	 *
	 * @see #deathMessageType(DeathMessageType)
	 */
	public DamageTypeBuilder msgId(String msgId) {
		this.msgId = msgId;
		return this;
	}

	public DamageTypeBuilder simpleMsgId() {
		return msgId(key.getValue().getNamespace() + "." + key.getValue().getPath());
	}

	/**
	 * Set the scaling of this type. This determines whether damage is increased based on difficulty or not.
	 */
	public DamageTypeBuilder scaling(DamageScaling scaling) {
		this.scaling = scaling;
		return this;
	}

	/**
	 * Set the exhaustion of this type. This is the amount of hunger that will be consumed when an entity is damaged.
	 */
	public DamageTypeBuilder exhaustion(float exhaustion) {
		this.exhaustion = exhaustion;
		return this;
	}

	/**
	 * Set the effects of this type. This determines the sound that plays when damaged.
	 */
	public DamageTypeBuilder effects(DamageEffects effects) {
		this.effects = effects;
		return this;
	}

	/**
	 * Set the death message type of this damage type. This determines how a death message lang key is assembled.
	 * <ul>
	 *     <li>{@link DeathMessageType#DEFAULT}: {@link DamageSource#getDeathMessage}</li>
	 *     <li>{@link DeathMessageType#FALL_VARIANTS}: {@link DamageTracker#getFallDeathMessage(DamageRecord, Entity)}</li>
	 *     <li>{@link DeathMessageType#INTENTIONAL_GAME_DESIGN}: "death.attack." + msgId, wrapped in brackets, linking to MCPE-28723</li>
	 * </ul>
	 */
	public DamageTypeBuilder deathMessageType(DeathMessageType deathMessageType) {
		this.deathMessageType = deathMessageType;
		return this;
	}

	public DamageType build() {
		if (msgId == null) {
			simpleMsgId();
		}
		if (scaling == null) {
			scaling(DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER);
		}
		if (effects == null) {
			effects(DamageEffects.HURT);
		}
		if (deathMessageType == null) {
			deathMessageType(DeathMessageType.DEFAULT);
		}
		return new DamageType(msgId, scaling, exhaustion, effects, deathMessageType);
	}

	public DamageType register(Registerable<DamageType> ctx) {
		DamageType type = build();
		ctx.register(key, type);
		return type;
	}
}
