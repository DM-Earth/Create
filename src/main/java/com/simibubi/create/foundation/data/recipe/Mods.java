package com.simibubi.create.foundation.data.recipe;

import java.util.function.Consumer;
import net.minecraft.util.Identifier;

public enum Mods {

	MEK("mekanism", b -> b.reverseMetalPrefix()),
	TH("thermal"),
	IE("immersiveengineering", b -> b.reverseMetalPrefix()),

	FD("farmersdelight"),
	ARS_N("ars_nouveau"),
	BSK("blue_skies"),
	BTN("botania", b -> b.omitWoodSuffix()),
	FA("forbidden_arcanus", b -> b.omitWoodSuffix()),
	HEX("hexcasting"),
	ID("integrateddynamics", b -> b.strippedWoodIsSuffix()),
	BYG("byg"),
	SG("silentgear"),
	TIC("tconstruct"),
	AP("architects_palette"),
	Q("quark"),
	BOP("biomesoplenty"),
	TF("twilightforest"),
	ECO("ecologics"),

	ENV("environmental"),
	ATM("atmospheric"),
	ATM_2("atmospheric", b -> b.omitWoodSuffix()),
	IX("infernalexp"),
	GOOD("goodending"),
	BMK("biomemakeover"),

	// Fabric Mods
	TR("techreborn"),
	MI("modern_industrialization"),
	MTM("mythicmetals"),
	ALG("alloygery"),
	BC("bitsandchisels")

	;

	private final String id;

	public boolean reversedMetalPrefix;
	public boolean strippedIsSuffix;
	public boolean omitWoodSuffix;

	private Mods(String id) {
		this(id, b -> {
		});
	}

	private Mods(String id, Consumer<Builder> props) {
		props.accept(new Builder());
		this.id = id;
	}

	public Identifier ingotOf(String type) {
		return new Identifier(id, reversedMetalPrefix ? "ingot_" + type : type + "_ingot");
	}

	public Identifier nuggetOf(String type) {
		return new Identifier(id, reversedMetalPrefix ? "nugget_" + type : type + "_nugget");
	}

	public Identifier oreOf(String type) {
		return new Identifier(id, reversedMetalPrefix ? "ore_" + type : type + "_ore");
	}

	public Identifier deepslateOreOf(String type) {
		return new Identifier(id, reversedMetalPrefix ? "deepslate_ore_" + type : "deepslate_" + type + "_ore");
	}

	public Identifier asResource(String id) {
		return new Identifier(this.id, id);
	}

	public String recipeId(String id) {
		return "compat/" + this.id + "/" + id;
	}

	public String getId() {
		return id;
	}

	class Builder {

		Builder reverseMetalPrefix() {
			reversedMetalPrefix = true;
			return this;
		}

		Builder strippedWoodIsSuffix() {
			strippedIsSuffix = true;
			return this;
		}

		Builder omitWoodSuffix() {
			omitWoodSuffix = true;
			return this;
		}

	}

}
