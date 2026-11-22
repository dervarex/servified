
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;

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

    static void handleClient(Socket client) {
        try (client;
             DataInputStream in = new DataInputStream(client.getInputStream())) {

            OutputStream out = client.getOutputStream();

            System.out.println("Client connected: " + client.getInetAddress());

            // Read handshake
            int length = readVarInt(in);

            if (length <= 0 || length > 2_097_152)
                throw new IOException("Invalid handshake length");

            byte[] frame = new byte[length];
            in.readFully(frame);

            DataInputStream handshake = new DataInputStream(
                    new ByteArrayInputStream(frame)
            );

            int packetId = readVarInt(handshake);

            if (packetId != 0x00)
                throw new IOException("Expected Handshake");

            int protocol = readVarInt(handshake);
            String address = readString(handshake);
            int port = handshake.readUnsignedShort();
            int nextState = readVarInt(handshake);

            System.out.printf(
                    "Handshake: protocol=%d, address=%s, port=%d, nextState=%d%n",
                    protocol, address, port, nextState
            );

            if (nextState != 1) {
                System.out.println("Not a STATUS connection. Ignoring."); // not doing login here
                return;
            }

            // STATUS state
            while (true) {
                length = readVarInt(in);

                if (length <= 0 || length > 2_097_152)
                    throw new IOException("Invalid packet length");

                frame = new byte[length];
                in.readFully(frame);

                DataInputStream packet = new DataInputStream(
                        new ByteArrayInputStream(frame)
                );

                packetId = readVarInt(packet);

                switch (packetId) {
                    case 0x00 -> {
                        // Status Request
                        System.out.println("Received Status Request");
                        sendStatusResponse(out);
                    }

                    case 0x01 -> {
                        // Ping Request
                        long timestamp = packet.readLong();

                        System.out.println("Received Ping Request");
                        sendPingResponse(out, timestamp);
                    }

                    default -> System.out.printf(
                            "Unknown STATUS packet: 0x%02X%n", packetId
                    );
                }
            }

        } catch (EOFException e) {
            // Normal client disconnect
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }

        System.out.println("Client disconnected.");
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
