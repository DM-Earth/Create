package com.simibubi.create.foundation.ponder;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public class PonderWorldParticles {

	private final Map<ParticleTextureSheet, Queue<Particle>> byType = Maps.newIdentityHashMap();
	private final Queue<Particle> queue = Queues.newArrayDeque();

	PonderWorld world;

	public PonderWorldParticles(PonderWorld world) {
		this.world = world;
	}

	public void addParticle(Particle p) {
		this.queue.add(p);
	}

	public void tick() {
		this.byType.forEach((p_228347_1_, p_228347_2_) -> this.tickParticleList(p_228347_2_));

		Particle particle;
		if (queue.isEmpty())
			return;
		while ((particle = this.queue.poll()) != null)
			this.byType.computeIfAbsent(particle.getType(), $ -> EvictingQueue.create(16384))
				.add(particle);
	}

	private void tickParticleList(Collection<Particle> p_187240_1_) {
		if (p_187240_1_.isEmpty())
			return;

		Iterator<Particle> iterator = p_187240_1_.iterator();
		while (iterator.hasNext()) {
			Particle particle = iterator.next();
			particle.tick();
			if (!particle.isAlive())
				iterator.remove();
		}
	}

	public void renderParticles(MatrixStack ms, VertexConsumerProvider buffer, Camera renderInfo, float pt) {
		MinecraftClient mc = MinecraftClient.getInstance();
		LightmapTextureManager lightTexture = mc.gameRenderer.getLightmapTextureManager();

		lightTexture.enable();
		RenderSystem.enableDepthTest();
		MatrixStack posestack = RenderSystem.getModelViewStack();
		posestack.push();
		posestack.multiplyPositionMatrix(ms.peek().getPositionMatrix());
		RenderSystem.applyModelViewMatrix();

		for (ParticleTextureSheet iparticlerendertype : this.byType.keySet()) {
			if (iparticlerendertype == ParticleTextureSheet.NO_RENDER)
				continue;
			Iterable<Particle> iterable = this.byType.get(iparticlerendertype);
			if (iterable != null) {
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.setShader(GameRenderer::getParticleProgram);

				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder bufferbuilder = tessellator.getBuffer();
				iparticlerendertype.begin(bufferbuilder, mc.getTextureManager());

				for (Particle particle : iterable)
					particle.buildGeometry(bufferbuilder, renderInfo, pt);

				iparticlerendertype.draw(tessellator);
			}
		}

		posestack.pop();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		lightTexture.disable();
	}

	public void clearEffects() {
		this.byType.clear();
	}

}
