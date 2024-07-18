package com.simibubi.create.foundation.utility;

import java.util.List;

import joptsimple.internal.Strings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class LangBuilder {

	String namespace;
	MutableText component;

	public LangBuilder(String namespace) {
		this.namespace = namespace;
	}

	public LangBuilder space() {
		return text(" ");
	}

	public LangBuilder newLine() {
		return text("\n");
	}

	/**
	 * Appends a localised component<br>
	 * To add an independently formatted localised component, use add() and a nested
	 * builder
	 * 
	 * @param langKey
	 * @param args
	 * @return
	 */
	public LangBuilder translate(String langKey, Object... args) {
		return add(Components.translatable(namespace + "." + langKey, Lang.resolveBuilders(args)));
	}

	/**
	 * Appends a text component
	 * 
	 * @param literalText
	 * @return
	 */
	public LangBuilder text(String literalText) {
		return add(Components.literal(literalText));
	}

	/**
	 * Appends a colored text component
	 * 
	 * @param format
	 * @param literalText
	 * @return
	 */
	public LangBuilder text(Formatting format, String literalText) {
		return add(Components.literal(literalText).formatted(format));
	}

	/**
	 * Appends a colored text component
	 * 
	 * @param color
	 * @param literalText
	 * @return
	 */
	public LangBuilder text(int color, String literalText) {
		return add(Components.literal(literalText).styled(s -> s.withColor(color)));
	}

	/**
	 * Appends the contents of another builder
	 * 
	 * @param otherBuilder
	 * @return
	 */
	public LangBuilder add(LangBuilder otherBuilder) {
		return add(otherBuilder.component());
	}

	/**
	 * Appends a component
	 * 
	 * @param customComponent
	 * @return
	 */
	public LangBuilder add(MutableText customComponent) {
		component = component == null ? customComponent : component.append(customComponent);
		return this;
	}

	//

	/**
	 * Applies the format to all added components
	 * 
	 * @param format
	 * @return
	 */
	public LangBuilder style(Formatting format) {
		assertComponent();
		component = component.formatted(format);
		return this;
	}

	/**
	 * Applies the color to all added components
	 * 
	 * @param color
	 * @return
	 */
	public LangBuilder color(int color) {
		assertComponent();
		component = component.styled(s -> s.withColor(color));
		return this;
	}

	//

	public MutableText component() {
		assertComponent();
		return component;
	}

	public String string() {
		return component().getString();
	}

	public String json() {
		return Text.Serializer.toJson(component());
	}

	public void sendStatus(PlayerEntity player) {
		player.sendMessage(component(), true);
	}

	public void sendChat(PlayerEntity player) {
		player.sendMessage(component(), false);
	}

	public void addTo(List<? super MutableText> tooltip) {
		tooltip.add(component());
	}

	public void forGoggles(List<? super MutableText> tooltip) {
		forGoggles(tooltip, 0);
	}

	public void forGoggles(List<? super MutableText> tooltip, int indents) {
		tooltip.add(Lang.builder()
			.text(Strings.repeat(' ', 4 + indents))
			.add(this)
			.component());
	}

	//

	private void assertComponent() {
		if (component == null)
			throw new IllegalStateException("No components were added to builder");
	}

}