package com.temog.notminelegacy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.temog.notminelegacy.NotMineLegacy;
import com.temog.notminelegacy.speech.SpeechBubbleHandler;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = NotMineLegacy.MODID, value = Dist.CLIENT)
public final class SpeechBubbleRenderer {

    private static final ResourceLocation[] SPEECH_BUBBLE_FRAMES = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath(NotMineLegacy.MODID, "textures/entity/speech_bubble/frame_0.png"),
            ResourceLocation.fromNamespaceAndPath(NotMineLegacy.MODID, "textures/entity/speech_bubble/frame_1.png"),
            ResourceLocation.fromNamespaceAndPath(NotMineLegacy.MODID, "textures/entity/speech_bubble/frame_2.png"),
            ResourceLocation.fromNamespaceAndPath(NotMineLegacy.MODID, "textures/entity/speech_bubble/frame_3.png"),
            ResourceLocation.fromNamespaceAndPath(NotMineLegacy.MODID, "textures/entity/speech_bubble/frame_4.png"),
            ResourceLocation.fromNamespaceAndPath(NotMineLegacy.MODID, "textures/entity/speech_bubble/frame_5.png"),
            ResourceLocation.fromNamespaceAndPath(NotMineLegacy.MODID, "textures/entity/speech_bubble/frame_6.png"),
            ResourceLocation.fromNamespaceAndPath(NotMineLegacy.MODID, "textures/entity/speech_bubble/frame_7.png")
    };
    private static final double MAX_RENDER_DISTANCE_SQR = 64.0D * 64.0D;
    private static final float BASE_SIZE = 0.62F;
    private static final double BUBBLE_VERTICAL_OFFSET = 0.82D;
    private static final int BASE_FRAME_TIME_TICKS = 4;
    private static final int FRAME_TIME_EXTRA_MIN_TICKS = 2;
    private static final int FRAME_TIME_EXTRA_MAX_TICKS = 4;
    private static final int INACTIVE_CLEANUP_INTERVAL_TICKS = 20;

    private static final Map<Integer, BubbleAnimationState> BUBBLE_ANIMATIONS = new HashMap<>();
    private static long lastInactiveCleanupTick = Long.MIN_VALUE;

    private SpeechBubbleRenderer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            BUBBLE_ANIMATIONS.clear();
            lastInactiveCleanupTick = Long.MIN_VALUE;
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        long levelTick = level.getGameTime();
        cleanupInactiveBubbles(level, levelTick);

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Mob mob) || !shouldRenderBubble(mob, cameraPos)) {
                continue;
            }

            renderBubble(mob, camera, poseStack, buffers, levelTick, event.getPartialTick());
        }

        buffers.endBatch();
    }

    private static boolean shouldRenderBubble(Mob mob, Vec3 cameraPos) {
        if (!mob.isAlive() || mob.isInvisible()) {
            return false;
        }
        if (!SpeechBubbleHandler.isActive(mob)) {
            BUBBLE_ANIMATIONS.remove(mob.getId());
            return false;
        }
        return mob.position().distanceToSqr(cameraPos) <= MAX_RENDER_DISTANCE_SQR;
    }

    private static void renderBubble(
            Mob mob,
            Camera camera,
            PoseStack poseStack,
            MultiBufferSource buffers,
            long levelTick,
            float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        double x = Mth.lerp(partialTick, mob.xOld, mob.getX()) - cameraPos.x;
        double y = Mth.lerp(partialTick, mob.yOld, mob.getY()) - cameraPos.y + mob.getBbHeight() + BUBBLE_VERTICAL_OFFSET;
        double z = Mth.lerp(partialTick, mob.zOld, mob.getZ()) - cameraPos.z;

        BubbleAnimationState animation = BUBBLE_ANIMATIONS.computeIfAbsent(
                mob.getId(),
                ignored -> createAnimationState(mob, levelTick));
        float age = Math.max(0.0F, (float) (levelTick - animation.startTick()) + partialTick);
        float popScale = Mth.clamp(age / 4.0F, 0.0F, 1.0F);
        float size = BASE_SIZE * Mth.lerp(popScale * popScale, 0.35F, 1.0F);
        int frame = resolveAnimatedFrame(age, animation);
        RenderType renderType = RenderType.entityTranslucent(SPEECH_BUBBLE_FRAMES[frame]);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        VertexConsumer consumer = buffers.getBuffer(renderType);
        addVertex(consumer, pose, normal, -size * 0.5F, -size * 0.5F, 0.0F, 0.0F, 1.0F);
        addVertex(consumer, pose, normal, size * 0.5F, -size * 0.5F, 0.0F, 1.0F, 1.0F);
        addVertex(consumer, pose, normal, size * 0.5F, size * 0.5F, 0.0F, 1.0F, 0.0F);
        addVertex(consumer, pose, normal, -size * 0.5F, size * 0.5F, 0.0F, 0.0F, 0.0F);
        poseStack.popPose();
    }

    private static BubbleAnimationState createAnimationState(Mob mob, long levelTick) {
        long seed = mob.getUUID().getMostSignificantBits()
                ^ mob.getUUID().getLeastSignificantBits()
                ^ ((long) mob.getId() * 31L)
                ^ levelTick;
        int frameTimeExtraRange = (FRAME_TIME_EXTRA_MAX_TICKS - FRAME_TIME_EXTRA_MIN_TICKS) + 1;
        int frameTimeTicks = BASE_FRAME_TIME_TICKS
                + FRAME_TIME_EXTRA_MIN_TICKS
                + (int) Math.floorMod(seed, frameTimeExtraRange);
        int repeatedFrame = 1 + (int) Math.floorMod(seed >> 8, SPEECH_BUBBLE_FRAMES.length - 2L);
        return new BubbleAnimationState(levelTick, frameTimeTicks, repeatedFrame);
    }

    private static int resolveAnimatedFrame(float age, BubbleAnimationState animation) {
        int sequenceLength = SPEECH_BUBBLE_FRAMES.length + 1;
        int sequenceIndex = ((int) age / animation.frameTimeTicks()) % sequenceLength;
        if (sequenceIndex <= animation.repeatedFrame()) {
            return sequenceIndex;
        }
        return sequenceIndex - 1;
    }

    private static void addVertex(
            VertexConsumer consumer,
            Matrix4f pose,
            Matrix3f normal,
            float x,
            float y,
            float z,
            float u,
            float v) {
        consumer.vertex(pose, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static void cleanupInactiveBubbles(ClientLevel level, long levelTick) {
        if (lastInactiveCleanupTick != Long.MIN_VALUE
                && levelTick - lastInactiveCleanupTick < INACTIVE_CLEANUP_INTERVAL_TICKS) {
            return;
        }
        lastInactiveCleanupTick = levelTick;

        Iterator<Integer> iterator = BUBBLE_ANIMATIONS.keySet().iterator();
        while (iterator.hasNext()) {
            Entity entity = level.getEntity(iterator.next());
            if (!(entity instanceof Mob mob) || !SpeechBubbleHandler.isActive(mob)) {
                iterator.remove();
            }
        }
    }

    private record BubbleAnimationState(long startTick, int frameTimeTicks, int repeatedFrame) {}
}
