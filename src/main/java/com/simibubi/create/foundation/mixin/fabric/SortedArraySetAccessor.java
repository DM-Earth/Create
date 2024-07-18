package com.simibubi.create.foundation.mixin.fabric;

import java.util.Comparator;
import net.minecraft.util.collection.SortedArraySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SortedArraySet.class)
public interface SortedArraySetAccessor<T> {
	@Accessor("elements")
	void create$setElements(T[] contents);

	@Accessor("elements")
	T[] create$getElements();

	@Accessor("comparator")
	Comparator<T> create$getComparator();

	@Accessor("size")
	void create$setSize(int size);

	@Invoker("remove")
	void create$callRemove(int index);
}
