package com.simibubi.create.content.fluids.particle;

import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.foundation.utility.Color;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRenderHandler;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;

public class FluidStackParticle extends SpriteBillboardParticle {
	private final float uo;
	private final float vo;
	private FluidStack fluid;
//	private IClientFluidTypeExtensions clientFluid; // fabric: replaced with FluidVariantRendering

	public static FluidStackParticle create(ParticleType<FluidParticleData> type, ClientWorld world, FluidStack fluid,
		double x, double y, double z, double vx, double vy, double vz) {
		if (type == AllParticleTypes.BASIN_FLUID.get())
			return new BasinFluidParticle(world, fluid, x, y, z, vx, vy, vz);
		return new FluidStackParticle(world, fluid, x, y, z, vx, vy, vz);
	}

	@SuppressWarnings("UnstableApiUsage")
	public FluidStackParticle(ClientWorld world, FluidStack fluid, double x, double y, double z, double vx, double vy,
							  double vz) {
		super(world, x, y, z, vx, vy, vz);

		this.fluid = fluid;
		FluidVariantRenderHandler handler = FluidVariantRendering.getHandlerOrDefault(fluid.getFluid());
		this.setSprite(handler.getSprites(fluid.getType())[0]);

		this.gravityStrength = 1.0F;
		this.red = 0.8F;
		this.green = 0.8F;
		this.blue = 0.8F;
		int color = handler.getColor(fluid.getType(), world, BlockPos.ofFloored(x, y, z));
		multiplyColor(color);

		this.velocityX = vx;
		this.velocityY = vy;
		this.velocityZ = vz;

		this.scale /= 2.0F;
		this.uo = this.random.nextFloat() * 3.0F;
		this.vo = this.random.nextFloat() * 3.0F;
	}

	@Override
	protected int getBrightness(float p_189214_1_) {
		int brightnessForRender = super.getBrightness(p_189214_1_);
		int skyLight = brightnessForRender >> 20;
		int blockLight = (brightnessForRender >> 4) & 0xf;
		blockLight = Math.max(blockLight, FluidVariantAttributes.getLuminance(fluid.getType()));
		return (skyLight << 20) | (blockLight << 4);
	}

	protected void multiplyColor(int color) {
		this.red *= (float) (color >> 16 & 255) / 255.0F;
		this.green *= (float) (color >> 8 & 255) / 255.0F;
		this.blue *= (float) (color & 255) / 255.0F;
	}

	protected float getMinU() {
		return this.sprite.getFrameU((double) ((this.uo + 1.0F) / 4.0F * 16.0F));
	}

	protected float getMaxU() {
		return this.sprite.getFrameU((double) (this.uo / 4.0F * 16.0F));
	}

	protected float getMinV() {
		return this.sprite.getFrameV((double) (this.vo / 4.0F * 16.0F));
	}

	protected float getMaxV() {
		return this.sprite.getFrameV((double) ((this.vo + 1.0F) / 4.0F * 16.0F));
	}

	@Override
	public void tick() {
		super.tick();
		if (!canEvaporate())
			return;
		if (onGround)
			markDead();
		if (!dead)
			return;
		if (!onGround && world.random.nextFloat() < 1 / 8f)
			return;

		Color color = new Color(red, green, blue, 1);
		world.addParticle(ParticleTypes.ENTITY_EFFECT, x, y, z, color.getRedAsFloat(), color.getGreenAsFloat(),
			color.getBlueAsFloat());
	}

	protected boolean canEvaporate() {
		return fluid.getFluid() instanceof PotionFluid;
	}

	@Override
	public ParticleTextureSheet getType() {
		return ParticleTextureSheet.TERRAIN_SHEET;
	}

}
