
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.UUID;

/**
 * This is a simple program that listens on port 25565 and tells us what minecraft wants to tell us, but readable <br>
 * When you connect to "localhost" in minecraft, it'll print a few things, they come from minecraft :) <br>
 * Additionally, it'll send information to your game client, like the online player count or a motd
 */
public class ByteServer {

    static String STATUS_JSON;

    static void loadStatus() throws Exception {
        byte[] icon = Files.readAllBytes(
                Path.of(ByteServer.class.getProtectionDomain()
                                .getCodeSource().getLocation().toURI())
                        .resolve("server-icon.png")
        );

        String base64 = Base64.getEncoder().encodeToString(icon);

        STATUS_JSON = """
            {
              "version": { "name": "servified", "protocol": 777 },
              "players": {
                "max": 20,
                "online": 0,
                "sample": [
                  {
                    "name": "dervarex",
                    "id": "a31ccf30-00e4-4928-a590-e366c90af710"
                  }
                ]
              },
              "description": {
                "text": "servified",
                "color": "gold",
                "extra": [
                  { "text": " • not normal", "color": "gray" }
                ]
              },
              "favicon": "data:image/png;base64,%s"
            }
            """.formatted(base64);
    }

    static int readVarInt(InputStream in) throws IOException {
        int result = 0;
        int numRead = 0;
        int currentByte;

        do {
            currentByte = in.read();

            if (currentByte == -1)
                throw new EOFException();

            result |= (currentByte & 0x7F) << (7 * numRead);

            if (++numRead > 5)
                throw new IOException("VarInt too big");

        } while ((currentByte & 0x80) != 0);

        return result;
    }

    static void writeVarInt(OutputStream out, int value) throws IOException {
        do {
            int temp = value & 0x7F;
            value >>>= 7;

            if (value != 0)
                temp |= 0x80;

            out.write(temp);
        } while (value != 0);
    }

    static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);

        if (length < 0 || length > 32767)
            throw new IOException("Invalid string length");

        byte[] bytes = new byte[length];
        in.readFully(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Send a framed Minecraft packet
    static void sendPacket(OutputStream out, byte[] data) throws IOException {
        writeVarInt(out, data.length);
        out.write(data);
        out.flush();
    }

    // Status Response (0x00)
    static void sendStatusResponse(OutputStream out) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        writeVarInt(buffer, 0x00);

        byte[] json = STATUS_JSON.getBytes(StandardCharsets.UTF_8);

        writeVarInt(buffer, json.length);
        buffer.write(json);

        sendPacket(out, buffer.toByteArray());

        System.out.println("Sent Status Response");
    }

    // Ping Response (0x01)
    static void sendPingResponse(OutputStream out, long timestamp)
            throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(buffer);

        writeVarInt(data, 0x01);
        data.writeLong(timestamp);

        sendPacket(out, buffer.toByteArray());

        System.out.println("Sent Ping Response: " + timestamp);
    }

    enum State { HANDSHAKE, STATUS, LOGIN, CONFIGURATION, PLAY }

    /** Everything we know about a connected client */
    static class Connection {
        final Socket socket;
        final DataInputStream in;
        final OutputStream out;
        State state = State.HANDSHAKE;

        Connection(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = socket.getOutputStream();
        }
    }

    /** Reads one framed packet */
    static DataInputStream readFrame(DataInputStream in) throws IOException {
        int length = readVarInt(in);

        if (length <= 0 || length > 2_097_152)
            throw new IOException("Invalid packet length: " + length);

        byte[] frame = new byte[length];
        in.readFully(frame);

        return new DataInputStream(new ByteArrayInputStream(frame));
    }

    static void handleClient(Socket client) {
        try (client) {
            Connection conn = new Connection(client);

            System.out.println("Client connected: " + client.getInetAddress());

            while (true) {
                DataInputStream packet = readFrame(conn.in);
                int packetId = readVarInt(packet);

                switch (conn.state) {
                    case HANDSHAKE -> handleHandshake(conn, packetId, packet);
                    case STATUS -> handleStatus(conn, packetId, packet);
                    case LOGIN -> handleLogin(conn, packetId, packet);
                    case CONFIGURATION -> handleConfiguration(conn, packetId, packet);
                    case PLAY -> logUnknown(conn, packetId);
                }

                if (conn.socket.isClosed())
                    break;
            }

        } catch (EOFException e) {
            // Normal client disconnect
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }

        System.out.println("Client disconnected.");
    }

    static void handleHandshake(Connection conn, int packetId, DataInputStream packet) throws IOException {
        if (packetId != 0x00)
            throw new IOException("Expected Handshake");

        int protocol = readVarInt(packet);
        String address = readString(packet);
        int port = packet.readUnsignedShort();
        int nextState = readVarInt(packet);

        System.out.printf(
                "Handshake: protocol=%d, address=%s, port=%d, nextState=%d%n",
                protocol, address, port, nextState
        );

        conn.state = switch (nextState) {
            case 1 -> State.STATUS;
            case 2 -> State.LOGIN;
            default -> throw new IOException("Unsupported next state: " + nextState); // 3 = transfer, later
        };
    }

    static void handleStatus(Connection conn, int packetId, DataInputStream packet) throws IOException {
        switch (packetId) {
            case 0x00 -> {
                // Status Request
                System.out.println("Received Status Request");
                sendStatusResponse(conn.out);
            }

            case 0x01 -> {
                // Ping Request
                long timestamp = packet.readLong();

                System.out.println("Received Ping Request");
                sendPingResponse(conn.out, timestamp);
            }

            default -> logUnknown(conn, packetId);
        }
    }

    static void handleLogin(Connection conn, int packetId, DataInputStream packet) throws IOException {
        switch (packetId) {
            case 0x00 -> {
                // Login Start
                String username = readString(packet);

                UUID uuid = new UUID(
                        packet.readLong(),
                        packet.readLong()
                );

                System.out.println("Login Start:");
                System.out.println("  Username: " + username);
                System.out.println("  UUID: " + uuid);

                // Login Success (0x02)
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream data = new DataOutputStream(buffer);

                writeVarInt(data, 0x02);

                // Game Profile
                data.writeLong(uuid.getMostSignificantBits());
                data.writeLong(uuid.getLeastSignificantBits());

                byte[] name = username.getBytes(StandardCharsets.UTF_8);
                writeVarInt(data, name.length);
                data.write(name);

                // Properties: empty array
                writeVarInt(data, 0);

                // Session ID
                data.writeLong(0);
                data.writeLong(0);

                sendPacket(conn.out, buffer.toByteArray());

                System.out.println("Sent Login Success to " + username);
            }

            case 0x03 -> {
                // Login Acknowledged
                conn.state = State.CONFIGURATION;

                System.out.println("Login Acknowledged");
                System.out.println("Switched to CONFIGURATION");
            }

            default -> logUnknown(conn, packetId);
        }
    }

    static void handleConfiguration(Connection conn, int packetId, DataInputStream packet) throws IOException {
        switch (packetId) {
            default -> logUnknown(conn, packetId);
        }
    }

    static void logUnknown(Connection conn, int packetId) {
        System.out.printf("Unhandled %s packet: 0x%02X%n", conn.state, packetId);
    }

    public static void main(String[] args) throws Exception {
        loadStatus();

        try (ServerSocket server = new ServerSocket(25565)) {
            System.out.println("Listening on port 25565...");

            while (true) {
                Socket client = server.accept();

                Thread.ofVirtual().start(() -> handleClient(client));
            }
        }
    }
}
