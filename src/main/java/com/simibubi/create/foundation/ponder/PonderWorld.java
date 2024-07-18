package com.simibubi.create.foundation.ponder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.base.Suppliers;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.mixin.accessor.ParticleEngineAccessor;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.worldWrappers.WrappedClientWorld;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

public class PonderWorld extends SchematicWorld {

	public PonderScene scene;

	protected Map<BlockPos, BlockState> originalBlocks;
	protected Map<BlockPos, NbtCompound> originalBlockEntities;
	protected Map<BlockPos, Integer> blockBreakingProgressions;
	protected List<Entity> originalEntities;
	private Supplier<ClientWorld> asClientWorld = Suppliers.memoize(() -> WrappedClientWorld.of(this));

	protected PonderWorldParticles particles;
	private final Int2ObjectMap<ParticleFactory<?>> particleProviders;

	int overrideLight;
	Selection mask;
	boolean currentlyTickingEntities;

	public PonderWorld(BlockPos anchor, World original) {
		super(anchor, original);
		originalBlocks = new HashMap<>();
		originalBlockEntities = new HashMap<>();
		blockBreakingProgressions = new HashMap<>();
		originalEntities = new ArrayList<>();
		particles = new PonderWorldParticles(this);
		particleProviders = ((ParticleEngineAccessor) MinecraftClient.getInstance().particleManager).create$getFactories();
	}

	public void createBackup() {
		originalBlocks.clear();
		originalBlockEntities.clear();
		blocks.forEach((k, v) -> originalBlocks.put(k, v));
		blockEntities.forEach((k, v) -> originalBlockEntities.put(k, v.createNbtWithIdentifyingData()));
		entities.forEach(e -> EntityType.getEntityFromNbt(NBTSerializer.serializeNBTCompound(e), this)
			.ifPresent(originalEntities::add));
	}

	public void restore() {
		entities.clear();
		blocks.clear();
		blockEntities.clear();
		blockBreakingProgressions.clear();
		renderedBlockEntities.clear();
		originalBlocks.forEach((k, v) -> blocks.put(k, v));
		originalBlockEntities.forEach((k, v) -> {
			BlockEntity blockEntity = BlockEntity.createFromNbt(k, originalBlocks.get(k), v);
			onBEadded(blockEntity, blockEntity.getPos());
			blockEntities.put(k, blockEntity);
			renderedBlockEntities.add(blockEntity);
		});
		originalEntities.forEach(e -> EntityType.getEntityFromNbt(NBTSerializer.serializeNBTCompound(e), this)
			.ifPresent(entities::add));
		particles.clearEffects();
		fixControllerBlockEntities();
	}

	public void restoreBlocks(Selection selection) {
		selection.forEach(p -> {
			if (originalBlocks.containsKey(p))
				blocks.put(p, originalBlocks.get(p));
			if (originalBlockEntities.containsKey(p)) {
				BlockEntity blockEntity = BlockEntity.createFromNbt(p, originalBlocks.get(p), originalBlockEntities.get(p));
				onBEadded(blockEntity, blockEntity.getPos());
				blockEntities.put(p, blockEntity);
			}
		});
		redraw();
	}

	private void redraw() {
		if (scene != null)
			scene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
	}

	public void pushFakeLight(int light) {
		this.overrideLight = light;
	}

	public void popLight() {
		this.overrideLight = -1;
	}

	@Override
	public int getLightLevel(LightType p_226658_1_, BlockPos p_226658_2_) {
		return overrideLight == -1 ? 15 : overrideLight;
	}

	public void setMask(Selection mask) {
		this.mask = mask;
	}

	public void clearMask() {
		this.mask = null;
	}

	@Override
	public BlockState getBlockState(BlockPos globalPos) {
		if (mask != null && !mask.test(globalPos.subtract(anchor)))
			return Blocks.AIR.getDefaultState();
		if (currentlyTickingEntities && globalPos.getY() < 0)
			return Blocks.AIR.getDefaultState();
		return super.getBlockState(globalPos);
	}

	@Override // For particle collision
	public BlockView getChunkAsView(int p_225522_1_, int p_225522_2_) {
		return this;
	}

	public void renderEntities(MatrixStack ms, SuperRenderTypeBuffer buffer, Camera ari, float pt) {
		Vec3d Vector3d = ari.getPos();
		double d0 = Vector3d.getX();
		double d1 = Vector3d.getY();
		double d2 = Vector3d.getZ();

		for (Entity entity : entities) {
			if (entity.age == 0) {
				entity.lastRenderX = entity.getX();
				entity.lastRenderY = entity.getY();
				entity.lastRenderZ = entity.getZ();
			}
			renderEntity(entity, d0, d1, d2, pt, ms, buffer);
		}

		buffer.draw(RenderLayer.getEntitySolid(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
		buffer.draw(RenderLayer.getEntityCutout(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
		buffer.draw(RenderLayer.getEntityCutoutNoCull(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
		buffer.draw(RenderLayer.getEntitySmoothCutout(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
	}

	private void renderEntity(Entity entity, double x, double y, double z, float pt, MatrixStack ms,
		VertexConsumerProvider buffer) {
		double d0 = MathHelper.lerp((double) pt, entity.lastRenderX, entity.getX());
		double d1 = MathHelper.lerp((double) pt, entity.lastRenderY, entity.getY());
		double d2 = MathHelper.lerp((double) pt, entity.lastRenderZ, entity.getZ());
		float f = MathHelper.lerp(pt, entity.prevYaw, entity.getYaw());
		EntityRenderDispatcher renderManager = MinecraftClient.getInstance()
			.getEntityRenderDispatcher();
		int light = renderManager.getRenderer(entity)
			.getLight(entity, pt);
		renderManager.render(entity, d0 - x, d1 - y, d2 - z, f, pt, ms, buffer, light);
	}

	public void renderParticles(MatrixStack ms, VertexConsumerProvider buffer, Camera ari, float pt) {
		particles.renderParticles(ms, buffer, ari, pt);
	}

	public void tick() {
		currentlyTickingEntities = true;

		particles.tick();

		for (Iterator<Entity> iterator = entities.iterator(); iterator.hasNext();) {
			Entity entity = iterator.next();

			entity.age++;
			entity.lastRenderX = entity.getX();
			entity.lastRenderY = entity.getY();
			entity.lastRenderZ = entity.getZ();
			entity.tick();

			if (entity.getY() <= -.5f)
				entity.discard();

			if (!entity.isAlive())
				iterator.remove();
		}

		currentlyTickingEntities = false;
	}

	@Override
	public void addParticle(ParticleEffect data, double x, double y, double z, double mx, double my, double mz) {
		addParticle(makeParticle(data, x, y, z, mx, my, mz));
	}

	@Override
	public void addImportantParticle(ParticleEffect data, double x, double y, double z, double mx, double my, double mz) {
		addParticle(data, x, y, z, mx, my, mz);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private <T extends ParticleEffect> Particle makeParticle(T data, double x, double y, double z, double mx, double my,
		double mz) {
		int id = Registries.PARTICLE_TYPE.getRawId(data.getType());
		ParticleFactory<T> particleProvider = (ParticleFactory<T>) particleProviders.get(id);
		return particleProvider == null ? null
			: particleProvider.createParticle(data, asClientWorld.get(), x, y, z, mx, my, mz);
	}

	@Override
	public boolean setBlockState(BlockPos pos, BlockState arg1, int arg2) {
		return super.setBlockState(pos, arg1, arg2);
	}

	public void addParticle(Particle p) {
		if (p != null)
			particles.addParticle(p);
	}

	@Override
	protected void onBEadded(BlockEntity blockEntity, BlockPos pos) {
		super.onBEadded(blockEntity, pos);
		if (!(blockEntity instanceof SmartBlockEntity))
			return;
		SmartBlockEntity smartBlockEntity = (SmartBlockEntity) blockEntity;
		smartBlockEntity.markVirtual();
	}

	public void fixControllerBlockEntities() {
		for (BlockEntity blockEntity : blockEntities.values()) {

			if (blockEntity instanceof BeltBlockEntity) {
				BeltBlockEntity beltBlockEntity = (BeltBlockEntity) blockEntity;
				if (!beltBlockEntity.isController())
					continue;
				BlockPos controllerPos = blockEntity.getPos();
				for (BlockPos blockPos : BeltBlock.getBeltChain(this, controllerPos)) {
					BlockEntity blockEntity2 = getBlockEntity(blockPos);
					if (!(blockEntity2 instanceof BeltBlockEntity))
						continue;
					BeltBlockEntity belt2 = (BeltBlockEntity) blockEntity2;
					belt2.setController(controllerPos);
				}
			}

			if (blockEntity instanceof IMultiBlockEntityContainer) {
				IMultiBlockEntityContainer multiBlockEntity = (IMultiBlockEntityContainer) blockEntity;
				BlockPos lastKnown = multiBlockEntity.getLastKnownPos();
				BlockPos current = blockEntity.getPos();
				if (lastKnown == null || current == null)
					continue;
				if (multiBlockEntity.isController())
					continue;
				if (!lastKnown.equals(current)) {
					BlockPos newControllerPos = multiBlockEntity.getController()
						.add(current.subtract(lastKnown));
					multiBlockEntity.setController(newControllerPos);
				}
			}

		}
	}

	public void setBlockBreakingProgress(BlockPos pos, int damage) {
		if (damage == 0)
			blockBreakingProgressions.remove(pos);
		else
			blockBreakingProgressions.put(pos, damage - 1);
	}

	public Map<BlockPos, Integer> getBlockBreakingProgressions() {
		return blockBreakingProgressions;
	}

	public void addBlockDestroyEffects(BlockPos pos, BlockState state) {
		VoxelShape voxelshape = state.getOutlineShape(this, pos);
		if (voxelshape.isEmpty())
			return;

		Box bb = voxelshape.getBoundingBox();
		double d1 = Math.min(1.0D, bb.maxX - bb.minX);
		double d2 = Math.min(1.0D, bb.maxY - bb.minY);
		double d3 = Math.min(1.0D, bb.maxZ - bb.minZ);
		int i = Math.max(2, MathHelper.ceil(d1 / 0.25D));
		int j = Math.max(2, MathHelper.ceil(d2 / 0.25D));
		int k = Math.max(2, MathHelper.ceil(d3 / 0.25D));

		for (int l = 0; l < i; ++l) {
			for (int i1 = 0; i1 < j; ++i1) {
				for (int j1 = 0; j1 < k; ++j1) {
					double d4 = (l + 0.5D) / i;
					double d5 = (i1 + 0.5D) / j;
					double d6 = (j1 + 0.5D) / k;
					double d7 = d4 * d1 + bb.minX;
					double d8 = d5 * d2 + bb.minY;
					double d9 = d6 * d3 + bb.minZ;
					addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), pos.getX() + d7, pos.getY() + d8,
						pos.getZ() + d9, d4 - 0.5D, d5 - 0.5D, d6 - 0.5D);
				}
			}
		}
	}

	@Override
	protected BlockState processBlockStateForPrinting(BlockState state) {
		return state;
	}

	@Override
	public boolean isChunkLoaded(BlockPos pos) {
		return true; // fix particle lighting
	}

	@Override
	public boolean isChunkLoaded(int x, int y) {
		return true; // fix particle lighting
	}

	@Override
	public boolean canSetBlock(BlockPos pos) {
		return true; // fix particle lighting
	}

	@Override
	public boolean isPlayerInRange(double p_217358_1_, double p_217358_3_, double p_217358_5_, double p_217358_7_) {
		return true; // always enable spawner animations
	}

	// In case another mod (read: Lithium) has overwritten noCollision and would break PonderWorlds, force vanilla behavior in PonderWorlds
	@Override
	public boolean isSpaceEmpty(@Nullable Entity entity, Box collisionBox) {
		// Vanilla copy
		Iterator var3 = this.getBlockCollisions(entity, collisionBox).iterator();

		while(var3.hasNext()) {
			VoxelShape voxelShape = (VoxelShape)var3.next();
			if (!voxelShape.isEmpty()) {
				return false;
			}
		}

		if (!this.getEntityCollisions(entity, collisionBox).isEmpty()) {
			return false;
		} else if (entity == null) {
			return true;
		} else {
			VoxelShape voxelShape2 = this.getWorldBorderCollisions(entity, collisionBox);
			return voxelShape2 == null || !VoxelShapes.matchesAnywhere(voxelShape2, VoxelShapes.cuboid(collisionBox), BooleanBiFunction.AND);
		}
	}

	VoxelShape getWorldBorderCollisions(Entity entity, Box aABB) {
		WorldBorder worldBorder = this.getWorldBorder();
		return worldBorder.canCollide(entity, aABB) ? worldBorder.asVoxelShape() : null;
	}
}
