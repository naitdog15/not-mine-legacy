package com.temog.notminelegacy.mixin.common;

import com.temog.notminelegacy.speech.SpeechBubbleAccess;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class SpeechBubbleMobMixin implements SpeechBubbleAccess {

    @Unique
    private static final EntityDataAccessor<Integer> NML_SPEECH_BUBBLE_TICKS =
            SynchedEntityData.defineId(Mob.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void notminelegacy$defineSpeechBubbleTicks(CallbackInfo ci) {
        ((Mob) (Object) this).getEntityData().define(NML_SPEECH_BUBBLE_TICKS, 0);
    }

    @Override
    public int notminelegacy$getSpeechBubbleTicks() {
        return ((Mob) (Object) this).getEntityData().get(NML_SPEECH_BUBBLE_TICKS);
    }

    @Override
    public void notminelegacy$setSpeechBubbleTicks(int ticks) {
        ((Mob) (Object) this).getEntityData().set(NML_SPEECH_BUBBLE_TICKS, Math.max(ticks, 0));
    }
}
