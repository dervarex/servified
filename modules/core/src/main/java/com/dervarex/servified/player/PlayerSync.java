package com.dervarex.servified.player;

import com.dervarex.servified.protocol.Connection;

import java.io.IOException;

/**
 * Makes players see each other: tab list, player models and movement <br>
 * Packet formats from 26.3's ClientboundPlayerInfoUpdatePacket, ClientboundAddEntityPacket and ClientboundTeleportEntityPacket
 */
public final class PlayerSync {

    /** minecraft:player in the entity_type registry (reports/registries.json) */
    private static final int PLAYER_ENTITY_TYPE = 159;

    /** ADD_PLAYER, UPDATE_GAME_MODE, UPDATE_LISTED, UPDATE_LATENCY */
    private static final int ADD_ACTIONS = 1 | 1 << 2 | 1 << 3 | 1 << 4;

    private PlayerSync() {
    }

    /** dervarex, this is derwarex. derwarex, this is dervarex. everybody gets to know everyone */
    public static void join(Player joined, Players players) throws IOException {
        // set this before looking at the others, so two players joining at the same time always find each other
        joined.setInWorld(true);

        sendInfo(joined.connection(), joined);

        for (Player other : players.all()) {
            if (other == joined || !other.inWorld())
                continue;

            sendInfo(other.connection(), joined);
            spawnEntity(other.connection(), joined);

            sendInfo(joined.connection(), other);
            spawnEntity(joined.connection(), other);
        }
    }

    public static void move(Player moved, Players players) {
        for (Player other : players.all()) {
            if (other == moved || !other.inWorld())
                continue;

            try {
                other.connection().send(0x80, out -> out
                        .writeVarInt(moved.entityId())
                        .writeDouble(moved.x()).writeDouble(moved.y()).writeDouble(moved.z())
                        .writeDouble(0).writeDouble(0).writeDouble(0)   // velocity
                        .writeFloat(moved.yaw()).writeFloat(moved.pitch())
                        .writeInt(0)                                    // nothing relative
                        .writeBoolean(moved.onGround()));

                other.connection().send(0x55, out -> out
                        .writeVarInt(moved.entityId())
                        .writeByte(angle(moved.yaw())));
            } catch (IOException e) {
                // that player is probably leaving right now
            }
        }
    }

    public static void leave(Player left, Players players) {
        left.setInWorld(false);

        for (Player other : players.all()) {
            if (other == left || !other.inWorld())
                continue;

            try {
                other.connection().send(0x4E, out -> out.writeVarInt(1).writeVarInt(left.entityId()));
                other.connection().send(0x46, out -> out.writeVarInt(1).writeUUID(left.uuid()));
            } catch (IOException e) {
                //
            }
        }
    }

    /** Player Info Update */
    private static void sendInfo(Connection to, Player player) throws IOException {
        to.send(0x47, out -> out
                .writeByte(ADD_ACTIONS)
                .writeVarInt(1)                 // entries
                .writeUUID(player.uuid())
                .writeString(player.name())     // ADD_PLAYER: name
                .writeVarInt(0)                 //             properties, no skin data in offline mode
                .writeVarInt(1)                 // UPDATE_GAME_MODE: creative
                .writeBoolean(true)             // UPDATE_LISTED: show in tab list
                .writeVarInt(0));               // UPDATE_LATENCY: ping in ms
    }

    private static void spawnEntity(Connection to, Player player) throws IOException {
        to.send(0x01, out -> out
                .writeVarInt(player.entityId())
                .writeUUID(player.uuid())
                .writeVarInt(PLAYER_ENTITY_TYPE)
                .writeDouble(player.x()).writeDouble(player.y()).writeDouble(player.z())
                .writeByte(0)                   // velocity, low precision vec3, 0 = not moving
                .writeByte(angle(player.pitch()))
                .writeByte(angle(player.yaw()))
                .writeByte(angle(player.yaw())) // head yaw
                .writeVarInt(0));               // extra data, players don't have any
    }

    private static int angle(float degrees) {
        return (int) Math.floor(degrees * 256f / 360f);
    }
}
