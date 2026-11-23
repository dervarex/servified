package com.dervarex.servified.protocol;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Reads Minecraft's data types from the body of one packet
 * <p>
 * Reading past the end throws a {@link ProtocolException}
 */
public final class PacketInput {

    /** Default max string length, in characters */
    public static final int MAX_STRING_LENGTH = 32767;

    private final byte[] data;
    private int position;

    public PacketInput(byte[] data) {
        this.data = data;
    }

    public byte readByte() throws ProtocolException {
        require(1);
        return data[position++];
    }

    public int readUnsignedByte() throws ProtocolException {
        return readByte() & 0xFF;
    }

    public boolean readBoolean() throws ProtocolException {
        return readByte() != 0;
    }

    public short readShort() throws ProtocolException {
        require(2);
        return (short) ((data[position++] & 0xFF) << 8 | (data[position++] & 0xFF));
    }

    public int readUnsignedShort() throws ProtocolException {
        return readShort() & 0xFFFF;
    }

    public int readInt() throws ProtocolException {
        require(4);
        return (data[position++] & 0xFF) << 24
                | (data[position++] & 0xFF) << 16
                | (data[position++] & 0xFF) << 8
                | (data[position++] & 0xFF);
    }

    public long readLong() throws ProtocolException {
        return (long) readInt() << 32 | (readInt() & 0xFFFFFFFFL);
    }

    public float readFloat() throws ProtocolException {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws ProtocolException {
        return Double.longBitsToDouble(readLong());
    }

    public int readVarInt() throws ProtocolException {
        int result = 0;
        int numRead = 0;
        byte currentByte;

        do {
            currentByte = readByte();
            result |= (currentByte & 0x7F) << (7 * numRead);

            if (++numRead > VarInt.MAX_BYTES)
                throw new ProtocolException("VarInt too big");

        } while ((currentByte & 0x80) != 0);

        return result;
    }

    public long readVarLong() throws ProtocolException {
        long result = 0;
        int numRead = 0;
        byte currentByte;

        do {
            currentByte = readByte();
            result |= (long) (currentByte & 0x7F) << (7 * numRead);

            if (++numRead > 10)
                throw new ProtocolException("VarLong too big");

        } while ((currentByte & 0x80) != 0);

        return result;
    }

    public String readString() throws ProtocolException {
        return readString(MAX_STRING_LENGTH);
    }

    /** @param maxLength max length in characters, like the wiki lists it (String (16) for usernames for example) */
    public String readString(int maxLength) throws ProtocolException {
        int byteLength = readVarInt();

        // a character is max 3 bytes in UTF-8, the protocol counts in UTF-16 units
        if (byteLength < 0 || byteLength > maxLength * 3)
            throw new ProtocolException("String byte length " + byteLength + " over limit for " + maxLength + " chars");

        String value = new String(readBytes(byteLength), StandardCharsets.UTF_8);

        if (value.length() > maxLength)
            throw new ProtocolException("String length " + value.length() + " over limit " + maxLength);

        return value;
    }

    public UUID readUUID() throws ProtocolException {
        return new UUID(readLong(), readLong());
    }

    public byte[] readBytes(int length) throws ProtocolException {
        require(length);
        byte[] bytes = new byte[length];
        System.arraycopy(data, position, bytes, 0, length);
        position += length;
        return bytes;
    }

    /** Everything that's left I guess */
    public byte[] readRemaining() throws ProtocolException {
        return readBytes(remaining());
    }

    public int remaining() {
        return data.length - position;
    }

    private void require(int bytes) throws ProtocolException {
        if (bytes < 0 || remaining() < bytes)
            throw new ProtocolException("Packet ended early: needed " + bytes + " bytes, " + remaining() + " left");
    }
}
