/**
 * The network layer of Servified.
 * It handles sockets, packets adn minecraft data types
 * <br><br>
 * <h3>Quick Start</h3>
 * {@snippet :
 * PacketServer server = new PacketServer(25565);
 *
 * server.on(State.HANDSHAKE, 0x00, (conn, in) -> {
 *     in.readVarInt();          // protocol version
 *     in.readString(255);       // address
 *     in.readUnsignedShort();   // port
 *     conn.setState(in.readVarInt() == 1 ? State.STATUS : State.LOGIN);
 * });
 *
 * server.on(State.STATUS, 0x00, (conn, in) ->
 *         conn.send(0x00, out -> out.writeString(statusJson)));
 *
 * server.on(State.STATUS, 0x01, (conn, in) ->
 *         conn.send(0x01, out -> out.writeLong(in.readLong())));
 *
 * server.start(); // blocks forever
 * }
 * <br><br>
 * <table class="striped">
 *   <caption>Packet Data Types</caption>
 *   <thead>
 *     <tr>
 *       <th>Wiki type</th>
 *       <th>Method</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>Boolean</td>
 *       <td>{@code readBoolean()}</td>
 *     </tr>
 *     <tr>
 *       <td>Byte / Unsigned Byte</td>
 *       <td>{@code readByte()} / {@code readUnsignedByte()}</td>
 *     </tr>
 *     <tr>
 *       <td>Short / Unsigned Short</td>
 *       <td>{@code readShort()} / {@code readUnsignedShort()}</td>
 *     </tr>
 *     <tr>
 *       <td>Int, Long, Float, Double</td>
 *       <td>{@code readInt()}, {@code readLong()}, {@code readFloat()}, {@code readDouble()}</td>
 *     </tr>
 *     <tr>
 *       <td>VarInt / VarLong</td>
 *       <td>{@code readVarInt()} / {@code readVarLong()}</td>
 *     </tr>
 *     <tr>
 *       <td>String (n)</td>
 *       <td>{@code readString(n)} ({@code readString()} uses the default of 32767)</td>
 *     </tr>
 *     <tr>
 *       <td>Identifier</td>
 *       <td>{@code readString()} (it's just a string)</td>
 *     </tr>
 *     <tr>
 *       <td>UUID</td>
 *       <td>{@code readUUID()}</td>
 *     </tr>
 *     <tr>
 *       <td>Byte Array / rest of the packet</td>
 *       <td>{@code readBytes(n)} / {@code readRemaining()}</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * - Since the packet is already in memory, reading won't block you
 * - If you read further than the end you'll get a ProtocolException
 * - remaining() tells you how many bytes are left, for example if you're not sure whether you read everything
 *<br>
 * <b>Sending: {@code Connection.send}</b>
 * {@snippet :
 * conn.send(0x02, out -> out
 *         .writeUUID(uuid)
 *         .writeString(name)
 *         .writeVarInt(0));
 * }
 * ^ this is thread safe
 * <br><br>
 * <b>Changing State</b> <br><br>
 * {@code conn.setState(State.X)}<br>
 * You decide when this happens, in your handler, exactly at the point the protocol says so: <br>
 * - the handshake's next state -> STATUS or LOGIN <br>
 * - Login Acknowledged -> CONFIGURATION <br>
 * - Acknowledge Finish Configuration -> PLAY <br>
 * <br>
 * If you forget it, the next packets end up in the wrong state's handlers, the "Unhandled ..." log usually should tell you.
 * <br><br>
 * <b>Hooks</b>
 * {@snippet :
 * server.onConnect(conn -> ...);
 * server.onDisconnect(conn -> ...);
 * server.onUnhandled((conn, id, in) -> ...);  // default: prints "Unhandled LOGIN packet: 0x00 (25 bytes)"
 * }
 * <br><br>
 * <b>Automatic Handling</b>
 * - Legacy ping: if the first byte of a connection is 0xFE (the old server list ping from 1.6, which modern clients send as a fallback to check if the server speaks the old protocol), the connection gets closed. <br>
 * - Packet size limits: packets longer than 2,097,151 bytes, or with a length of 0, are rejected (something must have went wrong there) <br>
 * - Broken data: a VarInt that's too long, a string over its limit, or a packet that ends early throws a ProtocolException, which gets logged, and the connection is closed. <br>
 * - Threads: every client gets its own virtual thread, so handlers can simply block (file I/O and so on) without slowing down other players. Not really faster than vanilla but it works<br>
 */
package com.dervarex.servified.protocol;
