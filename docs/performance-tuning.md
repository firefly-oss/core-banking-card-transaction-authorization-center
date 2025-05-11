# Performance Tuning Guide

This document provides detailed guidance on tuning the Firefly Card Transaction Authorization Center for optimal performance.

## Table of Contents

- [Overview](#overview)
- [Hardware Recommendations](#hardware-recommendations)
- [JVM Tuning](#jvm-tuning)
- [Database Tuning](#database-tuning)
- [Application Configuration](#application-configuration)
- [Network Optimization](#network-optimization)
- [Monitoring and Profiling](#monitoring-and-profiling)
- [Scaling Strategies](#scaling-strategies)
- [Performance Testing](#performance-testing)

## Overview

The Firefly Card Transaction Authorization Center is designed for high performance, but optimal performance requires proper tuning based on your specific deployment environment and transaction volume. This guide provides recommendations for various aspects of performance tuning.

## Hardware Recommendations

### Application Servers

For production deployments, we recommend the following minimum specifications per application instance:

| Transaction Volume (TPS) | CPU Cores | Memory | Disk |
|--------------------------|-----------|--------|------|
| Up to 500 TPS | 4 cores | 8 GB | 50 GB SSD |
| 500-1000 TPS | 8 cores | 16 GB | 100 GB SSD |
| 1000-2000 TPS | 16 cores | 32 GB | 200 GB SSD |
| 2000+ TPS | 32 cores | 64 GB | 500 GB SSD |

### Database Servers

For the PostgreSQL database, we recommend:

| Transaction Volume (TPS) | CPU Cores | Memory | Disk |
|--------------------------|-----------|--------|------|
| Up to 500 TPS | 4 cores | 16 GB | 100 GB SSD |
| 500-1000 TPS | 8 cores | 32 GB | 200 GB SSD |
| 1000-2000 TPS | 16 cores | 64 GB | 500 GB SSD |
| 2000+ TPS | 32 cores | 128 GB | 1 TB SSD |

## JVM Tuning

Optimal JVM settings are crucial for performance. Here are our recommended settings:

```
-Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/firefly/heapdump.hprof
```

For higher transaction volumes, increase the heap size accordingly:

- 500-1000 TPS: `-Xms8g -Xmx8g`
- 1000-2000 TPS: `-Xms16g -Xmx16g`
- 2000+ TPS: `-Xms32g -Xmx32g`

## Database Tuning

### Connection Pool Settings

Optimize the R2DBC connection pool in `application.yml`:

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 10
      max-size: 50
      max-idle-time: 30m
      max-life-time: 2h
      validation-query: SELECT 1
```

For higher transaction volumes:

- 500-1000 TPS: `max-size: 100`
- 1000-2000 TPS: `max-size: 200`
- 2000+ TPS: `max-size: 300`

### PostgreSQL Configuration

Recommended PostgreSQL settings for a production environment:

```
max_connections = 500
shared_buffers = 8GB
effective_cache_size = 24GB
maintenance_work_mem = 2GB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 41943kB
min_wal_size = 1GB
max_wal_size = 4GB
max_worker_processes = 8
max_parallel_workers_per_gather = 4
max_parallel_workers = 8
```

Adjust these values based on your server specifications.

## Application Configuration

### Reactive Settings

Optimize the reactive settings in `application.yml`:

```yaml
spring:
  webflux:
    base-path: /
  codec:
    max-in-memory-size: 2MB
  reactor:
    netty:
      worker:
        count: 16  # Set to number of CPU cores
      connection:
        provider:
          pool:
            max-connections: 1000
            acquire-timeout: 5000
```

### Caching Configuration

Enable and configure caching for frequently accessed data:

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=60s

firefly:
  caching:
    card-details:
      max-size: 10000
      expire-after-write: 60s
    spending-windows:
      max-size: 5000
      expire-after-write: 30s
```

## Network Optimization

### Load Balancer Settings

For optimal performance with a load balancer:

- Enable HTTP/2
- Configure keep-alive connections
- Set appropriate timeouts (we recommend 30s for connection timeout)
- Enable SSL termination at the load balancer

### Service Communication

Optimize service-to-service communication:

```yaml
firefly:
  integration:
    connection-timeout: 5000
    read-timeout: 10000
    write-timeout: 10000
    max-connections: 200
    max-connections-per-route: 50
```

## Monitoring and Profiling

### Key Metrics to Monitor

Monitor these key metrics for performance optimization:

- Transaction throughput (transactions per second)
- Response time percentiles (p50, p95, p99)
- JVM memory usage and garbage collection
- Database connection pool utilization
- Database query performance
- External service call latency

### Profiling Tools

Use these tools for performance profiling:

- JFR (Java Flight Recorder) for JVM profiling
- Async-profiler for flame graphs
- Spring Boot Actuator for application metrics
- pgBadger for PostgreSQL query analysis

## Scaling Strategies

### Horizontal Scaling

For horizontal scaling:

1. Deploy multiple application instances behind a load balancer
2. Use sticky sessions if required for certain operations
3. Scale the database using read replicas for read-heavy operations
4. Consider database sharding for very high transaction volumes

### Vertical Scaling

For vertical scaling:

1. Increase CPU and memory resources
2. Adjust JVM heap size accordingly
3. Optimize database server resources
4. Ensure network capacity is sufficient

## Performance Testing

### Test Scenarios

We recommend testing the following scenarios:

1. **Baseline Performance**: Measure performance with a steady, moderate load
2. **Peak Load**: Test with expected peak transaction volume
3. **Stress Test**: Test with 2-3x expected peak volume
4. **Endurance Test**: Run at moderate load for 24+ hours
5. **Spike Test**: Suddenly increase load to simulate traffic spikes

### Testing Tools

Recommended performance testing tools:

- JMeter for load testing
- Gatling for high-throughput testing
- Chaos Monkey for resilience testing
- k6 for modern performance testing

### Sample JMeter Test Plan

A sample JMeter test plan is available in the `performance-tests` directory. This plan includes test cases for:

- Authorization requests
- Hold management operations
- Administrative operations

To run the test plan:

```bash
jmeter -n -t performance-tests/authorization-center-test-plan.jmx -l results.jtl -e -o report
```
