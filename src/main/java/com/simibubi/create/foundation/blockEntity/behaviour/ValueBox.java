package com.simibubi.create.foundation.blockEntity.behaviour;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.outliner.ChasingAABBOutline;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class ValueBox extends ChasingAABBOutline {

	protected Text label;
	protected Text sublabel = Components.immutableEmpty();
	protected Text scrollTooltip = Components.immutableEmpty();
	protected Vec3d labelOffset = Vec3d.ZERO;

	public int overrideColor = -1;

	public boolean isPassive;

	protected BlockPos pos;
	protected ValueBoxTransform transform;
	protected BlockState blockState;

	protected AllIcons outline = AllIcons.VALUE_BOX_HOVER_4PX;

	public ValueBox(Text label, Box bb, BlockPos pos) {
		this(label, bb, pos, MinecraftClient.getInstance().world.getBlockState(pos));
	}

	public ValueBox(Text label, Box bb, BlockPos pos, BlockState state) {
		super(bb);
		this.label = label;
		this.pos = pos;
		this.blockState = state;
	}

	public ValueBox transform(ValueBoxTransform transform) {
		this.transform = transform;
		return this;
	}

	public ValueBox wideOutline() {
		this.outline = AllIcons.VALUE_BOX_HOVER_6PX;
		return this;
	}

	public ValueBox passive(boolean passive) {
		this.isPassive = passive;
		return this;
	}

	public ValueBox withColor(int color) {
		this.overrideColor = color;
		return this;
	}

	@Override
	public void render(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera, float pt) {
		boolean hasTransform = transform != null;
		if (transform instanceof Sided && params.getHighlightedFace() != null)
			((Sided) transform).fromSide(params.getHighlightedFace());
		if (hasTransform && !transform.shouldRender(blockState))
			return;

		ms.push();
		ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
		if (hasTransform)
			transform.transform(blockState, ms);

		if (!isPassive) {
			ms.push();
			ms.scale(-2.01f, -2.01f, 2.01f);
			ms.translate(-8 / 16.0, -8 / 16.0, -.5 / 16.0);
			getOutline().render(ms, buffer, 0xffffff);
			ms.pop();
		}

		float fontScale = hasTransform ? -transform.getFontScale() : -1 / 64f;
		ms.scale(fontScale, fontScale, fontScale);
		renderContents(ms, buffer);

		ms.pop();
	}

	public AllIcons getOutline() {
		return outline;
	}

	public void renderContents(MatrixStack ms, VertexConsumerProvider buffer) {}

	public static class ItemValueBox extends ValueBox {
		ItemStack stack;
		int count;
		boolean upTo;

		public ItemValueBox(Text label, Box bb, BlockPos pos, ItemStack stack, int count, boolean upTo) {
			super(label, bb, pos);
			this.stack = stack;
			this.count = count;
			this.upTo = upTo;
		}

		@Override
		public AllIcons getOutline() {
			if (!stack.isEmpty())
				return AllIcons.VALUE_BOX_HOVER_6PX;
			return super.getOutline();
		}

		@Override
		public void renderContents(MatrixStack ms, VertexConsumerProvider buffer) {
			super.renderContents(ms, buffer);
			if (count == -1)
				return;

			TextRenderer font = MinecraftClient.getInstance().textRenderer;
			boolean wildcard = count == 0 || upTo && count >= stack.getMaxCount();
			Text countString = Components.literal(wildcard ? "*" : count + "");
			ms.translate(17.5f, -5f, 7f);

			boolean isFilter = stack.getItem() instanceof FilterItem;
			boolean isEmpty = stack.isEmpty();

			ItemRenderer itemRenderer = MinecraftClient.getInstance()
				.getItemRenderer();
			BakedModel modelWithOverrides = itemRenderer.getModel(stack, null, null, 0);
			boolean blockItem =
				modelWithOverrides.hasDepth();

			float scale = 1.5f;
			ms.translate(-font.getWidth(countString), 0, 0);

			if (isFilter)
				ms.translate(-5, 8, 7.25f);
			else if (isEmpty) {
				ms.translate(-15, -1f, -2.75f);
				scale = 1.65f;
			} else
				ms.translate(-7, 10, blockItem ? 10 + 1 / 4f : 0);

			if (wildcard)
				ms.translate(-1, 3f, 0);

			ms.scale(scale, scale, scale);
			drawString(ms, buffer, countString, 0, 0, isFilter ? 0xFFFFFF : 0xEDEDED);
			ms.translate(0, 0, -1 / 16f);
			drawString(ms, buffer, countString, 1 - 1 / 8f, 1 - 1 / 8f, 0x4F4F4F);
		}

	}

	public static class TextValueBox extends ValueBox {
		Text text;

		public TextValueBox(Text label, Box bb, BlockPos pos, Text text) {
			super(label, bb, pos);
			this.text = text;
		}

		public TextValueBox(Text label, Box bb, BlockPos pos, BlockState state, Text text) {
			super(label, bb, pos, state);
			this.text = text;
		}

		@Override
		public void renderContents(MatrixStack ms, VertexConsumerProvider buffer) {
			super.renderContents(ms, buffer);
			TextRenderer font = MinecraftClient.getInstance().textRenderer;
			float scale = 3;
			ms.scale(scale, scale, 1);
			ms.translate(-4, -3.75, 5);

			int stringWidth = font.getWidth(text);
			float numberScale = (float) font.fontHeight / stringWidth;
			boolean singleDigit = stringWidth < 10;
			if (singleDigit)
				numberScale = numberScale / 2;
			float verticalMargin = (stringWidth - font.fontHeight) / 2f;

			ms.scale(numberScale, numberScale, numberScale);
			ms.translate(singleDigit ? stringWidth / 2 : 0, singleDigit ? -verticalMargin : verticalMargin, 0);

			int overrideColor = transform.getOverrideColor();
			renderHoveringText(ms, buffer, text, overrideColor != -1 ? overrideColor : 0xEDEDED);
		}

	}

	public static class IconValueBox extends ValueBox {
		AllIcons icon;

		public IconValueBox(Text label, INamedIconOptions iconValue, Box bb, BlockPos pos) {
			super(label, bb, pos);
			icon = iconValue.getIcon();
		}

		@Override
		public void renderContents(MatrixStack ms, VertexConsumerProvider buffer) {
			super.renderContents(ms, buffer);
			float scale = 2 * 16;
			ms.scale(scale, scale, scale);
			ms.translate(-.5f, -.5f, 5 / 32f);

			int overrideColor = transform.getOverrideColor();
			icon.render(ms, buffer, overrideColor != -1 ? overrideColor : 0xFFFFFF);
		}

	}

	protected void renderHoveringText(MatrixStack ms, VertexConsumerProvider buffer, Text text, int color) {
		ms.push();
		drawString(ms, buffer, text, 0, 0, color);
		ms.pop();
	}

	private static void drawString(MatrixStack ms, VertexConsumerProvider buffer, Text text, float x, float y,
		int color) {
		MinecraftClient.getInstance().textRenderer.draw(text, x, y, color, false, ms.peek()
			.getPositionMatrix(), buffer, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
	}

}
