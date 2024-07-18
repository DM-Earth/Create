package com.simibubi.create.content.equipment.armor;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.particle.AirParticleData;
import com.simibubi.create.foundation.utility.VecHelper;

public class BacktankBlockEntity extends KineticBlockEntity implements Nameable {

	public int airLevel;
	public int airLevelTimer;
	private Text defaultName;
	private Text customName;

	private int capacityEnchantLevel;
	private NbtList enchantmentTag;

	public BacktankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		defaultName = getDefaultName(state);
		enchantmentTag = new NbtList();
	}

	public static Text getDefaultName(BlockState state) {
		if (AllBlocks.NETHERITE_BACKTANK.has(state)) {
			AllItems.NETHERITE_BACKTANK.get().getName();
		}

		return AllItems.COPPER_BACKTANK.get().getName();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.BACKTANK);
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (getSpeed() != 0)
			award(AllAdvancements.BACKTANK);
	}

	@Override
	public void tick() {
		super.tick();
		if (getSpeed() == 0)
			return;

		BlockState state = getCachedState();
		BooleanProperty waterProperty = Properties.WATERLOGGED;
		if (state.contains(waterProperty) && state.get(waterProperty))
			return;

		if (airLevelTimer > 0) {
			airLevelTimer--;
			return;
		}

		int max = BacktankUtil.maxAir(capacityEnchantLevel);
		if (world.isClient) {
			Vec3d centerOf = VecHelper.getCenterOf(pos);
			Vec3d v = VecHelper.offsetRandomly(centerOf, world.random, .65f);
			Vec3d m = centerOf.subtract(v);
			if (airLevel != max)
				world.addParticle(new AirParticleData(1, .05f), v.x, v.y, v.z, m.x, m.y, m.z);
			return;
		}

		if (airLevel == max)
			return;

		int prevComparatorLevel = getComparatorOutput();
		float abs = Math.abs(getSpeed());
		int increment = MathHelper.clamp(((int) abs - 100) / 20, 1, 5);
		airLevel = Math.min(max, airLevel + increment);
		if (getComparatorOutput() != prevComparatorLevel && !world.isClient)
			world.updateComparators(pos, state.getBlock());
		if (airLevel == max)
			sendData();
		airLevelTimer = MathHelper.clamp((int) (128f - abs / 5f) - 108, 0, 20);
	}

	public int getComparatorOutput() {
		int max = BacktankUtil.maxAir(capacityEnchantLevel);
		return ComparatorUtil.fractionToRedstoneLevel(airLevel / (float) max);
	}

	@Override
	protected void write(NbtCompound compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.putInt("Air", airLevel);
		compound.putInt("Timer", airLevelTimer);
		compound.putInt("CapacityEnchantment", capacityEnchantLevel);
		if (this.customName != null)
			compound.putString("CustomName", Text.Serializer.toJson(this.customName));
		compound.put("Enchantments", enchantmentTag);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		int prev = airLevel;
		capacityEnchantLevel = compound.getInt("CapacityEnchantment");
		airLevel = compound.getInt("Air");
		airLevelTimer = compound.getInt("Timer");
		enchantmentTag = compound.getList("Enchantments", NbtElement.COMPOUND_TYPE);
		if (compound.contains("CustomName", 8))
			this.customName = Text.Serializer.fromJson(compound.getString("CustomName"));
		if (prev != 0 && prev != airLevel && airLevel == BacktankUtil.maxAir(capacityEnchantLevel) && clientPacket)
			playFilledEffect();
	}

	protected void playFilledEffect() {
		AllSoundEvents.CONFIRM.playAt(world, pos, 0.4f, 1, true);
		Vec3d baseMotion = new Vec3d(.25, 0.1, 0);
		Vec3d baseVec = VecHelper.getCenterOf(pos);
		for (int i = 0; i < 360; i += 10) {
			Vec3d m = VecHelper.rotate(baseMotion, i, Axis.Y);
			Vec3d v = baseVec.add(m.normalize()
				.multiply(.25f));

			world.addParticle(ParticleTypes.SPIT, v.x, v.y, v.z, m.x, m.y, m.z);
		}
	}

	@Override
	public Text getName() {
		return this.customName != null ? this.customName
			: defaultName;
	}

	public int getAirLevel() {
		return airLevel;
	}

	public void setAirLevel(int airLevel) {
		this.airLevel = airLevel;
		sendData();
	}

	public void setCustomName(Text customName) {
		this.customName = customName;
	}

	public Text getCustomName() {
		return customName;
	}

	public NbtList getEnchantmentTag() {
		return enchantmentTag;
	}

	public void setEnchantmentTag(NbtList enchantmentTag) {
		this.enchantmentTag = enchantmentTag;
	}

	public void setCapacityEnchantLevel(int capacityEnchantLevel) {
		this.capacityEnchantLevel = capacityEnchantLevel;
	}

}
