package com.simibubi.create.content.contraptions.behaviour.dispenser;

import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@MethodsReturnNonnullByDefault
public class ContraptionBlockSource implements BlockPointer {
	private final BlockPos pos;
	private final MovementContext context;
	private final Direction overrideFacing;

	public ContraptionBlockSource(MovementContext context, BlockPos pos) {
		this(context, pos, null);
	}

	public ContraptionBlockSource(MovementContext context, BlockPos pos, @Nullable Direction overrideFacing) {
		this.pos = pos;
		this.context = context;
		this.overrideFacing = overrideFacing;
	}

	@Override
	public double getX() {
		return (double)this.pos.getX() + 0.5D;
	}

	@Override
	public double getY() {
		return (double)this.pos.getY() + 0.5D;
	}

	@Override
	public double getZ() {
		return (double)this.pos.getZ() + 0.5D;
	}

	@Override
	public BlockPos getPos() {
		return pos;
	}

	@Override
	public BlockState getBlockState() {
		if (context.state.contains(Properties.FACING) && overrideFacing != null)
			return context.state.with(Properties.FACING, overrideFacing);
		return context.state;
	}

	@Override
	@Nullable
	public <T extends BlockEntity> T getBlockEntity() {
		return null;
	}

	@Override
	@Nullable
	public ServerWorld getWorld() {
		MinecraftServer server = context.world.getServer();
		return server != null ? server.getWorld(context.world.getRegistryKey()) : null;
	}
}
