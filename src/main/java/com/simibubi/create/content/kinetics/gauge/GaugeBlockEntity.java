package com.simibubi.create.content.kinetics.gauge;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.Lang;

public abstract class GaugeBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

	public float dialTarget;
	public float dialState;
	public float prevDialState;
	public int color;

	public GaugeBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.putFloat("Value", dialTarget);
		compound.putInt("Color", color);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		dialTarget = compound.getFloat("Value");
		color = compound.getInt("Color");
		super.read(compound, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();
		prevDialState = dialState;
		dialState += (dialTarget - dialState) * .125f;
		if (dialState > 1 && world.random.nextFloat() < 1 / 2f)
			dialState -= (dialState - 1) * world.random.nextFloat();
	}

	@Override
	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		tooltip.add(componentSpacing.copyContentOnly().append(Lang.translateDirect("gui.gauge.info_header")));

		return true;
	}

}
