package org.example;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

        MoodTracker tracker = new MoodTracker("data/moods.csv");

        boolean running = true;
        while (running) {
            System.out.println("\n=== Moodify Menu ===");
            System.out.println("1) Tambah mood (input user)");
            System.out.println("2) Simpan ke file");
            System.out.println("3) Tampilkan statistik mingguan");
            System.out.println("4) Tampilkan rekomendasi");
            System.out.println("5) Keluar");
            System.out.println("6) Tampilkan payload JSON yang akan dikirim");
            System.out.println("7) Simulasikan kirim data (tulis payload ke file)");
            System.out.print("Pilih (1-7): ");

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

                    System.out.print("Masukkan tanggal (yyyy-MM-dd) atau kosongkan untuk hari ini: ");
                    String dateStr = scanner.nextLine().trim();
                    LocalDate date;
                    if (dateStr.isEmpty()) {
                        date = LocalDate.now();
                    } else {
                        try {
                            date = LocalDate.parse(dateStr, fmt);
                        } catch (DateTimeParseException ex) {
                            System.out.println("Format tanggal salah, gunakan yyyy-MM-dd. Entry dibatalkan.");
                            break;
                        }
                    }
                    tracker.inputMood(mood, date);
                    int score = MoodTracker.scoreForMood(mood);
                    System.out.println("Entry ditambahkan: " + mood + " (skor: " + score + ") pada " + date);
                }
                case 2 -> tracker.saveToLocal();
                case 3 -> tracker.displayWeeklyGraph();
                case 4 -> {
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
                    System.out.println("\nPayload JSON (all entries):");
                    System.out.println(tracker.getAllEntriesJson());
                }
                case 7 -> {
                    System.out.print("Masukkan path output untuk payload (enter untuk data/sent_payload.json): ");
                    String out = scanner.nextLine().trim();
                    if (out.isEmpty()) out = "data/sent_payload.json";
                    tracker.simulateSendPayload(out);
                }
                case 5 -> {
                    System.out.print("Simpan sebelum keluar? (y/N): ");
                    String save = scanner.nextLine().trim().toLowerCase();
                    if (save.equals("y") || save.equals("yes")) {
                        tracker.saveToLocal();
                    }
                    running = false;
                }
                default -> System.out.println("Pilihan tidak dikenal. Masukkan 1-7.");
            }
        }

        scanner.close();
        System.out.println("Keluar. Terima kasih menggunakan Moodify.");
    }
}