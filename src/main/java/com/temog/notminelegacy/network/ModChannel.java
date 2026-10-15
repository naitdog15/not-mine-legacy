package com.temog.notminelegacy.network;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Universal channel registration helper, namespaced by mod id. A module creates one instance
 * ({@code new ModChannel("mymod", "main", "1")}), declares its packets in a registrar passed to
 * {@link #registerOnCommonSetup}, and sends through {@link #channel()}. Packets stay in the module
 * that owns them.
 *
 * <p>Message ids are assigned positionally via {@link #nextId()}. Bump the protocol version
 * whenever the packet table changes (entry added, removed or reordered) so a mismatched client
 * fails the handshake loudly instead of mis-decoding one id as another.
 */
@SuppressWarnings("removal") // NetworkRegistry.newSimpleChannel — the house pattern for every NML channel
public final class ModChannel {

    private final SimpleChannel channel;
    private int id = 0;

    public ModChannel(String modId, String path, String protocolVersion) {
        this.channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(modId, path),
                () -> protocolVersion,
                protocolVersion::equals,
                protocolVersion::equals
        );
    }

    /** The underlying channel, for {@code messageBuilder} and the send methods. */
    public SimpleChannel channel() {
        return channel;
    }

    /** Next positional message id. */
    public int nextId() {
        return id++;
    }

    /**
     * Subscribes {@code registrar} to the given mod event bus's {@link FMLCommonSetupEvent}; it runs
     * through {@code enqueueWork} so registration lands on the main thread and the module's entry
     * point needs no other edit. The registrar receives this helper so it can call
     * {@code helper.channel().messageBuilder(Foo.class, helper.nextId(), ...)}.
     */
    public void registerOnCommonSetup(IEventBus modBus, Consumer<ModChannel> registrar) {
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> registrar.accept(this)));
    }

    /**
     * Same as {@link #registerOnCommonSetup} for a module that already has a static
     * {@code @SubscribeEvent} handler for {@link FMLCommonSetupEvent} (the What Remains pattern).
     */
    public void enqueueRegister(FMLCommonSetupEvent event, Consumer<ModChannel> registrar) {
        event.enqueueWork(() -> registrar.accept(this));
    }

    /** Convenience wrapper so callers do not have to reach through {@link #channel()}. */
    public void sendToServer(Object message) {
        channel.sendToServer(message);
    }
}
