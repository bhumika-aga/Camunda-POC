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

/**
 * External Task Worker for handling validation errors.
 *
 * Topic: handleError
 *
 * This worker:
 * 1. Fetches tasks from the Camunda Engine with topic "handleError"
 * 2. Logs the error details
 * 3. Sets error flags and messages for process tracking
 * 4. Could be extended to send notifications, log to external systems, etc.
 */
@Component
public class HandleErrorWorker implements ExternalTaskHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(HandleErrorWorker.class);
    
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String taskId = externalTask.getId();
        String processInstanceId = externalTask.getProcessInstanceId();
        String businessKey = externalTask.getBusinessKey();
        
        logger.info("========================================");
        logger.info("HANDLE ERROR WORKER - Task Received");
        logger.info("========================================");
        logger.info("Task ID: {}", taskId);
        logger.info("Process Instance ID: {}", processInstanceId);
        logger.info("Business Key: {}", businessKey);
        
        try {
            // Extract variables from the task
            String customerName = externalTask.getVariable("customerName");
            String email = externalTask.getVariable("email");
            String validationMessage = externalTask.getVariable("validationMessage");
            String errorCode = externalTask.getVariable("errorCode");
            
            logger.warn("========================================");
            logger.warn("ERROR HANDLING DETAILS");
            logger.warn("========================================");
            logger.warn("  Customer Name: {}", customerName);
            logger.warn("  Email: {}", email);
            logger.warn("  Error Code: {}", errorCode);
            logger.warn("  Validation Message: {}", validationMessage);
            logger.warn("========================================");
            
            // Prepare output variables for error tracking
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("errorOccurred", true);
            outputVariables.put("errorMessage", validationMessage != null ? validationMessage : "Unknown error");
            outputVariables.put("errorHandledAt", LocalDateTime.now().format(DATE_FORMATTER));
            outputVariables.put("errorHandledBy", "HandleErrorWorker");
            outputVariables.put("onboardingCompleted", false);
            outputVariables.put("onboardingStatus", "FAILED_VALIDATION");
            
            // In a real scenario, you might:
            // - Send email notification to customer
            // - Log to external monitoring system
            // - Create support ticket
            // - Trigger compensation workflow
            
            logger.info("Error handling completed. Setting error flags.");
            
            // Complete the task
            externalTaskService.complete(externalTask, outputVariables);
            
            logger.info("Task completed. Process will end with FAILED status.");
            
        } catch (Exception e) {
            logger.error("Error in HandleErrorWorker: {}", e.getMessage(), e);
            
            // For error handling worker, we typically don't want to fail
            // Just complete with error details
            Map<String, Object> errorVariables = new HashMap<>();
            errorVariables.put("errorOccurred", true);
            errorVariables.put("errorMessage", "Error handler failed: " + e.getMessage());
            errorVariables.put("errorHandledAt", LocalDateTime.now().format(DATE_FORMATTER));
            errorVariables.put("onboardingCompleted", false);
            
            externalTaskService.complete(externalTask, errorVariables);
        }
        
        logger.info("========================================\n");
    }
}
