package org.example.Statistics;

import java.util.ArrayList;
import java.util.List;


public class BarrelStats {
    private List<Double> responseTimes;
    private long barrelSize;  // Size of the barrel, in whatever metric you need (e.g., number of items)

    public BarrelStats() {
        this.responseTimes = new ArrayList<>();
        this.barrelSize = 0;  // Initialize to zero
    }

    // Method to set or update the size of the barrel
    public void setBarrelSize(long size) {
        this.barrelSize = size;
    }

    public long getBarrelSize() {
        return this.barrelSize;
    }

    // Add response time to the list
    public void addResponseTime(double responseTime) {
        responseTimes.add(responseTime);
    }

    // Get average response time
    public double getAverageResponseTime() {
        return responseTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
