package com.simibubi.create.content.trains.schedule;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.mixin.accessor.AgeableListModelAccessor;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.Couple;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.ModelPartAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPart.Cuboid;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.AxolotlEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.FrogEntityModel;
import net.minecraft.client.render.entity.model.MagmaCubeEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.entity.model.SlimeEntityModel;
import net.minecraft.client.render.entity.model.WardenEntityModel;
import net.minecraft.client.render.entity.model.WolfEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class TrainHatArmorLayer<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

	private Vec3d offset;

	public TrainHatArmorLayer(FeatureRendererContext<T, M> renderer, Vec3d offset) {
		super(renderer);
		this.offset = offset;
	}

	@Override
	public void render(MatrixStack ms, VertexConsumerProvider buffer, int light, LivingEntity entity, float yaw, float pitch,
		float pt, float p_225628_8_, float p_225628_9_, float p_225628_10_) {
		if (!shouldRenderOn(entity))
			return;

		M entityModel = getContextModel();
		RenderLayer renderType = TexturedRenderLayers.getEntityCutout();
		ms.push();

		boolean valid = false;
		TransformStack msr = TransformStack.cast(ms);
		float scale = 1;

		if (entityModel instanceof AnimalModel<?> model && entityModel instanceof io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.AgeableListModelAccessor access) {
			if (model.child) {
				if (access.porting_lib$scaleHead()) {
					float f = 1.5F / access.porting_lib$babyHeadScale();
					ms.scale(f, f, f);
				}
				ms.translate(0.0D, access.porting_lib$babyYHeadOffset() / 16.0F, access.porting_lib$babyZHeadOffset() / 16.0F);
			}

			ModelPart head = getHeadPart(model);
			if (head != null) {
				head.rotate(ms);

				if (model instanceof WolfEntityModel)
					head = head.getChild("real_head");
				if (model instanceof AxolotlEntityModel)
					head = head.getChild("head");

				ms.translate(offset.x / 16f, offset.y / 16f, offset.z / 16f);

				if (!head.isEmpty()) {
					Cuboid cube = ((ModelPartAccessor) (Object) head).porting_lib$cubes().get(0);
					ms.translate(offset.x / 16f, (cube.minY - cube.maxY + offset.y) / 16f, offset.z / 16f);
					float max = Math.max(cube.maxX - cube.minX, cube.maxZ - cube.minZ) / 8f;
					ms.scale(max, max, max);
				}

				valid = true;
			}
		}

		else if (entityModel instanceof SinglePartEntityModel<?> model) {
			boolean slime = model instanceof SlimeEntityModel || model instanceof MagmaCubeEntityModel;
			ModelPart root = model.getPart();
			String headName = slime ? "cube" : "head";
			ModelPart head = root.hasChild(headName) ? root.getChild(headName) : null;

			if (model instanceof WardenEntityModel)
				head = root.getChild("bone").getChild("body").getChild("head");

			if (model instanceof FrogEntityModel) {
				head = root.getChild("body").getChild("head");
				scale = .5f;
			}

			if (head != null) {
				head.rotate(ms);

				if (!head.isEmpty()) {
					ModelPartAccessor headAccess = (ModelPartAccessor) (Object) head;
					Cuboid cube = headAccess.porting_lib$cubes().get(0);
					ms.translate(offset.x, (cube.minY - cube.maxY + offset.y) / 16f, offset.z / 16f);
					float max = Math.max(cube.maxX - cube.minX, cube.maxZ - cube.minZ) / (slime ? 6.5f : 8f) * scale;
					ms.scale(max, max, max);
				}

				valid = true;
			}
		}

		if (valid) {
			ms.scale(1, -1, -1);
			ms.translate(0, -2.25f / 16f, 0);
			msr.rotateX(-8.5f);
			BlockState air = Blocks.AIR.getDefaultState();
			CachedBufferer.partial(AllPartialModels.TRAIN_HAT, air)
				.forEntityRender()
				.light(light)
				.renderInto(ms, buffer.getBuffer(renderType));
		}

		ms.pop();
	}

	private boolean shouldRenderOn(LivingEntity entity) {
		if (entity == null)
			return false;
		if (entity.getCustomData()
			.contains("TrainHat"))
			return true;
		if (!entity.hasVehicle())
			return false;
		if (entity instanceof PlayerEntity p) {
			ItemStack headItem = p.getEquippedStack(EquipmentSlot.HEAD);
			if (!headItem.isEmpty())
				return false;
		}
		Entity vehicle = entity.getVehicle();
		if (!(vehicle instanceof CarriageContraptionEntity cce))
			return false;
		if (!cce.hasSchedule() && !(entity instanceof PlayerEntity))
			return false;
		Contraption contraption = cce.getContraption();
		if (!(contraption instanceof CarriageContraption cc))
			return false;
		BlockPos seatOf = cc.getSeatOf(entity.getUuid());
		if (seatOf == null)
			return false;
		Couple<Boolean> validSides = cc.conductorSeats.get(seatOf);
		return validSides != null;
	}

	public static void registerOn(EntityRenderer<?> entityRenderer, RegistrationHelper helper) {
		if (!(entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer))
			return;

		EntityModel<?> model = livingRenderer.getModel();

		if (!(model instanceof SinglePartEntityModel) && !(model instanceof AnimalModel))
			return;

		Vec3d offset = TrainHatOffsets.getOffset(model);
		TrainHatArmorLayer<?, ?> layer = new TrainHatArmorLayer<>(livingRenderer, offset);
		helper.register(layer);
	}

	private static ModelPart getHeadPart(AnimalModel<?> model) {
		for (ModelPart part : ((AgeableListModelAccessor) model).create$callGetHeadParts())
			return part;
		for (ModelPart part : ((AgeableListModelAccessor) model).create$callGetBodyParts())
			return part;
		return null;
	}

}
