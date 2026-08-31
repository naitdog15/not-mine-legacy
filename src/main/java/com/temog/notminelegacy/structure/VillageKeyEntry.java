package com.temog.notminelegacy.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Immutable record of a single tracked village/structure instance.
 *
 * <p>Created once per unique structure origin and stored by {@link VillageKeyRegistry}.
 * Retrieve via {@link VillageKeyService#getVillageKeyEntry}.
 *
 * <h2>Tidal Accord usage (Stage 2)</h2>
 * When {@code SeaVillagePiece} places a structure, TA calls:
 * <pre>
 *   UUID key = VillageKeyService.getOrCreateVillageKey(level, seaVillageStructureId, originPos);
 * </pre>
 * That call creates and persists one {@code VillageKeyEntry} for the village. TA then tags each
 * spawned mermorph with {@code VillageKeyService.setEntityVillageKey(mermorph, key)}.
 */
public final class VillageKeyEntry {

    private static final String NBT_KEY         = "VillageKey";
    private static final String NBT_DIMENSION   = "Dimension";
    private static final String NBT_STRUCTURE   = "StructureId";
    private static final String NBT_ORIGIN_X    = "OriginX";
    private static final String NBT_ORIGIN_Y    = "OriginY";
    private static final String NBT_ORIGIN_Z    = "OriginZ";
    private static final String NBT_CREATED     = "CreatedGameTime";

    private final UUID             villageKey;
    private final String           dimensionId;
    @Nullable
    private final ResourceLocation structureId;
    private final BlockPos         origin;
    private final long             createdGameTime;

    VillageKeyEntry(UUID villageKey, String dimensionId,
                    @Nullable ResourceLocation structureId,
                    BlockPos origin, long createdGameTime) {
        this.villageKey      = villageKey;
        this.dimensionId     = dimensionId;
        this.structureId     = structureId;
        this.origin          = origin.immutable();
        this.createdGameTime = createdGameTime;
    }

    public UUID             villageKey()       { return villageKey; }
    public String           dimensionId()      { return dimensionId; }
    @Nullable
    public ResourceLocation structureId()      { return structureId; }
    public BlockPos         origin()           { return origin; }
    public ChunkPos         chunkPos()         { return new ChunkPos(origin); }
    public long             createdGameTime()  { return createdGameTime; }

    // ── Serialisation (package-private: only VillageKeyRegistry needs these) ─

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(NBT_KEY, villageKey);
        tag.putString(NBT_DIMENSION, dimensionId);
        if (structureId != null) {
            tag.putString(NBT_STRUCTURE, structureId.toString());
        }
        tag.putInt(NBT_ORIGIN_X, origin.getX());
        tag.putInt(NBT_ORIGIN_Y, origin.getY());
        tag.putInt(NBT_ORIGIN_Z, origin.getZ());
        tag.putLong(NBT_CREATED, createdGameTime);
        return tag;
    }

    static VillageKeyEntry load(CompoundTag tag) {
        UUID             villageKey      = tag.getUUID(NBT_KEY);
        String           dimensionId     = tag.getString(NBT_DIMENSION);
        ResourceLocation structureId     = null;
        if (tag.contains(NBT_STRUCTURE)) {
            structureId = ResourceLocation.tryParse(tag.getString(NBT_STRUCTURE));
        }
        BlockPos origin = new BlockPos(
                tag.getInt(NBT_ORIGIN_X),
                tag.getInt(NBT_ORIGIN_Y),
                tag.getInt(NBT_ORIGIN_Z));
        long createdGameTime = tag.getLong(NBT_CREATED);
        return new VillageKeyEntry(villageKey, dimensionId, structureId, origin, createdGameTime);
    }
}
