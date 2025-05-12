package com.example.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Main entry point for the Spring Boot Frontend application.</p>
 * 
 * <p>This class is responsible for bootstrapping the Spring application context and 
 * launching the web frontend server.</p>
 */
@SpringBootApplication
public class FrontendApplication {

    /**
     * <p>Launches the Spring Boot application.</p>
     *
     * @param args command-line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}

