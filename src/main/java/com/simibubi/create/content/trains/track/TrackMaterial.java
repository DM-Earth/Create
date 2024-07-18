package com.simibubi.create.content.trains.track;

import static com.simibubi.create.content.trains.track.TrackMaterialFactory.make;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;

import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.Create;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

public class TrackMaterial {
	public static final Map<Identifier, TrackMaterial> ALL = new HashMap<>();

	public static final TrackMaterial ANDESITE = make(Create.asResource("andesite"))
			.lang("Andesite")
			.block(NonNullSupplier.lazy(() -> AllBlocks.TRACK))
			.particle(Create.asResource("block/palettes/stone_types/polished/andesite_cut_polished"))
			.defaultModels()
			.build();

	public final Identifier id;
	public final String langName;
	public final NonNullSupplier<NonNullSupplier<? extends TrackBlock>> trackBlock;
	public final Ingredient sleeperIngredient;
	public final Ingredient railsIngredient;
	public final Identifier particle;
	public final TrackType trackType;

	@Nullable
	private final TrackMaterial.TrackType.TrackBlockFactory customFactory;

	@Environment(EnvType.CLIENT)
	protected TrackModelHolder modelHolder;

	@Environment(EnvType.CLIENT)
	public TrackModelHolder getModelHolder() {
		return modelHolder;
	}

	public TrackMaterial(Identifier id, String langName, NonNullSupplier<NonNullSupplier<? extends TrackBlock>> trackBlock,
						 Identifier particle, Ingredient sleeperIngredient, Ingredient railsIngredient,
						 TrackType trackType, Supplier<Supplier<TrackModelHolder>> modelHolder) {
		this(id, langName, trackBlock, particle, sleeperIngredient, railsIngredient, trackType, modelHolder, null);
	}

	public TrackMaterial(Identifier id, String langName, NonNullSupplier<NonNullSupplier<? extends TrackBlock>> trackBlock,
						 Identifier particle, Ingredient sleeperIngredient, Ingredient railsIngredient,
						 TrackType trackType, Supplier<Supplier<TrackModelHolder>> modelHolder,
						 @Nullable TrackType.TrackBlockFactory customFactory) {
		this.id = id;
		this.langName = langName;
		this.trackBlock = trackBlock;
		this.sleeperIngredient = sleeperIngredient;
		this.railsIngredient = railsIngredient;
		this.particle = particle;
		this.trackType = trackType;
		this.customFactory = customFactory;
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> this.modelHolder = modelHolder.get().get());
		ALL.put(this.id, this);
	}

	public NonNullSupplier<? extends TrackBlock> getBlockSupplier() {
		return this.trackBlock.get();
	}

	public TrackBlock getBlock() {
		return getBlockSupplier().get();
	}

	public ItemStack asStack() {
		return asStack(1);
	}

	public ItemStack asStack(int count) {
		return new ItemStack(getBlock(), count);
	}

	public TrackBlock createBlock(AbstractBlock.Settings properties) {
		return (this.customFactory != null ? this.customFactory : this.trackType.factory)
				.create(properties, this);
	}

	public boolean isFromMod(String modId) {
		return this.id.getNamespace().equals(modId);
	}

	public static List<TrackMaterial> allFromMod(String modid) {
		return ALL.values().stream().filter(tm -> tm.isFromMod(modid)).toList();
	}

	public static List<NonNullSupplier<? extends Block>> allBlocksFromMod(String modid) {
		List<NonNullSupplier<? extends Block>> list = new ArrayList<>();
		for (TrackMaterial material : allFromMod(modid)) {
			list.add(material.getBlockSupplier());
		}
		return list;
	}

	public static List<NonNullSupplier<? extends Block>> allBlocks() {
		List<NonNullSupplier<? extends Block>> list = new ArrayList<>();
		for (TrackMaterial material : ALL.values()) {
			list.add(material.getBlockSupplier());
		}
		return list;
	}

	public String resourceName() {
		return this.id.getPath();
	}

	public static TrackMaterial deserialize(String serializedName) {
		if (serializedName.isBlank()) // Data migrating from 0.5
			return ANDESITE;

		Identifier id = Identifier.tryParse(serializedName);
		if (ALL.containsKey(id))
			return ALL.get(id);

		Create.LOGGER.error("Failed to locate serialized track material: " + serializedName);
		return ANDESITE;
	}

	public static class TrackType {
		@FunctionalInterface
		public interface TrackBlockFactory {
			TrackBlock create(AbstractBlock.Settings properties, TrackMaterial material);
		}

		public static final TrackType STANDARD = new TrackType(Create.asResource("standard"), TrackBlock::new);

		public final Identifier id;
		protected final TrackBlockFactory factory;

		public TrackType(Identifier id, TrackBlockFactory factory) {
			this.id = id;
			this.factory = factory;
		}
	}

	public static TrackMaterial fromItem(Item item) {
		if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ITrackBlock trackBlock)
			return trackBlock.getMaterial();
		return TrackMaterial.ANDESITE;
	}

	@Environment(EnvType.CLIENT)
	public record TrackModelHolder(PartialModel tie, PartialModel segment_left, PartialModel segment_right) {
		static final TrackModelHolder DEFAULT = new TrackModelHolder(AllPartialModels.TRACK_TIE,
			AllPartialModels.TRACK_SEGMENT_LEFT, AllPartialModels.TRACK_SEGMENT_RIGHT);
	}
}
