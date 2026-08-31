package com.temog.notminelegacy.compat;

import net.minecraftforge.fml.ModList;

/**
 * Shared NML-facing constant for Quark's own donkey/mule chest NBT format.
 *
 * <p>Both OverdueOverhauls (native chest management: fresh equip, shears-removal,
 * swap) and BuildingBridges (external variant-chest rendering) need to agree on
 * this exact key — it's Quark's own external storage format, not owned by either —
 * but neither module may import the other directly, so it lives here instead.
 * Both read {@link #QUARK_DONK_CHEST_KEY} from NML; neither reads it from the
 * other's jar.
 */
public final class DonkeyChestBridge {

    /** Quark's own persistent-NBT key holding its serialized variant chest ItemStack. */
    public static final String QUARK_DONK_CHEST_KEY = "Quark:DonkChest";

    private DonkeyChestBridge() {}

    /**
     * True when this bridge is worth having: at least one of Quark (the mod that
     * actually writes the key), BuildingBridges (renders it), or OverdueOverhauls
     * (equips/shears/swaps it) is loaded. False only when all three are absent —
     * in that case nothing in the project will ever read or write this key.
     */
    public static boolean isActive() {
        ModList modList = ModList.get();
        return modList.isLoaded("quark")
                || modList.isLoaded("buildingbridges")
                || modList.isLoaded("overdueoverhauls");
    }
}
