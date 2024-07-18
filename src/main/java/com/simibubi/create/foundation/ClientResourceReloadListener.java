package com.simibubi.create.foundation;

import java.util.Collection;
import java.util.Set;

import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.utility.LangNumberFormat;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

public class ClientResourceReloadListener implements SynchronousResourceReloader, IdentifiableResourceReloadListener {
	public static final Identifier ID = Create.asResource("client_reload_listener");
	// fabric: make sure number format is updated after languages load
	public static final Set<Identifier> DEPENDENCIES = Set.of(ResourceReloadListenerKeys.LANGUAGES);

	@Override
	public void reload(ResourceManager resourceManager) {
		CreateClient.invalidateRenderers();
		SoundScapes.invalidateAll();
		LangNumberFormat.numberFormat.update();
		BeltHelper.uprightCache.clear();
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	@Override
	public Collection<Identifier> getFabricDependencies() {
		return DEPENDENCIES;
	}
}
