package com.example.camunda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application for Camunda 7 Process Engine.
 *
 * This application:
 * - Runs the Camunda 7 Process Engine
 * - Exposes Camunda Web Applications (Tasklist, Cockpit, Admin)
 * - Provides REST API endpoints for process management
 * - Auto-deploys BPMN processes from resources/processes folder
 *
 * Access Points:
 * - Camunda Tasklist: http://localhost:8080/camunda/app/tasklist
 * - Camunda Cockpit:  http://localhost:8080/camunda/app/cockpit
 * - Camunda Admin:    http://localhost:8080/camunda/app/admin
 * - REST API:         http://localhost:8080/engine-rest
 * - H2 Console:       http://localhost:8080/h2-console
 *
 * Default Credentials: demo / demo
 */
@SpringBootApplication
public class CamundaEngineApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(CamundaEngineApplication.class);
    
    public static void main(String[] args) {
        SpringApplication.run(CamundaEngineApplication.class, args);
        
        logger.info("==========================================================");
        logger.info("  CAMUNDA ENGINE STARTED SUCCESSFULLY");
        logger.info("==========================================================");
        logger.info("  Camunda Tasklist: http://localhost:8080/camunda/app/tasklist");
        logger.info("  Camunda Cockpit:  http://localhost:8080/camunda/app/cockpit");
        logger.info("  Camunda Admin:    http://localhost:8080/camunda/app/admin");
        logger.info("  REST API:         http://localhost:8080/engine-rest");
        logger.info("  H2 Console:       http://localhost:8080/h2-console");
        logger.info("  Custom API:       http://localhost:8080/api/process");
        logger.info("----------------------------------------------------------");
        logger.info("  Login Credentials: demo / demo");
        logger.info("==========================================================");
    }
}
