package com.simibubi.create.foundation.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.Create;

public class AttachedRegistry<K, V> {
	private static final List<AttachedRegistry<?, ?>> ALL = new ArrayList<>();

	protected final Registry<K> objectRegistry;
	protected final Map<Identifier, V> idMap = new HashMap<>();
	protected final Map<K, V> objectMap = new IdentityHashMap<>();
	protected final Map<Identifier, Function<K, V>> deferredRegistrations = new HashMap<>();
	protected boolean unwrapped = false;

	public AttachedRegistry(Registry<K> objectRegistry) {
		this.objectRegistry = objectRegistry;
		ALL.add(this);
	}

	public void register(Identifier id, V value) {
		if (!unwrapped) {
			idMap.put(id, value);
		} else {
			K object = objectRegistry.get(id);
			if (object != null) {
				objectMap.put(object, value);
			} else {
				Create.LOGGER.warn("Could not get object for id '" + id + "' in AttachedRegistry after unwrapping!");
			}
		}
	}

	public void register(K object, V value) {
		if (unwrapped) {
			objectMap.put(object, value);
		} else {
			Identifier id = objectRegistry.getId(object);
			if (id != null) {
				idMap.put(id, value);
			} else {
				Create.LOGGER.warn("Could not get id of object '" + object + "' in AttachedRegistry before unwrapping!");
			}
		}
	}

	public void registerDeferred(Identifier id, Function<K, V> func) {
		if (!unwrapped) {
			deferredRegistrations.put(id, func);
		} else {
			K object = objectRegistry.get(id);
			if (object != null) {
				objectMap.put(object, func.apply(object));
			} else {
				Create.LOGGER.warn("Could not get object for id '" + id + "' in AttachedRegistry after unwrapping!");
			}
		}
	}

	public void registerDeferred(K object, Function<K, V> func) {
		if (unwrapped) {
			objectMap.put(object, func.apply(object));
		} else {
			Identifier id = objectRegistry.getId(object);
			if (id != null) {
				deferredRegistrations.put(id, func);
			} else {
				Create.LOGGER.warn("Could not get id of object '" + object + "' in AttachedRegistry before unwrapping!");
			}
		}
	}

	@Nullable
	public V get(Identifier id) {
		if (!unwrapped) {
			return idMap.get(id);
		} else {
			K object = objectRegistry.get(id);
			if (object != null) {
				return objectMap.get(object);
			} else {
				Create.LOGGER.warn("Could not get object for id '" + id + "' in AttachedRegistry after unwrapping!");
				return null;
			}
		}
	}

	@Nullable
	public V get(K object) {
		if (unwrapped) {
			return objectMap.get(object);
		} else {
			Identifier id = objectRegistry.getId(object);
			if (id != null) {
				return idMap.get(id);
			} else {
				Create.LOGGER.warn("Could not get id of object '" + object + "' in AttachedRegistry before unwrapping!");
				return null;
			}
		}
	}

	public boolean isUnwrapped() {
		return unwrapped;
	}

	protected void unwrap() {
		deferredRegistrations.forEach((id, func) -> {
			K object = objectRegistry.get(id);
			if (object != null) {
				objectMap.put(object, func.apply(object));
			} else {
				Create.LOGGER.warn("Could not get object for id '" + id + "' in AttachedRegistry during unwrapping!");
			}
		});

		idMap.forEach((id, value) -> {
			K object = objectRegistry.get(id);
			if (object != null) {
				objectMap.put(object, value);
			} else {
				Create.LOGGER.warn("Could not get object for id '" + id + "' in AttachedRegistry during unwrapping!");
			}
		});

		deferredRegistrations.clear();
		idMap.clear();
		unwrapped = true;
	}

	public static void unwrapAll() {
		for (AttachedRegistry<?, ?> registry : ALL) {
			registry.unwrap();
		}
	}
}