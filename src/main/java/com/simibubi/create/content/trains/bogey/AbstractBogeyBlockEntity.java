package com.simibubi.create.content.trains.bogey;

import static com.simibubi.create.content.trains.entity.CarriageBogey.UPSIDE_DOWN_KEY;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public abstract class AbstractBogeyBlockEntity extends CachedRenderBBBlockEntity {
	public static final String BOGEY_STYLE_KEY = "BogeyStyle";
	public static final String BOGEY_DATA_KEY = "BogeyData";

	private NbtCompound bogeyData;

	public AbstractBogeyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract BogeyStyle getDefaultStyle();

	public NbtCompound getBogeyData() {
		if (this.bogeyData == null || !this.bogeyData.contains(BOGEY_STYLE_KEY))
			this.bogeyData = this.createBogeyData();
		return this.bogeyData;
	}

	public void setBogeyData(@NotNull NbtCompound newData) {
		if (!newData.contains(BOGEY_STYLE_KEY)) {
			Identifier style = getDefaultStyle().name;
			NBTHelper.writeResourceLocation(newData, BOGEY_STYLE_KEY, style);
		}
		this.bogeyData = newData;
	}

	public void setBogeyStyle(@NotNull BogeyStyle style) {
		Identifier location = style.name;
		NbtCompound data = this.getBogeyData();
		NBTHelper.writeResourceLocation(data, BOGEY_STYLE_KEY, location);
		markUpdated();
	}

	@NotNull
	public BogeyStyle getStyle() {
		NbtCompound data = this.getBogeyData();
		Identifier currentStyle = NBTHelper.readResourceLocation(data, BOGEY_STYLE_KEY);
		BogeyStyle style = AllBogeyStyles.BOGEY_STYLES.get(currentStyle);
		if (style == null) {
			setBogeyStyle(getDefaultStyle());
			return getStyle();
		}
		return style;
	}

	@Override
	protected void writeNbt(@NotNull NbtCompound pTag) {
		NbtCompound data = this.getBogeyData();
		if (data != null) pTag.put(BOGEY_DATA_KEY, data); // Now contains style
		super.writeNbt(pTag);
	}

	@Override
	public void readNbt(NbtCompound pTag) {
		if (pTag.contains(BOGEY_DATA_KEY))
			this.bogeyData = pTag.getCompound(BOGEY_DATA_KEY);
		else
			this.bogeyData = this.createBogeyData();
		super.readNbt(pTag);
	}

	private NbtCompound createBogeyData() {
		NbtCompound nbt = new NbtCompound();
		NBTHelper.writeResourceLocation(nbt, BOGEY_STYLE_KEY, getDefaultStyle().name);
		boolean upsideDown = false;
		if (getCachedState().getBlock() instanceof AbstractBogeyBlock<?> bogeyBlock)
			upsideDown = bogeyBlock.isUpsideDown(getCachedState());
		nbt.putBoolean(UPSIDE_DOWN_KEY, upsideDown);
		return nbt;
	}

	@Override
	protected Box createRenderBoundingBox() {
		return super.createRenderBoundingBox().expand(2);
	}

	// Ponder
	LerpedFloat virtualAnimation = LerpedFloat.angular();

	public float getVirtualAngle(float partialTicks) {
		return virtualAnimation.getValue(partialTicks);
	}

	public void animate(float distanceMoved) {
		BlockState blockState = getCachedState();
		if (!(blockState.getBlock() instanceof AbstractBogeyBlock<?> type))
			return;
		double angleDiff = 360 * distanceMoved / (Math.PI * 2 * type.getWheelRadius());
		double newWheelAngle = (virtualAnimation.getValue() - angleDiff) % 360;
		virtualAnimation.setValue(newWheelAngle);
	}

	private void markUpdated() {
		markDirty();
		World level = getWorld();
		if (level != null)
			getWorld().updateListeners(getPos(), getCachedState(), getCachedState(), 3);
	}
}
