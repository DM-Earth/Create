package com.simibubi.create.content.equipment.toolbox;

import static com.simibubi.create.content.equipment.toolbox.ToolboxInventory.STACKS_PER_COMPARTMENT;

import java.util.List;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;

public class RadialToolboxMenu extends AbstractSimiScreen {

	private State state;
	private int ticksOpen;
	private int hoveredSlot;
	private boolean scrollMode;
	private int scrollSlot = 0;
	private List<ToolboxBlockEntity> toolboxes;
	private ToolboxBlockEntity selectedBox;

	private static final int DEPOSIT = -7;
	private static final int UNEQUIP = -5;

	public RadialToolboxMenu(List<ToolboxBlockEntity> toolboxes, State state, @Nullable ToolboxBlockEntity selectedBox) {
		this.toolboxes = toolboxes;
		this.state = state;
		hoveredSlot = -1;

		if (selectedBox != null)
			this.selectedBox = selectedBox;
	}

	public void prevSlot(int slot) {
		scrollSlot = slot;
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		float fade = MathHelper.clamp((ticksOpen + AnimationTickHolder.getPartialTicks()) / 10f, 1 / 512f, 1);

		hoveredSlot = -1;
		Window window = MinecraftClient.getInstance().getWindow();
		float hoveredX = mouseX - window.getScaledWidth() / 2;
		float hoveredY = mouseY - window.getScaledHeight() / 2;

		float distance = hoveredX * hoveredX + hoveredY * hoveredY;
		if (distance > 25 && distance < 10000)
			hoveredSlot =
				(MathHelper.floor((AngleHelper.deg(MathHelper.atan2(hoveredY, hoveredX)) + 360 + 180 - 22.5f)) % 360)
					/ 45;
		boolean renderCenterSlot = state == State.SELECT_ITEM_UNEQUIP;
		if (scrollMode && distance > 150)
			scrollMode = false;
		if (renderCenterSlot && distance <= 150)
			hoveredSlot = UNEQUIP;

		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(width / 2, height / 2, 0);
		Text tip = null;

		if (state == State.DETACH) {

			tip = Lang.translateDirect("toolbox.outOfRange");
			if (hoveredX > -20 && hoveredX < 20 && hoveredY > -80 && hoveredY < -20)
				hoveredSlot = UNEQUIP;

			ms.push();
			AllGuiTextures.TOOLBELT_INACTIVE_SLOT.render(graphics, -12, -12);
			GuiGameElement.of(AllBlocks.TOOLBOXES.get(DyeColor.BROWN)
				.asStack())
				.at(-9, -9)
				.render(graphics);

			ms.translate(0, -40 + (10 * (1 - fade) * (1 - fade)), 0);
			AllGuiTextures.TOOLBELT_SLOT.render(graphics, -12, -12);
			ms.translate(-0.5, 0.5, 0);
			AllIcons.I_DISABLE.render(graphics, -9, -9);
			ms.translate(0.5, -0.5, 0);
			if (!scrollMode && hoveredSlot == UNEQUIP) {
				AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, -13, -13);
				tip = Lang.translateDirect("toolbox.detach")
					.formatted(Formatting.GOLD);
			}
			ms.pop();

		} else {

			if (hoveredX > 60 && hoveredX < 100 && hoveredY > -20 && hoveredY < 20)
				hoveredSlot = DEPOSIT;

			ms.push();
			ms.translate(80 + (-5 * (1 - fade) * (1 - fade)), 0, 0);
			AllGuiTextures.TOOLBELT_SLOT.render(graphics, -12, -12);
			ms.translate(-0.5, 0.5, 0);
			AllIcons.I_TOOLBOX.render(graphics, -9, -9);
			ms.translate(0.5, -0.5, 0);
			if (!scrollMode && hoveredSlot == DEPOSIT) {
				AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, -13, -13);
				tip = Lang.translateDirect(state == State.SELECT_BOX ? "toolbox.depositAll" : "toolbox.depositBox")
					.formatted(Formatting.GOLD);
			}
			ms.pop();

			for (int slot = 0; slot < 8; slot++) {
				ms.push();
				TransformStack.cast(ms)
					.rotateZ(slot * 45 - 45)
					.translate(0, -40 + (10 * (1 - fade) * (1 - fade)), 0)
					.rotateZ(-slot * 45 + 45);
				ms.translate(-12, -12, 0);

				if (state == State.SELECT_ITEM || state == State.SELECT_ITEM_UNEQUIP) {
					ToolboxInventory inv = selectedBox.inventory;
					ItemStack stackInSlot = inv.filters.get(slot);

					if (!stackInSlot.isEmpty()) {
						boolean empty = inv.getStackInSlot(slot * STACKS_PER_COMPARTMENT)
							.isEmpty();

						(empty ? AllGuiTextures.TOOLBELT_INACTIVE_SLOT : AllGuiTextures.TOOLBELT_SLOT)
							.render(graphics, 0, 0);
						GuiGameElement.of(stackInSlot)
							.at(3, 3)
							.render(graphics);

						if (slot == (scrollMode ? scrollSlot : hoveredSlot) && !empty) {
							AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, -1, -1);
							tip = stackInSlot.getName();
						}
					} else
						AllGuiTextures.TOOLBELT_EMPTY_SLOT.render(graphics, 0, 0);

				} else if (state == State.SELECT_BOX) {

					if (slot < toolboxes.size()) {
						AllGuiTextures.TOOLBELT_SLOT.render(graphics, 0, 0);
						ToolboxBlockEntity toolboxBlockEntity = toolboxes.get(slot);
						GuiGameElement.of(AllBlocks.TOOLBOXES.get(toolboxBlockEntity.getColor())
							.asStack())
							.at(3, 3)
							.render(graphics);

						if (slot == (scrollMode ? scrollSlot : hoveredSlot)) {
							AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, -1, -1);
							tip = toolboxBlockEntity.getDisplayName();
						}
					} else
						AllGuiTextures.TOOLBELT_EMPTY_SLOT.render(graphics, 0, 0);

				}

				ms.pop();
			}

			if (renderCenterSlot) {
				ms.push();
				AllGuiTextures.TOOLBELT_SLOT.render(graphics, -12, -12);
				(scrollMode ? AllIcons.I_REFRESH : AllIcons.I_FLIP).render(graphics, -9, -9);
				if (!scrollMode && UNEQUIP == hoveredSlot) {
					AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, -13, -13);
					tip = Lang.translateDirect("toolbox.unequip", client.player.getMainHandStack()
						.getName())
						.formatted(Formatting.GOLD);
				}
				ms.pop();
			}
		}
		ms.pop();

		if (tip != null) {
			int i1 = (int) (fade * 255.0F);
			if (i1 > 255)
				i1 = 255;

			if (i1 > 8) {
				ms.push();
				ms.translate((float) (width / 2), (float) (height - 68), 0.0F);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				int k1 = 16777215;
				int k = i1 << 24 & -16777216;
				int l = textRenderer.getWidth(tip);
				graphics.drawText(textRenderer, tip, Math.round(-l / 2f), -4, k1 | k, false);
				RenderSystem.disableBlend();
				ms.pop();
			}
		}

	}

	@Override
	public void renderBackground(DrawContext graphics) {
		int a = ((int) (0x50 * Math.min(1, (ticksOpen + AnimationTickHolder.getPartialTicks()) / 20f))) << 24;
		graphics.fillGradient(0, 0, this.width, this.height, 0x101010 | a, 0x101010 | a);
	}

	@Override
	public void tick() {
		ticksOpen++;
		super.tick();
	}

	@Override
	public void removed() {
		super.removed();

		int selected = (scrollMode ? scrollSlot : hoveredSlot);

		if (selected == DEPOSIT) {
			if (state == State.DETACH)
				return;
			else if (state == State.SELECT_BOX)
				toolboxes.forEach(be -> AllPackets.getChannel().sendToServer(new ToolboxDisposeAllPacket(be.getPos())));
			else
				AllPackets.getChannel().sendToServer(new ToolboxDisposeAllPacket(selectedBox.getPos()));
			return;
		}

		if (state == State.SELECT_BOX)
			return;

		if (state == State.DETACH) {
			if (selected == UNEQUIP)
				AllPackets.getChannel().sendToServer(
					new ToolboxEquipPacket(null, selected, client.player.getInventory().selectedSlot));
			return;
		}

		if (selected == UNEQUIP)
			AllPackets.getChannel().sendToServer(new ToolboxEquipPacket(selectedBox.getPos(), selected,
				client.player.getInventory().selectedSlot));

		if (selected < 0)
			return;
		ToolboxInventory inv = selectedBox.inventory;
		ItemStack stackInSlot = inv.filters.get(selected);
		if (stackInSlot.isEmpty())
			return;
		if (inv.getStackInSlot(selected * STACKS_PER_COMPARTMENT)
			.isEmpty())
			return;

		AllPackets.getChannel().sendToServer(new ToolboxEquipPacket(selectedBox.getPos(), selected,
			client.player.getInventory().selectedSlot));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		Window window = MinecraftClient.getInstance().getWindow();
		double hoveredX = mouseX - window.getScaledWidth() / 2;
		double hoveredY = mouseY - window.getScaledHeight() / 2;
		double distance = hoveredX * hoveredX + hoveredY * hoveredY;
		if (distance <= 150) {
			scrollMode = true;
			scrollSlot = (((int) (scrollSlot - delta)) + 8) % 8;
			for (int i = 0; i < 10; i++) {

				if (state == State.SELECT_ITEM || state == State.SELECT_ITEM_UNEQUIP) {
					ToolboxInventory inv = selectedBox.inventory;
					ItemStack stackInSlot = inv.filters.get(scrollSlot);
					if (!stackInSlot.isEmpty() && !inv.getStackInSlot(scrollSlot * STACKS_PER_COMPARTMENT)
						.isEmpty())
						break;
				}

				if (state == State.SELECT_BOX)
					if (scrollSlot < toolboxes.size())
						break;

				if (state == State.DETACH)
					break;

				scrollSlot -= MathHelper.sign(delta);
				scrollSlot = (scrollSlot + 8) % 8;
			}
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		int selected = scrollMode ? scrollSlot : hoveredSlot;

		if (button == 0) {
			if (selected == DEPOSIT) {
				close();
				ToolboxHandlerClient.COOLDOWN = 2;
				return true;
			}

			if (state == State.SELECT_BOX && selected >= 0 && selected < toolboxes.size()) {
				state = State.SELECT_ITEM;
				selectedBox = toolboxes.get(selected);
				return true;
			}

			if (state == State.DETACH || state == State.SELECT_ITEM || state == State.SELECT_ITEM_UNEQUIP) {
				if (selected == UNEQUIP || selected >= 0) {
					close();
					ToolboxHandlerClient.COOLDOWN = 2;
					return true;
				}
			}
		}

		if (button == 1) {
			if (state == State.SELECT_ITEM && toolboxes.size() > 1) {
				state = State.SELECT_BOX;
				return true;
			}

			if (state == State.SELECT_ITEM_UNEQUIP && selected == UNEQUIP) {
				if (toolboxes.size() > 1) {
					AllPackets.getChannel().sendToServer(new ToolboxEquipPacket(selectedBox.getPos(), selected,
						client.player.getInventory().selectedSlot));
					state = State.SELECT_BOX;
					return true;
				}

				close();
				ToolboxHandlerClient.COOLDOWN = 2;
				return true;
			}
		}

		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean keyPressed(int code, int scanCode, int modifiers) {
		KeyBinding[] hotbarBinds = client.options.hotbarKeys;
		for (int i = 0; i < hotbarBinds.length && i < 8; i++) {
			if (hotbarBinds[i].matchesKey(code, scanCode)) {

				if (state == State.SELECT_ITEM || state == State.SELECT_ITEM_UNEQUIP) {
					ToolboxInventory inv = selectedBox.inventory;
					ItemStack stackInSlot = inv.filters.get(i);
					if (stackInSlot.isEmpty() || inv.getStackInSlot(i * STACKS_PER_COMPARTMENT)
						.isEmpty())
						return false;
				}

				if (state == State.SELECT_BOX)
					if (i >= toolboxes.size())
						return false;

				scrollMode = true;
				scrollSlot = i;
				mouseClicked(0, 0, 0);
				return true;
			}
		}

		return super.keyPressed(code, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int code, int scanCode, int modifiers) {
		InputUtil.Key mouseKey = InputUtil.fromKeyCode(code, scanCode);
		if (KeyBindingHelper.isActiveAndMatches(AllKeys.TOOLBELT.getKeybind(), mouseKey)) {
			close();
			return true;
		}
		return super.keyReleased(code, scanCode, modifiers);
	}

	public static enum State {
		SELECT_BOX, SELECT_ITEM, SELECT_ITEM_UNEQUIP, DETACH
	}

}
