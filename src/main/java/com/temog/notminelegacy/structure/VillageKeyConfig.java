package com.temog.notminelegacy.structure;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Config gate for the NML village key system.
 *
 * <p>TOML path (in notminelegacy-common.toml):
 * <pre>
 *   [structures.villageKeys]
 *     enabled = true   # master switch
 *     debug   = false  # extra logging on key creation / entity tagging
 * </pre>
 *
 * <p>Add-on modules (e.g. Tidal Accord) should gate their own registration calls on
 * {@link VillageKeyService#isVillageKeysEnabled()} rather than reading this config directly.
 */
public final class VillageKeyConfig {

    private static Pair<Common, ForgeConfigSpec> PAIR;
    public static ForgeConfigSpec SPEC;
    public static Common COMMON;

    private VillageKeyConfig() {}

    @SuppressWarnings("removal")
    public static void register() {
        if (SPEC == null) {
            PAIR = new ForgeConfigSpec.Builder().configure(Common::new);
            SPEC  = PAIR.getRight();
            COMMON = PAIR.getLeft();
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static class Common {

        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.BooleanValue debug;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("NML per-instance structure / village UUID tracking")
                   .push("structures");
            builder.push("villageKeys");

            enabled = builder
                    .comment("Master switch for the village key system.",
                             "When false, all registry operations, entity tagging, and structure lookups are skipped.")
                    .define("enabled", true);

            debug = builder
                    .comment("Log extra debug info when village keys are created or assigned to entities.",
                             "Has no effect when 'enabled' is false.")
                    .define("debug", false);

            builder.pop(); // villageKeys
            builder.pop(); // structures
        }
    }

    public static boolean isEnabled() {
        return COMMON != null && COMMON.enabled.get();
    }

    public static boolean isDebug() {
        return isEnabled() && COMMON.debug.get();
    }
}
