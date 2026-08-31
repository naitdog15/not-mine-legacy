package com.temog.notminelegacy.mixin.client;

import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import net.minecraftforge.fml.ModList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * Provides a fallback LayerDefinition for VanillaBackport's wolf_armor layer when VB's
 * own layer registration is skipped (dev-env config race — registerSafeConfig() suppressed,
 * CLIENT_CONFIG null, VB's RegisterLayerDefinitionsEvent handler NPEs before registering).
 *
 * Uses @Redirect on the Map.get() call instead of @Inject + HEAD setReturnValue, because
 * the HEAD injection was consistently not cancelling (investigation inconclusive).
 * @Redirect directly replaces the get() call that returns null, giving us a fallback
 * LayerDefinition before the null-check throw can fire.
 */
@Mixin(EntityModelSet.class)
public abstract class EntityModelSetWolfArmorFallbackMixin {

    @SuppressWarnings({"unchecked", "removal"})
    @Redirect(
        method = "bakeLayer",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"),
        require = 0
    )
    private Object notMineLegacy$wolfArmorFallback(Map<?, ?> map, Object key) {
        Object existing = map.get(key);
        if (existing != null) return existing;
        if (!(key instanceof ModelLayerLocation mll)) return null;
        if (!"minecraft:wolf_armor#main".equals(mll.toString())) return null;
        if (!ModList.get().isLoaded("vanillabackport")) return null;
        return WolfModel.createBodyLayer();
    }
}