package com.simibubi.create.content.logistics.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.logistics.filter.attribute.BookAuthorAttribute;
import com.simibubi.create.content.logistics.filter.attribute.BookCopyAttribute;
import com.simibubi.create.content.logistics.filter.attribute.ColorAttribute;
import com.simibubi.create.content.logistics.filter.attribute.EnchantAttribute;
import com.simibubi.create.content.logistics.filter.attribute.FluidContentsAttribute;
import com.simibubi.create.content.logistics.filter.attribute.ItemNameAttribute;
import com.simibubi.create.content.logistics.filter.attribute.ShulkerFillLevelAttribute;
import com.simibubi.create.content.logistics.filter.attribute.astralsorcery.AstralSorceryAmuletAttribute;
import com.simibubi.create.content.logistics.filter.attribute.astralsorcery.AstralSorceryAttunementAttribute;
import com.simibubi.create.content.logistics.filter.attribute.astralsorcery.AstralSorceryCrystalAttribute;
import com.simibubi.create.content.logistics.filter.attribute.astralsorcery.AstralSorceryPerkGemAttribute;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandlerContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public interface ItemAttribute {

	static List<ItemAttribute> types = new ArrayList<>();

	static ItemAttribute standard = register(StandardTraits.DUMMY);
	static ItemAttribute inTag = register(new InTag(ItemTags.LOGS));
	static ItemAttribute addedBy = register(new AddedBy("dummy"));
	static ItemAttribute hasEnchant = register(EnchantAttribute.EMPTY);
	static ItemAttribute shulkerFillLevel = register(ShulkerFillLevelAttribute.EMPTY);
	static ItemAttribute hasColor = register(ColorAttribute.EMPTY);
	static ItemAttribute hasFluid = register(FluidContentsAttribute.EMPTY);
	static ItemAttribute hasName = register(new ItemNameAttribute("dummy"));
	static ItemAttribute bookAuthor = register(new BookAuthorAttribute("dummy"));
	static ItemAttribute bookCopy = register(new BookCopyAttribute(-1));
	static ItemAttribute astralAmulet = register(new AstralSorceryAmuletAttribute("dummy", -1));
	static ItemAttribute astralAttunement = register(new AstralSorceryAttunementAttribute("dummy"));
	static ItemAttribute astralCrystal = register(new AstralSorceryCrystalAttribute("dummy"));
	static ItemAttribute astralPerkGem = register(new AstralSorceryPerkGemAttribute("dummy"));

	static ItemAttribute register(ItemAttribute attributeType) {
		types.add(attributeType);
		return attributeType;
	}

	@Nullable
	static ItemAttribute fromNBT(NbtCompound nbt) {
		for (ItemAttribute itemAttribute : types)
			if (itemAttribute.canRead(nbt))
				return itemAttribute.readNBT(nbt.getCompound(itemAttribute.getNBTKey()));
		return null;
	}

	default boolean appliesTo(ItemStack stack, World world) {
		return appliesTo(stack);
	}

	boolean appliesTo(ItemStack stack);

	default List<ItemAttribute> listAttributesOf(ItemStack stack, World world) {
		return listAttributesOf(stack);
	}

	List<ItemAttribute> listAttributesOf(ItemStack stack);

	String getTranslationKey();

	void writeNBT(NbtCompound nbt);

	ItemAttribute readNBT(NbtCompound nbt);

	default void serializeNBT(NbtCompound nbt) {
		NbtCompound compound = new NbtCompound();
		writeNBT(compound);
		nbt.put(getNBTKey(), compound);
	}

	default Object[] getTranslationParameters() {
		return new String[0];
	}

	default boolean canRead(NbtCompound nbt) {
		return nbt.contains(getNBTKey());
	}

	default String getNBTKey() {
		return getTranslationKey();
	}

	@Environment(value = EnvType.CLIENT)
	default MutableText format(boolean inverted) {
		return Lang.translateDirect("item_attributes." + getTranslationKey() + (inverted ? ".inverted" : ""),
				getTranslationParameters());
	}

	public static enum StandardTraits implements ItemAttribute {

		DUMMY(s -> false),
		PLACEABLE(s -> s.getItem() instanceof BlockItem),
		CONSUMABLE(ItemStack::isFood),
		FLUID_CONTAINER(s -> ContainerItemContext.withConstant(s).find(FluidStorage.ITEM) != null),
		ENCHANTED(ItemStack::hasEnchantments),
		MAX_ENCHANTED(StandardTraits::maxEnchanted),
		RENAMED(ItemStack::hasCustomName),
		DAMAGED(ItemStack::isDamaged),
		BADLY_DAMAGED(s -> s.isDamaged() && (float) s.getDamage() / s.getMaxDamage() > 3 / 4f),
		NOT_STACKABLE(((Predicate<ItemStack>) ItemStack::isStackable).negate()),
		EQUIPABLE(s -> LivingEntity.getPreferredEquipmentSlot(s)
				.getType() != EquipmentSlot.Type.HAND),
		FURNACE_FUEL(AbstractFurnaceBlockEntity::canUseAsFuel),
		WASHABLE(AllFanProcessingTypes.SPLASHING::canProcess),
		HAUNTABLE(AllFanProcessingTypes.HAUNTING::canProcess),
		CRUSHABLE((s, w) -> testRecipe(s, w, AllRecipeTypes.CRUSHING.getType())
				|| testRecipe(s, w, AllRecipeTypes.MILLING.getType())),
		SMELTABLE((s, w) -> testRecipe(s, w, RecipeType.SMELTING)),
		SMOKABLE((s, w) -> testRecipe(s, w, RecipeType.SMOKING)),
		BLASTABLE((s, w) -> testRecipe(s, w, RecipeType.BLASTING)),
		COMPOSTABLE(s -> ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(s.getItem()));

		private static final Inventory RECIPE_WRAPPER = new ItemStackHandlerContainer(1);
		private Predicate<ItemStack> test;
		private BiPredicate<ItemStack, World> testWithWorld;

		private StandardTraits(Predicate<ItemStack> test) {
			this.test = test;
		}

		private static boolean testRecipe(ItemStack s, World w, RecipeType<? extends Recipe<Inventory>> type) {
			RECIPE_WRAPPER.setStack(0, s.copy());
			return w.getRecipeManager()
					.getFirstMatch(type, RECIPE_WRAPPER, w)
					.isPresent();
		}

		private static boolean maxEnchanted(ItemStack s) {
			return EnchantmentHelper.get(s)
					.entrySet()
					.stream()
					.anyMatch(e -> e.getKey()
							.getMaxLevel() <= e.getValue());
		}

		private StandardTraits(BiPredicate<ItemStack, World> test) {
			this.testWithWorld = test;
		}

		@Override
		public boolean appliesTo(ItemStack stack, World world) {
			if (testWithWorld != null)
				return testWithWorld.test(stack, world);
			return appliesTo(stack);
		}

		@Override
		public boolean appliesTo(ItemStack stack) {
			return test.test(stack);
		}

		@Override
		public List<ItemAttribute> listAttributesOf(ItemStack stack, World world) {
			List<ItemAttribute> attributes = new ArrayList<>();
			for (StandardTraits trait : values())
				if (trait.appliesTo(stack, world))
					attributes.add(trait);
			return attributes;
		}

		@Override
		public List<ItemAttribute> listAttributesOf(ItemStack stack) {
			return null;
		}

		@Override
		public String getTranslationKey() {
			return Lang.asId(name());
		}

		@Override
		public String getNBTKey() {
			return "standard_trait";
		}

		@Override
		public void writeNBT(NbtCompound nbt) {
			nbt.putBoolean(name(), true);
		}

		@Override
		public ItemAttribute readNBT(NbtCompound nbt) {
			for (StandardTraits trait : values())
				if (nbt.contains(trait.name()))
					return trait;
			return null;
		}

	}

	public static class InTag implements ItemAttribute {

		public TagKey<Item> tag;

		public InTag(TagKey<Item> tag) {
			this.tag = tag;
		}

		@Override
		public boolean appliesTo(ItemStack stack) {
			return stack.isIn(tag);
		}

		@Override
		public List<ItemAttribute> listAttributesOf(ItemStack stack) {
			return stack.streamTags()
					.map(InTag::new)
					.collect(Collectors.toList());
		}

		@Override
		public String getTranslationKey() {
			return "in_tag";
		}

		@Override
		public Object[] getTranslationParameters() {
			return new Object[]{"#" + tag.id()};
		}

		@Override
		public void writeNBT(NbtCompound nbt) {
			nbt.putString("space", tag.id().getNamespace());
			nbt.putString("path", tag.id().getPath());
		}

		@Override
		public ItemAttribute readNBT(NbtCompound nbt) {
			return new InTag(TagKey.of(RegistryKeys.ITEM, new Identifier(nbt.getString("space"), nbt.getString("path"))));
		}

	}

	public static class AddedBy implements ItemAttribute {

		private String modId;

		public AddedBy(String modId) {
			this.modId = modId;
		}

		@Override
		public boolean appliesTo(ItemStack stack) {
			return modId.equals(Registries.ITEM.getId(stack.getItem()).getNamespace());
		}

		@Override
		public List<ItemAttribute> listAttributesOf(ItemStack stack) {
			String id = Registries.ITEM.getId(stack.getItem()).getNamespace();
			return id == null ? Collections.emptyList() : Arrays.asList(new AddedBy(id));
		}

		@Override
		public String getTranslationKey() {
			return "added_by";
		}

		@Override
		public Object[] getTranslationParameters() {
			ModContainer container = FabricLoader.getInstance().getModContainer(modId).orElse(null);
			String name = container == null ? name = StringUtils.capitalize(modId) : container.getMetadata().getName();
			return new Object[]{name};
		}

		@Override
		public void writeNBT(NbtCompound nbt) {
			nbt.putString("id", modId);
		}

		@Override
		public ItemAttribute readNBT(NbtCompound nbt) {
			return new AddedBy(nbt.getString("id"));
		}

	}

}
