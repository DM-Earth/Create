package com.simibubi.create;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataProvider.Factory;
import net.minecraft.data.DataWriter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

//@EventBusSubscriber(bus = Bus.FORGE)
public class AllSoundEvents {

	public static final Map<Identifier, SoundEntry> ALL = new HashMap<>();

	public static final SoundEntry

	SCHEMATICANNON_LAUNCH_BLOCK = create("schematicannon_launch_block").subtitle("Schematicannon fires")
		.playExisting(SoundEvents.ENTITY_GENERIC_EXPLODE, .1f, 1.1f)
		.category(SoundCategory.BLOCKS)
		.build(),

		SCHEMATICANNON_FINISH = create("schematicannon_finish").subtitle("Schematicannon dings")
			.playExisting(SoundEvents.BLOCK_NOTE_BLOCK_BELL, 1, .7f)
			.category(SoundCategory.BLOCKS)
			.build(),

		DEPOT_SLIDE = create("depot_slide").subtitle("Item slides")
			.playExisting(SoundEvents.BLOCK_SAND_BREAK, .125f, 1.5f)
			.category(SoundCategory.BLOCKS)
			.build(),

		DEPOT_PLOP = create("depot_plop").subtitle("Item lands")
			.playExisting(SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, .25f, 1.25f)
			.category(SoundCategory.BLOCKS)
			.build(),

		FUNNEL_FLAP = create("funnel_flap").subtitle("Funnel flaps")
			.playExisting(SoundEvents.ENTITY_ITEM_FRAME_ROTATE_ITEM, .125f, 1.5f)
			.playExisting(SoundEvents.BLOCK_WOOL_BREAK, .0425f, .75f)
			.category(SoundCategory.BLOCKS)
			.build(),

		SLIME_ADDED = create("slime_added").subtitle("Slime squishes")
			.playExisting(SoundEvents.BLOCK_SLIME_BLOCK_PLACE)
			.category(SoundCategory.BLOCKS)
			.build(),

		MECHANICAL_PRESS_ACTIVATION = create("mechanical_press_activation").subtitle("Mechanical Press clangs")
			.playExisting(SoundEvents.BLOCK_ANVIL_LAND, .125f, 1f)
			.playExisting(SoundEvents.ENTITY_ITEM_BREAK, .5f, 1f)
			.category(SoundCategory.BLOCKS)
			.build(),

		MECHANICAL_PRESS_ACTIVATION_ON_BELT =
			create("mechanical_press_activation_belt").subtitle("Mechanical Press bonks")
				.playExisting(SoundEvents.BLOCK_WOOL_HIT, .75f, 1f)
				.playExisting(SoundEvents.ENTITY_ITEM_BREAK, .15f, .75f)
				.category(SoundCategory.BLOCKS)
				.build(),

		MIXING = create("mixing").subtitle("Mixing noises")
			.playExisting(SoundEvents.BLOCK_GILDED_BLACKSTONE_BREAK, .125f, .5f)
			.playExisting(SoundEvents.BLOCK_NETHERRACK_BREAK, .125f, .5f)
			.category(SoundCategory.BLOCKS)
			.build(),

		CRANKING = create("cranking").subtitle("Hand Crank turns")
			.playExisting(SoundEvents.BLOCK_WOOD_PLACE, .075f, .5f)
			.playExisting(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_OFF, .025f, .5f)
			.category(SoundCategory.BLOCKS)
			.build(),

		WORLDSHAPER_PLACE = create("worldshaper_place").subtitle("Worldshaper zaps")
			.playExisting(SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM)
			.category(SoundCategory.PLAYERS)
			.build(),

		SCROLL_VALUE = create("scroll_value").subtitle("Scroll-input clicks")
			.playExisting(SoundEvents.BLOCK_NOTE_BLOCK_HAT, .124f, 1f)
			.category(SoundCategory.PLAYERS)
			.build(),

		CONFIRM = create("confirm").subtitle("Affirmative ding")
			.playExisting(SoundEvents.BLOCK_NOTE_BLOCK_BELL, 0.5f, 0.8f)
			.category(SoundCategory.PLAYERS)
			.build(),

		DENY = create("deny").subtitle("Declining boop")
			.playExisting(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f)
			.category(SoundCategory.PLAYERS)
			.build(),

		COGS = create("cogs").subtitle("Cogwheels rumble")
			.category(SoundCategory.BLOCKS)
			.build(),

		FWOOMP = create("fwoomp").subtitle("Potato Launcher fwoomps")
			.category(SoundCategory.PLAYERS)
			.build(),

		POTATO_HIT = create("potato_hit").subtitle("Vegetable impacts")
			.playExisting(SoundEvents.ENTITY_ITEM_FRAME_BREAK, .75f, .75f)
			.playExisting(SoundEvents.BLOCK_WEEPING_VINES_BREAK, .75f, 1.25f)
			.category(SoundCategory.PLAYERS)
			.build(),

		CONTRAPTION_ASSEMBLE = create("contraption_assemble").subtitle("Contraption moves")
			.playExisting(SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN, .5f, .5f)
			.playExisting(SoundEvents.BLOCK_CHEST_OPEN, .045f, .74f)
			.category(SoundCategory.BLOCKS)
			.build(),

		CONTRAPTION_DISASSEMBLE = create("contraption_disassemble").subtitle("Contraption stops")
			.playExisting(SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, .35f, .75f)
			.category(SoundCategory.BLOCKS)
			.build(),

		WRENCH_ROTATE = create("wrench_rotate").subtitle("Wrench used")
			.playExisting(SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, .25f, 1.25f)
			.category(SoundCategory.BLOCKS)
			.build(),

		WRENCH_REMOVE = create("wrench_remove").subtitle("Component breaks")
			.playExisting(SoundEvents.ENTITY_ITEM_PICKUP, .25f, .75f)
			.playExisting(SoundEvents.BLOCK_NETHERITE_BLOCK_HIT, .25f, .75f)
			.category(SoundCategory.BLOCKS)
			.build(),

		CRAFTER_CLICK = create("crafter_click").subtitle("Crafter clicks")
			.playExisting(SoundEvents.BLOCK_NETHERITE_BLOCK_HIT, .25f, 1)
			.playExisting(SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN, .125f, 1)
			.category(SoundCategory.BLOCKS)
			.build(),

		CRAFTER_CRAFT = create("crafter_craft").subtitle("Crafter crafts")
			.playExisting(SoundEvents.ENTITY_ITEM_BREAK, .125f, .75f)
			.category(SoundCategory.BLOCKS)
			.build(),

		COPPER_ARMOR_EQUIP = create("copper_armor_equip").subtitle("Diving equipment clinks")
			.playExisting(SoundEvents.ITEM_ARMOR_EQUIP_GOLD, 1f, 1f)
			.category(SoundCategory.PLAYERS)
			.build(),

		SANDING_SHORT = create("sanding_short").subtitle("Sanding noises")
			.addVariant("sanding_short_1")
			.category(SoundCategory.BLOCKS)
			.build(),

		SANDING_LONG = create("sanding_long").subtitle("Sanding noises")
			.category(SoundCategory.BLOCKS)
			.build(),

		CONTROLLER_CLICK = create("controller_click").subtitle("Controller clicks")
			.playExisting(SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, .35f, 1f)
			.category(SoundCategory.BLOCKS)
			.build(),

		CONTROLLER_PUT = create("controller_put").subtitle("Controller thumps")
			.playExisting(SoundEvents.ITEM_BOOK_PUT, 1f, 1f)
			.category(SoundCategory.BLOCKS)
			.build(),

		CONTROLLER_TAKE = create("controller_take").subtitle("Lectern empties")
			.playExisting(SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1f, 1f)
			.category(SoundCategory.BLOCKS)
			.build(),

		SAW_ACTIVATE_WOOD = create("saw_activate_wood").subtitle("Mechanical Saw activates")
			.playExisting(SoundEvents.ENTITY_BOAT_PADDLE_LAND, .75f, 1.5f)
			.category(SoundCategory.BLOCKS)
			.build(),

		SAW_ACTIVATE_STONE = create("saw_activate_stone").subtitle("Mechanical Saw activates")
			.playExisting(SoundEvents.UI_STONECUTTER_TAKE_RESULT, .125f, 1.25f)
			.category(SoundCategory.BLOCKS)
			.build(),

		BLAZE_MUNCH = create("blaze_munch").subtitle("Blaze Burner munches")
			.playExisting(SoundEvents.ENTITY_GENERIC_EAT, .5f, 1f)
			.category(SoundCategory.BLOCKS)
			.build(),

		CRUSHING_1 = create("crushing_1").subtitle("Crushing noises")
			.playExisting(SoundEvents.BLOCK_NETHERRACK_HIT)
			.category(SoundCategory.BLOCKS)
			.build(),

		CRUSHING_2 = create("crushing_2").noSubtitle()
			.playExisting(SoundEvents.BLOCK_GRAVEL_PLACE)
			.category(SoundCategory.BLOCKS)
			.build(),

		CRUSHING_3 = create("crushing_3").noSubtitle()
			.playExisting(SoundEvents.BLOCK_NETHERITE_BLOCK_BREAK)
			.category(SoundCategory.BLOCKS)
			.build(),

		PECULIAR_BELL_USE = create("peculiar_bell_use").subtitle("Peculiar Bell tolls")
			.playExisting(SoundEvents.BLOCK_BELL_USE)
			.category(SoundCategory.BLOCKS)
			.build(),

		WHISTLE_HIGH = create("whistle_high").subtitle("High whistling")
			.category(SoundCategory.RECORDS)
			.attenuationDistance(64)
			.build(),

		WHISTLE_MEDIUM = create("whistle").subtitle("Whistling")
			.category(SoundCategory.RECORDS)
			.attenuationDistance(64)
			.build(),

		WHISTLE_LOW = create("whistle_low").subtitle("Low whistling")
			.category(SoundCategory.RECORDS)
			.attenuationDistance(64)
			.build(),

		STEAM = create("steam").subtitle("Steam noises")
			.category(SoundCategory.NEUTRAL)
			.attenuationDistance(32)
			.build(),

		TRAIN = create("train").subtitle("Bogey wheels rumble")
			.category(SoundCategory.NEUTRAL)
			.attenuationDistance(128)
			.build(),

		TRAIN2 = create("train2").noSubtitle()
			.category(SoundCategory.NEUTRAL)
			.attenuationDistance(128)
			.build(),

		TRAIN3 = create("train3").subtitle("Bogey wheels rumble muffled")
			.category(SoundCategory.NEUTRAL)
			.attenuationDistance(16)
			.build(),

		WHISTLE_TRAIN = create("whistle_train").subtitle("Whistling")
			.category(SoundCategory.RECORDS)
			.build(),

		WHISTLE_TRAIN_LOW = create("whistle_train_low").subtitle("Low whistling")
			.category(SoundCategory.RECORDS)
			.build(),

		WHISTLE_TRAIN_MANUAL = create("whistle_train_manual").subtitle("Train honks")
			.category(SoundCategory.NEUTRAL)
			.attenuationDistance(64)
			.build(),

		WHISTLE_TRAIN_MANUAL_LOW = create("whistle_train_manual_low").subtitle("Train honks")
			.category(SoundCategory.NEUTRAL)
			.attenuationDistance(64)
			.build(),

		WHISTLE_TRAIN_MANUAL_END = create("whistle_train_manual_end").noSubtitle()
			.category(SoundCategory.NEUTRAL)
			.attenuationDistance(64)
			.build(),

		WHISTLE_TRAIN_MANUAL_LOW_END = create("whistle_train_manual_low_end").noSubtitle()
			.category(SoundCategory.NEUTRAL)
			.attenuationDistance(64)
			.build(),

		WHISTLE_CHIFF = create("chiff").noSubtitle()
			.category(SoundCategory.RECORDS)
			.build(),

		HAUNTED_BELL_CONVERT = create("haunted_bell_convert").subtitle("Haunted Bell awakens")
			.category(SoundCategory.BLOCKS)
			.build(),

		HAUNTED_BELL_USE = create("haunted_bell_use").subtitle("Haunted Bell tolls")
			.category(SoundCategory.BLOCKS)
			.build();

	private static SoundEntryBuilder create(String name) {
		return create(Create.asResource(name));
	}

	public static SoundEntryBuilder create(Identifier id) {
		return new SoundEntryBuilder(id);
	}

	public static void prepare() {
		for (SoundEntry entry : ALL.values())
			entry.prepare();
	}

	public static void register() {
		for (SoundEntry entry : ALL.values())
			entry.register();
	}

	public static void provideLang(BiConsumer<String, String> consumer) {
		for (SoundEntry entry : ALL.values())
			if (entry.hasSubtitle())
				consumer.accept(entry.getSubtitleKey(), entry.getSubtitle());
	}

	public static DataProvider provider(FabricDataOutput output) {
		return new SoundEntryProvider(output);
	}

	public static void playItemPickup(PlayerEntity player) {
		player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .2f,
			1f + Create.RANDOM.nextFloat());
	}

//	@SubscribeEvent
//	public static void cancelSubtitlesOfCompoundedSounds(PlaySoundEvent event) {
//		ResourceLocation soundLocation = event.getSound().getSoundLocation();
//		if (!soundLocation.getNamespace().equals(Create.ID))
//			return;
//		if (soundLocation.getPath().contains("_compounded_")
//			event.setResultSound();
//
//	}

	private static class SoundEntryProvider implements DataProvider {

		private DataOutput output;

		public SoundEntryProvider(DataOutput output) {
			this.output = output;
		}

		@Override
		public CompletableFuture<?> run(DataWriter cache) {
			return generate(output.getPath(), cache);
		}

		@Override
		public String getName() {
			return "Create's Custom Sounds";
		}

		public CompletableFuture<?> generate(Path path, DataWriter cache) {
			path = path.resolve("assets/create");
			JsonObject json = new JsonObject();
			ALL.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> {
					entry.getValue()
						.write(json);
				});
			return DataProvider.writeToPath(cache, json, path.resolve("sounds.json"));
		}

	}

	public record ConfiguredSoundEvent(Supplier<SoundEvent> event, float volume, float pitch) {
	}

	public static class SoundEntryBuilder {

		protected Identifier id;
		protected String subtitle = "unregistered";
		protected SoundCategory category = SoundCategory.BLOCKS;
		protected List<ConfiguredSoundEvent> wrappedEvents;
		protected List<Identifier> variants;
		protected int attenuationDistance;

		public SoundEntryBuilder(Identifier id) {
			wrappedEvents = new ArrayList<>();
			variants = new ArrayList<>();
			this.id = id;
		}

		public SoundEntryBuilder subtitle(String subtitle) {
			this.subtitle = subtitle;
			return this;
		}

		public SoundEntryBuilder attenuationDistance(int distance) {
			this.attenuationDistance = distance;
			return this;
		}

		public SoundEntryBuilder noSubtitle() {
			this.subtitle = null;
			return this;
		}

		public SoundEntryBuilder category(SoundCategory category) {
			this.category = category;
			return this;
		}

		public SoundEntryBuilder addVariant(String name) {
			return addVariant(Create.asResource(name));
		}

		public SoundEntryBuilder addVariant(Identifier id) {
			variants.add(id);
			return this;
		}

		public SoundEntryBuilder playExisting(Supplier<SoundEvent> event, float volume, float pitch) {
			wrappedEvents.add(new ConfiguredSoundEvent(event, volume, pitch));
			return this;
		}

		// fabric: holders are not suppliers
		public SoundEntryBuilder playExisting(RegistryEntry<SoundEvent> event, float volume, float pitch) {
			return playExisting(event::value, volume, pitch);
		}

		public SoundEntryBuilder playExisting(SoundEvent event, float volume, float pitch) {
			return playExisting(() -> event, volume, pitch);
		}

		public SoundEntryBuilder playExisting(SoundEvent event) {
			return playExisting(event, 1, 1);
		}

		public SoundEntryBuilder playExisting(RegistryEntry<SoundEvent> event) {
			return playExisting(event::value, 1, 1);
		}

		public SoundEntry build() {
			SoundEntry entry =
				wrappedEvents.isEmpty() ? new CustomSoundEntry(id, variants, subtitle, category, attenuationDistance)
					: new WrappedSoundEntry(id, subtitle, wrappedEvents, category, attenuationDistance);
			ALL.put(entry.getId(), entry);
			return entry;
		}

	}

	public static abstract class SoundEntry {

		protected Identifier id;
		protected String subtitle;
		protected SoundCategory category;
		protected int attenuationDistance;

		public SoundEntry(Identifier id, String subtitle, SoundCategory category, int attenuationDistance) {
			this.id = id;
			this.subtitle = subtitle;
			this.category = category;
			this.attenuationDistance = attenuationDistance;
		}

		public abstract void prepare();

		public abstract void register();

		public abstract void write(JsonObject json);

		public abstract SoundEvent getMainEvent();

		public String getSubtitleKey() {
			return id.getNamespace() + ".subtitle." + id.getPath();
		}

		public Identifier getId() {
			return id;
		}

		public boolean hasSubtitle() {
			return subtitle != null;
		}

		public String getSubtitle() {
			return subtitle;
		}

		public void playOnServer(World world, Vec3i pos) {
			playOnServer(world, pos, 1, 1);
		}

		public void playOnServer(World world, Vec3i pos, float volume, float pitch) {
			play(world, null, pos, volume, pitch);
		}

		public void play(World world, PlayerEntity entity, Vec3i pos) {
			play(world, entity, pos, 1, 1);
		}

		public void playFrom(Entity entity) {
			playFrom(entity, 1, 1);
		}

		public void playFrom(Entity entity, float volume, float pitch) {
			if (!entity.isSilent())
				play(entity.getWorld(), null, entity.getBlockPos(), volume, pitch);
		}

		public void play(World world, PlayerEntity entity, Vec3i pos, float volume, float pitch) {
			play(world, entity, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, volume, pitch);
		}

		public void play(World world, PlayerEntity entity, Vec3d pos, float volume, float pitch) {
			play(world, entity, pos.getX(), pos.getY(), pos.getZ(), volume, pitch);
		}

		public abstract void play(World world, PlayerEntity entity, double x, double y, double z, float volume, float pitch);

		public void playAt(World world, Vec3i pos, float volume, float pitch, boolean fade) {
			playAt(world, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, volume, pitch, fade);
		}

		public void playAt(World world, Vec3d pos, float volume, float pitch, boolean fade) {
			playAt(world, pos.getX(), pos.getY(), pos.getZ(), volume, pitch, fade);
		}

		public abstract void playAt(World world, double x, double y, double z, float volume, float pitch, boolean fade);

	}

	private static class WrappedSoundEntry extends SoundEntry {

		private List<ConfiguredSoundEvent> wrappedEvents;
		private List<CompiledSoundEvent> compiledEvents;

		public WrappedSoundEntry(Identifier id, String subtitle,
			List<ConfiguredSoundEvent> wrappedEvents, SoundCategory category, int attenuationDistance) {
			super(id, subtitle, category, attenuationDistance);
			this.wrappedEvents = wrappedEvents;
			compiledEvents = new ArrayList<>();
		}

		@Override
		public void prepare() {
			for (int i = 0; i < wrappedEvents.size(); i++) {
				ConfiguredSoundEvent wrapped = wrappedEvents.get(i);
				Identifier location = getIdOf(i);
				SoundEvent event = SoundEvent.of(location);
				compiledEvents.add(new CompiledSoundEvent(event, wrapped.volume(), wrapped.pitch()));
			}
		}

		@Override
		public void register() {
			for (CompiledSoundEvent event : compiledEvents) {
				Registry.register(Registries.SOUND_EVENT, event.event.getId(), event.event);
			}
		}

		@Override
		public SoundEvent getMainEvent() {
			return compiledEvents.get(0)
				.event();
		}

		protected Identifier getIdOf(int i) {
			return new Identifier(id.getNamespace(), i == 0 ? id.getPath() : id.getPath() + "_compounded_" + i);
		}

		@Override
		public void write(JsonObject json) {
			for (int i = 0; i < wrappedEvents.size(); i++) {
				ConfiguredSoundEvent event = wrappedEvents.get(i);
				JsonObject entry = new JsonObject();
				JsonArray list = new JsonArray();
				JsonObject s = new JsonObject();
				s.addProperty("name", event.event()
					.get()
					.getId()
					.toString());
				s.addProperty("type", "event");
				if (attenuationDistance != 0)
					s.addProperty("attenuation_distance", attenuationDistance);
				list.add(s);
				entry.add("sounds", list);
				if (i == 0 && hasSubtitle())
					entry.addProperty("subtitle", getSubtitleKey());
				json.add(getIdOf(i).getPath(), entry);
			}
		}

		@Override
		public void play(World world, PlayerEntity entity, double x, double y, double z, float volume, float pitch) {
			for (CompiledSoundEvent event : compiledEvents) {
				world.playSound(entity, x, y, z, event.event(), category, event.volume() * volume,
					event.pitch() * pitch);
			}
		}

		@Override
		public void playAt(World world, double x, double y, double z, float volume, float pitch, boolean fade) {
			for (CompiledSoundEvent event : compiledEvents) {
				world.playSound(x, y, z, event.event(), category, event.volume() * volume,
					event.pitch() * pitch, fade);
			}
		}

		private record CompiledSoundEvent(SoundEvent event, float volume, float pitch) {
		}

	}

	private static class CustomSoundEntry extends SoundEntry {

		protected List<Identifier> variants;
		protected SoundEvent event;

		public CustomSoundEntry(Identifier id, List<Identifier> variants, String subtitle,
			SoundCategory category, int attenuationDistance) {
			super(id, subtitle, category, attenuationDistance);
			this.variants = variants;
		}

		@Override
		public void prepare() {
			 event = SoundEvent.of(id);
		}

		@Override
		public void register() {
			Registry.register(Registries.SOUND_EVENT, event.getId(), event);
		}

		@Override
		public SoundEvent getMainEvent() {
			return event;
		}

		@Override
		public void write(JsonObject json) {
			JsonObject entry = new JsonObject();
			JsonArray list = new JsonArray();

			JsonObject s = new JsonObject();
			s.addProperty("name", id.toString());
			s.addProperty("type", "file");
			if (attenuationDistance != 0)
				s.addProperty("attenuation_distance", attenuationDistance);
			list.add(s);

			for (Identifier variant : variants) {
				s = new JsonObject();
				s.addProperty("name", variant.toString());
				s.addProperty("type", "file");
				if (attenuationDistance != 0)
					s.addProperty("attenuation_distance", attenuationDistance);
				list.add(s);
			}

			entry.add("sounds", list);
			if (hasSubtitle())
				entry.addProperty("subtitle", getSubtitleKey());
			json.add(id.getPath(), entry);
		}

		@Override
		public void play(World world, PlayerEntity entity, double x, double y, double z, float volume, float pitch) {
			world.playSound(entity, x, y, z, event, category, volume, pitch);
		}

		@Override
		public void playAt(World world, double x, double y, double z, float volume, float pitch, boolean fade) {
			world.playSound(x, y, z, event, category, volume, pitch, fade);
		}

	}

}
