package org.example;

public class WeeklyStats {
    private final int totalCount;
    private final int sumScore;
    private final double averageScore;

    public WeeklyStats(int totalCount, int sumScore, double averageScore) {
        this.totalCount = totalCount;
        this.sumScore = sumScore;
        this.averageScore = averageScore;
    }

    public int getTotalCount() { return totalCount; }
    public int getSumScore() { return sumScore; }
    public double getAverageScore() { return averageScore; }

    public String getAverageMoodLabel() {
        double a = averageScore;
        if (totalCount == 0) return "Tidak ada data";
        if (a < 1.5) return "Kacau";
        if (a < 2.5) return "Buruk";
        if (a < 3.5) return "Netral";
        if (a < 4.5) return "Bagus";
        return "Sangat bagus";
    }
}