package com.simibubi.create.foundation.ponder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;
import com.google.gson.JsonObject;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.infrastructure.ponder.PonderIndex;
import com.tterrag.registrate.AbstractRegistrate;

public class PonderLocalization {
	static final Map<Identifier, String> SHARED = new HashMap<>();
	static final Map<Identifier, Couple<String>> TAG = new HashMap<>();
	static final Map<Identifier, String> CHAPTER = new HashMap<>();
	static final Map<Identifier, Map<String, String>> SPECIFIC = new HashMap<>();

	//

	public static void registerShared(Identifier key, String enUS) {
		SHARED.put(key, enUS);
	}

	public static void registerTag(Identifier key, String enUS, String description) {
		TAG.put(key, Couple.create(enUS, description));
	}

	public static void registerChapter(Identifier key, String enUS) {
		CHAPTER.put(key, enUS);
	}

	public static void registerSpecific(Identifier sceneId, String key, String enUS) {
		SPECIFIC.computeIfAbsent(sceneId, $ -> new HashMap<>())
			.put(key, enUS);
	}

	//

	public static final String LANG_PREFIX = "ponder.";

	protected static String langKeyForShared(Identifier k) {
		return k.getNamespace() + "." + LANG_PREFIX + "shared." + k.getPath();
	}

	protected static String langKeyForTag(Identifier k) {
		return k.getNamespace() + "." + LANG_PREFIX + "tag." + k.getPath();
	}

	protected static String langKeyForTagDescription(Identifier k) {
		return k.getNamespace() + "." + LANG_PREFIX + "tag." + k.getPath() + ".description";
	}

	protected static String langKeyForChapter(Identifier k) {
		return k.getNamespace() + "." + LANG_PREFIX + "chapter." + k.getPath();
	}

	protected static String langKeyForSpecific(Identifier sceneId, String k) {
		return sceneId.getNamespace() + "." + LANG_PREFIX + sceneId.getPath() + "." + k;
	}

	//

	public static String getShared(Identifier key) {
		if (PonderIndex.editingModeActive())
			return SHARED.containsKey(key) ? SHARED.get(key) : ("unregistered shared entry: " + key);
		return I18n.translate(langKeyForShared(key));
	}

	public static String getTag(Identifier key) {
		if (PonderIndex.editingModeActive())
			return TAG.containsKey(key) ? TAG.get(key)
				.getFirst() : ("unregistered tag entry: " + key);
		return I18n.translate(langKeyForTag(key));
	}

	public static String getTagDescription(Identifier key) {
		if (PonderIndex.editingModeActive())
			return TAG.containsKey(key) ? TAG.get(key)
				.getSecond() : ("unregistered tag entry: " + key);
		return I18n.translate(langKeyForTagDescription(key));
	}

	public static String getChapter(Identifier key) {
		if (PonderIndex.editingModeActive())
			return CHAPTER.containsKey(key) ? CHAPTER.get(key) : ("unregistered chapter entry: " + key);
		return I18n.translate(langKeyForChapter(key));
	}

	public static String getSpecific(Identifier sceneId, String k) {
		if (PonderIndex.editingModeActive())
			return SPECIFIC.get(sceneId)
				.get(k);
		return I18n.translate(langKeyForSpecific(sceneId, k));
	}

	//

	private static boolean sceneLangGenerated = false;

	public static void generateSceneLang() {
		if (sceneLangGenerated) {
			return;
		}

		sceneLangGenerated = true;
		PonderRegistry.ALL.forEach((id, list) -> {
			for (int i = 0; i < list.size(); i++)
				PonderRegistry.compileScene(i, list.get(i), null);
		});
	}

	public static void provideLang(String namespace, BiConsumer<String, String> consumer) {
		SHARED.forEach((k, v) -> {
			if (k.getNamespace().equals(namespace)) {
				consumer.accept(langKeyForShared(k), v);
			}
		});

		TAG.forEach((k, v) -> {
			if (k.getNamespace().equals(namespace)) {
				consumer.accept(langKeyForTag(k), v.getFirst());
				consumer.accept(langKeyForTagDescription(k), v.getSecond());
			}
		});

		CHAPTER.forEach((k, v) -> {
			if (k.getNamespace().equals(namespace)) {
				consumer.accept(langKeyForChapter(k), v);
			}
		});

		SPECIFIC.entrySet()
			.stream()
			.filter(entry -> entry.getKey().getNamespace().equals(namespace))
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry -> {
				entry.getValue()
					.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey())
					.forEach(subEntry -> consumer.accept(
						langKeyForSpecific(entry.getKey(), subEntry.getKey()), subEntry.getValue()));
			});
	}

	@Deprecated(forRemoval = true)
	public static void record(String namespace, JsonObject object) {
		provideLang(namespace, object::addProperty);
	}

	public static void provideRegistrateLang(AbstractRegistrate<?> registrate) {
		generateSceneLang();
		provideLang(registrate.getModid(), registrate::addRawLang);
	}
}
