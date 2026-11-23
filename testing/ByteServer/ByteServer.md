# ByteServer

Output:
```
Client connected: /127.0.0.1
10 00 89 06 09 6C 6F 63 61 6C 68 6F 73 74 63 DD 02 1A 00 08 64 65 72 76 61 72 65 78 A3 1C CF 30 00 E4 49 28 A5 90 E3 66 C9 0A F7 10
Client disconnected.
```

Lets analyze those bytes:

## Packet 1: Handshake

10 | 00 | 89 06 | 09 6C 6F 63 61 6C 68 6F 73 74 | 63 DD | 02

| Bytes                      | Meaning                                                                                             |
|----------------------------|------------------------------------------------------------------------------------------------------|
| 10                         | packet length: 16 bytes follow                                                                       |
| 00                         | packet ID 0 = Handshake                                                                              |
| 89 06                      | protocol version as VarInt = 777 (decode below)                                                      |
| 09                         | string length: 9 bytes                                                                               |
| 6C 6F 63 61 6C 68 6F 73 74 | localhost                                                                                            |
| 63 DD                      | port, unsigned short: 0x63DD = 25565                                                                |
| 02                         | next state = 2 = LOGIN, because you pressed Join. Refreshing the list will send 01 (STATUS) instead|

1 + 2 + 1 + 9 + 2 + 1 = 16 = 0x10
So the first byte is correct here

## Packet 2: Login Start

1A | 00 | 08 64 65 72 76 61 72 65 78 | A3 1C CF 30 00 E4 49 28 A5 90 E3 66 C9 0A F7 10

| Bytes                      | Meaning                                                                                             |
|----------------------------|------------------------------------------------------------------------------------------------------|
| 1A                         | packet length: 26 bytes follow                                                                       |
| 00                         | packet ID 0 = Login Start here                                                                |
| 08 64 65 72 76 61 72 65 78 | username, here it's dervarex                                              |

**Last 16 Bytes are my UUID:**

A3 1C CF 30 00 E4 49 28 A5 90 E3 66 C9 0A F7 10
=
a31ccf30-00e4-4928-a590-e366c90af710


## The status exchange

After a handshake with nextState=1, the connection is in the STATUS state:
| Direction | ID | Packet | Contents   |
|---------------|----|-----------------|-------------|
| client -> you | 0x00 | Status Request | nothing (empty) |
| you -> client| 0x00 | Status Response| one string with json |
| client -> you | 0x01| Ping Request| one long|
| you -> client| 0x01| Ping Response   | same long, sent back|

The client uses the ping and pong round trip to measure the ping bars in the server list:

```
{
  "version": { "name": "servified", "protocol": 777 },
  "players": { "max": 20, "online": 0 },
  "description": { "text": "hii from servified!" }
}
```
- If protocol matches the clients protocol, the version looks normal, otherwise the client shows it as incompatible. Sending a wrong one on purpose is funny xD
- version.name is the text shown in red  in that case, so it can be anything you want, no matter if that's actually a minecraft version

## Customizing the server list entry
The status response also contains all the information displayed in the multiplayer menu, so we have a few things here that we can customize:

- version.name – version text displayed by minecraft
- version.protocol – the protocol version minecraft compares against its own. If it doesn't match, the server will be shown as incompatible
- players.max – the displayed maximum player count
- players.online – how many players currently are online
- players.sample – an optional list of players displayed when hovering over the player count (doesn't need to be who actually is on there)
- description – the server's MOTD, supports formatting, colors and text components

## Server icon

Minecraft also supports a custom server icon.

It expects a PNG image that is exactly 64x64 pixels, encoded as base64 and prefixed with `data:image/png;base64,`

Instead of re-encoding that everytime someone pings your server (trust me, that does matter once some people have you in their server list), we're encoding the image once and including it in the status JSON from then on.

## Colored MOTD

We can send an object with multiple components:

- text – the actual text
- color – the color of the text
- extra – additional components appended after it

## Responding to Minecraft

After we get the handshake, we check `nextState`.

If `nextState` is 1, we enter STATUS state and wait for packages.

We have two important packages:

### Status Request (0x00)

Empty package. When we recive it, we respond with a Status Response.

The response contains out JSON as a Minecraft string, just like the other strings we had, it starts with a VarInt containing the byte length followed by the actual data.

### Ping Request (0x01)

This packet contains a single 64-bit integer (long).

We read it using DataInputStream.readLong() and send back the exact same value, so the client knows we're alive and can measure the response time to calculate the ping.

## Login Success

Once the player sends Login Start, the server responds with a Login Success packet (0x02)

That tells Minecraft that the player has successfully logged in. The packet contains the player's UUID, username, an empty properties array and a Session ID.

After reciving Login Success, the client sends Login Acknowledged (0x03), configming that it's ready to continue.

The server then switches the connection state to CONFIGURATION.

## Configuration

The client now starts sending configuration packets, such as Client Information and the minecraft:brand plugin message

## Known Packs

After Login was acknowledged, the server sends a Known Packs packet 0x0F.

This tells the client which data packs the server already knows about.
Send:

- Namespace: `minecraft`
- ID: `core`
- Version: `26.3`

The client then responds with its own Known Packs packet (`0x07`), containing the packs it supports, we can compare that.

**Why??**

If both the server and client have `minecraft:core`, we can skip sending the registry data later, saves bandwidth and makes it easier for both.
We can send registry entries with `Has Data = false`, telling the client to use its own built-in data instead of downloading all assets.