# Configuration Guide

This document provides detailed information about configuring the Core Banking Card Transaction Authorization Center.

## Table of Contents

- [Overview](#overview)
- [Configuration Files](#configuration-files)
- [Database Configuration](#database-configuration)
- [Authorization Settings](#authorization-settings)
  - [Limit Configuration](#limit-configuration)
  - [Risk Assessment Configuration](#risk-assessment-configuration)
  - [Hold Management Configuration](#hold-management-configuration)
- [Integration Configuration](#integration-configuration)
  - [Card Service](#card-service)
  - [Ledger Service](#ledger-service)
  - [Notification Service](#notification-service)
- [Security Configuration](#security-configuration)
- [Logging Configuration](#logging-configuration)
- [Monitoring Configuration](#monitoring-configuration)
- [Performance Tuning](#performance-tuning)
- [Environment-Specific Configuration](#environment-specific-configuration)

## Overview

The Core Banking Card Transaction Authorization Center is designed to be highly configurable to meet the specific needs of different financial institutions. Configuration is primarily managed through YAML files, environment variables, and a configuration database.

## Configuration Files

The main configuration files are:

1. **application.yml**: Core application configuration
2. **application-{profile}.yml**: Environment-specific configuration (dev, test, prod)
3. **bootstrap.yml**: Configuration for the configuration server (if used)
4. **logback-spring.xml**: Logging configuration

These files are located in the `corebanking-card-transaction-authorization-center-web/src/main/resources` directory.

## Database Configuration

Database connection is configured in the `application.yml` file:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/card_authorization
    username: postgres
    password: postgres
    pool:
      initial-size: 10
      max-size: 30
      max-idle-time: 30m
      validation-query: SELECT 1
```

For production environments, it's recommended to use environment variables for sensitive information:

```yaml
spring:
  r2dbc:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

## Authorization Settings

### Limit Configuration

Spending limits are configured in the `application.yml` file:

```yaml
authorization:
  limits:
    default-transaction: 2000.00
    default-daily: 5000.00
    default-monthly: 20000.00
    channel-multipliers:
      atm: 0.5
      e-commerce: 0.75
      pos: 1.0
    product-limits:
      standard:
        transaction: 1000.00
        daily: 3000.00
        monthly: 10000.00
      gold:
        transaction: 3000.00
        daily: 7000.00
        monthly: 30000.00
      platinum:
        transaction: 5000.00
        daily: 10000.00
        monthly: 50000.00
```

These settings define the default limits and product-specific limits. Channel multipliers are applied to the base limits to determine channel-specific limits.

### Risk Assessment Configuration

Risk assessment is configured in the `application.yml` file:

```yaml
authorization:
  risk:
    challenge-threshold: 70
    decline-threshold: 90
    high-risk-countries:
      - "XY"
      - "ZZ"
    high-risk-mccs:
      - "7995"  # Gambling
      - "5993"  # Cigar stores
    rules:
      high-value-transaction:
        enabled: true
        threshold: 1000.00
        score: 10
      unusual-hour:
        enabled: true
        start-hour: 0
        end-hour: 5
        score: 5
      cross-border-transaction:
        enabled: true
        score: 15
      high-risk-merchant-category:
        enabled: true
        score: 20
      round-amount:
        enabled: true
        score: 5
      velocity-check:
        enabled: true
        max-transactions: 5
        time-window-minutes: 60
        score: 25
```

These settings define the risk assessment thresholds, high-risk countries and merchant categories, and the configuration for individual risk rules.

### Hold Management Configuration

Hold management is configured in the `application.yml` file:

```yaml
authorization:
  hold:
    expiry-hours: 168  # 7 days
    cleanup-days: 90
    scheduled-processing:
      expired-holds-cron: "0 0 * * * *"  # Every hour
      cleanup-cron: "0 0 0 * * *"  # Every day at midnight
```

These settings define the hold expiration period, cleanup period, and the schedule for processing expired holds and cleaning up old hold records.

## Integration Configuration

### Card Service

Integration with the card service is configured in the `application.yml` file:

```yaml
integration:
  card-service:
    base-url: http://card-service:8080
    api-key: ${CARD_SERVICE_API_KEY}
    timeout-seconds: 5
    retry:
      max-attempts: 3
      backoff-ms: 500
```

### Ledger Service

Integration with the ledger service is configured in the `application.yml` file:

```yaml
integration:
  ledger-service:
    base-url: http://ledger-service:8080
    api-key: ${LEDGER_SERVICE_API_KEY}
    timeout-seconds: 5
    retry:
      max-attempts: 3
      backoff-ms: 500
```

### Notification Service

Integration with the notification service is configured in the `application.yml` file:

```yaml
integration:
  notification-service:
    base-url: http://notification-service:8080
    api-key: ${NOTIFICATION_SERVICE_API_KEY}
    timeout-seconds: 5
    events:
      declined-transaction: true
      high-risk-transaction: true
      limit-breach: true
```

## Security Configuration

Security settings are configured in the `application.yml` file:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com
          jwk-set-uri: https://auth.example.com/.well-known/jwks.json

security:
  cors:
    allowed-origins: "*"
    allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
    allowed-headers: "*"
    max-age: 3600
  rate-limiting:
    enabled: true
    limit-per-client: 100
    time-window-seconds: 60
```

These settings define the OAuth 2.0 configuration for securing the API, CORS settings, and rate limiting configuration.

## Logging Configuration

Logging is configured in the `logback-spring.xml` file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>clientId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE"/>
    </root>

    <logger name="com.catalis.core.banking.cards.authorization" level="DEBUG"/>
    <logger name="org.springframework" level="INFO"/>
    <logger name="io.r2dbc" level="INFO"/>
</configuration>
```

For production environments, it's recommended to use a more sophisticated logging configuration that includes log aggregation and monitoring.

## Monitoring Configuration

Monitoring is configured in the `application.yml` file:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
```

These settings enable Spring Boot Actuator endpoints for health checks, metrics, and Prometheus integration.

## Performance Tuning

Performance settings are configured in the `application.yml` file:

```yaml
spring:
  webflux:
    base-path: /
  codec:
    max-in-memory-size: 2MB
  r2dbc:
    pool:
      initial-size: 10
      max-size: 30
      max-idle-time: 30m
      max-life-time: 2h
      validation-query: SELECT 1

server:
  port: 8080
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
    min-response-size: 1024
```

These settings define the web server configuration, codec settings, and connection pool settings for optimal performance.

## Environment-Specific Configuration

Environment-specific configuration is managed through profile-specific YAML files:

### application-dev.yml

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/card_authorization_dev
    username: postgres
    password: postgres

logging:
  level:
    com.catalis.core.banking.cards.authorization: DEBUG
    org.springframework: INFO
    io.r2dbc: DEBUG

integration:
  card-service:
    base-url: http://localhost:8081
  ledger-service:
    base-url: http://localhost:8082
  notification-service:
    base-url: http://localhost:8083
```

### application-test.yml

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/card_authorization_test
    username: postgres
    password: postgres

logging:
  level:
    com.catalis.core.banking.cards.authorization: DEBUG
    org.springframework: INFO
    io.r2dbc: INFO

integration:
  card-service:
    base-url: http://card-service-test:8080
  ledger-service:
    base-url: http://ledger-service-test:8080
  notification-service:
    base-url: http://notification-service-test:8080
```

### application-prod.yml

```yaml
spring:
  r2dbc:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    pool:
      initial-size: 20
      max-size: 50
      max-idle-time: 30m
      max-life-time: 2h
      validation-query: SELECT 1

logging:
  level:
    com.catalis.core.banking.cards.authorization: INFO
    org.springframework: WARN
    io.r2dbc: WARN

integration:
  card-service:
    base-url: ${CARD_SERVICE_URL}
    api-key: ${CARD_SERVICE_API_KEY}
  ledger-service:
    base-url: ${LEDGER_SERVICE_URL}
    api-key: ${LEDGER_SERVICE_API_KEY}
  notification-service:
    base-url: ${NOTIFICATION_SERVICE_URL}
    api-key: ${NOTIFICATION_SERVICE_API_KEY}

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

To activate a specific profile, set the `SPRING_PROFILES_ACTIVE` environment variable:

```bash
export SPRING_PROFILES_ACTIVE=prod
```

Or use the `--spring.profiles.active` command-line argument:

```bash
java -jar corebanking-card-transaction-authorization-center.jar --spring.profiles.active=prod
```
