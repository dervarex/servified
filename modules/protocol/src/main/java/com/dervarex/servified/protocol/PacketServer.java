package com.dervarex.servified.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Accepts connections, reads packets and passes each one to the handler
 * registered for the connection's current state and the packet id
 * <pre>{@code
 * PacketServer server = new PacketServer(25565);
 * server.on(State.STATUS, 0x01, (conn, in) -> conn.send(0x01, out -> out.writeLong(in.readLong())));
 * server.start();
 * }</pre>
 */
public final class PacketServer {

    /** Biggest packet vanilla allows, 2^21 - 1 bytes (the max a 3 byte VarInt can hold) */
    public static final int MAX_PACKET_LENGTH = 2_097_151;

    /** First byte of the old pre-1.7 server list ping, modern clients still send it when the normal ping fails */
    private static final int LEGACY_PING = 0xFE;

    private final int port;
    private final Map<State, Map<Integer, PacketHandler>> handlers = new EnumMap<>(State.class);

    private Consumer<Connection> onConnect = conn -> {
    };
    private Consumer<Connection> onDisconnect = conn -> {
    };
    /** State + packet id combos we already printed, so we don't spam and can see what is still missing implementation */
    private final Set<String> reportedUnhandled = ConcurrentHashMap.newKeySet();

    private UnhandledListener onUnhandled = (conn, id, in) -> {
        if (reportedUnhandled.add(conn.state() + ":" + id))
            System.out.printf("Unhandled %s packet: 0x%02X (%d bytes), you gotta implement that one aswell%n", conn.state(), id, in.remaining());
    };

    public PacketServer(int port) {
        this.port = port;
    }

    /** Registers the handler for {@code packetId} in {@code state}, replaces the old one if there is one */
    public PacketServer on(State state, int packetId, PacketHandler handler) {
        handlers.computeIfAbsent(state, s -> new HashMap<>()).put(packetId, handler);
        return this;
    }

    public PacketServer onConnect(Consumer<Connection> listener) {
        this.onConnect = listener;
        return this;
    }

    public PacketServer onDisconnect(Consumer<Connection> listener) {
        this.onDisconnect = listener;
        return this;
    }

    /** Gets called for packets without a handler, by default it prints every packet id once */
    public PacketServer onUnhandled(UnhandledListener listener) {
        this.onUnhandled = listener;
        return this;
    }

    /** Starts accepting connections and blocks forever, every client gets its own virtual thread */
    public void start() throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Listening on port " + port + "...");

            while (true) {
                Socket socket = server.accept();
                Thread.ofVirtual().start(() -> handle(socket));
            }
        }
    }

    private void handle(Socket socket) {
        try (Connection conn = new Connection(socket)) {
            onConnect.accept(conn);

            try {
                if (isLegacyPing(conn.input()))
                    return;

                while (conn.isOpen())
                    dispatch(conn, readFrame(conn.input()));

            } catch (EOFException e) {
                // normal disconnect
            } catch (IOException e) {
                if (conn.isOpen()) // if we closed it ourselves, it's not an error, it's our fault
                    System.err.println("Client error (" + conn.remoteAddress() + "): " + e.getMessage());
            } finally {
                onDisconnect.accept(conn);
            }

        } catch (IOException e) {
            System.err.println("Could not set up connection: " + e.getMessage());
        }
    }

    private void dispatch(Connection conn, PacketInput in) throws IOException {
        int packetId = in.readVarInt();
        PacketHandler handler = handlers.getOrDefault(conn.state(), Map.of()).get(packetId);

        if (handler == null)
            onUnhandled.unhandled(conn, packetId, in);
        else
            handler.handle(conn, in);
    }

    private static PacketInput readFrame(InputStream in) throws IOException {
        int length = VarInt.read(in);

        if (length <= 0 || length > MAX_PACKET_LENGTH)
            throw new ProtocolException("Invalid packet length: " + length);

        byte[] frame = in.readNBytes(length);

        if (frame.length < length)
            throw new EOFException();

        return new PacketInput(frame);
    }

    private static boolean isLegacyPing(InputStream in) throws IOException {
        in.mark(1);
        int first = in.read();
        in.reset();
        return first == LEGACY_PING;
    }

    @FunctionalInterface
    public interface UnhandledListener {
        void unhandled(Connection conn, int packetId, PacketInput in) throws IOException;
    }
}
