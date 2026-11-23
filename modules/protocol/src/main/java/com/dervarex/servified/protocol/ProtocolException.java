package com.dervarex.servified.protocol;

import java.io.IOException;

/**
 * Thrown when a client sends something that doesn't follow the protocol,
 * like a VarInt that's too long, a string over its limit or a packet that ends too early
 * Didn't happen yet, but when it does then you're probably using the wrong version!
 */
public class ProtocolException extends IOException {

    public ProtocolException(String message) {
        super(message);
    }
}
