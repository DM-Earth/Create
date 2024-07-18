package com.simibubi.create.foundation.blockEntity.behaviour;

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public interface ValueSettingsBehaviour extends ClipboardCloneable {

	public static record ValueSettings(int row, int value) {

		public MutableText format() {
			return Lang.number(value)
				.component();
		}

	};

	public boolean testHit(Vec3d hit);

	public boolean isActive();

	default boolean onlyVisibleWithWrench() {
		return false;
	}

	default void newSettingHovered(ValueSettings valueSetting) {}

	public ValueBoxTransform getSlotPositioning();

	public ValueSettingsBoard createBoard(PlayerEntity player, BlockHitResult hitResult);

	public void setValueSettings(PlayerEntity player, ValueSettings valueSetting, boolean ctrlDown);

	public ValueSettings getValueSettings();

	default boolean acceptsValueSettings() {
		return true;
	}

	@Override
	default String getClipboardKey() {
		return "Settings";
	}

	@Override
	default boolean writeToClipboard(NbtCompound tag, Direction side) {
		if (!acceptsValueSettings())
			return false;
		ValueSettings valueSettings = getValueSettings();
		tag.putInt("Value", valueSettings.value());
		tag.putInt("Row", valueSettings.row());
		return true;
	}

	@Override
	default boolean readFromClipboard(NbtCompound tag, PlayerEntity player, Direction side, boolean simulate) {
		if (!acceptsValueSettings())
			return false;
		if (!tag.contains("Value") || !tag.contains("Row"))
			return false;
		if (simulate)
			return true;
		setValueSettings(player, new ValueSettings(tag.getInt("Row"), tag.getInt("Value")), false);
		return true;
	}

	default void playFeedbackSound(BlockEntityBehaviour origin) {
		origin.getWorld()
			.playSound(null, origin.getPos(), SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 0.25f, 2f);
		origin.getWorld()
			.playSound(null, origin.getPos(), SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value(), SoundCategory.BLOCKS, 0.03f,
				1.125f);
	}

	default void onShortInteract(PlayerEntity player, Hand hand, Direction side) {}

}
