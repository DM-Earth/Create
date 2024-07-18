package com.simibubi.create.content.decoration.girder;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.utility.Iterate;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class ConnectedGirderModel extends CTModel {

	public ConnectedGirderModel(BakedModel originalModel) {
		super(originalModel, new GirderCTBehaviour());
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		ConnectionData data = new ConnectionData();
		for (Direction d : Iterate.horizontalDirections)
			data.setConnected(d, GirderBlock.isConnected(blockView, pos, state, d));

		super.emitBlockQuads(blockView, state, pos, randomSupplier, context);

		for (Direction d : Iterate.horizontalDirections)
			if (data.isConnected(d))
				((FabricBakedModel) AllPartialModels.METAL_GIRDER_BRACKETS.get(d)
					.get())
					.emitBlockQuads(blockView, state, pos, randomSupplier, context);
	}

	private static class ConnectionData {
		boolean[] connectedFaces;

		public ConnectionData() {
			connectedFaces = new boolean[4];
			Arrays.fill(connectedFaces, false);
		}

		void setConnected(Direction face, boolean connected) {
			connectedFaces[face.getHorizontal()] = connected;
		}

		boolean isConnected(Direction face) {
			return connectedFaces[face.getHorizontal()];
		}
	}

}
