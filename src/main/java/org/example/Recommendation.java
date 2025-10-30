package org.example;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class Recommendation {
    public List<String> getPositiveRecommendations() {
        return Arrays.asList(
                "Take a short walk",
                "Listen to uplifting music",
                "Call a friend"
        );
    }

    public List<String> getMoodBoosterRecommendation() {
        return Arrays.asList(
                "Try a 5-minute breathing exercise",
                "Drink a glass of water",
                "Write down one thing you're grateful for"
        );
    }

    // Rekomendasi berdasarkan rata-rata skor 7 hari terakhir
    // <= 2.0: mood rendah, 2.0-<3.5: netral, 3.5-<4.5: positif, >=4.5: sangat positif
    public String getHeadingForAverage(double avgScore) {
        if (avgScore <= 0.0) return "Belum ada data 7 hari terakhir";
        if (avgScore <= 2.0) return "Rekomendasi untuk mood rendah";
        if (avgScore < 3.5) return "Rekomendasi untuk mood netral";
        if (avgScore < 4.5) return "Rekomendasi untuk mood positif";
        return "Rekomendasi untuk mood sangat positif";
    }

    public List<String> getRecommendationsForAverage(double avgScore) {
        List<String> recs = new ArrayList<>();
        if (avgScore <= 0.0) {
            recs.add("Tambahkan beberapa catatan mood terlebih dahulu untuk mendapatkan rekomendasi yang sesuai.");
            return recs;
        }
        if (avgScore <= 2.0) {
            recs.addAll(Arrays.asList(
                "Luangkan 5–10 menit untuk napas dalam (4-4-4-4 box breathing)",
                "Coba journaling singkat: tulis apa yang kamu rasakan tanpa menghakimi",
                "Hubungi seseorang yang kamu percaya atau minta dukungan kecil",
                "Lakukan aktivitas ringan: berjalan pelan atau peregangan"
            ));
        } else if (avgScore < 3.5) {
            recs.addAll(Arrays.asList(
                "Susun rencana kecil untuk esok hari (3 hal saja)",
                "Minum air dan makan camilan sehat",
                "Dengarkan musik yang menenangkan atau playlist favorit",
                "Catat 1 hal yang berjalan cukup baik hari ini"
            ));
        } else if (avgScore < 4.5) {
            recs.addAll(Arrays.asList(
                "Pertahankan kebiasaan baik: 20 menit aktivitas fisik",
                "Tentukan 1 tujuan kecil yang dapat diselesaikan hari ini",
                "Bagikan hal positifmu ke teman/keluarga",
                "Luangkan 10 menit untuk hobi yang kamu nikmati"
            ));
        } else { // >= 4.5
            recs.addAll(Arrays.asList(
                "Rayakan pencapaianmu (reward kecil)",
                "Coba tantangan ringan baru untuk berkembang",
                "Bantu orang lain: kirim pesan penyemangat",
                "Rencanakan aktivitas yang kamu nantikan akhir pekan ini"
            ));
        }
        return recs;
    }
}
