package com.dervarex.servified.registry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry names of every registry we know, see scripts/generate-data.py
 */
public final class Registries {

    /** Registries the client gets as Registry Data */
    private final Map<String, List<String>> synced = new LinkedHashMap<>();
    /** Built into the game, we only need their ids for tags and stuff */
    private final Map<String, List<String>> builtIn = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> ids = new HashMap<>();

    private Registries() {
    }

    public static Registries load() throws IOException {
        Registries registries = new Registries();

        for (String registry : lines("/registries/synced.txt"))
            registries.synced.put("minecraft:" + registry, lines("/registries/" + registry + ".txt"));

        for (String registry : lines("/registries/static.txt"))
            registries.builtIn.put("minecraft:" + registry, lines("/registries/" + registry + ".txt"));

        registries.indexIds(registries.synced);
        registries.indexIds(registries.builtIn);

        return registries;
    }

    public Map<String, List<String>> synced() {
        return synced;
    }

    /** -1 if the registry or entry doesn't exist (uh... not good) */
    public int id(String registry, String entry) {
        return ids.getOrDefault(registry, Map.of()).getOrDefault(entry, -1);
    }

    private void indexIds(Map<String, List<String>> registries) {
        registries.forEach((registry, entries) -> {
            Map<String, Integer> byName = new HashMap<>();

            for (int i = 0; i < entries.size(); i++)
                byName.put(entries.get(i), i);

            ids.put(registry, byName);
        });
    }

    static List<String> lines(String path) throws IOException {
        try (InputStream in = Registries.class.getResourceAsStream(path)) {
            if (in == null)
                throw new IOException(path + " missing in resources, run scripts/generate-data.py");

            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .lines()
                    .filter(line -> !line.isBlank())
                    .toList();
        }
    }
}
