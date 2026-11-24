package com.dervarex.servified.registry;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tags are named groups of entries (#minecraft:in_overworld, #minecraft:enchantable/armor, ...) <br>
 * Client doesn't work without
 */
public final class Tags {

    /** Registry -> tag name -> entry ids */
    private final Map<String, Map<String, int[]>> tags = new LinkedHashMap<>();

    private Tags() {
    }

    /** Nested tags are already flattened by the generator, so every line is just tag name + entries basically */
    public static Tags load(Registries registries) throws IOException {
        Tags result = new Tags();

        for (String registry : Registries.lines("/tags/index.txt")) {
            String name = "minecraft:" + registry;
            Map<String, int[]> byTag = new LinkedHashMap<>();

            for (String line : Registries.lines("/tags/" + registry + ".txt")) {
                List<String> parts = Arrays.asList(line.split(" "));

                int[] ids = parts.subList(1, parts.size()).stream()
                        .mapToInt(entry -> registries.id(name, entry))
                        .filter(id -> id >= 0)
                        .toArray();

                byTag.put(parts.getFirst(), ids);
            }

            result.tags.put(name, byTag);
        }

        return result;
    }

    public Map<String, Map<String, int[]>> all() {
        return tags;
    }
}
