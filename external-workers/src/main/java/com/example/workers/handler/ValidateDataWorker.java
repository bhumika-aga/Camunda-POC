package com.example.workers.handler;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * External Task Worker for validating customer data.
 *
 * Topic: validateData
 *
 * This worker:
 * 1. Fetches tasks from the Camunda Engine with topic "validateData"
 * 2. Validates customer name (not empty, minimum length)
 * 3. Validates email format
 * 4. Validates document type
 * 5. On success: completes task with validation result variables
 * 6. On validation failure: throws BPMN error "VALIDATION_ERROR"
 * 7. On technical failure: fails task with retry
 */
@Component
public class ValidateDataWorker implements ExternalTaskHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidateDataWorker.class);
    
    // Email validation regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    // Valid document types
    private static final String[] VALID_DOCUMENT_TYPES = {"ID", "PASSPORT", "DRIVING_LICENSE"};
    
    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_TIMEOUT = 5000L; // 5 seconds
    
    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String taskId = externalTask.getId();
        String processInstanceId = externalTask.getProcessInstanceId();
        String businessKey = externalTask.getBusinessKey();
        
        logger.info("========================================");
        logger.info("VALIDATE DATA WORKER - Task Received");
        logger.info("========================================");
        logger.info("Task ID: {}", taskId);
        logger.info("Process Instance ID: {}", processInstanceId);
        logger.info("Business Key: {}", businessKey);
        
        try {
            // Extract variables from the task
            String customerName = externalTask.getVariable("customerName");
            String email = externalTask.getVariable("email");
            String documentType = externalTask.getVariable("documentType");
            
            logger.info("Input Variables:");
            logger.info("  - Customer Name: {}", customerName);
            logger.info("  - Email: {}", email);
            logger.info("  - Document Type: {}", documentType);
            
            // Perform validation
            ValidationResult validationResult = validateCustomerData(customerName, email, documentType);
            
            if (validationResult.isValid()) {
                // Validation successful - complete the task
                logger.info("Validation PASSED for customer: {}", customerName);
                
                Map<String, Object> outputVariables = new HashMap<>();
                outputVariables.put("isValid", true);
                outputVariables.put("validationMessage", validationResult.message());
                outputVariables.put("validatedAt", System.currentTimeMillis());
                
                externalTaskService.complete(externalTask, outputVariables);
                
                logger.info("Task completed successfully with variables: {}", outputVariables);
                
            } else {
                // Validation failed - throw BPMN error
                logger.warn("Validation FAILED for customer: {}. Reason: {}",
                    customerName, validationResult.message());
                
                // Throw BPMN Error - this will trigger the Error Boundary Event
                externalTaskService.handleBpmnError(
                    externalTask,
                    "VALIDATION_ERROR",
                    validationResult.message(),
                    Map.of(
                        "isValid", false,
                        "validationMessage", validationResult.message(),
                        "errorCode", "VALIDATION_ERROR"
                    )
                );
                
                logger.info("BPMN Error 'VALIDATION_ERROR' thrown. Process will follow error boundary path.");
            }
            
        } catch (Exception e) {
            // Technical failure - fail the task with retry
            logger.error("Technical error in ValidateDataWorker: {}", e.getMessage(), e);
            
            int retries = externalTask.getRetries() != null ? externalTask.getRetries() : MAX_RETRIES;
            int remainingRetries = retries - 1;
            
            if (remainingRetries > 0) {
                logger.info("Failing task with {} retries remaining. Will retry in {} ms",
                    remainingRetries, RETRY_TIMEOUT);
                
                externalTaskService.handleFailure(
                    externalTask,
                    "Technical error: " + e.getMessage(),
                    e.toString(),
                    remainingRetries,
                    RETRY_TIMEOUT
                );
            } else {
                logger.error("No retries remaining. Task will be marked as failed permanently.");
                
                externalTaskService.handleFailure(
                    externalTask,
                    "Technical error (no retries left): " + e.getMessage(),
                    e.toString(),
                    0,
                    0L
                );
            }
        }
        
        logger.info("========================================\n");
    }
    
    /**
     * Validates customer data and returns the result.
     */
    private ValidationResult validateCustomerData(String customerName, String email, String documentType) {
        StringBuilder errors = new StringBuilder();
        
        // Validate customer name
        if (customerName == null || customerName.trim().isEmpty()) {
            errors.append("Customer name is required. ");
        } else if (customerName.trim().length() < 2) {
            errors.append("Customer name must be at least 2 characters. ");
        }
        
        // Validate email
        if (email == null || email.trim().isEmpty()) {
            errors.append("Email is required. ");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.append("Invalid email format. ");
        }
        
        // Validate document type
        if (documentType == null || documentType.trim().isEmpty()) {
            errors.append("Document type is required. ");
        } else {
            boolean validDocType = false;
            for (String validType : VALID_DOCUMENT_TYPES) {
                if (validType.equalsIgnoreCase(documentType.trim())) {
                    validDocType = true;
                    break;
                }
            }
            if (!validDocType) {
                errors.append("Invalid document type. Allowed: ID, PASSPORT, DRIVING_LICENSE. ");
            }
        }
        
        // Return result
        if (errors.isEmpty()) {
            return new ValidationResult(true, "All validations passed successfully.");
        } else {
            return new ValidationResult(false, errors.toString().trim());
        }
    }
    
    /**
     * Simple record to hold validation results.
     */
    private record ValidationResult(boolean isValid, String message) {
    }
}
