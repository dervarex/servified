package com.dervarex.servified.handler;

import com.dervarex.servified.protocol.Connection;
import com.dervarex.servified.protocol.PacketServer;
import com.dervarex.servified.protocol.State;
import com.dervarex.servified.registry.Registries;

import java.io.IOException;

/**
 * Handles the PLAY mode, basically the Loading Terrain Screen.
 */
public final class PlayHandler {

    private static final String DIMENSION = "minecraft:overworld";

    private PlayHandler() {
    }

    public static void register(PacketServer server, Registries registries) {
        int dimensionType = registries.id("minecraft:dimension_type", DIMENSION);

        // Acknowledge Finish Configuration, the client is ready to join
        server.on(State.CONFIGURATION, 0x03, (conn, in) -> {
            conn.setState(State.PLAY);
            System.out.println("Switched to PLAY mode");

            sendLogin(conn, dimensionType);
        });
    }

    /** Field order, see 26.3's ClientboundLoginPacket and CommonPlayerSpawnInfo, it does that too so it should work here aswell*/
    private static void sendLogin(Connection conn, int dimensionType) throws IOException {
        conn.send(0x32, out -> out
                .writeInt(1)         // entity id
                .writeBoolean(false) // hardcore
                .writeVarInt(1)      // dimensions on the server
                .writeString(DIMENSION)
                .writeVarInt(20)     // max players, the client ignores it anyway
                .writeVarInt(8)      // view distance
                .writeVarInt(8)      // simulation distance
                .writeBoolean(false) // reduced debug info
                .writeBoolean(true)  // respawn screen
                .writeBoolean(false) // limited crafting

                // spawn info
                .writeVarInt(dimensionType)  // id in minecraft:dimension_type
                .writeString(DIMENSION)
                .writeLong(0)          // hashed seed
                .writeVarInt(1)        // game mode, creative so we don't die in the void
                .writeVarInt(0)        // previous game mode, 0 = none (it's id + 1)
                .writeBoolean(false)   // debug world
                .writeBoolean(true)    // flat world
                .writeBoolean(false)   // no death location
                .writeVarInt(0)        // portal cooldown
                .writeVarInt(63)       // sea level

                .writeBoolean(false)   // online mode
                .writeBoolean(false)); // enforces secure chat

        System.out.println("Sent Login (play)");
    }
}
