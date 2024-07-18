package com.simibubi.create.content.kinetics.fan;

import java.util.List;

import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;

@MethodsReturnNonnullByDefault
public class EncasedFanBlockEntity extends KineticBlockEntity implements IAirCurrentSource {

	public AirCurrent airCurrent;
	protected int airCurrentUpdateCooldown;
	protected int entitySearchCooldown;
	protected boolean updateAirFlow;

	public EncasedFanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		airCurrent = new AirCurrent(this);
		updateAirFlow = true;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.ENCASED_FAN, AllAdvancements.FAN_PROCESSING);
	}
	
	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		if (clientPacket)
			airCurrent.rebuild();
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		super.write(compound, clientPacket);
	}

	@Override
	public AirCurrent getAirCurrent() {
		return airCurrent;
	}

	@Nullable
	@Override
	public World getAirCurrentWorld() {
		return world;
	}

	@Override
	public BlockPos getAirCurrentPos() {
		return pos;
	}

	@Override
	public Direction getAirflowOriginSide() {
		return this.getCachedState()
			.get(EncasedFanBlock.FACING);
	}

	@Override
	public Direction getAirFlowDirection() {
		float speed = getSpeed();
		if (speed == 0)
			return null;
		Direction facing = getCachedState().get(Properties.FACING);
		speed = convertToDirection(speed, facing);
		return speed > 0 ? facing : facing.getOpposite();
	}

	@Override
	public void remove() {
		super.remove();
		updateChute();
	}
	
	@Override
	public boolean isSourceRemoved() {
		return removed;
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		updateAirFlow = true;
		updateChute();
	}

	public void updateChute() {
		Direction direction = getCachedState().get(EncasedFanBlock.FACING);
		if (!direction.getAxis()
			.isVertical())
			return;
		BlockEntity poweredChute = world.getBlockEntity(pos.offset(direction));
		if (!(poweredChute instanceof ChuteBlockEntity))
			return;
		ChuteBlockEntity chuteBE = (ChuteBlockEntity) poweredChute;
		if (direction == Direction.DOWN)
			chuteBE.updatePull();
		else
			chuteBE.updatePush(1);
	}

	public void blockInFrontChanged() {
		updateAirFlow = true;
	}

	@Override
	public void tick() {
		super.tick();

		boolean server = !world.isClient || isVirtual();

		if (server && airCurrentUpdateCooldown-- <= 0) {
			airCurrentUpdateCooldown = AllConfigs.server().kinetics.fanBlockCheckRate.get();
			updateAirFlow = true;
		}

		if (updateAirFlow) {
			updateAirFlow = false;
			airCurrent.rebuild();
			if (airCurrent.maxDistance > 0)
				award(AllAdvancements.ENCASED_FAN);
			sendData();
		}

		if (getSpeed() == 0)
			return;

		if (entitySearchCooldown-- <= 0) {
			entitySearchCooldown = 5;
			airCurrent.findEntities();
		}

		airCurrent.tick();
	}

}
