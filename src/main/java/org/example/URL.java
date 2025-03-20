package org.example;

/**
 * This class represents a URL and ensures that it is valid upon creation.
 */
public class URL {
    private String url;

    /**
     * Constructs a URL object after validating the given URL string.
     *
     * @param url The URL string to be validated and stored.
     * @throws IllegalArgumentException if the given URL is not valid.
     */
    public URL(String url) {
        if (!isValid(url)) {
            throw new IllegalArgumentException("Invalid URL format: " + url);
        }
        this.url = url;
    }

    /**
     * Returns the stored URL as a string.
     *
     * @return The valid URL string.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Show the URL
     *
     * @return The URL string.
     */
    public String toString() {
        return url;
    }

    /**
     * Checks if the URL is valid.
     * 
     * @return True if the URL starts with "http://" or "https://", False otherwise.
     */
    public boolean isValid(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
