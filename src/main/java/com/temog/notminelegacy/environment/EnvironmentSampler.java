package com.temog.notminelegacy.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Stateless sampler — reads the physical surroundings at a position. Uses tag-based
 * {@code getFluidState(pos).is(FluidTags.LAVA/WATER)} rather than the entity convenience methods
 * ({@code Entity#isInLava()}/{@code isUnderWater()}), so a modded fluid tagged into
 * {@code #minecraft:lava}/{@code #minecraft:water} is picked up automatically.
 *
 * <p>Entity state is passed in by the caller: {@code onFire} should come from
 * {@code Entity#isOnFire()}, which is preferred over a raw {@code getRemainingFireTicks() > 0}
 * because it also honours fireImmune(), and is the same predicate vanilla uses for the flame
 * overlay. Call it server-side so it reads the authoritative fire ticks, never the client shared
 * flag.
 */
public final class EnvironmentSampler {

    private EnvironmentSampler() {}

    public static EnvironmentSample sample(Level level, BlockPos pos, boolean onFire, int ageTicks) {
        boolean inLava = level.getFluidState(pos).is(FluidTags.LAVA);
        boolean inWater = level.getFluidState(pos).is(FluidTags.WATER);
        Holder<Biome> biome = level.getBiome(pos);
        float temp = biome.value().getBaseTemperature();
        boolean sky = level.canSeeSky(pos);
        return new EnvironmentSample(inLava, inWater, onFire, temp, sky, biome, ageTicks);
    }
}
