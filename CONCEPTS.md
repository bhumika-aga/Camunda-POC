# Camunda 7 - Technical Concepts and Implementation Guide

This document explains the technical concepts, architecture decisions, and implementation details of the Customer Onboarding Camunda 7 project.

---

## Table of Contents

1. [What is Camunda?](#what-is-camunda)
2. [BPMN 2.0 Overview](#bpmn-20-overview)
3. [External Task Pattern](#external-task-pattern)
4. [Project Architecture](#project-architecture)
5. [BPMN Process Design](#bpmn-process-design)
6. [External Task Workers](#external-task-workers)
7. [Error Handling Strategies](#error-handling-strategies)
8. [Process Variables](#process-variables)
9. [User Tasks and Forms](#user-tasks-and-forms)
10. [REST API Integration](#rest-api-integration)
11. [Why Not JavaDelegates?](#why-not-javadelegates)
12. [Production Considerations](#production-considerations)

---

## What is Camunda?

**Camunda** is an open-source workflow and decision automation platform. It allows you to:

- **Model** business processes using BPMN 2.0
- **Execute** workflows with a process engine
- **Monitor** running processes via web applications
- **Integrate** with any technology stack

### Camunda 7 vs Camunda 8

| Feature | Camunda 7 | Camunda 8 |
|---------|-----------|-----------|
| Architecture | Embedded/Shared Engine | Cloud-Native (Zeebe) |
| Database | RDBMS (H2, PostgreSQL, etc.) | Own Storage (RocksDB) |
| Deployment | WAR/JAR/Spring Boot | Kubernetes/SaaS |
| External Tasks | REST API Polling | gRPC Streaming |
| Best For | Traditional enterprise apps | Cloud-native microservices |

This project uses **Camunda 7** with the External Task pattern.

---

## BPMN 2.0 Overview

**BPMN (Business Process Model and Notation)** is an ISO standard for process modeling.

### Key BPMN Elements Used

#### Events

```txt
○ Start Event    - Process entry point
◉ End Event      - Process completion
⊗ Error Event    - Catches/throws errors
```

#### Tasks

```txt
┌─────────────┐
│ User Task   │  - Requires human interaction
└─────────────┘

┌─────────────┐
│Service Task │  - Automated processing (External Task)
└─────────────┘
```

#### Gateways

```txt
    ◇
   / \        Exclusive Gateway - XOR routing based on conditions
   \ /
```

#### Flows

```txt
────────>     Sequence Flow - Normal process flow
- - - - >     Conditional Flow - Based on expression
```

### BPMN XML Structure

```xml
<bpmn:process id="customer_onboarding" isExecutable="true">
  <bpmn:startEvent id="start">
    <bpmn:outgoing>flow1</bpmn:outgoing>
  </bpmn:startEvent>

  <bpmn:serviceTask id="task1" camunda:type="external" camunda:topic="validateData">
    <bpmn:incoming>flow1</bpmn:incoming>
    <bpmn:outgoing>flow2</bpmn:outgoing>
  </bpmn:serviceTask>

  <bpmn:sequenceFlow id="flow1" sourceRef="start" targetRef="task1"/>
</bpmn:process>
```

---

## External Task Pattern

The **External Task Pattern** is a key architectural pattern in Camunda for building scalable, decoupled systems.

### How It Works

```txt
┌─────────────────┐         ┌─────────────────┐
│  Camunda Engine │◄───────►│ External Worker │
│   (REST API)    │  HTTP   │   (Separate)    │
└─────────────────┘         └─────────────────┘
         │
         │ 1. Engine creates task with topic
         │ 2. Worker polls for tasks on topic
         │ 3. Worker locks and processes task
         │ 4. Worker completes/fails task
         ▼
```

### Task Lifecycle

```txt
CREATED → LOCKED → COMPLETED
              ↓
           FAILED → RETRY → LOCKED
              ↓
           INCIDENT (no retries left)
```

### Why External Tasks?

| Benefit | Description |
|---------|-------------|
| **Decoupling** | Workers are independent of engine |
| **Scalability** | Workers can scale horizontally |
| **Technology Freedom** | Workers can use any language |
| **Resilience** | Engine continues if workers fail |
| **Monitoring** | Built-in retry and incident handling |

### External Task Configuration

```xml
<bpmn:serviceTask
  id="validateData"
  camunda:type="external"
  camunda:topic="validateData"
  camunda:taskPriority="10">
</bpmn:serviceTask>
```

- `type="external"` - Marks as external task
- `topic` - Identifier workers subscribe to
- `taskPriority` - Higher priority tasks processed first

---

## Project Architecture

### Multi-Module Maven Structure

```txt
customer-onboarding-camunda7/
├── pom.xml                    # Parent POM
├── camunda-engine/            # Module 1: Camunda Engine
└── external-workers/          # Module 2: External Workers
```

### Module 1: Camunda Engine

Responsibilities:

- Hosts the Camunda Process Engine
- Deploys BPMN process definitions
- Exposes REST API for process management
- Provides web applications (Tasklist, Cockpit, Admin)
- Stores process state in H2 database

Key Dependencies:

```xml
<dependency>
  <groupId>org.camunda.bpm.springboot</groupId>
  <artifactId>camunda-bpm-spring-boot-starter-webapp</artifactId>
</dependency>
<dependency>
  <groupId>org.camunda.bpm.springboot</groupId>
  <artifactId>camunda-bpm-spring-boot-starter-rest</artifactId>
</dependency>
```

### Module 2: External Workers

Responsibilities:

- Polls Camunda Engine for external tasks
- Processes business logic
- Completes or fails tasks
- Handles retries on failure

Key Dependencies:

```xml
<dependency>
  <groupId>org.camunda.bpm</groupId>
  <artifactId>camunda-external-task-client</artifactId>
</dependency>
```

### Communication Flow

```txt
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP/REST                                │
│                                                                  │
│  ┌─────────────────┐                    ┌────────────────────┐  │
│  │  Camunda Engine │◄──────────────────►│  External Workers  │  │
│  │     :8080       │   Long Polling     │   (Separate JVM)   │  │
│  └────────┬────────┘                    └────────────────────┘  │
│           │                                                      │
│           │ JDBC                                                 │
│           ▼                                                      │
│  ┌─────────────────┐                                            │
│  │   H2 Database   │                                            │
│  │   (In-Memory)   │                                            │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## BPMN Process Design

### Customer Onboarding Process

```txt
┌─────────┐    ┌───────────────┐    ┌────────────────┐    ┌───────────────┐    ┌─────────┐
│  START  │───►│ Validate Data │───►│ Review Docs    │───►│Create Account │───►│   END   │
│         │    │ (External)    │    │ (User Task)    │    │ (External)    │    │ SUCCESS │
└─────────┘    └───────┬───────┘    └───────┬────────┘    └───────────────┘    └─────────┘
                       │                    │
               [VALIDATION_ERROR]    [documentsApproved=false]
                       │                    │
                       ▼                    ▼
               ┌───────────────┐    ┌─────────────┐
               │ Handle Error  │    │    END      │
               │ (External)    │    │  REJECTED   │
               └───────┬───────┘    └─────────────┘
                       │
                       ▼
               ┌─────────────┐
               │    END      │
               │   FAILED    │
               └─────────────┘
```

### Process Elements Explained

#### 1. Start Event

- Entry point for the process
- Triggered via REST API or programmatically
- Receives initial variables (customerName, email, documentType)

#### 2. Validate Data (External Task)

- Topic: `validateData`
- Validates input data format
- Can throw BPMN Error on validation failure
- Output: `isValid`, `validationMessage`

#### 3. Error Boundary Event

- Attached to Validate Data task
- Catches `VALIDATION_ERROR` BPMN error
- Routes to error handling path

#### 4. Review Documents (User Task)

- Assigned to `employees` candidate group
- Displays embedded HTML form
- User sets `documentsApproved` and `reviewerComments`

#### 5. Exclusive Gateway

- Evaluates `documentsApproved` variable
- Routes to Create Account (true) or End Rejected (false)

#### 6. Create Account (External Task)

- Topic: `createAccount`
- Creates customer account
- Output: `accountId`, `accountStatus`

#### 7. Handle Error (External Task)

- Topic: `handleError`
- Logs error details
- Sets error flags for tracking

---

## External Task Workers

### Worker Implementation Pattern

```java
@Component
public class ValidateDataWorker implements ExternalTaskHandler {

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            // 1. Get input variables
            String customerName = task.getVariable("customerName");

            // 2. Execute business logic
            ValidationResult result = validate(customerName);

            // 3. Handle result
            if (result.isValid()) {
                // Complete with output variables
                service.complete(task, Map.of("isValid", true));
            } else {
                // Throw BPMN error
                service.handleBpmnError(task, "VALIDATION_ERROR", result.message());
            }
        } catch (Exception e) {
            // Technical failure - retry
            service.handleFailure(task, e.getMessage(), null,
                task.getRetries() - 1, 5000L);
        }
    }
}
```

### Worker Subscription

```java
externalTaskClient.subscribe("validateData")
    .lockDuration(30000)        // Lock for 30 seconds
    .handler(validateDataWorker)
    .open();
```

### Task Completion Options

| Method | Use Case |
|--------|----------|
| `complete(task, variables)` | Successful completion |
| `handleBpmnError(task, errorCode, message)` | Business error (triggers boundary event) |
| `handleFailure(task, message, details, retries, timeout)` | Technical failure (retry) |

---

## Error Handling Strategies

### Business Errors vs Technical Errors

```txt
Business Errors                    Technical Errors
(Expected, Handled)                (Unexpected, Retry)
────────────────────              ────────────────────
- Invalid input data               - Database connection lost
- Missing required fields          - External API timeout
- Business rule violations         - Out of memory
                                   - Network issues
        │                                   │
        ▼                                   ▼
  BPMN Error Event                    Task Failure
  (handleBpmnError)                  (handleFailure)
        │                                   │
        ▼                                   ▼
  Error Boundary                      Retry Queue
  Event catches                      (Exponential backoff)
```

### BPMN Error Handling

```xml
<!-- Error Definition -->
<bpmn:error id="Error_Validation" errorCode="VALIDATION_ERROR"/>

<!-- Boundary Event -->
<bpmn:boundaryEvent attachedToRef="Task_ValidateData">
  <bpmn:errorEventDefinition errorRef="Error_Validation"/>
</bpmn:boundaryEvent>
```

### Retry Configuration

```java
// Exponential backoff: 5s, 10s, 20s
int currentRetries = task.getRetries();
int remainingRetries = currentRetries - 1;
long retryTimeout = 5000L * (long) Math.pow(2, 3 - remainingRetries);

service.handleFailure(task, "Error message", null, remainingRetries, retryTimeout);
```

---

## Process Variables

### Variable Scopes

```txt
Process Instance Scope
├── customerName (String)
├── email (String)
├── documentType (String)
├── isValid (Boolean)
├── validationMessage (String)
├── documentsApproved (Boolean)
├── reviewerComments (String)
├── accountId (String)
├── accountStatus (String)
├── errorOccurred (Boolean)
└── errorMessage (String)
```

### Setting Variables

In Worker:

```java
Map<String, Object> variables = new HashMap<>();
variables.put("isValid", true);
variables.put("validationMessage", "All validations passed");
externalTaskService.complete(externalTask, variables);
```

In BPMN (Input/Output Mapping):

```xml
<camunda:inputOutput>
  <camunda:inputParameter name="customerName">${customerName}</camunda:inputParameter>
  <camunda:outputParameter name="accountId">${accountId}</camunda:outputParameter>
</camunda:inputOutput>
```

---

## User Tasks and Forms

### User Task Configuration

```xml
<bpmn:userTask id="Task_ReviewDocuments"
  camunda:candidateGroups="employees"
  camunda:formKey="embedded:app:forms/review-documents.html">
</bpmn:userTask>
```

- `candidateGroups` - Groups that can claim the task
- `formKey` - Path to embedded form

### Embedded Form HTML

```html
<form>
  <!-- Read-only field bound to process variable -->
  <input cam-variable-name="customerName"
         cam-variable-type="String"
         readonly />

  <!-- Editable checkbox -->
  <input type="checkbox"
         cam-variable-name="documentsApproved"
         cam-variable-type="Boolean" />

  <!-- Text area -->
  <textarea cam-variable-name="reviewerComments"
            cam-variable-type="String"></textarea>
</form>
```

### Form Variable Binding

| Attribute | Purpose |
|-----------|---------|
| `cam-variable-name` | Maps to process variable |
| `cam-variable-type` | String, Boolean, Integer, etc. |
| `readonly` | Display only (not editable) |

---

## REST API Integration

### Camunda Engine REST API

Base URL: `http://localhost:8080/engine-rest`

Key Endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/process-definition/key/{key}/start` | POST | Start process |
| `/task` | GET | List tasks |
| `/task/{id}/complete` | POST | Complete task |
| `/external-task/fetchAndLock` | POST | Fetch external tasks |
| `/external-task/{id}/complete` | POST | Complete external task |

### Custom REST Controller

```java
@RestController
@RequestMapping("/api/process")
public class ProcessController {

    @PostMapping("/start")
    public ResponseEntity<ProcessInstanceResponse> startProcess(
            @RequestBody StartProcessRequest request) {

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "customer_onboarding",
            businessKey,
            variables
        );

        return ResponseEntity.ok(new ProcessInstanceResponse(instance));
    }
}
```

---

## Why Not JavaDelegates?

### JavaDelegate Pattern (Embedded)

```java
public class ValidateDataDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        String customerName = (String) execution.getVariable("customerName");
        // Process...
        execution.setVariable("isValid", true);
    }
}
```

### Comparison

| Aspect | JavaDelegate | External Task |
|--------|--------------|---------------|
| Coupling | Tightly coupled to engine | Loosely coupled |
| Deployment | Same JVM as engine | Separate deployment |
| Scaling | Scales with engine | Independent scaling |
| Technology | Java only | Any language |
| Failure Handling | Transaction rollback | Retry mechanism |
| Debugging | Complex (embedded) | Simple (isolated) |

### When to Use External Tasks

Use External Tasks when:

- Building microservices architecture
- Workers need to scale independently
- Using non-Java technologies
- Long-running operations
- Need isolation for resilience

Use JavaDelegates when:

- Simple, fast operations
- Tight transactional requirements
- Monolithic applications

---

## Production Considerations

### Database

Replace H2 with production database:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/camunda
    driver-class-name: org.postgresql.Driver
```

### Worker Scaling

Run multiple worker instances:

```bash
# Instance 1
WORKER_ID=worker-1 java -jar external-workers.jar

# Instance 2
WORKER_ID=worker-2 java -jar external-workers.jar
```

### Monitoring

Use Camunda Cockpit to monitor:

- Running process instances
- External task incidents
- Task completion rates
- Process history

### Security

Enable authentication:

```yaml
camunda:
  bpm:
    authorization:
      enabled: true
```

### High Availability

```txt
                    ┌─────────────────┐
                    │  Load Balancer  │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  Engine Node 1  │ │  Engine Node 2  │ │  Engine Node 3  │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
                    ┌────────▼────────┐
                    │  PostgreSQL     │
                    │  (Shared DB)    │
                    └─────────────────┘
```

---

## Summary

This project demonstrates:

1. **BPMN 2.0** - Standard process modeling notation
2. **External Task Pattern** - Scalable, decoupled architecture
3. **Multi-Module Maven** - Separation of concerns
4. **Error Handling** - Business and technical error strategies
5. **User Tasks** - Human workflow integration
6. **REST API** - Programmatic process control

The architecture supports:

- Independent scaling of workers
- Technology flexibility
- Resilience through retry mechanisms
- Clear separation between engine and business logic

For production deployment, consider:

- Replacing H2 with PostgreSQL
- Enabling security and authorization
- Setting up monitoring and alerting
- Implementing high availability
