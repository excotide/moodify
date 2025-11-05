// File: src/main/java/org/example/MoodTracker.java
package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Versi MoodTracker yang menyimpan/ambil data melalui SupabaseClient.
 * Hanya implementasi metode yang diperlukan oleh Main.java disediakan.
 */
public class MoodTracker {
    private final SupabaseClient supabase;
    // store last login date/time in memory for anchoring entry selection
    private java.time.LocalDateTime userLoginDate = null;
    // store anchor date (first login / account creation) if available
    private java.time.LocalDateTime anchorDate = null;
    // store current logged-in user id (nullable)
    private String userId = null;

    public MoodTracker(SupabaseClient supabase) {
        this.supabase = supabase;
    }

    public void setUserLoginDate(LocalDateTime dt) {
        this.userLoginDate = dt;
    }

    public LocalDateTime getUserLoginDate() {
        return this.userLoginDate;
    }

    public void setAnchorDate(LocalDateTime dt) {
        this.anchorDate = dt;
    }

    public LocalDateTime getAnchorDate() {
        return this.anchorDate;
    }

    /**
     * Hitung hari ke-berapa hari ini sejak anchor (anchor = first-login/createdAt jika tersedia,
     * jika tidak pakai userLoginDate, jika tidak pakai hari ini sebagai anchor sehingga hasil = 1).
     * Mengembalikan nilai minimal 1.
     */
    public long getTodayDayNumber() {
        // Compute day-number as difference between account creation (anchorDate)
        // and the current login date (userLoginDate). If anchorDate is missing,
        // return 1. If userLoginDate is missing, fall back to LocalDate.now().
        if (this.anchorDate == null) return 1L;
        java.time.LocalDate anchorLocal = this.anchorDate.toLocalDate();
        java.time.LocalDate loginLocal = (this.userLoginDate != null) ? this.userLoginDate.toLocalDate() : java.time.LocalDate.now();
        if (anchorLocal.isAfter(loginLocal)) anchorLocal = loginLocal;
        long days = java.time.Duration.between(anchorLocal.atStartOfDay(), loginLocal.atStartOfDay()).toDays() + 1;
        return Math.max(1, days);
    }

    /**
     * Return a debug string that shows intermediate values used to compute today's day number.
     */
    public String getTodayDayNumberDebug() {
        StringBuilder sb = new StringBuilder();
        sb.append("getTodayDayNumber debug:\n");
        sb.append("  anchorDate (raw) = ").append(this.anchorDate).append("\n");
        sb.append("  userLoginDate (raw) = ").append(this.userLoginDate).append("\n");
        if (this.anchorDate == null) {
            sb.append("  -> anchorDate is null, returning day=1\n");
            return sb.toString();
        }
        java.time.LocalDate anchorLocal = this.anchorDate.toLocalDate();
        java.time.LocalDate loginLocal = (this.userLoginDate != null) ? this.userLoginDate.toLocalDate() : java.time.LocalDate.now();
        sb.append("  anchorLocal = ").append(anchorLocal).append("\n");
        sb.append("  loginLocal  = ").append(loginLocal).append("\n");
        if (anchorLocal.isAfter(loginLocal)) {
            sb.append("  anchorLocal is after loginLocal; adjusted anchorLocal = loginLocal\n");
            anchorLocal = loginLocal;
        }
        long days = java.time.Duration.between(anchorLocal.atStartOfDay(), loginLocal.atStartOfDay()).toDays() + 1;
        sb.append("  computed days (before max) = ").append(days).append("\n");
        sb.append("  result (max 1) = ").append(Math.max(1, days)).append("\n");
        return sb.toString();
    }

    public boolean inputMood(String mood, LocalDateTime dateTime) {
        int score = scoreForMood(mood);
        boolean ok = supabase.insertMood(mood, score, dateTime, userId);
        if (!ok) {
            System.err.println("Gagal menyimpan ke Supabase. Cek logs untuk detail (SupabaseClient akan menampilkan respons).");
        }
        return ok;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return this.userId;
    }

    public void saveToLocal() {
        // data sudah disimpan langsung ke Supabase saat inputMood dipanggil
        System.out.println("Data disimpan langsung ke Supabase (tidak ada file lokal).");
    }

    public void displayEntryHistory() {
        List<SupabaseClient.MoodEntry> entries = supabase.fetchAllEntriesForUser(this.userId);

        if (entries.isEmpty()) {
            System.out.println("Tidak ditemukan entri untuk user ini. Menampilkan semua entri sebagai fallback.");
            entries = supabase.fetchAllEntries();
        }

        if (entries.isEmpty()) {
            System.out.println("Tidak ada entri.");
            return;
        }

        System.out.println("Riwayat entri:");
    // determine anchor for day counting: prefer anchorDate (account creation), otherwise fall back to earliest entry date
    LocalDate anchorLocal = null;
    if (this.anchorDate != null) anchorLocal = this.anchorDate.toLocalDate();
    else anchorLocal = entries.get(0).timestamp.toLocalDate();

        for (SupabaseClient.MoodEntry e : entries) {
            String dayOfWeek = e.timestamp.getDayOfWeek().toString();
            String dayInIndonesian = translateDayToIndonesian(dayOfWeek);
            long daysSinceStart = java.time.Duration.between(anchorLocal.atStartOfDay(), e.timestamp.toLocalDate().atStartOfDay()).toDays() + 1;
            System.out.println("Hari ke-" + daysSinceStart + " - " + e.mood + " (skor: " + e.score + ") [hari: " + dayInIndonesian + "]");
        }
    }

    public void displayWeeklyGraph() {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(6);
        LocalDate start;
        if (this.anchorDate != null) {
            LocalDate a = this.anchorDate.toLocalDate();
            start = a.isAfter(windowStart) ? a : windowStart;
        } else {
            start = windowStart;
        }
        List<SupabaseClient.MoodEntry> entries = supabase.fetchEntriesBetweenForUser(start, today, this.userId);

        if (entries.isEmpty()) {
            entries = supabase.fetchEntriesBetween(start, today);
        }

        if (entries.isEmpty()) {
            return;
        }

        double[] totals = new double[7];
        int[] counts = new int[7];

        for (SupabaseClient.MoodEntry e : entries) {
            int idx = (int) java.time.Duration.between(start.atStartOfDay(), e.timestamp.toLocalDate().atStartOfDay()).toDays();
            if (idx < 0 || idx > 6) continue;
            totals[idx] += e.score;
            counts[idx] += 1;
        }

        for (int i = 0; i < 7; i++) {
            double avg = counts[i] == 0 ? 0.0 : totals[i] / counts[i];
            System.out.printf("Hari %d (%s): %.2f (%d entri)%n", i + 1, start.plusDays(i), avg, counts[i]);
        }
    }

    public WeeklyStats calculateWeeklyStats() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        List<SupabaseClient.MoodEntry> entries;
        if (this.userId != null) entries = supabase.fetchEntriesBetweenForUser(start, today, this.userId);
        else entries = supabase.fetchEntriesBetween(start, today);
        int total = 0;
        int sum = 0;
        for (SupabaseClient.MoodEntry e : entries) {
            total++;
            sum += e.score;
        }
        double avg = total == 0 ? 0.0 : ((double)sum)/total;
        return new WeeklyStats(total, sum, avg);
    }

    public static int scoreForMood(String mood) {
        return switch (mood) {
            case "Kacau" -> 1;
            case "Buruk" -> 2;
            case "Netral" -> 3;
            case "Bagus" -> 4;
            case "Sangat bagus" -> 5;
            default -> 0;
        };
    }

    public String translateDayToIndonesian(String dayOfWeek) {
        switch (dayOfWeek) {
            case "MONDAY":
                return "Senin";
            case "TUESDAY":
                return "Selasa";
            case "WEDNESDAY":
                return "Rabu";
            case "THURSDAY":
                return "Kamis";
            case "FRIDAY":
                return "Jumat";
            case "SATURDAY":
                return "Sabtu";
            case "SUNDAY":
                return "Minggu";
            default:
                return dayOfWeek;
        }
    }
}
