package com.temog.notminelegacy.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dev-environment workaround for Vanilla Backport's {@code MusicFadeManager}.
 * VB's mixin into {@code MusicManager} fires before its {@code ForgeConfigSpec}
 * is loaded; in Forge 47.4+ userdev, that throws {@link IllegalStateException}
 * when {@code isFeatureEnabled()} reads its config value.
 * <p>
 * All four public entry points — {@code onTick(SoundInstance)},
 * {@code preventPlayingInPaleGarden()}, {@code updateVolume(SoundInstance)} and
 * {@code onStartPlaying()} — funnel into {@code isFeatureEnabled()}, so we guard
 * each at HEAD: probe the config via reflection; if it throws, short-circuit. The
 * {@code boolean} returners get {@code Boolean.FALSE}; the {@code void} ones cancel.
 * <p>
 * {@code isFeatureEnabled()} itself is deliberately left un-injected so the probe
 * can still trigger (and catch) VB's natural {@link IllegalStateException}.
 * <p>
 * {@code @Pseudo} silences errors when VB is absent.
 */
@Pseudo
@Mixin(targets = "com.blackgear.vanillabackport.client.api.music.MusicFadeManager", remap = false)
public class MusicFadeManagerDevFix {

    /** Flips to {@code true} once VB's config is confirmed loaded. */
    private static volatile boolean notMineLegacy$configReady = false;

    /**
     * Returns true if VB's config is loaded (or becomes loaded by this call).
     * Returns false if the config spec is still uninitialised — the caller
     * should bail out of VB's music handling for this tick.
     */
    private static boolean notMineLegacy$probeConfigReady() {
        if (notMineLegacy$configReady) return true;
        try {
            java.lang.reflect.Method m = Class.forName(
                    "com.blackgear.vanillabackport.client.api.music.MusicFadeManager")
                    .getDeclaredMethod("isFeatureEnabled");
            m.setAccessible(true);
            m.invoke(null);
            notMineLegacy$configReady = true;
            return true;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalStateException
                    || cause instanceof NullPointerException
                    || cause instanceof NoClassDefFoundError
                    || cause instanceof ExceptionInInitializerError) {
                // Config not ready (IllegalStateException), config field null because
                // registerSafeConfig was skipped on a worker thread (NullPointerException),
                // or VB class init permanently failed (NoClassDefFoundError/EIIE).
                return false;
            }
            // Other failures — assume ready and let VB surface the real error.
            return true;
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            // VanillaBackport's static initializer failed (Forge userdev worker-thread race).
            // The class is permanently poisoned for this session — bail on every call.
            return false;
        } catch (Throwable ignored) {
            // Class/method shape changed — give VB its natural code path.
            return true;
        }
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void notMineLegacy$devSafeTick(SoundInstance soundInstance, CallbackInfoReturnable<Boolean> cir) {
        if (!notMineLegacy$probeConfigReady()) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "preventPlayingInPaleGarden", at = @At("HEAD"), cancellable = true, remap = false)
    private void notMineLegacy$devSafePalePrevent(CallbackInfoReturnable<Boolean> cir) {
        if (!notMineLegacy$probeConfigReady()) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "updateVolume", at = @At("HEAD"), cancellable = true, remap = false)
    private void notMineLegacy$devSafeUpdateVolume(SoundInstance soundInstance, CallbackInfo ci) {
        if (!notMineLegacy$probeConfigReady()) {
            ci.cancel();
        }
    }

    @Inject(method = "onStartPlaying", at = @At("HEAD"), cancellable = true, remap = false)
    private void notMineLegacy$devSafeStartPlaying(CallbackInfo ci) {
        if (!notMineLegacy$probeConfigReady()) {
            ci.cancel();
        }
    }
}
