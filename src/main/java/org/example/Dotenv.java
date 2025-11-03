package org.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Very small .env loader: reads key=value lines from a .env file in the working directory.
 * It ignores empty lines and lines starting with #. Values may be quoted.
 */
public final class Dotenv {
    private static Map<String, String> cache = null;

    private Dotenv() {}

    public static synchronized void load() {
        if (cache != null) return;
        cache = new HashMap<>();
        Path p = Paths.get(".env");
        if (!Files.exists(p)) return;
        try {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                // strip surrounding quotes if present
                if ((val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2)
                        || (val.startsWith("'") && val.endsWith("'") && val.length() >= 2)) {
                    val = val.substring(1, val.length() - 1);
                }
                cache.put(key, val);
            }
        } catch (IOException e) {
            System.err.println("Gagal membaca .env: " + e.getMessage());
        }
    }

    public static String get(String key) {
        if (cache == null) load();
        return cache == null ? null : cache.get(key);
    }
}
