package org.example;

public class WeeklyStats {
    private int totalPositive;
    private int totalNegative;
    private String dominantMood;
    private int totalCount;
    private double averageScore;

    public WeeklyStats(int totalPositive, int totalNegative, String dominantMood) {
        this.totalPositive = totalPositive;
        this.totalNegative = totalNegative;
        this.dominantMood = dominantMood;
        this.totalCount = 0;
        this.averageScore = 0.0;
    }

    public WeeklyStats(int totalPositive, int totalNegative, String dominantMood, int totalCount, double averageScore) {
        this.totalPositive = totalPositive;
        this.totalNegative = totalNegative;
        this.dominantMood = dominantMood;
        this.totalCount = totalCount;
        this.averageScore = averageScore;
    }

    public String calculateDominantMood() {
        return dominantMood;
    }

    public float getPositiveRatio() {
        int total = totalPositive + totalNegative;
        if (total == 0) return 0f;
        return (float) totalPositive / total;
    }

    public int getTotalPositive() {
        return totalPositive;
    }

    public int getTotalNegative() {
        return totalNegative;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public String getAverageMoodLabel() {
        if (totalCount == 0) return "";
        int rounded = (int) Math.round(averageScore);
        return MoodTracker.moodForScore(rounded);
    }
}