package com.dervarex.servified.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * VarInt helpers that work directly on streams, used to read the packet length
 * before the rest of the packet is in a {@link PacketInput}
 * <p>
 * Every byte holds 7 bits of data, lowest bits first. If the top bit (0x80) is set, another byte follows
 */
public final class VarInt {

    /** A VarInt is never longer than 5 bytes, if that happens mojang made some serious mistakes or you modded your client heavily */
    public static final int MAX_BYTES = 5;

    private VarInt() {
    }

    public static int read(InputStream in) throws IOException {
        int result = 0;
        int numRead = 0;
        int currentByte;

        do {
            currentByte = in.read();

            if (currentByte == -1)
                throw new EOFException();

            result |= (currentByte & 0x7F) << (7 * numRead);

            if (++numRead > MAX_BYTES)
                throw new ProtocolException("VarInt too big");

        } while ((currentByte & 0x80) != 0);

        return result;
    }

    /** How many bytes {@code value} takes as a VarInt */
    public static int size(int value) {
        int size = 1;

        while ((value & ~0x7F) != 0) {
            value >>>= 7;
            size++;
        }

        return size;
    }
}
