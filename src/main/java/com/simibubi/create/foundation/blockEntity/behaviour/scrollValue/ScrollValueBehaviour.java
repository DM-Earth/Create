package com.simibubi.create.foundation.blockEntity.behaviour.scrollValue;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ScrollValueBehaviour extends BlockEntityBehaviour implements ValueSettingsBehaviour {

	public static final BehaviourType<ScrollValueBehaviour> TYPE = new BehaviourType<>();

	ValueBoxTransform slotPositioning;
	Vec3d textShift;

	int min = 0;
	protected int max = 1;
	public int value;
	public Text label;
	Consumer<Integer> callback;
	Consumer<Integer> clientCallback;
	Function<Integer, String> formatter;
	private Supplier<Boolean> isActive;
	boolean needsWrench;

	public ScrollValueBehaviour(Text label, SmartBlockEntity be, ValueBoxTransform slot) {
		super(be);
		this.setLabel(label);
		slotPositioning = slot;
		callback = i -> {
		};
		clientCallback = i -> {
		};
		formatter = i -> Integer.toString(i);
		value = 0;
		isActive = () -> true;
	}

	@Override
	public boolean isSafeNBT() {
		return true;
	}

	@Override
	public void write(NbtCompound nbt, boolean clientPacket) {
		nbt.putInt("ScrollValue", value);
		super.write(nbt, clientPacket);
	}

	@Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		value = nbt.getInt("ScrollValue");
		super.read(nbt, clientPacket);
	}

	public ScrollValueBehaviour withClientCallback(Consumer<Integer> valueCallback) {
		clientCallback = valueCallback;
		return this;
	}

	public ScrollValueBehaviour withCallback(Consumer<Integer> valueCallback) {
		callback = valueCallback;
		return this;
	}

	public ScrollValueBehaviour between(int min, int max) {
		this.min = min;
		this.max = max;
		return this;
	}

	public ScrollValueBehaviour requiresWrench() {
		this.needsWrench = true;
		return this;
	}

	public ScrollValueBehaviour withFormatter(Function<Integer, String> formatter) {
		this.formatter = formatter;
		return this;
	}

	public ScrollValueBehaviour onlyActiveWhen(Supplier<Boolean> condition) {
		isActive = condition;
		return this;
	}

	public void setValue(int value) {
		value = MathHelper.clamp(value, min, max);
		if (value == this.value)
			return;
		this.value = value;
		callback.accept(value);
		blockEntity.markDirty();
		blockEntity.sendData();
	}

	public int getValue() {
		return value;
	}

	public String formatValue() {
		return formatter.apply(value);
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	@Override
	public boolean isActive() {
		return isActive.get();
	}

	@Override
	public boolean testHit(Vec3d hit) {
		BlockState state = blockEntity.getCachedState();
		Vec3d localHit = hit.subtract(Vec3d.of(blockEntity.getPos()));
		return slotPositioning.testHit(state, localHit);
	}

	public void setLabel(Text label) {
		this.label = label;
	}

	public static class StepContext {
		public int currentValue;
		public boolean forward;
		public boolean shift;
		public boolean control;
	}

	@Override
	public ValueBoxTransform getSlotPositioning() {
		return slotPositioning;
	}

	@Override
	public ValueSettingsBoard createBoard(PlayerEntity player, BlockHitResult hitResult) {
		return new ValueSettingsBoard(label, max, 10, ImmutableList.of(Components.literal("Value")),
			new ValueSettingsFormatter(ValueSettings::format));
	}

	@Override
	public void setValueSettings(PlayerEntity player, ValueSettings valueSetting, boolean ctrlDown) {
		if (valueSetting.equals(getValueSettings()))
			return;
		setValue(valueSetting.value());
		playFeedbackSound(this);
	}

	@Override
	public ValueSettings getValueSettings() {
		return new ValueSettings(0, value);
	}

	@Override
	public boolean onlyVisibleWithWrench() {
		return needsWrench;
	}

	@Override
	public void onShortInteract(PlayerEntity player, Hand hand, Direction side) {
		if (player instanceof FakePlayer)
			blockEntity.getCachedState()
				.onUse(getWorld(), player, hand,
					new BlockHitResult(VecHelper.getCenterOf(getPos()), side, getPos(), true));
	}

}
