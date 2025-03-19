package org.example;

public class URL {
    
    /**
     * The URL string.
     */
    private String url;

     /**
     * Constructs a URL with a specified URL string.
     * 
     * @param url The URL string.
     */
    public URL(String url) {
        this.url = url;
    }

    /**
     * Checks if the URL is valid.
     * 
     * @return True if the URL starts with "http://" or "https://", False otherwise.
     */
    public boolean isValid() {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
