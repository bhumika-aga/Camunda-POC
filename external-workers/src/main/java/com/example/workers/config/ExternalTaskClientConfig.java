package com.example.workers.config;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.backoff.ExponentialBackoffStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Camunda External Task Client.
 *
 * This configures the client that connects to the Camunda Engine's REST API
 * and polls for external tasks.
 */
@Configuration
public class ExternalTaskClientConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalTaskClientConfig.class);
    
    @Value("${camunda.client.base-url:http://localhost:8080/engine-rest}")
    private String baseUrl;
    
    @Value("${camunda.client.worker-id:external-worker-1}")
    private String workerId;
    
    @Value("${camunda.client.max-tasks:10}")
    private int maxTasks;
    
    @Value("${camunda.client.lock-duration:30000}")
    private long lockDuration;
    
    @Value("${camunda.client.async-response-timeout:20000}")
    private long asyncResponseTimeout;
    
    /**
     * Creates and configures the Camunda External Task Client.
     *
     * The client will:
     * - Connect to the Camunda Engine REST API
     * - Use long polling to fetch tasks
     * - Apply exponential backoff on errors
     * - Automatically subscribe handlers annotated with @ExternalTaskSubscription
     */
    @Bean
    public ExternalTaskClient externalTaskClient() {
        logger.info("========================================");
        logger.info("Configuring External Task Client");
        logger.info("========================================");
        logger.info("Base URL: {}", baseUrl);
        logger.info("Worker ID: {}", workerId);
        logger.info("Max Tasks: {}", maxTasks);
        logger.info("Lock Duration: {} ms", lockDuration);
        logger.info("Async Response Timeout: {} ms", asyncResponseTimeout);
        logger.info("========================================");
        
        ExternalTaskClient client = ExternalTaskClient.create()
                                        .baseUrl(baseUrl)
                                        .workerId(workerId)
                                        .maxTasks(maxTasks)
                                        .lockDuration(lockDuration)
                                        .asyncResponseTimeout(asyncResponseTimeout)
                                        // Exponential backoff: starts at 500ms, max 60s, factor 2
                                        .backoffStrategy(new ExponentialBackoffStrategy(500L, 2.0f, 60000L))
                                        .build();
        
        logger.info("External Task Client created successfully");
        
        return client;
    }
}
