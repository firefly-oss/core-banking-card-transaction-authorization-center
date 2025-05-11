# Core Banking Card Transaction Authorization Center

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-green.svg)](https://spring.io/projects/spring-boot)

This microservice is responsible for orchestrating and executing the decision of authorization online (approve/decline + reason) for each payment attempt, ATM withdrawal, or balance inquiry in a core banking system. It provides a robust, scalable solution for real-time card transaction authorization with comprehensive risk assessment and fraud detection capabilities.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
  - [Project Structure](#project-structure)
  - [Key Components](#key-components)
  - [Data Flow](#data-flow)
  - [Technology Stack](#technology-stack)
- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Monitoring and Observability](#monitoring-and-observability)
- [Contributing](#contributing)
- [License](#license)

## Overview

The Core Banking Card Transaction Authorization Center is a critical component of a modern banking infrastructure, designed to process and authorize card transactions in real-time. It integrates with various banking systems to validate cards, check balances, assess risk, and make authorization decisions based on configurable rules and policies.

The service follows a reactive programming model using Spring WebFlux, ensuring high throughput and low latency for transaction processing. It uses R2DBC for non-blocking database operations and implements a comprehensive event-driven architecture for transaction processing.

## Architecture

### Project Structure

The project follows a modular, hexagonal architecture with clear separation of concerns:

- **corebanking-card-transaction-authorization-center-interfaces**: Contains DTOs, interfaces, and enums that define the contract between modules
- **corebanking-card-transaction-authorization-center-models**: Contains domain entities, repositories, and database-related configurations
- **corebanking-card-transaction-authorization-center-core**: Contains business logic, service implementations, and domain-specific rules
- **corebanking-card-transaction-authorization-center-web**: Contains REST controllers, API configurations, and web-related components
- **corebanking-card-transaction-authorization-center-sdk**: Contains client libraries for integrating with the authorization service

### Key Components

#### Domain Model

##### Enums
- `AuthorizationDecisionType`: Defines possible authorization outcomes (APPROVED, DECLINED, CHALLENGE, PARTIAL)
- `AuthorizationReasonCode`: Standardized reason codes for approvals/declines following industry standards
- `TransactionChannel`: Channels through which transactions can occur (POS, E_COMMERCE, ATM, etc.)
- `TransactionType`: Types of transactions (PURCHASE, WITHDRAWAL, BALANCE_INQUIRY, etc.)
- `CardStatus`: Possible card statuses (ACTIVE, BLOCKED, FROZEN, etc.)

##### DTOs
- `AuthorizationRequestDTO`: Contains all transaction details required for authorization
- `AuthorizationDecisionDTO`: Contains the decision result and associated metadata
- `AuthorizationHoldDTO`: Represents funds held for a pending transaction
- `CardDetailsDTO`: Contains card information retrieved from the card service
- `LimitSnapshotDTO`: Represents daily/monthly spending limits and usage
- `BalanceSnapshotDTO`: Contains pre/post transaction balance information
- `RiskAssessmentDTO`: Contains risk scoring and fraud detection results

##### Entities
- `AuthorizationRequest`: Persistent entity for storing transaction requests
- `AuthorizationDecision`: Persistent entity for storing authorization decisions
- `AuthorizationHold`: Persistent entity for tracking funds on hold
- `SpendingWindow`: Entity for tracking spending limits over time periods

#### Core Services

##### Primary Services
- `AuthorizationService`: Orchestrates the entire authorization flow, coordinating all validation and decision steps
- `CardValidationService`: Validates card status, expiry, and other card-specific rules
- `LimitValidationService`: Validates transaction against daily, monthly, and per-transaction limits
- `RiskAssessmentService`: Performs fraud detection and risk scoring based on transaction patterns
- `BalanceService`: Checks available balance and manages fund reservations
- `HoldManagementService`: Creates, updates, releases, and captures authorization holds

##### Supporting Services
- `NotificationService`: Sends notifications for declined transactions or suspicious activity
- `AuditService`: Records detailed audit logs for compliance and troubleshooting
- `MetricsService`: Collects performance and business metrics

#### API Layer

##### Controllers
- `AuthorizationController`: Exposes REST endpoints for transaction authorization
- `HoldManagementController`: Exposes endpoints for managing authorization holds
- `AdminController`: Provides administrative functions for configuration and monitoring

#### Integration Layer

##### External Service Clients
- `CardServiceClient`: Retrieves card information from the card management system
- `CustomerServiceClient`: Retrieves customer information and preferences
- `LedgerServiceClient`: Interacts with the core banking ledger for balance operations
- `FraudDetectionClient`: Integrates with advanced fraud detection systems

### Data Flow

The authorization process follows a well-defined flow:

1. **Request Reception**: The system receives an authorization request via the API
2. **Request Validation**: Basic validation of the request format and required fields
3. **Card Validation**: Verification of card status, expiry, and other card-specific rules
4. **Limit Validation**: Checking against daily, monthly, and per-transaction limits
5. **Risk Assessment**: Fraud detection and risk scoring
6. **Balance Check**: Verification of sufficient funds
7. **Hold Creation**: Creation of an authorization hold if all validations pass
8. **Decision Formation**: Generation of the final authorization decision
9. **Response Delivery**: Return of the decision to the requesting system
10. **Post-Processing**: Asynchronous tasks like notifications and metrics collection

### Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.2, Spring WebFlux
- **Database**: PostgreSQL with R2DBC for reactive database access
- **API Documentation**: OpenAPI 3.0 with Swagger UI
- **Testing**: JUnit 5, Mockito, TestContainers
- **Build Tool**: Maven
- **Containerization**: Docker
- **CI/CD**: Jenkins/GitHub Actions
- **Monitoring**: Micrometer, Prometheus, Grafana
- **Logging**: SLF4J, Logback

## Features

- **Real-time Transaction Authorization**: Process card transactions in real-time with low latency
- **Comprehensive Validation**: Multi-layered validation including card status, limits, and balance
- **Risk-based Authorization**: Sophisticated risk assessment for fraud prevention
- **Flexible Decision Rules**: Configurable rules engine for authorization decisions
- **Hold Management**: Complete lifecycle management of authorization holds
- **Idempotent Processing**: Support for idempotent transaction processing
- **Detailed Audit Trail**: Comprehensive logging for compliance and troubleshooting
- **Performance Metrics**: Detailed metrics for monitoring and optimization
- **High Availability**: Designed for high availability and fault tolerance
- **Scalability**: Horizontal scalability to handle varying transaction volumes

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8 or higher
- PostgreSQL 14 or higher
- Docker (optional, for containerized deployment)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/firefly-oss/core-banking-card-transaction-authorization-center.git
   cd core-banking-card-transaction-authorization-center
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Set up the database:
   ```bash
   # Create a PostgreSQL database named 'card_authorization'
   createdb card_authorization

   # The application will automatically create the schema on startup
   ```

4. Run the application:
   ```bash
   mvn spring-boot:run -pl corebanking-card-transaction-authorization-center-web
   ```

### Configuration

The application can be configured through the `application.yml` file located in the `corebanking-card-transaction-authorization-center-web/src/main/resources` directory. Key configuration properties include:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/card_authorization
    username: postgres
    password: postgres

authorization:
  limits:
    default-daily: 5000.00
    default-monthly: 20000.00
    default-transaction: 2000.00
  risk:
    challenge-threshold: 70
    decline-threshold: 90
  hold:
    expiry-hours: 168  # 7 days
```

For production deployments, it's recommended to use environment variables or a configuration server to manage sensitive configuration values.

## API Documentation

The service provides a comprehensive RESTful API for transaction authorization and hold management. The API is documented using OpenAPI 3.0 and can be accessed through Swagger UI when the application is running:

```
http://localhost:8080/swagger-ui.html
```

Key API endpoints include:

- `POST /api/v1/authorizations`: Process a new authorization request
- `GET /api/v1/authorizations/{decisionId}`: Retrieve an authorization decision
- `POST /api/v1/authorizations/{requestId}/reverse`: Reverse an authorization
- `POST /api/v1/holds/{holdId}/capture`: Capture an authorization hold
- `POST /api/v1/holds/{holdId}/release`: Release an authorization hold

For detailed API documentation, please refer to the [API Documentation](docs/api-documentation.md) in the docs folder.

## Testing

The project includes comprehensive unit and integration tests. To run the tests:

```bash
mvn test
```

For integration tests that require a database, the project uses TestContainers to automatically set up and tear down the required infrastructure.

To run a specific test class:

```bash
mvn test -Dtest=AuthorizationServiceImplTest
```

## Monitoring and Observability

The service exposes metrics through Micrometer that can be scraped by Prometheus. Key metrics include:

- Transaction volume and success rates
- Response times for authorization requests
- Error rates and types
- Database operation latencies

For production deployments, it's recommended to set up Grafana dashboards to visualize these metrics and configure alerts for anomalies.

## Exception Handling

The application implements a global exception handler that provides consistent error responses for different types of exceptions:

- `IllegalArgumentException`: Returns a 400 Bad Request response
- `IllegalStateException`: Returns a 409 Conflict response
- `ResourceNotFoundException`: Returns a 404 Not Found response
- Validation exceptions: Returns a 400 Bad Request response with details about the validation errors

All exceptions are logged with appropriate context information for troubleshooting.

## Scheduled Tasks

The application includes several scheduled tasks:

- **Expired Hold Processing**: Runs every hour to process authorization holds that have expired but have not been captured or released
- **Spending Window Cleanup**: Runs daily to clean up expired spending window records
- **Metrics Aggregation**: Runs every 15 minutes to aggregate transaction metrics

## Contributing

Contributions to the Core Banking Card Transaction Authorization Center are welcome! Please follow these steps to contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the project's coding standards and includes appropriate tests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.