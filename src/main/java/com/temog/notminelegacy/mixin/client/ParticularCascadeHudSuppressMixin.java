package com.temog.notminelegacy.mixin.client;

import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the Particular mod's dev-only cascade counter HUD overlay (FX-00).
 * <p>
 * Particular's {@code ClientEvents.onRenderHUD} draws an {@code N} (cascade map
 * size) label in the corner whenever {@code FMLLoader.isProduction()} is false.
 * We're in a userdev workspace, so that always renders. Cancel at HEAD so the
 * draw is skipped entirely.
 * <p>
 * {@code @Pseudo} keeps mixin silent when Particular isn't loaded.
 */
@Pseudo
@Mixin(targets = "com.leclowndu93150.particular.ClientStuff$ClientEvents", remap = false)
public class ParticularCascadeHudSuppressMixin {

    @Inject(method = "onRenderHUD", at = @At("HEAD"), cancellable = true, remap = false)
    private static void notMineLegacy$suppressCascadeCounter(RenderGuiOverlayEvent.Post event, CallbackInfo ci) {
        ci.cancel();
    }
}
