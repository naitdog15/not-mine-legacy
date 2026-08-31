package com.temog.notminelegacy.client.compat;

import com.temog.notminelegacy.NotMineLegacy;

import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Compatibility fix for VanillaBackport:
 * VanillaBackport's wolf armor render layer bakes {@code minecraft:wolf_armor#main},
 * but some versions forget to register the layer definition, causing client startup crashes.
 *
 * <p>This registers a safe fallback definition using vanilla's wolf body layer.
 */
@Mod.EventBusSubscriber(modid = NotMineLegacy.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WolfArmorLayerCompat {

    @SuppressWarnings("removal")
    private static final ModelLayerLocation WOLF_ARMOR_LAYER = new ModelLayerLocation(
            new ResourceLocation("minecraft", "wolf_armor"),
            "main");

    private WolfArmorLayerCompat() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        if (!ModList.get().isLoaded("vanillabackport")) return;

        // Avoid overriding a correct registration (e.g., if VanillaBackport fixes this in the future).
        if (isLayerDefinitionAlreadyRegistered(WOLF_ARMOR_LAYER)) return;

        event.registerLayerDefinition(WOLF_ARMOR_LAYER, WolfModel::createBodyLayer);
    }

    private static boolean isLayerDefinitionAlreadyRegistered(ModelLayerLocation location) {
        try {
            Field field = ForgeHooksClient.class.getDeclaredField("layerDefinitions");
            field.setAccessible(true);

            Object value = field.get(null);
            if (!(value instanceof Map<?, ?> map)) return false;

            return map.containsKey(location);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
