package com.dervarex.servified.protocol;

/**
 * The state of a connection, the same packet id means a different packet in every state
 */
public enum State {
    HANDSHAKE,
    STATUS,
    LOGIN,
    CONFIGURATION,
    PLAY
}
