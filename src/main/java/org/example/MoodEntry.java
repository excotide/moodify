package org.example;

import java.time.LocalDate;

public class MoodEntry {
    private final LocalDate date;
    private final String mood;
    private final int score; // 1..5 (1: Kacau, 5: Sangat bagus)

    public MoodEntry(String mood, int score, LocalDate date) {
        this.mood = mood;
        this.score = score;
        this.date = date;
    }

    // Backward-compat constructor (derive score from mood string if needed)
    public MoodEntry(String mood, LocalDate date) {
        this.mood = mood;
        this.score = MoodTracker.scoreForMood(mood);
        this.date = date;
    }

    public String getMood() {
        return mood;
    }

    public int getScore() {
        return score;
    }

    public LocalDate getDate() {
        return date;
    }
}