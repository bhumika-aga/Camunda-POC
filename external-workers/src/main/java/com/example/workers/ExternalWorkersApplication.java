package com.example.workers;

import com.example.workers.handler.CreateAccountWorker;
import com.example.workers.handler.HandleErrorWorker;
import com.example.workers.handler.ValidateDataWorker;
import org.camunda.bpm.client.ExternalTaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CountDownLatch;

/**
 * Main Spring Boot Application for External Task Workers.
 *
 * This application:
 * - Connects to the Camunda Engine REST API
 * - Polls for external tasks on subscribed topics
 * - Processes tasks using the configured handlers
 *
 * Topics handled:
 * - validateData: Validates customer data
 * - createAccount: Creates customer account
 * - handleError: Handles validation errors
 *
 * Run with: mvn -pl external-workers spring-boot:run
 */
@SpringBootApplication
public class ExternalWorkersApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalWorkersApplication.class);
    
    // Latch to keep the application running
    private static final CountDownLatch keepAliveLatch = new CountDownLatch(1);
    
    public static void main(String[] args) {
        SpringApplication.run(ExternalWorkersApplication.class, args);
        
        // Add shutdown hook to release latch on SIGTERM/SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received. Stopping workers...");
            keepAliveLatch.countDown();
        }));
        
        // Keep the main thread alive
        try {
            keepAliveLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Main thread interrupted. Exiting...");
        }
    }
    
    /**
     * CommandLineRunner to start the external task client and subscribe handlers.
     */
    @Bean
    public CommandLineRunner startExternalTaskClient(
        ExternalTaskClient externalTaskClient,
        ValidateDataWorker validateDataWorker,
        CreateAccountWorker createAccountWorker,
        HandleErrorWorker handleErrorWorker) {
        
        return args -> {
            logger.info("==========================================================");
            logger.info("  EXTERNAL WORKERS STARTING");
            logger.info("==========================================================");
            
            // Subscribe to validateData topic
            externalTaskClient.subscribe("validateData")
                .lockDuration(30000)
                .handler(validateDataWorker)
                .open();
            logger.info("  Subscribed to topic: validateData");
            
            // Subscribe to createAccount topic
            externalTaskClient.subscribe("createAccount")
                .lockDuration(30000)
                .handler(createAccountWorker)
                .open();
            logger.info("  Subscribed to topic: createAccount");
            
            // Subscribe to handleError topic
            externalTaskClient.subscribe("handleError")
                .lockDuration(10000)
                .handler(handleErrorWorker)
                .open();
            logger.info("  Subscribed to topic: handleError");
            
            logger.info("==========================================================");
            logger.info("  EXTERNAL WORKERS STARTED SUCCESSFULLY");
            logger.info("==========================================================");
            logger.info("  Polling Camunda Engine at: http://localhost:8080/engine-rest");
            logger.info("  Topics subscribed:");
            logger.info("    - validateData (Validate customer data)");
            logger.info("    - createAccount (Create customer account)");
            logger.info("    - handleError (Handle validation errors)");
            logger.info("==========================================================");
            logger.info("  Workers are now polling for tasks...");
            logger.info("  Press Ctrl+C to stop");
            logger.info("==========================================================");
        };
    }
}
