package com.simibubi.create.content.decoration.placard;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.utility.AngleHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class PlacardRenderer extends SafeBlockEntityRenderer<PlacardBlockEntity> {

	public PlacardRenderer(BlockEntityRendererFactory.Context context) {}

	@Override
	protected void renderSafe(PlacardBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		ItemStack heldItem = be.getHeldItem();
		if (heldItem.isEmpty())
			return;

		BlockState blockState = be.getCachedState();
		Direction facing = blockState.get(PlacardBlock.FACING);
		WallMountLocation face = blockState.get(PlacardBlock.FACE);

		ItemRenderer itemRenderer = MinecraftClient.getInstance()
			.getItemRenderer();
		boolean blockItem = itemRenderer.getModel(heldItem, null, null, 0)
			.hasDepth();

		ms.push();
		TransformStack.cast(ms)
			.centre()
			.rotate(Direction.UP,
				(face == WallMountLocation.CEILING ? MathHelper.PI : 0) + AngleHelper.rad(180 + AngleHelper.horizontalAngle(facing)))
			.rotate(Direction.EAST,
				face == WallMountLocation.CEILING ? -MathHelper.PI / 2 : face == WallMountLocation.FLOOR ? MathHelper.PI / 2 : 0)
			.translate(0, 0, 4.5 / 16f)
			.scale(blockItem ? .5f : .375f);

		itemRenderer.renderItem(heldItem, ModelTransformationMode.FIXED, light, overlay, ms, buffer, be.getWorld(), 0);
		ms.pop();
	}

}
