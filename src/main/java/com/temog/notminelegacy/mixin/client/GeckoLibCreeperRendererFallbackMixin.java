package com.temog.notminelegacy.mixin.client;

import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = EntityRenderers.class, priority = 2000)
public abstract class GeckoLibCreeperRendererFallbackMixin {

    @Inject(method = "createEntityRenderers", at = @At("RETURN"), cancellable = true, require = 0)
    private static void notMineLegacy$forceVanillaCreeperRenderer(EntityRendererProvider.Context context,
                                                                   CallbackInfoReturnable<Map<EntityType<?>, EntityRenderer<?>>> cir) {
        Map<EntityType<?>, EntityRenderer<?>> renderers = new HashMap<>(cir.getReturnValue());
        renderers.put(EntityType.CREEPER, new CreeperRenderer(context));
        cir.setReturnValue(renderers);
    }
}