package com.simibubi.create.foundation.ponder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.Identifier;

public class PonderStoryBoardEntry {

	private final PonderStoryBoard board;
	private final String namespace;
	private final Identifier schematicLocation;
	private final Identifier component;
	private final List<PonderTag> tags;

	public PonderStoryBoardEntry(PonderStoryBoard board, String namespace, Identifier schematicLocation, Identifier component) {
		this.board = board;
		this.namespace = namespace;
		this.schematicLocation = schematicLocation;
		this.component = component;
		this.tags = new ArrayList<>();
	}

	public PonderStoryBoardEntry(PonderStoryBoard board, String namespace, String schematicPath, Identifier component) {
		this(board, namespace, new Identifier(namespace, schematicPath), component);
	}

	public PonderStoryBoard getBoard() {
		return board;
	}

	public String getNamespace() {
		return namespace;
	}

	public Identifier getSchematicLocation() {
		return schematicLocation;
	}

	public Identifier getComponent() {
		return component;
	}

	public List<PonderTag> getTags() {
		return tags;
	}

	// Builder start

	public PonderStoryBoardEntry highlightTag(PonderTag tag) {
		tags.add(tag);
		return this;
	}

	public PonderStoryBoardEntry highlightTags(PonderTag... tags) {
		Collections.addAll(this.tags, tags);
		return this;
	}

	public PonderStoryBoardEntry highlightAllTags() {
		tags.add(PonderTag.HIGHLIGHT_ALL);
		return this;
	}

	public PonderStoryBoardEntry chapter(PonderChapter chapter) {
		PonderRegistry.CHAPTERS.addStoriesToChapter(chapter, this);
		return this;
	}

	public PonderStoryBoardEntry chapters(PonderChapter... chapters) {
		for (PonderChapter c : chapters)
			chapter(c);
		return this;
	}

	// Builder end

	@FunctionalInterface
	public interface PonderStoryBoard {
		void program(SceneBuilder scene, SceneBuildingUtil util);
	}

}
