package com.temog.notminelegacy.compat;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Mod-presence check safe to call from {@code IMixinConfigPlugin#shouldApplyMixin}, which runs
 * extremely early in the Forge/Mixin bootstrap — before {@link ModList#get()} is guaranteed to
 * be populated. Falls back to the internal {@code LoadingModList} via reflection when the public
 * API isn't ready yet.
 *
 * <p>Lives in core ({@code notminelegacy}) — not inside any package module — because every
 * module's mixin plugin needs to gate mixins on other modules' presence, and mixin plugins run
 * too early to safely take a hard dependency on another package module's classes being
 * classloadable. Core is the one module every package may require, per the inter-module
 * dependency rule in {@code CLAUDE.md}.
 *
 * <p>Keep this class dependency-free beyond Forge's own {@code ModList} / {@code LoadingModList}
 * — do not add other imports here, and do not "improve" the early-safe fallback logic; it is a
 * faithful copy of the semantics this helper has always had.
 */
public final class EarlyModCheck {
    private EarlyModCheck() {}

    /**
     * Checks whether {@code modId} is present at mixin-apply time.
     * ModList may not yet be fully initialised at this point, so we fall back
     * to the internal LoadingModList via reflection when the public API returns null.
     */
    public static boolean isModLoadedEarlySafe(String modId) {
        ModList modList = ModList.get();
        if (modList != null) {
            return modList.isLoaded(modId);
        }

        try {
            Class<?> loadingModListClass = Class.forName("net.minecraftforge.fml.loading.LoadingModList");
            Method getMethod = loadingModListClass.getMethod("get");
            Object loadingModList = getMethod.invoke(null);
            if (loadingModList != null) {
                Method getModFileByIdMethod = loadingModListClass.getMethod("getModFileById", String.class);
                Object modFile = getModFileByIdMethod.invoke(loadingModList, modId);
                return modFile != null;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }
}
