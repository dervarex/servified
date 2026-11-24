package com.dervarex.servified.player;

import com.dervarex.servified.protocol.Connection;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player[] but more thread safe
 */
public final class Players {

    private final Map<Connection, Player> byConnection = new ConcurrentHashMap<>();

    public void add(Player player) {
        byConnection.put(player.connection(), player);
    }

    /** null if that connection didn't log in (yet) */
    public Player get(Connection connection) {
        return byConnection.get(connection);
    }

    public Player remove(Connection connection) {
        return byConnection.remove(connection);
    }

    public Collection<Player> all() {
        return byConnection.values();
    }
}
