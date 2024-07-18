package com.simibubi.create.content.kinetics.belt;

import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.ShadowRenderHelper;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.worldWrappers.WrappedWorld;

import io.github.foundationgames.sandwichable.items.ItemsRegistry;

public class BeltRenderer extends SafeBlockEntityRenderer<BeltBlockEntity> {

	public BeltRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public boolean rendersOutsideBoundingBox(BeltBlockEntity be) {
		return be.isController();
	}

	@Override
	protected void renderSafe(BeltBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {

		if (!Backend.canUseInstancing(be.getWorld())) {

			BlockState blockState = be.getCachedState();
			if (!AllBlocks.BELT.has(blockState)) return;

			BeltSlope beltSlope = blockState.get(BeltBlock.SLOPE);
			BeltPart part = blockState.get(BeltBlock.PART);
			Direction facing = blockState.get(BeltBlock.HORIZONTAL_FACING);
			AxisDirection axisDirection = facing.getDirection();

			boolean downward = beltSlope == BeltSlope.DOWNWARD;
			boolean upward = beltSlope == BeltSlope.UPWARD;
			boolean diagonal = downward || upward;
			boolean start = part == BeltPart.START;
			boolean end = part == BeltPart.END;
			boolean sideways = beltSlope == BeltSlope.SIDEWAYS;
			boolean alongX = facing.getAxis() == Direction.Axis.X;

			MatrixStack localTransforms = new MatrixStack();
            TransformStack msr = TransformStack.cast(localTransforms);
			VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());
			float renderTick = AnimationTickHolder.getRenderTime(be.getWorld());

			msr.centre()
					.rotateY(AngleHelper.horizontalAngle(facing) + (upward ? 180 : 0) + (sideways ? 270 : 0))
					.rotateZ(sideways ? 90 : 0)
					.rotateX(!diagonal && beltSlope != BeltSlope.HORIZONTAL ? 90 : 0)
					.unCentre();

			if (downward || beltSlope == BeltSlope.VERTICAL && axisDirection == AxisDirection.POSITIVE) {
				boolean b = start;
				start = end;
				end = b;
			}

			DyeColor color = be.color.orElse(null);

			for (boolean bottom : Iterate.trueAndFalse) {

				PartialModel beltPartial = getBeltPartial(diagonal, start, end, bottom);

				SuperByteBuffer beltBuffer = CachedBufferer.partial(beltPartial, blockState)
						.light(light);

				SpriteShiftEntry spriteShift = getSpriteShiftEntry(color, diagonal, bottom);

				// UV shift
				float speed = be.getSpeed();
				if (speed != 0 || be.color.isPresent()) {
					float time = renderTick * axisDirection.offset();
					if (diagonal && (downward ^ alongX) || !sideways && !diagonal && alongX || sideways && axisDirection == AxisDirection.NEGATIVE)
						speed = -speed;

					float scrollMult = diagonal ? 3f / 8f : 0.5f;

					float spriteSize = spriteShift.getTarget().getMaxV() - spriteShift.getTarget().getMinV();

					double scroll = speed * time / (31.5 * 16) + (bottom ? 0.5 : 0.0);
					scroll = scroll - Math.floor(scroll);
					scroll = scroll * spriteSize * scrollMult;

					beltBuffer.shiftUVScrolling(spriteShift, (float) scroll);
				}

				beltBuffer
						.transform(localTransforms)
						.renderInto(ms, vb);

				// Diagonal belt do not have a separate bottom model
				if (diagonal) break;
			}

			if (be.hasPulley()) {
				Direction dir = sideways ? Direction.UP : blockState.get(BeltBlock.HORIZONTAL_FACING).rotateYClockwise();

				Supplier<MatrixStack> matrixStackSupplier = () -> {
					MatrixStack stack = new MatrixStack();
                    TransformStack stacker = TransformStack.cast(stack);
					stacker.centre();
					if (dir.getAxis() == Direction.Axis.X) stacker.rotateY(90);
					if (dir.getAxis() == Direction.Axis.Y) stacker.rotateX(90);
					stacker.rotateX(90);
					stacker.unCentre();
					return stack;
				};

				SuperByteBuffer superBuffer = CachedBufferer.partialDirectional(AllPartialModels.BELT_PULLEY, blockState, dir, matrixStackSupplier);
				KineticBlockEntityRenderer.standardKineticRotationTransform(superBuffer, be, light).renderInto(ms, vb);
			}
		}

		renderItems(be, partialTicks, ms, buffer, light, overlay);
	}

	public static SpriteShiftEntry getSpriteShiftEntry(DyeColor color, boolean diagonal, boolean bottom) {
		if (color != null) {
			return (diagonal ? AllSpriteShifts.DYED_DIAGONAL_BELTS
					: bottom ? AllSpriteShifts.DYED_OFFSET_BELTS : AllSpriteShifts.DYED_BELTS).get(color);
		} else
			return diagonal ? AllSpriteShifts.BELT_DIAGONAL
					: bottom ? AllSpriteShifts.BELT_OFFSET : AllSpriteShifts.BELT;
	}

	public static PartialModel getBeltPartial(boolean diagonal, boolean start, boolean end, boolean bottom) {
		if (diagonal) {
			if (start) return AllPartialModels.BELT_DIAGONAL_START;
			if (end) return AllPartialModels.BELT_DIAGONAL_END;
			return AllPartialModels.BELT_DIAGONAL_MIDDLE;
		} else if (bottom) {
			if (start) return AllPartialModels.BELT_START_BOTTOM;
			if (end) return AllPartialModels.BELT_END_BOTTOM;
			return AllPartialModels.BELT_MIDDLE_BOTTOM;
		} else {
			if (start) return AllPartialModels.BELT_START;
			if (end) return AllPartialModels.BELT_END;
			return AllPartialModels.BELT_MIDDLE;
		}
	}

	protected void renderItems(BeltBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		if (!be.isController())
			return;
		if (be.beltLength == 0)
			return;

		ms.push();

		Direction beltFacing = be.getBeltFacing();
		Vec3i directionVec = beltFacing
							   .getVector();
		Vec3d beltStartOffset = Vec3d.of(directionVec).multiply(-.5)
			.add(.5, 15 / 16f, .5);
		ms.translate(beltStartOffset.x, beltStartOffset.y, beltStartOffset.z);
		BeltSlope slope = be.getCachedState()
			.get(BeltBlock.SLOPE);
		int verticality = slope == BeltSlope.DOWNWARD ? -1 : slope == BeltSlope.UPWARD ? 1 : 0;
		boolean slopeAlongX = beltFacing
								.getAxis() == Direction.Axis.X;

		boolean onContraption = be.getWorld() instanceof WrappedWorld;

		for (TransportedItemStack transported : be.getInventory()
			.getTransportedItems()) {
			ms.push();
            TransformStack.cast(ms)
				.nudge(transported.angle);

			float offset;
			float sideOffset;
			float verticalMovement;

			if (be.getSpeed() == 0) {
				offset = transported.beltPosition;
				sideOffset = transported.sideOffset;
			} else {
				offset = MathHelper.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
				sideOffset = MathHelper.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);
			}

			if (offset < .5)
				verticalMovement = 0;
			else
				verticalMovement = verticality * (Math.min(offset, be.beltLength - .5f) - .5f);
			Vec3d offsetVec = Vec3d.of(directionVec).multiply(offset);
			if (verticalMovement != 0)
				offsetVec = offsetVec.add(0, verticalMovement, 0);
			boolean onSlope =
				slope != BeltSlope.HORIZONTAL && MathHelper.clamp(offset, .5f, be.beltLength - .5f) == offset;
			boolean tiltForward = (slope == BeltSlope.DOWNWARD ^ beltFacing
																   .getDirection() == AxisDirection.POSITIVE) == (beltFacing
																														.getAxis() == Direction.Axis.Z);
			float slopeAngle = onSlope ? tiltForward ? -45 : 45 : 0;

			ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);

			boolean alongX = beltFacing
							   .rotateYClockwise()
							   .getAxis() == Direction.Axis.X;
			if (!alongX)
				sideOffset *= -1;
			ms.translate(alongX ? sideOffset : 0, 0, alongX ? 0 : sideOffset);

			int stackLight = onContraption ? light : getPackedLight(be, offset);
			ItemRenderer itemRenderer = MinecraftClient.getInstance()
				.getItemRenderer();
			boolean renderUpright = BeltHelper.isItemUpright(transported.stack);
			boolean blockItem = itemRenderer.getModel(transported.stack, be.getWorld(), null, 0)
				.hasDepth();
			Boolean sandwich = Mods.SANDWICHABLE.runIfInstalled(() -> () -> transported.stack.isOf(ItemsRegistry.SANDWICH)).orElse(Boolean.FALSE);
			if (sandwich)
				blockItem = false;
			int count = (int) (MathHelper.floorLog2((int) (transported.stack.getCount()))) / 2;
			Random r = new Random(transported.angle);

			boolean slopeShadowOnly = renderUpright && onSlope;
			float slopeOffset = 1 / 8f;
			if (slopeShadowOnly)
				ms.push();
			if (!renderUpright || slopeShadowOnly)
				ms.multiply((slopeAlongX ? RotationAxis.POSITIVE_Z : RotationAxis.POSITIVE_X).rotationDegrees(slopeAngle));
			if (onSlope)
				ms.translate(0, slopeOffset, 0);
			ms.push();
			ms.translate(0, -1 / 8f + 0.005f, 0);
			ShadowRenderHelper.renderShadow(ms, buffer, .75f, .2f);
			ms.pop();
			if (slopeShadowOnly) {
				ms.pop();
				ms.translate(0, slopeOffset, 0);
			}

			if (renderUpright) {
				Entity renderViewEntity = MinecraftClient.getInstance().cameraEntity;
				if (renderViewEntity != null) {
					Vec3d positionVec = renderViewEntity.getPos();
					Vec3d vectorForOffset = BeltHelper.getVectorForOffset(be, offset);
					Vec3d diff = vectorForOffset.subtract(positionVec);
					float yRot = (float) (MathHelper.atan2(diff.x, diff.z) + Math.PI);
					ms.multiply(RotationAxis.POSITIVE_Y.rotation(yRot));
				}
				ms.translate(0, 3 / 32d, 1 / 16f);
			}

			for (int i = 0; i <= count; i++) {
				ms.push();

				ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(transported.angle));
				if (!blockItem && !renderUpright) {
					ms.translate(0, -.09375, 0);
					ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
				}

				if (blockItem) {
					ms.translate(r.nextFloat() * .0625f * i, 0, r.nextFloat() * .0625f * i);
				}

				ms.scale(.5f, .5f, .5f);
				itemRenderer.renderItem(null, transported.stack, ModelTransformationMode.FIXED, false, ms, buffer, be.getWorld(), stackLight, overlay, 0);
				ms.pop();

				if (!renderUpright) {
					if (!blockItem)
						ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10));
					ms.translate(0, blockItem ? 1 / 64d : 1 / 16d, 0);
				} else
					ms.translate(0, 0, -1 / 16f);

			}

			ms.pop();
		}
		ms.pop();
	}

	protected int getPackedLight(BeltBlockEntity controller, float beltPos) {
		int segment = (int) Math.floor(beltPos);
		if (controller.lighter == null || segment >= controller.lighter.lightSegments() || segment < 0)
			return 0;

		return controller.lighter.getPackedLight(segment);
	}

}
