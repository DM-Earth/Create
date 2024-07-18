package com.simibubi.create.content.kinetics.deployer;

import java.util.List;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.BlockHelper;

public class ManualApplicationRecipe extends ItemApplicationRecipe {

	public static ActionResult manualApplicationRecipesApplyInWorld(PlayerEntity player, World level, Hand hand, BlockHitResult hitResult) {
		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;

		ItemStack heldItem = player.getStackInHand(hand);
		BlockPos pos = hitResult.getBlockPos();
		BlockState blockState = level.getBlockState(pos);

		if (heldItem.isEmpty())
			return ActionResult.PASS;
		if (blockState.isAir())
			return ActionResult.PASS;

		RecipeType<Recipe<Inventory>> type = AllRecipeTypes.ITEM_APPLICATION.getType();
		Optional<Recipe<Inventory>> foundRecipe = level.getRecipeManager()
			.listAllOfType(type)
			.stream()
			.filter(r -> {
				ManualApplicationRecipe mar = (ManualApplicationRecipe) r;
				return mar.testBlock(blockState) && mar.ingredients.get(1)
					.test(heldItem);
			})
			.findFirst();

		if (foundRecipe.isEmpty())
			return ActionResult.PASS;

//		event.setCancellationResult(InteractionResult.SUCCESS);
//		event.setCanceled(true);

		if (level.isClient())
			return ActionResult.SUCCESS;

		level.playSound(null, pos, SoundEvents.BLOCK_COPPER_BREAK, SoundCategory.PLAYERS, 1, 1.45f);
		ManualApplicationRecipe recipe = (ManualApplicationRecipe) foundRecipe.get();
		level.breakBlock(pos, false);

		BlockState transformedBlock = recipe.transformBlock(blockState);
		level.setBlockState(pos, transformedBlock, 3);
		recipe.rollResults()
			.forEach(stack -> Block.dropStack(level, pos, stack));

		boolean creative = player != null && player.isCreative();
		boolean unbreakable = heldItem.hasNbt() && heldItem.getNbt()
			.getBoolean("Unbreakable");
		boolean keepHeld = recipe.shouldKeepHeldItem() || creative;

		if (!unbreakable && !keepHeld) {
			if (heldItem.isDamageable())
				heldItem.damage(1, player, s -> s.sendToolBreakStatus(Hand.MAIN_HAND));
			else
				heldItem.decrement(1);
		}

		awardAdvancements(player, transformedBlock);
		return ActionResult.SUCCESS;
	}

	private static void awardAdvancements(PlayerEntity player, BlockState placed) {
		CreateAdvancement advancement = null;

		if (AllBlocks.ANDESITE_CASING.has(placed))
			advancement = AllAdvancements.ANDESITE_CASING;
		else if (AllBlocks.BRASS_CASING.has(placed))
			advancement = AllAdvancements.BRASS_CASING;
		else if (AllBlocks.COPPER_CASING.has(placed))
			advancement = AllAdvancements.COPPER_CASING;
		else if (AllBlocks.RAILWAY_CASING.has(placed))
			advancement = AllAdvancements.TRAIN_CASING;
		else
			return;

		advancement.awardTo(player);
	}

	public ManualApplicationRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.ITEM_APPLICATION, params);
	}

	public static DeployerApplicationRecipe asDeploying(Recipe<?> recipe) {
		ManualApplicationRecipe mar = (ManualApplicationRecipe) recipe;
		ProcessingRecipeBuilder<DeployerApplicationRecipe> builder =
			new ProcessingRecipeBuilder<>(DeployerApplicationRecipe::new,
				new Identifier(mar.id.getNamespace(), mar.id.getPath() + "_using_deployer"))
					.require(mar.ingredients.get(0))
					.require(mar.ingredients.get(1));
		for (ProcessingOutput output : mar.results)
			builder.output(output);
		if (mar.shouldKeepHeldItem())
			builder.toolNotConsumed();
		return builder.build();
	}

	public boolean testBlock(BlockState in) {
		return ingredients.get(0)
			.test(new ItemStack(in.getBlock()
				.asItem()));
	}

	public BlockState transformBlock(BlockState in) {
		ProcessingOutput mainOutput = results.get(0);
		ItemStack output = mainOutput.rollOutput();
		if (output.getItem() instanceof BlockItem bi)
			return BlockHelper.copyProperties(in, bi.getBlock()
				.getDefaultState());
		return Blocks.AIR.getDefaultState();
	}

	@Override
	public List<ItemStack> rollResults() {
		return rollResults(getRollableResultsExceptBlock());
	}

	public List<ProcessingOutput> getRollableResultsExceptBlock() {
		ProcessingOutput mainOutput = results.get(0);
		if (mainOutput.getStack()
			.getItem() instanceof BlockItem)
			return results.subList(1, results.size());
		return results;
	}

}
