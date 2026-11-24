package com.dervarex.servified.world;

import com.dervarex.servified.protocol.PacketOutput;

/**
 * A chunk that's air everywhere except one layer of stone sections, real world would be kinda hard :( <br>
 * See 26.3's LevelChunkSection, PalettedContainer and ClientboundLevelChunkWithLightPacket, got some information from there
 */
public final class FlatChunk {

    /** Overworld is -64 – 320, -> 24 sections of 16 blocks (chunk sections) */
    public static final int SECTIONS = 24;
    /** The client wants light for one extra section below and above */
    private static final int LIGHT_SECTIONS = SECTIONS + 2;

    /** Block state ids from reports/blocks.json, they're not the same as block ids though!! */
    private static final int AIR = 0;
    private static final int STONE = 1;

    /** Section 7 is 48 to 63, so you'll stand at 64 */
    public static final int STONE_SECTION = 7;
    public static final int SURFACE_Y = -64 + (STONE_SECTION + 1) * 16;

    private final byte[] body;

    /** Every flat chunk is the same, so we'll only build it once and change x and z when sending later */
    public FlatChunk(int biome) {
        PacketOutput out = new PacketOutput(64 * 1024);

        out.writeVarInt(0); // heightmaps, the client figures them out itself, less work for us

        byte[] sections = sections(biome);
        out.writeVarInt(sections.length).writeBytes(sections);

        out.writeVarInt(0); // block entities

        light(out);

        this.body = out.toByteArray();
    }

    /** Fields after chunk x and z */
    public byte[] body() {
        return body;
    }

    private static byte[] sections(int biome) {
        PacketOutput out = new PacketOutput();

        for (int i = 0; i < SECTIONS; i++) {
            boolean stone = i == STONE_SECTION;

            out.writeShort(stone ? 16 * 16 * 16 : 0); // non air blocks
            out.writeShort(0); // fluids

            // block states: 0 bits per entry = single value palette, no data longs after it
            out.writeByte(0).writeVarInt(stone ? STONE : AIR);

            // biomes, same thing but biomes
            out.writeByte(0).writeVarInt(biome);
        }

        return out.toByteArray();
    }

    /** Full sky light everywhere, otherwise everything is black */
    private static void light(PacketOutput out) {
        out.writeVarInt(1).writeLong((1L << LIGHT_SECTIONS) - 1); // sky light mask, every section
        out.writeVarInt(0);                                             // block light mask
        out.writeVarInt(0);                                             // empty sky light mask
        out.writeVarInt(0);                                             // empty block light mask

        byte[] fullBright = new byte[2048];
        java.util.Arrays.fill(fullBright, (byte) 0xFF);

        out.writeVarInt(LIGHT_SECTIONS);
        for (int i = 0; i < LIGHT_SECTIONS; i++)
            out.writeVarInt(fullBright.length).writeBytes(fullBright);

        out.writeVarInt(0); // block light arrays
    }
}
