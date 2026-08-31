package com.temog.notminelegacy.compat;

import net.minecraft.resources.ResourceLocation;

/**
 * Shared NML-facing bridge for optional animal-variant integrations.
 *
 * <p>Feature modules such as Remnant Stars can call into this API without
 * taking a hard runtime dependency on the Building Bridges jar. When that jar
 * is present, it registers an implementation during mod construction.
 */
public final class AnimalVariantBridge {

    public interface Service {
        void forceRitualVariant(int entityId, ResourceLocation texture);
    }

    private static final Service NOOP = (entityId, texture) -> {
    };

    private static volatile Service service = NOOP;

    private AnimalVariantBridge() {
    }

    public static void register(Service variantService) {
        service = variantService != null ? variantService : NOOP;
    }

    public static void forceRitualVariant(int entityId, ResourceLocation texture) {
        service.forceRitualVariant(entityId, texture);
    }
}