package com.simibubi.create.foundation.ponder.element;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MinecartElement extends AnimatedSceneElement {

	private Vec3d location;
	private LerpedFloat rotation;
	private AbstractMinecartEntity entity;
	private MinecartConstructor constructor;
	private float initialRotation;

	public interface MinecartConstructor {
		AbstractMinecartEntity create(World w, double x, double y, double z);
	}

	public MinecartElement(Vec3d location, float rotation, MinecartConstructor constructor) {
		initialRotation = rotation;
		this.location = location.add(0, 1 / 16f, 0);
		this.constructor = constructor;
		this.rotation = LerpedFloat.angular()
			.startWithValue(rotation);
	}

	@Override
	public void reset(PonderScene scene) {
		super.reset(scene);
		entity.setPos(0, 0, 0);
		entity.prevX = 0;
		entity.prevY = 0;
		entity.prevZ = 0;
		entity.lastRenderX = 0;
		entity.lastRenderY = 0;
		entity.lastRenderZ = 0;
		rotation.startWithValue(initialRotation);
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		if (entity == null)
			entity = constructor.create(scene.getWorld(), 0, 0, 0);

		entity.age++;
		entity.setOnGround(true);
		entity.prevX = entity.getX();
		entity.prevY = entity.getY();
		entity.prevZ = entity.getZ();
		entity.lastRenderX = entity.getX();
		entity.lastRenderY = entity.getY();
		entity.lastRenderZ = entity.getZ();
	}

	public void setPositionOffset(Vec3d position, boolean immediate) {
		if (entity == null)
			return;
		entity.setPosition(position.x, position.y, position.z);
		if (!immediate)
			return;
		entity.prevX = position.x;
		entity.prevY = position.y;
		entity.prevZ = position.z;
	}

	public void setRotation(float angle, boolean immediate) {
		if (entity == null)
			return;
		rotation.setValue(angle);
		if (!immediate)
			return;
		rotation.startWithValue(angle);
	}

	public Vec3d getPositionOffset() {
		return entity != null ? entity.getPos() : Vec3d.ZERO;
	}

	public Vec3d getRotation() {
		return new Vec3d(0, rotation.getValue(), 0);
	}

	@Override
	protected void renderLast(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float fade, float pt) {
		EntityRenderDispatcher entityrenderermanager = MinecraftClient.getInstance()
			.getEntityRenderDispatcher();
		if (entity == null)
			entity = constructor.create(world, 0, 0, 0);

		ms.push();
		ms.translate(location.x, location.y, location.z);
		ms.translate(MathHelper.lerp(pt, entity.prevX, entity.getX()),
			MathHelper.lerp(pt, entity.prevY, entity.getY()), MathHelper.lerp(pt, entity.prevZ, entity.getZ()));

		TransformStack.cast(ms)
			.rotateY(rotation.getValue(pt));

		entityrenderermanager.render(entity, 0, 0, 0, 0, pt, ms, buffer, lightCoordsFromFade(fade));
		ms.pop();
	}

}
