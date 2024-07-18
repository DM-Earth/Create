package com.simibubi.create.foundation.advancement;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import com.google.common.collect.Maps;
import com.simibubi.create.Create;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CriterionTriggerBase<T extends CriterionTriggerBase.Instance> implements Criterion<T> {

	public CriterionTriggerBase(String id) {
		this.id = Create.asResource(id);
	}

	private final Identifier id;
	protected final Map<PlayerAdvancementTracker, Set<ConditionsContainer<T>>> listeners = Maps.newHashMap();

	@Override
	public void beginTrackingCondition(PlayerAdvancementTracker playerAdvancementsIn, ConditionsContainer<T> listener) {
		Set<ConditionsContainer<T>> playerListeners = this.listeners.computeIfAbsent(playerAdvancementsIn, k -> new HashSet<>());

		playerListeners.add(listener);
	}

	@Override
	public void endTrackingCondition(PlayerAdvancementTracker playerAdvancementsIn, ConditionsContainer<T> listener) {
		Set<ConditionsContainer<T>> playerListeners = this.listeners.get(playerAdvancementsIn);
		if (playerListeners != null) {
			playerListeners.remove(listener);
			if (playerListeners.isEmpty()) {
				this.listeners.remove(playerAdvancementsIn);
			}
		}
	}

	@Override
	public void endTracking(PlayerAdvancementTracker playerAdvancementsIn) {
		this.listeners.remove(playerAdvancementsIn);
	}

	@Override
	public Identifier getId() {
		return id;
	}

	protected void trigger(ServerPlayerEntity player, @Nullable List<Supplier<Object>> suppliers) {
		PlayerAdvancementTracker playerAdvancements = player.getAdvancementTracker();
		Set<ConditionsContainer<T>> playerListeners = this.listeners.get(playerAdvancements);
		if (playerListeners != null) {
			List<ConditionsContainer<T>> list = new LinkedList<>();

			for (ConditionsContainer<T> listener : playerListeners) {
				if (listener.getConditions()
					.test(suppliers)) {
					list.add(listener);
				}
			}

			list.forEach(listener -> listener.grant(playerAdvancements));

		}
	}

	public abstract static class Instance extends AbstractCriterionConditions {

		public Instance(Identifier idIn, LootContextPredicate predicate) {
			super(idIn, predicate);
		}

		protected abstract boolean test(@Nullable List<Supplier<Object>> suppliers);
	}

}
