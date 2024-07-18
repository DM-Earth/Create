package com.simibubi.create.content.fluids.drain;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class ItemDrainRenderer extends SmartBlockEntityRenderer<ItemDrainBlockEntity> {

	public ItemDrainRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ItemDrainBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		renderFluid(be, partialTicks, ms, buffer, light);
		renderItem(be, partialTicks, ms, buffer, light, overlay);
	}

	protected void renderItem(ItemDrainBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		TransportedItemStack transported = be.heldItem;
		if (transported == null)
			return;

		TransformStack msr = TransformStack.cast(ms);
		Vec3d itemPosition = VecHelper.getCenterOf(be.getPos());

		Direction insertedFrom = transported.insertedFrom;
		if (!insertedFrom.getAxis()
			.isHorizontal())
			return;

		ms.push();
		ms.translate(.5f, 15 / 16f, .5f);
		msr.nudge(0);
		float offset = MathHelper.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
		float sideOffset = MathHelper.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);

		Vec3d offsetVec = Vec3d.of(insertedFrom.getOpposite()
			.getVector())
			.multiply(.5f - offset);
		ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
		boolean alongX = insertedFrom.rotateYClockwise()
			.getAxis() == Direction.Axis.X;
		if (!alongX)
			sideOffset *= -1;
		ms.translate(alongX ? sideOffset : 0, 0, alongX ? 0 : sideOffset);

		ItemStack itemStack = transported.stack;
		Random r = Random.create(0);
		ItemRenderer itemRenderer = MinecraftClient.getInstance()
			.getItemRenderer();
		int count = (int) (MathHelper.floorLog2((int) (itemStack.getCount()))) / 2;
		boolean renderUpright = BeltHelper.isItemUpright(itemStack);
		boolean blockItem = itemRenderer.getModel(itemStack, null, null, 0)
			.hasDepth();

		if (renderUpright)
			ms.translate(0, 3 / 32d, 0);

		int positive = insertedFrom.getDirection()
			.offset();
		float verticalAngle = positive * offset * 360;
		if (insertedFrom.getAxis() != Direction.Axis.X)
			msr.rotateX(verticalAngle);
		if (insertedFrom.getAxis() != Direction.Axis.Z)
			msr.rotateZ(-verticalAngle);

		if (renderUpright) {
			Entity renderViewEntity = MinecraftClient.getInstance().cameraEntity;
			if (renderViewEntity != null) {
				Vec3d positionVec = renderViewEntity.getPos();
				Vec3d vectorForOffset = itemPosition.add(offsetVec);
				Vec3d diff = vectorForOffset.subtract(positionVec);

				if (insertedFrom.getAxis() != Direction.Axis.X)
					diff = VecHelper.rotate(diff, verticalAngle, Direction.Axis.X);
				if (insertedFrom.getAxis() != Direction.Axis.Z)
					diff = VecHelper.rotate(diff, -verticalAngle, Direction.Axis.Z);

				float yRot = (float) MathHelper.atan2(diff.z, -diff.x);
				ms.multiply(RotationAxis.POSITIVE_Y.rotation((float) (yRot - Math.PI / 2)));
			}
			ms.translate(0, 0, -1 / 16f);
		}

		for (int i = 0; i <= count; i++) {
			ms.push();
			if (blockItem)
				ms.translate(r.nextFloat() * .0625f * i, 0, r.nextFloat() * .0625f * i);
			ms.scale(.5f, .5f, .5f);
			if (!blockItem && !renderUpright)
				msr.rotateX(90);
			itemRenderer.renderItem(itemStack, ModelTransformationMode.FIXED, light, overlay, ms, buffer, be.getWorld(), 0);
			ms.pop();

			if (!renderUpright) {
				if (!blockItem)
					msr.rotateY(10);
				ms.translate(0, blockItem ? 1 / 64d : 1 / 16d, 0);
			} else
				ms.translate(0, 0, -1 / 16f);
		}

		ms.pop();
	}

	protected void renderFluid(ItemDrainBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light) {
		SmartFluidTankBehaviour tank = be.internalTank;
		if (tank == null)
			return;

		TankSegment primaryTank = tank.getPrimaryTank();
		FluidStack fluidStack = primaryTank.getRenderedFluid();
		float level = primaryTank.getFluidLevel()
			.getValue(partialTicks);

		if (!fluidStack.isEmpty() && level != 0) {
			float yMin = 5f / 16f;
			float min = 2f / 16f;
			float max = min + (12 / 16f);
			float yOffset = (7 / 16f) * level;
			ms.push();
			ms.translate(0, yOffset, 0);
			FluidRenderer.renderFluidBox(fluidStack, min, yMin - yOffset, min, max, yMin, max, buffer, ms, light,
				false);
			ms.pop();
		}

		ItemStack heldItemStack = be.getHeldItemStack();
		if (heldItemStack.isEmpty())
			return;
		FluidStack fluidStack2 = GenericItemEmptying.emptyItem(be.getWorld(), heldItemStack, true)
			.getFirst();
		if (fluidStack2.isEmpty()) {
			if (fluidStack.isEmpty())
				return;
			fluidStack2 = fluidStack;
		}

		int processingTicks = be.processingTicks;
		float processingPT = be.processingTicks - partialTicks;
		float processingProgress = 1 - (processingPT - 5) / 10;
		processingProgress = MathHelper.clamp(processingProgress, 0, 1);
		float radius = 0;

		if (processingTicks != -1) {
			radius = (float) (Math.pow(((2 * processingProgress) - 1), 2) - 1);
			Box bb = new Box(0.5, 1.0, 0.5, 0.5, 0.25, 0.5).expand(radius / 32f);
			FluidRenderer.renderFluidBox(fluidStack2, (float) bb.minX, (float) bb.minY, (float) bb.minZ,
				(float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, buffer, ms, light, true);
		}

	}

}
