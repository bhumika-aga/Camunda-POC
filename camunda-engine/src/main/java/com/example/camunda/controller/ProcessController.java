package com.example.camunda.controller;

import com.example.camunda.dto.ProcessInstanceResponse;
import com.example.camunda.dto.StartProcessRequest;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing Customer Onboarding process instances.
 *
 * Endpoints:
 * - POST /api/process/start        - Start a new process instance
 * - GET  /api/process/{id}/status  - Get process instance status
 * - GET  /api/process/tasks        - List all pending tasks
 * - POST /api/process/tasks/{id}/complete - Complete a task
 */
@RestController
@RequestMapping("/api/process")
public class ProcessController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessController.class);
    
    private static final String PROCESS_DEFINITION_KEY = "customer_onboarding";
    
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    
    public ProcessController(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }
    
    /**
     * Start a new Customer Onboarding process instance.
     *
     * curl -X POST http://localhost:8080/api/process/start \
     * -H "Content-Type: application/json" \
     * -d '{"customerName":"Amit Sharma","email":"amit@example.com","documentType":"PASSPORT"}'
     */
    @PostMapping("/start")
    public ResponseEntity<ProcessInstanceResponse> startProcess(@RequestBody StartProcessRequest request) {
        logger.info("Starting Customer Onboarding process for: {}", request);
        
        try {
            // Generate a unique business key
            String businessKey = "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // Prepare process variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", request.getCustomerName());
            variables.put("email", request.getEmail());
            variables.put("documentType", request.getDocumentType() != null ? request.getDocumentType() : "ID");
            variables.put("businessKey", businessKey);
            
            // Start the process instance
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_DEFINITION_KEY,
                businessKey,
                variables
            );
            
            logger.info("Process instance started successfully. ID: {}, BusinessKey: {}",
                processInstance.getId(), businessKey);
            
            ProcessInstanceResponse response = ProcessInstanceResponse.success(
                processInstance.getId(),
                processInstance.getProcessDefinitionId(),
                businessKey
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Failed to start process instance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                       .body(ProcessInstanceResponse.error("Failed to start process: " + e.getMessage()));
        }
    }
    
    /**
     * Get the status of a process instance.
     *
     * curl http://localhost:8080/api/process/{processInstanceId}/status
     */
    @GetMapping("/{processInstanceId}/status")
    public ResponseEntity<Map<String, Object>> getProcessStatus(@PathVariable("processInstanceId") String processInstanceId) {
        logger.info("Getting status for process instance: {}", processInstanceId);
        
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Check if process is still running
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                                                  .processInstanceId(processInstanceId)
                                                  .singleResult();
            
            if (processInstance != null) {
                status.put("processInstanceId", processInstanceId);
                status.put("isRunning", true);
                status.put("isEnded", processInstance.isEnded());
                status.put("isSuspended", processInstance.isSuspended());
                
                // Get current activity
                List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
                status.put("activeActivities", activeActivityIds);
                
                // Get process variables
                Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
                status.put("variables", variables);
                
                // Get pending tasks
                List<Task> tasks = taskService.createTaskQuery()
                                       .processInstanceId(processInstanceId)
                                       .list();
                
                List<Map<String, String>> taskList = tasks.stream()
                                                         .map(task -> {
                                                             Map<String, String> taskMap = new HashMap<>();
                                                             taskMap.put("taskId", task.getId());
                                                             taskMap.put("taskName", task.getName());
                                                             taskMap.put("assignee", task.getAssignee());
                                                             return taskMap;
                                                         })
                                                         .toList();
                
                status.put("pendingTasks", taskList);
            } else {
                // Process has ended - check history
                status.put("processInstanceId", processInstanceId);
                status.put("isRunning", false);
                status.put("isEnded", true);
                status.put("message", "Process instance has completed or does not exist");
            }
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Failed to get process status: {}", e.getMessage(), e);
            status.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(status);
        }
    }
    
    /**
     * List all pending user tasks (optionally filtered by assignee group).
     *
     * curl http://localhost:8080/api/process/tasks
     * curl http://localhost:8080/api/process/tasks?candidateGroup=employees
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> listTasks(
        @RequestParam(value = "candidateGroup", required = false) String candidateGroup) {
        
        logger.info("Listing tasks for candidateGroup: {}", candidateGroup);
        
        try {
            var taskQuery = taskService.createTaskQuery();
            
            if (candidateGroup != null && !candidateGroup.isEmpty()) {
                taskQuery.taskCandidateGroup(candidateGroup);
            }
            
            List<Task> tasks = taskQuery.list();
            
            List<Map<String, Object>> taskList = tasks.stream()
                                                     .map(task -> {
                                                         Map<String, Object> taskMap = new HashMap<>();
                                                         taskMap.put("taskId", task.getId());
                                                         taskMap.put("taskName", task.getName());
                                                         taskMap.put("taskDefinitionKey", task.getTaskDefinitionKey());
                                                         taskMap.put("processInstanceId", task.getProcessInstanceId());
                                                         taskMap.put("assignee", task.getAssignee());
                                                         taskMap.put("createTime", task.getCreateTime());
                                                         
                                                         // Get task variables
                                                         Map<String, Object> variables = taskService.getVariables(task.getId());
                                                         taskMap.put("variables", variables);
                                                         
                                                         return taskMap;
                                                     })
                                                     .toList();
            
            return ResponseEntity.ok(taskList);
            
        } catch (Exception e) {
            logger.error("Failed to list tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
    
    /**
     * Complete a user task with variables.
     *
     * curl -X POST http://localhost:8080/api/process/tasks/{taskId}/complete \
     * -H "Content-Type: application/json" \
     * -d '{"documentsApproved": true, "reviewerComments": "All documents verified"}'
     */
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
        @PathVariable("taskId") String taskId,
        @RequestBody(required = false) Map<String, Object> variables) {
        
        logger.info("Completing task: {} with variables: {}", taskId, variables);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Task task = taskService.createTaskQuery()
                            .taskId(taskId)
                            .singleResult();
            
            if (task == null) {
                response.put("success", false);
                response.put("message", "Task not found: " + taskId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Complete the task with provided variables
            if (variables != null && !variables.isEmpty()) {
                taskService.complete(taskId, variables);
            } else {
                taskService.complete(taskId);
            }
            
            response.put("success", true);
            response.put("message", "Task completed successfully");
            response.put("taskId", taskId);
            response.put("taskName", task.getName());
            
            logger.info("Task completed successfully: {}", taskId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to complete task: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to complete task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get all running process instances.
     *
     * curl http://localhost:8080/api/process/instances
     */
    @GetMapping("/instances")
    public ResponseEntity<List<Map<String, Object>>> listProcessInstances() {
        logger.info("Listing all running process instances");
        
        try {
            List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                                                  .processDefinitionKey(PROCESS_DEFINITION_KEY)
                                                  .list();
            
            List<Map<String, Object>> instanceList = instances.stream()
                                                         .map(instance -> {
                                                             Map<String, Object> instanceMap = new HashMap<>();
                                                             instanceMap.put("processInstanceId", instance.getId());
                                                             instanceMap.put("processDefinitionId", instance.getProcessDefinitionId());
                                                             instanceMap.put("businessKey", instance.getBusinessKey());
                                                             instanceMap.put("isSuspended", instance.isSuspended());
                                                             
                                                             // Get active activities
                                                             List<String> activeActivities = runtimeService.getActiveActivityIds(instance.getId());
                                                             instanceMap.put("activeActivities", activeActivities);
                                                             
                                                             return instanceMap;
                                                         })
                                                         .toList();
            
            return ResponseEntity.ok(instanceList);
            
        } catch (Exception e) {
            logger.error("Failed to list process instances: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
    
    /**
     * Delete/cancel a process instance.
     *
     * curl -X DELETE http://localhost:8080/api/process/{processInstanceId}
     */
    @DeleteMapping("/{processInstanceId}")
    public ResponseEntity<Map<String, Object>> deleteProcessInstance(@PathVariable("processInstanceId") String processInstanceId) {
        logger.info("Deleting process instance: {}", processInstanceId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            runtimeService.deleteProcessInstance(processInstanceId, "Cancelled via API");
            response.put("success", true);
            response.put("message", "Process instance deleted successfully");
            response.put("processInstanceId", processInstanceId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to delete process instance: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to delete process instance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
