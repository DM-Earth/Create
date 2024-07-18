package com.simibubi.create.foundation.config.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Formatting;
import io.github.fabricators_of_create.porting_lib.config.ConfigType;
import io.github.fabricators_of_create.porting_lib.config.ModConfigSpec;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.AbstractSelectionListAccessor;

import org.lwjgl.glfw.GLFW;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.Lists;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.config.ui.ConfigScreenList.LabeledEntry;
import com.simibubi.create.foundation.config.ui.entries.BooleanEntry;
import com.simibubi.create.foundation.config.ui.entries.EnumEntry;
import com.simibubi.create.foundation.config.ui.entries.NumberEntry;
import com.simibubi.create.foundation.config.ui.entries.SubMenuEntry;
import com.simibubi.create.foundation.config.ui.entries.ValueEntry;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.ConfirmationScreen;
import com.simibubi.create.foundation.gui.ConfirmationScreen.Response;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.element.DelegatedStencilElement;
import com.simibubi.create.foundation.gui.widget.BoxWidget;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Pair;

public class SubMenuConfigScreen extends ConfigScreen {

	public final ConfigType type;
	protected ModConfigSpec spec;
	protected UnmodifiableConfig configGroup;
	protected ConfigScreenList list;

	protected BoxWidget resetAll;
	protected BoxWidget saveChanges;
	protected BoxWidget discardChanges;
	protected BoxWidget goBack;
	protected BoxWidget serverLocked;
	protected HintableTextFieldWidget search;
	protected int listWidth;
	protected String title;
	protected Set<String> highlights = new HashSet<>();

	public static SubMenuConfigScreen find(ConfigHelper.ConfigPath path) {
		// TODO 1.17: can be null
		ModConfigSpec spec = ConfigHelper.findModConfigSpecFor(path.getType(), path.getModID());
		UnmodifiableConfig values = spec.getValues();
		BaseConfigScreen base = new BaseConfigScreen(null, path.getModID());
		SubMenuConfigScreen screen = new SubMenuConfigScreen(base, "root", path.getType(), spec, values);
		List<String> remainingPath = Lists.newArrayList(path.getPath());

		path: while (!remainingPath.isEmpty()) {
			String next = remainingPath.remove(0);
			for (Map.Entry<String, Object> entry : values.valueMap().entrySet()) {
				String key = entry.getKey();
				Object obj = entry.getValue();
				if (!key.equalsIgnoreCase(next))
					continue;

				if (!(obj instanceof AbstractConfig)) {
					//highlight entry
					screen.highlights.add(path.getPath()[path.getPath().length - 1]);
					continue;
				}

				values = (UnmodifiableConfig) obj;
				screen = new SubMenuConfigScreen(screen, toHumanReadable(key), path.getType(), spec, values);
				continue path;
			}

			break;
		}

		ConfigScreen.modID = path.getModID();
		return screen;
	}

	public SubMenuConfigScreen(Screen parent, String title, ConfigType type, ModConfigSpec configSpec, UnmodifiableConfig configGroup) {
		super(parent);
		this.type = type;
		this.spec = configSpec;
		this.title = title;
		this.configGroup = configGroup;
	}

	public SubMenuConfigScreen(Screen parent, ConfigType type, ModConfigSpec configSpec) {
		super(parent);
		this.type = type;
		this.spec = configSpec;
		this.title = "root";
		this.configGroup = configSpec.getValues();
	}

	protected void clearChanges() {
		ConfigHelper.changes.clear();
		list.children()
				.stream()
				.filter(e -> e instanceof ValueEntry)
				.forEach(e -> ((ValueEntry<?>) e).onValueChange());
	}

	protected void saveChanges() {
		UnmodifiableConfig values = spec.getValues();
		ConfigHelper.changes.forEach((path, change) -> {
			ModConfigSpec.ConfigValue<Object> configValue = values.get(path);
			configValue.set(change.value);

			if (type == ConfigType.SERVER) {
				AllPackets.getChannel().sendToServer(new CConfigureConfigPacket<>(ConfigScreen.modID, path, change.value));
			}

			String command = change.annotations.get("Execute");
			if (client.player != null && command != null && command.startsWith("/")) {
				client.player.networkHandler.sendChatCommand(command.substring(1));
			}
		});
		clearChanges();
	}

	protected void resetConfig(UnmodifiableConfig values) {
		values.valueMap().forEach((key, obj) -> {
			if (obj instanceof AbstractConfig) {
				resetConfig((UnmodifiableConfig) obj);
			} else if (obj instanceof ModConfigSpec.ConfigValue) {
				ModConfigSpec.ConfigValue<Object> configValue = (ModConfigSpec.ConfigValue<Object>) obj;
				ModConfigSpec.ValueSpec valueSpec = spec.getRaw((List<String>) configValue.getPath());

				List<String> comments = new ArrayList<>();

				if (valueSpec.getComment() != null)
					comments.addAll(Arrays.asList(valueSpec.getComment().split("\n")));

				Pair<String, Map<String, String>> metadata = ConfigHelper.readMetadataFromComment(comments);

				ConfigHelper.setValue(String.join(".", configValue.getPath()), configValue, valueSpec.getDefault(), metadata.getSecond());
			}
		});

		list.children()
				.stream()
				.filter(e -> e instanceof ValueEntry)
				.forEach(e -> ((ValueEntry<?>) e).onValueChange());
	}

	@Override
	protected void init() {
		super.init();

		listWidth = Math.min(width - 80, 300);

		int yCenter = height / 2;
		int listL = this.width / 2 - listWidth / 2;
		int listR = this.width / 2 + listWidth / 2;

		resetAll = new BoxWidget(listR + 10, yCenter - 25, 20, 20)
				.withPadding(2, 2)
				.withCallback((x, y) ->
						new ConfirmationScreen()
								.centered()
								.withText(StringVisitable.plain("Resetting all settings of the " + type.toString() + " config. Are you sure?"))
								.withAction(success -> {
									if (success)
										resetConfig(spec.getValues());
								})
								.open(this)
				);

		resetAll.showingElement(AllIcons.I_CONFIG_RESET.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(resetAll)));
		resetAll.getToolTip().add(Components.literal("Reset All"));
		resetAll.getToolTip().addAll(TooltipHelper.cutStringTextComponent("Click here to reset all settings to their default value.", Palette.ALL_GRAY));

		saveChanges = new BoxWidget(listL - 30, yCenter - 25, 20, 20)
				.withPadding(2, 2)
				.withCallback((x, y) -> {
					if (ConfigHelper.changes.isEmpty())
						return;

					ConfirmationScreen confirm = new ConfirmationScreen()
							.centered()
							.withText(StringVisitable.plain("Saving " + ConfigHelper.changes.size() + " changed value" + (ConfigHelper.changes.size() != 1 ? "s" : "") + ""))
							.withAction(success -> {
								if (success)
									saveChanges();
							});

					addAnnotationsToConfirm(confirm).open(this);
				});
		saveChanges.showingElement(AllIcons.I_CONFIG_SAVE.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(saveChanges)));
		saveChanges.getToolTip().add(Components.literal("Save Changes"));
		saveChanges.getToolTip().addAll(TooltipHelper.cutStringTextComponent("Click here to save your current changes.", Palette.ALL_GRAY));

		discardChanges = new BoxWidget(listL - 30, yCenter + 5, 20, 20)
				.withPadding(2, 2)
				.withCallback((x, y) -> {
					if (ConfigHelper.changes.isEmpty())
						return;

					new ConfirmationScreen()
							.centered()
							.withText(StringVisitable.plain("Discarding " + ConfigHelper.changes.size() + " unsaved change" + (ConfigHelper.changes.size() != 1 ? "s" : "") + ""))
							.withAction(success -> {
								if (success)
									clearChanges();
							})
							.open(this);
				});
		discardChanges.showingElement(AllIcons.I_CONFIG_DISCARD.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(discardChanges)));
		discardChanges.getToolTip().add(Components.literal("Discard Changes"));
		discardChanges.getToolTip().addAll(TooltipHelper.cutStringTextComponent("Click here to discard all the changes you made.", Palette.ALL_GRAY));

		goBack = new BoxWidget(listL - 30, yCenter + 65, 20, 20)
				.withPadding(2, 2)
				.withCallback(this::attemptBackstep);
		goBack.showingElement(AllIcons.I_CONFIG_BACK.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(goBack)));
		goBack.getToolTip().add(Components.literal("Go Back"));

		addDrawableChild(resetAll);
		addDrawableChild(saveChanges);
		addDrawableChild(discardChanges);
		addDrawableChild(goBack);

		list = new ConfigScreenList(client, listWidth, height - 80, 35, height - 45, 40);
		list.setLeftPos(this.width / 2 - ((AbstractSelectionListAccessor) list).port_lib$getWidth() / 2);

		addDrawableChild(list);

		search = new ConfigTextField(textRenderer, width / 2 - listWidth / 2, height - 35, listWidth, 20);
		search.setChangedListener(this::updateFilter);
		search.setHint("Search...");
		search.setCursorToStart();
		addDrawableChild(search);

		configGroup.valueMap().forEach((key, obj) -> {
			String humanKey = toHumanReadable(key);

			if (obj instanceof AbstractConfig) {
				SubMenuEntry entry = new SubMenuEntry(this, humanKey, spec, (UnmodifiableConfig) obj);
				entry.path = key;
				list.children().add(entry);
				if (configGroup.valueMap()
						.size() == 1)
					ScreenOpener.open(
							new SubMenuConfigScreen(parent, humanKey, type, spec, (UnmodifiableConfig) obj));

			} else if (obj instanceof ModConfigSpec.ConfigValue<?>) {
				ModConfigSpec.ConfigValue<?> configValue = (ModConfigSpec.ConfigValue<?>) obj;
				ModConfigSpec.ValueSpec valueSpec = spec.getRaw(configValue.getPath());
				Object value = configValue.get();
				ConfigScreenList.Entry entry = null;

				if (value instanceof Boolean) {
					entry = new BooleanEntry(humanKey, (ModConfigSpec.ConfigValue<Boolean>) configValue, valueSpec);
				} else if (value instanceof Enum) {
					entry = new EnumEntry(humanKey, (ModConfigSpec.ConfigValue<Enum<?>>) configValue, valueSpec);
				} else if (value instanceof Number) {
					entry = NumberEntry.create(value, humanKey, configValue, valueSpec);
				}

				if (entry == null)
					entry = new LabeledEntry("Impl missing - " + configValue.get().getClass().getSimpleName() + "  " + humanKey + " : " + value);

				if (highlights.contains(key))
					entry.annotations.put("highlight", ":)");

				list.children().add(entry);
			}
		});

		Collections.sort(list.children(),
				(e, e2) -> {
					int group = (e2 instanceof SubMenuEntry ? 1 : 0) - (e instanceof SubMenuEntry ? 1 : 0);
					if (group == 0 && e instanceof LabeledEntry && e2 instanceof LabeledEntry) {
						LabeledEntry le = (LabeledEntry) e;
						LabeledEntry le2 = (LabeledEntry) e2;
						return le.label.getComponent()
								.getString()
								.compareTo(le2.label.getComponent()
										.getString());
					}
					return group;
				});

		list.search(highlights.stream().findFirst().orElse(""));

		//extras for server configs
		if (type != ConfigType.SERVER)
			return;
		if (client.isIntegratedServerRunning())
			return;

		boolean canEdit = client != null && client.player != null && client.player.hasPermissionLevel(2);

		Couple<Color> red = Theme.p(Theme.Key.BUTTON_FAIL);
		Couple<Color> green = Theme.p(Theme.Key.BUTTON_SUCCESS);

		DelegatedStencilElement stencil = new DelegatedStencilElement();

		serverLocked = new BoxWidget(listR + 10, yCenter + 5, 20, 20)
				.withPadding(2, 2)
				.showingElement(stencil);

		if (!canEdit) {
			list.children().forEach(e -> e.setEditable(false));
			resetAll.active = false;
			stencil.withStencilRenderer((ms, w, h, alpha) -> AllIcons.I_CONFIG_LOCKED.render(ms, 0, 0));
			stencil.withElementRenderer((ms, w, h, alpha) -> UIRenderHelper.angledGradient(ms, 90, 8, 0, 16, 16, red));
			serverLocked.withBorderColors(red);
			serverLocked.getToolTip().add(Components.literal("Locked").formatted(Formatting.BOLD));
			serverLocked.getToolTip().addAll(TooltipHelper.cutStringTextComponent("You do not have enough permissions to edit the server config. You can still look at the current values here though.", Palette.ALL_GRAY));
		} else {
			stencil.withStencilRenderer((ms, w, h, alpha) -> AllIcons.I_CONFIG_UNLOCKED.render(ms, 0, 0));
			stencil.withElementRenderer((ms, w, h, alpha) -> UIRenderHelper.angledGradient(ms, 90, 8, 0, 16, 16, green));
			serverLocked.withBorderColors(green);
			serverLocked.getToolTip().add(Components.literal("Unlocked").formatted(Formatting.BOLD));
			serverLocked.getToolTip().addAll(TooltipHelper.cutStringTextComponent("You have enough permissions to edit the server config. Changes you make here will be synced with the server when you save them.", Palette.ALL_GRAY));
		}

		addDrawableChild(serverLocked);
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderWindow(graphics, mouseX, mouseY, partialTicks);

		int x = width / 2;
		graphics.drawCenteredTextWithShadow(client.textRenderer, ConfigScreen.modID + " > " + type.toString()
			.toLowerCase(Locale.ROOT) + " > " + title, x, 15, Theme.i(Theme.Key.TEXT));
	}

	@Override
	protected void renderWindowForeground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderWindowForeground(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public void resize(@Nonnull MinecraftClient client, int width, int height) {
		double scroll = list.getScrollAmount();
		init(client, width, height);
		list.setScrollAmount(scroll);
	}

	@Nullable
	@Override
	public Element getFocused() {
		if (ConfigScreenList.currentText != null)
			return ConfigScreenList.currentText;

		return super.getFocused();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;

		if (Screen.hasControlDown()) {
			if (keyCode == GLFW.GLFW_KEY_F) {
				search.setFocused(true);
			}
		}

		if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			attemptBackstep();
		}

		return false;
	}

	protected void updateFilter(String search) {
		if (list.search(search)) {
			this.search.setEditableColor(Theme.i(Theme.Key.TEXT));
		} else {
			this.search.setEditableColor(Theme.i(Theme.Key.BUTTON_FAIL));
		}
	}

	protected void attemptBackstep() {
		if (ConfigHelper.changes.isEmpty() || !(parent instanceof BaseConfigScreen)) {
			ScreenOpener.open(parent);
			return;
		}

		showLeavingPrompt(success -> {
			if (success == Response.Cancel)
				return;
			if (success == Response.Confirm)
				saveChanges();
			ConfigHelper.changes.clear();
			ScreenOpener.open(parent);
		});
	}

	@Override
	public void close() {
		if (ConfigHelper.changes.isEmpty()) {
			super.close();
			return;
		}

		showLeavingPrompt(success -> {
			if (success == Response.Cancel)
				return;
			if (success == Response.Confirm)
				saveChanges();
			ConfigHelper.changes.clear();
			super.close();
		});
	}

	public void showLeavingPrompt(Consumer<Response> action) {
		ConfirmationScreen screen = new ConfirmationScreen()
				.centered()
				.withThreeActions(action)
				.addText(StringVisitable.plain("Leaving with " + ConfigHelper.changes.size() + " unsaved change"
						+ (ConfigHelper.changes.size() != 1 ? "s" : "") + " for this config"));

		addAnnotationsToConfirm(screen).open(this);
	}

	protected ConfirmationScreen addAnnotationsToConfirm(ConfirmationScreen screen) {
		AtomicBoolean relog = new AtomicBoolean(false);
		AtomicBoolean restart = new AtomicBoolean(false);
		ConfigHelper.changes.values().forEach(change -> {
			if (change.annotations.containsKey(ConfigAnnotations.RequiresRelog.TRUE.getName()))
				relog.set(true);

			if (change.annotations.containsKey(ConfigAnnotations.RequiresRestart.CLIENT.getName()))
				restart.set(true);
		});

		if (relog.get()) {
			screen.addText(StringVisitable.plain(" "));
			screen.addText(StringVisitable.plain("At least one changed value will require you to relog to take full effect"));
		}

		if (restart.get()) {
			screen.addText(StringVisitable.plain(" "));
			screen.addText(StringVisitable.plain("At least one changed value will require you to restart your game to take full effect"));
		}

		return screen;
	}

}
