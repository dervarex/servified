package com.dervarex.servified.handler;

import com.dervarex.servified.protocol.PacketServer;
import com.dervarex.servified.protocol.ProtocolException;
import com.dervarex.servified.protocol.State;

public final class HandshakeHandler {

    private HandshakeHandler() {
    }

    public static void register(PacketServer server) {
        server.on(State.HANDSHAKE, 0x00, (conn, in) -> {
            int protocol = in.readVarInt();
            String address = in.readString(255);
            int port = in.readUnsignedShort();
            int nextState = in.readVarInt();

            System.out.printf("Handshake: protocol=%d, address=%s, port=%d, nextState=%d%n",
                    protocol, address, port, nextState);

            conn.setState(switch (nextState) {
                case 1 -> State.STATUS;
                case 2 -> State.LOGIN;
                default -> throw new ProtocolException("Unsupported next state: " + nextState); // 3 = transfer, later
            });
        });
    }
}
