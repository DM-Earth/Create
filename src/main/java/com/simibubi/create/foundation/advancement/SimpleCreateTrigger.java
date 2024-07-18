package com.simibubi.create.foundation.advancement;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import com.google.gson.JsonObject;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SimpleCreateTrigger extends CriterionTriggerBase<SimpleCreateTrigger.Instance> {

	public SimpleCreateTrigger(String id) {
		super(id);
	}

	@Override
	public com.simibubi.create.foundation.advancement.SimpleCreateTrigger.Instance conditionsFromJson(JsonObject json, AdvancementEntityPredicateDeserializer context) {
		return new com.simibubi.create.foundation.advancement.SimpleCreateTrigger.Instance(getId());
	}

	public void trigger(ServerPlayerEntity player) {
		super.trigger(player, null);
	}

	public com.simibubi.create.foundation.advancement.SimpleCreateTrigger.Instance instance() {
		return new com.simibubi.create.foundation.advancement.SimpleCreateTrigger.Instance(getId());
	}

	public static class Instance extends CriterionTriggerBase.Instance {

		public Instance(Identifier idIn) {
			super(idIn, LootContextPredicate.EMPTY);
		}

		@Override
		protected boolean test(@Nullable List<Supplier<Object>> suppliers) {
			return true;
		}
	}
}
