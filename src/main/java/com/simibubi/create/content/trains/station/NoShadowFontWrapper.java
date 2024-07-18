package com.simibubi.create.content.trains.station;

import java.util.List;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.FontAccessor;

import org.joml.Matrix4f;

public class NoShadowFontWrapper extends TextRenderer {

	private TextRenderer wrapped;

	public NoShadowFontWrapper(TextRenderer wrapped) {
		super(null, false);
		this.wrapped = wrapped;
	}

	public FontStorage getFontStorage(Identifier pFontLocation) {
		return ((FontAccessor) wrapped).port_lib$getFontSet(pFontLocation);
	}

	@Override
	public int draw(Text pText, float pX, float pY, int pColor, boolean pDropShadow, Matrix4f pMatrix,
		VertexConsumerProvider pBuffer, TextLayerType pDisplayMode, int pBackgroundColor, int pPackedLightCoords) {
		return wrapped.draw(pText, pX, pY, pColor, false, pMatrix, pBuffer, pDisplayMode, pBackgroundColor,
			pPackedLightCoords);
	}

	@Override
	public int draw(OrderedText pText, float pX, float pY, int pColor, boolean pDropShadow,
		Matrix4f pMatrix, VertexConsumerProvider pBuffer, TextLayerType pDisplayMode, int pBackgroundColor,
		int pPackedLightCoords) {
		return wrapped.draw(pText, pX, pY, pColor, false, pMatrix, pBuffer, pDisplayMode, pBackgroundColor,
			pPackedLightCoords);
	}

	@Override
	public int draw(String pText, float pX, float pY, int pColor, boolean pDropShadow, Matrix4f pMatrix,
		VertexConsumerProvider pBuffer, TextLayerType pDisplayMode, int pBackgroundColor, int pPackedLightCoords) {
		return wrapped.draw(pText, pX, pY, pColor, false, pMatrix, pBuffer, pDisplayMode, pBackgroundColor,
			pPackedLightCoords);
	}

	@Override
	public int draw(String pText, float pX, float pY, int pColor, boolean pDropShadow, Matrix4f pMatrix,
		VertexConsumerProvider pBuffer, TextLayerType pDisplayMode, int pBackgroundColor, int pPackedLightCoords,
		boolean pBidirectional) {
		return wrapped.draw(pText, pX, pY, pColor, false, pMatrix, pBuffer, pDisplayMode, pBackgroundColor,
			pPackedLightCoords, pBidirectional);
	}

	@Override
	public int getWrappedLinesHeight(StringVisitable pText, int pMaxWidth) {
		return wrapped.getWrappedLinesHeight(pText, pMaxWidth);
	}

	public String mirror(String pText) {
		return wrapped.mirror(pText);
	}

	public void drawWithOutline(OrderedText pText, float pX, float pY, int pColor, int pBackgroundColor,
		Matrix4f pMatrix, VertexConsumerProvider pBuffer, int pPackedLightCoords) {
		wrapped.drawWithOutline(pText, pX, pY, pColor, pBackgroundColor, pMatrix, pBuffer, pPackedLightCoords);
	}

	public int getWidth(String pText) {
		return wrapped.getWidth(pText);
	}

	public int getWidth(StringVisitable pText) {
		return wrapped.getWidth(pText);
	}

	public int getWidth(OrderedText pText) {
		return wrapped.getWidth(pText);
	}

	public String trimToWidth(String p_92838_, int p_92839_, boolean p_92840_) {
		return wrapped.trimToWidth(p_92838_, p_92839_, p_92840_);
	}

	public String trimToWidth(String pText, int pMaxWidth) {
		return wrapped.trimToWidth(pText, pMaxWidth);
	}

	public StringVisitable trimToWidth(StringVisitable pText, int pMaxWidth) {
		return wrapped.trimToWidth(pText, pMaxWidth);
	}

	public int getWrappedLinesHeight(String pStr, int pMaxWidth) {
		return wrapped.getWrappedLinesHeight(pStr, pMaxWidth);
	}

	public List<OrderedText> wrapLines(StringVisitable pText, int pMaxWidth) {
		return wrapped.wrapLines(pText, pMaxWidth);
	}

	public boolean isRightToLeft() {
		return wrapped.isRightToLeft();
	}

	public TextHandler getTextHandler() {
		return wrapped.getTextHandler();
	}

}
