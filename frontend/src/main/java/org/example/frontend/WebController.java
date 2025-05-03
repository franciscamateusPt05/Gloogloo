package com.example.frontend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.rmi.RemoteException;

import com.example.frontend.beans.Number;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
public class WebController {

    @Autowired
    private IGateway gateway;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Resource(name = "requestScopedNumberGenerator")
    private Number nRequest;

    @Resource(name = "sessionScopedNumberGenerator")
    private Number nSession;

    @Resource(name = "applicationScopedNumberGenerator")
    private Number nApplication;

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Number requestScopedNumberGenerator() {
        return new Number();
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Number sessionScopedNumberGenerator() {
        return new Number();
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_APPLICATION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Number applicationScopedNumberGenerator() {
        return new Number();
    }

    @GetMapping("/")
    public String redirect() {
        return "redirect:/index";
    }

    @PostMapping("/insert-url")
    public String insertUrl(@RequestParam String url,
                            @RequestParam(defaultValue = "false") boolean prioritize,
                            Model model) {
        try {
            if (url == null || url.trim().isEmpty()) {
                model.addAttribute("error", "URL cannot be empty.");
                return "error";
            }

            if (!isValidURL(url)) {
                model.addAttribute("error", "Invalid URL format.");
                return "error";
            }

            if (prioritize) {
                gateway.addFirst(url);
            } else {
                gateway.insertURL(url);
            }

            model.addAttribute("success", "URL successfully added to queue" + (prioritize ? " (prioritized)." : "."));
        } catch (RemoteException e) {
            model.addAttribute("error", "Failed to insert URL: " + e.getMessage());
            return "error";
        }

        return "result-url";
    }



    @GetMapping("/index")
    public String search(@RequestParam String input, @RequestParam(defaultValue = "1") int page, Model model) {
        int size = 10;

        try {
            if (input == null || input.trim().isEmpty()) {
                model.addAttribute("error", "Search query cannot be empty.");
                return "error";
            }

            String normalizedQuery = normalizeWords(input);
            List<String> stopwords = gateway.getStopwords();

            List<String> filteredSearch = Arrays.stream(normalizedQuery.trim().split("\\s+"))
                .filter(word -> !stopwords.contains(word.trim()))
                .map(String::trim)
                .collect(Collectors.toList());

            if (filteredSearch.isEmpty()) {
                model.addAttribute("error", "Search query contains only stopwords.");
                return "error";
            }

            List<SearchResult> results = gateway.search(filteredSearch);

            // Pagination logic
            int totalResults = results.size();
            int totalPages = (int) Math.ceil((double) totalResults / size);

            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;

            int start = (page - 1) * size;
            int end = Math.min(start + size, totalResults);

            List<SearchResult> pageResults = results.subList(start, end);

            model.addAttribute("results", pageResults);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalResults", totalResults);
            model.addAttribute("input", input);

        } catch (RemoteException e) {
            model.addAttribute("error", "Search failed: " + e.getMessage());
            return "error";
        }

        return "result-search";
    }

    @GetMapping("/url-connections")
    public String getConnections(@RequestParam String input, @RequestParam(defaultValue = "1") int page, Model model) {
        int size = 10;

        try {
            if (input == null || input.trim().isEmpty()) {
                model.addAttribute("error", "URL input cannot be empty.");
                return "error";
            }

            if (!isValidURL(input)) {
                model.addAttribute("error", "Invalid URL format.");
                return "error";
            }

            SearchResult result = gateway.getConnections(input);
            List<String> urls = result.getUrls();

            int totalResults = urls.size();
            int totalPages = (int) Math.ceil((double) totalResults / size);

            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;

            int start = (page - 1) * size;
            int end = Math.min(start + size, totalResults);
            List<String> pageUrls = urls.subList(start, end);

            model.addAttribute("urls", pageUrls);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalResults", totalResults);
            model.addAttribute("input", input);

        } catch (RemoteException e) {
            model.addAttribute("error", "Failed to get connections: " + e.getMessage());
            return "error";
        }

        return "result-url-connections";
    }


    @GetMapping("/statistics")
    public String showStatistics(Model model) {
        try {
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

    @MessageMapping("/statistics")  // Endpoint where clients request statistics
    public void sendStatistics() {
        try {
            SystemStatistics stats = gateway.getStatistics();  // Assuming the method fetches stats

            // Send the statistics to the topic for broadcasting to clients
            messagingTemplate.convertAndSend("/topicGloogloo/statistics", stats);  // Broadcast statistics
        } catch (Exception e) {
            // If there's an error, send the error message to the topic
            messagingTemplate.convertAndSend("/topicGloogloo/statistics", "Error fetching statistics: " + e.getMessage());
        }
    }

    @GetMapping("/error")
    public String showError(@RequestParam String message, Model model) {
        model.addAttribute("error", message);
        return "error";  // Return the error page with the error message
    }

    private boolean isValidURL(String url) {
        String regex = "^(https?|ftp)://[^\s/$.?#].[^\s]*$";
        return url.matches(regex);
    }

    private String normalizeWords(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        text = text.replaceAll("[\\p{Punct}]", "").toLowerCase();
        return text;
    }
}

// Exception Handler Class (ControllerAdvice)
@ControllerAdvice
class GlobalExceptionHandler {

    // Handles all exceptions globally
    @ExceptionHandler(Exception.class)
    public String handleError(Exception e, Model model) {
        model.addAttribute("error", "An error occurred: " + e.getMessage());
        return "error";
    }

    // Handle specific exceptions like RemoteException
    @ExceptionHandler(RemoteException.class)
    public String handleRemoteException(RemoteException e, Model model) {
        model.addAttribute("error", "Remote exception occurred: " + e.getMessage());
        return "error";
    }
}