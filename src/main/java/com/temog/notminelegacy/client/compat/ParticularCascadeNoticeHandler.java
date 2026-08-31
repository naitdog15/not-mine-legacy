package com.temog.notminelegacy.client.compat;

import com.temog.notminelegacy.NotMineLegacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Replaces Particular's permanent corner cascade counter with a transient
 * "you just walked past a cascade" notification (FX-00).
 * <p>
 * Particular's own HUD is suppressed by {@code ParticularCascadeHudSuppressMixin}.
 * Here we re-add the counter as a Forge GUI overlay that:
 * <ol>
 *   <li>Each client tick, counts entries in {@code Main.cascades}
 *       ({@code ConcurrentHashMap<BlockPos, CascadeData>}) within
 *       {@link #DETECT_RADIUS} blocks of the player.</li>
 *   <li>When that count <em>increases</em>, records the new total and starts
 *       a one-shot slide-in / hold / slide-up animation.</li>
 *   <li>Renders the count at an animated Y offset + alpha; once the animation
 *       finishes, draws nothing until the next trigger.</li>
 * </ol>
 * Walking past a cascade pops the counter briefly; standing in range
 * indefinitely won't keep it on screen.
 */
public final class ParticularCascadeNoticeHandler {

    private static final String PARTICULAR_MOD_ID = "particular";
    private static final String PARTICULAR_MAIN_CLASS = "com.leclowndu93150.particular.Main";

    /** Player-relative cube radius (blocks) within which cascades count as "seen". */
    private static final int DETECT_RADIUS = 48;
    private static final double DETECT_RADIUS_SQR = DETECT_RADIUS * DETECT_RADIUS;

    // Animation phases (ticks). Total ≈ 4 seconds.
    private static final int SLIDE_IN_TICKS  = 8;
    private static final int HOLD_TICKS      = 50;
    private static final int SLIDE_OUT_TICKS = 22;
    private static final int TOTAL_TICKS     = SLIDE_IN_TICKS + HOLD_TICKS + SLIDE_OUT_TICKS;

    // Y travel (pixels). Rest position is REST_Y; animation moves between START_Y and END_Y.
    private static final int REST_Y  = 10;
    private static final int START_Y = -10;   // off-screen above
    private static final int END_Y   = -30;   // slides up off-screen

    // Reflection cache for Particular.Main.cascades
    private static volatile boolean cascadesFieldResolved = false;
    private static volatile Field cascadesField;

    // Animation state (client-side, single-player UI)
    private static int lastInRangeCount = 0;
    private static int displayCount = 0;
    private static long animationStartTick = -1L;

    private ParticularCascadeNoticeHandler() {}

    @Mod.EventBusSubscriber(modid = NotMineLegacy.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(),
                    "particular_cascade_notice",
                    (forgeGui, graphics, partialTick, screenWidth, screenHeight) ->
                            renderNotification(graphics));
        }
    }

    @Mod.EventBusSubscriber(modid = NotMineLegacy.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (!ModList.get().isLoaded(PARTICULAR_MOD_ID)) return;

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null) {
                lastInRangeCount = 0;
                animationStartTick = -1L;
                return;
            }

            int currentInRange = countCascadesInRange(player.position());
            if (currentInRange > lastInRangeCount) {
                // New cascade(s) entered detection range — fire a fresh notification.
                displayCount = currentInRange;
                animationStartTick = mc.level.getGameTime();
            }
            lastInRangeCount = currentInRange;
        }
    }

    private static void renderNotification(GuiGraphics graphics) {
        if (animationStartTick < 0L) return;
        if (!ModList.get().isLoaded(PARTICULAR_MOD_ID)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;

        long elapsed = mc.level.getGameTime() - animationStartTick;
        if (elapsed >= TOTAL_TICKS) {
            animationStartTick = -1L;
            return;
        }

        float y;
        float alpha;
        if (elapsed < SLIDE_IN_TICKS) {
            float t = elapsed / (float) SLIDE_IN_TICKS;
            y = Mth.lerp(t, START_Y, REST_Y);
            alpha = t;
        } else if (elapsed < SLIDE_IN_TICKS + HOLD_TICKS) {
            y = REST_Y;
            alpha = 1.0F;
        } else {
            float t = (elapsed - SLIDE_IN_TICKS - HOLD_TICKS) / (float) SLIDE_OUT_TICKS;
            y = Mth.lerp(t, REST_Y, END_Y);
            alpha = 1.0F - t;
        }

        int alphaInt = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        if (alphaInt <= 0) return;

        Font font = mc.font;
        Component text = Component.translatable("hud.notminelegacy.cascades", displayCount);
        int color = (alphaInt << 24) | 0xFFFFFF;
        graphics.drawString(font, text, 10, (int) y, color, true);
    }

    private static int countCascadesInRange(Vec3 playerPos) {
        Map<?, ?> map = resolveCascadesMap();
        if (map == null || map.isEmpty()) return 0;

        int count = 0;
        // ConcurrentHashMap iteration is weakly consistent — safe but may miss concurrent inserts.
        for (Object key : map.keySet()) {
            if (!(key instanceof BlockPos pos)) continue;
            double dx = pos.getX() + 0.5D - playerPos.x;
            double dy = pos.getY() + 0.5D - playerPos.y;
            double dz = pos.getZ() + 0.5D - playerPos.z;
            if (dx * dx + dy * dy + dz * dz <= DETECT_RADIUS_SQR) {
                count++;
            }
        }
        return count;
    }

    private static Map<?, ?> resolveCascadesMap() {
        if (!cascadesFieldResolved) {
            synchronized (ParticularCascadeNoticeHandler.class) {
                if (!cascadesFieldResolved) {
                    try {
                        Class<?> cls = Class.forName(PARTICULAR_MAIN_CLASS);
                        Field f = cls.getField("cascades");
                        f.setAccessible(true);
                        cascadesField = f;
                    } catch (Throwable ignored) {
                        cascadesField = null;
                    }
                    cascadesFieldResolved = true;
                }
            }
        }
        Field f = cascadesField;
        if (f == null) return null;
        try {
            Object value = f.get(null);
            return value instanceof Map<?, ?> m ? m : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
