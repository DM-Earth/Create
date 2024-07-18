package com.simibubi.create.content.trains.schedule;

import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.AxolotlEntityModel;
import net.minecraft.client.render.entity.model.BeeEntityModel;
import net.minecraft.client.render.entity.model.BlazeEntityModel;
import net.minecraft.client.render.entity.model.ChickenEntityModel;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.FoxEntityModel;
import net.minecraft.client.render.entity.model.FrogEntityModel;
import net.minecraft.client.render.entity.model.GuardianEntityModel;
import net.minecraft.client.render.entity.model.HoglinEntityModel;
import net.minecraft.client.render.entity.model.IronGolemEntityModel;
import net.minecraft.client.render.entity.model.MagmaCubeEntityModel;
import net.minecraft.client.render.entity.model.OcelotEntityModel;
import net.minecraft.client.render.entity.model.PandaEntityModel;
import net.minecraft.client.render.entity.model.ParrotEntityModel;
import net.minecraft.client.render.entity.model.PigEntityModel;
import net.minecraft.client.render.entity.model.QuadrupedEntityModel;
import net.minecraft.client.render.entity.model.SheepEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.entity.model.SlimeEntityModel;
import net.minecraft.client.render.entity.model.SnowGolemEntityModel;
import net.minecraft.client.render.entity.model.SpiderEntityModel;
import net.minecraft.client.render.entity.model.WardenEntityModel;
import net.minecraft.client.render.entity.model.WolfEntityModel;
import net.minecraft.util.math.Vec3d;

public class TrainHatOffsets {

	// sorry
	public static Vec3d getOffset(EntityModel<?> model) {

		float x = 0;
		float y = 0;
		float z = 0;
		
		if (model instanceof AnimalModel) {
			if (model instanceof WolfEntityModel) {
				x += .5f;
				y += 1.5f;
				z += .25f;
			} else if (model instanceof OcelotEntityModel) {
				y += 1f;
				z -= .25f;
			} else if (model instanceof ChickenEntityModel) {
				z -= .25f;
			} else if (model instanceof FoxEntityModel) {
				x += .5f;
				y += 2f;
				z -= 1f;
			} else if (model instanceof QuadrupedEntityModel) {
				y += 2f;

				if (model instanceof CowEntityModel)
					z -= 1.25f;
				else if (model instanceof PandaEntityModel)
					z += .5f;
				else if (model instanceof PigEntityModel)
					z -= 2f;
				else if (model instanceof SheepEntityModel) {
					z -= .75f;
					y -= 1.5f;

				}
			} else if (model instanceof HoglinEntityModel)
				z -= 4.5f;
			else if (model instanceof BeeEntityModel) {
				z -= .75f;
				y -= 4f;
			} else if (model instanceof AxolotlEntityModel) {
				z -= 5f;
				y += .5f;
			}
		}
		
		if (model instanceof SinglePartEntityModel) {
			if (model instanceof BlazeEntityModel)
				y += 4;
			else if (model instanceof GuardianEntityModel)
				y += 20;
			else if (model instanceof IronGolemEntityModel) {
				z -= 1.5f;
				y -= 2f;
			} else if (model instanceof SnowGolemEntityModel) {
				z -= .75f;
				y -= 3f;
			} else if (model instanceof SlimeEntityModel || model instanceof MagmaCubeEntityModel) {
				y += 22;
			} else if (model instanceof SpiderEntityModel) {
				z -= 3.5f;
				y += 2f;
			} else if (model instanceof ParrotEntityModel) {
				z -= 1.5f;
			} else if (model instanceof WardenEntityModel) {
				y += 3.5f;
				z += .5f;
			} else if (model instanceof FrogEntityModel) {
				y += 16.75f;
				z -= .25f;
			}
		}

		return new Vec3d(x, y, z);

	}

}
