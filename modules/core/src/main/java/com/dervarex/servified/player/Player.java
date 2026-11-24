package com.dervarex.servified.player;

import com.dervarex.servified.protocol.Connection;

import java.util.UUID;

/**
 * Someone who's logged in, and where they are
 */
public final class Player {

    private final String name;
    private final UUID uuid;
    private final Connection connection;

    // only the player's own connection thread writes these
    private volatile double x, y, z;
    private volatile float yaw, pitch;
    private volatile boolean onGround;

    private int lastChunkX, lastChunkZ;

    public Player(String name, UUID uuid, Connection connection) {
        this.name = name;
        this.uuid = uuid;
        this.connection = connection;
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    /** Chunk coordinates are block coordinates / 16, rounded down (so -1 is chunk -1, not 0) */
    public int chunkX() {
        return Math.floorDiv((int) Math.floor(x), 16);
    }

    public int chunkZ() {
        return Math.floorDiv((int) Math.floor(z), 16);
    }

    /** True once every time the player walks into another chunk */
    public boolean enteredNewChunk() {
        if (chunkX() == lastChunkX && chunkZ() == lastChunkZ)
            return false;

        lastChunkX = chunkX();
        lastChunkZ = chunkZ();
        return true;
    }

    public String name() {
        return name;
    }

    public UUID uuid() {
        return uuid;
    }

    public Connection connection() {
        return connection;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public boolean onGround() {
        return onGround;
    }
}
