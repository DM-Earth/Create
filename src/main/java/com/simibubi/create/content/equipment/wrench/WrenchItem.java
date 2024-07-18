package com.simibubi.create.content.equipment.wrench;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;

public class WrenchItem extends Item {

	public WrenchItem(Settings properties) {
		super(properties);
	}

	@Nonnull
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		if (player == null || !player.canModifyBlocks())
			return super.useOnBlock(context);

		BlockState state = context.getWorld()
				.getBlockState(context.getBlockPos());
		Block block = state.getBlock();

		if (!(block instanceof IWrenchable)) {
			if (player.isSneaking() && canWrenchPickup(state))
				return onItemUseOnOther(context);
			return super.useOnBlock(context);
		}

		IWrenchable actor = (IWrenchable) block;
		if (player.isSneaking())
			return actor.onSneakWrenched(state, context);
		return actor.onWrenched(state, context);
	}

	private static boolean canWrenchPickup(BlockState state) {
		return AllTags.AllBlockTags.WRENCH_PICKUP.matches(state);
	}

	private static ActionResult onItemUseOnOther(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState state = world.getBlockState(pos);
		if (!(world instanceof ServerWorld))
			return ActionResult.SUCCESS;
		if (player != null && !player.isCreative())
			Block.getDroppedStacks(state, (ServerWorld) world, pos, world.getBlockEntity(pos), player, context.getStack())
				.forEach(itemStack -> player.getInventory().offerOrDrop(itemStack));
		state.onStacksDropped((ServerWorld) world, pos, ItemStack.EMPTY, true);
		world.breakBlock(pos, false);
		AllSoundEvents.WRENCH_REMOVE.playOnServer(world, pos, 1, Create.RANDOM.nextFloat() * .5f + .5f);
		return ActionResult.SUCCESS;
	}

	public static ActionResult wrenchInstaKillsMinecarts(PlayerEntity player, World world, Hand hand, Entity target, @Nullable EntityHitResult entityRayTraceResult) {
		if (!(target instanceof AbstractMinecartEntity))
			return ActionResult.PASS;
		ItemStack heldItem = player.getMainHandStack();
		if (!AllItems.WRENCH.isIn(heldItem))
			return ActionResult.PASS;
		if (player.isCreative())
			return ActionResult.PASS;
		AbstractMinecartEntity minecart = (AbstractMinecartEntity) target;
		minecart.damage(minecart.getDamageSources().playerAttack(player), 100);
		return ActionResult.SUCCESS;
	}

//	@Override
//	@OnlyIn(Dist.CLIENT)
//	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
//		consumer.accept(SimpleCustomRenderer.create(this, new WrenchItemRenderer()));
//	}

}
