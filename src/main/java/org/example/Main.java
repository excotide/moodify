package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        MoodTracker tracker = new MoodTracker("data/moods.csv");

        // --- Login flow: tampilkan terlebih dahulu; lanjutkan hanya jika login berhasil ---
        boolean loggedIn = false;
        while (!loggedIn) {
            System.out.println("=== Login (simulasi) ===");
            System.out.print("Masukkan username (atau 'q' untuk keluar): ");
            String username = scanner.nextLine().trim();
            if (username.equalsIgnoreCase("q")) {
                System.out.println("Keluar. Terima kasih.");
                scanner.close();
                return;
            }
            if (username.isEmpty()) {
                System.out.println("Username tidak boleh kosong. Coba lagi.");
                continue;
            }
            System.out.print("Masukkan tanggal login (yyyy-MM-dd) atau kosongkan untuk sekarang: ");
            String loginDateStr = scanner.nextLine().trim();
            java.time.LocalDate loginDate;
            if (loginDateStr.isEmpty()) {
                loginDate = java.time.LocalDate.now();
            } else {
                try {
                    loginDate = java.time.LocalDate.parse(loginDateStr);
                } catch (java.time.format.DateTimeParseException ex) {
                    System.out.println("Format tanggal login salah. Gunakan yyyy-MM-dd. Coba lagi.");
                    continue;
                }
            }
            java.time.LocalDateTime loginDateTime = java.time.LocalDateTime.of(loginDate, java.time.LocalTime.now());
            tracker.setUserLoginDate(loginDateTime);
            System.out.println("Login berhasil sebagai '" + username + "' pada " + loginDateTime + ".");
            loggedIn = true;
        }

        boolean running = true;
        while (running) {
            System.out.println("\n=== Moodify Menu ===");
            System.out.println("1) Tambah mood (input user)");
            System.out.println("2) Simpan ke file");
            System.out.println("3) Tampilkan statistik mingguan");
            System.out.println("4) Tampilkan riwayat entri");
            System.out.println("5) Tampilkan rekomendasi");
            System.out.println("6) Keluar");
            System.out.print("Pilih (1-6): ");

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.println("Masukan tidak valid. Coba lagi.");
                continue;
            }

            switch (choice) {
                
                case 1 -> {
                    System.out.println("Pilih mood:");
                    System.out.println("1) Kacau");
                    System.out.println("2) Buruk");
                    System.out.println("3) Netral");
                    System.out.println("4) Bagus");
                    System.out.println("5) Sangat bagus");
                    System.out.print("Masukkan pilihan (1-5): ");
                    String moodChoiceStr = scanner.nextLine().trim();

                    String mood;
                    try {
                        int moodChoice = Integer.parseInt(moodChoiceStr);
                        mood = switch (moodChoice) {
                            case 1 -> "Kacau";
                            case 2 -> "Buruk";
                            case 3 -> "Netral";
                            case 4 -> "Bagus";
                            case 5 -> "Sangat bagus";
                            default -> null;
                        };
                    } catch (NumberFormatException ex) {
                        mood = null;
                    }

                    if (mood == null) {
                        System.out.println("Pilihan mood tidak valid.");
                        break;
                    }

                    // Pilihan input: pilih hari-ke (1..7) relatif pada 7 hari terakhir, dan jam (0..23)
                    LocalDate today = LocalDate.now();
                    LocalDate windowStart = today.minusDays(6); // default window start
                    // prefer tracker user login date as anchor if set and within window
                    LocalDate start;
                    if (tracker.getUserLoginDate() != null) {
                        LocalDate loginLocal = tracker.getUserLoginDate().toLocalDate();
                        if (!loginLocal.isBefore(windowStart) && !loginLocal.isAfter(today)) {
                            start = loginLocal;
                        } else {
                            start = windowStart;
                        }
                    } else {
                        start = windowStart;
                    }

                    System.out.print("Masukkan hari-ke (1-7) untuk memilih hari dalam 7-hari terakhir, atau kosongkan untuk hari ini: ");
                    String hariKeStr = scanner.nextLine().trim();
                    LocalDate chosenDate;
                    if (hariKeStr.isEmpty()) {
                        chosenDate = LocalDate.now();
                    } else {
                        int hk;
                        try {
                            hk = Integer.parseInt(hariKeStr);
                        } catch (NumberFormatException ex) {
                            System.out.println("Input hari-ke tidak valid. Entry dibatalkan.");
                            break;
                        }
                        if (hk < 1 || hk > 7) {
                            System.out.println("Hari-ke harus antara 1 dan 7. Entry dibatalkan.");
                            break;
                        }
                        // map hari-ke ke tanggal: 1 -> start, 7 -> today
                        chosenDate = start.plusDays(hk - 1);
                    }

                    System.out.print("Masukkan jam (0-23) atau kosongkan untuk jam sekarang: ");
                    String jamStr = scanner.nextLine().trim();
                    int hour;
                    if (jamStr.isEmpty()) {
                        hour = LocalDateTime.now().getHour();
                    } else {
                        try {
                            hour = Integer.parseInt(jamStr);
                        } catch (NumberFormatException ex) {
                            System.out.println("Input jam tidak valid. Entry dibatalkan.");
                            break;
                        }
                        if (hour < 0 || hour > 23) {
                            System.out.println("Jam harus antara 0 dan 23. Entry dibatalkan.");
                            break;
                        }
                    }

                    LocalDateTime dateTime = LocalDateTime.of(chosenDate, LocalTime.of(hour, 0));
                    tracker.inputMood(mood, dateTime);
                    int score = MoodTracker.scoreForMood(mood);
                    System.out.println("Entry ditambahkan: " + mood + " (skor: " + score + ") pada " + dateTime);
                }
                case 2 -> tracker.saveToLocal();
                case 3 -> tracker.displayWeeklyGraph();
                case 4 -> tracker.displayEntryHistory();
                case 5 -> {
                    WeeklyStats stats = tracker.calculateWeeklyStats();
                    double avg = stats.getAverageScore();
                    Recommendation rec = new Recommendation();
                    String heading = rec.getHeadingForAverage(avg);
                    System.out.println("\n" + heading +
                            (stats.getTotalCount() > 0 ? String.format(" (rata-rata: %.2f, label: %s)", avg, stats.getAverageMoodLabel()) : ""));
                    for (String r : rec.getRecommendationsForAverage(avg)) {
                        System.out.println(" - " + r);
                    }
                }
                case 6 -> {
                    System.out.print("Simpan sebelum keluar? (y/N): ");
                    String save = scanner.nextLine().trim().toLowerCase();
                    if (save.equals("y") || save.equals("yes")) {
                        tracker.saveToLocal();
                    }
                    running = false;
                }
                default -> System.out.println("Pilihan tidak dikenal. Masukkan 1-6.");
            }
        }

        scanner.close();
        System.out.println("Keluar. Terima kasih menggunakan Moodify.");
    }
}