package com.simibubi.create.content.kinetics.drill;

import javax.annotation.Nullable;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorInstance;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DrillMovementBehaviour extends BlockBreakingMovementBehaviour {

	@Override
	public boolean isActive(MovementContext context) {
		return super.isActive(context)
			&& !VecHelper.isVecPointingTowards(context.relativeMotion, context.state.get(DrillBlock.FACING)
				.getOpposite());
	}

	@Override
	public Vec3d getActiveAreaOffset(MovementContext context) {
		return Vec3d.of(context.state.get(DrillBlock.FACING)
			.getVector()).multiply(.65f);
	}

	@Override
	@Environment(value = EnvType.CLIENT)
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffer) {
        if (!ContraptionRenderDispatcher.canInstance())
			DrillRenderer.renderInContraption(context, renderWorld, matrices, buffer);
	}

	@Override
	public boolean hasSpecialInstancedRendering() {
		return true;
	}

	@Nullable
	@Override
	public ActorInstance createInstance(MaterialManager materialManager, VirtualRenderWorld simulationWorld, MovementContext context) {
		return new DrillActorInstance(materialManager, simulationWorld, context);
	}

	@Override
	protected DamageSource getDamageSource(World level) {
		return CreateDamageSources.drill(level);
	}

	@Override
	public boolean canBreak(World world, BlockPos breakingPos, BlockState state) {
		return super.canBreak(world, breakingPos, state) && !state.getCollisionShape(world, breakingPos)
			.isEmpty() && !AllTags.AllBlockTags.TRACKS.matches(state);
	}

}
