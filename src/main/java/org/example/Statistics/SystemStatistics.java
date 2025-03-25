package org.example.Statistics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Represents the system's statistics, including:
 * - The 10 most common searches
 * - The index sizes of active barrels
 * - The average response time per barrel
 */
public class SystemStatistics implements Serializable {
    
        private static final long serialVersionUID = 1L;
        private List<String> topSearches;
        private HashMap<String, Integer> barrelIndexSizes;
        private HashMap<String, Double> averageResponseTimes;
    
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
    
        public void setTopSearches(List<String> topSearches){
            this.topSearches = topSearches;
        }

        public void setResponseTimes(HashMap<String, Double> averageResponseTimes){
            this.averageResponseTimes = averageResponseTimes;
        }

        public void setBarrelIndexSizes(HashMap<String, Integer> barrelIndexSizes){
            this.barrelIndexSizes = barrelIndexSizes;
        }

    public String toString() {
        return "\nTop 10 Searches: " + topSearches +
               "\nBarrel Index Sizes: " + barrelIndexSizes +
               "\nAverage Response Times (ms): " + averageResponseTimes;
    }
}
