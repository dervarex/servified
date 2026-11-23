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
 */
package com.dervarex.servified.protocol;
