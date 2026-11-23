package com.dervarex.servified.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Writes Minecraft's data types into a byte array that grows when needed <br>
 * Every method returns {@code this}, so you can write a whole packet in one chain:
 * <pre>{@code
 * out.writeUUID(uuid).writeString(name).writeVarInt(0);
 * }</pre>
 */
public final class PacketOutput {

    private byte[] data;
    private int size;

    public PacketOutput() {
        this(64);
    }

    public PacketOutput(int initialCapacity) {
        this.data = new byte[Math.max(initialCapacity, 16)];
    }

    public PacketOutput writeByte(int value) {
        ensure(1);
        data[size++] = (byte) value;
        return this;
    }

    public PacketOutput writeBoolean(boolean value) {
        return writeByte(value ? 1 : 0);
    }

    public PacketOutput writeShort(int value) {
        ensure(2);
        data[size++] = (byte) (value >>> 8);
        data[size++] = (byte) value;
        return this;
    }

    public PacketOutput writeInt(int value) {
        ensure(4);
        data[size++] = (byte) (value >>> 24);
        data[size++] = (byte) (value >>> 16);
        data[size++] = (byte) (value >>> 8);
        data[size++] = (byte) value;
        return this;
    }

    public PacketOutput writeLong(long value) {
        return writeInt((int) (value >>> 32)).writeInt((int) value);
    }

    public PacketOutput writeFloat(float value) {
        return writeInt(Float.floatToIntBits(value));
    }

    public PacketOutput writeDouble(double value) {
        return writeLong(Double.doubleToLongBits(value));
    }

    public PacketOutput writeVarInt(int value) {
        do {
            int temp = value & 0x7F;
            value >>>= 7;

            if (value != 0)
                temp |= 0x80;

            writeByte(temp);
        } while (value != 0);

        return this;
    }

    public PacketOutput writeVarLong(long value) {
        do {
            int temp = (int) (value & 0x7F);
            value >>>= 7;

            if (value != 0)
                temp |= 0x80;

            writeByte(temp);
        } while (value != 0);

        return this;
    }

    /** Writes the length in UTF-8 bytes (not characters) and then the bytes */
    public PacketOutput writeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return writeVarInt(bytes.length).writeBytes(bytes);
    }

    public PacketOutput writeUUID(UUID value) {
        return writeLong(value.getMostSignificantBits()).writeLong(value.getLeastSignificantBits());
    }

    public PacketOutput writeBytes(byte[] bytes) {
        ensure(bytes.length);
        System.arraycopy(bytes, 0, data, size, bytes.length);
        size += bytes.length;
        return this;
    }

    public int size() {
        return size;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(data, size);
    }

    private void ensure(int extra) {
        if (size + extra > data.length)
            data = Arrays.copyOf(data, Math.max(data.length * 2, size + extra));
    }
}
