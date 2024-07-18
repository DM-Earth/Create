package com.simibubi.create.content.equipment.symmetryWand;

import org.joml.Vector3f;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.equipment.symmetryWand.mirror.CrossPlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.EmptyMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.PlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.TriplePlaneMirror;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class SymmetryWandScreen extends AbstractSimiScreen {

	private AllGuiTextures background;

	private ScrollInput areaType;
	private Label labelType;
	private ScrollInput areaAlign;
	private Label labelAlign;
	private IconButton confirmButton;

	private final Text mirrorType = Lang.translateDirect("gui.symmetryWand.mirrorType");
	private final Text orientation = Lang.translateDirect("gui.symmetryWand.orientation");

	private SymmetryMirror currentElement;
	private ItemStack wand;
	private Hand hand;

	public SymmetryWandScreen(ItemStack wand, Hand hand) {
		background = AllGuiTextures.WAND_OF_SYMMETRY;

		currentElement = SymmetryWandItem.getMirror(wand);
		if (currentElement instanceof EmptyMirror) {
			currentElement = new PlaneMirror(Vec3d.ZERO);
		}
		this.hand = hand;
		this.wand = wand;
	}

	@Override
	public void init() {
		setWindowSize(background.width, background.height);
		setWindowOffset(-20, 0);
		super.init();

		int x = guiLeft;
		int y = guiTop;

		labelType = new Label(x + 49, y + 28, Components.immutableEmpty()).colored(0xFFFFFFFF)
			.withShadow();
		labelAlign = new Label(x + 49, y + 50, Components.immutableEmpty()).colored(0xFFFFFFFF)
			.withShadow();

		int state =
			currentElement instanceof TriplePlaneMirror ? 2 : currentElement instanceof CrossPlaneMirror ? 1 : 0;
		areaType = new SelectionScrollInput(x + 45, y + 21, 109, 18).forOptions(SymmetryMirror.getMirrors())
			.titled(mirrorType.copyContentOnly())
			.writingTo(labelType)
			.setState(state);

		areaType.calling(position -> {
			switch (position) {
			case 0:
				currentElement = new PlaneMirror(currentElement.getPosition());
				break;
			case 1:
				currentElement = new CrossPlaneMirror(currentElement.getPosition());
				break;
			case 2:
				currentElement = new TriplePlaneMirror(currentElement.getPosition());
				break;
			default:
				break;
			}
			initAlign(currentElement, x, y);
		});

		initAlign(currentElement, x, y);

		addDrawableChild(labelAlign);
		addDrawableChild(areaType);
		addDrawableChild(labelType);

		confirmButton = new IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			close();
		});
		addDrawableChild(confirmButton);
	}

	private void initAlign(SymmetryMirror element, int x, int y) {
		if (areaAlign != null)
			remove(areaAlign);

		areaAlign = new SelectionScrollInput(x + 45, y + 43, 109, 18).forOptions(element.getAlignToolTips())
			.titled(orientation.copyContentOnly())
			.writingTo(labelAlign)
			.setState(element.getOrientationIndex())
			.calling(element::setOrientation);

		addDrawableChild(areaAlign);
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft;
		int y = guiTop;

		background.render(graphics, x, y);
		graphics.drawText(textRenderer, wand.getName(), x + 11, y + 4, 0x592424, false);

		renderBlock(graphics, x, y);
		GuiGameElement.of(wand)
				.scale(4)
				.rotate(-70, 20, 20)
				.at(x + 178, y + 448, -150)
				.render(graphics);
	}

	protected void renderBlock(DrawContext graphics, int x, int y) {
		MatrixStack ms = graphics.getMatrices();
		
		ms.push();
		ms.translate(x + 26, y + 39, 20);
		ms.scale(16, 16, 16);
		ms.multiply(RotationAxis.of(new Vector3f(.3f, 1f, 0f)).rotationDegrees(-22.5f));
		currentElement.applyModelTransform(ms);
		// RenderSystem.multMatrix(ms.peek().getModel());
		GuiGameElement.of(currentElement.getModel())
			.render(graphics);

		ms.pop();
	}

	@Override
	public void removed() {
		SymmetryWandItem.configureSettings(wand, currentElement);
		AllPackets.getChannel().sendToServer(new ConfigureSymmetryWandPacket(hand, currentElement));
	}

}
