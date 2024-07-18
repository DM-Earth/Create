package com.simibubi.create.content.kinetics.waterwheel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.random.Random;
import com.jozufozu.flywheel.core.StitchedSprite;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.render.BakedModelRenderHelper;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.render.SuperByteBufferCache.Compartment;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

public class WaterWheelRenderer<T extends WaterWheelBlockEntity> extends KineticBlockEntityRenderer<T> {
	public static final Compartment<WaterWheelModelKey> WATER_WHEEL = new Compartment<>();

	public static final StitchedSprite OAK_PLANKS_TEMPLATE = new StitchedSprite(new Identifier("block/oak_planks"));
	public static final StitchedSprite OAK_LOG_TEMPLATE = new StitchedSprite(new Identifier("block/oak_log"));
	public static final StitchedSprite OAK_LOG_TOP_TEMPLATE = new StitchedSprite(new Identifier("block/oak_log_top"));

	private static final String[] LOG_SUFFIXES = new String[] { "_log", "_stem", "_block" };

	protected final boolean large;

	public WaterWheelRenderer(Context context, boolean large) {
		super(context);
		this.large = large;
	}

	public static <T extends WaterWheelBlockEntity> WaterWheelRenderer<T> standard(Context context) {
		return new WaterWheelRenderer<>(context, false);
	}

	public static <T extends WaterWheelBlockEntity> WaterWheelRenderer<T> large(Context context) {
		return new WaterWheelRenderer<>(context, true);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(T be, BlockState state) {
		WaterWheelModelKey key = new WaterWheelModelKey(large, state, be.material);
		return CreateClient.BUFFER_CACHE.get(WATER_WHEEL, key, () -> {
			BakedModel model = generateModel(key);
			BlockState state1 = key.state();
			Direction dir;
			if (key.large()) {
				dir = Direction.from(state1.get(LargeWaterWheelBlock.AXIS), AxisDirection.POSITIVE);
			} else {
				dir = state1.get(WaterWheelBlock.FACING);
			}
			MatrixStack transform = CachedBufferer.rotateToFaceVertical(dir).get();
			return BakedModelRenderHelper.standardModelRender(model, Blocks.AIR.getDefaultState(), transform);
		});
	}

	public static BakedModel generateModel(WaterWheelModelKey key) {
		BakedModel template;
		if (key.large()) {
			boolean extension = key.state()
				.get(LargeWaterWheelBlock.EXTENSION);
			if (extension) {
				template = AllPartialModels.LARGE_WATER_WHEEL_EXTENSION.get();
			} else {
				template = AllPartialModels.LARGE_WATER_WHEEL.get();
			}
		} else {
			template = AllPartialModels.WATER_WHEEL.get();
		}

		return generateModel(template, key.material());
	}

	public static BakedModel generateModel(BakedModel template, BlockState planksBlockState) {
		Block planksBlock = planksBlockState.getBlock();
		Identifier id = RegisteredObjects.getKeyOrThrow(planksBlock);
		String path = id.getPath();

		if (path.endsWith("_planks")) {
			String namespace = id.getNamespace();
			String wood = path.substring(0, path.length() - 7);
			BlockState logBlockState = getLogBlockState(namespace, wood);

			Map<Sprite, Sprite> map = new Reference2ReferenceOpenHashMap<>();
			map.put(OAK_PLANKS_TEMPLATE.get(), getSpriteOnSide(planksBlockState, Direction.UP));
			map.put(OAK_LOG_TEMPLATE.get(), getSpriteOnSide(logBlockState, Direction.SOUTH));
			map.put(OAK_LOG_TOP_TEMPLATE.get(), getSpriteOnSide(logBlockState, Direction.UP));

			return BakedModelHelper.generateModel(template, map::get);
		}

		return BakedModelHelper.generateModel(template, sprite -> null);
	}

	private static BlockState getLogBlockState(String namespace, String wood) {
		for (String suffix : LOG_SUFFIXES) {
			Optional<BlockState> state =
				Registries.BLOCK.getEntry(RegistryKey.of(RegistryKeys.BLOCK, new Identifier(namespace, wood + suffix)))
					.map(RegistryEntry::value)
					.map(Block::getDefaultState);
			if (state.isPresent())
				return state.get();
		}
		return Blocks.OAK_LOG.getDefaultState();
	}

	private static Sprite getSpriteOnSide(BlockState state, Direction side) {
		BakedModel model = MinecraftClient.getInstance()
			.getBlockRenderManager()
			.getModel(state);
		if (model == null)
			return null;
		Random random = Random.create();
		random.setSeed(42L);
		List<BakedQuad> quads = model.getQuads(state, side, random);
		if (!quads.isEmpty()) {
			return quads.get(0)
				.getSprite();
		}
		random.setSeed(42L);
		quads = model.getQuads(state, null, random);
		if (!quads.isEmpty()) {
			for (BakedQuad quad : quads) {
				if (quad.getFace() == side) {
					return quad.getSprite();
				}
			}
		}
		return model.getParticleSprite();
	}

}
