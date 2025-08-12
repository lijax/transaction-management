package com.banking.transactionmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger/OpenAPI configuration for the Transaction Management API.
 * Provides comprehensive API documentation with proper metadata.
 * Dynamically configures server URLs based on environment.
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${swagger.server.production.url:}")
    private String productionServerUrl;

    @Value("${swagger.server.production.description:Production Server}")
    private String productionServerDescription;

    @Bean
    public OpenAPI transactionManagementOpenAPI() {
        List<Server> servers = new ArrayList<>();
        
        // 添加本地开发服务器（动态端口）
        servers.add(new Server()
                .url("http://localhost:" + serverPort)
                .description("Development Server"));
        
        // 如果配置了生产服务器URL，则添加生产服务器
        if (StringUtils.hasText(productionServerUrl)) {
            servers.add(new Server()
                    .url(productionServerUrl)
                    .description(productionServerDescription));
        }
        
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Management API")
                        .description("Banking Transaction Management System - RESTful API for managing financial transactions")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Banking Development Team")
                                .email("dev@banking.com")
                                .url("https://banking.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(servers);
    }
}