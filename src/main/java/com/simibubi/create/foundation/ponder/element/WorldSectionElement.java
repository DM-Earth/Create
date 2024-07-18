package com.simibubi.create.foundation.ponder.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.jozufozu.flywheel.core.model.ModelUtil;
import com.jozufozu.flywheel.core.model.ShadeSeparatedBufferedData;
import com.jozufozu.flywheel.core.model.ShadeSeparatingVertexConsumer;
import com.jozufozu.flywheel.fabric.model.LayerFilteringBakedModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.outliner.AABBOutline;
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.ponder.Selection;
import com.simibubi.create.foundation.render.BlockEntityRenderHelper;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.render.SuperByteBufferCache;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class WorldSectionElement extends AnimatedSceneElement {

	public static final SuperByteBufferCache.Compartment<Pair<Integer, Integer>> DOC_WORLD_SECTION =
			new SuperByteBufferCache.Compartment<>();

	private static final ThreadLocal<ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(ThreadLocalObjects::new);

	List<BlockEntity> renderedBlockEntities;
	List<Pair<BlockEntity, Consumer<World>>> tickableBlockEntities;
	Selection section;
	boolean redraw;

	Vec3d prevAnimatedOffset = Vec3d.ZERO;
	Vec3d animatedOffset = Vec3d.ZERO;
	Vec3d prevAnimatedRotation = Vec3d.ZERO;
	Vec3d animatedRotation = Vec3d.ZERO;
	Vec3d centerOfRotation = Vec3d.ZERO;
	Vec3d stabilizationAnchor = null;

	BlockPos selectedBlock;

	public WorldSectionElement() {}

	public WorldSectionElement(Selection section) {
		this.section = section.copy();
		centerOfRotation = section.getCenter();
	}

	public void mergeOnto(WorldSectionElement other) {
		setVisible(false);
		if (other.isEmpty())
			other.set(section);
		else
			other.add(section);
	}

	public void set(Selection selection) {
		applyNewSelection(selection.copy());
	}

	public void add(Selection toAdd) {
		applyNewSelection(this.section.add(toAdd));
	}

	public void erase(Selection toErase) {
		applyNewSelection(this.section.substract(toErase));
	}

	private void applyNewSelection(Selection selection) {
		this.section = selection;
		queueRedraw();
	}

	public void setCenterOfRotation(Vec3d center) {
		centerOfRotation = center;
	}

	public void stabilizeRotation(Vec3d anchor) {
		stabilizationAnchor = anchor;
	}

	@Override
	public void reset(PonderScene scene) {
		super.reset(scene);
		resetAnimatedTransform();
		resetSelectedBlock();
	}

	public void selectBlock(BlockPos pos) {
		selectedBlock = pos;
	}

	public void resetSelectedBlock() {
		selectedBlock = null;
	}

	public void resetAnimatedTransform() {
		prevAnimatedOffset = Vec3d.ZERO;
		animatedOffset = Vec3d.ZERO;
		prevAnimatedRotation = Vec3d.ZERO;
		animatedRotation = Vec3d.ZERO;
	}

	public void queueRedraw() {
		redraw = true;
	}

	public boolean isEmpty() {
		return section == null;
	}

	public void setEmpty() {
		section = null;
	}

	public void setAnimatedRotation(Vec3d eulerAngles, boolean force) {
		this.animatedRotation = eulerAngles;
		if (force)
			prevAnimatedRotation = animatedRotation;
	}

	public Vec3d getAnimatedRotation() {
		return animatedRotation;
	}

	public void setAnimatedOffset(Vec3d offset, boolean force) {
		this.animatedOffset = offset;
		if (force)
			prevAnimatedOffset = animatedOffset;
	}

	public Vec3d getAnimatedOffset() {
		return animatedOffset;
	}

	@Override
	public boolean isVisible() {
		return super.isVisible() && !isEmpty();
	}

	class WorldSectionRayTraceResult {
		Vec3d actualHitVec;
		BlockPos worldPos;
	}

	public Pair<Vec3d, BlockHitResult> rayTrace(PonderWorld world, Vec3d source, Vec3d target) {
		world.setMask(this.section);
		Vec3d transformedTarget = reverseTransformVec(target);
		BlockHitResult rayTraceBlocks = world.raycast(new RaycastContext(reverseTransformVec(source), transformedTarget,
				RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, null));
		world.clearMask();

		if (rayTraceBlocks == null)
			return null;
		if (rayTraceBlocks.getPos() == null)
			return null;

		double t = rayTraceBlocks.getPos()
				.subtract(transformedTarget)
				.lengthSquared()
				/ source.subtract(target)
				.lengthSquared();
		Vec3d actualHit = VecHelper.lerp((float) t, target, source);
		return Pair.of(actualHit, rayTraceBlocks);
	}

	private Vec3d reverseTransformVec(Vec3d in) {
		float pt = AnimationTickHolder.getPartialTicks();
		in = in.subtract(VecHelper.lerp(pt, prevAnimatedOffset, animatedOffset));
		if (!animatedRotation.equals(Vec3d.ZERO) || !prevAnimatedRotation.equals(Vec3d.ZERO)) {
			if (centerOfRotation == null)
				centerOfRotation = section.getCenter();
			double rotX = MathHelper.lerp(pt, prevAnimatedRotation.x, animatedRotation.x);
			double rotZ = MathHelper.lerp(pt, prevAnimatedRotation.z, animatedRotation.z);
			double rotY = MathHelper.lerp(pt, prevAnimatedRotation.y, animatedRotation.y);
			in = in.subtract(centerOfRotation);
			in = VecHelper.rotate(in, -rotX, Axis.X);
			in = VecHelper.rotate(in, -rotZ, Axis.Z);
			in = VecHelper.rotate(in, -rotY, Axis.Y);
			in = in.add(centerOfRotation);
			if (stabilizationAnchor != null) {
				in = in.subtract(stabilizationAnchor);
				in = VecHelper.rotate(in, rotX, Axis.X);
				in = VecHelper.rotate(in, rotZ, Axis.Z);
				in = VecHelper.rotate(in, rotY, Axis.Y);
				in = in.add(stabilizationAnchor);
			}
		}
		return in;
	}

	public void transformMS(MatrixStack ms, float pt) {
		TransformStack.cast(ms)
				.translate(VecHelper.lerp(pt, prevAnimatedOffset, animatedOffset));
		if (!animatedRotation.equals(Vec3d.ZERO) || !prevAnimatedRotation.equals(Vec3d.ZERO)) {
			if (centerOfRotation == null)
				centerOfRotation = section.getCenter();
			double rotX = MathHelper.lerp(pt, prevAnimatedRotation.x, animatedRotation.x);
			double rotZ = MathHelper.lerp(pt, prevAnimatedRotation.z, animatedRotation.z);
			double rotY = MathHelper.lerp(pt, prevAnimatedRotation.y, animatedRotation.y);
			TransformStack.cast(ms)
					.translate(centerOfRotation)
					.rotateX(rotX)
					.rotateZ(rotZ)
					.rotateY(rotY)
					.translateBack(centerOfRotation);
			if (stabilizationAnchor != null) {
				TransformStack.cast(ms)
						.translate(stabilizationAnchor)
						.rotateX(-rotX)
						.rotateZ(-rotZ)
						.rotateY(-rotY)
						.translateBack(stabilizationAnchor);
			}
		}
	}

	public void tick(PonderScene scene) {
		prevAnimatedOffset = animatedOffset;
		prevAnimatedRotation = animatedRotation;
		if (!isVisible())
			return;
		loadBEsIfMissing(scene.getWorld());
		renderedBlockEntities.removeIf(be -> scene.getWorld()
				.getBlockEntity(be.getPos()) != be);
		tickableBlockEntities.removeIf(be -> scene.getWorld()
				.getBlockEntity(be.getFirst()
						.getPos()) != be.getFirst());
		tickableBlockEntities.forEach(be -> be.getSecond()
				.accept(scene.getWorld()));
	}

	@Override
	public void whileSkipping(PonderScene scene) {
		if (redraw) {
			renderedBlockEntities = null;
			tickableBlockEntities = null;
		}
		redraw = false;
	}

	protected void loadBEsIfMissing(PonderWorld world) {
		if (renderedBlockEntities != null)
			return;
		tickableBlockEntities = new ArrayList<>();
		renderedBlockEntities = new ArrayList<>();
		section.forEach(pos -> {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			BlockState blockState = world.getBlockState(pos);
			Block block = blockState.getBlock();
			if (blockEntity == null)
				return;
			if (!(block instanceof BlockEntityProvider))
				return;
			blockEntity.setCachedState(world.getBlockState(pos));
			BlockEntityTicker<?> ticker = ((BlockEntityProvider) block).getTicker(world, blockState, blockEntity.getType());
			if (ticker != null)
				addTicker(blockEntity, ticker);
			renderedBlockEntities.add(blockEntity);
		});
	}

	@SuppressWarnings("unchecked")
	private <T extends BlockEntity> void addTicker(T blockEntity, BlockEntityTicker<?> ticker) {
		tickableBlockEntities.add(Pair.of(blockEntity, w -> ((BlockEntityTicker<T>) ticker).tick(w,
				blockEntity.getPos(), blockEntity.getCachedState(), blockEntity)));
	}

	@Override
	public void renderFirst(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float fade, float pt) {
		int light = -1;
		if (fade != 1)
			light = (int) (MathHelper.lerp(fade, 5, 14));
		if (redraw) {
			renderedBlockEntities = null;
			tickableBlockEntities = null;
		}

		ms.push();
		transformMS(ms, pt);
		world.pushFakeLight(light);
		renderBlockEntities(world, ms, buffer, pt);
		world.popLight();

		Map<BlockPos, Integer> blockBreakingProgressions = world.getBlockBreakingProgressions();
		MatrixStack overlayMS = null;

		BlockRenderManager renderer = MinecraftClient.getInstance().getBlockRenderManager();
		for (Entry<BlockPos, Integer> entry : blockBreakingProgressions.entrySet()) {
			BlockPos pos = entry.getKey();
			if (!section.test(pos))
				continue;

			if (overlayMS == null) {
				overlayMS = new MatrixStack();
				overlayMS.peek().getPositionMatrix().set(ms.peek().getPositionMatrix());
				overlayMS.peek().getNormalMatrix().set(ms.peek().getNormalMatrix());
			}

			VertexConsumer builder = new OverlayVertexConsumer(
					buffer.getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(entry.getValue())), overlayMS.peek()
					.getPositionMatrix(),
					overlayMS.peek()
							.getNormalMatrix(),
				1);

			ms.push();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());
			renderer
				.renderDamage(world.getBlockState(pos), pos, world, ms, builder);
			ms.pop();
		}

		ms.pop();
	}

	@Override
	protected void renderLayer(PonderWorld world, VertexConsumerProvider buffer, RenderLayer type, MatrixStack ms, float fade,
							   float pt) {
		SuperByteBufferCache bufferCache = CreateClient.BUFFER_CACHE;

		int code = hashCode() ^ world.hashCode();
		Pair<Integer, Integer> key = Pair.of(code, RenderLayer.getBlockLayers()
				.indexOf(type));

		if (redraw)
			bufferCache.invalidate(DOC_WORLD_SECTION, key);
		SuperByteBuffer contraptionBuffer =
				bufferCache.get(DOC_WORLD_SECTION, key, () -> buildStructureBuffer(world, type));
		if (contraptionBuffer.isEmpty())
			return;

		transformMS(contraptionBuffer.getTransforms(), pt);
		int light = lightCoordsFromFade(fade);
		contraptionBuffer
				.light(light)
				.renderInto(ms, buffer.getBuffer(type));
	}

	@Override
	protected void renderLast(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float fade, float pt) {
		redraw = false;
		if (selectedBlock == null)
			return;
		BlockState blockState = world.getBlockState(selectedBlock);
		if (blockState.isAir())
			return;
		VoxelShape shape =
				blockState.getOutlineShape(world, selectedBlock, ShapeContext.of(MinecraftClient.getInstance().player));
		if (shape.isEmpty())
			return;

		ms.push();
		transformMS(ms, pt);
		ms.translate(selectedBlock.getX(), selectedBlock.getY(), selectedBlock.getZ());

		AABBOutline aabbOutline = new AABBOutline(shape.getBoundingBox());
		aabbOutline.getParams()
				.lineWidth(1 / 64f)
				.colored(0xefefef)
				.disableLineNormals();
		aabbOutline.render(ms, (SuperRenderTypeBuffer) buffer, Vec3d.ZERO, pt);

		ms.pop();
	}

	private void renderBlockEntities(PonderWorld world, MatrixStack ms, VertexConsumerProvider buffer, float pt) {
		loadBEsIfMissing(world);
		BlockEntityRenderHelper.renderBlockEntities(world, renderedBlockEntities, ms, buffer, pt);
	}

	private SuperByteBuffer buildStructureBuffer(PonderWorld world, RenderLayer layer) {
		BlockRenderManager dispatcher = MinecraftClient.getInstance().getBlockRenderManager();
		ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();

		MatrixStack poseStack = objects.poseStack;
		Random random = objects.random;
		ShadeSeparatingVertexConsumer shadeSeparatingWrapper = objects.shadeSeparatingWrapper;
		BufferBuilder shadedBuilder = objects.shadedBuilder;
		BufferBuilder unshadedBuilder = objects.unshadedBuilder;

		shadedBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
		unshadedBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
		shadeSeparatingWrapper.prepare(shadedBuilder, unshadedBuilder);

		world.setMask(this.section);
		BlockModelRenderer.enableBrightnessCache();
		section.forEach(pos -> {
			BlockState state = world.getBlockState(pos);
			FluidState fluidState = world.getFluidState(pos);

			poseStack.push();
			poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

			if (state.getRenderType() == BlockRenderType.MODEL) {
				BakedModel model = dispatcher.getModel(state);
				if (model.isVanillaAdapter()) {
					if (RenderLayers.getBlockLayer(state) != layer) {
						model = null;
					}
				} else {
					model = LayerFilteringBakedModel.wrap(model, layer);
				}
				if (model != null) {
					model = shadeSeparatingWrapper.wrapModel(model);
					dispatcher.getModelRenderer()
						.render(world, model, state, pos, poseStack, shadeSeparatingWrapper, true, random, state.getRenderingSeed(pos), OverlayTexture.DEFAULT_UV);
				}
			}

			if (!fluidState.isEmpty() && RenderLayers.getFluidLayer(fluidState) == layer)
				dispatcher.renderFluid(pos, world, shadedBuilder, state, fluidState);

			poseStack.pop();
		});
		BlockModelRenderer.disableBrightnessCache();
		world.clearMask();

		shadeSeparatingWrapper.clear();
		ShadeSeparatedBufferedData bufferedData = ModelUtil.endAndCombine(shadedBuilder, unshadedBuilder);

		SuperByteBuffer sbb = new SuperByteBuffer(bufferedData);
		bufferedData.release();
		return sbb;
	}

	private static class ThreadLocalObjects {
		public final MatrixStack poseStack = new MatrixStack();
		public final Random random = Random.createLocal();
		public final ShadeSeparatingVertexConsumer shadeSeparatingWrapper = new ShadeSeparatingVertexConsumer();
		public final BufferBuilder shadedBuilder = new BufferBuilder(512);
		public final BufferBuilder unshadedBuilder = new BufferBuilder(512);
	}

}
