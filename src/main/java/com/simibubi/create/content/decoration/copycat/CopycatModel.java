package com.simibubi.create.content.decoration.copycat;

import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.models.CustomParticleIconModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public abstract class CopycatModel extends ForwardingBakedModel implements CustomParticleIconModel {

	public CopycatModel(BakedModel originalModel) {
		wrapped = originalModel;
	}

	private void gatherOcclusionData(BlockRenderView world, BlockPos pos, BlockState state, BlockState material,
		OcclusionData occlusionData, CopycatBlock copycatBlock) {
		Mutable mutablePos = new Mutable();
		for (Direction face : Iterate.directions) {
			if (!copycatBlock.canFaceBeOccluded(state, face))
				continue;
			Mutable neighbourPos = mutablePos.set(pos, face);
			if (!Block.shouldDrawSide(material, world, pos, face, neighbourPos))
				occlusionData.occlude(face);
		}
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		BlockState material;
		if (blockView instanceof RenderAttachedBlockView attachmentView
				&& attachmentView.getBlockEntityRenderAttachment(pos) instanceof BlockState material1) {
			material = material1;
		} else {
			material = AllBlocks.COPYCAT_BASE.getDefaultState();
		}

		OcclusionData occlusionData = new OcclusionData();
		if (state.getBlock() instanceof CopycatBlock copycatBlock) {
			gatherOcclusionData(blockView, pos, state, material, occlusionData, copycatBlock);
		}

		CullFaceRemovalData cullFaceRemovalData = new CullFaceRemovalData();
		if (state.getBlock() instanceof CopycatBlock copycatBlock) {
			for (Direction cullFace : Iterate.directions) {
				if (copycatBlock.shouldFaceAlwaysRender(state, cullFace)) {
					cullFaceRemovalData.remove(cullFace);
				}
			}
		}

		// fabric: If it is the default state do not push transformations, will cause issues with GhostBlockRenderer
		boolean shouldTransform = material != AllBlocks.COPYCAT_BASE.getDefaultState();

		// fabric: need to change the default render material
		if (shouldTransform)
			context.pushTransform(MaterialFixer.create(material));

		emitBlockQuadsInner(blockView, state, pos, randomSupplier, context, material, cullFaceRemovalData, occlusionData);

		// fabric: pop the material changer transform
		if (shouldTransform)
			context.popTransform();
	}

	protected abstract void emitBlockQuadsInner(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context, BlockState material, CullFaceRemovalData cullFaceRemovalData, OcclusionData occlusionData);

	@Override
	public Sprite getParticleIcon(Object data) {
		if (data instanceof BlockState state) {
			BlockState material = getMaterial(state);

			return getIcon(getModelOf(material), null);
		}

		return CustomParticleIconModel.super.getParticleIcon(data);
	}

	public static Sprite getIcon(BakedModel model, @Nullable Object data) {
		if (model instanceof CustomParticleIconModel particleIconModel)
			return particleIconModel.getParticleIcon(data);
		return model.getParticleSprite();
	}

	@Nullable
	public static BlockState getMaterial(BlockState material) {
		return material == null ? AllBlocks.COPYCAT_BASE.getDefaultState() : material;
	}

	public static BakedModel getModelOf(BlockState state) {
		return MinecraftClient.getInstance()
			.getBlockRenderManager()
			.getModel(state);
	}

	protected static class OcclusionData {
		private final boolean[] occluded;

		public OcclusionData() {
			occluded = new boolean[6];
		}

		public void occlude(Direction face) {
			occluded[face.getId()] = true;
		}

		public boolean isOccluded(Direction face) {
			return face == null ? false : occluded[face.getId()];
		}
	}

	protected static class CullFaceRemovalData {
		private final boolean[] shouldRemove;

		public CullFaceRemovalData() {
			shouldRemove = new boolean[6];
		}

		public void remove(Direction face) {
			shouldRemove[face.getId()] = true;
		}

		public boolean shouldRemove(Direction face) {
			return face == null ? false : shouldRemove[face.getId()];
		}
	}

	private record MaterialFixer(RenderMaterial materialDefault) implements QuadTransform {
		@Override
		public boolean transform(MutableQuadView quad) {
			if (quad.material().blendMode() == BlendMode.DEFAULT) {
				// default needs to be changed from the Copycat's default (cutout) to the wrapped material's default.
				quad.material(materialDefault);
			}
			return true;
		}

		public static MaterialFixer create(BlockState materialState) {
			RenderLayer type = RenderLayers.getBlockLayer(materialState);
			BlendMode blendMode = BlendMode.fromRenderLayer(type);
			MaterialFinder finder = Objects.requireNonNull(RendererAccess.INSTANCE.getRenderer()).materialFinder();
			RenderMaterial renderMaterial = finder.blendMode(0, blendMode).find();
			return new MaterialFixer(renderMaterial);
		}
	}
}
