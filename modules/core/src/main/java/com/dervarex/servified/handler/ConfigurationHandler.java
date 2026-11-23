package com.dervarex.servified.handler;

import com.dervarex.servified.protocol.PacketServer;
import com.dervarex.servified.protocol.State;

/**
 * Everything between login and joining is here, that's the configuration
 */
public final class ConfigurationHandler {

    private ConfigurationHandler() {
    }

    public static void register(PacketServer server) {
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

            // TODO we can send Registry Data here then :)
        });
    }
}
