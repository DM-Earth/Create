package com.simibubi.create.content.kinetics.belt;

import java.util.function.Supplier;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.RenderData;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;

import io.github.fabricators_of_create.porting_lib.models.CustomParticleIconModel;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class BeltModel extends ForwardingBakedModel implements CustomParticleIconModel {

	private static final SpriteShiftEntry SPRITE_SHIFT = AllSpriteShifts.ANDESIDE_BELT_CASING;

	public BeltModel(BakedModel template) {
		wrapped = template;
	}

	@Override
	public Sprite getParticleIcon(Object data) {
		if (data instanceof RenderData renderData) {
			CasingType type = renderData.casingType();
			if (type == CasingType.NONE || type == CasingType.BRASS)
				return CustomParticleIconModel.super.getParticleIcon(data);
		}

		return AllSpriteShifts.ANDESITE_CASING.getOriginal();
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		if (!(blockView instanceof RenderAttachedBlockView attachmentView
				&& attachmentView.getBlockEntityRenderAttachment(pos) instanceof RenderData data)) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}

		boolean cover = data.covered();
		CasingType type = data.casingType();
		boolean brassCasing = type == CasingType.BRASS;

		if (type == CasingType.NONE || brassCasing && !cover) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}

		if (!brassCasing) {
			SpriteFinder spriteFinder = SpriteFinder.get(MinecraftClient.getInstance().getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
			context.pushTransform(quad -> {
				Sprite sprite = spriteFinder.find(quad, 0);
				if (sprite == SPRITE_SHIFT.getOriginal()) {
					for (int vertex = 0; vertex < 4; vertex++) {
						float u = quad.spriteU(vertex, 0);
						float v = quad.spriteV(vertex, 0);
						quad.sprite(vertex, 0,
								SPRITE_SHIFT.getTargetU(u),
								SPRITE_SHIFT.getTargetV(v)
						);
					}
				}
				return true;
			});
		}

		super.emitBlockQuads(blockView, state, pos, randomSupplier, context);

		if (cover) {
			boolean alongX = state.get(BeltBlock.HORIZONTAL_FACING)
				.getAxis() == Axis.X;
			BakedModel coverModel =
				(brassCasing ? alongX ? AllPartialModels.BRASS_BELT_COVER_X : AllPartialModels.BRASS_BELT_COVER_Z
					: alongX ? AllPartialModels.ANDESITE_BELT_COVER_X : AllPartialModels.ANDESITE_BELT_COVER_Z).get();
			((FabricBakedModel) coverModel).emitBlockQuads(blockView, state, pos, randomSupplier, context);
		}

		if (!brassCasing) {
			context.popTransform();
		}
	}

}
