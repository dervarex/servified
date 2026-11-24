package com.dervarex.servified.handler;

import com.dervarex.servified.protocol.PacketServer;
import com.dervarex.servified.protocol.State;
import com.dervarex.servified.registry.Registries;
import com.dervarex.servified.registry.Tags;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Everything between login and joining is here, that's the configuration
 */
public final class ConfigurationHandler {

    private ConfigurationHandler() {
    }

    public static void register(PacketServer server, Registries loaded) throws IOException {
        Map<String, List<String>> registries = loaded.synced();
        Map<String, Map<String, int[]>> tags = Tags.load(loaded).all();

        // Client Information
        server.on(State.CONFIGURATION, 0x00, (conn, in) -> {
            String locale = in.readString(16);
            int viewDistance = in.readByte();

            System.out.println("Client Information: locale=" + locale + ", viewDistance=" + viewDistance);
        });

        // Plugin Message, client sends us what brand it is here
        server.on(State.CONFIGURATION, 0x02, (conn, in) -> {
            String channel = in.readString();

            if (channel.equals("minecraft:brand"))
                System.out.println("Client brand: " + in.readString());
            else
                System.out.println("Plugin message on " + channel + " (" + in.remaining() + " bytes)");
        });

        // Known Packs, the client's answer to ours
        server.on(State.CONFIGURATION, 0x07, (conn, in) -> {
            int count = in.readVarInt();
            System.out.println("Received Known Packs (" + count + "):");

            for (int i = 0; i < count; i++)
                System.out.printf("  %s:%s (version %s)%n", in.readString(), in.readString(), in.readString());

            // Registry Data, one packet per registry, no data since the client has it already
            for (var registry : registries.entrySet()) {
                conn.send(0x07, out -> {
                    out.writeString(registry.getKey()).writeVarInt(registry.getValue().size());

                    for (String entry : registry.getValue())
                        out.writeString(entry).writeBoolean(false);
                });
            }

            System.out.println("Sent " + registries.size() + " registries");

            // Update Tags, registry entries point at tags so the client needs them before it can load anything
            conn.send(0x0E, out -> {
                out.writeVarInt(tags.size());

                tags.forEach((registry, byTag) -> {
                    out.writeString(registry).writeVarInt(byTag.size());

                    byTag.forEach((tag, ids) -> {
                        out.writeString(tag).writeVarInt(ids.length);

                        for (int id : ids)
                            out.writeVarInt(id);
                    });
                });
            });

            System.out.println("Sent tags for " + tags.size() + " registries");

            // Finish Configuration
            conn.send(0x03);
        });
    }
}
