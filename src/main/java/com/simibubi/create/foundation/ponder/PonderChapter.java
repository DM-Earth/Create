package com.simibubi.create.foundation.ponder;

import javax.annotation.Nonnull;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import com.simibubi.create.foundation.gui.element.ScreenElement;

public class PonderChapter implements ScreenElement {

	private final Identifier id;
	private final Identifier icon;

	private PonderChapter(Identifier id) {
		this.id = id;
		icon = new Identifier(id.getNamespace(), "textures/ponder/chapter/" + id.getPath() + ".png");
	}

	public Identifier getId() {
		return id;
	}

	public String getTitle() {
		return PonderLocalization.getChapter(id);
	}

	public PonderChapter addTagsToChapter(PonderTag... tags) {
		for (PonderTag t : tags)
			PonderRegistry.TAGS.add(t, this);
		return this;
	}

	@Override
	public void render(DrawContext graphics, int x, int y) {
		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.scale(0.25f, 0.25f, 1);
		//x and y offset, blit z offset, tex x and y, tex width and height, entire tex sheet width and height
		graphics.drawTexture(icon, x, y, 0, 0, 0, 64, 64, 64, 64);
		ms.pop();
	}

	@Nonnull
	public static PonderChapter of(Identifier id) {
		PonderChapter chapter = PonderRegistry.CHAPTERS.getChapter(id);
		if (chapter == null) {
			 chapter = PonderRegistry.CHAPTERS.addChapter(new PonderChapter(id));
		}

		return chapter;
	}
}
