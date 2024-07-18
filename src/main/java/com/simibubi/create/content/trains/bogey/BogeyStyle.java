package com.simibubi.create.content.trains.bogey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import com.jozufozu.flywheel.api.MaterialManager;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.bogey.BogeyRenderer.CommonRenderer;
import com.simibubi.create.content.trains.entity.CarriageBogey;


public class BogeyStyle {

	public final Identifier name;
	public final Identifier cycleGroup;
	public final Text displayName;
	public final Identifier soundType;
	public final ParticleEffect contactParticle;
	public final ParticleEffect smokeParticle;
	public final NbtCompound defaultData;

	private Optional<Supplier<? extends CommonRenderer>> commonRendererFactory;
private Map<BogeySizes.BogeySize, Identifier> sizes;

	@Environment(EnvType.CLIENT)
	private Map<BogeySizes.BogeySize, SizeRenderData> sizeRenderers;

	@Environment(EnvType.CLIENT)
	private Optional<CommonRenderer> commonRenderer;

	public BogeyStyle(Identifier name, Identifier cycleGroup, Text displayName,
		Identifier soundType, ParticleEffect contactParticle, ParticleEffect smokeParticle,
		NbtCompound defaultData, Map<BogeySizes.BogeySize, Identifier> sizes,
		Map<BogeySizes.BogeySize, Supplier<SizeRenderData>> sizeRenderers,
		Optional<Supplier<? extends CommonRenderer>> commonRenderer) {

		this.name = name;
		this.cycleGroup = cycleGroup;
		this.displayName = displayName;
		this.soundType = soundType;
		this.contactParticle = contactParticle;
		this.smokeParticle = smokeParticle;
		this.defaultData = defaultData;
		this.sizes = sizes;

		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			this.sizeRenderers = new HashMap<>();
			sizeRenderers.forEach((k, v) -> this.sizeRenderers.put(k, v.get()));
			this.commonRendererFactory = commonRenderer;
			this.commonRenderer = commonRenderer.map(Supplier::get);
		});
	}

	public Map<Identifier, BogeyStyle> getCycleGroup() {
		return AllBogeyStyles.getCycleGroup(cycleGroup);
	}

	public Block getNextBlock(BogeySizes.BogeySize currentSize) {
		return Stream.iterate(currentSize.increment(), BogeySizes.BogeySize::increment)
				.filter(sizes::containsKey)
				.findFirst()
				.map(this::getBlockOfSize)
				.orElse(getBlockOfSize(currentSize));
	}

	public Block getBlockOfSize(BogeySizes.BogeySize size) {
		return Registries.BLOCK.get(sizes.get(size));
	}

	public Set<BogeySizes.BogeySize> validSizes() {
		return sizes.keySet();
	}

	@NotNull
	public SoundEvent getSoundType() {
		AllSoundEvents.SoundEntry entry = AllSoundEvents.ALL.get(this.soundType);
		if (entry == null || entry.getMainEvent() == null) entry = AllSoundEvents.TRAIN2;
		return entry.getMainEvent();
	}

	@Environment(EnvType.CLIENT)
	public BogeyRenderer createRendererInstance(BogeySizes.BogeySize size) {
		return this.sizeRenderers.get(size).createRenderInstance();
	}

	@Environment(EnvType.CLIENT)
	public BogeyRenderer getInWorldRenderInstance(BogeySizes.BogeySize size) {
		SizeRenderData sizeData = this.sizeRenderers.get(size);
		return sizeData != null ? sizeData.getInWorldInstance() : BackupBogeyRenderer.INSTANCE;
	}

	public Optional<CommonRenderer> getInWorldCommonRenderInstance() {
		return this.commonRenderer;
	}

	public Optional<CommonRenderer> getNewCommonRenderInstance() {
		return this.commonRendererFactory.map(Supplier::get);
	}

	public BogeyInstance createInstance(CarriageBogey bogey, BogeySizes.BogeySize size, MaterialManager materialManager) {
		return new BogeyInstance(bogey, this, size, materialManager);
	}

	@Environment(EnvType.CLIENT)
	public record SizeRenderData(Supplier<? extends BogeyRenderer> rendererFactory, BogeyRenderer instance) {
		public BogeyRenderer createRenderInstance() {
			return rendererFactory.get();
		}

		public BogeyRenderer getInWorldInstance() {
			return instance;
		}
	}
}
