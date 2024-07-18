package com.simibubi.create.content.equipment.potatoCannon;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler;
import com.simibubi.create.foundation.particle.AirParticleData;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class PotatoCannonRenderHandler extends ShootableGadgetRenderHandler {

	private float nextPitch;

	@Override
	protected void playSound(Hand hand, Vec3d position) {
		PotatoProjectileEntity.playLaunchSound(MinecraftClient.getInstance().world, position, nextPitch);
	}

	@Override
	protected boolean appliesTo(ItemStack stack) {
		return AllItems.POTATO_CANNON.get()
			.isCannon(stack);
	}

	public void beforeShoot(float nextPitch, Vec3d location, Vec3d motion, ItemStack stack) {
		this.nextPitch = nextPitch;
		if (stack.isEmpty())
			return;
		ClientWorld world = MinecraftClient.getInstance().world;
		for (int i = 0; i < 2; i++) {
			Vec3d m = VecHelper.offsetRandomly(motion.multiply(0.1f), world.random, .025f);
			world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, stack), location.x, location.y, location.z, m.x,
				m.y, m.z);

			Vec3d m2 = VecHelper.offsetRandomly(motion.multiply(2f), world.random, .5f);
			world.addParticle(new AirParticleData(1, 1 / 4f), location.x, location.y, location.z, m2.x, m2.y, m2.z);
		}
	}

	@Override
	protected void transformTool(MatrixStack ms, float flip, float equipProgress, float recoil, float pt) {
		ms.translate(flip * -.1f, 0, .14f);
		ms.scale(.75f, .75f, .75f);
		TransformStack.cast(ms)
			.rotateX(recoil * 80);
	}

	@Override
	protected void transformHand(MatrixStack ms, float flip, float equipProgress, float recoil, float pt) {
		ms.translate(flip * -.09, -.275, -.25);
		TransformStack.cast(ms)
			.rotateZ(flip * -10);
	}

}
