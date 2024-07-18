package com.simibubi.create.foundation.ponder;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.element.ScreenElement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class PonderTag implements ScreenElement {

	public static final PonderTag HIGHLIGHT_ALL = new PonderTag(Create.asResource("_all"));

	private final Identifier id;
	private Identifier icon;
	private ItemStack itemIcon = ItemStack.EMPTY;
	private ItemStack mainItem = ItemStack.EMPTY;

	public PonderTag(Identifier id) {
		this.id = id;
	}

	public Identifier getId() {
		return id;
	}

	public ItemStack getMainItem() {
		return mainItem;
	}

	public String getTitle() {
		return PonderLocalization.getTag(id);
	}

	public String getDescription() {
		return PonderLocalization.getTagDescription(id);
	}

	// Builder

	public PonderTag defaultLang(String title, String description) {
		PonderLocalization.registerTag(id, title, description);
		return this;
	}

	public PonderTag addToIndex() {
		PonderRegistry.TAGS.listTag(this);
		return this;
	}

	public PonderTag icon(Identifier location) {
		this.icon = new Identifier(location.getNamespace(), "textures/ponder/tag/" + location.getPath() + ".png");
		return this;
	}

	public PonderTag icon(String location) {
		this.icon = new Identifier(id.getNamespace(), "textures/ponder/tag/" + location + ".png");
		return this;
	}

	public PonderTag idAsIcon() {
		return icon(id);
	}

	public PonderTag item(ItemConvertible item, boolean useAsIcon, boolean useAsMainItem) {
		if (useAsIcon)
			this.itemIcon = new ItemStack(item);
		if (useAsMainItem)
			this.mainItem = new ItemStack(item);
		return this;
	}

	public PonderTag item(ItemConvertible item) {
		return this.item(item, true, false);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void render(DrawContext graphics, int x, int y) {
		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(x, y, 0);
		if (icon != null) {
			ms.scale(0.25f, 0.25f, 1);
			graphics.drawTexture(icon, 0, 0, 0, 0, 0, 64, 64, 64, 64);
		} else if (!itemIcon.isEmpty()) {
			ms.translate(-2, -2, 0);
			ms.scale(1.25f, 1.25f, 1.25f);
			GuiGameElement.of(itemIcon)
				.render(graphics);
		}
		ms.pop();
	}

}
