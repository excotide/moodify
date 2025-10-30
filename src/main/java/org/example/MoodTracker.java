// File: src/main/java/org/example/MoodTracker.java
package org.example;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MoodTracker {
    private final List<MoodEntry> moodData = new ArrayList<>();
    private final String filePath;

    public MoodTracker(String filePath) {
        this.filePath = filePath;
        loadFromLocal();
    }

    // Centralized mapping: mood text <-> numeric score
    // 1: Kacau, 2: Buruk, 3: Netral, 4: Bagus, 5: Sangat bagus
    public static int scoreForMood(String mood) {
        if (mood == null) return 3;
        String m = mood.trim().toLowerCase();
        return switch (m) {
            case "kacau" -> 1;
            case "buruk" -> 2;
            case "netral" -> 3;
            case "bagus" -> 4;
            case "sangat bagus", "sangat_bagus", "sangat-bagus", "sangatbagus" -> 5;
            default -> 3; // fallback neutral for unknown
        };
    }

    public static String moodForScore(int score) {
        return switch (score) {
            case 1 -> "Kacau";
            case 2 -> "Buruk";
            case 3 -> "Netral";
            case 4 -> "Bagus";
            case 5 -> "Sangat bagus";
            default -> "Netral";
        };
    }

    // Load existing entries from CSV file. Support both legacy (date,mood) and new (date,mood,score)
    private void loadFromLocal() {
        if (filePath == null || filePath.isEmpty()) return;
        Path p = Paths.get(filePath);
        if (!Files.exists(p)) return;
        try {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                try {
                    LocalDate date = LocalDate.parse(parts[0].trim());
                    String mood = parts[1].trim();
                    int score;
                    if (parts.length >= 3) {
                        try {
                            score = Integer.parseInt(parts[2].trim());
                        } catch (NumberFormatException nfe) {
                            score = scoreForMood(mood);
                        }
                    } else {
                        score = scoreForMood(mood);
                    }
                    moodData.add(new MoodEntry(mood, score, date));
                } catch (DateTimeParseException ex) {
                    // skip malformed lines
                }
            }
        } catch (IOException ex) {
            System.err.println("Failed to load moods: " + ex.getMessage());
        }
    }

    public void inputMood(String mood, LocalDate date) {
        int score = scoreForMood(mood);
        moodData.add(new MoodEntry(mood, score, date));
    }

    // Save CSV lines: date,mood,score
    public void saveToLocal() {
        if (filePath == null || filePath.isEmpty()) {
            System.out.println("No file path configured for saving.");
            return;
        }
        try {
            Path p = Paths.get(filePath);
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            List<String> lines = new ArrayList<>();
            for (MoodEntry e : moodData) {
                lines.add(e.getDate() + "," + e.getMood() + "," + e.getScore());
            }
            Files.write(p, lines, StandardCharsets.UTF_8);
            System.out.println("Saved " + lines.size() + " entries to " + filePath);
        } catch (IOException ex) {
            System.err.println("Failed to save moods: " + ex.getMessage());
        }
    }

    // Calculate stats for the last 7 days (including today)
    public WeeklyStats calculateWeeklyStats() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        int positive = 0; // score >= 4
        int negative = 0; // score <= 2
        int total = 0;
        int sumScores = 0;
        Map<String, Integer> counts = new HashMap<>();

        for (MoodEntry e : moodData) {
            if (e.getDate().isBefore(start) || e.getDate().isAfter(today)) continue;
            int s = e.getScore();
            String m = e.getMood();
            counts.put(m, counts.getOrDefault(m, 0) + 1);
            if (s >= 4) positive++;
            else if (s <= 2) negative++;
            sumScores += s;
            total++;
        }

        Optional<Map.Entry<String, Integer>> dom = counts.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue));
        String dominant = dom.map(Map.Entry::getKey).orElse("");
        double avg = total == 0 ? 0.0 : (double) sumScores / total;

        return new WeeklyStats(positive, negative, dominant, total, avg);
    }

    public void displayWeeklyGraph() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        WeeklyStats stats = calculateWeeklyStats();
        System.out.println("Statistik 7 hari terakhir (" + start + " s/d " + today + ")");
        System.out.println("Dominant mood: " + (stats.calculateDominantMood().isEmpty() ? "(tidak ada data)" : stats.calculateDominantMood()));
        System.out.printf("Total input: %d, Rata-rata skor: %.2f (%s)\n",
            stats.getTotalCount(), stats.getAverageScore(),
            stats.getTotalCount() == 0 ? "-" : stats.getAverageMoodLabel());
        System.out.printf("Positive: %d, Negative: %d, Positive ratio: %.2f\n",
            stats.getTotalPositive(), stats.getTotalNegative(), stats.getPositiveRatio());

        // Weekly distribution bar chart
        Map<String, Integer> counts = new HashMap<>();
        for (MoodEntry e : moodData) {
            if (e.getDate().isBefore(start) || e.getDate().isAfter(today)) continue;
            String m = e.getMood();
            counts.put(m, counts.getOrDefault(m, 0) + 1);
        }
        if (counts.isEmpty()) {
            System.out.println("Mood distribution: (tidak ada data dalam 7 hari)");
        } else {
            System.out.println("Mood distribution minggu ini:");
            for (Map.Entry<String, Integer> en : counts.entrySet()) {
                int c = en.getValue();
                String bar = "#".repeat(Math.max(0, c));
                System.out.printf("  %s: %s (%d)\n", en.getKey(), bar, c);
            }
        }
    }

    // Produce JSON for all entries
    public String getAllEntriesJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < moodData.size(); i++) {
            MoodEntry e = moodData.get(i);
            sb.append(entryToJson(e));
            if (i < moodData.size() - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }

    // Produce JSON for the most recent entry (or empty string if none)
    public String getLastEntryJson() {
        if (moodData.isEmpty()) return "";
        return entryToJson(moodData.get(moodData.size() - 1));
    }

    // Helper to convert one MoodEntry to JSON object
    private String entryToJson(MoodEntry e) {
        // simple manual JSON (no external deps)
        return String.format("{\"date\":\"%s\",\"mood\":\"%s\",\"score\":%d}",
            e.getDate().toString(), escapeJson(e.getMood()), e.getScore());
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // Simulate sending by writing JSON payload to a file (path relative to project)
    public void simulateSendPayload(String outPath) {
        try {
            Path p = Paths.get(outPath);
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            Files.write(p, getAllEntriesJson().getBytes(StandardCharsets.UTF_8));
            System.out.println("Simulated send: payload written to " + outPath);
        } catch (IOException ex) {
            System.err.println("Failed to write payload: " + ex.getMessage());
        }
    }
}
