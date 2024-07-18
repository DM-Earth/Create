package com.simibubi.create.foundation.ponder.ui;

import static com.simibubi.create.foundation.ponder.PonderLocalization.LANG_PREFIX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.ScreenAccessor;

import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.element.BoxElement;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.ponder.FabricPonderProcessing;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.ponder.PonderChapter;
import com.simibubi.create.foundation.ponder.PonderRegistry;
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.PonderScene.SceneTransform;
import com.simibubi.create.foundation.ponder.PonderStoryBoardEntry;
import com.simibubi.create.foundation.ponder.PonderTag;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.ponder.element.TextWindowElement;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.FontHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.Pointing;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.ponder.DebugScenes;
import com.simibubi.create.infrastructure.ponder.PonderIndex;

import io.github.fabricators_of_create.porting_lib.util.client.ScreenUtils;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.Clipboard;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class PonderUI extends NavigatableSimiScreen {

	public static int ponderTicks;
	public static float ponderPartialTicksPaused;

	public static final String PONDERING = LANG_PREFIX + "pondering";
	public static final String IDENTIFY_MODE = LANG_PREFIX + "identify_mode";
	public static final String IN_CHAPTER = LANG_PREFIX + "in_chapter";
	public static final String IDENTIFY = LANG_PREFIX + "identify";
	public static final String PREVIOUS = LANG_PREFIX + "previous";
	public static final String CLOSE = LANG_PREFIX + "close";
	public static final String NEXT = LANG_PREFIX + "next";
	public static final String NEXT_UP = LANG_PREFIX + "next_up";
	public static final String REPLAY = LANG_PREFIX + "replay";
	public static final String SLOW_TEXT = LANG_PREFIX + "slow_text";

	private List<PonderScene> scenes;
	private List<PonderTag> tags;
	private List<PonderButton> tagButtons;
	private List<LerpedFloat> tagFades;
	private LerpedFloat fadeIn;
	ItemStack stack;
	PonderChapter chapter = null;

	private boolean userViewMode;
	private boolean identifyMode;
	private ItemStack hoveredTooltipItem;
	private BlockPos hoveredBlockPos;

	private Clipboard clipboardHelper;
	private BlockPos copiedBlockPos;

	private LerpedFloat finishingFlash;
	private LerpedFloat nextUp;
	private int finishingFlashWarmup = 0;
	private int nextUpWarmup = 0;

	private LerpedFloat lazyIndex;
	private int index = 0;
	private PonderTag referredToByTag;

	private PonderButton left, right, scan, chap, userMode, close, replay, slowMode;
	private int skipCooling = 0;

	private int extendedTickLength = 0;
	private int extendedTickTimer = 0;

	public static PonderUI of(Identifier id) {
		return new PonderUI(PonderRegistry.compile(id));
	}

	public static PonderUI of(ItemStack item) {
		return new PonderUI(PonderRegistry.compile(RegisteredObjects.getKeyOrThrow(item.getItem())));
	}

	public static PonderUI of(ItemStack item, PonderTag tag) {
		PonderUI ponderUI = new PonderUI(PonderRegistry.compile(RegisteredObjects.getKeyOrThrow(item.getItem())));
		ponderUI.referredToByTag = tag;
		return ponderUI;
	}

	public static PonderUI of(PonderChapter chapter) {
		PonderUI ui = new PonderUI(PonderRegistry.compile(chapter));
		ui.chapter = chapter;
		return ui;
	}

	protected PonderUI(List<PonderScene> scenes) {
		Identifier component = scenes.get(0)
			.getComponent();
		if (Registries.ITEM.containsId(component))
			stack = new ItemStack(Registries.ITEM.get(component));
		else
			stack = new ItemStack(Registries.BLOCK.get(component));

		tags = new ArrayList<>(PonderRegistry.TAGS.getTags(component));
		this.scenes = scenes;
		if (scenes.isEmpty()) {
			List<PonderStoryBoardEntry> l = Collections.singletonList(new PonderStoryBoardEntry(DebugScenes::empty,
				Create.ID, "debug/scene_1", new Identifier("minecraft", "stick")));
			scenes.addAll(PonderRegistry.compile(l));
		}
		lazyIndex = LerpedFloat.linear()
			.startWithValue(index);
		fadeIn = LerpedFloat.linear()
			.startWithValue(0)
			.chase(1, .1f, Chaser.EXP);
		clipboardHelper = new Clipboard();
		finishingFlash = LerpedFloat.linear()
			.startWithValue(0)
			.chase(0, .1f, Chaser.EXP);
		nextUp = LerpedFloat.linear()
			.startWithValue(0)
			.chase(0, .4f, Chaser.EXP);
	}

	@Override
	protected void init() {
		super.init();

		tagButtons = new ArrayList<>();
		tagFades = new ArrayList<>();

		tags.forEach(t -> {
			int i = tagButtons.size();
			int x = 31;
			int y = 81 + i * 30;

			PonderButton b2 = new PonderButton(x, y).showing(t)
				.withCallback((mX, mY) -> {
					centerScalingOn(mX, mY);
					ScreenOpener.transitionTo(new PonderTagScreen(t));
				});

			addDrawableChild(b2);
			tagButtons.add(b2);

			LerpedFloat chase = LerpedFloat.linear()
				.startWithValue(0)
				.chase(0, .05f, Chaser.exp(.1));
			tagFades.add(chase);

		});

		/*
		 * if (chapter != null) { widgets.add(chap = new PonderButton(width - 31 - 24,
		 * 31, () -> { }).showing(chapter)); }
		 */

		GameOptions bindings = client.options;
		int spacing = 8;
		int bX = (width - 20) / 2 - (70 + 2 * spacing);
		int bY = height - 20 - 31;

		{
			int pX = (width / 2) - 110;
			int pY = bY + 20 + 4;
			int pW = width - 2 * pX;
			addDrawableChild(new PonderProgressBar(this, pX, pY, pW, 1));
		}

		addDrawableChild(scan = new PonderButton(bX, bY).withShortcut(bindings.dropKey)
			.showing(AllIcons.I_MTD_SCAN)
			.enableFade(0, 5)
			.withCallback(() -> {
				identifyMode = !identifyMode;
				if (!identifyMode)
					scenes.get(index)
						.deselect();
				else
					ponderPartialTicksPaused = client.getTickDelta();
			}));
		scan.atZLevel(600);

		addDrawableChild(slowMode = new PonderButton(width - 20 - 31, bY).showing(AllIcons.I_MTD_SLOW_MODE)
			.enableFade(0, 5)
			.withCallback(() -> setComfyReadingEnabled(!isComfyReadingEnabled())));

		if (PonderIndex.editingModeActive()) {
			addDrawableChild(userMode = new PonderButton(width - 50 - 31, bY).showing(AllIcons.I_MTD_USER_MODE)
				.enableFade(0, 5)
				.withCallback(() -> userViewMode = !userViewMode));
		}

		bX += 50 + spacing;
		addDrawableChild(left = new PonderButton(bX, bY).withShortcut(bindings.leftKey)
			.showing(AllIcons.I_MTD_LEFT)
			.enableFade(0, 5)
			.withCallback(() -> this.scroll(false)));

		bX += 20 + spacing;
		addDrawableChild(close = new PonderButton(bX, bY).withShortcut(bindings.inventoryKey)
			.showing(AllIcons.I_MTD_CLOSE)
			.enableFade(0, 5)
			.withCallback(this::close));

		bX += 20 + spacing;
		addDrawableChild(right = new PonderButton(bX, bY).withShortcut(bindings.rightKey)
			.showing(AllIcons.I_MTD_RIGHT)
			.enableFade(0, 5)
			.withCallback(() -> this.scroll(true)));

		bX += 50 + spacing;
		addDrawableChild(replay = new PonderButton(bX, bY).withShortcut(bindings.backKey)
			.showing(AllIcons.I_MTD_REPLAY)
			.enableFade(0, 5)
			.withCallback(this::replay));
	}

	@Override
	protected void initBackTrackIcon(PonderButton backTrack) {
		backTrack.showing(stack);
	}

	@Override
	public void tick() {
		super.tick();

		if (skipCooling > 0)
			skipCooling--;

		if (referredToByTag != null) {
			for (int i = 0; i < scenes.size(); i++) {
				PonderScene ponderScene = scenes.get(i);
				if (!ponderScene.getTags()
					.contains(referredToByTag))
					continue;
				if (i == index)
					break;
				scenes.get(index)
					.fadeOut();
				index = i;
				scenes.get(index)
					.begin();
				lazyIndex.chase(index, 1 / 4f, Chaser.EXP);
				identifyMode = false;
				break;
			}
			referredToByTag = null;
		}

		lazyIndex.tickChaser();
		fadeIn.tickChaser();
		finishingFlash.tickChaser();
		nextUp.tickChaser();
		PonderScene activeScene = scenes.get(index);

		extendedTickLength = 0;
		if (isComfyReadingEnabled())
			activeScene.forEachVisible(TextWindowElement.class, twe -> extendedTickLength = 2);

		if (extendedTickTimer == 0) {
			if (!identifyMode) {
				ponderTicks++;
				if (skipCooling == 0)
					activeScene.tick();
			}

			if (!identifyMode) {
				float lazyIndexValue = lazyIndex.getValue();
				if (Math.abs(lazyIndexValue - index) > 1 / 512f)
					scenes.get(lazyIndexValue < index ? index - 1 : index + 1)
						.tick();
			}
			extendedTickTimer = extendedTickLength;
		} else
			extendedTickTimer--;

		if (activeScene.getCurrentTime() == activeScene.getTotalTime() - 1) {
			finishingFlashWarmup = 30;
			nextUpWarmup = 50;
		}

		if (finishingFlashWarmup > 0) {
			finishingFlashWarmup--;
			if (finishingFlashWarmup == 0) {
				finishingFlash.setValue(1);
				finishingFlash.setValue(1);
			}
		}

		if (nextUpWarmup > 0) {
			nextUpWarmup--;
			if (nextUpWarmup == 0)
				nextUp.updateChaseTarget(1);
		}

		updateIdentifiedItem(activeScene);
	}

	public PonderScene getActiveScene() {
		return scenes.get(index);
	}

	public void seekToTime(int time) {
		if (getActiveScene().getCurrentTime() > time)
			replay();

		getActiveScene().seekToTime(time);
		if (time != 0)
			coolDownAfterSkip();
	}

	public void updateIdentifiedItem(PonderScene activeScene) {
		hoveredTooltipItem = ItemStack.EMPTY;
		hoveredBlockPos = null;
		if (!identifyMode)
			return;

		Window w = client.getWindow();
		double mouseX = client.mouse.getX() * w.getScaledWidth() / w.getWidth();
		double mouseY = client.mouse.getY() * w.getScaledHeight() / w.getHeight();
		SceneTransform t = activeScene.getTransform();
		Vec3d vec1 = t.screenToScene(mouseX, mouseY, 1000, 0);
		Vec3d vec2 = t.screenToScene(mouseX, mouseY, -100, 0);
		Pair<ItemStack, BlockPos> pair = activeScene.rayTraceScene(vec1, vec2);
		hoveredTooltipItem = pair.getFirst();
		hoveredBlockPos = pair.getSecond();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (scroll(delta > 0))
			return true;
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	protected void replay() {
		identifyMode = false;
		PonderScene scene = scenes.get(index);

		if (hasShiftDown()) {
			List<PonderStoryBoardEntry> list = PonderRegistry.ALL.get(scene.getComponent());
			PonderStoryBoardEntry sb = list.get(index);
			Identifier id = sb.getSchematicLocation();
			StructureTemplate activeTemplate = PonderRegistry.loadSchematic(id);
			PonderWorld world = new PonderWorld(BlockPos.ORIGIN, MinecraftClient.getInstance().world);
			StructurePlacementData settings = FabricPonderProcessing.makePlaceSettings(id);
			activeTemplate.place(world, BlockPos.ORIGIN, BlockPos.ORIGIN, settings, Random.create(),
				Block.NOTIFY_LISTENERS);
			world.createBackup();
			scene = PonderRegistry.compileScene(index, sb, world);
			scene.begin();
			scenes.set(index, scene);
		}

		scene.begin();
	}

	protected boolean scroll(boolean forward) {
		int prevIndex = index;
		index = forward ? index + 1 : index - 1;
		index = MathHelper.clamp(index, 0, scenes.size() - 1);
		if (prevIndex != index) {// && Math.abs(index - lazyIndex.getValue()) < 1.5f) {
			scenes.get(prevIndex)
				.fadeOut();
			scenes.get(index)
				.begin();
			lazyIndex.chase(index, 1 / 4f, Chaser.EXP);
			identifyMode = false;
			return true;
		} else
			index = prevIndex;
		return false;
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		partialTicks = getPartialTicks();
		RenderSystem.enableBlend();
		renderVisibleScenes(graphics, mouseX, mouseY,
			skipCooling > 0 ? 0 : identifyMode ? ponderPartialTicksPaused : partialTicks);
		renderWidgets(graphics, mouseX, mouseY, identifyMode ? ponderPartialTicksPaused : partialTicks);
	}

	@Override
	public void renderBackground(DrawContext graphics) {
		super.renderBackground(graphics);
	}

	protected void renderVisibleScenes(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		renderScene(graphics, mouseX, mouseY, index, partialTicks);
		float lazyIndexValue = lazyIndex.getValue(partialTicks);
		if (Math.abs(lazyIndexValue - index) > 1 / 512f)
			renderScene(graphics, mouseX, mouseY, lazyIndexValue < index ? index - 1 : index + 1, partialTicks);
	}

	protected void renderScene(DrawContext graphics, int mouseX, int mouseY, int i, float partialTicks) {
		SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
		PonderScene scene = scenes.get(i);
		double value = lazyIndex.getValue(client.getTickDelta());
		double diff = i - value;
		double slide = MathHelper.lerp(diff * diff, 200, 600) * diff;

		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.backupProjectionMatrix();

		// has to be outside of MS transforms, important for vertex sorting
		Matrix4f matrix4f = new Matrix4f(RenderSystem.getProjectionMatrix());
		matrix4f.translate(0, 0, 800);
		RenderSystem.setProjectionMatrix(matrix4f, VertexSorter.BY_DISTANCE);

		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(0, 0, -800);

		scene.getTransform()
			.updateScreenParams(width, height, slide);
		scene.getTransform()
			.apply(ms, partialTicks);

//		ms.translate(-story.getBasePlateOffsetX() * .5, 0, -story.getBasePlateOffsetZ() * .5);

		scene.getTransform()
			.updateSceneRVE(partialTicks);

		scene.renderScene(buffer, ms, partialTicks);
		buffer.draw();

		BlockBox bounds = scene.getBounds();
		ms.push();

		// kool shadow fx
		if (!scene.shouldHidePlatformShadow()) {
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();
			ms.push();
			ms.translate(scene.getBasePlateOffsetX(), 0, scene.getBasePlateOffsetZ());
			UIRenderHelper.flipForGuiRender(ms);

			float flash = finishingFlash.getValue(partialTicks) * .9f;
			float alpha = flash;
			flash *= flash;
			flash = ((flash * 2) - 1);
			flash *= flash;
			flash = 1 - flash;

			for (int f = 0; f < 4; f++) {
				ms.translate(scene.getBasePlateSize(), 0, 0);
				ms.push();
				ms.translate(0, 0, -1 / 1024f);
				if (flash > 0) {
					ms.push();
					ms.scale(1, .5f + flash * .75f, 1);
					graphics.fillGradient(0, -1, -scene.getBasePlateSize(), 0, 0, 0x00_c6ffc9,
						new Color(0xaa_c6ffc9).scaleAlpha(alpha)
							.getRGB());
					ms.pop();
				}
				ms.translate(0, 0, 2 / 1024f);
				graphics.fillGradient(0, 0, -scene.getBasePlateSize(), 4, 0, 0x66_000000, 0x00_000000);
				ms.pop();
				ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
			}
			ms.pop();
			RenderSystem.disableCull();
			RenderSystem.disableDepthTest();
		}

		// coords for debug
		if (PonderIndex.editingModeActive() && !userViewMode) {

			ms.scale(-1, -1, 1);
			ms.scale(1 / 16f, 1 / 16f, 1 / 16f);
			ms.translate(1, -8, -1 / 64f);

			// X AXIS
			ms.push();
			ms.translate(4, -3, 0);
			ms.translate(0, 0, -2 / 1024f);
			for (int x = 0; x <= bounds.getBlockCountX(); x++) {
				ms.translate(-16, 0, 0);
				graphics.drawText(textRenderer, x == bounds.getBlockCountX() ? "x" : "" + x, 0, 0, 0xFFFFFFFF, false);
			}
			ms.pop();

			// Z AXIS
			ms.push();
			ms.scale(-1, 1, 1);
			ms.translate(0, -3, -4);
			ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
			ms.translate(-8, -2, 2 / 64f);
			for (int z = 0; z <= bounds.getBlockCountZ(); z++) {
				ms.translate(16, 0, 0);
				graphics.drawText(textRenderer, z == bounds.getBlockCountZ() ? "z" : "" + z, 0, 0, 0xFFFFFFFF, false);
			}
			ms.pop();

			// DIRECTIONS
			ms.push();
			ms.translate(bounds.getBlockCountX() * -8, 0, bounds.getBlockCountZ() * 8);
			ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
			for (Direction d : Iterate.horizontalDirections) {
				ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
				ms.push();
				ms.translate(0, 0, bounds.getBlockCountZ() * 16);
				ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
				graphics.drawText(textRenderer, d.name()
					.substring(0, 1), 0, 0, 0x66FFFFFF, false);
				graphics.drawText(textRenderer, "|", 2, 10, 0x44FFFFFF, false);
				graphics.drawText(textRenderer, ".", 2, 14, 0x22FFFFFF, false);
				ms.pop();
			}
			ms.pop();
			buffer.draw();
		}

		ms.pop();
		ms.pop();
		RenderSystem.restoreProjectionMatrix();
	}

	protected void renderWidgets(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		RenderSystem.disableDepthTest();

		float fade = fadeIn.getValue(partialTicks);
		float lazyIndexValue = lazyIndex.getValue(partialTicks);
		float indexDiff = Math.abs(lazyIndexValue - index);
		PonderScene activeScene = scenes.get(index);
		PonderScene nextScene = scenes.size() > index + 1 ? scenes.get(index + 1) : null;

		boolean noWidgetsHovered = true;
		for (Element child : children())
			noWidgetsHovered &= !child.isMouseOver(mouseX, mouseY);

		int tooltipColor = Theme.i(Theme.Key.TEXT_DARKER);
		renderChapterTitle(graphics, fade, indexDiff, activeScene, tooltipColor);
		renderNavigationMenu(graphics);

		MatrixStack ms = graphics.getMatrices();

		if (identifyMode) {
			if (noWidgetsHovered && mouseY < height - 80) {
				ms.push();
				ms.translate(mouseX, mouseY, 100);
				if (hoveredTooltipItem.isEmpty()) {
					MutableText text = Lang
						.translateDirect(IDENTIFY_MODE,
							((MutableText) client.options.dropKey.getBoundKeyLocalizedText())
								.formatted(Formatting.WHITE))
						.formatted(Formatting.GRAY);
					graphics.drawOrderedTooltip(textRenderer, textRenderer.wrapLines(text, width / 3), 0, 0);
				} else
					graphics.drawItemTooltip(textRenderer, hoveredTooltipItem, 0, 0);
				if (hoveredBlockPos != null && PonderIndex.editingModeActive() && !userViewMode) {
					ms.translate(0, -15, 0);
					boolean copied = copiedBlockPos != null && hoveredBlockPos.equals(copiedBlockPos);
					MutableText coords = Components.literal(
						hoveredBlockPos.getX() + ", " + hoveredBlockPos.getY() + ", " + hoveredBlockPos.getZ())
							.formatted(copied ? Formatting.GREEN : Formatting.GOLD);
					graphics.drawTooltip(textRenderer, coords, 0, 0);
				}
				ms.pop();
			}
			scan.flash();
		} else {
			scan.dim();
		}

		if (PonderIndex.editingModeActive()) {
			if (userViewMode)
				userMode.flash();
			else
				userMode.dim();
		}

		if (isComfyReadingEnabled())
			slowMode.flash();
		else
			slowMode.dim();

		renderSceneOverlay(graphics, partialTicks, lazyIndexValue, indexDiff);

		boolean finished = activeScene.isFinished();

		if (finished) {
			jumpToNextScene(graphics, partialTicks, nextScene);
		}

		// Widgets
		((ScreenAccessor) this).port_lib$getRenderables().forEach(w -> {
			if (w instanceof PonderButton button) {
				button.fade()
					.startWithValue(fade);
			}
		});

		if (index == 0 || index == 1 && lazyIndexValue < index)
			left.fade()
				.startWithValue(lazyIndexValue);
		if (index == scenes.size() - 1 || index == scenes.size() - 2 && lazyIndexValue > index)
			right.fade()
				.startWithValue(scenes.size() - lazyIndexValue - 1);

		if (finished)
			right.flash();
		else {
			right.dim();
			nextUp.updateChaseTarget(0);
		}

		renderPonderTags(graphics, mouseX, mouseY, partialTicks, fade, activeScene);

		renderHoverTooltips(graphics, tooltipColor);
		RenderSystem.enableDepthTest();
	}

	protected void renderPonderTags(DrawContext graphics, int mouseX, int mouseY, float partialTicks, float fade, PonderScene activeScene) {
		MatrixStack ms = graphics.getMatrices();

		// Tags
		List<PonderTag> sceneTags = activeScene.getTags();
		boolean highlightAll = sceneTags.contains(PonderTag.HIGHLIGHT_ALL);
		double s = MinecraftClient.getInstance()
			.getWindow()
			.getScaleFactor();
		IntStream.range(0, tagButtons.size())
			.forEach(i -> {
				ms.push();
				LerpedFloat chase = tagFades.get(i);
				PonderButton button = tagButtons.get(i);
				if (button.isMouseOver(mouseX, mouseY)) {
					chase.updateChaseTarget(1);
				} else
					chase.updateChaseTarget(0);

				chase.tickChaser();

				if (highlightAll)
					button.flash();
				else
					button.dim();

				int x = button.getX() + button.getWidth() + 4;
				int y = button.getY() - 2;
				ms.translate(x, y + 5 * (1 - fade), 800);

				float fadedWidth = 200 * chase.getValue(partialTicks);
				UIRenderHelper.streak(graphics, 0, 0, 12, 26, (int) fadedWidth);

				RenderSystem.enableScissor((int) (x * s), 0, (int) (fadedWidth * s), (int) (height * s));

				String tagName = this.tags.get(i)
					.getTitle();
				graphics.drawText(textRenderer, tagName, 3, 8, Theme.i(Theme.Key.TEXT_ACCENT_SLIGHT), false);

				RenderSystem.disableScissor();

				ms.pop();
			});
	}

	protected void renderSceneOverlay(DrawContext graphics, float partialTicks, float lazyIndexValue, float indexDiff) {
		MatrixStack ms = graphics.getMatrices();

		// Scene overlay
		float scenePT = skipCooling > 0 ? 0 : partialTicks;
		ms.push();
		ms.translate(0, 0, 100);
		renderOverlay(graphics, index, scenePT);
		if (indexDiff > 1 / 512f)
			renderOverlay(graphics, lazyIndexValue < index ? index - 1 : index + 1, scenePT);
		ms.pop();
	}

	protected void jumpToNextScene(DrawContext graphics, float partialTicks, PonderScene nextScene) {
		MatrixStack ms = graphics.getMatrices();

		if (nextScene != null && nextUp.getValue() > 1 / 16f && !nextScene.getId()
				.equals(Create.asResource("creative_motor_mojang"))) {
			ms.push();
			ms.translate(right.getX() + 10, right.getY() - 6 + nextUp.getValue(partialTicks) * 5, 400);
			int boxWidth = (Math.max(textRenderer.getWidth(nextScene.getTitle()), textRenderer.getWidth(Lang.translateDirect(NEXT_UP))) + 5);
			renderSpeechBox(graphics, 0, 0, boxWidth, 20, right.isSelected(), Pointing.DOWN, false);
			ms.translate(0, -29, 100);
			graphics.drawCenteredTextWithShadow(textRenderer, Lang.translateDirect(NEXT_UP), 0, 0, Theme.i(Theme.Key.TEXT_DARKER));
			graphics.drawCenteredTextWithShadow(textRenderer, nextScene.getTitle(), 0, 10, Theme.i(Theme.Key.TEXT));
			ms.pop();
		}
	}

	protected void renderHoverTooltips(DrawContext graphics, int tooltipColor) {
		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(0, 0, 500);
		int tooltipY = height - 16;
		if (scan.isSelected())
			graphics.drawCenteredTextWithShadow(textRenderer, Lang.translateDirect(IDENTIFY), scan.getX() + 10, tooltipY, tooltipColor);
		if (index != 0 && left.isSelected())
			graphics.drawCenteredTextWithShadow(textRenderer, Lang.translateDirect(PREVIOUS), left.getX() + 10, tooltipY, tooltipColor);
		if (close.isSelected())
			graphics.drawCenteredTextWithShadow(textRenderer, Lang.translateDirect(CLOSE), close.getX() + 10, tooltipY, tooltipColor);
		if (index != scenes.size() - 1 && right.isSelected())
			graphics.drawCenteredTextWithShadow(textRenderer, Lang.translateDirect(NEXT), right.getX() + 10, tooltipY, tooltipColor);
		if (replay.isSelected())
			graphics.drawCenteredTextWithShadow(textRenderer, Lang.translateDirect(REPLAY), replay.getX() + 10, tooltipY, tooltipColor);
		if (slowMode.isSelected())
			graphics.drawCenteredTextWithShadow(textRenderer, Lang.translateDirect(SLOW_TEXT), slowMode.getX() + 5, tooltipY, tooltipColor);
		if (PonderIndex.editingModeActive() && userMode.isSelected())
			graphics.drawCenteredTextWithShadow(textRenderer, "Editor View", userMode.getX() + 10, tooltipY, tooltipColor);
		ms.pop();
	}

	protected void renderChapterTitle(DrawContext graphics, float fade, float indexDiff, PonderScene activeScene, int tooltipColor) {
		MatrixStack ms = graphics.getMatrices();

		// Chapter title
		ms.push();
		ms.translate(0, 0, 400);
		int x = 31 + 20 + 8;
		int y = 31;

		String title = activeScene.getTitle();
		int wordWrappedHeight = textRenderer.getWrappedLinesHeight(title, left.getX() - 51);

		int streakHeight = 35 - 9 + wordWrappedHeight;
		UIRenderHelper.streak(graphics, 0, x - 4, y - 12 + streakHeight / 2, streakHeight, (int) (150 * fade));
		UIRenderHelper.streak(graphics, 180, x - 4, y - 12 + streakHeight / 2, streakHeight, (int) (30 * fade));
		new BoxElement().withBackground(Theme.c(Theme.Key.PONDER_BACKGROUND_FLAT))
				.gradientBorder(Theme.p(Theme.Key.PONDER_IDLE))
				.at(21, 21, 100)
				.withBounds(30, 30)
				.render(graphics);

		GuiGameElement.of(stack)
				.scale(2)
				.at(x - 39f, y - 11f)
				.render(graphics);

		graphics.drawText(textRenderer, Lang.translateDirect(PONDERING), x, y - 6, tooltipColor, false);
		y += 8;
		x += 0;
		ms.translate(x, y, 0);
		ms.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(indexDiff * -75));
		ms.translate(0, 0, 5);
		FontHelper.drawSplitString(ms, textRenderer, title, 0, 0, left.getX() - 51, Theme.c(Theme.Key.TEXT)
				.scaleAlpha(1 - indexDiff)
				.getRGB());
		ms.pop();
		if (chapter != null) {
			ms.push();

			ms.translate(chap.getX() - 8, chap.getY(), 0);
			UIRenderHelper.streak(graphics, 180, 4, 10, 26, (int) (150 * fade));

			drawRightAlignedString(graphics, ms, Lang.translateDirect(IN_CHAPTER)
					.getString(), 0, 0, tooltipColor);
			drawRightAlignedString(graphics, ms, chapter.getTitle(), 0, 12, Theme.i(Theme.Key.TEXT));

			ms.pop();
		}
	}

	protected void renderNavigationMenu(DrawContext graphics) {
		Color c1 = Theme.c(Theme.Key.PONDER_BACK_ARROW)
			.setAlpha(0x40);
		Color c2 = Theme.c(Theme.Key.PONDER_BACK_ARROW)
			.setAlpha(0x20);
		Color c3 = Theme.c(Theme.Key.PONDER_BACK_ARROW)
			.setAlpha(0x10);
		UIRenderHelper.breadcrumbArrow(graphics, width / 2 - 20, height - 51, 0, 20, 20, 5, c1, c2);
		UIRenderHelper.breadcrumbArrow(graphics, width / 2 + 20, height - 51, 0, -20, 20, -5, c1, c2);
		UIRenderHelper.breadcrumbArrow(graphics, width / 2 - 90, height - 51, 0, 70, 20, 5, c1, c3);
		UIRenderHelper.breadcrumbArrow(graphics, width / 2 + 90, height - 51, 0, -70, 20, -5, c1, c3);
	}

	private void renderOverlay(DrawContext graphics, int i, float partialTicks) {
		if (identifyMode)
			return;
		MatrixStack ms = graphics.getMatrices();
		ms.push();
		PonderScene story = scenes.get(i);
		story.renderOverlay(this, graphics, skipCooling > 0 ? 0 : identifyMode ? ponderPartialTicksPaused : partialTicks);
		ms.pop();
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (identifyMode && hoveredBlockPos != null && PonderIndex.editingModeActive()) {
			long handle = client.getWindow()
				.getHandle();
			if (copiedBlockPos != null && button == 1) {
				clipboardHelper.setClipboard(handle,
					"util.select.fromTo(" + copiedBlockPos.getX() + ", " + copiedBlockPos.getY() + ", "
						+ copiedBlockPos.getZ() + ", " + hoveredBlockPos.getX() + ", " + hoveredBlockPos.getY() + ", "
						+ hoveredBlockPos.getZ() + ")");
				copiedBlockPos = hoveredBlockPos;
				return true;
			}

			if (hasShiftDown())
				clipboardHelper.setClipboard(handle, "util.select.position(" + hoveredBlockPos.getX() + ", "
					+ hoveredBlockPos.getY() + ", " + hoveredBlockPos.getZ() + ")");
			else
				clipboardHelper.setClipboard(handle, "util.grid.at(" + hoveredBlockPos.getX() + ", "
					+ hoveredBlockPos.getY() + ", " + hoveredBlockPos.getZ() + ")");
			copiedBlockPos = hoveredBlockPos;
			return true;
		}

		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		GameOptions settings = MinecraftClient.getInstance().options;
		int sCode = KeyBindingHelper.getBoundKeyOf(settings.backKey)
			.getCode();
		int aCode = KeyBindingHelper.getBoundKeyOf(settings.leftKey)
			.getCode();
		int dCode = KeyBindingHelper.getBoundKeyOf(settings.rightKey)
			.getCode();
		int qCode = KeyBindingHelper.getBoundKeyOf(settings.dropKey)
			.getCode();

		if (code == sCode) {
			replay();
			return true;
		}

		if (code == aCode) {
			scroll(false);
			return true;
		}

		if (code == dCode) {
			scroll(true);
			return true;
		}

		if (code == qCode) {
			identifyMode = !identifyMode;
			if (!identifyMode)
				scenes.get(index)
					.deselect();
			return true;
		}

		return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
	}

	@Override
	protected String getBreadcrumbTitle() {
		if (chapter != null)
			return chapter.getTitle();

		return stack.getItem()
			.getName()
			.getString();
	}

	public TextRenderer getFontRenderer() {
		return textRenderer;
	}

	protected boolean isMouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
		boolean hovered = !(mouseX < x || mouseX > x + w);
		hovered &= !(mouseY < y || mouseY > y + h);
		return hovered;
	}

	public static void renderSpeechBox(DrawContext graphics, int x, int y, int w, int h, boolean highlighted, Pointing pointing,
		boolean returnWithLocalTransform) {
		MatrixStack ms = graphics.getMatrices();
		if (!returnWithLocalTransform)
			ms.push();

		int boxX = x;
		int boxY = y;
		int divotX = x;
		int divotY = y;
		int divotRotation = 0;
		int divotSize = 8;
		int distance = 1;
		int divotRadius = divotSize / 2;
		Couple<Color> borderColors = Theme.p(highlighted ? Theme.Key.PONDER_BUTTON_HOVER : Theme.Key.PONDER_IDLE);
		Color c;

		switch (pointing) {
		default:
		case DOWN:
			divotRotation = 0;
			boxX -= w / 2;
			boxY -= h + divotSize + 1 + distance;
			divotX -= divotRadius;
			divotY -= divotSize + distance;
			c = borderColors.getSecond();
			break;
		case LEFT:
			divotRotation = 90;
			boxX += divotSize + 1 + distance;
			boxY -= h / 2;
			divotX += distance;
			divotY -= divotRadius;
			c = Color.mixColors(borderColors, 0.5f);
			break;
		case RIGHT:
			divotRotation = 270;
			boxX -= w + divotSize + 1 + distance;
			boxY -= h / 2;
			divotX -= divotSize + distance;
			divotY -= divotRadius;
			c = Color.mixColors(borderColors, 0.5f);
			break;
		case UP:
			divotRotation = 180;
			boxX -= w / 2;
			boxY += divotSize + 1 + distance;
			divotX -= divotRadius;
			divotY += distance;
			c = borderColors.getFirst();
			break;
		}

		new BoxElement().withBackground(Theme.c(Theme.Key.PONDER_BACKGROUND_FLAT))
			.gradientBorder(borderColors)
			.at(boxX, boxY, 100)
			.withBounds(w, h)
			.render(graphics);

		ms.push();
		ms.translate(divotX + divotRadius, divotY + divotRadius, 10);
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(divotRotation));
		ms.translate(-divotRadius, -divotRadius, 0);
		AllGuiTextures.SPEECH_TOOLTIP_BACKGROUND.render(graphics, 0, 0);
		AllGuiTextures.SPEECH_TOOLTIP_COLOR.render(graphics, 0, 0, c);
		ms.pop();

		if (returnWithLocalTransform) {
			ms.translate(boxX, boxY, 0);
			return;
		}

		ms.pop();

	}

	public ItemStack getHoveredTooltipItem() {
		return hoveredTooltipItem;
	}

	public ItemStack getSubject() {
		return stack;
	}

	@Override
	public boolean isEquivalentTo(NavigatableSimiScreen other) {
		if (other instanceof PonderUI)
			return ItemHelper.sameItem(stack, ((PonderUI) other).stack);
		return super.isEquivalentTo(other);
	}

	@Override
	public void shareContextWith(NavigatableSimiScreen other) {
		if (other instanceof PonderUI) {
			PonderUI ponderUI = (PonderUI) other;
			ponderUI.referredToByTag = referredToByTag;
		}
	}

	public static float getPartialTicks() {
		float renderPartialTicks = MinecraftClient.getInstance()
			.getTickDelta();

		if (MinecraftClient.getInstance().currentScreen instanceof PonderUI) {
			PonderUI ui = (PonderUI) MinecraftClient.getInstance().currentScreen;
			if (ui.identifyMode)
				return ponderPartialTicksPaused;

			return (renderPartialTicks + (ui.extendedTickLength - ui.extendedTickTimer)) / (ui.extendedTickLength + 1);
		}

		return renderPartialTicks;
	}

	@Override
	public boolean shouldPause() {
		return true;
	}

	public void coolDownAfterSkip() {
		skipCooling = 15;
	}

	@Override
	public void removed() {
		super.removed();
		hoveredTooltipItem = ItemStack.EMPTY;
	}

	public void drawRightAlignedString(DrawContext graphics, MatrixStack ms, String string, int x, int y, int color) {
		graphics.drawText(textRenderer, string, x - textRenderer.getWidth(string), y, color, false);
	}

	public boolean isComfyReadingEnabled() {
		return AllConfigs.client().comfyReading.get();
	}

	public void setComfyReadingEnabled(boolean slowTextMode) {
		AllConfigs.client().comfyReading.set(slowTextMode);
	}

}
