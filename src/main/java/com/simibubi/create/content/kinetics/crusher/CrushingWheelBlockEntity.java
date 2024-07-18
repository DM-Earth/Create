package com.simibubi.create.content.kinetics.crusher;

import java.util.Collection;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Iterate;

public class CrushingWheelBlockEntity extends KineticBlockEntity {
	public CrushingWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(20);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.CRUSHING_WHEEL, AllAdvancements.CRUSHER_MAXED);
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		fixControllers();
	}

	public void fixControllers() {
		for (Direction d : Iterate.directions)
			((CrushingWheelBlock) getCachedState().getBlock()).updateControllers(getCachedState(), getWorld(), getPos(),
					d);
	}

	@Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).expand(1);
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		fixControllers();
	}

	public static int crushingIsFortunate(DamageSource source, LivingEntity target, int currentLevel, boolean recentlyHit) {
		if (source == null || !source.isOf(AllDamageTypes.CRUSH))
			return 0;
		return 2;		//This does not currently increase mob drops. It seems like this only works for damage done by an entity.
	}

	public static boolean handleCrushedMobDrops(LivingEntity target, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit) {
		if (source == null || !source.isOf(AllDamageTypes.CRUSH))
			return false;
		Vec3d outSpeed = Vec3d.ZERO;
		for (ItemEntity outputItem : drops) {
			outputItem.setVelocity(outSpeed);
		}
		return false;
	}

}
