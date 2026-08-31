package com.temog.notminelegacy.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dev-environment fix for VanillaBackport's class-init race.
 *
 * VanillaBackport's SpriteResourceLoader mixin triggers VanillaBackport.<clinit>
 * inside a ForkJoin worker thread during texture atlas loading. The static initializer
 * calls Platform's EnvironmentImpl.registerSafeConfig(), which calls
 * ModLoadingContext.getActiveContainer() — but that ThreadLocal is empty on worker
 * threads, so it throws RuntimeException("Where is minecraft???!") and permanently
 * poisons the VanillaBackport class for the JVM session.
 *
 * This mixin makes registerSafeConfig() silently return null when called outside a
 * valid mod loading context. VB's config is simply not registered in those contexts;
 * the race doesn't exist in production so this only affects the dev client.
 *
 * @Pseudo silences errors when Platform is absent.
 * Handler captures no parameters — Mixin allows any prefix subset of the target params.
 * registerSafeConfig is static and returns a non-void value, hence: static + CIR.
 */
@Pseudo
@Mixin(targets = "com.blackgear.platform.core.forge.EnvironmentImpl", remap = false)
public class PlatformSafeConfigDevFix {

    @SuppressWarnings("deprecation")
    @Inject(method = "registerSafeConfig", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nml$guardSafeConfig(CallbackInfoReturnable<?> cir) {
        try {
            net.minecraftforge.fml.ModLoadingContext.get().getActiveContainer();
        } catch (RuntimeException e) {
            // Not in a mod loading context (worker / render thread).
            // Return null — VB's <clinit> completes without poisoning the class.
            cir.cancel();
        }
    }
}
