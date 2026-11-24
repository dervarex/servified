package com.dervarex.servified.handler;

import com.dervarex.servified.protocol.Connection;
import com.dervarex.servified.protocol.PacketServer;
import com.dervarex.servified.protocol.State;
import com.dervarex.servified.registry.Registries;
import com.dervarex.servified.world.FlatChunk;

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
        FlatChunk chunk = new FlatChunk(registries.id("minecraft:worldgen/biome", "minecraft:plains"));

        // Acknowledge Finish Configuration, the client is ready to join
        server.on(State.CONFIGURATION, 0x03, (conn, in) -> {
            conn.setState(State.PLAY);
            System.out.println("Switched to PLAY mode");

            sendLogin(conn, dimensionType);
            spawn(conn, chunk);
            startKeepAlive(conn);
        });

        // Confirm Teleportation, the client accepted our position
        server.on(State.PLAY, 0x00, (conn, in) -> System.out.println("Teleport confirmed: " + in.readVarInt()));

        // Player Loaded, the loading screen is gone
        server.on(State.PLAY, 0x2C, (conn, in) -> System.out.println("Player loaded, we're in the world :)")); // or atleast we should be :(

        // Client Tick End, comes 20 times a second, nothing to do yet
        server.on(State.PLAY, 0x0D, (conn, in) -> {
        });

        // Keep Alive answer, we don't check it yet
        server.on(State.PLAY, 0x1C, (conn, in) -> {
        });
    }

    private static void spawn(Connection conn, FlatChunk chunk) throws IOException {
        conn.send(0x27, out -> out.writeByte(13).writeFloat(0));

        // Set Center Chunk
        conn.send(0x60, out -> out.writeVarInt(0).writeVarInt(0));

        // Chunk Data and Update Light, a 5x5 platform around 0 0
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                int chunkX = x, chunkZ = z;
                conn.send(0x2E, out -> out.writeInt(chunkX).writeInt(chunkZ).writeBytes(chunk.body()));
            }
        }

        // Synchronize Player Position, middle of chunk 0 0 on top of the stone
        conn.send(0x49, out -> out
                .writeVarInt(1)                                                           // teleport id, the client confirms it
                .writeDouble(8.5).writeDouble(FlatChunk.SURFACE_Y).writeDouble(8.5)
                .writeDouble(0).writeDouble(0).writeDouble(0)                 // velocity
                .writeFloat(0).writeFloat(0)                                        // yaw, pitch
                .writeInt(0));                                                            // nothing relative, absolute position

        System.out.println("Sent spawn chunks and position");
    }

    /** The client disconnects after ~30s without packets, so wake it up every 10s */
    private static void startKeepAlive(Connection conn) {
        Thread.ofVirtual().start(() -> {
            try {
                while (conn.isOpen()) {
                    Thread.sleep(10_000);
                    conn.send(0x2D, out -> out.writeLong(System.currentTimeMillis()));
                }
            } catch (IOException | InterruptedException e) {
                // connection is gone, so no need to keep it awake anymore
            }
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
