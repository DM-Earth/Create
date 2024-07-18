package com.simibubi.create.foundation.mixin.client;

import java.lang.ref.Reference;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.MovementType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.util.TriConsumer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.ContraptionHandler;

import io.github.fabricators_of_create.porting_lib.block.CustomRunningEffectsBlock;

@Mixin(Entity.class)
public abstract class EntityContraptionInteractionMixin {
	@Shadow
	public World world;

	@Shadow
	private Vec3d pos;

	@Shadow
	private float nextStepSoundDistance;

	@Shadow
	@Final
	protected Random random;

	@Shadow
	private EntityDimensions dimensions;

	@Shadow
	protected abstract float calculateNextStepSoundDistance();

	@Shadow
	protected abstract void playStepSound(BlockPos pos, BlockState state);

	@Unique
	private Stream<AbstractContraptionEntity> create$getIntersectionContraptionsStream() {
		return ContraptionHandler.loadedContraptions.get(world)
				.values()
				.stream()
				.map(Reference::get)
				.filter(cEntity -> cEntity != null && cEntity.collidingEntities.containsKey((Entity) (Object) this));
	}

	@Unique
	private Set<AbstractContraptionEntity> create$getIntersectingContraptions() {
		Set<AbstractContraptionEntity> contraptions = create$getIntersectionContraptionsStream().collect(Collectors.toSet());

		contraptions.addAll(world.getNonSpectatingEntities(AbstractContraptionEntity.class, ((Entity) (Object) this).getBoundingBox()
			.expand(1f)));
		return contraptions;
	}

	@Unique
	private void forCollision(Vec3d worldPos, TriConsumer<Contraption, BlockState, BlockPos> action) {
		create$getIntersectingContraptions().forEach(cEntity -> {
			Vec3d localPos = ContraptionCollider.worldToLocalPos(worldPos, cEntity);

			BlockPos blockPos = BlockPos.ofFloored(localPos);
			Contraption contraption = cEntity.getContraption();
			StructureTemplate.StructureBlockInfo info = contraption.getBlocks()
				.get(blockPos);

			if (info != null) {
				BlockState blockstate = info.state();
				action.accept(contraption, blockstate, blockPos);
			}
		});
	}

	// involves block step sounds on contraptions
	// IFNE line 661 injecting before `!blockstate.isAir(this.world, blockpos)`
	@Inject(method = "move", at = @At(value = "JUMP", opcode = Opcodes.IFNE, ordinal = 7))
	private void create$contraptionStepSounds(MovementType mover, Vec3d movement, CallbackInfo ci) {
		Vec3d worldPos = pos.add(0, -0.2, 0);
		MutableBoolean stepped = new MutableBoolean(false);

		forCollision(worldPos, (contraption, state, pos) -> {
			playStepSound(pos, state);
			stepped.setTrue();
		});

		if (stepped.booleanValue())
			nextStepSoundDistance = calculateNextStepSoundDistance();
	}

	// involves client-side view bobbing animation on contraptions
	@Inject(method = "move", at = @At(value = "TAIL"))
	private void create$onMove(MovementType mover, Vec3d movement, CallbackInfo ci) {
		if (!world.isClient)
			return;
		Entity self = (Entity) (Object) this;
		if (self.isOnGround())
			return;
		if (self.hasVehicle())
			return;

		Vec3d worldPos = pos.add(0, -0.2, 0);
		boolean onAtLeastOneContraption = create$getIntersectionContraptionsStream().anyMatch(cEntity -> {
			Vec3d localPos = ContraptionCollider.worldToLocalPos(worldPos, cEntity);

			BlockPos blockPos = BlockPos.ofFloored(localPos);
			Contraption contraption = cEntity.getContraption();
			StructureTemplate.StructureBlockInfo info = contraption.getBlocks()
				.get(blockPos);

			if (info == null)
				return false;

			cEntity.registerColliding(self);
			return true;
		});

		if (!onAtLeastOneContraption)
			return;

		self.setOnGround(true);
		self.getCustomData()
			.putBoolean("ContraptionGrounded", true);
	}

	@Inject(method = "spawnSprintingParticles", at = @At(value = "TAIL"))
	private void create$onSpawnSprintParticle(CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		Vec3d worldPos = pos.add(0, -0.2, 0);
		BlockPos particlePos = BlockPos.ofFloored(worldPos); // pos where particles are spawned

		forCollision(worldPos, (contraption, state, pos) -> {
			boolean particles = state.getRenderType() != BlockRenderType.INVISIBLE;
			if (state.getBlock() instanceof CustomRunningEffectsBlock custom &&
					custom.addRunningEffects(state, self.getWorld(), pos, self)) {
				particles = false;
			}
			if (particles) {
				Vec3d speed = self.getVelocity();
				world.addParticle(
					new BlockStateParticleEffect(ParticleTypes.BLOCK, state).setSourcePos(particlePos),
					self.getX() + ((double) random.nextFloat() - 0.5D) * (double) dimensions.width,
					self.getY() + 0.1D,
					self.getZ() + ((double) random.nextFloat() - 0.5D) * (double) dimensions.height,
					speed.x * -4.0D, 1.5D, speed.z * -4.0D
				);
			}
		});
	}
}
