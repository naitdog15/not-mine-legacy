package com.temog.notminelegacy.compat;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shared bridge that lets Overdue Overhauls talk to Sea Dwellers guidance
 * providers without depending on Tidal Accord classes directly.
 */
public final class SeaDwellersGuidanceBridge {

    @Nullable
    private static volatile Provider provider;

    private SeaDwellersGuidanceBridge() {
    }

    public static void registerProvider(Provider newProvider) {
        provider = newProvider;
    }

    public static boolean isProviderRegistered() {
        return provider != null;
    }

    public static boolean hasFallbackTargets() {
        Provider currentProvider = provider;
        return currentProvider != null && currentProvider.hasFallbackTargets();
    }

    @Nullable
    public static LivingEntity findNearestEntityFallbackTarget(ServerLevel serverLevel, BlockPos origin, int searchRadiusBlocks) {
        Provider currentProvider = provider;
        return currentProvider != null
                ? currentProvider.findNearestEntityFallbackTarget(serverLevel, origin, searchRadiusBlocks)
                : null;
    }

    @Nullable
    public static BlockPos findNearestStructureFallbackTarget(ServerLevel serverLevel, BlockPos origin, int searchRadiusBlocks) {
        Provider currentProvider = provider;
        return currentProvider != null
                ? currentProvider.findNearestStructureFallbackTarget(serverLevel, origin, searchRadiusBlocks)
                : null;
    }

    @Nullable
    public static BlockPos findNearestVillageViaRegistry(ServerLevel serverLevel, BlockPos origin, int searchRadiusChunks) {
        Provider currentProvider = provider;
        return currentProvider != null
                ? currentProvider.findNearestVillageViaRegistry(serverLevel, origin, searchRadiusChunks)
                : null;
    }

    public static boolean isStructureTargetStillValid(ServerLevel serverLevel, BlockPos targetPos) {
        Provider currentProvider = provider;
        return currentProvider != null && currentProvider.isStructureTargetStillValid(serverLevel, targetPos);
    }

    public static boolean isEntityFallbackTarget(Entity entity) {
        Provider currentProvider = provider;
        return currentProvider != null && currentProvider.isEntityFallbackTarget(entity);
    }

    public interface Provider {
        boolean hasFallbackTargets();

        @Nullable
        LivingEntity findNearestEntityFallbackTarget(ServerLevel serverLevel, BlockPos origin, int searchRadiusBlocks);

        @Nullable
        BlockPos findNearestStructureFallbackTarget(ServerLevel serverLevel, BlockPos origin, int searchRadiusBlocks);

        @Nullable
        BlockPos findNearestVillageViaRegistry(ServerLevel serverLevel, BlockPos origin, int searchRadiusChunks);

        boolean isStructureTargetStillValid(ServerLevel serverLevel, BlockPos targetPos);

        boolean isEntityFallbackTarget(Entity entity);
    }
}