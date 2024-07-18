package com.simibubi.create.foundation.outliner;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public class ItemOutline extends Outline {

	protected Vec3d pos;
	protected ItemStack stack;

	public ItemOutline(Vec3d pos, ItemStack stack) {
		this.pos = pos;
		this.stack = stack;
	}

	@Override
	public void render(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera, float pt) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ms.push();

		TransformStack.cast(ms)
			.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z)
			.scale(params.alpha);

		mc.getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, false, ms,
				buffer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV,
				mc.getItemRenderer().getModel(stack, null, null, 0));

		ms.pop();
	}
}
