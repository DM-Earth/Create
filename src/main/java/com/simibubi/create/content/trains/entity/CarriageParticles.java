package com.simibubi.create.content.trains.entity;

import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class CarriageParticles {

	CarriageContraptionEntity entity;
	boolean arrived;
	int depressurise;

	double prevMotion;
	LerpedFloat brakes;

	public CarriageParticles(CarriageContraptionEntity entity) {
		this.entity = entity;
		arrived = true;
		depressurise = 0;
		prevMotion = 0;
		brakes = LerpedFloat.linear();
	}

	public void tick(DimensionalCarriageEntity dce) {
		MinecraftClient mc = MinecraftClient.getInstance();
		Entity camEntity = mc.cameraEntity;
		if (camEntity == null)
			return;
		Vec3d leadingAnchor = dce.leadingAnchor();
		if (leadingAnchor == null || !leadingAnchor.isInRange(camEntity.getPos(), 64))
			return;

		Random r = entity.getWorld().random;
		Vec3d contraptionMotion = entity.getPos()
			.subtract(entity.getPrevPositionVec());
		double length = contraptionMotion.length();
		if (arrived && length > 0.01f)
			arrived = false;
		arrived |= entity.isStalled();

		boolean stopped = length < .002f;
		if (stopped) {
			if (!arrived) {
				arrived = true;
				depressurise = (int) (20 * entity.getCarriage().train.accumulatedSteamRelease / 10f);
			}
		} else
			depressurise = 0;

		if (depressurise > 0)
			depressurise--;

		brakes.chase(prevMotion > length + length / 512f ? 1 : 0, .25f, Chaser.exp(.625f));
		brakes.tickChaser();
		prevMotion = length;

		World level = entity.getWorld();
		Vec3d position = entity.getLerpedPos(0);
		float viewYRot = entity.getYaw(0);
		float viewXRot = entity.getPitch(0);
		int bogeySpacing = entity.getCarriage().bogeySpacing;

		for (CarriageBogey bogey : entity.getCarriage().bogeys) {
			if (bogey == null)
				continue;

			boolean spark = depressurise == 0 || depressurise > 10;

			float cutoff = length < 1 / 8f ? 0 : 1 / 8f;

			if (length > 1 / 6f)
				cutoff = Math.max(cutoff, brakes.getValue() * 1.15f);

			for (int j : Iterate.positiveAndNegative) {
				if (r.nextFloat() > cutoff && (spark || r.nextInt(4) == 0))
					continue;
				for (int i : Iterate.positiveAndNegative) {
					if (r.nextFloat() > cutoff && (spark || r.nextInt(4) == 0))
						continue;

					Vec3d v = Vec3d.ZERO.add(j * 1.15, spark ? -.6f : .32, i);
					Vec3d m = Vec3d.ZERO.add(j * (spark ? .5 : .25), spark ? .49 : -.29, 0);

					m = VecHelper.rotate(m, bogey.pitch.getValue(0), Axis.X);
					m = VecHelper.rotate(m, bogey.yaw.getValue(0), Axis.Y);

					v = VecHelper.rotate(v, bogey.pitch.getValue(0), Axis.X);
					v = VecHelper.rotate(v, bogey.yaw.getValue(0), Axis.Y);

					v = VecHelper.rotate(v, -viewYRot - 90, Axis.Y);
					v = VecHelper.rotate(v, viewXRot, Axis.X);
					v = VecHelper.rotate(v, -180, Axis.Y);

					v = v.add(0, 0, bogey.isLeading ? 0 : -bogeySpacing);
					v = VecHelper.rotate(v, 180, Axis.Y);
					v = VecHelper.rotate(v, -viewXRot, Axis.X);
					v = VecHelper.rotate(v, viewYRot + 90, Axis.Y);
					v = v.add(position);

					m = m.add(contraptionMotion.multiply(.75f));

					level.addParticle(spark ? bogey.getStyle().contactParticle : bogey.getStyle().smokeParticle, v.x, v.y, v.z, m.x, m.y, m.z);
				}
			}
		}

	}

}
