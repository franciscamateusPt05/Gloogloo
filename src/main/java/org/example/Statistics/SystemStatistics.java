package org.example.Statistics;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.HashMap;

/**
 * Represents the system's statistics, including:
 * - The 10 most common searches
 * - The index sizes of active barrels
 * - The average response time per barrel
 */
public class SystemStatistics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private final List<String> topSearches;
    private final HashMap<String, Integer> barrelIndexSizes;
    private final HashMap<String, Double> averageResponseTimes;

    public SystemStatistics(List<String> topSearches, HashMap<String, Integer> barrelIndexSizes, HashMap<String, Double> averageResponseTimes) {
        this.topSearches = topSearches;
        this.barrelIndexSizes = barrelIndexSizes;
        this.averageResponseTimes = averageResponseTimes;
    }

    public List<String> getTopSearches() {
        return topSearches;
    }

    public HashMap<String, Integer> getBarrelIndexSizes() {
        return barrelIndexSizes;
    }

    public HashMap<String, Double> getAverageResponseTimes() {
        return averageResponseTimes;
    }
    public String toString() {
        return "Top 10 Searches: " + topSearches +
               "\nBarrel Index Sizes: " + barrelIndexSizes +
               "\nAverage Response Times (ms): " + averageResponseTimes;
    }
}
