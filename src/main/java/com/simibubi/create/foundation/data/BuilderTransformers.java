package com.simibubi.create.foundation.data;

import static com.simibubi.create.AllInteractionBehaviours.interactionBehaviour;
import static com.simibubi.create.AllMovementBehaviours.movementBehaviour;
import static com.simibubi.create.foundation.data.BlockStateGen.axisBlock;
import static com.simibubi.create.foundation.data.CreateRegistrate.casingConnectivity;
import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.PistonType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemConvertible;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTable.Builder;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.SurvivesExplosionLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.CopyNameLootFunction;
import net.minecraft.loot.function.CopyNbtLootFunction;
import net.minecraft.loot.provider.nbt.ContextLootNbtProvider;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.behaviour.DoorMovingInteraction;
import com.simibubi.create.content.contraptions.behaviour.TrapdoorMovingInteraction;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonGenerator;
import com.simibubi.create.content.decoration.MetalScaffoldingBlock;
import com.simibubi.create.content.decoration.MetalScaffoldingBlockItem;
import com.simibubi.create.content.decoration.MetalScaffoldingCTBehaviour;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;
import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogCTBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock.Shape;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelItem;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.foundation.block.ItemUseOverrides;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import io.github.fabricators_of_create.porting_lib.models.generators.ConfiguredModel;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;

public class BuilderTransformers {

	public static <B extends EncasedShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedShaft(String casing,
		Supplier<CTSpriteShiftEntry> casingShift) {
		return builder -> encasedBase(builder, () -> AllBlocks.SHAFT.get())
			.onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(casingShift.get())))
			.onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, casingShift.get(),
				(s, f) -> f.getAxis() != s.get(EncasedShaftBlock.AXIS))))
			.blockstate((c, p) -> axisBlock(c, p, blockState -> p.models()
				.getExistingFile(p.modLoc("block/encased_shaft/block_" + casing)), true))
			.item()
			.model(AssetLookup.customBlockItemModel("encased_shaft", "item_" + casing))
			.build();
	}

	@SuppressWarnings("deprecation")
	public static <B extends StandardBogeyBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> bogey() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.sounds(BlockSoundGroup.NETHERITE))
			.properties(p -> p.nonOpaque())
			.transform(pickaxeOnly())
			.blockstate((c, p) -> BlockStateGen.horizontalAxisBlock(c, p, s -> p.models()
				.getExistingFile(p.modLoc("block/track/bogey/top"))))
			.loot((p, l) -> p.addDrop(l, AllBlocks.RAILWAY_CASING.get()))
			.onRegister(block -> AbstractBogeyBlock.registerStandardBogey(RegisteredObjects.getKeyOrThrow(block)));
	}

	public static <B extends CopycatBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> copycat() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.blockstate((c, p) -> p.simpleBlock(c.get(), p.models()
				.getExistingFile(p.mcLoc("air"))))
			.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.nonOpaque()
				.mapColor(MapColor.CLEAR))
			// fabric: only render base model on cutout. When rendering the wrapped model's material is copied.
			.addLayer(() -> RenderLayer::getCutout)
			.color(() -> CopycatBlock::wrappedColor)
			.transform(TagGen.axeOrPickaxe());
	}

	public static <B extends TrapdoorBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> trapdoor(boolean orientable) {
		return b -> b.blockstate((c, p) -> {
			ModelFile bottom = AssetLookup.partialBaseModel(c, p, "bottom");
			ModelFile top = AssetLookup.partialBaseModel(c, p, "top");
			ModelFile open = AssetLookup.partialBaseModel(c, p, "open");
			if (orientable)
				p.trapdoorBlock(c.get(), bottom, top, open, orientable);
			else
				BlockStateGen.uvLockedTrapdoorBlock(c.get(), bottom, top, open)
					.accept(c, p);
		})
			.transform(pickaxeOnly())
			.tag(BlockTags.TRAPDOORS)
			.onRegister(interactionBehaviour(new TrapdoorMovingInteraction()))
			.item()
			.tag(ItemTags.TRAPDOORS)
			.build();
	}

	public static <B extends SlidingDoorBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> slidingDoor(String type) {
		return b -> b.initialProperties(() -> Blocks.IRON_DOOR)
			.properties(p -> p.requiresTool()
				.strength(3.0F, 6.0F))
			.blockstate((c, p) -> {
				ModelFile bottom = AssetLookup.partialBaseModel(c, p, "bottom");
				ModelFile top = AssetLookup.partialBaseModel(c, p, "top");
				p.doorBlock(c.get(), bottom, bottom, bottom, bottom, top, top, top, top);
			})
			.addLayer(() -> RenderLayer::getCutoutMipped)
			.transform(pickaxeOnly())
			.onRegister(interactionBehaviour(new DoorMovingInteraction()))
			.onRegister(movementBehaviour(new SlidingDoorMovementBehaviour()))
			.tag(BlockTags.DOORS)
			.tag(BlockTags.WOODEN_DOORS) // for villager AI
			.tag(AllBlockTags.NON_DOUBLE_DOOR.tag)
			.loot((lr, block) -> lr.addDrop(block, lr.doorDrops(block)))
			.item()
			.tag(ItemTags.DOORS)
			.tag(AllItemTags.CONTRAPTION_CONTROLLED.tag)
			.model((c, p) -> p.blockSprite(c, p.modLoc("item/" + type + "_door")))
			.build();
	}

	public static <B extends EncasedCogwheelBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedCogwheel(
		String casing, Supplier<CTSpriteShiftEntry> casingShift) {
		return b -> encasedCogwheelBase(b, casing, casingShift, () -> AllBlocks.COGWHEEL.get(), false);
	}

	public static <B extends EncasedCogwheelBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedLargeCogwheel(
		String casing, Supplier<CTSpriteShiftEntry> casingShift) {
		return b -> encasedCogwheelBase(b, casing, casingShift, () -> AllBlocks.LARGE_COGWHEEL.get(), true)
			.onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCogCTBehaviour(casingShift.get())));
	}

	private static <B extends EncasedCogwheelBlock, P> BlockBuilder<B, P> encasedCogwheelBase(BlockBuilder<B, P> b,
		String casing, Supplier<CTSpriteShiftEntry> casingShift, Supplier<ItemConvertible> drop, boolean large) {
		String encasedSuffix = "_encased_cogwheel_side" + (large ? "_connected" : "");
		String blockFolder = large ? "encased_large_cogwheel" : "encased_cogwheel";
		String wood = casing.equals("brass") ? "dark_oak" : "spruce";
		String gearbox = casing.equals("brass") ? "brass_gearbox" : "gearbox";
		return encasedBase(b, drop).addLayer(() -> RenderLayer::getCutoutMipped)
			.onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, casingShift.get(),
				(s, f) -> f.getAxis() == s.get(EncasedCogwheelBlock.AXIS)
					&& !s.get(f.getDirection() == AxisDirection.POSITIVE ? EncasedCogwheelBlock.TOP_SHAFT
						: EncasedCogwheelBlock.BOTTOM_SHAFT))))
			.blockstate((c, p) -> axisBlock(c, p, blockState -> {
				String suffix = (blockState.get(EncasedCogwheelBlock.TOP_SHAFT) ? "_top" : "")
					+ (blockState.get(EncasedCogwheelBlock.BOTTOM_SHAFT) ? "_bottom" : "");
				String modelName = c.getName() + suffix;
				return p.models()
					.withExistingParent(modelName, p.modLoc("block/" + blockFolder + "/block" + suffix))
					.texture("casing", Create.asResource("block/" + casing + "_casing"))
					.texture("particle", Create.asResource("block/" + casing + "_casing"))
					.texture("4", Create.asResource("block/" + gearbox))
					.texture("1", new Identifier("block/stripped_" + wood + "_log_top"))
					.texture("side", Create.asResource("block/" + casing + encasedSuffix));
			}, false))
			.item()
			.model((c, p) -> p.withExistingParent(c.getName(), p.modLoc("block/" + blockFolder + "/item"))
				.texture("casing", Create.asResource("block/" + casing + "_casing"))
				.texture("particle", Create.asResource("block/" + casing + "_casing"))
				.texture("1", new Identifier("block/stripped_" + wood + "_log_top"))
				.texture("side", Create.asResource("block/" + casing + encasedSuffix)))
			.build();
	}

	private static <B extends RotatedPillarKineticBlock, P> BlockBuilder<B, P> encasedBase(BlockBuilder<B, P> b,
		Supplier<ItemConvertible> drop) {
		return b.initialProperties(SharedProperties::stone)
			.properties(AbstractBlock.Settings::nonOpaque)
			.transform(BlockStressDefaults.setNoImpact())
			.loot((p, lb) -> p.addDrop(lb, drop.get()));
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> cuckooClock() {
		return b -> b.initialProperties(SharedProperties::wooden)
			.blockstate((c, p) -> p.horizontalBlock(c.get(), p.models()
				.getExistingFile(p.modLoc("block/cuckoo_clock/block"))))
			.addLayer(() -> RenderLayer::getCutoutMipped)
			.transform(BlockStressDefaults.setImpact(1.0))
			.item()
			.transform(ModelGen.customItemModel("cuckoo_clock", "item"));
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> ladder(String name,
		Supplier<DataIngredient> ingredient, MapColor color) {
		return b -> b.initialProperties(() -> Blocks.LADDER)
			.properties(p -> p.mapColor(color))
			.addLayer(() -> RenderLayer::getCutout)
			.blockstate((c, p) -> p.horizontalBlock(c.get(), p.models()
				.withExistingParent(c.getName(), p.modLoc("block/ladder"))
				.texture("0", p.modLoc("block/ladder_" + name + "_hoop"))
				.texture("1", p.modLoc("block/ladder_" + name))
				.texture("particle", p.modLoc("block/ladder_" + name))))
			.properties(p -> p.sounds(BlockSoundGroup.COPPER))
			.transform(pickaxeOnly())
			.tag(BlockTags.CLIMBABLE)
			.item()
			.recipe((c, p) -> p.stonecutting(ingredient.get(), RecipeCategory.DECORATIONS, c::get, 2))
			.model((c, p) -> p.blockSprite(c::get, p.modLoc("block/ladder_" + name)))
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> scaffold(String name,
		Supplier<DataIngredient> ingredient, MapColor color, CTSpriteShiftEntry scaffoldShift,
		CTSpriteShiftEntry scaffoldInsideShift, CTSpriteShiftEntry casingShift) {
		return b -> b.initialProperties(() -> Blocks.SCAFFOLDING)
			.properties(p -> p.sounds(BlockSoundGroup.COPPER)
				.mapColor(color))
			.addLayer(() -> RenderLayer::getCutout)
			.blockstate((c, p) -> p.getVariantBuilder(c.get())
				.forAllStatesExcept(s -> {
					String suffix = s.get(MetalScaffoldingBlock.BOTTOM) ? "_horizontal" : "";
					return ConfiguredModel.builder()
						.modelFile(p.models()
							.withExistingParent(c.getName() + suffix, p.modLoc("block/scaffold/block" + suffix))
							.texture("top", p.modLoc("block/funnel/" + name + "_funnel_frame"))
							.texture("inside", p.modLoc("block/scaffold/" + name + "_scaffold_inside"))
							.texture("side", p.modLoc("block/scaffold/" + name + "_scaffold"))
							.texture("casing", p.modLoc("block/" + name + "_casing"))
							.texture("particle", p.modLoc("block/scaffold/" + name + "_scaffold")))
						.build();
				}, MetalScaffoldingBlock.WATERLOGGED, MetalScaffoldingBlock.DISTANCE))
			.onRegister(connectedTextures(
				() -> new MetalScaffoldingCTBehaviour(scaffoldShift, scaffoldInsideShift, casingShift)))
			.transform(pickaxeOnly())
			.tag(BlockTags.CLIMBABLE)
			.item(MetalScaffoldingBlockItem::new)
			.recipe((c, p) -> p.stonecutting(ingredient.get(), RecipeCategory.DECORATIONS, c::get, 2))
			.model((c, p) -> p.withExistingParent(c.getName(), p.modLoc("block/" + c.getName())))
			.build();
	}

	public static <B extends ValveHandleBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> valveHandle(
		@Nullable DyeColor color) {
		return b -> b.initialProperties(SharedProperties::copperMetal)
			.blockstate((c, p) -> {
				String variant = color == null ? "copper" : color.asString();
				p.directionalBlock(c.get(), p.models()
					.withExistingParent(variant + "_valve_handle", p.modLoc("block/valve_handle"))
					.texture("3", p.modLoc("block/valve_handle/valve_handle_" + variant)));
			})
			.tag(AllBlockTags.BRITTLE.tag, AllBlockTags.VALVE_HANDLES.tag)
			.transform(BlockStressDefaults.setGeneratorSpeed(ValveHandleBlock::getSpeedRange))
			.onRegister(ItemUseOverrides::addBlock)
			.item()
			.tag(AllItemTags.VALVE_HANDLES.tag)
			.build();
	}

	public static <B extends CasingBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> casing(
		Supplier<CTSpriteShiftEntry> ct) {
		return b -> b.initialProperties(SharedProperties::stone)
			.properties(p -> p.sounds(BlockSoundGroup.WOOD))
			.transform(axeOrPickaxe())
			.blockstate((c, p) -> p.simpleBlock(c.get()))
			.onRegister(connectedTextures(() -> new EncasedCTBehaviour(ct.get())))
			.onRegister(casingConnectivity((block, cc) -> cc.makeCasing(block, ct.get())))
			.tag(AllBlockTags.CASING.tag)
			.item()
			.tag(AllItemTags.CASING.tag)
			.build();
	}

	public static <B extends CasingBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> layeredCasing(
		Supplier<CTSpriteShiftEntry> ct, Supplier<CTSpriteShiftEntry> ct2) {
		return b -> b.initialProperties(SharedProperties::stone)
			.transform(axeOrPickaxe())
			.blockstate((c, p) -> p.simpleBlock(c.get(), p.models()
				.cubeColumn(c.getName(), ct.get()
					.getOriginalResourceLocation(),
					ct2.get()
						.getOriginalResourceLocation())))
			.onRegister(connectedTextures(() -> new HorizontalCTBehaviour(ct.get(), ct2.get())))
			.onRegister(casingConnectivity((block, cc) -> cc.makeCasing(block, ct.get())))
			.tag(AllBlockTags.CASING.tag)
			.item()
			.tag(AllItemTags.CASING.tag)
			.build();
	}

	public static <B extends BeltTunnelBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> beltTunnel(
		String type, Identifier particleTexture) {
		String prefix = "block/tunnel/" + type + "_tunnel";
		String funnel_prefix = "block/funnel/" + type + "_funnel";
		return b -> b.initialProperties(SharedProperties::stone)
			.addLayer(() -> RenderLayer::getCutoutMipped)
			.properties(AbstractBlock.Settings::nonOpaque)
			.transform(pickaxeOnly())
			.blockstate((c, p) -> p.getVariantBuilder(c.get())
				.forAllStates(state -> {
					Shape shape = state.get(BeltTunnelBlock.SHAPE);
					String window = shape == Shape.WINDOW ? "_window" : "";
					if (shape == BeltTunnelBlock.Shape.CLOSED)
						shape = BeltTunnelBlock.Shape.STRAIGHT;
					String shapeName = shape.asString();
					return ConfiguredModel.builder()
						.modelFile(p.models()
							.withExistingParent(prefix + "/" + shapeName, p.modLoc("block/belt_tunnel/" + shapeName))
							.texture("top", p.modLoc(prefix + "_top" + window))
							.texture("tunnel", p.modLoc(prefix))
							.texture("direction", p.modLoc(funnel_prefix + "_neutral"))
							.texture("frame", p.modLoc(funnel_prefix + "_frame"))
							.texture("particle", particleTexture))
						.rotationY(state.get(BeltTunnelBlock.HORIZONTAL_AXIS) == Axis.X ? 0 : 90)
						.build();
				}))
			.item(BeltTunnelItem::new)
			.model((c, p) -> {
				p.withExistingParent("item/" + type + "_tunnel", p.modLoc("block/belt_tunnel/item"))
					.texture("top", p.modLoc(prefix + "_top"))
					.texture("tunnel", p.modLoc(prefix))
					.texture("direction", p.modLoc(funnel_prefix + "_neutral"))
					.texture("frame", p.modLoc(funnel_prefix + "_frame"))
					.texture("particle", particleTexture);
			})
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> mechanicalPiston(PistonType type) {
		return b -> b.initialProperties(SharedProperties::stone)
			.properties(p -> p.nonOpaque())
			.blockstate(new MechanicalPistonGenerator(type)::generate)
			.addLayer(() -> RenderLayer::getCutoutMipped)
			.transform(BlockStressDefaults.setImpact(4.0))
			.item()
			.transform(ModelGen.customItemModel("mechanical_piston", type.asString(), "item"));
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> bearing(String prefix,
		String backTexture) {
		Identifier baseBlockModelLocation = Create.asResource("block/bearing/block");
		Identifier baseItemModelLocation = Create.asResource("block/bearing/item");
		Identifier topTextureLocation = Create.asResource("block/bearing_top");
		Identifier sideTextureLocation = Create.asResource("block/" + prefix + "_bearing_side");
		Identifier backTextureLocation = Create.asResource("block/" + backTexture);
		return b -> b.initialProperties(SharedProperties::stone)
			.properties(p -> p.nonOpaque())
			.blockstate((c, p) -> p.directionalBlock(c.get(), p.models()
				.withExistingParent(c.getName(), baseBlockModelLocation)
				.texture("side", sideTextureLocation)
				.texture("back", backTextureLocation)))
			.item()
			.model((c, p) -> p.withExistingParent(c.getName(), baseItemModelLocation)
				.texture("top", topTextureLocation)
				.texture("side", sideTextureLocation)
				.texture("back", backTextureLocation))
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> crate(String type) {
		return b -> b.initialProperties(SharedProperties::stone)
			.transform(axeOrPickaxe())
			.blockstate((c, p) -> {
				String[] variants = { "single", "top", "bottom", "left", "right" };
				Map<String, ModelFile> models = new HashMap<>();

				Identifier crate = p.modLoc("block/crate_" + type);
				Identifier side = p.modLoc("block/crate_" + type + "_side");
				Identifier casing = p.modLoc("block/" + type + "_casing");

				for (String variant : variants)
					models.put(variant, p.models()
						.withExistingParent("block/crate/" + type + "/" + variant, p.modLoc("block/crate/" + variant))
						.texture("crate", crate)
						.texture("side", side)
						.texture("casing", casing));

				p.getVariantBuilder(c.get())
					.forAllStates(state -> {
						String variant = "single";
						return ConfiguredModel.builder()
							.modelFile(models.get(variant))
							.build();
					});
			})
			.item()
			.properties(p -> type.equals("creative") ? p.rarity(Rarity.EPIC) : p)
			.transform(ModelGen.customItemModel("crate", type, "single"));
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> backtank(Supplier<ItemConvertible> drop) {
		return b -> b.blockstate((c, p) -> p.horizontalBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
			.transform(pickaxeOnly())
			.addLayer(() -> RenderLayer::getCutoutMipped)
			.transform(BlockStressDefaults.setImpact(4.0))
			.loot((lt, block) -> {
				Builder builder = LootTable.builder();
				LootCondition.Builder survivesExplosion = SurvivesExplosionLootCondition.builder();
				lt.addDrop(block, builder.pool(LootPool.builder()
					.conditionally(survivesExplosion)
					.rolls(ConstantLootNumberProvider.create(1))
					.with(ItemEntry.builder(drop.get())
						.apply(CopyNameLootFunction.builder(CopyNameLootFunction.Source.BLOCK_ENTITY))
						.apply(CopyNbtLootFunction.builder(ContextLootNbtProvider.BLOCK_ENTITY)
							.withOperation("Air", "Air"))
						.apply(CopyNbtLootFunction.builder(ContextLootNbtProvider.BLOCK_ENTITY)
							.withOperation("Enchantments", "Enchantments")))));
			});
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> bell() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.nonOpaque()
				.sounds(BlockSoundGroup.ANVIL))
			.transform(pickaxeOnly())
			.addLayer(() -> RenderLayer::getCutoutMipped)
			.tag(AllBlockTags.BRITTLE.tag)
			.blockstate((c, p) -> p.horizontalBlock(c.getEntry(), state -> {
				String variant = state.get(Properties.ATTACHMENT)
					.asString();
				return p.models()
					.withExistingParent(c.getName() + "_" + variant, p.modLoc("block/bell_base/block_" + variant));
			}))
			.item()
			.model((c, p) -> p.withExistingParent(c.getName(), p.modLoc("block/" + c.getName())))
			.tag(AllItemTags.CONTRAPTION_CONTROLLED.tag)
			.build();
	}

}
