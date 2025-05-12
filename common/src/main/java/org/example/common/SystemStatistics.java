package org.example.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * <p>Represents the system's statistics, including:</p>
 * <ul>
 *   <li>The 10 most common searches</li>
 *   <li>The index sizes of active barrels</li>
 *   <li>The average response time per barrel</li>
 * </ul>
 * 
 * <p>This class encapsulates statistical data related to the system's performance and search activities.
 * It includes lists of searches, barrel sizes, and response times that can be used for analysis or reporting.</p>
 * 
 * <p>The data is represented in the form of lists and hashmaps to store the top searches, barrel index sizes, 
 * and average response times, with methods to access and update this information.</p>
 */
public class SystemStatistics implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * <p>List of the top 10 most common searches in the system.</p>
     */
    private List<String> topSearches;

    /**
     * <p>List of the top 10 most common searches in the system.</p>
     */
    private HashMap<String, Integer> barrelIndexSizes;

    /**
     * <p>A map where the keys are barrel names and the values are the average response times for each barrel, in milliseconds.</p>
     */
    private HashMap<String, Double> averageResponseTimes;

    /**
     * <p>Constructs a new {@code SystemStatistics} object with the specified values for 
     * top searches, barrel index sizes, and average response times.</p>
     *
     * @param topSearches a {@code List<String>} containing the top 10 most frequent search terms.
     * @param barrelIndexSizes a {@code HashMap<String, Integer>} mapping barrel names to the number of URLs they index.
     * @param averageResponseTimes a {@code HashMap<String, Double>} mapping barrel names to their average response times (in milliseconds).
     */
    public SystemStatistics(List<String> topSearches, HashMap<String, Integer> barrelIndexSizes, HashMap<String, Double> averageResponseTimes) {
        this.topSearches = topSearches;
        this.barrelIndexSizes = barrelIndexSizes;
        this.averageResponseTimes = averageResponseTimes;
    }

    /**
     * <p>Gets the list of the top 10 most common searches.</p>
     *
     * @return a list of the top 10 searches.
     */
    public List<String> getTopSearches() {
        return topSearches;
    }

    /**
     * <p>Gets a map of barrel index sizes.</p>
     *
     * @return a map where the keys are barrel names and the values are the barrel sizes.
     */
    public HashMap<String, Integer> getBarrelIndexSizes() {
        return barrelIndexSizes;
    }

    /**
     * <p>Gets a map of average response times for each barrel.</p>
     *
     * @return a map where the keys are barrel names and the values are the average response times in milliseconds.
     */
    public HashMap<String, Double> getAverageResponseTimes() {
        return averageResponseTimes;
    }

    /**
     * <p>Sets the list of the top 10 most common searches.</p>
     *
     * @param topSearches the list of the top 10 searches to be set.
     */
    public void setTopSearches(List<String> topSearches){
        this.topSearches = topSearches;
    }

    /**
     * <p>Sets the map of average response times for each barrel.</p>
     *
     * @param averageResponseTimes the map of average response times to be set.
     */
    public void setResponseTimes(HashMap<String, Double> averageResponseTimes){
        this.averageResponseTimes = averageResponseTimes;
    }

    /**
     * <p>Sets the map of barrel index sizes.</p>
     *
     * @param barrelIndexSizes the map of barrel sizes to be set.
     */
    public void setBarrelIndexSizes(HashMap<String, Integer> barrelIndexSizes){
        this.barrelIndexSizes = barrelIndexSizes;
    }

    /**
     * <p>Returns a string representation of the system statistics, including the top searches, barrel index sizes,
     * and average response times.</p>
     *
     * @return a string containing the system statistics information.
     */
    @Override
    public String toString() {
        return "\nTop 10 Searches: " + topSearches +
               "\nBarrel Index Sizes: " + barrelIndexSizes +
               "\nAverage Response Times (ms): " + averageResponseTimes;
    }
}
