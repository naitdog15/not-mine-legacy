package com.temog.notminelegacy;

import com.mojang.logging.LogUtils;
import com.temog.notminelegacy.compat.DonkeyChestBridge;
import com.temog.notminelegacy.structure.VillageKeyConfig;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Entry point for the <b>Not Mine Legacy</b> umbrella module.
 * <p>
 * NML provides shared compatibility and service infrastructure used by the
 * feature jars in this workspace, including:
 * <ul>
 *   <li>Cross-module compat detection and lightweight service hooks</li>
 *   <li>Thread-safety fixes for modded AI goals</li>
 *   <li>Dev-environment stability patches for VB config loading</li>
 * </ul>
 * <p>
 * Content-specific features and bridge packs live in their own module jars.
 * NML is the shared tooling layer they depend on.
 */
@Mod(NotMineLegacy.MODID)
public class NotMineLegacy {

    public static final String MODID = "notminelegacy";
    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public NotMineLegacy() {
        VillageKeyConfig.register();
        prewarmExternalWorldgenTypes();
        // VanillaBackport's SpriteResourceLoader mixin triggers VanillaBackport.<clinit>
        // in a ForkJoin worker thread during texture atlas loading — at which point
        // ModLoadingContext.getActiveContainer() throws "Where is minecraft???!" and poisons
        // the class permanently. Force the init here, while the mod loading context IS set,
        // so the <clinit> completes successfully before any worker thread touches the class.
        if (ModList.get().isLoaded("vanillabackport")) {
            prewarmClass("com.blackgear.vanillabackport.core.VanillaBackport");
        }
        logDonkeyChestBridgeStatus();
        LOGGER.info("Not Mine Legacy initialized — shared compat layer active");
    }

    /**
     * The donkey/mule chest NBT-key bridge (Quark's own {@code Quark:DonkChest}
     * format, shared between OverdueOverhauls and BuildingBridges via
     * {@link DonkeyChestBridge} instead of either importing the other) is only
     * worth having when at least one of Quark, BuildingBridges, or
     * OverdueOverhauls is actually loaded — skipped (logged only, nothing to
     * disable) when all three are absent.
     */
    private static void logDonkeyChestBridgeStatus() {
        if (DonkeyChestBridge.isActive()) {
            LOGGER.debug("[NML] Donkey chest NBT-key bridge active");
        } else {
            LOGGER.debug("[NML] Donkey chest NBT-key bridge skipped — Quark, BuildingBridges, and OverdueOverhauls all absent");
        }
    }

    private static void prewarmExternalWorldgenTypes() {
        if (!ModList.get().isLoaded("aeroblender") || !ModList.get().isLoaded("terrablender")) {
            return;
        }

        prewarmClass("terrablender.api.RegionType");
        prewarmClass("teamrazor.aeroblender.aether.AetherRegionType");
    }

    private static void prewarmClass(String className) {
        try {
            Class.forName(className, true, NotMineLegacy.class.getClassLoader());
            LOGGER.debug("Prewarmed class {}", className);
        } catch (ClassNotFoundException ignored) {
            LOGGER.debug("Skipped prewarm for missing class {}", className);
        } catch (Throwable throwable) {
            LOGGER.warn("Prewarm failed for {} (continuing startup)", className, throwable);
        }
    }
}
