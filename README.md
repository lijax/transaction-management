# Transaction Management System

A high-performance Spring Boot application for managing financial transactions in a banking context. Built with Java 17, featuring comprehensive validation, caching, and robust error handling.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Local Development](#local-development)
- [Docker Deployment](#docker-deployment)
- [API Documentation](#api-documentation)
- [Testing](#testing)

## Features

- **RESTful API** for complete transaction CRUD operations
- **High Performance** with Caffeine caching and optimized queries
- **Comprehensive Validation** with custom business rule validators
- **Robust Error Handling** with detailed error responses
- **Performance Monitoring** with Spring Boot Actuator
- **Docker Support** with optimized containerization
- **Extensive Testing** including unit, integration, and stress tests
- **H2 In-Memory Database** for fast development and testing

## Requirements

### System Requirements
- **Java 17** or higher
- **Maven 3.6+** (or use included Maven wrapper)
- **Docker** (optional, for containerized deployment)
- **curl** (for API testing)

### Hardware Recommendations
- **Memory**: Minimum 1GB RAM, Recommended 2GB+
- **CPU**: 2+ cores recommended for optimal performance
- **Storage**: 100MB+ free space

## Quick Start

### 1. Clone and Build
```bash
git clone <repository-url>
cd transaction-management
./mvnw clean compile
```

### 2. Run Tests
```bash
./mvnw test
```

### 3. Start Application
```bash
./mvnw spring-boot:run
```

### 4. Verify Installation
```bash
curl http://localhost:8080/actuator/health
```

The application will be available at `http://localhost:8080`

## Local Development

### Using Maven Wrapper (Recommended)

```bash
# Clean and compile
./mvnw clean compile

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=TransactionServiceTest

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Package application
./mvnw clean package

# Skip tests during build
./mvnw clean package -DskipTests
```

### Using System Maven

```bash
# All commands work the same, just replace ./mvnw with mvn
mvn clean compile
mvn test
mvn spring-boot:run
```

### Development Tools

#### H2 Database Console
Access the H2 console at `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:transactiondb`
- **Username**: `sa`
- **Password**: `password`

#### Spring Boot Actuator Endpoints
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Info: `http://localhost:8080/actuator/info`
- Cache: `http://localhost:8080/actuator/caches`

## Docker Deployment

### Docker Commands

```bash
# Build image
docker build -t transaction-management .

# Run container
docker run -d \
  --name transaction-management-app \
  -p 8080:8080 \
  transaction-management

# Check logs
docker logs -f transaction-management-app

# Health check
curl http://localhost:8080/actuator/health
```

### Docker Environment Variables

Key environment variables for Docker deployment:

```bash
# Server Configuration
SERVER_PORT=8080

# Database Configuration
DATABASE_URL=jdbc:h2:mem:transactiondb
DATABASE_USERNAME=sa
DATABASE_PASSWORD=password

# Performance Tuning
HIKARI_MAX_POOL_SIZE=15
CACHE_SPEC=maximumSize=800,expireAfterWrite=10m
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

# Logging
LOG_LEVEL_APP=INFO
LOG_LEVEL_SQL=WARN
```

## API Documentation

### Swagger UI

- URI: `http://localhost:8080/swagger-ui/index.html`

### Base URL
- Local: `http://localhost:8080/api/v1`
- Docker: `http://localhost:8080/api/v1`

### Endpoints

#### 1. List Transactions (GET /transactions)
```bash
# Get all transactions (paginated)
curl "http://localhost:8080/api/v1/transactions"

# With pagination parameters
curl "http://localhost:8080/api/v1/transactions?page=0&size=10&sort=timestamp,desc"
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "amount": 150.75,
      "description": "Grocery shopping",
      "type": "DEBIT",
      "timestamp": "2024-01-15T10:30:00",
      "category": "Food",
      "accountNumber": "12345678",
      "referenceNumber": "TXN123456",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "empty": false
}
```

#### 2. Create Transaction (POST /transactions)
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 150.75,
    "description": "Grocery shopping",
    "type": "DEBIT",
    "category": "Food",
    "accountNumber": "12345678"
  }'
```

**Response:**
```json
{
  "id": 1,
  "amount": 150.75,
  "description": "Grocery shopping",
  "type": "DEBIT",
  "category": "Food",
  "accountNumber": "12345678",
  "referenceNumber": "TXN123456",
  "timestamp": "2024-01-15T10:30:00"
}
```

#### 3. Get Transaction (GET /transactions/{id})
```bash
curl http://localhost:8080/api/v1/transactions/1
```

#### 4. Update Transaction (PUT /transactions/{id})
```bash
curl -X PUT http://localhost:8080/api/v1/transactions/1 \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 175.50,
    "description": "Updated grocery shopping"
  }'
```

#### 5. Delete Transaction (DELETE /transactions/{id})
```bash
curl -X DELETE http://localhost:8080/api/v1/transactions/1
```

### Transaction Types
- `DEBIT` - Money going out
- `CREDIT` - Money coming in
- `TRANSFER` - Money transfer between accounts
- `WITHDRAWAL` - Cash withdrawal
- `DEPOSIT` - Cash deposit

## Testing

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test categories
./mvnw test -Dtest="*Test"                    # Unit tests
./mvnw test -Dtest="*IntegrationTest"         # Integration tests
./mvnw test -Dtest="*PerformanceTest*"        # Performance tests

# Run with coverage
./mvnw test jacoco:report

# Run tests with specific profile
./mvnw test -Dspring.profiles.active=test
```

### Test Categories

#### Unit Tests
- **Controller Tests**: REST endpoint validation
- **Service Tests**: Business logic verification
- **Repository Tests**: Data access testing
- **Validation Tests**: Input validation rules

#### Integration Tests
- **API Integration**: End-to-end API testing
- **Database Integration**: Database interaction testing
- **Cache Integration**: Caching behavior verification

#### Performance Tests
- **Load Tests**: High-volume transaction processing
- **Stress Tests**: System limits and degradation
- **Memory Tests**: Memory usage under load
- **Concurrent Tests**: Multi-threaded operations
