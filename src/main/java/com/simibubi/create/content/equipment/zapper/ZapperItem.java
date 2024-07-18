package com.simibubi.create.content.equipment.zapper;

import java.util.List;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.NBTProcessors;
import io.github.fabricators_of_create.porting_lib.item.EntitySwingListenerItem;
import io.github.fabricators_of_create.porting_lib.item.ReequipAnimationItem;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

public abstract class ZapperItem extends Item implements CustomArmPoseItem, EntitySwingListenerItem, ReequipAnimationItem {

	public ZapperItem(Settings properties) {
		super(properties.maxCount(1));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendTooltip(ItemStack stack, World worldIn, List<Text> tooltip, TooltipContext flagIn) {
		if (stack.hasNbt() && stack.getNbt()
			.contains("BlockUsed")) {
			MutableText usedBlock = NbtHelper.toBlockState(worldIn.createCommandRegistryWrapper(RegistryKeys.BLOCK), stack.getNbt()
				.getCompound("BlockUsed"))
				.getBlock()
				.getName();
			tooltip.add(Lang.translateDirect("terrainzapper.usingBlock",
				usedBlock.formatted(Formatting.GRAY))
					.formatted(Formatting.DARK_GRAY));
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		boolean differentBlock = false;
		if (oldStack.hasNbt() && newStack.hasNbt() && oldStack.getNbt()
			.contains("BlockUsed")
			&& newStack.getNbt()
				.contains("BlockUsed"))
			differentBlock = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), oldStack.getNbt()
				.getCompound("BlockUsed")) != NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(),
					newStack.getNbt()
						.getCompound("BlockUsed"));
		return slotChanged || !isZapper(newStack) || differentBlock;
	}

	public boolean isZapper(ItemStack newStack) {
		return newStack.getItem() instanceof ZapperItem;
	}

	@Nonnull
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		// Shift -> open GUI
		if (context.getPlayer() != null && context.getPlayer()
			.isSneaking()) {
			if (context.getWorld().isClient) {
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
					openHandgunGUI(context.getStack(), context.getHand());
				});
				context.getPlayer()
					.getItemCooldownManager()
					.set(context.getStack()
						.getItem(), 10);
			}
			return ActionResult.SUCCESS;
		}
		return super.useOnBlock(context);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack item = player.getStackInHand(hand);
		NbtCompound nbt = item.getOrCreateNbt();
		boolean mainHand = hand == Hand.MAIN_HAND;

		// Shift -> Open GUI
		if (player.isSneaking()) {
			if (world.isClient) {
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
					openHandgunGUI(item, hand);
				});
				player.getItemCooldownManager()
					.set(item.getItem(), 10);
			}
			return new TypedActionResult<>(ActionResult.SUCCESS, item);
		}

		if (ShootableGadgetItemMethods.shouldSwap(player, item, hand, this::isZapper))
			return new TypedActionResult<>(ActionResult.FAIL, item);

		// Check if can be used
		Text msg = validateUsage(item);
		if (msg != null) {
			AllSoundEvents.DENY.play(world, player, player.getBlockPos());
			player.sendMessage(msg.copyContentOnly()
				.formatted(Formatting.RED), true);
			return new TypedActionResult<>(ActionResult.FAIL, item);
		}

		BlockState stateToUse = Blocks.AIR.getDefaultState();
		if (nbt.contains("BlockUsed"))
			stateToUse = NbtHelper.toBlockState(world.createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("BlockUsed"));
		stateToUse = BlockHelper.setZeroAge(stateToUse);
		NbtCompound data = null;
		if (AllBlockTags.SAFE_NBT.matches(stateToUse) && nbt.contains("BlockData", NbtElement.COMPOUND_TYPE)) {
			data = nbt.getCompound("BlockData");
		}

		// Raytrace - Find the target
		Vec3d start = player.getPos()
			.add(0, player.getStandingEyeHeight(), 0);
		Vec3d range = player.getRotationVector()
			.multiply(getZappingRange(item));
		BlockHitResult raytrace =
			world.raycast(new RaycastContext(start, start.add(range), ShapeType.OUTLINE, FluidHandling.NONE, player));
		BlockPos pos = raytrace.getBlockPos();
		BlockState stateReplaced = world.getBlockState(pos);

		// No target
		if (pos == null || stateReplaced.getBlock() == Blocks.AIR) {
			ShootableGadgetItemMethods.applyCooldown(player, item, hand, this::isZapper, getCooldownDelay(item));
			return new TypedActionResult<>(ActionResult.SUCCESS, item);
		}

		// Find exact position of gun barrel for VFX
		Vec3d barrelPos = ShootableGadgetItemMethods.getGunBarrelVec(player, mainHand, new Vec3d(.35f, -0.1f, 1));

		// Client side
		if (world.isClient) {
			CreateClient.ZAPPER_RENDER_HANDLER.dontAnimateItem(hand);
			return new TypedActionResult<>(ActionResult.SUCCESS, item);
		}

		// Server side
		if (activate(world, player, item, stateToUse, raytrace, data)) {
			ShootableGadgetItemMethods.applyCooldown(player, item, hand, this::isZapper, getCooldownDelay(item));
			ShootableGadgetItemMethods.sendPackets(player,
				b -> new ZapperBeamPacket(barrelPos, raytrace.getPos(), hand, b));
		}

		return new TypedActionResult<>(ActionResult.SUCCESS, item);
	}

	public Text validateUsage(ItemStack item) {
		NbtCompound tag = item.getOrCreateNbt();
		if (!canActivateWithoutSelectedBlock(item) && !tag.contains("BlockUsed"))
			return Lang.translateDirect("terrainzapper.leftClickToSet");
		return null;
	}

	protected abstract boolean activate(World world, PlayerEntity player, ItemStack item, BlockState stateToUse,
		BlockHitResult raytrace, NbtCompound data);

	@Environment(EnvType.CLIENT)
	protected abstract void openHandgunGUI(ItemStack item, Hand hand);

	protected abstract int getCooldownDelay(ItemStack item);

	protected abstract int getZappingRange(ItemStack stack);

	protected boolean canActivateWithoutSelectedBlock(ItemStack stack) {
		return false;
	}

	@Override
	public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
		return true;
	}

	@Override
	public boolean canMine(BlockState state, World worldIn, BlockPos pos, PlayerEntity player) {
		return false;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.NONE;
	}

	@Override
	@Nullable
	public ArmPose getArmPose(ItemStack stack, AbstractClientPlayerEntity player, Hand hand) {
		if (!player.handSwinging) {
			return ArmPose.CROSSBOW_HOLD;
		}
		return null;
	}

	public static void configureSettings(ItemStack stack, PlacementPatterns pattern) {
		NbtCompound nbt = stack.getOrCreateNbt();
		NBTHelper.writeEnum(nbt, "Pattern", pattern);
	}

	public static void setBlockEntityData(World world, BlockPos pos, BlockState state, NbtCompound data, PlayerEntity player) {
		if (data != null && AllBlockTags.SAFE_NBT.matches(state)) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity != null) {
				data = NBTProcessors.process(blockEntity, data, !player.isCreative());
				if (data == null)
					return;
				data.putInt("x", pos.getX());
				data.putInt("y", pos.getY());
				data.putInt("z", pos.getZ());
				blockEntity.readNbt(data);
			}
		}
	}

}
