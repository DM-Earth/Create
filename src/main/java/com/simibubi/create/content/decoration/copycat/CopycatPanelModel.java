package com.simibubi.create.content.decoration.copycat;

import java.util.function.Supplier;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.utility.Iterate;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.model.WrapperBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class CopycatPanelModel extends CopycatModel {

	protected static final Box CUBE_AABB = new Box(BlockPos.ORIGIN);

	public CopycatPanelModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	protected void emitBlockQuadsInner(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context, BlockState material, CullFaceRemovalData cullFaceRemovalData, OcclusionData occlusionData) {
		Direction facing = state.getOrEmpty(CopycatPanelBlock.FACING)
			.orElse(Direction.UP);
		BlockRenderManager blockRenderer = MinecraftClient.getInstance()
			.getBlockRenderManager();

		BlockState specialCopycatModelState = null;
		if (CopycatSpecialCases.isBarsMaterial(material))
			specialCopycatModelState = AllBlocks.COPYCAT_BARS.getDefaultState();
		if (CopycatSpecialCases.isTrapdoorMaterial(material)) {
			((FabricBakedModel) blockRenderer.getModel(material))
				.emitBlockQuads(blockView, material, pos, randomSupplier, context);
			return;
		}

		if (specialCopycatModelState != null) {
			BakedModel blockModel = blockRenderer
				.getModel(specialCopycatModelState.with(FacingBlock.FACING, facing));

			// fabric: extra handling for wrapped models, see: #1176
			while (blockModel instanceof WrapperBakedModel wbm) {
				if (blockModel instanceof CopycatModel) break;
				blockModel = wbm.getWrappedModel();
			}

			if (blockModel instanceof CopycatModel cm) {
				cm.emitBlockQuadsInner(blockView, state, pos, randomSupplier, context, material, cullFaceRemovalData, occlusionData);
				return;
			}
		}

		BakedModel model = getModelOf(material);

		Vec3d normal = Vec3d.of(facing.getVector());
		Vec3d normalScaled14 = normal.multiply(14 / 16f);

		SpriteFinder spriteFinder = SpriteFinder.get(MinecraftClient.getInstance().getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));

		// Use a mesh to defer quad emission since quads cannot be emitted inside a transform
		MeshBuilder meshBuilder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
		QuadEmitter emitter = meshBuilder.getEmitter();
		context.pushTransform(quad -> {
			if (cullFaceRemovalData.shouldRemove(quad.cullFace())) {
				quad.cullFace(null);
			} else if (occlusionData.isOccluded(quad.cullFace())) {
				// Add quad to mesh and do not render original quad to preserve quad render order
				// copyTo does not copy the material
				RenderMaterial quadMaterial = quad.material();
				quad.copyTo(emitter);
				emitter.material(quadMaterial);
				emitter.emit();
				return false;
			}

			// 2 Pieces
			for (boolean front : Iterate.trueAndFalse) {
				Vec3d normalScaledN13 = normal.multiply(front ? 0 : -13 / 16f);
				float contract = 16 - (front ? 1 : 2);
				Box bb = CUBE_AABB.shrink(normal.x * contract / 16, normal.y * contract / 16, normal.z * contract / 16);
				if (!front)
					bb = bb.offset(normalScaled14);

				Direction direction = quad.lightFace();

				if (front && direction == facing)
					continue;
				if (!front && direction == facing.getOpposite())
					continue;

				// copyTo does not copy the material
				RenderMaterial quadMaterial = quad.material();
				quad.copyTo(emitter);
				emitter.material(quadMaterial);
				BakedModelHelper.cropAndMove(emitter, spriteFinder.find(emitter, 0), bb, normalScaledN13);
				emitter.emit();
			}

			return false;
		});
		((FabricBakedModel) model).emitBlockQuads(blockView, material, pos, randomSupplier, context);
		context.popTransform();
		context.meshConsumer().accept(meshBuilder.build());
	}

}
