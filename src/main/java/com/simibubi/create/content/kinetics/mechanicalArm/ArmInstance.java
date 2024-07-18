package com.simibubi.create.content.kinetics.mechanicalArm;

import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import com.google.common.collect.Lists;
import com.jozufozu.flywheel.api.InstanceData;
import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Iterate;

public class ArmInstance extends SingleRotatingInstance<ArmBlockEntity> implements DynamicInstance {

	final ModelData base;
	final ModelData lowerBody;
	final ModelData upperBody;
	ModelData claw;

	private final ArrayList<ModelData> clawGrips;

	private final ArrayList<ModelData> models;
	private final Boolean ceiling;

	private boolean firstRender = true;

	private float baseAngle = Float.NaN;
	private float lowerArmAngle = Float.NaN;
	private float upperArmAngle = Float.NaN;
	private float headAngle = Float.NaN;

	public ArmInstance(MaterialManager materialManager, ArmBlockEntity blockEntity) {
		super(materialManager, blockEntity);

		Material<ModelData> mat = getTransformMaterial();

		base = mat.getModel(AllPartialModels.ARM_BASE, blockState)
			.createInstance();
		lowerBody = mat.getModel(AllPartialModels.ARM_LOWER_BODY, blockState)
			.createInstance();
		upperBody = mat.getModel(AllPartialModels.ARM_UPPER_BODY, blockState)
			.createInstance();
		claw = mat
			.getModel(blockEntity.goggles ? AllPartialModels.ARM_CLAW_BASE_GOGGLES : AllPartialModels.ARM_CLAW_BASE,
				blockState)
			.createInstance();

		ModelData clawGrip1 = mat.getModel(AllPartialModels.ARM_CLAW_GRIP_UPPER, blockState)
			.createInstance();
		ModelData clawGrip2 = mat.getModel(AllPartialModels.ARM_CLAW_GRIP_LOWER, blockState)
			.createInstance();

		clawGrips = Lists.newArrayList(clawGrip1, clawGrip2);
		models = Lists.newArrayList(base, lowerBody, upperBody, claw, clawGrip1, clawGrip2);
		ceiling = blockState.get(ArmBlock.CEILING);

		animateArm(false);
	}

	@Override
	public void beginFrame() {
		if (blockEntity.phase == ArmBlockEntity.Phase.DANCING && blockEntity.getSpeed() != 0) {
			animateArm(true);
			firstRender = true;
			return;
		}

		float pt = AnimationTickHolder.getPartialTicks();

		float baseAngleNow = blockEntity.baseAngle.getValue(pt);
		float lowerArmAngleNow = blockEntity.lowerArmAngle.getValue(pt);
		float upperArmAngleNow = blockEntity.upperArmAngle.getValue(pt);
		float headAngleNow = blockEntity.headAngle.getValue(pt);

		boolean settled = MathHelper.approximatelyEquals(baseAngle, baseAngleNow) && MathHelper.approximatelyEquals(lowerArmAngle, lowerArmAngleNow)
			&& MathHelper.approximatelyEquals(upperArmAngle, upperArmAngleNow) && MathHelper.approximatelyEquals(headAngle, headAngleNow);

		this.baseAngle = baseAngleNow;
		this.lowerArmAngle = lowerArmAngleNow;
		this.upperArmAngle = upperArmAngleNow;
		this.headAngle = headAngleNow;

		if (!settled || firstRender)
			animateArm(false);

		if (firstRender)
			firstRender = false;
	}

	private void animateArm(boolean rave) {
		float baseAngle;
		float lowerArmAngle;
		float upperArmAngle;
		float headAngle;
		int color;

		if (rave) {
			float renderTick =
				AnimationTickHolder.getRenderTime(blockEntity.getWorld()) + (blockEntity.hashCode() % 64);
			baseAngle = (renderTick * 10) % 360;
			lowerArmAngle = MathHelper.lerp((MathHelper.sin(renderTick / 4) + 1) / 2, -45, 15);
			upperArmAngle = MathHelper.lerp((MathHelper.sin(renderTick / 8) + 1) / 4, -45, 95);
			headAngle = -lowerArmAngle;
			color = Color.rainbowColor(AnimationTickHolder.getTicks() * 100)
				.getRGB();
		} else {
			baseAngle = this.baseAngle;
			lowerArmAngle = this.lowerArmAngle - 135;
			upperArmAngle = this.upperArmAngle - 90;
			headAngle = this.headAngle;
			color = 0xFFFFFF;
		}

		MatrixStack msLocal = new MatrixStack();
		TransformStack msr = TransformStack.cast(msLocal);
		msr.translate(getInstancePosition());
		msr.centre();

		if (ceiling)
			msr.rotateX(180);

		ArmRenderer.transformBase(msr, baseAngle);
		base.setTransform(msLocal);

		ArmRenderer.transformLowerArm(msr, lowerArmAngle);
		lowerBody.setTransform(msLocal)
			.setColor(color);

		ArmRenderer.transformUpperArm(msr, upperArmAngle);
		upperBody.setTransform(msLocal)
			.setColor(color);

		ArmRenderer.transformHead(msr, headAngle);
		
		if (ceiling && blockEntity.goggles)
			msr.rotateZ(180);
		
		claw.setTransform(msLocal);
		
		if (ceiling && blockEntity.goggles)
			msr.rotateZ(180);

		ItemStack item = blockEntity.heldItem;
		ItemRenderer itemRenderer = MinecraftClient.getInstance()
			.getItemRenderer();
		boolean hasItem = !item.isEmpty();
		boolean isBlockItem = hasItem && (item.getItem() instanceof BlockItem)
			&& itemRenderer.getModel(item, MinecraftClient.getInstance().world, null, 0)
				.hasDepth();

		for (int index : Iterate.zeroAndOne) {
			msLocal.push();
			int flip = index * 2 - 1;
			ArmRenderer.transformClawHalf(msr, hasItem, isBlockItem, flip);
			clawGrips.get(index)
				.setTransform(msLocal);
			msLocal.pop();
		}
	}

	@Override
	public void update() {
		super.update();
		models.remove(claw);
		claw.delete();
		claw = getTransformMaterial()
			.getModel(blockEntity.goggles ? AllPartialModels.ARM_CLAW_BASE_GOGGLES : AllPartialModels.ARM_CLAW_BASE,
				blockState)
			.createInstance();
		models.add(claw);
		updateLight();
		animateArm(false);
	}

	@Override
	public void updateLight() {
		super.updateLight();

		relight(pos, models.stream());
	}

	@Override
	protected Instancer<RotatingData> getModel() {
		return getRotatingMaterial().getModel(AllPartialModels.ARM_COG, blockEntity.getCachedState());
	}

	@Override
	public void remove() {
		super.remove();
		models.forEach(InstanceData::delete);
	}

}