package com.dervarex.servified.handler;

import com.dervarex.servified.MinecraftVersion;
import com.dervarex.servified.protocol.PacketServer;
import com.dervarex.servified.protocol.State;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * Server list information
 */
public final class StatusHandler {

    private StatusHandler() {
    }

    public static void register(PacketServer server) throws IOException {
        String json = statusJson();

        server.on(State.STATUS, 0x00, (conn, in) -> conn.send(0x00, out -> out.writeString(json)));
        server.on(State.STATUS, 0x01, (conn, in) -> {
            long timestamp = in.readLong();
            conn.send(0x01, out -> out.writeLong(timestamp));
        });
    }

    private static String statusJson() throws IOException {
        return """
            {
              "version": { "name": "servified", "protocol": %d },
              "players": {
                "max": 20,
                "online": 0,
                "sample": [
                  { "name": "dervarex", "id": "a31ccf30-00e4-4928-a590-e366c90af710" }
                ]
              },
              "description": {
                "text": "servified",
                "color": "gold",
                "extra": [
                  { "text": " • not normal", "color": "gray" }
                ]
              },
              "favicon": "data:image/png;base64,%s"
            }
            """.formatted(MinecraftVersion.PROTOCOL, icon());
    }

    private static String icon() throws IOException {
        try (InputStream in = StatusHandler.class.getResourceAsStream("/server-icon.png")) {
            if (in == null)
                throw new IOException("server-icon.png missing in resources!!");

            return Base64.getEncoder().encodeToString(in.readAllBytes());
        }
    }
}
