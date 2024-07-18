package com.simibubi.create.foundation.mixin.accessor;

import java.util.Map;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorFeatureRenderer.class)
public interface HumanoidArmorLayerAccessor {
	@Accessor("ARMOR_TEXTURE_CACHE")
	static Map<String, Identifier> create$getArmorTextureCache() {
		throw new RuntimeException();
	}

	@Accessor("innerModel")
	BipedEntityModel<?> create$getInnerModel();

	@Accessor("outerModel")
	BipedEntityModel<?> create$getOuterModel();

	@Invoker("setVisible")
	void create$callSetVisible(BipedEntityModel<?> model, EquipmentSlot slot);

}
