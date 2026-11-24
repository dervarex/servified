package com.dervarex.servified;

import com.dervarex.servified.handler.ConfigurationHandler;
import com.dervarex.servified.handler.HandshakeHandler;
import com.dervarex.servified.handler.LoginHandler;
import com.dervarex.servified.handler.PlayHandler;
import com.dervarex.servified.handler.StatusHandler;
import com.dervarex.servified.protocol.PacketServer;
import com.dervarex.servified.registry.Registries;

import java.io.IOException;

public class Main {
    static void main() throws IOException {
        PacketServer server = new PacketServer(25565);
        Registries registries = Registries.load();

        HandshakeHandler.register(server);
        StatusHandler.register(server);
        LoginHandler.register(server);
        ConfigurationHandler.register(server, registries);
        PlayHandler.register(server, registries);

        server.onConnect(conn -> System.out.println("Client connected: " + conn.remoteAddress()));
        server.onDisconnect(conn -> System.out.println("Client disconnected: " + conn.remoteAddress()));

        server.start();
    }
}
