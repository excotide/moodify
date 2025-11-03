package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Scanner;
import org.mindrot.jbcrypt.BCrypt;


public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Inisialisasi Supabase dari environment variables atau file .env (fallback)
        // Catatan: System.getenv() menerima NAMA variabel lingkungan, bukan nilai.
        // Jika Anda tidak menyetel env vars, buat file .env di working directory dengan:
        // SUPABASE_URL="https://your-project.supabase.co"
        // SUPABASE_KEY="your_anon_or_service_key"
        Dotenv.load();
        String supabaseUrl = System.getenv("SUPABASE_URL");
        if (supabaseUrl == null) supabaseUrl = Dotenv.get("SUPABASE_URL");
        String supabaseKey = System.getenv("SUPABASE_KEY");
        if (supabaseKey == null) supabaseKey = Dotenv.get("SUPABASE_KEY");
        if (supabaseUrl == null || supabaseKey == null) {
            System.err.println("Environment variables SUPABASE_URL dan SUPABASE_KEY belum diset (atau .env tidak berisi keduanya). Keluar.");
            System.err.println("Contoh (PowerShell session):");
            System.err.println("  $env:SUPABASE_URL='https://your-project.supabase.co'");
            System.err.println("  $env:SUPABASE_KEY='your_anon_or_service_key'");
            System.err.println("Atau buat file .env di folder proyek dengan dua baris: SUPABASE_URL=... dan SUPABASE_KEY=...");
            scanner.close();
            return;
        }
        SupabaseClient supabaseClient = new SupabaseClient(supabaseUrl, supabaseKey);
        MoodTracker tracker = new MoodTracker(supabaseClient);

        // --- Login / Register flow: pilih register atau login ---
        SupabaseClient.UserEntry currentUser = null;
        boolean authenticated = false;
        while (!authenticated) {
            System.out.println("=== Autentikasi ===");
            System.out.println("1) Login");
            System.out.println("2) Daftar user baru");
            System.out.println("q) Keluar");
            System.out.print("Pilih (1/2/q): ");
            String opt = scanner.nextLine().trim();
            if (opt.equalsIgnoreCase("q")) {
                System.out.println("Keluar. Terima kasih.");
                scanner.close();
                return;
            }
            if (opt.equals("2")) {
                // Register
                System.out.print("Pilih username: ");
                String username = scanner.nextLine().trim();
                if (username.isEmpty()) { System.out.println("Username tidak boleh kosong."); continue; }
                // check exists
                SupabaseClient.UserEntry uexists = supabaseClient.fetchUserByUsername(username);
                if (uexists != null) { System.out.println("Username sudah terdaftar. Silakan login atau pilih username lain."); continue; }
                System.out.print("Pilih password: ");
                String password = scanner.nextLine();
                if (password.isEmpty()) { System.out.println("Password tidak boleh kosong."); continue; }
                String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
                boolean ok = supabaseClient.createUser(username, hash, LocalDateTime.now());
                if (ok) {
                    System.out.println("Pendaftaran berhasil. Silakan login.");
                } else {
                    System.out.println("Gagal mendaftar (cek koneksi / aturan DB). Coba lagi.");
                }
                continue;
            }
            if (opt.equals("1")) {
                System.out.print("Username: ");
                String username = scanner.nextLine().trim();
                if (username.isEmpty()) { System.out.println("Username tidak boleh kosong."); continue; }
                System.out.print("Password: ");
                String password = scanner.nextLine();
                SupabaseClient.UserEntry u = supabaseClient.fetchUserByUsername(username);
                if (u == null) { System.out.println("User tidak ditemukan."); continue; }
                if (u.passwordHash == null || u.passwordHash.isEmpty()) { System.out.println("User belum memiliki password yang valid."); continue; }
                boolean ok = BCrypt.checkpw(password, u.passwordHash);
                if (!ok) { System.out.println("Password salah. Coba lagi."); continue; }
                // sukses
                currentUser = u;
                LocalDateTime now = LocalDateTime.now();
                tracker.setUserLoginDate(now);
                // set current user id in tracker so entries are associated
                tracker.setUserId(currentUser.id);
                // print current user id for quick debugging
                System.out.println("(debug) current user id = " + currentUser.id);
                supabaseClient.updateUserLastLogin(u.id, now);
                System.out.println("Login berhasil sebagai '" + username + "' pada " + now + ".");
                authenticated = true;
                continue;
            }
            System.out.println("Pilihan tidak dikenal. Masukkan 1,2 atau q.");
        }

        boolean running = true;
        while (running) {
            System.out.println("\n=== Moodify Menu ===");
            System.out.println("1) Tambah mood (input user)");
            System.out.println("2) Tampilkan statistik mingguan");
            System.out.println("3) Tampilkan riwayat entri");
            System.out.println("4) Tampilkan rekomendasi");
            System.out.println("5) Keluar");
            System.out.print("Pilih (1-5): ");

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

                    // Validasi untuk tidak menambahkan data pada masa depan
                    if (dateTime.isAfter(LocalDateTime.now())) {
                        System.out.println("Tidak dapat menambahkan data pada masa depan. Entry dibatalkan.");
                        break;
                    }

                    boolean saved = tracker.inputMood(mood, dateTime);
                    int score = MoodTracker.scoreForMood(mood);
                    if (saved) {
                        System.out.println("Entry ditambahkan: " + mood + " (skor: " + score + ") pada " + dateTime);
                    } else {
                        System.out.println("Gagal menambahkan entry. Periksa koneksi/permission pada Supabase.");
                    }
                }
                case 2 -> {
                    tracker.displayWeeklyGraph();
                }
                case 3 -> {
                    tracker.displayEntryHistory();
                }
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
                case 5 -> {
                    running = false;
                }
                default -> System.out.println("Pilihan tidak dikenal. Masukkan 1-6.");
            }
        }

        scanner.close();
        System.out.println("Keluar. Terima kasih menggunakan Moodify.");
    }
}