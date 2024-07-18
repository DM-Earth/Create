package com.simibubi.create.content.equipment.blueprint;

import org.joml.Matrix3f;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.equipment.blueprint.BlueprintEntity.BlueprintSection;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.Couple;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class BlueprintRenderer extends EntityRenderer<BlueprintEntity> {

	public BlueprintRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(BlueprintEntity entity, float yaw, float pt, MatrixStack ms, VertexConsumerProvider buffer,
		int light) {
		PartialModel partialModel = entity.size == 3 ? AllPartialModels.CRAFTING_BLUEPRINT_3x3
			: entity.size == 2 ? AllPartialModels.CRAFTING_BLUEPRINT_2x2 : AllPartialModels.CRAFTING_BLUEPRINT_1x1;
		SuperByteBuffer sbb = CachedBufferer.partial(partialModel, Blocks.AIR.getDefaultState());
		sbb.rotateY(-yaw)
			.rotateX(90.0F + entity.getPitch())
			.translate(-.5, -1 / 32f, -.5);
		if (entity.size == 2)
			sbb.translate(.5, 0, -.5);

		sbb.forEntityRender()
			.light(light)
			.renderInto(ms, buffer.getBuffer(TexturedRenderLayers.getEntitySolid()));
		super.render(entity, yaw, pt, ms, buffer, light);

		ms.push();

		float fakeNormalXRotation = -15;
		int bl = light >> 4 & 0xf;
		int sl = light >> 20 & 0xf;
		boolean vertical = entity.getPitch() != 0;
		if (entity.getPitch() == -90)
			fakeNormalXRotation = -45;
		else if (entity.getPitch() == 90 || yaw % 180 != 0) {
			bl /= 1.35;
			sl /= 1.35;
		}
		int itemLight = MathHelper.floor(sl + .5) << 20 | (MathHelper.floor(bl + .5) & 0xf) << 4;

		TransformStack.cast(ms)
			.rotateY(vertical ? 0 : -yaw)
			.rotateX(fakeNormalXRotation);
		Matrix3f copy = new Matrix3f(ms.peek()
			.getNormalMatrix());

		ms.pop();
		ms.push();

		TransformStack.cast(ms)
			.rotateY(-yaw)
			.rotateX(entity.getPitch())
			.translate(0, 0, 1 / 32f + .001);

		if (entity.size == 3)
			ms.translate(-1, -1, 0);

		MatrixStack squashedMS = new MatrixStack();
		squashedMS.peek()
			.getPositionMatrix()
			.mul(ms.peek()
				.getPositionMatrix());

		for (int x = 0; x < entity.size; x++) {
			squashedMS.push();
			for (int y = 0; y < entity.size; y++) {
				BlueprintSection section = entity.getSection(x * entity.size + y);
				Couple<ItemStack> displayItems = section.getDisplayItems();
				squashedMS.push();
				squashedMS.scale(.5f, .5f, 1 / 1024f);
				displayItems.forEachWithContext((stack, primary) -> {
					if (stack.isEmpty())
						return;

					squashedMS.push();
					if (!primary) {
						squashedMS.translate(0.325f, -0.325f, 1);
						squashedMS.scale(.625f, .625f, 1);
					}

					squashedMS.peek()
						.getNormalMatrix()
						.set(copy);

					MinecraftClient.getInstance()
						.getItemRenderer()
						.renderItem(stack, ModelTransformationMode.GUI, itemLight, OverlayTexture.DEFAULT_UV, squashedMS,
							buffer, entity.getWorld(), 0);
					squashedMS.pop();
				});
				squashedMS.pop();
				squashedMS.translate(1, 0, 0);
			}
			squashedMS.pop();
			squashedMS.translate(0, 1, 0);
		}

		ms.pop();
	}

	@Override
	public Identifier getTexture(BlueprintEntity entity) {
		return null;
	}

}
