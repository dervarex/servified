package com.dervarex.servified.protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * A connected client
 */
public final class Connection implements AutoCloseable {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final Object writeLock = new Object();

    private volatile State state = State.HANDSHAKE;

    Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = new BufferedOutputStream(socket.getOutputStream());
    }

    /**
     * Sends a packet, {@code writer} writes its fields
     * <pre>{@code
     * send(0x01, out -> out.writeLong(timestamp));
     * }</pre>
     */
    public void send(int packetId, PacketWriter writer) throws IOException {
        PacketOutput body = new PacketOutput();
        body.writeVarInt(packetId);
        writer.write(body);

        PacketOutput frame = new PacketOutput(body.size() + VarInt.MAX_BYTES);
        frame.writeVarInt(body.size());
        frame.writeBytes(body.toByteArray());

        synchronized (writeLock) {
            out.write(frame.toByteArray());
            out.flush();
        }
    }

    /** Sends a packet without any fields */
    public void send(int packetId) throws IOException {
        send(packetId, out -> {
        });
    }

    public State state() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public SocketAddress remoteAddress() {
        return socket.getRemoteSocketAddress();
    }

    public boolean isOpen() {
        return !socket.isClosed();
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // we're closing it anyway, so ignore that
        }
    }

    InputStream input() {
        return in;
    }

    @FunctionalInterface
    public interface PacketWriter {
        void write(PacketOutput out);
    }
}
