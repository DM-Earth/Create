package com.simibubi.create.foundation.ponder;

import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.util.Identifier;
import com.simibubi.create.foundation.ponder.PonderStoryBoardEntry.PonderStoryBoard;
import com.tterrag.registrate.util.entry.ItemProviderEntry;

public class PonderRegistrationHelper {

	protected String namespace;

	public PonderRegistrationHelper(String namespace) {
		this.namespace = namespace;
	}

	public PonderStoryBoardEntry addStoryBoard(Identifier component,
		Identifier schematicLocation, PonderStoryBoard storyBoard, PonderTag... tags) {
		PonderStoryBoardEntry entry = this.createStoryBoardEntry(storyBoard, schematicLocation, component);
		entry.highlightTags(tags);
		PonderRegistry.addStoryBoard(entry);
		return entry;
	}

	public PonderStoryBoardEntry addStoryBoard(Identifier component,
		String schematicPath, PonderStoryBoard storyBoard, PonderTag... tags) {
		return addStoryBoard(component, asLocation(schematicPath), storyBoard, tags);
	}

	public PonderStoryBoardEntry addStoryBoard(ItemProviderEntry<?> component,
		Identifier schematicLocation, PonderStoryBoard storyBoard, PonderTag... tags) {
		return addStoryBoard(component.getId(), schematicLocation, storyBoard, tags);
	}

	public PonderStoryBoardEntry addStoryBoard(ItemProviderEntry<?> component,
		String schematicPath, PonderStoryBoard storyBoard, PonderTag... tags) {
		return addStoryBoard(component, asLocation(schematicPath), storyBoard, tags);
	}

	public MultiSceneBuilder forComponents(ItemProviderEntry<?>... components) {
		return new MultiSceneBuilder(Arrays.asList(components));
	}

	public MultiSceneBuilder forComponents(Iterable<? extends ItemProviderEntry<?>> components) {
		return new MultiSceneBuilder(components);
	}

	public PonderStoryBoardEntry createStoryBoardEntry(PonderStoryBoard storyBoard, Identifier schematicLocation, Identifier component) {
		return new PonderStoryBoardEntry(storyBoard, namespace, schematicLocation, component);
	}

	public PonderStoryBoardEntry createStoryBoardEntry(PonderStoryBoard storyBoard, String schematicPath, Identifier component) {
		return createStoryBoardEntry(storyBoard, asLocation(schematicPath), component);
	}

	public PonderTag createTag(String name) {
		return new PonderTag(asLocation(name));
	}

	public PonderChapter getOrCreateChapter(String name) {
		return PonderChapter.of(asLocation(name));
	}

	public Identifier asLocation(String path) {
		return new Identifier(namespace, path);
	}

	public class MultiSceneBuilder {

		protected Iterable<? extends ItemProviderEntry<?>> components;

		protected MultiSceneBuilder(Iterable<? extends ItemProviderEntry<?>> components) {
			this.components = components;
		}

		public MultiSceneBuilder addStoryBoard(Identifier schematicLocation, PonderStoryBoard storyBoard) {
			return addStoryBoard(schematicLocation, storyBoard, $ -> {
			});
		}

		public MultiSceneBuilder addStoryBoard(Identifier schematicLocation, PonderStoryBoard storyBoard, PonderTag... tags) {
			return addStoryBoard(schematicLocation, storyBoard, sb -> sb.highlightTags(tags));
		}

		public MultiSceneBuilder addStoryBoard(Identifier schematicLocation, PonderStoryBoard storyBoard,
			Consumer<PonderStoryBoardEntry> extras) {
			components.forEach(c -> extras.accept(PonderRegistrationHelper.this.addStoryBoard(c, schematicLocation, storyBoard)));
			return this;
		}

		public MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard) {
			return addStoryBoard(asLocation(schematicPath), storyBoard);
		}

		public MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard, PonderTag... tags) {
			return addStoryBoard(asLocation(schematicPath), storyBoard, tags);
		}

		public MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard,
			Consumer<PonderStoryBoardEntry> extras) {
			return addStoryBoard(asLocation(schematicPath), storyBoard, extras);
		}

	}

}
