package com.simibubi.create.foundation.advancement;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.advancement.criterion.Criteria;

public class AllTriggers {

	private static final List<CriterionTriggerBase<?>> triggers = new LinkedList<>();

	public static SimpleCreateTrigger addSimple(String id) {
		return add(new SimpleCreateTrigger(id));
	}

	private static <T extends CriterionTriggerBase<?>> T add(T instance) {
		triggers.add(instance);
		return instance;
	}

	public static void register() {
		triggers.forEach(Criteria::register);
	}

}
