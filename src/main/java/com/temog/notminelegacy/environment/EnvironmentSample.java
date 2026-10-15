package com.temog.notminelegacy.environment;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * A one-shot sample of the physical conditions at a position: fluid contact, live fire state,
 * ambient biome temperature/identity, sky visibility, and an owner-supplied age. Produced by
 * {@link EnvironmentSampler#sample} and consumed immediately; never stored.
 *
 * <p>{@code onFire} is an entity condition passed in by the caller, deliberately a separate axis
 * from {@code inLava}: lava is a place, fire is a condition the subject carries with it, and the
 * two do not imply each other in either direction.
 */
public record EnvironmentSample(boolean inLava, boolean inWater, boolean onFire,
                                float biomeTemperature, boolean canSeeSky, Holder<Biome> biome,
                                int ageTicks) {}
