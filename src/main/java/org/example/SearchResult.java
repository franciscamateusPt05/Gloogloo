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

    /** Title of the search result */
    private String title;

    /** URL of the search result */
    private String url;

    /** Snippet of the search result */
    private String snippet;

    /**
     * Constructs a SearchResult object for a query with multiple results.
     *
     * @param query The search URL.
     * @param urls  The list of URLs that match the query.
     */
    public SearchResult(String query, List<String> urls) {
        this.query = query;
        this.urls = urls;
    }

    /**
     * Constructs a SearchResult object for a single result.
     *
     * @param title   The title of the webpage.
     * @param url     The URL of the webpage.
     * @param snippet A short snippet from the webpage.
     */
    public SearchResult(String title, String url, String snippet) {
        this.title = title;
        this.url = url;
        this.snippet = snippet;
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
     * Gets the title of the search result.
     *
     * @return The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the URL of the search result.
     *
     * @return The URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the snippet of the search result.
     *
     * @return The snippet.
     */
    public String getSnippet() {
        return snippet;
    }

    /**
     * Converts the search result into a readable format.
     *
     * @return A formatted string representation.
     */
    @Override
    public String toString() {
        if (title != null && url != null && snippet != null) {
            return "Title: " + title + "\nURL: " + url + "\nSnippet: " + snippet + "\n";
        } else {
            return "Search Query: " + query + "\nResults:\n" + String.join("\n", urls);
        }
    }
}
