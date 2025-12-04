package com.example.camunda.dto;

import java.util.Map;

/**
 * Response DTO containing process instance details after starting a workflow.
 */
public class ProcessInstanceResponse {
    
    private String processInstanceId;
    private String processDefinitionId;
    private String businessKey;
    private boolean isEnded;
    private boolean isSuspended;
    private Map<String, Object> variables;
    private String message;
    
    // Default constructor
    public ProcessInstanceResponse() {
    }
    
    // Builder-style static factory methods
    public static ProcessInstanceResponse success(String processInstanceId,
                                                  String processDefinitionId,
                                                  String businessKey) {
        ProcessInstanceResponse response = new ProcessInstanceResponse();
        response.processInstanceId = processInstanceId;
        response.processDefinitionId = processDefinitionId;
        response.businessKey = businessKey;
        response.isEnded = false;
        response.isSuspended = false;
        response.message = "Process instance started successfully";
        return response;
    }
    
    public static ProcessInstanceResponse error(String message) {
        ProcessInstanceResponse response = new ProcessInstanceResponse();
        response.message = message;
        return response;
    }
    
    // Getters and Setters
    public String getProcessInstanceId() {
        return processInstanceId;
    }
    
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
    
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }
    
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }
    
    public String getBusinessKey() {
        return businessKey;
    }
    
    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }
    
    public boolean isEnded() {
        return isEnded;
    }
    
    public void setEnded(boolean ended) {
        isEnded = ended;
    }
    
    public boolean isSuspended() {
        return isSuspended;
    }
    
    public void setSuspended(boolean suspended) {
        isSuspended = suspended;
    }
    
    public Map<String, Object> getVariables() {
        return variables;
    }
    
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "ProcessInstanceResponse{" +
                   "processInstanceId='" + processInstanceId + '\'' +
                   ", processDefinitionId='" + processDefinitionId + '\'' +
                   ", businessKey='" + businessKey + '\'' +
                   ", isEnded=" + isEnded +
                   ", isSuspended=" + isSuspended +
                   ", message='" + message + '\'' +
                   '}';
    }
}
