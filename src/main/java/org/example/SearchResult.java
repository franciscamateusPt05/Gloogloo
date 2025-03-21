package org.example;


import java.io.Serializable;
import java.util.List;

/**
 * Represents the result of a search query, containing matched URLs and metadata.
 */
public class SearchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The search query */
    private String query;

    /** List of matching URLs */
    private List<String> urls;

    /**
     * Constructs a SearchResult object.
     *
     * @param query The search query.
     * @param urls  The list of URLs that match the query.
     */
    public SearchResult(String query, List<String> urls) {
        this.query = query;
        this.urls = urls;
    }

    /**
     * Gets the search query.
     *
     * @return The search query.
     */
    public String getQuery() {
        return query;
    }

    /**
     * Gets the list of matched URLs.
     *
     * @return List of URLs.
     */
    public List<String> getUrls() {
        return urls;
    }

    /**
     * Converts the search result into a readable format.
     *
     * @return A formatted string representation.
     */
    @Override
    public String toString() {
        return "Search Query: " + query + "\nResults:\n" + String.join("\n", urls);
    }
}
