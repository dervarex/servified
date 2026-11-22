import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * This is a simple program that listens on port 25565 and tells us what minecraft wants to tell us, but readable <br>
 * When you connect to "localhost" in minecraft, it'll print a few bytes, they come from minecraft :)
 */
public class ByteServer {

    static int readVarInt(InputStream in) throws IOException {
        int result = 0;
        int numRead = 0;
        int currentByte;

        do {
            currentByte = in.read();

            if (currentByte == -1)
                throw new EOFException("Unexpected end of stream");

            result |= (currentByte & 0x7F) << (7 * numRead);

            numRead++;

            if (numRead > 5)
                throw new IOException("VarInt is too big");

        } while ((currentByte & 0x80) != 0);

        return result;
    }

    static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);

        byte[] bytes = new byte[length];
        in.readFully(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    static void handleClient(Socket client) {
        try (client;
             DataInputStream in = new DataInputStream(client.getInputStream())) {

            System.out.println("Client connected: " + client.getInetAddress());

            boolean firstPacket = true;

            while (true) {
                int length = readVarInt(in);

                if (length <= 0)
                    throw new IOException("Invalid packet length: " + length);

                byte[] frame = new byte[length];
                in.readFully(frame);

                DataInputStream packet = new DataInputStream(
                        new ByteArrayInputStream(frame)
                );

                int packetId = readVarInt(packet);

                if (firstPacket && packetId == 0) {
                    // Handshake packet
                    int protocol = readVarInt(packet);
                    String address = readString(packet);
                    int port = packet.readUnsignedShort();
                    int nextState = readVarInt(packet);

                    System.out.printf(
                            "Handshake: protocol=%d, address=%s, port=%d, nextState=%d%n",
                            protocol, address, port, nextState
                    );
                }

                firstPacket = false;
            }

        } catch (EOFException e) {
            // Client disconnected normally
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }

        System.out.println("Client disconnected.");
    }

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(25565)) {
            System.out.println("Listening on port 25565...");

            while (true) {
                Socket client = server.accept();

                Thread.ofVirtual().start(() -> handleClient(client));
            }
        }
    }
}