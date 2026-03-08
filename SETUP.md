# Incident Response Platform — Setup Guide

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| **Java JDK** | 17+ | `java -version` |
| **Docker** | Any (Docker Desktop, Rancher Desktop, Colima) | `docker --version` |
| **Node.js** | 18+ | `node -v` |
| **npm** | 9+ | `npm -v` |

> **Rancher Desktop users**: The build is pre-configured to auto-detect `~/.rd/docker.sock`.

---

## Quick Start (One Command)

```bash
./start.sh
```

This script will:
1. Start Elasticsearch in Docker (port `9200`)
2. Build and start the Spring Boot backend (port `8080`)
3. Install frontend dependencies and start the Vite dev server (port `3000`)
4. Seed the database with 10 realistic incidents (SLA breaches, different assignees, full lifecycle)
5. Open the UI at `http://localhost:3000`

See the [start.sh](#startsh-details) section for flags and options.

---

## Manual Setup

### 1. Start Elasticsearch

```bash
docker run -d --name incident-es \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.4
```

Wait for it to be healthy:

```bash
curl -s http://localhost:9200/_cluster/health | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])"
```

Expected output: `green` or `yellow`.

### 2. Start the Backend

```bash
./gradlew bootRun
```

The Spring Boot app starts on **port 8080**. It will auto-create Elasticsearch indices on first write.

Verify:

```bash
curl -s http://localhost:8080/api/incidents?page=0&size=1 | head -c 80
```

### 3. Start the Frontend

```bash
cd incident-response-ui
npm install
npm run dev
```

The Vite dev server starts on **port 3000** and proxies `/api/*` requests to `localhost:8080`.

Open: [http://localhost:3000](http://localhost:3000)

### 4. Seed Test Data (Optional)

```bash
cd incident-response-ui
bash seed-data.sh
```

Creates 10 incidents across all severities with:
- 3 CRITICAL incidents that breach SLA (acknowledgement and/or resolution)
- 6 closed incidents by 3 different users (Alice, Bob, Charlie)
- 1 open/unassigned incident
- Full lifecycle progression with RCA recordings

---

## Configuration

### SLA Thresholds

Edit `src/main/resources/application.yml`:

```yaml
incident:
  sla:
    critical:
      acknowledge-ms: 5000      # 5 seconds (demo-friendly)
      resolution-ms: 10000      # 10 seconds
    high:
      acknowledge-ms: 30000     # 30 seconds
      resolution-ms: 60000      # 1 minute
    medium:
      acknowledge-ms: 300000    # 5 minutes
      resolution-ms: 900000     # 15 minutes
    low:
      acknowledge-ms: 3600000   # 1 hour
      resolution-ms: 86400000   # 24 hours
```

For production, increase these to realistic values (e.g., CRITICAL: 15min ack, 4hr resolution).

### Elasticsearch Connection

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

### Logging

Structured JSON logging is configured via `logback-spring.xml`. In non-test profiles, all logs are emitted as JSON with MDC fields (`requestId`, `userId`, `incidentId`).

---

## Running Tests

### Unit Tests

```bash
./gradlew test
```

Includes:
- `IncidentServiceTest` — Service layer with mocks
- `IncidentStateMachineTest` — State transitions, guards
- `ShifuEngineTest` — Report validation, registry
- `ESSearchQueryAdapterTest` — Query building

### Integration / E2E Tests

```bash
./gradlew test --tests "*E2E*"
```

Requires Docker (uses Testcontainers to spin up a temporary Elasticsearch instance).

`IncidentLifecycleE2ETest` drives 6 incidents through their full lifecycle and validates Shifu reporting output.

### Frontend Tests (Playwright)

```bash
cd incident-response-ui
npx playwright test
```

Requires the backend + ES to be running.

---

## Ports Summary

| Service | Port | Purpose |
|---------|------|---------|
| Elasticsearch | 9200 | Document store |
| Spring Boot API | 8080 | REST API (accessed via frontend proxy, not directly in browser) |
| Vite Dev Server | 3000 | Frontend UI (proxies `/api/*` to backend on 8080) |

---

## Stopping Everything

```bash
# Frontend: Ctrl+C in the npm terminal

# Backend: Ctrl+C in the gradlew terminal

# Elasticsearch:
docker stop incident-es && docker rm incident-es
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `ECONNREFUSED` from frontend | Backend isn't running. Start it with `./gradlew bootRun` |
| `Connection refused` from backend | Elasticsearch isn't running. Start the Docker container |
| `Port 8080 already in use` | Kill the old process: `lsof -i :8080 -P` then `kill <PID>` |
| Testcontainers can't find Docker | Set `DOCKER_HOST=unix://$HOME/.rd/docker.sock` (auto-done in `build.gradle` for Rancher) |
| SLA breaches not showing | Ensure you're running with the demo thresholds (5s/10s for CRITICAL) |
| Stale data after restart | Delete indices: `curl -X DELETE http://localhost:9200/incidents,incident_audits` then re-seed |
