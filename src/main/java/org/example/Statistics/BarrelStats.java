package org.example.Statistics;

/**
 * Helper class to track statistics for each barrel.
 * It stores the total response time and the count of requests.
 */
public class BarrelStats {
    private double totalResponseTime;
    private int requestCount;

    public BarrelStats() {
        this.totalResponseTime = 0.0;
        this.requestCount = 0;
    }

    public void addResponseTime(double responseTime) {
        this.totalResponseTime += responseTime;
        this.requestCount++;
    }

    public double getAverageResponseTime() {
        return requestCount > 0 ? totalResponseTime / requestCount : 0.0;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public double getTotalResponseTime() {
        return totalResponseTime;
    }
}
