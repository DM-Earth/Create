package com.simibubi.create.foundation.block.connected;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.CTContext;
import com.simibubi.create.foundation.utility.Iterate;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class CTModel extends ForwardingBakedModel {

	private final ConnectedTextureBehaviour behaviour;

	public CTModel(BakedModel originalModel, ConnectedTextureBehaviour behaviour) {
		wrapped = originalModel;
		this.behaviour = behaviour;
	}

	protected CTData createCTData(BlockRenderView world, BlockPos pos, BlockState state) {
		CTData data = new CTData();
		Mutable mutablePos = new Mutable();
		for (Direction face : Iterate.directions) {
			BlockState actualState = world.getBlockState(pos);
			if (!behaviour.buildContextForOccludedDirections()
				&& !Block.shouldDrawSide(state, world, pos, face, mutablePos.set(pos, face))
				&& !(actualState.getBlock()instanceof CopycatBlock ufb
					&& !ufb.canFaceBeOccluded(actualState, face)))
				continue;
			CTType dataType = behaviour.getDataType(world, pos, state, face);
			if (dataType == null)
				continue;
			CTContext context = behaviour.buildContext(world, pos, state, face, dataType.getContextRequirement());
			data.put(face, dataType.getTextureIndex(context));
		}
		return data;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		CTData data = createCTData(blockView, pos, state);

		SpriteFinder spriteFinder = SpriteFinder.get(MinecraftClient.getInstance().getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
		context.pushTransform(quad -> {
			int index = data.get(quad.lightFace());
			if (index != -1) {
				Sprite sprite = spriteFinder.find(quad, 0);
				CTSpriteShiftEntry spriteShift = behaviour.getShift(state, quad.lightFace(), sprite);
				if (spriteShift != null) {
					if (sprite == spriteShift.getOriginal()) {
						for (int vertex = 0; vertex < 4; vertex++) {
							float u = quad.spriteU(vertex, 0);
							float v = quad.spriteV(vertex, 0);
							quad.sprite(vertex, 0,
									spriteShift.getTargetU(u, index),
									spriteShift.getTargetV(v, index)
							);
						}
					}
				}
			}
			return true;
		});
		super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
		context.popTransform();
	}

	private static class CTData {
		private final int[] indices;

		public CTData() {
			indices = new int[6];
			Arrays.fill(indices, -1);
		}

		public void put(Direction face, int texture) {
			indices[face.getId()] = texture;
		}

		public int get(Direction face) {
			return indices[face.getId()];
		}
	}

}
