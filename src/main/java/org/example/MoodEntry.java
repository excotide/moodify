package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class MoodEntry {
    private final LocalDateTime dateTime;
    private final String mood;
    private final int score; // 1..5 (1: Kacau, 5: Sangat bagus)

    public MoodEntry(String mood, int score, LocalDateTime dateTime) {
        this.mood = mood;
        this.score = score;
        this.dateTime = dateTime;
    }

    // Backward-compat constructor (derive score from mood string if needed)
    public MoodEntry(String mood, LocalDate date) {
        this.mood = mood;
        this.score = MoodTracker.scoreForMood(mood);
        // keep time as start of day by default
        this.dateTime = date.atStartOfDay();
    }

    // Convenience constructor: mood + date (LocalDate) + assign current time
    public MoodEntry(String mood, int score, LocalDate date) {
        this.mood = mood;
        this.score = score;
        this.dateTime = LocalDateTime.of(date, LocalTime.now());
    }

    public String getMood() {
        return mood;
    }

    public int getScore() {
        return score;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public LocalDate getDate() {
        return dateTime.toLocalDate();
    }
}