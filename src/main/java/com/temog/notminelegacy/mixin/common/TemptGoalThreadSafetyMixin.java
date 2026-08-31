package com.temog.notminelegacy.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fixes a crash caused by Quark's FeedingTroughModule accessing
 * LegacyRandomSource from multiple threads during TemptGoal.canUse().
 * <p>
 * Quark's TemptGoalMixin injects into canUse() and calls level.random.nextFloat(),
 * which can race with worldgen threads using the same RandomSource, triggering
 * {@code IllegalStateException: Accessing LegacyRandomSource from multiple threads}.
 * <p>
 * This mixin wraps the {@code goal.canUse()} delegation in {@link WrappedGoal}
 * with a try-catch. If the threading exception fires, the goal simply skips
 * that tick instead of crashing the server.
 */
@Mixin(WrappedGoal.class)
public abstract class TemptGoalThreadSafetyMixin {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean loggedOnce = false;

    @WrapOperation(
        method = "canUse",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/Goal;canUse()Z")
    )
    private boolean notMineLegacy$catchRandomSourceThreading(Goal goal, Operation<Boolean> original) {
        try {
            return original.call(goal);
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("LegacyRandomSource")) {
                if (!loggedOnce) {
                    LOGGER.warn("[NotMineLegacy] Caught LegacyRandomSource threading crash in {} — skipping tick (this message logs once)", goal.getClass().getSimpleName());
                    loggedOnce = true;
                }
                return false;
            }
            throw e; // re-throw if not the threading issue
        }
    }
}
