package com.example.camunda.dto;

/**
 * Request DTO for starting a new Customer Onboarding process instance.
 *
 * Example JSON:
 * {
 * "customerName": "Amit Sharma",
 * "email": "amit@example.com",
 * "documentType": "PASSPORT"
 * }
 */
public class StartProcessRequest {
    
    private String customerName;
    private String email;
    private String documentType;
    
    // Default constructor
    public StartProcessRequest() {
    }
    
    // All-args constructor
    public StartProcessRequest(String customerName, String email, String documentType) {
        this.customerName = customerName;
        this.email = email;
        this.documentType = documentType;
    }
    
    // Getters and Setters
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    @Override
    public String toString() {
        return "StartProcessRequest{" +
                   "customerName='" + customerName + '\'' +
                   ", email='" + email + '\'' +
                   ", documentType='" + documentType + '\'' +
                   '}';
    }
}
