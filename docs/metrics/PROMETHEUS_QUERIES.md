# Prometheus Query Guide for Elicit Applications

This guide provides useful PromQL queries to monitor your Elicit applications. Copy and paste these queries into the Prometheus web interface at http://localhost:9090.

## 🚀 Application Performance Queries

### Request Rate and Volume
```promql
# Total requests per second across all applications
sum(rate(http_server_requests_seconds_count[5m]))

# Requests per second by application
sum(rate(http_server_requests_seconds_count[5m])) by (job)

# Requests per second by endpoint
sum(rate(http_server_requests_seconds_count[5m])) by (job, uri)

# Top 10 busiest endpoints
topk(10, sum(rate(http_server_requests_seconds_count[5m])) by (uri))
```

### Response Times
```promql
# Average response time across all applications
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# 95th percentile response time by application
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (job, le))

# 99th percentile response time by application
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (job, le))

# Slowest endpoints (95th percentile)
topk(10, histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (uri, le)))
```

### Error Rates
```promql
# Overall error rate (4xx and 5xx responses)
sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))

# Error rate by application
sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])) by (job) / sum(rate(http_server_requests_seconds_count[5m])) by (job)

# 5xx server errors by endpoint
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (uri)

# 4xx client errors by endpoint
sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m])) by (uri)
```

## 💾 JVM and Memory Queries

### Memory Usage
```promql
# JVM heap memory usage ratio by application
sum(jvm_memory_used_bytes{area="heap"}) by (job) / sum(jvm_memory_max_bytes{area="heap"}) by (job)

# JVM non-heap memory usage
sum(jvm_memory_used_bytes{area="nonheap"}) by (job)

# Memory usage by memory pool
jvm_memory_used_bytes / jvm_memory_max_bytes

# Applications using more than 80% heap memory
(sum(jvm_memory_used_bytes{area="heap"}) by (job) / sum(jvm_memory_max_bytes{area="heap"}) by (job)) > 0.8
```

### Garbage Collection
```promql
# GC rate per second by application
rate(jvm_gc_collection_seconds_count[5m])

# Time spent in GC as percentage
rate(jvm_gc_collection_seconds_sum[5m]) * 100

# GC pause times by collector
rate(jvm_gc_collection_seconds_sum[5m]) / rate(jvm_gc_collection_seconds_count[5m])
```

### Thread Usage
```promql
# Current thread count by application
jvm_threads_current

# Thread states breakdown
jvm_threads_states_threads

# Peak thread count
jvm_threads_peak
```

## 🗄️ Database Performance Queries

### SQL Execution Performance
```promql
# Database query execution rate by application
rate(hibernate_sessions_opened_total[5m])

# Database query rate by application (only apps with DB access)
sum(rate(hibernate_sessions_opened_total[5m])) by (job)

# Transaction rate by application
rate(hibernate_transactions_total[5m])

# Transaction success rate
rate(hibernate_transactions_total{result="success"}[5m]) / rate(hibernate_transactions_total[5m])

# Database operations by type (if available)
sum(rate(hibernate_entities_fetches_total[5m])) by (job, entity_name)

# SQL Cache hit ratio (if second level cache enabled)
rate(hibernate_second_level_cache_hit_count[5m]) / rate(hibernate_second_level_cache_requests_total[5m])
```

### Connection Pool Monitoring
```promql
# Active database connections by application
sum(agroal_active_count) by (job, datasource)

# Maximum connections used
agroal_max_used_count

# Connection pool usage ratio
agroal_active_count / agroal_max_count

# Applications with high connection usage (>80%)
(agroal_active_count / agroal_max_count) > 0.8

# Connection creation rate (indicates pool pressure)
rate(agroal_creation_count[5m])

# Connection destroy rate
rate(agroal_destroy_count[5m])

# Net connection growth (creation - destruction)
rate(agroal_creation_count[5m]) - rate(agroal_destroy_count[5m])

# Invalid connection rate (connection failures)
rate(agroal_invalid_count[5m])

# Connection pool wait time (if available)
agroal_blocking_timeout_total / agroal_active_count
```

### SQL Performance by Application
```promql
# Query execution rate for Survey application
rate(hibernate_sessions_opened_total{job="survey-app"}[5m])

# Query execution rate for Admin application  
rate(hibernate_sessions_opened_total{job="admin-app"}[5m])

# Query execution rate for FHHS application
rate(hibernate_sessions_opened_total{job="fhhs-app"}[5m])

# Query execution rate for PREMM5 application
rate(hibernate_sessions_opened_total{job="premm5-app"}[5m])

# Applications with no database activity (should be Pedigree)
absent(hibernate_sessions_opened_total{job="pedigree-app"})

# Top database-active applications by query volume
topk(5, sum(rate(hibernate_sessions_opened_total[5m])) by (job))
```

### Database Response Time Analysis
```promql
# Average session open time (approximation of query time)
rate(hibernate_sessions_opened_total[5m]) > 0

# Connection acquisition time distribution
histogram_quantile(0.95, rate(agroal_acquire_count[5m]))

# Transaction duration by application (if available)
rate(hibernate_transactions_total[5m]) / rate(hibernate_sessions_opened_total[5m])

# Database connection pool efficiency
agroal_active_count / agroal_max_count

# Connection leaks detection
increase(agroal_leak_detection_count[1h])
```

### SQL Query Pattern Analysis
```promql
# Entity fetch patterns by application
sum(rate(hibernate_entities_fetches_total[5m])) by (job)

# Query cache effectiveness
sum(rate(hibernate_query_cache_hit_count[5m])) by (job) / sum(rate(hibernate_query_cache_requests_total[5m])) by (job)

# Lazy loading patterns (N+1 query detection)
rate(hibernate_collections_fetches_total[5m])

# Flush operations indicating write activity
rate(hibernate_flush_count[5m])

# Entity updates vs loads ratio
rate(hibernate_entities_updates_total[5m]) / rate(hibernate_entities_fetches_total[5m])
```

### Database Health and Errors
```promql
# Database connection failures
rate(agroal_invalid_count[5m])

# Transaction rollbacks
rate(hibernate_transactions_total{result="failure"}[5m])

# Database connection pool exhaustion
agroal_active_count >= agroal_max_count

# Connection timeout events
rate(agroal_blocking_timeout_total[5m])

# Applications with database connectivity issues
(rate(agroal_invalid_count[5m]) / rate(agroal_creation_count[5m])) > 0.05
```

## 📊 Business Metrics (Custom)

### Survey Application Metrics
```promql
# Survey completion rate (requires custom metrics)
rate(survey_completed_total[1h]) / rate(survey_started_total[1h])

# Active survey sessions
survey_active_sessions

# Average survey completion time
rate(survey_completion_time_sum[1h]) / rate(survey_completion_time_count[1h])
```

## 🔍 System Health Queries

### Application Availability
```promql
# Application uptime
up

# Applications that are down
up == 0

# Uptime percentage over 24 hours
avg_over_time(up[24h]) * 100
```

### Resource Utilization
```promql
# CPU usage by application (if available)
rate(process_cpu_seconds_total[5m]) * 100

# File descriptor usage
process_open_fds / process_max_fds

# Network connections
jvm_classes_loaded
```

## 🚨 Alert Condition Queries

### Performance Alerts
```promql
# High error rate (>5%)
(sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))) > 0.05

# High response time (95th percentile >2s)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2

# High memory usage (>85% heap)
(sum(jvm_memory_used_bytes{area="heap"}) by (job) / sum(jvm_memory_max_bytes{area="heap"}) by (job)) > 0.85
```

## 📈 Trend Analysis Queries

### Growth Patterns
```promql
# Request volume growth over 7 days
increase(http_server_requests_seconds_count[7d])

# Memory usage trend
avg_over_time(jvm_memory_used_bytes[1h])

# Error rate trend over 24 hours
avg_over_time((sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])))[24h:1h])
```

## 🎯 Quick Health Dashboard Queries

Copy these queries to create a quick health overview:

1. **Application Status**: `up`
2. **Request Rate**: `sum(rate(http_server_requests_seconds_count[5m])) by (job)`
3. **Error Rate**: `sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])) by (job) / sum(rate(http_server_requests_seconds_count[5m])) by (job)`
4. **Response Time**: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (job, le))`
5. **Memory Usage**: `sum(jvm_memory_used_bytes{area="heap"}) by (job) / sum(jvm_memory_max_bytes{area="heap"}) by (job)`
6. **DB Connections**: `sum(agroal_active_count) by (job, datasource)`

## 💡 Tips for Using Prometheus UI

1. **Time Range**: Use the time picker to focus on specific periods
2. **Auto-refresh**: Enable auto-refresh for real-time monitoring
3. **Graph vs Table**: Switch between graph and table views for different insights
4. **Expression Browser**: Use Tab completion for metric names and label values
5. **Recording Rules**: Use the pre-defined recording rules (elicit:*) for faster queries