package com.simibubi.create.content.decoration.palettes;

import static com.simibubi.create.content.decoration.palettes.PaletteBlockPattern.STANDARD_RANGE;
import static com.simibubi.create.content.decoration.palettes.PaletteBlockPattern.VANILLA_RANGE;

import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.utility.Lang;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

public enum AllPaletteStoneTypes {

	GRANITE(VANILLA_RANGE, r -> () -> Blocks.GRANITE),
	DIORITE(VANILLA_RANGE, r -> () -> Blocks.DIORITE),
	ANDESITE(VANILLA_RANGE, r -> () -> Blocks.ANDESITE),
	CALCITE(VANILLA_RANGE, r -> () -> Blocks.CALCITE),
	DRIPSTONE(VANILLA_RANGE, r -> () -> Blocks.DRIPSTONE_BLOCK),
	DEEPSLATE(VANILLA_RANGE, r -> () -> Blocks.DEEPSLATE),
	TUFF(VANILLA_RANGE, r -> () -> Blocks.TUFF),

	ASURINE(STANDARD_RANGE, r -> r.paletteStoneBlock("asurine", () -> Blocks.DEEPSLATE, true, true)
		.properties(p -> p.hardness(1.25f)
			.mapColor(MapColor.BLUE))
		.register()),

	CRIMSITE(STANDARD_RANGE, r -> r.paletteStoneBlock("crimsite", () -> Blocks.DEEPSLATE, true, true)
		.properties(p -> p.hardness(1.25f)
			.mapColor(MapColor.RED))
		.register()),

	LIMESTONE(STANDARD_RANGE, r -> r.paletteStoneBlock("limestone", () -> Blocks.SANDSTONE, true, false)
		.properties(p -> p.hardness(1.25f)
			.mapColor(MapColor.PALE_YELLOW))
		.register()),

	OCHRUM(STANDARD_RANGE, r -> r.paletteStoneBlock("ochrum", () -> Blocks.CALCITE, true, true)
		.properties(p -> p.hardness(1.25f)
			.mapColor(MapColor.TERRACOTTA_YELLOW))
		.register()),

	SCORIA(STANDARD_RANGE, r -> r.paletteStoneBlock("scoria", () -> Blocks.BLACKSTONE, true, false)
		.properties(p -> p.mapColor(MapColor.BROWN))
		.register()),

	SCORCHIA(STANDARD_RANGE, r -> r.paletteStoneBlock("scorchia", () -> Blocks.BLACKSTONE, true, false)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_GRAY))
		.register()),

	VERIDIUM(STANDARD_RANGE, r -> r.paletteStoneBlock("veridium", () -> Blocks.TUFF, true, true)
		.properties(p -> p.hardness(1.25f)
			.mapColor(MapColor.TEAL))
		.register())

	;

	private Function<CreateRegistrate, NonNullSupplier<Block>> factory;
	private PalettesVariantEntry variants;

	public NonNullSupplier<Block> baseBlock;
	public PaletteBlockPattern[] variantTypes;
	public TagKey<Item> materialTag;

	private AllPaletteStoneTypes(PaletteBlockPattern[] variantTypes,
		Function<CreateRegistrate, NonNullSupplier<Block>> factory) {
		this.factory = factory;
		this.variantTypes = variantTypes;
	}

	public NonNullSupplier<Block> getBaseBlock() {
		return baseBlock;
	}

	public PalettesVariantEntry getVariants() {
		return variants;
	}

	public static void register(CreateRegistrate registrate) {
		for (AllPaletteStoneTypes paletteStoneVariants : values()) {
			NonNullSupplier<Block> baseBlock = paletteStoneVariants.factory.apply(registrate);
			paletteStoneVariants.baseBlock = baseBlock;
			String id = Lang.asId(paletteStoneVariants.name());
			paletteStoneVariants.materialTag =
				AllTags.optionalTag(Registries.ITEM, Create.asResource("stone_types/" + id));
			paletteStoneVariants.variants = new PalettesVariantEntry(id, paletteStoneVariants);
		}
	}

}
