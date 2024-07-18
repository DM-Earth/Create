package com.simibubi.create.content.decoration.copycat;

import java.util.function.Supplier;

import com.simibubi.create.foundation.block.render.SpriteShiftEntry;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class CopycatBarsModel extends CopycatModel {

	public CopycatBarsModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	public boolean useAmbientOcclusion() {
		return false;
	}

	@Override
	protected void emitBlockQuadsInner(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context, BlockState material, CullFaceRemovalData cullFaceRemovalData, OcclusionData occlusionData) {
		BakedModel model = getModelOf(material);
		Sprite mainTargetSprite = model.getParticleSprite();

		boolean vertical = state.get(CopycatPanelBlock.FACING)
			.getAxis() == Axis.Y;

		SpriteFinder spriteFinder = SpriteFinder.get(MinecraftClient.getInstance().getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));

		// This is very cursed
		SpriteAndBool altTargetSpriteHolder = new SpriteAndBool(mainTargetSprite, true);
		context.pushTransform(quad -> {
			if (altTargetSpriteHolder.bool && quad.cullFace() == null && quad.lightFace() == Direction.UP) {
				altTargetSpriteHolder.sprite = spriteFinder.find(quad, 0);
				altTargetSpriteHolder.bool = false;
			}
			return false;
		});
		((FabricBakedModel) model).emitBlockQuads(blockView, material, pos, randomSupplier, context);
		context.popTransform();
		Sprite altTargetSprite = altTargetSpriteHolder.sprite;

		context.pushTransform(quad -> {
			Sprite targetSprite;
			Direction cullFace = quad.cullFace();
			if (cullFace != null && (vertical || cullFace.getAxis() == Axis.Y)) {
				targetSprite = altTargetSprite;
			} else {
				targetSprite = mainTargetSprite;
			}

			Sprite original = spriteFinder.find(quad, 0);
			for (int vertex = 0; vertex < 4; vertex++) {
				float u = targetSprite.getFrameU(SpriteShiftEntry.getUnInterpolatedU(original, quad.spriteU(vertex, 0)));
				float v = targetSprite.getFrameV(SpriteShiftEntry.getUnInterpolatedV(original, quad.spriteV(vertex, 0)));
				quad.sprite(vertex, 0, u, v);
			}
			return true;
		});
		((FabricBakedModel) wrapped).emitBlockQuads(blockView, state, pos, randomSupplier, context);
		context.popTransform();
	}

	private static class SpriteAndBool {
		public Sprite sprite;
		public boolean bool;

		public SpriteAndBool(Sprite sprite, boolean bool) {
			this.sprite = sprite;
			this.bool = bool;
		}
	}

}
