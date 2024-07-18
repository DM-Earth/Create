package com.simibubi.create.foundation.item;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;

public interface CustomArmPoseItem {
	@Nullable
	ArmPose getArmPose(ItemStack stack, AbstractClientPlayerEntity player, Hand hand);
}
