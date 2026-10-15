package com.temog.notminelegacy.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Soft NML version contract. Sibling modules no longer hard-require a matching NML build;
 * they declare the NML version they were built against in their mods.toml:
 * <pre>
 * [modproperties.mymod]
 * nmlBuiltAgainst="1.1.0"
 * </pre>
 * On a player's first login to a world, if the installed NML is older than any loaded module's
 * declared version, a one-time chat notice recommends updating NML.
 */
@Mod.EventBusSubscriber(modid = "notminelegacy")
public final class NmlVersionNotice {

    public static final String PROPERTY = "nmlBuiltAgainst";
    private static final String NBT_KEY = "notminelegacy_version_notice_shown";
    private static final Logger LOGGER = LogUtils.getLogger();

    private NmlVersionNotice() {}

    /** Mod ids (with their wanted version) whose declared NML version is newer than the installed one. */
    public static List<String> findOutdatedDependents() {
        List<String> out = new ArrayList<>();
        ModList list = ModList.get();
        ArtifactVersion installed = list.getModContainerById("notminelegacy")
                .map(c -> c.getModInfo().getVersion()).orElse(null);
        if (installed == null) return out;
        for (IModInfo info : list.getMods().stream().map(m -> (IModInfo) m).toList()) {
            Map<String, Object> props = info.getModProperties();
            Object wanted = props.get(PROPERTY);
            if (wanted == null) continue;
            try {
                if (new DefaultArtifactVersion(wanted.toString()).compareTo(installed) > 0) {
                    out.add(info.getDisplayName() + " (built for NML " + wanted + ")");
                }
            } catch (RuntimeException e) {
                LOGGER.debug("[NML] Unparseable {} on {}: {}", PROPERTY, info.getModId(), wanted);
            }
        }
        return out;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag persisted = player.getPersistentData();
        CompoundTag root = persisted.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        if (root.getBoolean(NBT_KEY)) return;
        List<String> outdated = findOutdatedDependents();
        if (outdated.isEmpty()) return;
        root.putBoolean(NBT_KEY, true);
        persisted.put(ServerPlayer.PERSISTED_NBT_TAG, root);
        String installed = ModList.get().getModContainerById("notminelegacy")
                .map(c -> c.getModInfo().getVersion().toString()).orElse("?");
        player.sendSystemMessage(Component.literal("[Not Mine Legacy] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Installed NML " + installed + " is older than what "
                        + String.join(", ", outdated)
                        + " expect. Updating NML is recommended; otherwise you may see crashes or bugs.")
                        .withStyle(ChatFormatting.YELLOW)));
        LOGGER.warn("[NML] Installed {} older than required by: {}", installed, outdated);
    }
}
