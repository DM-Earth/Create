package com.simibubi.create.foundation.ponder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.jozufozu.flywheel.util.DiffuseLightCalculator;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.outliner.Outliner;
import com.simibubi.create.foundation.ponder.element.PonderElement;
import com.simibubi.create.foundation.ponder.element.PonderOverlayElement;
import com.simibubi.create.foundation.ponder.element.PonderSceneElement;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;
import com.simibubi.create.foundation.ponder.instruction.HideAllInstruction;
import com.simibubi.create.foundation.ponder.instruction.PonderInstruction;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
import com.simibubi.create.foundation.render.ForcedDiffuseState;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.infrastructure.ponder.PonderIndex;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class PonderScene {

	public static final String TITLE_KEY = "header";

	private boolean finished;
//	private int sceneIndex;
	private int textIndex;
	Identifier sceneId;

	private IntList keyframeTimes;

	List<PonderInstruction> schedule;
	private List<PonderInstruction> activeSchedule;
	private Map<UUID, PonderElement> linkedElements;
	private Set<PonderElement> elements;
	private List<PonderTag> tags;

	private PonderWorld world;
	private String namespace;
	private Identifier component;
	private SceneTransform transform;
	private SceneCamera camera;
	private Outliner outliner;
//	private String defaultTitle;

	private Vec3d pointOfInterest;
	private Vec3d chasingPointOfInterest;
	private WorldSectionElement baseWorldSection;
	@Nullable
	private Entity renderViewEntity;

	int basePlateOffsetX;
	int basePlateOffsetZ;
	int basePlateSize;
	float scaleFactor;
	float yOffset;
	boolean hidePlatformShadow;

	private boolean stoppedCounting;
	private int totalTime;
	private int currentTime;

	public PonderScene(PonderWorld world, String namespace, Identifier component, Collection<PonderTag> tags) {
		if (world != null)
			world.scene = this;

		pointOfInterest = Vec3d.ZERO;
		textIndex = 1;
		hidePlatformShadow = false;

		this.world = world;
		this.namespace = namespace;
		this.component = component;

		outliner = new Outliner();
		elements = new HashSet<>();
		linkedElements = new HashMap<>();
		this.tags = new ArrayList<>(tags);
		schedule = new ArrayList<>();
		activeSchedule = new ArrayList<>();
		transform = new SceneTransform();
		basePlateSize = getBounds().getBlockCountX();
		camera = new SceneCamera();
		baseWorldSection = new WorldSectionElement();
		renderViewEntity = world != null ? new ArmorStandEntity(world, 0, 0, 0) : null;
		keyframeTimes = new IntArrayList(4);
		scaleFactor = 1;
		yOffset = 0;

		setPointOfInterest(new Vec3d(0, 4, 0));
	}

	public void deselect() {
		forEach(WorldSectionElement.class, WorldSectionElement::resetSelectedBlock);
	}

	public Pair<ItemStack, BlockPos> rayTraceScene(Vec3d from, Vec3d to) {
		MutableObject<Pair<WorldSectionElement, Pair<Vec3d, BlockHitResult>>> nearestHit = new MutableObject<>();
		MutableDouble bestDistance = new MutableDouble(0);

		forEach(WorldSectionElement.class, wse -> {
			wse.resetSelectedBlock();
			if (!wse.isVisible())
				return;
			Pair<Vec3d, BlockHitResult> rayTrace = wse.rayTrace(world, from, to);
			if (rayTrace == null)
				return;
			double distanceTo = rayTrace.getFirst()
				.distanceTo(from);
			if (nearestHit.getValue() != null && distanceTo >= bestDistance.getValue())
				return;

			nearestHit.setValue(Pair.of(wse, rayTrace));
			bestDistance.setValue(distanceTo);
		});

		if (nearestHit.getValue() == null)
			return Pair.of(ItemStack.EMPTY, null);

		Pair<Vec3d, BlockHitResult> selectedHit = nearestHit.getValue()
			.getSecond();
		BlockPos selectedPos = selectedHit.getSecond()
			.getBlockPos();

		BlockPos origin = new BlockPos(basePlateOffsetX, 0, basePlateOffsetZ);
		if (!world.getBounds()
			.contains(selectedPos))
			return Pair.of(ItemStack.EMPTY, null);
		if (BlockBox.create(origin, origin.add(new Vec3i(basePlateSize - 1, 0, basePlateSize - 1)))
			.contains(selectedPos)) {
			if (PonderIndex.editingModeActive())
				nearestHit.getValue()
					.getFirst()
					.selectBlock(selectedPos);
			return Pair.of(ItemStack.EMPTY, selectedPos);
		}

		nearestHit.getValue()
			.getFirst()
			.selectBlock(selectedPos);
		BlockState blockState = world.getBlockState(selectedPos);

		Direction direction = selectedHit.getSecond()
			.getSide();
		Vec3d location = selectedHit.getSecond()
			.getPos();		ItemStack pickBlock;

		if (blockState instanceof BlockPickInteractionAware) {
			pickBlock = ((BlockPickInteractionAware) blockState).getPickedStack(blockState, world, selectedPos, MinecraftClient.getInstance().player, new BlockHitResult(VecHelper.getCenterOf(selectedPos), Direction.UP, selectedPos, true));
		} else {
			pickBlock = blockState.getBlock().getPickStack(world, selectedPos, blockState);
		}

//		= blockState.getCloneItemStack(
//			new BlockHitResult(location, direction, selectedPos, true), world, selectedPos,
//			Minecraft.getInstance().player);

		return Pair.of(pickBlock, selectedPos);
	}

	public void reset() {
		currentTime = 0;
		activeSchedule.clear();
		schedule.forEach(mdi -> mdi.reset(this));
	}

	public void begin() {
		reset();
		forEach(pe -> pe.reset(this));

		world.restore();
		elements.clear();
		linkedElements.clear();
		keyframeTimes.clear();

		transform = new SceneTransform();
		finished = false;
		setPointOfInterest(new Vec3d(0, 4, 0));

		baseWorldSection.setEmpty();
		baseWorldSection.forceApplyFade(1);
		elements.add(baseWorldSection);

		totalTime = 0;
		stoppedCounting = false;
		activeSchedule.addAll(schedule);
		activeSchedule.forEach(i -> i.onScheduled(this));
	}

	public WorldSectionElement getBaseWorldSection() {
		return baseWorldSection;
	}

	public float getSceneProgress() {
		return totalTime == 0 ? 0 : currentTime / (float) totalTime;
	}

	public void fadeOut() {
		reset();
		activeSchedule.add(new HideAllInstruction(10, null));
	}

	public void renderScene(SuperRenderTypeBuffer buffer, MatrixStack ms, float pt) {
		ForcedDiffuseState.pushCalculator(DiffuseLightCalculator.DEFAULT);
		ms.push();

		MinecraftClient mc = MinecraftClient.getInstance();
		Entity prevRVE = mc.cameraEntity;

		mc.cameraEntity = this.renderViewEntity;
		forEachVisible(PonderSceneElement.class, e -> e.renderFirst(world, buffer, ms, pt));
		mc.cameraEntity = prevRVE;

		for (RenderLayer type : RenderLayer.getBlockLayers())
			forEachVisible(PonderSceneElement.class, e -> e.renderLayer(world, buffer, type, ms, pt));

		forEachVisible(PonderSceneElement.class, e -> e.renderLast(world, buffer, ms, pt));
		camera.set(transform.xRotation.getValue(pt) + 90, transform.yRotation.getValue(pt) + 180);
		world.renderEntities(ms, buffer, camera, pt);
		world.renderParticles(ms, buffer, camera, pt);
		outliner.renderOutlines(ms, buffer, Vec3d.ZERO, pt);

		ms.pop();
		ForcedDiffuseState.popCalculator();
	}

	public void renderOverlay(PonderUI screen, DrawContext graphics, float partialTicks) {
		MatrixStack ms = graphics.getMatrices();
		ms.push();
		forEachVisible(PonderOverlayElement.class, e -> e.render(this, screen, graphics, partialTicks));
		ms.pop();
	}

	public void setPointOfInterest(Vec3d poi) {
		if (chasingPointOfInterest == null)
			pointOfInterest = poi;
		chasingPointOfInterest = poi;
	}

	public Vec3d getPointOfInterest() {
		return pointOfInterest;
	}

	public void tick() {
		if (chasingPointOfInterest != null)
			pointOfInterest = VecHelper.lerp(.25f, pointOfInterest, chasingPointOfInterest);

		outliner.tickOutlines();
		world.tick();
		transform.tick();
		forEach(e -> e.tick(this));

		if (currentTime < totalTime)
			currentTime++;

		for (Iterator<PonderInstruction> iterator = activeSchedule.iterator(); iterator.hasNext();) {
			PonderInstruction instruction = iterator.next();
			instruction.tick(this);
			if (instruction.isComplete()) {
				iterator.remove();
				if (instruction.isBlocking())
					break;
				continue;
			}
			if (instruction.isBlocking())
				break;
		}

		if (activeSchedule.isEmpty())
			finished = true;
	}

	public void seekToTime(int time) {
		if (time < currentTime)
			throw new IllegalStateException("Cannot seek backwards. Rewind first.");

		while (currentTime < time && !finished) {
			forEach(e -> e.whileSkipping(this));
			tick();
		}

		forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
	}

	public void addToSceneTime(int time) {
		if (!stoppedCounting)
			totalTime += time;
	}

	public void stopCounting() {
		stoppedCounting = true;
	}

	public void markKeyframe(int offset) {
		if (!stoppedCounting)
			keyframeTimes.add(totalTime + offset);
	}

	public void addElement(PonderElement e) {
		elements.add(e);
	}

	public <E extends PonderElement> void linkElement(E e, ElementLink<E> link) {
		linkedElements.put(link.getId(), e);
	}

	public <E extends PonderElement> E resolve(ElementLink<E> link) {
		return link.cast(linkedElements.get(link.getId()));
	}

	public <E extends PonderElement> void runWith(ElementLink<E> link, Consumer<E> callback) {
		callback.accept(resolve(link));
	}

	public <E extends PonderElement, F> F applyTo(ElementLink<E> link, Function<E, F> function) {
		return function.apply(resolve(link));
	}

	public void forEach(Consumer<? super PonderElement> function) {
		for (PonderElement elemtent : elements)
			function.accept(elemtent);
	}

	public <T extends PonderElement> void forEach(Class<T> type, Consumer<T> function) {
		for (PonderElement element : elements)
			if (type.isInstance(element))
				function.accept(type.cast(element));
	}

	public <T extends PonderElement> void forEachVisible(Class<T> type, Consumer<T> function) {
		for (PonderElement element : elements)
			if (type.isInstance(element) && element.isVisible())
				function.accept(type.cast(element));
	}

	public <T extends Entity> void forEachWorldEntity(Class<T> type, Consumer<T> function) {
		world.getEntityStream()
			.filter(type::isInstance)
			.map(type::cast)
			.forEach(function);
		/*
		 * for (Entity element : world.getEntities()) { if (type.isInstance(element))
		 * function.accept(type.cast(element)); }
		 */
	}

	public Supplier<String> registerText(String defaultText) {
		final String key = "text_" + textIndex;
		PonderLocalization.registerSpecific(sceneId, key, defaultText);
		Supplier<String> supplier = () -> PonderLocalization.getSpecific(sceneId, key);
		textIndex++;
		return supplier;
	}

	public SceneBuilder builder() {
		return new SceneBuilder(this);
	}

	public SceneBuildingUtil getSceneBuildingUtil() {
		return new SceneBuildingUtil(getBounds());
	}

	public String getTitle() {
		return getString(TITLE_KEY);
	}

	public String getString(String key) {
		return PonderLocalization.getSpecific(sceneId, key);
	}

	public PonderWorld getWorld() {
		return world;
	}

	public String getNamespace() {
		return namespace;
	}

	public int getKeyframeCount() {
		return keyframeTimes.size();
	}

	public int getKeyframeTime(int index) {
		return keyframeTimes.getInt(index);
	}

	public List<PonderTag> getTags() {
		return tags;
	}

	public Identifier getComponent() {
		return component;
	}

	public Set<PonderElement> getElements() {
		return elements;
	}

	public BlockBox getBounds() {
		return world == null ? new BlockBox(BlockPos.ORIGIN) : world.getBounds();
	}

	public Identifier getId() {
		return sceneId;
	}

	public SceneTransform getTransform() {
		return transform;
	}

	public Outliner getOutliner() {
		return outliner;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public int getBasePlateOffsetX() {
		return basePlateOffsetX;
	}

	public int getBasePlateOffsetZ() {
		return basePlateOffsetZ;
	}

	public boolean shouldHidePlatformShadow() {
		return hidePlatformShadow;
	}

	public int getBasePlateSize() {
		return basePlateSize;
	}

	public float getScaleFactor() {
		return scaleFactor;
	}

	public float getYOffset() {
		return yOffset;
	}

	public int getTotalTime() {
		return totalTime;
	}

	public int getCurrentTime() {
		return currentTime;
	}

	public class SceneTransform {

		public LerpedFloat xRotation, yRotation;

		// Screen params
		private int width, height;
		private double offset;
		private Matrix4f cachedMat;

		public SceneTransform() {
			xRotation = LerpedFloat.angular()
				.disableSmartAngleChasing()
				.startWithValue(-35);
			yRotation = LerpedFloat.angular()
				.disableSmartAngleChasing()
				.startWithValue(55 + 90);
		}

		public void tick() {
			xRotation.tickChaser();
			yRotation.tickChaser();
		}

		public void updateScreenParams(int width, int height, double offset) {
			this.width = width;
			this.height = height;
			this.offset = offset;
			cachedMat = null;
		}

		public MatrixStack apply(MatrixStack ms) {
			return apply(ms, AnimationTickHolder.getPartialTicks(world));
		}

		public MatrixStack apply(MatrixStack ms, float pt) {
			ms.translate(width / 2, height / 2, 200 + offset);

			TransformStack.cast(ms)
				.rotateX(-35)
				.rotateY(55)
				.translate(offset, 0, 0)
				.rotateY(-55)
				.rotateX(35)
				.rotateX(xRotation.getValue(pt))
				.rotateY(yRotation.getValue(pt));

			UIRenderHelper.flipForGuiRender(ms);
			float f = 30 * scaleFactor;
			ms.scale(f, f, f);
			ms.translate((basePlateSize) / -2f - basePlateOffsetX, -1f + yOffset,
				(basePlateSize) / -2f - basePlateOffsetZ);

			return ms;
		}

		public void updateSceneRVE(float pt) {
			Vec3d v = screenToScene(width / 2, height / 2, 500, pt);
			if (renderViewEntity != null)
				renderViewEntity.setPosition(v.x, v.y, v.z);
		}

		public Vec3d screenToScene(double x, double y, int depth, float pt) {
			refreshMatrix(pt);
			Vec3d vec = new Vec3d(x, y, depth);

			vec = vec.subtract(width / 2, height / 2, 200 + offset);
			vec = VecHelper.rotate(vec, 35, Axis.X);
			vec = VecHelper.rotate(vec, -55, Axis.Y);
			vec = vec.subtract(offset, 0, 0);
			vec = VecHelper.rotate(vec, 55, Axis.Y);
			vec = VecHelper.rotate(vec, -35, Axis.X);
			vec = VecHelper.rotate(vec, -xRotation.getValue(pt), Axis.X);
			vec = VecHelper.rotate(vec, -yRotation.getValue(pt), Axis.Y);

			float f = 1f / (30 * scaleFactor);

			vec = vec.multiply(f, -f, f);
			vec = vec.subtract((basePlateSize) / -2f - basePlateOffsetX, -1f + yOffset,
				(basePlateSize) / -2f - basePlateOffsetZ);

			return vec;
		}

		public Vec2f sceneToScreen(Vec3d vec, float pt) {
			refreshMatrix(pt);
			Vector4f vec4 = new Vector4f((float) vec.x, (float) vec.y, (float) vec.z, 1);
			vec4.mul(cachedMat);
			return new Vec2f(vec4.x(), vec4.y());
		}

		protected void refreshMatrix(float pt) {
			if (cachedMat != null)
				return;
			cachedMat = apply(new MatrixStack(), pt).peek()
				.getPositionMatrix();
		}

	}

	public class SceneCamera extends Camera {

		public void set(float xRotation, float yRotation) {
			setRotation(yRotation, xRotation);
		}

	}

}
