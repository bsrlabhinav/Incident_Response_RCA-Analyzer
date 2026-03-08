# Incident Response Platform

A full-lifecycle incident management platform for tracking production incidents from creation through investigation, resolution, root cause analysis (RCA), and closure. Built with Spring Boot, React, and Elasticsearch.

## Features

- **Incident Management** — Create, assign, update, and close incidents with severity levels and ownership tracking
- **State Machine Workflow** — Enforced status transitions (OPEN → INVESTIGATING → FIXED → RCA_PENDING → CLOSED) with guards ensuring owners are assigned before investigation and RCAs exist before closure
- **SLA Tracking** — Per-severity acknowledge and resolution SLA thresholds with automatic breach detection
- **Root Cause Analysis** — Record RCAs with categories, summaries, and action items
- **Evidence Attachments** — Attach supporting evidence to incidents
- **Audit Trail** — Automatic logging of all incident state changes
- **Shifu Reporting Engine** — Pluggable analytics with predefined reports for incident summaries, SLA breaches, and RCA categories
- **Database Explorer** — Browse raw Elasticsearch documents with filters and full-text search
- **User Management** — Create and manage users with roles for assignment and reporting

## Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | In-depth architecture guide covering backend layers, data model, state machine design, Shifu reporting engine internals, REST API reference, frontend structure, data flow diagrams, and extensibility patterns |
| [start.sh](start.sh) | One-command setup script that bootstraps Elasticsearch, the backend, the frontend, and optionally seeds sample data |
| [seed-data.sh](incident-response-ui/seed-data.sh) | Populates the platform with sample incidents, users, and RCAs for local development |

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot 3.2.3 |
| Database | Elasticsearch 8.11.4 |
| Frontend | React 19, Vite, Tailwind CSS |
| Routing | React Router 7 |
| Build | Gradle |
| Testing | JUnit 5, Mockito, Testcontainers, Playwright |

## Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- Docker

### One-Command Startup

The [`start.sh`](start.sh) script handles the full environment setup:

```bash
./start.sh
```

This starts Elasticsearch, the Spring Boot backend, and the Vite dev server, then seeds sample data.

| Flag | Description |
|------|-------------|
| `--no-seed` | Skip seeding sample data |
| `--clean` | Wipe existing data and re-seed |

### Manual Setup

**1. Start Elasticsearch**

```bash
docker run -d --name incident-es \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.4
```

**2. Start the backend**

```bash
./gradlew bootRun
```

**3. Start the frontend**

```bash
cd incident-response-ui
npm install
npm run dev
```

**4. Seed sample data (optional)**

```bash
cd incident-response-ui
bash seed-data.sh
```

The backend runs on `http://localhost:8080` and the frontend on `http://localhost:3000` (proxying `/api/*` to the backend).

## Architecture

> For the full architecture deep-dive — including data models, sequence diagrams, the Shifu reporting engine, and extensibility patterns — see [ARCHITECTURE.md](ARCHITECTURE.md).

```
Browser (React) → Vite :3000 → /api/* proxy → Spring Boot :8080 → Elasticsearch :9200
```

### Backend Layers

| Layer | Responsibility |
|-------|---------------|
| Controllers | HTTP handling, request validation, response mapping |
| Services | Business logic, orchestration, state machine invocation |
| State Machine | Transition validation, guards, and side-effect actions |
| Reporting (Shifu) | Report definitions, ES aggregation queries, analytics |
| Repositories | Spring Data Elasticsearch data access |

### Incident Lifecycle

```
OPEN ──→ INVESTIGATING ──→ FIXED ──→ RCA_PENDING ──→ CLOSED
         (requires owner)                            (requires RCA)
```

Guards enforce preconditions at each transition, and actions automatically record audit entries and compute SLA breaches. See the [State Machine](ARCHITECTURE.md#incident-lifecycle--state-machine) and [Guards & Actions](ARCHITECTURE.md#guards--actions) sections in the architecture doc for the full transition diagram and details.

### Elasticsearch Indices

| Index | Purpose |
|-------|---------|
| `incidents` | Core incident documents |
| `incident_audits` | Change audit trail |
| `incident_evidence` | Attached evidence |
| `root_cause_analyses` | RCA records |
| `users` | User profiles |

## API

All endpoints are under `/api/`:

| Resource | Path | Description |
|----------|------|-------------|
| Incidents | `/api/incidents` | CRUD, status transitions, assign owner |
| Reports | `/api/reports` | Shifu reporting engine queries |
| Database | `/api/database` | Raw ES document browsing and index info |
| Users | `/api/users` | User management |

## Configuration

SLA thresholds are configured in `src/main/resources/application.yml`:

| Severity | Acknowledge | Resolution |
|----------|-------------|------------|
| Critical | 5s | 10s |
| High | 30s | 60s |
| Medium | 5m | 15m |
| Low | 1h | 24h |

## Testing

**Backend unit and integration tests** (uses Testcontainers for Elasticsearch):

```bash
./gradlew test
```

**Frontend E2E tests** (Playwright):

```bash
cd incident-response-ui
npx playwright test
```

## Project Structure

```
├── src/main/java/org/example/incidentresponse/
│   ├── controller/        # REST controllers
│   ├── service/           # Business logic
│   ├── statemachine/      # Workflow engine, guards, actions
│   ├── reporting/         # Shifu analytics engine
│   ├── document/          # Elasticsearch document models
│   ├── dto/               # Request/response DTOs
│   ├── repository/        # Data access layer
│   ├── enums/             # Status, severity, RCA categories
│   ├── exception/         # Error handling
│   ├── config/            # App configuration
│   └── filter/            # HTTP filters (MDC logging)
├── src/test/              # Backend tests
├── incident-response-ui/  # React frontend
│   ├── src/
│   │   ├── pages/         # Page components
│   │   ├── components/    # Shared UI components
│   │   └── api/           # API client
│   └── *.spec.js          # Playwright E2E tests
├── ARCHITECTURE.md        # Detailed architecture documentation
└── start.sh               # One-command setup script
```

## Further Reading

- [ARCHITECTURE.md](ARCHITECTURE.md) — Deep-dive into system design, data models, state machine, Shifu engine, API reference, and extensibility
- [Backend Configuration](src/main/resources/application.yml) — SLA thresholds, Elasticsearch connection, logging levels
- [Vite Proxy Config](incident-response-ui/vite.config.js) — Frontend dev server and API proxy setup
