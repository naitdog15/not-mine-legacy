package com.temog.notminelegacy.compat;

import net.minecraftforge.fml.ModList;

/**
 * Central mod-compatibility detection utility shared across NML submodules.
 *
 * <p>Only broad, cross-module mod presence checks belong here. Module-specific
 * compat adapters should remain inside their own feature packages so subparts
 * can stay independent.
 */
public final class ModCompat {
    private ModCompat() {}

    public static final String NML_MODID = "notminelegacy";
    public static final String CURIOS_MODID = "curios";
    public static final String AETHER_MODID = "aether";
    public static final String QUARK_MODID = "quark";
    public static final String VB_MODID = "vanillabackport";
    public static final String CITADEL_MODID = "citadel";
    public static final String TIDAL_ACCORD_MODID = "tidalaccord";
    public static final String SEA_DWELLERS_MODID = "seadwellers";
    public static final String DEEPER_OCEANS_MODID = "deeper_oceans";
    public static final String REGIONS_UNEXPLORED_MODID = "regions_unexplored";
    public static final String TINKERS_CONSTRUCT_MODID = "tconstruct";
    public static final String CONCORD_MODID = "nml_concord";

    private static Boolean nmlLoaded;
    private static Boolean curiosLoaded;
    private static Boolean aetherLoaded;
    private static Boolean quarkLoaded;
    private static Boolean vbLoaded;
    private static Boolean citadelLoaded;
    private static Boolean tidalAccordLoaded;
    private static Boolean seaDwellersLoaded;
    private static Boolean deeperOceansLoaded;
    private static Boolean regionsUnexploredLoaded;
    private static Boolean tinkersConstructLoaded;
    private static Boolean concordLoaded;

    public static boolean isNmlLoaded() {
        if (nmlLoaded == null) nmlLoaded = ModList.get().isLoaded(NML_MODID);
        return nmlLoaded;
    }

    public static boolean isCuriosLoaded() {
        if (curiosLoaded == null) curiosLoaded = ModList.get().isLoaded(CURIOS_MODID);
        return curiosLoaded;
    }

    public static boolean isAetherLoaded() {
        if (aetherLoaded == null) aetherLoaded = ModList.get().isLoaded(AETHER_MODID);
        return aetherLoaded;
    }

    public static boolean isQuarkLoaded() {
        if (quarkLoaded == null) quarkLoaded = ModList.get().isLoaded(QUARK_MODID);
        return quarkLoaded;
    }

    public static boolean isVBLoaded() {
        if (vbLoaded == null) vbLoaded = ModList.get().isLoaded(VB_MODID);
        return vbLoaded;
    }

    public static boolean isCitadelLoaded() {
        if (citadelLoaded == null) citadelLoaded = ModList.get().isLoaded(CITADEL_MODID);
        return citadelLoaded;
    }

    public static boolean isTidalAccordLoaded() {
        if (tidalAccordLoaded == null) tidalAccordLoaded = ModList.get().isLoaded(TIDAL_ACCORD_MODID);
        return tidalAccordLoaded;
    }

    public static boolean isSeaDwellersLoaded() {
        if (seaDwellersLoaded == null) seaDwellersLoaded = ModList.get().isLoaded(SEA_DWELLERS_MODID);
        return seaDwellersLoaded;
    }

    public static boolean isDeeperOceansLoaded() {
        if (deeperOceansLoaded == null) deeperOceansLoaded = ModList.get().isLoaded(DEEPER_OCEANS_MODID);
        return deeperOceansLoaded;
    }

    public static boolean isRegionsUnexploredLoaded() {
        if (regionsUnexploredLoaded == null) regionsUnexploredLoaded = ModList.get().isLoaded(REGIONS_UNEXPLORED_MODID);
        return regionsUnexploredLoaded;
    }

    public static boolean isTinkersConstructLoaded() {
        if (tinkersConstructLoaded == null) tinkersConstructLoaded = ModList.get().isLoaded(TINKERS_CONSTRUCT_MODID);
        return tinkersConstructLoaded;
    }

    public static boolean isConcordLoaded() {
        if (concordLoaded == null) concordLoaded = ModList.get().isLoaded(CONCORD_MODID);
        return concordLoaded;
    }
}