package com.example.frontend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.rmi.RemoteException;
import java.lang.String;


import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import java.text.Normalizer;

import org.example.common.IGateway;
import org.example.common.SystemStatistics;
import org.example.common.SearchResult;

/**
 * <p>Main Spring MVC controller for handling web requests related to search, URL insertion,
 * OpenAI integration, and system statistics.</p>
 *
 * <p>This controller interacts with an RMI backend service (via {@link IGateway}) to perform search operations,
 * retrieve statistics, and insert URLs into the system queue. It also manages view rendering, session state, and WebSocket messaging.</p>
 *
 * <p>Session attributes:</p>
 * <ul>
 *   <li><b>searchCache</b>: Cached search results for pagination and re-rendering</li>
 *   <li><b>inputCache</b>: Last user input for search</li>
 *   <li><b>openaiCache</b>: Cached result from OpenAI (if enabled)</li>
 *   <li><b>ResultOpenAICache</b>: Flag to track OpenAI result state</li>
 * </ul>
 */
@Controller
@SessionAttributes({"searchCache", "inputCache", "openaiCache", "ResultOpenAICache"})
public class WebController {

    /**
     * Initializes session attribute for storing cached search results.
     * @return an empty list of search results.
     */
    @ModelAttribute("searchCache")
    public List<SearchResult> searchCache() {
        return new ArrayList<>();
    }

    /**
     * Initializes session attribute for storing the last input query.
     * @return an empty string.
     */
    @ModelAttribute("inputCache")
    public String inputCache() {
        return "";
    }

    /**
     * Initializes session attribute for caching OpenAI result.
     * @return an empty string.
     */
    @ModelAttribute("openaiCache")
    public String openaiCache() {
        return "";
    }

    /**
     * Initializes session attribute for tracking OpenAI checkbox state.
     * @return "off" by default.
     */
    @ModelAttribute("ResultOpenAICache")
    public String ResultOpenAICache() {
        return "off";
    }

    @Autowired
    private RmiClientService rmiClientService; 

    private IGateway gateway;

    /**
     * Initializes the controller by retrieving the gateway from the RMI client.
     */
    @PostConstruct
    public void init() {
        // Initialize the gateway after RmiClientService is autowired
        this.gateway = rmiClientService.getGateway();
    }

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Redirects root URL to the main index page.
     * @return redirect string to /index.
     */
    @GetMapping("/")
    public String redirect() {
        return "redirect:/index";
    }

    /**
     * Renders the insert URL form page.
     * @return name of the insert-url template.
     */
    @GetMapping("/insert-url")
    public String showInsertUrlPage() {
        return "insert-url";
    }

    /**
     * Handles insertion of a URL into the indexing queue.
     * @param url The URL to insert.
     * @param prioritize Whether to insert it at the front of the queue.
     * @return Response indicating success or error.
     */
    @PostMapping("/insert-url")
    @ResponseBody
    public ResponseEntity<String> insertUrl(@RequestParam String url,
                                            @RequestParam(defaultValue = "false") boolean prioritize) {

        System.out.println("[DEBUG] prioritize:" + prioritize);
        try {
            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("URL cannot be empty.");
            }

            if (!isValidURL(url)) {
                return ResponseEntity.badRequest().body("Invalid URL format.");
            }

            if (prioritize) {
                gateway.addFirst(url);
            } else {
                gateway.insertURL(url);
            }

            return ResponseEntity.ok("URL successfully added to queue" + (prioritize ? " (prioritized).": "."));
        } catch (RemoteException e) {
            return ResponseEntity.status(500).body("Failed to insert URL: " + e.getMessage());
        }
    }

    /**
     * Displays the main search page, handles new searches, pagination,
     * and optionally integrates OpenAI results and HackerNews URLs insertions.
     *
     * @param input User input string.
     * @param page Page number for pagination.
     * @param hackerNews Whether to include HackerNews analysis.
     * @param ResultOpenAI Whether to request OpenAI-based results.
     * @param model Spring model.
     * @return view name to render.
     */
    @GetMapping("/index")
    public String showIndexPage(@RequestParam(value = "input", required = false) String input,
                                @RequestParam(value = "page", defaultValue = "1") Integer page,
                                @RequestParam(value = "hackerNews", defaultValue = "off") String hackerNews,
                                @RequestParam(value = "ResultOpenAI", defaultValue = "off") String ResultOpenAI,
                                Model model,
                                @ModelAttribute("searchCache") List<SearchResult> searchCache,
                                @ModelAttribute("inputCache") String inputCache,
                                @ModelAttribute("openaiCache") String openaiCache,
                                @ModelAttribute("ResultOpenAICache") String ResultOpenAICache) {
    
        boolean isHackerNews = "on".equals(hackerNews);
        boolean isResultOpenAI = "on".equals(ResultOpenAI);
    
        if (input == null || input.trim().isEmpty()) {
            return "index";
        }
    
        final int pageSize = 10;
        input = normalizeWords(input.trim());
        boolean isNewSearch = !input.equals(inputCache);
    
        System.out.println("DEBUG: hackerNews = " + hackerNews + ", ResultOpenAI = " + ResultOpenAI);
        System.out.println("------------------");
    
        String currentOpenAI = "";
    
        try {
            // Only perform a new search if input changed or HackerNews is triggered
            if (isNewSearch) {
                inputCache = input;
                model.addAttribute("inputCache", inputCache);
            }

            if (isNewSearch || isHackerNews) {
                List<String> stopwords = gateway.getStopwords();
                ArrayList<String> filteredSearch = new ArrayList<>();
    
                for (String word : input.split("\\s+")) {
                    if (!stopwords.contains(word)) {
                        filteredSearch.add(word);
                    }
                }
    
                List<SearchResult> freshResults = gateway.search(filteredSearch);
    
                if (isHackerNews) {
                    String content = String.join(" ", input);
                    gateway.hacker(content);
                }
    
                // Cache search results and input
                searchCache.clear();
                searchCache.addAll(freshResults);
                inputCache = input;
                model.addAttribute("searchCache", searchCache);
                model.addAttribute("inputCache", inputCache);
    
                // Call OpenAI if checkbox is on
                if (isResultOpenAI) {
                    currentOpenAI = gateway.getAI(input, freshResults);
                    openaiCache = currentOpenAI;
                    model.addAttribute("openaiCache", openaiCache);
                } else {
                    openaiCache = "";
                    model.addAttribute("openaiCache", openaiCache);
                }
            } else if (isResultOpenAI && (openaiCache == null || openaiCache.isEmpty())) {
                // Input hasn't changed, but we still need OpenAI result and it's not cached
                currentOpenAI = gateway.getAI(input, searchCache);
                openaiCache = currentOpenAI;
                model.addAttribute("openaiCache", openaiCache);
            }
    

            currentOpenAI = openaiCache;
            sendStatistics();
    
            int totalResults = searchCache.size();
            int totalPages = (int) Math.ceil((double) totalResults / pageSize);
    
            if (totalPages == 0) {
                model.addAttribute("message", "No results found for your query.");
                model.addAttribute("results", new ArrayList<>());
                if (isResultOpenAI) model.addAttribute("openai", openaiCache);
                model.addAttribute("input", input);
                model.addAttribute("currentPage", 1);
                model.addAttribute("totalPages", 0);
                return "result-search";
            }
    
            // Pagination
            page = Math.max(1, Math.min(page, totalPages));
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, totalResults);
            List<SearchResult> pageResults = searchCache.subList(start, end);
    
            // Add attributes for rendering
            model.addAttribute("results", pageResults);
            if (isResultOpenAI) model.addAttribute("openai", openaiCache);
            model.addAttribute("input", input);
            model.addAttribute("ResultOpenAI", ResultOpenAI);
            model.addAttribute("ResultOpenAICache", ResultOpenAI);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalResults", totalResults);
            model.addAttribute("prevPage", page > 1 ? page - 1 : 1);
            model.addAttribute("nextPage", page < totalPages ? page + 1 : totalPages);
    
            return "result-search";
    
        } catch (RemoteException e) {
            sendStatistics();
            model.addAttribute("error", "Search failed: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Retrieves URLs connected to a given input URL and renders the results page.
     *
     * @param input The input URL.
     * @param page Page number for pagination.
     * @param model Spring model.
     * @return View or error message.
     */
    @GetMapping("/url-connections")
    public Object checkConnections(@RequestParam(value = "input", required = false) String input,
                                 @RequestParam(defaultValue = "1") int page,
                                 Model model) {
        int size = 10;

        // Initial load, just show the form page
        if (input == null) {
            return "url-connections";
        }

        try {
            if (input == null || input.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("URL input cannot be empty.");
            }

            if (!isValidURL(input)) {
                return ResponseEntity.badRequest().body("Invalid URL format.");
            }

            SearchResult result = gateway.getConnections(input);
            List<String> urls = result.getUrls();

            if (urls == null || urls.isEmpty()) {
                model.addAttribute("error", "No results found for your query.");
                urls = Collections.emptyList();
            }

            sendStatistics();

            int totalResults = urls.size();
            int totalPages = (int) Math.ceil((double) totalResults / size);

            page = Math.max(1, Math.min(page, totalPages));
            int start = (page - 1) * size;
            int end = Math.min(start + size, totalResults);

            List<String> pageUrls = urls.subList(start, end);

            model.addAttribute("urls", pageUrls);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalResults", totalResults);
            model.addAttribute("input", input);
            model.addAttribute("prevPage", page > 1 ? page - 1 : 1);
            model.addAttribute("nextPage", page < totalPages ? page + 1 : totalPages);

            return "result-url-connections";
        } catch (RemoteException e) {
            sendStatistics();
            model.addAttribute("error", "Search failed: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Displays the system statistics page.
     *
     * @param model Spring model to inject statistics.
     * @return Name of the statistics view template.
     */
    @GetMapping("/statistics")
    public synchronized String showStatistics(Model model) {
        try {
            if (gateway == null) {
                model.addAttribute("error", "Gateway not initialized.");
                return "error";
            }
            SystemStatistics stats = gateway.getStatistics();

            model.addAttribute("topSearches", stats.getTopSearches());
            model.addAttribute("barrelSizes", stats.getBarrelIndexSizes());
            model.addAttribute("responseTimes", stats.getAverageResponseTimes());
        } catch (RemoteException e) {
            model.addAttribute("error", "Failed to fetch statistics: " + e.getMessage());
            return "error";
        }

        return "statistics";
    }

    /**
     * Sends real-time system statistics over WebSocket to subscribed clients.
     * Used by STOMP via @MessageMapping.
     */
    @MessageMapping("/statistics")
    public synchronized void sendStatistics() {
        try {
            SystemStatistics stats = gateway.getStatistics();  // Assuming the method fetches stats

            // Send the statistics to the topic for broadcasting to clients
            messagingTemplate.convertAndSend("/topicGloogloo/statistics", stats);  // Broadcast statistics
        } catch (Exception e) {
            // If there's an error, send the error message to the topic
            messagingTemplate.convertAndSend("/topicGloogloo/statistics", "Error fetching statistics: " + e.getMessage());
        }
    }

    /**
     * Displays a generic error page with optional message.
     *
     * @param message Optional error message.
     * @param model Spring model to hold message.
     * @return error page name.
     */
    @GetMapping("/error")
    public String showErrorPage(@RequestParam(name = "message", required = false) String message, Model model) {
        model.addAttribute("errorMessage", message != null ? message : "Unknown error");
        return "error"; // assuming error.html is your template
    }

    /**
     * Utility method to validate if a URL is in a valid format.
     * @param url URL string to validate.
     * @return true if valid, false otherwise.
     */
    private boolean isValidURL(String url) {
        String regex = "^(https?|ftp)://[^\s/$.?#].[^\s]*$";
        return url.matches(regex);
    }

    /**
     * Utility method to normalize and clean a user input string:
     * remove accents, punctuation, and convert to lowercase.
     *
     * @param text Input text.
     * @return Normalized text.
     */
    private String normalizeWords(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        text = text.replaceAll("[\\p{Punct}]", "").toLowerCase();
        return text;
    }
}

