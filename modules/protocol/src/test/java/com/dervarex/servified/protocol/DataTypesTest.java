package com.dervarex.servified.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DataTypesTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 127, 128, 255, 777, 25565, 2_097_151, Integer.MAX_VALUE, -1, Integer.MIN_VALUE})
    void varIntRoundTrip(int value) throws Exception {
        byte[] bytes = new PacketOutput().writeVarInt(value).toByteArray();

        assertEquals(VarInt.size(value), bytes.length);
        assertEquals(value, new PacketInput(bytes).readVarInt());
        assertEquals(value, VarInt.read(new ByteArrayInputStream(bytes)));
    }

    @Test
    void varIntMatchesWikiExamples() {
        assertArrayEquals(bytes(0x89, 0x06), new PacketOutput().writeVarInt(777).toByteArray());
        assertArrayEquals(bytes(0xFF, 0xFF, 0xFF, 0xFF, 0x0F), new PacketOutput().writeVarInt(-1).toByteArray());
    }

    @Test
    void varIntTooLong() {
        PacketInput in = new PacketInput(bytes(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01));
        assertThrows(ProtocolException.class, in::readVarInt);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 300, Long.MAX_VALUE, -1, Long.MIN_VALUE})
    void varLongRoundTrip(long value) throws Exception {
        assertEquals(value, new PacketInput(new PacketOutput().writeVarLong(value).toByteArray()).readVarLong());
    }

    @Test
    void primitivesRoundTrip() throws Exception {
        byte[] bytes = new PacketOutput()
                .writeBoolean(true).writeByte(-5).writeShort(25565).writeInt(-123456)
                .writeLong(Long.MIN_VALUE).writeFloat(1.5f).writeDouble(-0.25)
                .toByteArray();

        PacketInput in = new PacketInput(bytes);
        assertTrue(in.readBoolean());
        assertEquals(-5, in.readByte());
        assertEquals(25565, in.readUnsignedShort());
        assertEquals(-123456, in.readInt());
        assertEquals(Long.MIN_VALUE, in.readLong());
        assertEquals(1.5f, in.readFloat());
        assertEquals(-0.25, in.readDouble());
        assertEquals(0, in.remaining());
    }

    @Test
    void stringAndUuid() throws Exception {
        UUID uuid = UUID.fromString("a31ccf30-00e4-4928-a590-e366c90af710");
        byte[] bytes = new PacketOutput().writeString("hii from servified • öäü").writeUUID(uuid).toByteArray();

        PacketInput in = new PacketInput(bytes);
        assertEquals("hii from servified • öäü", in.readString());
        assertEquals(uuid, in.readUUID());
    }

    @Test
    void stringOverLimit() {
        PacketInput in = new PacketInput(new PacketOutput().writeString("a_very_very_very_very_long_username").toByteArray());
        assertThrows(ProtocolException.class, () -> in.readString(16));
    }

    @Test
    void readingPastEndThrows() {
        assertThrows(ProtocolException.class, () -> new PacketInput(bytes(0x00, 0x01)).readInt());
    }

    private static byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++)
            bytes[i] = (byte) values[i];
        return bytes;
    }
}
