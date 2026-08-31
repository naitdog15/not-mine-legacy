package com.temog.notminelegacy.mixin;

import com.temog.notminelegacy.compat.EarlyModCheck;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class NotMineLegacyMixinPlugin implements IMixinConfigPlugin {

    private static final String GECKOLIB_MODID = "geckolib";
    private static final String GECKOLIB_CREEPER_FALLBACK_MIXIN =
            "com.temog.notminelegacy.mixin.client.GeckoLibCreeperRendererFallbackMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (GECKOLIB_CREEPER_FALLBACK_MIXIN.equals(mixinClassName)) {
            return isGeckoLibLoadedEarlySafe();
        }
        return true;
    }

    private static boolean isGeckoLibLoadedEarlySafe() {
        return EarlyModCheck.isModLoadedEarlySafe(GECKOLIB_MODID);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}