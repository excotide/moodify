// File: src/main/java/org/example/MoodTracker.java
package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private java.time.LocalDateTime userLoginDate = null;

    public MoodTracker(String filePath) {
        this.filePath = filePath;
        loadFromLocal();
    }

    // Set the simulated user login datetime (anchor for day-1)
    public void setUserLoginDate(java.time.LocalDateTime dt) {
        this.userLoginDate = dt;
    }

    public java.time.LocalDateTime getUserLoginDate() {
        return this.userLoginDate;
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

    // Load existing entries from CSV file. Support both legacy (date,mood) and new (dateTime,mood,score)
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
                    String dateToken = parts[0].trim();
                    LocalDateTime dateTime;
                    try {
                        // try full date-time first
                        dateTime = LocalDateTime.parse(dateToken);
                    } catch (DateTimeParseException dtpe) {
                        // fallback to date only
                        LocalDate ld = LocalDate.parse(dateToken);
                        dateTime = ld.atStartOfDay();
                    }
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
                    moodData.add(new MoodEntry(mood, score, dateTime));
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
        // attach the current time to the provided date (user provided date has no time)
        LocalDateTime dt = LocalDateTime.of(date, LocalTime.now());
        moodData.add(new MoodEntry(mood, score, dt));
    }

    // Overload: accept LocalDateTime directly (when user specifies hour)
    public void inputMood(String mood, LocalDateTime dateTime) {
        // Centralized validation: reject future timestamps
        if (dateTime.isAfter(LocalDateTime.now())) {
            System.out.println("Tidak boleh memasukkan entri di masa depan. Entry dibatalkan.");
            return;
        }
        int score = scoreForMood(mood);
        moodData.add(new MoodEntry(mood, score, dateTime));
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
                // save ISO date-time so we preserve time information
                lines.add(e.getDateTime().toString() + "," + e.getMood() + "," + e.getScore());
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
        LocalDate windowStart = today.minusDays(6);

        // Group entries by date within the 7-day window (windowStart..today)
        Map<LocalDate, List<MoodEntry>> byDate = new HashMap<>();
        for (MoodEntry e : moodData) {
            if (e.getDate().isBefore(windowStart) || e.getDate().isAfter(today)) continue;
            byDate.computeIfAbsent(e.getDate(), d -> new ArrayList<>()).add(e);
        }

        // determine start anchor:
        // if userLoginDate is set and falls inside the 7-day window, use it as anchor
        LocalDate startCandidate = byDate.keySet().stream().min(LocalDate::compareTo).orElse(windowStart);
        LocalDate start;
        if (this.userLoginDate != null) {
            LocalDate loginLocal = this.userLoginDate.toLocalDate();
            if (!loginLocal.isBefore(windowStart) && !loginLocal.isAfter(today)) {
                start = loginLocal;
            } else {
                start = startCandidate;
            }
        } else {
            start = startCandidate;
        }

        int positive = 0; // days with average score >= 4
        int negative = 0; // days with average score <= 2
        int totalDays = 0;
        double sumDailyAverages = 0.0;
        Map<String, Integer> dayMoodCounts = new HashMap<>();

        // iterate each day in the 7-day range and compute per-day aggregates
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            List<MoodEntry> entries = byDate.get(d);
            if (entries == null || entries.isEmpty()) continue;
            totalDays++;

            int daySum = 0;
            Map<String, Integer> perDayMood = new HashMap<>();
            for (MoodEntry me : entries) {
                daySum += me.getScore();
                perDayMood.put(me.getMood(), perDayMood.getOrDefault(me.getMood(), 0) + 1);
            }
            double dayAvg = (double) daySum / entries.size();
            sumDailyAverages += dayAvg;

            if (dayAvg >= 4.0) positive++;
            else if (dayAvg <= 2.0) negative++;

            Optional<Map.Entry<String, Integer>> dm = perDayMood.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue));
            dm.ifPresent(en -> dayMoodCounts.put(en.getKey(), dayMoodCounts.getOrDefault(en.getKey(), 0) + 1));
        }

        String dominant = dayMoodCounts.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse("");
        double avg = totalDays == 0 ? 0.0 : sumDailyAverages / totalDays;

        return new WeeklyStats(positive, negative, dominant, totalDays, avg);
    }

    public void displayWeeklyGraph() {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(6);
        WeeklyStats stats = calculateWeeklyStats();

        // compute start anchor: prefer user login date if set and within window; otherwise earliest-data
        Map<LocalDate, List<MoodEntry>> _byDate = new HashMap<>();
        for (MoodEntry e : moodData) {
            if (e.getDate().isBefore(windowStart) || e.getDate().isAfter(today)) continue;
            _byDate.computeIfAbsent(e.getDate(), d -> new ArrayList<>()).add(e);
        }
        LocalDate startCandidate2 = _byDate.keySet().stream().min(LocalDate::compareTo).orElse(windowStart);
        LocalDate start;
        if (this.userLoginDate != null) {
            LocalDate loginLocal = this.userLoginDate.toLocalDate();
            if (!loginLocal.isBefore(windowStart) && !loginLocal.isAfter(today)) {
                start = loginLocal;
            } else {
                start = startCandidate2;
            }
        } else {
            start = startCandidate2;
        }

        System.out.println("Statistik mingguan (anchor hari pertama data) (" + start + " s/d " + today + ")");

        if (!stats.isCompleteWeek()) {
            System.out.printf("Statistik mingguan belum tersedia (butuh %d hari dengan data). Saat ini terisi: %d hari.\n",
                WeeklyStats.REQUIRED_DAYS, stats.getTotalCount());
            return;
        }

        System.out.println("Dominant mood: " + (stats.calculateDominantMood().isEmpty() ? "(tidak ada data)" : stats.calculateDominantMood()));
        System.out.printf("Total hari terisi: %d, Rata-rata skor: %.2f (%s)\n",
            stats.getTotalCount(), stats.getAverageScore(),
            stats.getTotalCount() == 0 ? "-" : stats.getAverageMoodLabel());
        System.out.printf("Positive days: %d, Negative days: %d, Positive ratio: %.2f\n",
            stats.getTotalPositive(), stats.getTotalNegative(), stats.getPositiveRatio());

        // Weekly distribution (per-day majority mood)
        Map<LocalDate, List<MoodEntry>> byDate = new HashMap<>();
        for (MoodEntry e : moodData) {
            if (e.getDate().isBefore(start) || e.getDate().isAfter(today)) continue;
            byDate.computeIfAbsent(e.getDate(), d -> new ArrayList<>()).add(e);
        }
        Map<String, Integer> counts = new HashMap<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            List<MoodEntry> entries = byDate.get(d);
            if (entries == null || entries.isEmpty()) continue;
            Map<String, Integer> perDayMood = new HashMap<>();
            for (MoodEntry me : entries) {
                perDayMood.put(me.getMood(), perDayMood.getOrDefault(me.getMood(), 0) + 1);
            }
            Optional<Map.Entry<String, Integer>> dm = perDayMood.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue));
            if (dm.isPresent()) {
                String m = dm.get().getKey();
                counts.put(m, counts.getOrDefault(m, 0) + 1);
            }
        }

        if (counts.isEmpty()) {
            System.out.println("Mood distribution: (tidak ada data dalam 7 hari)");
        } else {
            System.out.println("Mood distribution minggu ini (per-hari, majority mood):");
            for (Map.Entry<String, Integer> en : counts.entrySet()) {
                int c = en.getValue();
                String bar = "#".repeat(Math.max(0, c));
                System.out.printf("  %s: %s (%d hari)\n", en.getKey(), bar, c);
            }
        }
    }

    
    // Tampilkan riwayat entri untuk 7 hari terakhir (tanggal, hari-ke, jam)
    public void displayEntryHistory() {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(6);

        // Group entries by date within the 7-day window (windowStart..today)
        Map<LocalDate, List<MoodEntry>> byDate = new HashMap<>();
        for (MoodEntry e : moodData) {
            if (e.getDate().isBefore(windowStart) || e.getDate().isAfter(today)) continue;
            byDate.computeIfAbsent(e.getDate(), d -> new ArrayList<>()).add(e);
        }

        // determine start anchor: prefer user login date if set and within window; otherwise earliest-data
        LocalDate startCandidate3 = byDate.keySet().stream().min(LocalDate::compareTo).orElse(windowStart);
        LocalDate start;
        if (this.userLoginDate != null) {
            LocalDate loginLocal = this.userLoginDate.toLocalDate();
            if (!loginLocal.isBefore(windowStart) && !loginLocal.isAfter(today)) {
                start = loginLocal;
            } else {
                start = startCandidate3;
            }
        } else {
            start = startCandidate3;
        }

        System.out.println();
        System.out.println("Riwayat entri minggu ini (tanggal, hari-ke, jam):");
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            List<MoodEntry> entries = byDate.get(d);
            if (entries == null || entries.isEmpty()) continue;
            int dayIndex = (int) (d.toEpochDay() - start.toEpochDay()) + 1;
            // sort entries for the day by time
            entries.sort(Comparator.comparing(MoodEntry::getDateTime));
            for (MoodEntry me : entries) {
                String time = me.getDateTime().toLocalTime().withNano(0).toString();
                System.out.printf("  %s (hari ke-%d) %s - %s (skor: %d)\n",
                    d.toString(), dayIndex, time, me.getMood(), me.getScore());
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
        // simple manual JSON (no external deps) - include full date-time
        return String.format("{\"date\":\"%s\",\"mood\":\"%s\",\"score\":%d}",
            e.getDateTime().toString(), escapeJson(e.getMood()), e.getScore());
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
