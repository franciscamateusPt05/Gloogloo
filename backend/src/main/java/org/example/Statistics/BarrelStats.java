package org.example.Statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects and provides statistics for a Barrel, such as response times and size.
 * 
 * This class is used to store and analyze the performance metrics related to a Barrel,
 * including calculating the average response time and maintaining the current size.
 */
public class BarrelStats {
    private List<Double> responseTimes;
    private long barrelSize;

    /**
     * Constructs a new {@code BarrelStats} instance with empty statistics.
     */
    public BarrelStats() {
        this.responseTimes = new ArrayList<>();
        this.barrelSize = 0;  // Initialize to zero
    }

    /**
     * Sets the current size of the barrel.
     *
     * @param size the number of elements or bytes in the barrel
     */
    // Method to set or update the size of the barrel
    public void setBarrelSize(long size) {
        this.barrelSize = size;
    }

    /**
     * Returns the current size of the barrel.
     *
     * @return the barrel size
     */
    public long getBarrelSize() {
        return this.barrelSize;
    }

    /**
     * Adds a new response time measurement to the statistics.
     *
     * @param responseTime the response time in milliseconds
     */
    public void addResponseTime(double responseTime) {
        responseTimes.add(responseTime);
    }

    /**
     * Returns the average of all recorded response times, rounded to two decimal places.
     *
     * @return the average response time, or 0.0 if no data has been recorded
     */
    public double getAverageResponseTime() {
        double avgResponseTime = responseTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        avgResponseTime = (double) Math.round(avgResponseTime * 100)/ 100;
        return avgResponseTime;
    }
}
