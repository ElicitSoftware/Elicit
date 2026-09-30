# Prometheus Configuration

This directory contains all Prometheus-related configuration and storage for the Elicit platform.

> **Nothing here runs today.** There is no `prometheus` service in `docker-compose.yml`, so
> nothing scrapes the applications' `/q/metrics` endpoints and nothing serves `localhost:9090`.
> The applications do expose the endpoints, and the rules below are written against them, but
> this configuration describes a metrics pillar that is specified and not deployed. Jaeger is
> independent of it: the compose file runs `all-in-one` with OTLP enabled, which is traces only,
> so the Monitor tab in the Jaeger UI stays inert.

## 📁 Directory Structure

```
Prometheus/
├── prometheus.yml          # Main Prometheus configuration
├── recording_rules.yml     # Pre-calculated metrics rules
├── alerting_rules.yml      # Alert definitions
├── data/                   # Prometheus time-series storage (auto-generated)
└── README.md              # This file
```

## 🚀 Quick Start

### Using Docker Compose
The `docker-compose.yml` in the root directory automatically mounts this folder:

```bash
# Start Prometheus with the rest of the Elicit platform
docker-compose up -d

# Access Prometheus at http://localhost:9090
```

### Configuration Files

- **`prometheus.yml`**: Main configuration with scrape targets for all Elicit applications
- **`recording_rules.yml`**: Pre-calculated metrics for dashboard performance and complex queries
- **`alerting_rules.yml`**: Alert rules for critical application health monitoring
- **`data/`**: Time-series database storage (created automatically by Prometheus)

## 📊 Monitoring Coverage

`prometheus.yml` defines these scrape jobs:
- **survey-app**, **admin-app**, **fhhs-app** — the Quarkus applications, scraped by service
  name on the container-internal port 8080, not the host ports the compose file publishes
- **premm5-app** — PREMM5, which is commented out of `docker-compose.yml` and not cloned, so
  this job never resolves
- **postgresql**, **cadvisor** — expect a `postgres-exporter` and a `cadvisor` container,
  neither of which exists in this compose file
- **prometheus** — itself

Pedigree has no job at all. See the note at the top of this file: nothing runs this
configuration today.

## 🔍 Query Resources

For ready-to-use monitoring queries, see:
- [`docs/metrics/PROMETHEUS_QUERIES.md`](../docs/metrics/PROMETHEUS_QUERIES.md) - Comprehensive PromQL query library
- [`docs/metrics/OBSERVABILITY_IMPLEMENTATION_GUIDE.md`](../docs/metrics/OBSERVABILITY_IMPLEMENTATION_GUIDE.md) - Complete implementation guide

## 🚨 Alerts

The configuration includes pre-built alerts for:
- Application health and availability
- High error rates and response times
- JVM memory and garbage collection issues
- Database connection pool problems
- SQL query performance issues

## 💾 Data Retention

By default, Prometheus retains data for 15 days. To modify retention:

1. Edit the docker-compose.yml service arguments:
   ```yaml
   command:
     - '--storage.tsdb.retention.time=30d'  # 30 days retention
   ```

2. Or set size-based retention:
   ```yaml
   command:
     - '--storage.tsdb.retention.size=10GB'
   ```

## 🔧 Customization

To add new applications or modify monitoring:

1. **Add new scrape target**: Edit `prometheus.yml` scrape_configs section
2. **Add custom metrics**: Extend `recording_rules.yml` 
3. **Add new alerts**: Extend `alerting_rules.yml`
4. **Restart Prometheus**: `docker-compose restart prometheus`

## 📋 Maintenance

- **View logs**: `docker-compose logs prometheus`
- **Restart service**: `docker-compose restart prometheus`
- **Backup data**: Copy the `data/` directory
- **Clean data**: Stop container and remove `data/` directory