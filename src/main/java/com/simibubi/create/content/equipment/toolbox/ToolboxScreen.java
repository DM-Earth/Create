package com.simibubi.create.content.equipment.toolbox;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import com.google.common.collect.ImmutableList;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;

public class ToolboxScreen extends AbstractSimiContainerScreen<ToolboxMenu> {

	protected static final AllGuiTextures BG = AllGuiTextures.TOOLBOX;
	protected static final AllGuiTextures PLAYER = AllGuiTextures.PLAYER_INVENTORY;

	protected Slot hoveredToolboxSlot;
	private IconButton confirmButton;
	private IconButton disposeButton;
	private DyeColor color;

	private List<Rect2i> extraAreas = Collections.emptyList();

	public ToolboxScreen(ToolboxMenu menu, PlayerInventory inv, Text title) {
		super(menu, inv, title);
//		init(); // fabric: this causes a crash with Trinkets since minecraft is null.
				// removal seems to have no effect. why is it here?
	}

	@Override
	protected void init() {
		setWindowSize(30 + BG.width, BG.height + PLAYER.height - 24);
		setWindowOffset(-11, 0);
		super.init();
		clearChildren();

		color = handler.contentHolder.getColor();

		confirmButton = new IconButton(x + 30 + BG.width - 33, y + BG.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			client.player.closeHandledScreen();
		});
		addDrawableChild(confirmButton);

		disposeButton = new IconButton(x + 30 + 81, y + 69, AllIcons.I_TOOLBOX);
		disposeButton.withCallback(() -> {
			AllPackets.getChannel().sendToServer(new ToolboxDisposeAllPacket(handler.contentHolder.getPos()));
		});
		disposeButton.setToolTip(Lang.translateDirect("toolbox.depositBox"));
		addDrawableChild(disposeButton);

		extraAreas = ImmutableList.of(
			new Rect2i(x + 30 + BG.width, y + BG.height - 15 - 34 - 6, 72, 68)
		);
	}

	@Override
	public void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		handler.renderPass = true;
		super.render(graphics, mouseX, mouseY, partialTicks);
		handler.renderPass = false;
	}

	@Override
	protected void drawBackground(DrawContext graphics, float partialTicks, int mouseX, int mouseY) {
		int screenX = x + backgroundWidth - BG.width;
		int screenY = y;

		BG.render(graphics, screenX, screenY);
		graphics.drawText(textRenderer, title, screenX + 15, screenY + 4, 0x592424, false);

		int invX = x;
		int invY = y + backgroundHeight - PLAYER.height;
		renderPlayerInventory(graphics, invX, invY);

		renderToolbox(graphics, screenX + BG.width + 50, screenY + BG.height + 12, partialTicks);

		MatrixStack ms = graphics.getMatrices();

		hoveredToolboxSlot = null;
		for (int compartment = 0; compartment < 8; compartment++) {
			int baseIndex = compartment * ToolboxInventory.STACKS_PER_COMPARTMENT;
			Slot slot = handler.slots.get(baseIndex);
			ItemStack itemstack = slot.getStack();
			int i = slot.x + x;
			int j = slot.y + y;

			if (itemstack.isEmpty())
				itemstack = handler.getFilter(compartment);

			if (!itemstack.isEmpty()) {
				int count = handler.totalCountInCompartment(compartment);
				String s = String.valueOf(count);
				ms.push();
				ms.translate(0, 0, 100);
				RenderSystem.enableDepthTest();
				graphics.drawItem(client.player, itemstack, i, j, 0);
				graphics.drawItemInSlot(textRenderer, itemstack, i, j, s);
				ms.pop();
			}

			if (isPointWithinBounds(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
				hoveredToolboxSlot = slot;
				RenderSystem.disableDepthTest();
				RenderSystem.colorMask(true, true, true, false);
				int slotColor = 0x80FFFFFF;// default in forge, never overridden. // this.getSlotColor(baseIndex);
				graphics.fillGradient(i, j, i + 16, j + 16, slotColor, slotColor);
				RenderSystem.colorMask(true, true, true, true);
				RenderSystem.enableDepthTest();
			}
		}
	}

	private void renderToolbox(DrawContext graphics, int x, int y, float partialTicks) {
        MatrixStack ms = graphics.getMatrices();
		TransformStack.cast(ms)
			.pushPose()
			.translate(x, y, 100)
			.scale(50)
			.rotateX(-22)
			.rotateY(-202);

		GuiGameElement.of(AllBlocks.TOOLBOXES.get(color)
			.getDefaultState())
			.render(graphics);

        TransformStack.cast(ms)
			.pushPose()
			.translate(0, -6 / 16f, 12 / 16f)
			.rotateX(-105 * handler.contentHolder.lid.getValue(partialTicks))
			.translate(0, 6 / 16f, -12 / 16f);
		GuiGameElement.of(AllPartialModels.TOOLBOX_LIDS.get(color))
			.render(graphics);
		ms.pop();

		for (int offset : Iterate.zeroAndOne) {
			ms.push();
			ms.translate(0, -offset * 1 / 8f,
				handler.contentHolder.drawers.getValue(partialTicks) * -.175f * (2 - offset));
			GuiGameElement.of(AllPartialModels.TOOLBOX_DRAWER)
				.render(graphics);
			ms.pop();
		}
		ms.pop();
	}

	@Override
	protected void renderForeground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		if (hoveredToolboxSlot != null)
			focusedSlot = hoveredToolboxSlot;
		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}
