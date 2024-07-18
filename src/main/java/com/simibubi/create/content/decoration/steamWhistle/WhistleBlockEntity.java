package com.simibubi.create.content.decoration.steamWhistle;

import java.lang.ref.WeakReference;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock.WhistleExtenderShape;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticleData;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import com.tterrag.registrate.fabric.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class WhistleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	public WeakReference<FluidTankBlockEntity> source;
	public LerpedFloat animation;
	protected int pitch;

	public WhistleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		source = new WeakReference<>(null);
		animation = LerpedFloat.linear();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		registerAwardables(behaviours, AllAdvancements.STEAM_WHISTLE);
	}

	public void updatePitch() {
		BlockPos currentPos = pos.up();
		int newPitch;
		for (newPitch = 0; newPitch <= 24; newPitch += 2) {
			BlockState blockState = world.getBlockState(currentPos);
			if (!AllBlocks.STEAM_WHISTLE_EXTENSION.has(blockState))
				break;
			if (blockState.get(WhistleExtenderBlock.SHAPE) == WhistleExtenderShape.SINGLE) {
				newPitch++;
				break;
			}
			currentPos = currentPos.up();
		}
		if (pitch == newPitch)
			return;
		pitch = newPitch;

		notifyUpdate();

		FluidTankBlockEntity tank = getTank();
		if (tank != null && tank.boiler != null)
			tank.boiler.checkPipeOrganAdvancement(tank);
	}

	@Override
	public void tick() {
		super.tick();
		if (!world.isClient()) {
			if (isPowered())
				award(AllAdvancements.STEAM_WHISTLE);
			return;
		}

		FluidTankBlockEntity tank = getTank();
		boolean powered = isPowered()
			&& (tank != null && tank.boiler.isActive() && (tank.boiler.passiveHeat || tank.boiler.activeHeat > 0)
				|| isVirtual());
		animation.chase(powered ? 1 : 0, powered ? .5f : .4f, powered ? Chaser.EXP : Chaser.LINEAR);
		animation.tickChaser();
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> this.tickAudio(getOctave(), powered));
	}

	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		tag.putInt("Pitch", pitch);
		super.write(tag, clientPacket);
	}

	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		pitch = tag.getInt("Pitch");
		super.read(tag, clientPacket);
	}

	@Override
	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		String[] pitches = Lang.translateDirect("generic.notes")
			.getString()
			.split(";");
		MutableText textComponent = Components.literal(spacing);
		tooltip.add(textComponent.append(Lang.translateDirect("generic.pitch", pitches[pitch % pitches.length])));
		return true;
	}

	protected boolean isPowered() {
		return getCachedState().getOrEmpty(WhistleBlock.POWERED)
			.orElse(false);
	}

	protected WhistleSize getOctave() {
		return getCachedState().getOrEmpty(WhistleBlock.SIZE)
			.orElse(WhistleSize.MEDIUM);
	}

	@Environment(EnvType.CLIENT)
	protected WhistleSoundInstance soundInstance;

	@Environment(EnvType.CLIENT)
	protected void tickAudio(WhistleSize size, boolean powered) {
		if (!powered) {
			if (soundInstance != null) {
				soundInstance.fadeOut();
				soundInstance = null;
			}
			return;
		}

		float f = (float) Math.pow(2, -pitch / 12.0);
		boolean particle = world.getTime() % 8 == 0;
		Vec3d eyePosition = MinecraftClient.getInstance().cameraEntity.getEyePos();
		float maxVolume = (float) MathHelper.clamp((64 - eyePosition.distanceTo(Vec3d.ofCenter(pos))) / 64, 0, 1);

		if (soundInstance == null || soundInstance.isDone() || soundInstance.getOctave() != size) {
			MinecraftClient.getInstance()
				.getSoundManager()
				.play(soundInstance = new WhistleSoundInstance(size, pos));
			AllSoundEvents.WHISTLE_CHIFF.playAt(world, pos, maxVolume * .175f,
				size == WhistleSize.SMALL ? f + .75f : f, false);
			particle = true;
		}

		soundInstance.keepAlive();
		soundInstance.setPitch(f);

		if (!particle)
			return;

		Direction facing = getCachedState().getOrEmpty(WhistleBlock.FACING)
			.orElse(Direction.SOUTH);
		float angle = 180 + AngleHelper.horizontalAngle(facing);
		Vec3d sizeOffset = VecHelper.rotate(new Vec3d(0, -0.4f, 1 / 16f * size.ordinal()), angle, Axis.Y);
		Vec3d offset = VecHelper.rotate(new Vec3d(0, 1, 0.75f), angle, Axis.Y);
		Vec3d v = offset.multiply(.45f)
			.add(sizeOffset)
			.add(Vec3d.ofCenter(pos));
		Vec3d m = offset.subtract(Vec3d.of(facing.getVector())
			.multiply(.75f));
		world.addParticle(new SteamJetParticleData(1), v.x, v.y, v.z, m.x, m.y, m.z);
	}

	public int getPitchId() {
		return pitch + 100 * getCachedState().getOrEmpty(WhistleBlock.SIZE)
			.orElse(WhistleSize.MEDIUM)
			.ordinal();
	}

	public FluidTankBlockEntity getTank() {
		FluidTankBlockEntity tank = source.get();
		if (tank == null || tank.isRemoved()) {
			if (tank != null)
				source = new WeakReference<>(null);
			Direction facing = WhistleBlock.getAttachedDirection(getCachedState());
			BlockEntity be = world.getBlockEntity(pos.offset(facing));
			if (be instanceof FluidTankBlockEntity tankBe)
				source = new WeakReference<>(tank = tankBe);
		}
		if (tank == null)
			return null;
		return tank.getControllerBE();
	}

}
