package com.dervarex.servified;

import com.dervarex.servified.handler.ConfigurationHandler;
import com.dervarex.servified.handler.HandshakeHandler;
import com.dervarex.servified.handler.LoginHandler;
import com.dervarex.servified.handler.MovementHandler;
import com.dervarex.servified.handler.PlayHandler;
import com.dervarex.servified.handler.StatusHandler;
import com.dervarex.servified.player.Player;
import com.dervarex.servified.player.PlayerSync;
import com.dervarex.servified.player.Players;
import com.dervarex.servified.protocol.PacketServer;
import com.dervarex.servified.registry.Registries;

import java.io.IOException;

public class Main {
    static void main() throws IOException {
        PacketServer server = new PacketServer(25565);
        Registries registries = Registries.load();
        Players players = new Players();

        HandshakeHandler.register(server);
        StatusHandler.register(server);
        LoginHandler.register(server, players);
        ConfigurationHandler.register(server, registries);
        PlayHandler.register(server, registries, players);
        MovementHandler.register(server, players);

        server.onConnect(conn -> System.out.println("Client connected: " + conn.remoteAddress()));
        server.onDisconnect(conn -> {
            Player player = players.remove(conn);

            if (player != null)
                PlayerSync.leave(player, players);

            System.out.println("Client disconnected: " + (player != null ? player.name() : conn.remoteAddress()));
        });

        server.start();
    }
}
