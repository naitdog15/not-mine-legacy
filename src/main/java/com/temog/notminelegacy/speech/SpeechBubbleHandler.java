package com.temog.notminelegacy.speech;

import net.minecraft.world.entity.Entity;

public final class SpeechBubbleHandler {

    private SpeechBubbleHandler() {}

    public static void show(Entity entity, int ticks) {
        if (entity instanceof SpeechBubbleAccess access) {
            access.notminelegacy$setSpeechBubbleTicks(ticks);
        }
    }

    public static void clear(Entity entity) {
        show(entity, 0);
    }

    public static boolean isActive(Entity entity) {
        return getTicks(entity) > 0;
    }

    public static int getTicks(Entity entity) {
        if (entity instanceof SpeechBubbleAccess access) {
            return access.notminelegacy$getSpeechBubbleTicks();
        }
        return 0;
    }
}
