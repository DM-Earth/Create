package com.simibubi.create;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import com.simibubi.create.content.trains.bogey.BogeyRenderer.CommonRenderer;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyRenderer.CommonStandardBogeyRenderer;
import com.simibubi.create.content.trains.bogey.StandardBogeyRenderer.LargeStandardBogeyRenderer;
import com.simibubi.create.content.trains.bogey.StandardBogeyRenderer.SmallStandardBogeyRenderer;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AllBogeyStyles {
	public static final Map<Identifier, BogeyStyle> BOGEY_STYLES = new HashMap<>();
	public static final Map<Identifier, Map<Identifier, BogeyStyle>> CYCLE_GROUPS = new HashMap<>();
	private static final Map<Identifier, BogeyStyle> EMPTY_GROUP = ImmutableMap.of();

	public static Map<Identifier, BogeyStyle> getCycleGroup(Identifier cycleGroup) {
		return CYCLE_GROUPS.getOrDefault(cycleGroup, EMPTY_GROUP);
	}

	public static final String STANDARD_CYCLE_GROUP = "standard";

	public static final BogeyStyle STANDARD =
		create("standard", STANDARD_CYCLE_GROUP).commonRenderer(() -> CommonStandardBogeyRenderer::new)
			.displayName(Components.translatable("create.bogey.style.standard"))
			.size(BogeySizes.SMALL, () -> SmallStandardBogeyRenderer::new, AllBlocks.SMALL_BOGEY)
			.size(BogeySizes.LARGE, () -> LargeStandardBogeyRenderer::new, AllBlocks.LARGE_BOGEY)
			.build();

	private static BogeyStyleBuilder create(String name, String cycleGroup) {
		return create(Create.asResource(name), Create.asResource(cycleGroup));
	}

	public static BogeyStyleBuilder create(Identifier name, Identifier cycleGroup) {
		return new BogeyStyleBuilder(name, cycleGroup);
	}

	public static void register() {}

	public static class BogeyStyleBuilder {
		protected final Map<BogeySizes.BogeySize, Supplier<BogeyStyle.SizeRenderData>> sizeRenderers = new HashMap<>();
		protected final Map<BogeySizes.BogeySize, Identifier> sizes = new HashMap<>();
		protected final Identifier name;
		protected final Identifier cycleGroup;

		protected Text displayName = Lang.translateDirect("bogey.style.invalid");
		protected Identifier soundType = AllSoundEvents.TRAIN2.getId();
		protected NbtCompound defaultData = new NbtCompound();
		protected ParticleEffect contactParticle = ParticleTypes.CRIT;
		protected ParticleEffect smokeParticle = ParticleTypes.POOF;
		protected Optional<Supplier<? extends CommonRenderer>> commonRenderer = Optional.empty();

		public BogeyStyleBuilder(Identifier name, Identifier cycleGroup) {
			this.name = name;
			this.cycleGroup = cycleGroup;
		}

		public BogeyStyleBuilder displayName(Text displayName) {
			this.displayName = displayName;
			return this;
		}

		public BogeyStyleBuilder soundType(Identifier soundType) {
			this.soundType = soundType;
			return this;
		}

		public BogeyStyleBuilder defaultData(NbtCompound defaultData) {
			this.defaultData = defaultData;
			return this;
		}

		public BogeyStyleBuilder size(BogeySizes.BogeySize size, Supplier<Supplier<? extends BogeyRenderer>> renderer,
			BlockEntry<? extends AbstractBogeyBlock<?>> blockEntry) {
			this.size(size, renderer, blockEntry.getId());
			return this;
		}

		public BogeyStyleBuilder size(BogeySizes.BogeySize size, Supplier<Supplier<? extends BogeyRenderer>> renderer,
			Identifier location) {
			this.sizes.put(size, location);
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
				this.sizeRenderers.put(size, () -> new BogeyStyle.SizeRenderData(renderer.get(), renderer.get()
					.get()));
			});
			return this;
		}

		public BogeyStyleBuilder contactParticle(ParticleEffect contactParticle) {
			this.contactParticle = contactParticle;
			return this;
		}

		public BogeyStyleBuilder smokeParticle(ParticleEffect smokeParticle) {
			this.smokeParticle = smokeParticle;
			return this;
		}

		public BogeyStyleBuilder commonRenderer(Supplier<Supplier<? extends CommonRenderer>> commonRenderer) {
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
				this.commonRenderer = Optional.of(commonRenderer.get());
			});
			return this;
		}

		public BogeyStyle build() {
			BogeyStyle entry = new BogeyStyle(name, cycleGroup, displayName, soundType, contactParticle, smokeParticle,
				defaultData, sizes, sizeRenderers, commonRenderer);
			BOGEY_STYLES.put(name, entry);
			CYCLE_GROUPS.computeIfAbsent(cycleGroup, l -> new HashMap<>())
				.put(name, entry);
			return entry;
		}
	}
}
