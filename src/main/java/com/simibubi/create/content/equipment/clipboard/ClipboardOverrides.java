package com.simibubi.create.content.equipment.clipboard;

import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile.UncheckedModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.item.ItemModelBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class ClipboardOverrides {

	public enum ClipboardType {
		EMPTY("empty_clipboard"), WRITTEN("clipboard"), EDITING("clipboard_and_quill");

		public String file;
		public static Identifier ID = Create.asResource("clipboard_type");

		private ClipboardType(String file) {
			this.file = file;
		}
	}

	public static void switchTo(ClipboardType type, ItemStack clipboardItem) {
		NbtCompound tag = clipboardItem.getOrCreateNbt();
		tag.putInt("Type", type.ordinal());
	}

	@Environment(EnvType.CLIENT)
	public static void registerModelOverridesClient(ClipboardBlockItem item) {
		ModelPredicateProviderRegistry.register(item, ClipboardType.ID, (pStack, pLevel, pEntity, pSeed) -> {
			NbtCompound tag = pStack.getNbt();
			return tag == null ? 0 : tag.getInt("Type");
		});
	}

	public static ItemModelBuilder addOverrideModels(DataGenContext<Item, ClipboardBlockItem> c,
		RegistrateItemModelProvider p) {
		ItemModelBuilder builder = p.generated(() -> c.get());
		for (int i = 0; i < ClipboardType.values().length; i++) {
			builder.override()
				.predicate(ClipboardType.ID, i)
				.model(p.getBuilder(c.getName() + "_" + i)
					.parent(new UncheckedModelFile("item/generated"))
					.texture("layer0", Create.asResource("item/" + ClipboardType.values()[i].file)))
				.end();
		}
		return builder;
	}

}
