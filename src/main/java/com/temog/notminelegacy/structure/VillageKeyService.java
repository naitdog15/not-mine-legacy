package com.temog.notminelegacy.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Public API for the NML village key system.
 *
 * <p>All methods are safe to call unconditionally; they no-op gracefully when the system is
 * disabled via {@code structures.villageKeys.enabled = false} in {@code notminelegacy-common.toml}.
 *
 * <h2>Integration guide for Tidal Accord (Stage 2)</h2>
 * <ol>
 *   <li><b>Startup</b> — in your module's mod-bus subscriber register the Sea Dwellers
 *       village structure so the system tracks it:
 *       <pre>VillageKeyService.registerTrackedStructure(
 *           new ResourceLocation("seadwellers", "sea_village"));</pre></li>
 *   <li><b>Structure generation</b> — in {@code SeaVillagePiece.generatePiece()} (or wherever
 *       the origin is first known) create or retrieve the village's UUID:
 *       <pre>UUID key = VillageKeyService.getOrCreateVillageKey(
 *           serverLevel, structureId, originPos);</pre></li>
 *   <li><b>Entity spawn</b> — after each mermorph is spawned for the village:
 *       <pre>VillageKeyService.setEntityVillageKey(mermorph, key);</pre></li>
 *   <li><b>Runtime lookup</b> — anywhere you currently read
 *       {@code tidalaccord_generated_village_origin_*} from entity NBT, prefer:
 *       <pre>Optional&lt;UUID&gt; key = VillageKeyService.getEntityVillageKey(entity);</pre>
 *       then resolve full origin via {@link #getVillageKeyEntry} if needed.</li>
 * </ol>
 *
 * <p>The {@code tidalaccord_generated_village_origin_*} NBT tags can remain on entities for
 * backwards compatibility; they are orthogonal to the village key UUID stored under
 * {@code nml_village_key}.
 */
public final class VillageKeyService {

    private static final Logger LOGGER = LogUtils.getLogger();

    // NBT key written to entity's PersistentData; survives save/load via IntArray UUID encoding.
    static final String ENTITY_VILLAGE_KEY_NBT = "nml_village_key";

    // In-memory set of structure IDs that modules have opted into tracking.
    // Re-populated each launch (not persisted); add-ons register at mod-init time.
    private static final Set<ResourceLocation> TRACKED_STRUCTURES = new HashSet<>();

    private VillageKeyService() {}

    // ── Structure registration ────────────────────────────────────────────────

    /**
     * Register a structure ID for village key tracking.
     * Call this from your module's mod-bus subscriber at startup.
     * Safe to call even when the village key system is disabled (it stores the ID regardless,
     * in case the config changes at a later launch).
     */
    public static void registerTrackedStructure(ResourceLocation structureId) {
        if (structureId != null) {
            TRACKED_STRUCTURES.add(structureId);
        }
    }

    public static boolean isTrackedStructure(@Nullable ResourceLocation structureId) {
        return structureId != null && TRACKED_STRUCTURES.contains(structureId);
    }

    // ── Key creation ─────────────────────────────────────────────────────────

    /**
     * Retrieve the village key for the given origin, creating and persisting one if absent.
     * Safe to call on structure generation OR on first entity spawn — the call is idempotent.
     *
     * <p>Returns a random UUID (not persisted) when the system is disabled; callers should
     * discard that value or check {@link #isVillageKeysEnabled()} first.
     */
    public static UUID getOrCreateVillageKey(ServerLevel level, ResourceLocation structureId, BlockPos origin) {
        if (!VillageKeyConfig.isEnabled()) {
            return UUID.randomUUID();
        }

        VillageKeyRegistry registry = VillageKeyRegistry.get(level);
        Optional<UUID> existing = registry.findByOrigin(origin);
        if (existing.isPresent()) {
            return existing.get();
        }

        String dimensionId = level.dimension().location().toString();
        UUID key = registry.register(dimensionId, structureId, origin, level.getGameTime());

        if (VillageKeyConfig.isDebug()) {
            LOGGER.debug("[NML VillageKey] Created key {} for structure {} at {} in {}",
                    key, structureId, origin, dimensionId);
        }
        return key;
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public static boolean isVillageKeysEnabled() {
        return VillageKeyConfig.isEnabled();
    }

    /**
     * Look up the village key whose origin chunk contains {@code pos}.
     * O(1) — backed by the chunk-position secondary index in {@link VillageKeyRegistry}.
     */
    public static Optional<UUID> getVillageKey(ServerLevel level, BlockPos pos) {
        if (!VillageKeyConfig.isEnabled()) return Optional.empty();
        return VillageKeyRegistry.get(level).findByChunk(new ChunkPos(pos));
    }

    public static Optional<UUID> getVillageKey(ServerLevel level, ChunkPos chunkPos) {
        if (!VillageKeyConfig.isEnabled()) return Optional.empty();
        return VillageKeyRegistry.get(level).findByChunk(chunkPos);
    }

    /** Resolve full entry data (origin, structureId, dimension, creation time) for a known key. */
    public static Optional<VillageKeyEntry> getVillageKeyEntry(ServerLevel level, UUID villageKey) {
        if (!VillageKeyConfig.isEnabled()) return Optional.empty();
        return VillageKeyRegistry.get(level).findByUuid(villageKey);
    }

    // ── Entity affiliation ────────────────────────────────────────────────────

    /**
     * Read the village key UUID from an entity's persistent NBT data.
     * Returns empty if the entity has never been tagged or if the key was cleared.
     */
    public static Optional<UUID> getEntityVillageKey(Entity entity) {
        if (entity == null) return Optional.empty();
        CompoundTag data = entity.getPersistentData();
        if (!data.hasUUID(ENTITY_VILLAGE_KEY_NBT)) return Optional.empty();
        return Optional.of(data.getUUID(ENTITY_VILLAGE_KEY_NBT));
    }

    /**
     * Tag an entity with a village key UUID. The value is written to the entity's persistent
     * NBT and survives save/load automatically.
     * No-op when the system is disabled or when either argument is null.
     */
    public static void setEntityVillageKey(Entity entity, UUID villageKey) {
        if (entity == null || villageKey == null) return;
        if (!VillageKeyConfig.isEnabled()) return;
        entity.getPersistentData().putUUID(ENTITY_VILLAGE_KEY_NBT, villageKey);
        if (VillageKeyConfig.isDebug()) {
            LOGGER.debug("[NML VillageKey] Tagged entity {} ({}) with key {}",
                    entity.getUUID(), entity.getType().getDescriptionId(), villageKey);
        }
    }

    /**
     * Remove the village key tag from an entity.  No-op if the entity was not tagged.
     */
    public static void clearEntityVillageKey(Entity entity) {
        if (entity != null) {
            entity.getPersistentData().remove(ENTITY_VILLAGE_KEY_NBT);
        }
    }
}
