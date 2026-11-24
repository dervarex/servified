package com.dervarex.servified.handler;

import com.dervarex.servified.player.Player;
import com.dervarex.servified.player.Players;
import com.dervarex.servified.protocol.Connection;
import com.dervarex.servified.protocol.PacketInput;
import com.dervarex.servified.protocol.PacketServer;
import com.dervarex.servified.protocol.ProtocolException;
import com.dervarex.servified.protocol.State;

/**
 * The client tells us where it is, we just believe it for now (no anti cheat lol)
 */
public final class MovementHandler {

    private MovementHandler() {
    }

    public static void register(PacketServer server, Players players) {
        // Move Player Pos: x, y (feet), z, flags
        server.on(State.PLAY, 0x1E, (conn, in) -> {
            Player player = player(players, conn);
            player.setPosition(in.readDouble(), in.readDouble(), in.readDouble());
            flags(player, in);
        });

        // Move Player Pos Rot: x, y, z, yaw, pitch, flags
        server.on(State.PLAY, 0x1F, (conn, in) -> {
            Player player = player(players, conn);
            player.setPosition(in.readDouble(), in.readDouble(), in.readDouble());
            player.setRotation(in.readFloat(), in.readFloat());
            flags(player, in);
        });

        // Move Player Rot: yaw, pitch, flags
        server.on(State.PLAY, 0x20, (conn, in) -> {
            Player player = player(players, conn);
            player.setRotation(in.readFloat(), in.readFloat());
            flags(player, in);
        });

        // Move Player Status Only: just the flags
        server.on(State.PLAY, 0x21, (conn, in) -> flags(player(players, conn), in));
    }

    /** Bit 0 = on ground, bit 1 = walking against a wall (we don't care about that one yet, maybe we can later add some handling for that) */
    private static void flags(Player player, PacketInput in) throws ProtocolException {
        player.setOnGround((in.readUnsignedByte() & 0x01) != 0);

        // every movement packet ends with the flags, so this is a good spot to notice chunk changes
        if (player.enteredNewChunk())
            System.out.printf("%s is now in chunk %d %d%n", player.name(), player.chunkX(), player.chunkZ());
    }

    private static Player player(Players players, Connection conn) throws ProtocolException {
        Player player = players.get(conn);

        if (player == null)
            throw new ProtocolException("Movement from a connection that never logged in, wtf");

        return player;
    }
}
