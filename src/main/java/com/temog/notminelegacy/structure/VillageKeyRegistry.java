package com.temog.notminelegacy.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * World-persistent registry: maps village instance UUIDs to their {@link VillageKeyEntry} records.
 *
 * <p>Stored once per dimension under {@code <world>/data/notminelegacy_village_keys.dat}.
 * Two secondary indexes (by chunk and by exact origin) provide O(1) lookups so entity AI code
 * can query without scanning all entries on every tick.
 *
 * <p>Use {@link VillageKeyService} for the public-facing API; this class owns raw storage only.
 */
final class VillageKeyRegistry extends SavedData {

    private static final String DATA_NAME   = "notminelegacy_village_keys";
    private static final String NBT_ENTRIES = "Entries";

    // Primary store
    private final Map<UUID, VillageKeyEntry> byUuid   = new HashMap<>();

    // Secondary index: ChunkPos.toLong() → UUID  (one village per chunk origin)
    private final Map<Long, UUID>            byChunk  = new HashMap<>();

    // Secondary index: BlockPos.asLong()  → UUID  (exact origin position)
    private final Map<Long, UUID>            byOrigin = new HashMap<>();

    private VillageKeyRegistry() {}

    private VillageKeyRegistry(CompoundTag tag) {
        ListTag list = tag.getList(NBT_ENTRIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            index(VillageKeyEntry.load(list.getCompound(i)));
        }
    }

    static VillageKeyRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                VillageKeyRegistry::new,   // load from CompoundTag
                VillageKeyRegistry::new,   // create new (no-arg)
                DATA_NAME);
    }

    // ── Registration ─────────────────────────────────────────────────────────

    /**
     * Register a new village instance at {@code origin}, or return the existing UUID if the
     * origin was already registered.  Idempotent: multiple calls for the same origin are safe.
     */
    UUID register(String dimensionId, @Nullable ResourceLocation structureId,
                  BlockPos origin, long gameTime) {
        UUID existing = byOrigin.get(origin.asLong());
        if (existing != null) {
            return existing;
        }

        UUID key   = UUID.randomUUID();
        VillageKeyEntry entry = new VillageKeyEntry(key, dimensionId, structureId, origin, gameTime);
        index(entry);
        setDirty();
        return key;
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    Optional<VillageKeyEntry> findByUuid(UUID uuid) {
        return Optional.ofNullable(byUuid.get(uuid));
    }

    Optional<UUID> findByChunk(ChunkPos chunkPos) {
        return Optional.ofNullable(byChunk.get(chunkPos.toLong()));
    }

    Optional<UUID> findByOrigin(BlockPos origin) {
        return Optional.ofNullable(byOrigin.get(origin.asLong()));
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (VillageKeyEntry entry : byUuid.values()) {
            list.add(entry.save());
        }
        tag.put(NBT_ENTRIES, list);
        return tag;
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void index(VillageKeyEntry entry) {
        byUuid.put(entry.villageKey(), entry);
        byChunk.put(entry.chunkPos().toLong(), entry.villageKey());
        byOrigin.put(entry.origin().asLong(), entry.villageKey());
    }
}
