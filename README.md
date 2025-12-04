# Customer Onboarding - Camunda 7 Demo

A complete **Camunda 7** demonstration project using the **External Task Pattern** with **Spring Boot 3.x** and **Java 17**.

## Overview

This project showcases a production-ready architecture for workflow automation:

- **Multi-module Maven architecture** - Separate engine and worker modules
- **External Task Workers** - True microservice pattern for scalability
- **BPMN process** - User Tasks, Service Tasks, and Error Handling
- **Error Boundary Events** - Graceful error handling with retry mechanism
- **REST API** - Process management endpoints
- **Camunda Web Applications** - Tasklist, Cockpit, Admin

---

## Project Structure

```txt
customer-onboarding-camunda7/
├── pom.xml                              # Parent POM (multi-module)
├── README.md                            # This file
├── CONCEPTS.md                          # Technical concepts documentation
│
├── camunda-engine/                      # Camunda 7 Engine Module
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/camunda/
│       │   ├── CamundaEngineApplication.java
│       │   ├── controller/
│       │   │   └── ProcessController.java
│       │   └── dto/
│       │       ├── StartProcessRequest.java
│       │       └── ProcessInstanceResponse.java
│       └── resources/
│           ├── application.yml
│           ├── processes/
│           │   └── customer_onboarding.bpmn
│           └── forms/
│               └── review-documents.html
│
└── external-workers/                    # External Task Workers Module
    ├── pom.xml
    └── src/main/
        ├── java/com/example/workers/
        │   ├── ExternalWorkersApplication.java
        │   ├── config/
        │   │   └── ExternalTaskClientConfig.java
        │   └── handler/
        │       ├── ValidateDataWorker.java
        │       ├── CreateAccountWorker.java
        │       └── HandleErrorWorker.java
        └── resources/
            └── application.yml
```

---

## Prerequisites

- **Java 17** (JDK 17+)
- **Maven 3.9+**
- No Docker required
- No external database (uses H2 in-memory)

### Verify Installation

```bash
java -version   # Should show Java 17+
mvn -version    # Should show Maven 3.9+
```

---

## BPMN Process Flow

```txt
┌─────────┐    ┌──────────────────┐    ┌───────────────────┐    ┌─────────────────┐    ┌─────────┐
│  Start  │───▶│  validate-data   │───▶│  review-documents │───▶│  create-account │───▶│   End   │
│  Event  │    │ (External Task)  │    │   (User Task)     │    │ (External Task) │    │ Success │
└─────────┘    │ topic=validateData│   │ group=employees   │    │topic=createAccount│  └─────────┘
               └────────┬─────────┘    └─────────┬─────────┘    └─────────────────┘
                        │                        │
                        │ Error Boundary         │ If rejected
                        │ (VALIDATION_ERROR)     ▼
                        ▼                  ┌───────────┐
               ┌──────────────────┐        │   End     │
               │  Handle Error    │        │ Rejected  │
               │ (External Task)  │        └───────────┘
               └────────┬─────────┘
                        ▼
               ┌─────────────────┐
               │  Validation     │
               │  Failed End     │
               └─────────────────┘
```

---

## Quick Start

### 1. Build the Project

```bash
cd customer-onboarding-camunda7
mvn clean install
```

### 2. Start the Camunda Engine (Terminal 1)

```bash
mvn -pl camunda-engine spring-boot:run
```

Wait until you see:

```txt
CAMUNDA ENGINE STARTED SUCCESSFULLY
```

### 3. Start the External Workers (Terminal 2)

```bash
mvn -pl external-workers spring-boot:run
```

Wait until you see:

```txt
EXTERNAL WORKERS STARTED SUCCESSFULLY
```

### 4. Access Camunda Web Applications

| Application | URL | Credentials |
|-------------|-----|-------------|
| Tasklist | <http://localhost:8080/camunda/app/tasklist> | demo / demo |
| Cockpit | <http://localhost:8080/camunda/app/cockpit> | demo / demo |
| Admin | <http://localhost:8080/camunda/app/admin> | demo / demo |
| H2 Console | <http://localhost:8080/h2-console> | sa / (empty) |

---

## API Reference

### Start a New Process Instance

```bash
curl -X POST http://localhost:8080/api/process/start \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Amit Sharma",
    "email": "amit@example.com",
    "documentType": "PASSPORT"
  }'
```

Response:

```json
{
  "processInstanceId": "12345",
  "processDefinitionId": "customer_onboarding:1:67890",
  "businessKey": "CUST-A1B2C3D4",
  "isEnded": false,
  "isSuspended": false,
  "message": "Process instance started successfully"
}
```

### Get Process Status

```bash
curl http://localhost:8080/api/process/{processInstanceId}/status
```

### List All Running Process Instances

```bash
curl http://localhost:8080/api/process/instances
```

### List Pending User Tasks

```bash
# All tasks
curl http://localhost:8080/api/process/tasks

# Tasks for employees group
curl "http://localhost:8080/api/process/tasks?candidateGroup=employees"
```

### Complete a User Task

```bash
curl -X POST http://localhost:8080/api/process/tasks/{taskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "documentsApproved": true,
    "reviewerComments": "All documents verified successfully"
  }'
```

### Cancel a Process Instance

```bash
curl -X DELETE http://localhost:8080/api/process/{processInstanceId}
```

---

## Complete Workflow Walkthrough

### Step 1: Start the Process

```bash
curl -X POST http://localhost:8080/api/process/start \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Amit Sharma",
    "email": "amit@example.com",
    "documentType": "PASSPORT"
  }'
```

### Step 2: Validation Worker Processes Task

The `ValidateDataWorker` automatically:

- Validates customer name (required, min 2 chars)
- Validates email format
- Validates document type (ID, PASSPORT, DRIVING_LICENSE)
- Sets `isValid` and `validationMessage` variables

### Step 3: Complete the User Task

#### Option A: Via Camunda Tasklist UI

1. Go to <http://localhost:8080/camunda/app/tasklist>
2. Login with `demo` / `demo`
3. Click on "All tasks" filter
4. Click on "Review Customer Documents" task
5. Check "Approve Documents" and add comments
6. Click "Complete"

#### Option B: Via REST API

```bash
# Get task ID
curl http://localhost:8080/api/process/tasks

# Complete the task
curl -X POST http://localhost:8080/api/process/tasks/{taskId}/complete \
  -H "Content-Type: application/json" \
  -d '{"documentsApproved": true, "reviewerComments": "Verified"}'
```

### Step 4: Account Creation Worker Processes Task

The `CreateAccountWorker` automatically:

- Creates a unique account ID (ACC-XXXXXXXX)
- Sets account status to ACTIVE
- Completes the process

---

## Testing Error Handling

### Trigger Validation Error

```bash
curl -X POST http://localhost:8080/api/process/start \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "",
    "email": "invalid-email",
    "documentType": "UNKNOWN"
  }'
```

What happens:

1. `ValidateDataWorker` detects validation errors
2. Throws BPMN Error `VALIDATION_ERROR`
3. Error Boundary Event catches it
4. `HandleErrorWorker` processes the error
5. Process ends with "Validation Failed" status

---

## Process Variables

| Variable | Type | Set By | Description |
|----------|------|--------|-------------|
| customerName | String | Start | Customer's full name |
| email | String | Start | Customer's email |
| documentType | String | Start | ID, PASSPORT, or DRIVING_LICENSE |
| isValid | Boolean | ValidateDataWorker | Validation result |
| validationMessage | String | ValidateDataWorker | Validation details |
| documentsApproved | Boolean | User Task | Reviewer decision |
| reviewerComments | String | User Task | Reviewer notes |
| accountId | String | CreateAccountWorker | Generated account ID |
| accountStatus | String | CreateAccountWorker | Account status (ACTIVE) |
| errorOccurred | Boolean | HandleErrorWorker | Error flag |
| errorMessage | String | HandleErrorWorker | Error details |

---

## Retry Mechanism

External tasks support automatic retries on failure:

| Attempt | Retry Delay |
|---------|-------------|
| 1st failure | 5 seconds |
| 2nd failure | 10 seconds |
| 3rd failure | 20 seconds |
| 4th failure | Task marked as permanently failed |

---

## Configuration Files

### Camunda Engine

Location: `camunda-engine/src/main/resources/application.yml`

Key settings:

- Server port: 8080
- Admin user: demo/demo
- H2 in-memory database
- Auto-deploy BPMN from `processes/` folder

### External Workers

Location: `external-workers/src/main/resources/application.yml`

Key settings:

- Engine URL: <http://localhost:8080/engine-rest>
- Max tasks per fetch: 10
- Lock duration: 30 seconds
- Exponential backoff on errors

---

## Troubleshooting

### Workers not picking up tasks

1. Ensure Camunda Engine is running first
2. Check worker logs for connection errors
3. Verify topics match between BPMN and workers

### Process stuck on external task

1. Check if workers are running
2. Look for errors in worker logs
3. Tasks auto-unlock after 30 seconds

### User task not appearing

1. Ensure you're logged in as `demo`
2. Create "All tasks" filter if not present
3. Verify process reached the user task step

### H2 Console Access

- JDBC URL: `jdbc:h2:mem:camunda`
- Username: `sa`
- Password: (leave empty)

---

## Technology Stack

| Component | Version |
|-----------|---------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Camunda BPM | 7.21.0 |
| H2 Database | Runtime |
| Maven | 3.9+ |

---

## Further Reading

- See [CONCEPTS.md](CONCEPTS.md) for detailed technical concepts
- [Camunda 7 Documentation](https://docs.camunda.org/manual/7.21/)
- [External Task Pattern](https://docs.camunda.org/manual/7.21/user-guide/process-engine/external-tasks/)
