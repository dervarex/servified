package com.dervarex.servified.handler;

import com.dervarex.servified.MinecraftVersion;
import com.dervarex.servified.protocol.PacketServer;
import com.dervarex.servified.protocol.State;

import java.util.UUID;

/**
 * Offline mode login, no encryption, no Mojang auth (yet, might do that later)
 */
public final class LoginHandler {

    private LoginHandler() {
    }

    public static void register(PacketServer server) {
        // Login Start
        server.on(State.LOGIN, 0x00, (conn, in) -> {
            String name = in.readString(16);
            UUID uuid = in.readUUID();

            System.out.println("Login Start: " + name + " (" + uuid + ")");

            // Login Success: profile (uuid, name, no properties), then the session id
            conn.send(0x02, out -> out
                    .writeUUID(uuid)
                    .writeString(name)
                    .writeVarInt(0)
                    .writeUUID(new UUID(0, 0)));
        });

        // Login Acknowledged
        server.on(State.LOGIN, 0x03, (conn, in) -> {
            conn.setState(State.CONFIGURATION);
            System.out.println("Switched to CONFIGURATION");

            // Known Packs: we only have the vanilla one
            conn.send(0x0F, out -> out
                    .writeVarInt(1)
                    .writeString("minecraft")
                    .writeString("core")
                    .writeString(MinecraftVersion.NAME));
        });
    }
}
