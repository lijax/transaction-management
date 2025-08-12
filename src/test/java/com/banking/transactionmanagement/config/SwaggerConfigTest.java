package com.banking.transactionmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SwaggerConfig to verify dynamic server URL configuration.
 */
class SwaggerConfigTest {

    @Test
    void testDefaultConfiguration() {
        // Given
        SwaggerConfig config = new SwaggerConfig();
        ReflectionTestUtils.setField(config, "serverPort", 8080);
        ReflectionTestUtils.setField(config, "productionServerUrl", "");
        ReflectionTestUtils.setField(config, "productionServerDescription", "Production Server");

        // When
        OpenAPI openAPI = config.transactionManagementOpenAPI();

        // Then
        List<Server> servers = openAPI.getServers();
        assertThat(servers).hasSize(1);
        assertThat(servers.get(0).getUrl()).isEqualTo("http://localhost:8080");
        assertThat(servers.get(0).getDescription()).isEqualTo("Development Server");
    }

    @Test
    void testCustomPortConfiguration() {
        // Given
        SwaggerConfig config = new SwaggerConfig();
        ReflectionTestUtils.setField(config, "serverPort", 9090);
        ReflectionTestUtils.setField(config, "productionServerUrl", "");
        ReflectionTestUtils.setField(config, "productionServerDescription", "Production Server");

        // When
        OpenAPI openAPI = config.transactionManagementOpenAPI();

        // Then
        List<Server> servers = openAPI.getServers();
        assertThat(servers).hasSize(1);
        assertThat(servers.get(0).getUrl()).isEqualTo("http://localhost:9090");
        assertThat(servers.get(0).getDescription()).isEqualTo("Development Server");
    }

    @Test
    void testWithProductionServerConfiguration() {
        // Given
        SwaggerConfig config = new SwaggerConfig();
        ReflectionTestUtils.setField(config, "serverPort", 8080);
        ReflectionTestUtils.setField(config, "productionServerUrl", "https://api.banking.com");
        ReflectionTestUtils.setField(config, "productionServerDescription", "Production API Server");

        // When
        OpenAPI openAPI = config.transactionManagementOpenAPI();

        // Then
        List<Server> servers = openAPI.getServers();
        assertThat(servers).hasSize(2);
        
        // Development server
        assertThat(servers.get(0).getUrl()).isEqualTo("http://localhost:8080");
        assertThat(servers.get(0).getDescription()).isEqualTo("Development Server");
        
        // Production server
        assertThat(servers.get(1).getUrl()).isEqualTo("https://api.banking.com");
        assertThat(servers.get(1).getDescription()).isEqualTo("Production API Server");
    }

    @Test
    void testWithEmptyProductionServerUrl() {
        // Given
        SwaggerConfig config = new SwaggerConfig();
        ReflectionTestUtils.setField(config, "serverPort", 8080);
        ReflectionTestUtils.setField(config, "productionServerUrl", "   "); // whitespace only
        ReflectionTestUtils.setField(config, "productionServerDescription", "Production Server");

        // When
        OpenAPI openAPI = config.transactionManagementOpenAPI();

        // Then
        List<Server> servers = openAPI.getServers();
        assertThat(servers).hasSize(1); // Only development server should be present
        assertThat(servers.get(0).getUrl()).isEqualTo("http://localhost:8080");
        assertThat(servers.get(0).getDescription()).isEqualTo("Development Server");
    }

    @Test
    void testOpenAPIMetadata() {
        // Given
        SwaggerConfig config = new SwaggerConfig();
        ReflectionTestUtils.setField(config, "serverPort", 8080);
        ReflectionTestUtils.setField(config, "productionServerUrl", "");
        ReflectionTestUtils.setField(config, "productionServerDescription", "Production Server");

        // When
        OpenAPI openAPI = config.transactionManagementOpenAPI();

        // Then
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Transaction Management API");
        assertThat(openAPI.getInfo().getDescription()).contains("Banking Transaction Management System");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0");
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("Banking Development Team");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("dev@banking.com");
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("MIT License");
    }

    @Test
    void testMultipleEnvironmentConfiguration() {
        // Given
        SwaggerConfig config = new SwaggerConfig();
        ReflectionTestUtils.setField(config, "serverPort", 8081);
        ReflectionTestUtils.setField(config, "productionServerUrl", "https://prod-api.banking.com");
        ReflectionTestUtils.setField(config, "productionServerDescription", "Production Environment");

        // When
        OpenAPI openAPI = config.transactionManagementOpenAPI();

        // Then
        List<Server> servers = openAPI.getServers();
        assertThat(servers).hasSize(2);
        
        // Verify development server with custom port
        assertThat(servers.get(0).getUrl()).isEqualTo("http://localhost:8081");
        assertThat(servers.get(0).getDescription()).isEqualTo("Development Server");
        
        // Verify production server with custom description
        assertThat(servers.get(1).getUrl()).isEqualTo("https://prod-api.banking.com");
        assertThat(servers.get(1).getDescription()).isEqualTo("Production Environment");
    }
}