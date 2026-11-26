<h1 align="center">
  <br>
  <img width="150" src="logo.svg" alt="Servified Logo"> 
  <br>
  <p align="center">Servified</p>
</h1>

<p align="center">
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=for-the-badge" alt="License">
  </a>
</p>

<p align="center">
    A Custom Minecraft Server Implementation
</p>

---

## Features

* Send server info to the client
* Transmit a Basic Stone Chunk
* Display other players that joined the Server in 3D, with mostly accurate movements

## Versions
**Target:** 26.3
**Tested:** 26.3

## Running it

1. Requirements
- JDK 25. Check with java -version if you're not sure.
- Internet access
- A Minecraft Java Edition 26.3 Client. (no other version will work, it'll show as incompatible)
- Git

2. Get the code and build it
```
git clone https://github.com/dervarex/servified
cd servified
./gradlew build
```
On Windows, use gradlew.bat instead of ./gradlew.
```
gradlew.bat build
```

3. Start the Server
```
./gradlew :core:run
```

4. Connect
4.1 Open Minecraft 26.3
4.2 Enter localhost as the server address, since you're hosting the server on your machine
4.3 The Server should show up and be joinable.

## License

Licensed under the Apache License 2.0.

See the LICENSE file for details.

# Disclaimer

This project is not an official Minecraft service and is not approved by or associated with Mojang or Microsoft.
No Minecraft code or assets (textures, sounds, models, ...) are included in the project. The only thing taken from the game are the names of registry entries and tags in modules/core/src/main/resources, which the client needs to be able to join. See scripts/generate-data.py for how it works.
