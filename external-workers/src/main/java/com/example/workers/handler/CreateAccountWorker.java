package com.example.workers.handler;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * External Task Worker for creating customer accounts.
 *
 * Topic: createAccount
 *
 * This worker:
 * 1. Fetches tasks from the Camunda Engine with topic "createAccount"
 * 2. Simulates account creation in a backend system
 * 3. Generates a unique account ID
 * 4. Sets account status and metadata
 * 5. Completes the task with account details
 * 6. On failure: retries with exponential backoff
 */
@Component
public class CreateAccountWorker implements ExternalTaskHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CreateAccountWorker.class);
    
    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long BASE_RETRY_TIMEOUT = 5000L; // 5 seconds base
    
    // Date formatter for timestamps
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String taskId = externalTask.getId();
        String processInstanceId = externalTask.getProcessInstanceId();
        String businessKey = externalTask.getBusinessKey();
        
        logger.info("========================================");
        logger.info("CREATE ACCOUNT WORKER - Task Received");
        logger.info("========================================");
        logger.info("Task ID: {}", taskId);
        logger.info("Process Instance ID: {}", processInstanceId);
        logger.info("Business Key: {}", businessKey);
        
        try {
            // Extract variables from the task
            String customerName = externalTask.getVariable("customerName");
            String email = externalTask.getVariable("email");
            String documentType = externalTask.getVariable("documentType");
            String reviewerComments = externalTask.getVariable("reviewerComments");
            Boolean documentsApproved = externalTask.getVariable("documentsApproved");
            
            logger.info("Input Variables:");
            logger.info("  - Customer Name: {}", customerName);
            logger.info("  - Email: {}", email);
            logger.info("  - Document Type: {}", documentType);
            logger.info("  - Documents Approved: {}", documentsApproved);
            logger.info("  - Reviewer Comments: {}", reviewerComments);
            
            // Simulate account creation delay (mimics real system call)
            logger.info("Creating account in backend system...");
            Thread.sleep(1000); // Simulate 1 second processing time
            
            // Generate account details
            String accountId = generateAccountId();
            String accountStatus = "ACTIVE";
            String createdAt = LocalDateTime.now().format(DATE_FORMATTER);
            
            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("accountId", accountId);
            outputVariables.put("accountStatus", accountStatus);
            outputVariables.put("accountCreatedAt", createdAt);
            outputVariables.put("accountEmail", email);
            outputVariables.put("accountHolder", customerName);
            outputVariables.put("onboardingCompleted", true);
            
            // Log the created account
            logger.info("========================================");
            logger.info("ACCOUNT CREATED SUCCESSFULLY");
            logger.info("========================================");
            logger.info("  Account ID: {}", accountId);
            logger.info("  Account Holder: {}", customerName);
            logger.info("  Email: {}", email);
            logger.info("  Status: {}", accountStatus);
            logger.info("  Created At: {}", createdAt);
            logger.info("========================================");
            
            // Complete the task
            externalTaskService.complete(externalTask, outputVariables);
            
            logger.info("Task completed successfully. Account {} is now ACTIVE.", accountId);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleFailure(externalTask, externalTaskService, e);
        } catch (Exception e) {
            handleFailure(externalTask, externalTaskService, e);
        }
        
        logger.info("========================================\n");
    }
    
    /**
     * Generates a unique account ID.
     * Format: ACC-XXXXXXXX (8 character alphanumeric)
     */
    private String generateAccountId() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ACC-" + uuid;
    }
    
    /**
     * Handles task failure with exponential backoff retry.
     */
    private void handleFailure(ExternalTask externalTask,
                               ExternalTaskService externalTaskService,
                               Exception e) {
        logger.error("Error in CreateAccountWorker: {}", e.getMessage(), e);
        
        int currentRetries = externalTask.getRetries() != null ? externalTask.getRetries() : MAX_RETRIES;
        int remainingRetries = currentRetries - 1;
        
        if (remainingRetries > 0) {
            // Calculate exponential backoff: 5s, 10s, 20s
            long retryTimeout = BASE_RETRY_TIMEOUT * (long) Math.pow(2, MAX_RETRIES - remainingRetries - 1);
            
            logger.warn("Failing task with {} retries remaining. Will retry in {} ms",
                remainingRetries, retryTimeout);
            
            externalTaskService.handleFailure(
                externalTask,
                "Account creation failed: " + e.getMessage(),
                getStackTraceAsString(e),
                remainingRetries,
                retryTimeout
            );
        } else {
            logger.error("No retries remaining. Task will be marked as failed permanently.");
            
            externalTaskService.handleFailure(
                externalTask,
                "Account creation failed permanently: " + e.getMessage(),
                getStackTraceAsString(e),
                0,
                0L
            );
        }
    }
    
    /**
     * Converts exception stack trace to string for error details.
     */
    private String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 1000) {
                sb.append("... (truncated)");
                break;
            }
        }
        return sb.toString();
    }
}
