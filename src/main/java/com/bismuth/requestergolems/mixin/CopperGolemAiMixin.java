package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.ai.RequesterTransportBehavior;
import com.mojang.datafixers.util.Pair;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.animal.golem.CopperGolemAi")
public abstract class CopperGolemAiMixin {
	@Inject(method = "initCoreActivity", at = @At("RETURN"), cancellable = true)
	private static void requestergolems$addRequesterBehavior(
			CallbackInfoReturnable<ActivityData<CopperGolem>> cir
	) {
		ActivityData<CopperGolem> original = cir.getReturnValue();
		ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super CopperGolem>>> behaviors =
				ImmutableList.builder();
		behaviors.addAll(original.behaviorPriorityPairs());
		behaviors.add(Pair.of(0, new RequesterTransportBehavior()));

		cir.setReturnValue(new ActivityData<>(
				original.activityType(),
				behaviors.build(),
				original.conditions(),
				original.memoriesToEraseWhenStopped()
		));
	}
}
