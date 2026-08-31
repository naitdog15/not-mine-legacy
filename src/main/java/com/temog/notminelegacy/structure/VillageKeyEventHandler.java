package com.temog.notminelegacy.structure;

import com.mojang.logging.LogUtils;
import com.temog.notminelegacy.NotMineLegacy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Forge event integration for the NML village key system.
 *
 * <h2>Current responsibilities</h2>
 * <ul>
 *   <li>Entity join: validates that an entity's stored village key still exists in the world
 *       registry and logs it when debug mode is on.  No structural changes are made here;
 *       the key was already written at spawn time by the add-on module (e.g. TA).</li>
 * </ul>
 *
 * <h2>Structure-generation hooks</h2>
 * Village keys are created by add-on modules calling
 * {@link VillageKeyService#getOrCreateVillageKey} directly from their structure piece code,
 * not via a generic Forge structure event.  This is because:
 * <ol>
 *   <li>There is no reliable per-piece post-placement event in Forge 1.20.1.</li>
 *   <li>The add-on module already knows the origin when its piece is placed.</li>
 *   <li>The fallback "create on first lookup" behaviour in
 *       {@link VillageKeyService#getOrCreateVillageKey} handles existing worlds gracefully.</li>
 * </ol>
 *
 * <h2>Tidal Accord integration (Stage 2)</h2>
 * TA's {@code SeaVillagePiece.generatePiece()} should call:
 * <pre>
 *   UUID key = VillageKeyService.getOrCreateVillageKey(serverLevel, structureId, originPos);
 * </pre>
 * and TA's spawn code (currently in {@code SeaVillagePopulationHandler}) should call:
 * <pre>
 *   VillageKeyService.setEntityVillageKey(mermorph, key);
 * </pre>
 * immediately after the mermorph is added to the level.  From that point on, TA can replace
 * reads of {@code tidalaccord_generated_village_origin_*} with:
 * <pre>
 *   VillageKeyService.getEntityVillageKey(entity)
 *       .flatMap(k -> VillageKeyService.getVillageKeyEntry(serverLevel, k))
 *       .map(VillageKeyEntry::origin);
 * </pre>
 */
@Mod.EventBusSubscriber(modid = NotMineLegacy.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VillageKeyEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private VillageKeyEventHandler() {}

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!VillageKeyConfig.isEnabled()) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        Entity entity = event.getEntity();
        Optional<UUID> key = VillageKeyService.getEntityVillageKey(entity);
        if (key.isEmpty()) return;

        if (VillageKeyConfig.isDebug()) {
            boolean known = VillageKeyRegistry.get(serverLevel).findByUuid(key.get()).isPresent();
            LOGGER.debug("[NML VillageKey] Entity {} ({}) joined with key {} (registry: {})",
                    entity.getUUID(),
                    entity.getType().getDescriptionId(),
                    key.get(),
                    known ? "known" : "UNKNOWN — village may be in another dimension or pre-dates registry");
        }
    }
}
