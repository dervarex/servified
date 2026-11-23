package com.dervarex.servified.protocol;

import java.io.IOException;

/**
 * Handles one kind of serverbound packet, {@code in} starts after the packet id
 */
@FunctionalInterface
public interface PacketHandler {

    void handle(Connection conn, PacketInput in) throws IOException;
}
