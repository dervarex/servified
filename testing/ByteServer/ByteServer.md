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

