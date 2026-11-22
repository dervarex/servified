import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This is a simple program that listens on port 25565. <br>
 * When you connect to "localhost" in minecraft, it'll print a few bytes, they come from minecraft :)
 */
public class ByteServer {
    public static void main(String[] args) throws Exception {
        try (ServerSocket server = new ServerSocket(25565)) {
            System.out.println("Listening on port 25565...");

            while (true) {
                Socket client = server.accept();

                Thread.ofVirtual().start(() -> {
                    try (client) {
                        System.out.println("\nClient connected: " + client.getInetAddress());

                        InputStream in = client.getInputStream();
                        int b;

                        while ((b = in.read()) != -1) {
                            System.out.printf("%02X ", b);
                        }

                        System.out.println("\nClient disconnected.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}