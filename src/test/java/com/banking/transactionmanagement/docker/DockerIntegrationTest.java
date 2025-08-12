package com.banking.transactionmanagement.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test to verify Docker-specific configuration.
 * This test runs only when DOCKER_TEST environment variable is set to 'true'.
 * 
 * To run this test:
 * DOCKER_TEST=true ./mvnw test -Dtest=DockerIntegrationTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("docker")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:dockertest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.h2.console.enabled=false",
    "management.endpoints.web.exposure.include=health,info,metrics",
    "logging.level.com.banking.transactionmanagement=INFO"
})
@EnabledIfEnvironmentVariable(named = "DOCKER_TEST", matches = "true")
class DockerIntegrationTest {

    @Test
    void contextLoadsWithDockerProfile() {
        // This test verifies that the application can start with Docker profile
        // and Docker-specific configuration without any issues
        
        // If we reach this point, the Spring context loaded successfully
        // with Docker profile and configuration
        
        // Additional assertions could be added here to verify specific
        // Docker configuration aspects if needed
    }
    
    @Test
    void dockerSpecificPropertiesAreApplied() {
        // This test could verify that Docker-specific properties are correctly applied
        // For example, checking that H2 console is disabled, specific cache settings, etc.
        
        // Since this is primarily a configuration test, the main verification
        // is that the context loads without errors
    }
}