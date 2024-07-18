package com.simibubi.create.content.equipment.blueprint;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import io.github.fabricators_of_create.porting_lib.entity.IEntityAdditionalSpawnData;

import io.github.fabricators_of_create.porting_lib.entity.PortingLibEntity;
import net.fabricmc.fabric.api.entity.FakePlayer;

import org.apache.commons.lang3.Validate;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.requirement.ISpecialEntityItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.networking.ISyncPersistentData;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.IInteractionChecker;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.fabric.ReachUtil;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public class BlueprintEntity extends AbstractDecorationEntity
	implements IEntityAdditionalSpawnData, ISpecialEntityItemRequirement, ISyncPersistentData, IInteractionChecker {

	protected int size;
	protected Direction verticalOrientation;

	@SuppressWarnings("unchecked")
	public BlueprintEntity(EntityType<?> p_i50221_1_, World p_i50221_2_) {
		super((EntityType<? extends AbstractDecorationEntity>) p_i50221_1_, p_i50221_2_);
		size = 1;
	}

	public BlueprintEntity(World world, BlockPos pos, Direction facing, Direction verticalOrientation) {
		super(AllEntityTypes.CRAFTING_BLUEPRINT.get(), world, pos);

		for (int size = 3; size > 0; size--) {
			this.size = size;
			this.updateFacingWithBoundingBox(facing, verticalOrientation);
			if (this.canStayAttached())
				break;
		}
	}

	public static FabricEntityTypeBuilder<?> build(FabricEntityTypeBuilder<?> builder) {
//		@SuppressWarnings("unchecked")
//		EntityType.Builder<BlueprintEntity> entityBuilder = (EntityType.Builder<BlueprintEntity>) builder;
		return builder;
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		return PortingLibEntity.getEntitySpawningPacket(this);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound p_213281_1_) {
		p_213281_1_.putByte("Facing", (byte) this.facing.getId());
		p_213281_1_.putByte("Orientation", (byte) this.verticalOrientation.getId());
		p_213281_1_.putInt("Size", size);
		super.writeCustomDataToNbt(p_213281_1_);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound p_70037_1_) {
		if (p_70037_1_.contains("Facing", NbtElement.NUMBER_TYPE)) {
			this.facing = Direction.byId(p_70037_1_.getByte("Facing"));
			this.verticalOrientation = Direction.byId(p_70037_1_.getByte("Orientation"));
			this.size = p_70037_1_.getInt("Size");
		} else {
			this.facing = Direction.SOUTH;
			this.verticalOrientation = Direction.DOWN;
			this.size = 1;
		}
		super.readCustomDataFromNbt(p_70037_1_);
		this.updateFacingWithBoundingBox(this.facing, this.verticalOrientation);
	}

	protected void updateFacingWithBoundingBox(Direction facing, Direction verticalOrientation) {
		Validate.notNull(facing);
		this.facing = facing;
		this.verticalOrientation = verticalOrientation;
		if (facing.getAxis()
			.isHorizontal()) {
			setPitch(0.0F);
			setYaw(this.facing.getHorizontal() * 90);
		} else {
			setPitch(-90 * facing.getDirection()
				.offset());
			setYaw(verticalOrientation.getAxis()
				.isHorizontal() ? 180 + verticalOrientation.asRotation() : 0);
		}

		this.prevPitch = getPitch();
		this.prevYaw = getYaw();
		this.updateAttachmentPosition();
	}

	@Override
	protected float getEyeHeight(EntityPose p_213316_1_, EntityDimensions p_213316_2_) {
		return 0;
	}

	@Override
	protected void updateAttachmentPosition() {
		if (this.facing == null)
			return;
		if (this.verticalOrientation == null)
			return;

		Vec3d pos = Vec3d.of(getDecorationBlockPos())
			.add(.5, .5, .5)
			.subtract(Vec3d.of(facing.getVector())
				.multiply(0.46875));
		double d1 = pos.x;
		double d2 = pos.y;
		double d3 = pos.z;
		this.setPos(d1, d2, d3);

		Axis axis = facing.getAxis();
		if (size == 2)
			pos = pos.add(Vec3d.of(axis.isHorizontal() ? facing.rotateYCounterclockwise()
				.getVector()
				: verticalOrientation.rotateYClockwise()
					.getVector())
				.multiply(0.5))
				.add(Vec3d
					.of(axis.isHorizontal() ? Direction.UP.getVector()
						: facing == Direction.UP ? verticalOrientation.getVector()
							: verticalOrientation.getOpposite()
								.getVector())
					.multiply(0.5));

		d1 = pos.x;
		d2 = pos.y;
		d3 = pos.z;

		double d4 = (double) this.getWidthPixels();
		double d5 = (double) this.getHeightPixels();
		double d6 = (double) this.getWidthPixels();
		Direction.Axis direction$axis = this.facing.getAxis();
		switch (direction$axis) {
		case X:
			d4 = 1.0D;
			break;
		case Y:
			d5 = 1.0D;
			break;
		case Z:
			d6 = 1.0D;
		}

		d4 = d4 / 32.0D;
		d5 = d5 / 32.0D;
		d6 = d6 / 32.0D;
		this.setBoundingBox(new Box(d1 - d4, d2 - d5, d3 - d6, d1 + d4, d2 + d5, d3 + d6));
	}

	@Override
	public boolean canStayAttached() {
		if (!getWorld().isSpaceEmpty(this))
			return false;

		int i = Math.max(1, this.getWidthPixels() / 16);
		int j = Math.max(1, this.getHeightPixels() / 16);
		BlockPos blockpos = this.attachmentPos.offset(this.facing.getOpposite());
		Direction upDirection = facing.getAxis()
			.isHorizontal() ? Direction.UP
				: facing == Direction.UP ? verticalOrientation : verticalOrientation.getOpposite();
		Direction newDirection = facing.getAxis()
			.isVertical() ? verticalOrientation.rotateYClockwise() : facing.rotateYCounterclockwise();
		BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

		for (int k = 0; k < i; ++k) {
			for (int l = 0; l < j; ++l) {
				int i1 = (i - 1) / -2;
				int j1 = (j - 1) / -2;
				blockpos$mutable.set(blockpos)
					.move(newDirection, k + i1)
					.move(upDirection, l + j1);
				BlockState blockstate = this.getWorld().getBlockState(blockpos$mutable);
				if (Block.sideCoversSmallSquare(this.getWorld(), blockpos$mutable, this.facing))
					continue;
				if (!blockstate.isSolid() && !AbstractRedstoneGateBlock.isRedstoneGate(blockstate)) {
					return false;
				}
			}
		}

		return this.getWorld().getOtherEntities(this, this.getBoundingBox(), PREDICATE)
			.isEmpty();
	}

	@Override
	public int getWidthPixels() {
		return 16 * size;
	}

	@Override
	public int getHeightPixels() {
		return 16 * size;
	}

	@Override
	public boolean handleAttack(Entity source) {
		if (!(source instanceof PlayerEntity) || getWorld().isClient)
			return super.handleAttack(source);

		PlayerEntity player = (PlayerEntity) source;
		double attrib = ReachUtil.reach(player);

		Vec3d eyePos = source.getCameraPosVec(1);
		Vec3d look = source.getRotationVec(1);
		Vec3d target = eyePos.add(look.multiply(attrib));

		Optional<Vec3d> rayTrace = getBoundingBox().raycast(eyePos, target);
		if (!rayTrace.isPresent())
			return super.handleAttack(source);

		Vec3d hitVec = rayTrace.get();
		BlueprintSection sectionAt = getSectionAt(hitVec.subtract(getPos()));
		ItemStackHandler items = sectionAt.getItems();

		if (items.getStackInSlot(9)
			.isEmpty())
			return super.handleAttack(source);
		for (int i = 0; i < items.getSlotCount(); i++)
			items.setStackInSlot(i, ItemStack.EMPTY);
		sectionAt.save(items);
		return true;
	}

	@Override
	public void onBreak(@Nullable Entity p_110128_1_) {
		if (!getWorld().getGameRules()
			.getBoolean(GameRules.DO_ENTITY_DROPS))
			return;

		playSound(SoundEvents.ENTITY_PAINTING_BREAK, 1.0F, 1.0F);
		if (p_110128_1_ instanceof PlayerEntity) {
			PlayerEntity playerentity = (PlayerEntity) p_110128_1_;
			if (playerentity.getAbilities().creativeMode)
				return;
		}

		dropStack(AllItems.CRAFTING_BLUEPRINT.asStack());
	}

	@Nullable
	@Override
	public ItemStack getPickBlockStack() {
		return AllItems.CRAFTING_BLUEPRINT.asStack();
	}

	@Override
	public ItemRequirement getRequiredItems() {
		return new ItemRequirement(ItemUseType.CONSUME, AllItems.CRAFTING_BLUEPRINT.get());
	}

	@Override
	public void onPlace() {
		this.playSound(SoundEvents.ENTITY_PAINTING_PLACE, 1.0F, 1.0F);
	}

	@Override
	public void refreshPositionAndAngles(double p_70012_1_, double p_70012_3_, double p_70012_5_, float p_70012_7_, float p_70012_8_) {
		this.setPosition(p_70012_1_, p_70012_3_, p_70012_5_);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void updateTrackedPositionAndAngles(double p_180426_1_, double p_180426_3_, double p_180426_5_, float p_180426_7_, float p_180426_8_,
		int p_180426_9_, boolean p_180426_10_) {
		BlockPos blockpos =
			this.attachmentPos.add(BlockPos.ofFloored(p_180426_1_ - this.getX(), p_180426_3_ - this.getY(), p_180426_5_ - this.getZ()));
		this.setPosition((double) blockpos.getX(), (double) blockpos.getY(), (double) blockpos.getZ());
	}

	@Override
	public void writeSpawnData(PacketByteBuf buffer) {
		NbtCompound compound = new NbtCompound();
		writeCustomDataToNbt(compound);
		buffer.writeNbt(compound);
		buffer.writeNbt(getCustomData());
	}

	@Override
	public void readSpawnData(PacketByteBuf additionalData) {
		readCustomDataFromNbt(additionalData.readNbt());
		getCustomData().copyFrom(additionalData.readNbt());
	}

	@Override
	public ActionResult interactAt(PlayerEntity player, Vec3d vec, Hand hand) {
		if (player instanceof FakePlayer)
			return ActionResult.PASS;

		boolean holdingWrench = AllItems.WRENCH.isIn(player.getStackInHand(hand));
		BlueprintSection section = getSectionAt(vec);
		ItemStackHandler items = section.getItems();

		if (!holdingWrench && !getWorld().isClient && !items.getStackInSlot(9)
			.isEmpty()) {


			PlayerInventoryStorage playerInv = PlayerInventoryStorage.of(player);
			boolean firstPass = true;
			int amountCrafted = 0;
			//ForgeHooks.setCraftingPlayer(player);
			Optional<CraftingRecipe> recipe = Optional.empty();

			do {
				try (Transaction t = TransferUtil.getTransaction()) {
					Map<Integer, ItemStack> craftingGrid = new HashMap<>();
					boolean success = true;

					Search: for (int i = 0; i < 9; i++) {
						FilterItemStack requestedItem = FilterItemStack.of(items.getStackInSlot(i));
						if (requestedItem.isEmpty()) {
							craftingGrid.put(i, ItemStack.EMPTY);
							continue;
						}

						ResourceAmount<ItemVariant> resource = StorageUtil.findExtractableContent(
								playerInv, v -> requestedItem.test(getWorld(), v.toStack()), t);
						if (resource != null) {
							playerInv.extract(resource.resource(), 1, t);
							craftingGrid.put(i, resource.resource().toStack());
							continue Search;
						}

						success = false;
						break;
					}

					if (success) {
						RecipeInputInventory craftingInventory = new BlueprintCraftingInventory(craftingGrid);

						if (!recipe.isPresent())
							recipe = getWorld().getRecipeManager()
									.getFirstMatch(RecipeType.CRAFTING, craftingInventory, getWorld());
						ItemStack result = recipe.filter(r -> r.matches(craftingInventory, getWorld()))
								.map(r -> r.craft(craftingInventory, getWorld().getRegistryManager()))
								.orElse(ItemStack.EMPTY);

						if (result.isEmpty()) {
							success = false;
						} else if (result.getCount() + amountCrafted > 64) {
							success = false;
						} else {
							amountCrafted += result.getCount();
							result.onCraft(player.getWorld(), player, 1);
//						ForgeEventFactory.firePlayerCraftingEvent(player, result, craftingInventory);
							DefaultedList<ItemStack> nonnulllist = getWorld().getRecipeManager()
									.getRemainingStacks(RecipeType.CRAFTING, craftingInventory, getWorld());

							if (firstPass)
								getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS,
										.2f, 1f + Create.RANDOM.nextFloat());
							player.getInventory()
									.offerOrDrop(result);
							for (ItemStack itemStack : nonnulllist)
								player.getInventory()
										.offerOrDrop(itemStack);
							firstPass = false;
						}
					}

					if (!success) {
						t.abort();
						break;
					} else t.commit();
				}
			} while (player.isSneaking());

//			ForgeHooks.setCraftingPlayer(null);
			return ActionResult.SUCCESS;
		}

		int i = section.index;
		if (!getWorld().isClient && player instanceof ServerPlayerEntity) {
			NetworkHooks.openScreen((ServerPlayerEntity) player, section, buf -> {
				buf.writeVarInt(getId());
				buf.writeVarInt(i);
			});
		}

		return ActionResult.SUCCESS;
	}

	public BlueprintSection getSectionAt(Vec3d vec) {
		int index = 0;
		if (size > 1) {
			vec = VecHelper.rotate(vec, getYaw(), Axis.Y);
			vec = VecHelper.rotate(vec, -getPitch(), Axis.X);
			vec = vec.add(0.5, 0.5, 0);
			if (size == 3)
				vec = vec.add(1, 1, 0);
			int x = MathHelper.clamp(MathHelper.floor(vec.x), 0, size - 1);
			int y = MathHelper.clamp(MathHelper.floor(vec.y), 0, size - 1);
			index = x + y * size;
		}

		BlueprintSection section = getSection(index);
		return section;
	}

	static class BlueprintCraftingInventory extends CraftingInventory {

		private static final ScreenHandler dummyContainer = new ScreenHandler(null, -1) {
			public boolean canUse(PlayerEntity playerIn) {
				return false;
			}

			@Override
			public ItemStack quickMove(PlayerEntity p_38941_, int p_38942_) {
				return ItemStack.EMPTY;
			}
		};

		public BlueprintCraftingInventory(Map<Integer, ItemStack> items) {
			super(dummyContainer, 3, 3);
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					ItemStack stack = items.get(y * 3 + x);
					setStack(y * 3 + x, stack == null ? ItemStack.EMPTY : stack.copy());
				}
			}
		}

	}

	public NbtCompound getOrCreateRecipeCompound() {
		NbtCompound persistentData = getCustomData();
		if (!persistentData.contains("Recipes"))
			persistentData.put("Recipes", new NbtCompound());
		return persistentData.getCompound("Recipes");
	}

	private Map<Integer, BlueprintSection> sectionCache = new HashMap<>();

	public BlueprintSection getSection(int index) {
		return sectionCache.computeIfAbsent(index, i -> new BlueprintSection(i));
	}

	class BlueprintSection implements NamedScreenHandlerFactory, IInteractionChecker {
		int index;
		Couple<ItemStack> cachedDisplayItems;
		public boolean inferredIcon = false;

		public BlueprintSection(int index) {
			this.index = index;
		}

		public Couple<ItemStack> getDisplayItems() {
			if (cachedDisplayItems != null)
				return cachedDisplayItems;
			ItemStackHandler items = getItems();
			return cachedDisplayItems = Couple.create(items.getStackInSlot(9), items.getStackInSlot(10));
		}

		public ItemStackHandler getItems() {
			ItemStackHandler newInv = new ItemStackHandler(11);
			NbtCompound list = getOrCreateRecipeCompound();
			NbtCompound invNBT = list.getCompound(index + "");
			inferredIcon = list.getBoolean("InferredIcon");
			if (!invNBT.isEmpty())
				newInv.deserializeNBT(invNBT);
			return newInv;
		}

		public void save(ItemStackHandler inventory) {
			NbtCompound list = getOrCreateRecipeCompound();
			list.put(index + "", inventory.serializeNBT());
			list.putBoolean("InferredIcon", inferredIcon);
			cachedDisplayItems = null;
			if (!getWorld().isClient)
				syncPersistentDataWithTracking(BlueprintEntity.this);
		}

		public boolean isEntityAlive() {
			return isAlive();
		}

		public World getBlueprintWorld() {
			return getWorld();
		}

		@Override
		public ScreenHandler createMenu(int id, PlayerInventory inv, PlayerEntity player) {
			return BlueprintMenu.create(id, inv, this);
		}

		@Override
		public Text getDisplayName() {
			return AllItems.CRAFTING_BLUEPRINT.get()
				.getName();
		}

		@Override
		public boolean canPlayerUse(PlayerEntity player) {
			return BlueprintEntity.this.canPlayerUse(player);
		}

	}

	@Override
	public void onPersistentDataUpdated() {
		sectionCache.clear();
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		Box box = getBoundingBox();

		double dx = 0;
		if (box.minX > player.getX()) {
			dx = box.minX - player.getX();
		} else if (player.getX() > box.maxX) {
			dx = player.getX() - box.maxX;
		}

		double dy = 0;
		if (box.minY > player.getY()) {
			dy = box.minY - player.getY();
		} else if (player.getY() > box.maxY) {
			dy = player.getY() - box.maxY;
		}

		double dz = 0;
		if (box.minZ > player.getZ()) {
			dz = box.minZ - player.getZ();
		} else if (player.getZ() > box.maxZ) {
			dz = player.getZ() - box.maxZ;
		}

		return (dx * dx + dy * dy + dz * dz) <= 64.0D;
	}

}
