# Incident Response Platform — Architecture

## Table of Contents

1. [System Overview](#system-overview)
2. [High-Level Architecture](#high-level-architecture)
3. [Technology Stack](#technology-stack)
4. [Backend Architecture](#backend-architecture)
   - [Package Structure](#package-structure)
   - [Layered Design](#layered-design)
   - [Data Model](#data-model)
5. [Incident Lifecycle & State Machine](#incident-lifecycle--state-machine)
   - [State Transition Diagram](#state-transition-diagram)
   - [Guards & Actions](#guards--actions)
   - [SLA Tracking](#sla-tracking)
6. [Shifu Reporting Engine](#shifu-reporting-engine)
   - [Design Philosophy](#design-philosophy)
   - [Request Flow](#request-flow)
   - [Report Definitions](#report-definitions)
   - [Query Building](#query-building)
   - [Response Flattening](#response-flattening)
7. [REST API Design](#rest-api-design)
8. [Frontend Architecture](#frontend-architecture)
9. [Testing Strategy](#testing-strategy)
10. [Data Flow Diagrams](#data-flow-diagrams)

---

## System Overview

The Incident Response Platform manages the full lifecycle of production incidents — from creation through investigation, resolution, root cause analysis, and closure. It enforces workflow rules via a state machine, tracks SLA compliance, maintains an immutable audit trail, and provides a generic reporting engine (Shifu) for analytics.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        INCIDENT RESPONSE PLATFORM                   │
│                                                                     │
│  ┌──────────────┐    ┌──────────────────┐    ┌───────────────────┐ │
│  │   React UI   │───▶│  Spring Boot API  │───▶│  Elasticsearch   │ │
│  │  (Vite:3000) │◀───│    (:8080)        │◀───│    (:9200)       │ │
│  └──────────────┘    └──────────────────┘    └───────────────────┘ │
│                                                                     │
│  Features:                                                          │
│  • Incident CRUD & workflow        • SLA breach detection           │
│  • State machine enforcement       • Shifu reporting engine         │
│  • Audit trail                     • Database explorer              │
│  • Evidence attachments            • Structured JSON logging        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## High-Level Architecture

```
                           ┌──────────────────────┐
                           │      Browser / UI     │
                           │   React 19 + Vite     │
                           │   Tailwind CSS v4     │
                           └──────────┬───────────┘
                                      │ HTTP (port 3000)
                                      │ /api/* → proxy to 8080
                                      ▼
                           ┌──────────────────────┐
                           │    REST API Layer     │
                           │  IncidentController   │
                           │  ReportController     │
                           │  DatabaseController   │
                           └──────────┬───────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                  ▼
          ┌─────────────────┐ ┌────────────┐ ┌──────────────────┐
          │  Service Layer   │ │   State    │ │  Shifu Reporting │
          │                  │ │  Machine   │ │     Engine       │
          │ IncidentService  │ │            │ │                  │
          │ AuditService     │ │  Guards:   │ │ ShifuEngine      │
          │ SlaService       │ │  • Owner   │ │ ESQueryAdapter   │
          │ RcaService       │ │  • RCA     │ │ ReportRegistry   │
          │ EvidenceService  │ │            │ │ ReportDefinitions│
          └────────┬─────── │ │  Actions:  │ └────────┬─────────┘
                   │         │ │  • Audit   │          │
                   │         │ │  • SLA     │          │
                   │         │ └─────┬──────┘          │
                   │         │       │                  │
                   ▼         ▼       ▼                  ▼
          ┌──────────────────────────────────────────────────────┐
          │               Elasticsearch 8.11.x                   │
          │                                                      │
          │  Indices:                                             │
          │  ┌────────────┐ ┌──────────────┐ ┌────────────────┐ │
          │  │  incidents  │ │incident_audits│ │root_cause_     │ │
          │  │  (3 shards) │ │              │ │analyses        │ │
          │  └────────────┘ └──────────────┘ └────────────────┘ │
          │  ┌──────────────────┐                                │
          │  │ incident_evidence │                                │
          │  └──────────────────┘                                │
          └──────────────────────────────────────────────────────┘
```

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 3.2.3 |
| **Database** | Elasticsearch | 8.11.4 |
| **ES Client** | Spring Data Elasticsearch + co.elastic.clients | 5.2.x / 8.10.4 |
| **Build** | Gradle | 8.x |
| **Logging** | SLF4J + Logback + Logstash encoder | — |
| **Frontend** | React | 19.2.0 |
| **Bundler** | Vite | 7.x |
| **Styling** | Tailwind CSS | 4.x |
| **Routing** | React Router | 7.x |
| **Testing** | JUnit 5, Mockito, Testcontainers, Playwright | — |

---

## Backend Architecture

### Package Structure

```
org.example.incidentresponse/
├── IncidentResponseApplication.java     # Entry point
├── config/
│   ├── SlaProperties.java              # @ConfigurationProperties for SLA thresholds
│   └── WebConfig.java                  # CORS configuration
├── controller/
│   ├── IncidentController.java         # Incident CRUD + workflow endpoints
│   ├── ReportController.java           # Shifu reporting endpoints
│   └── DatabaseController.java         # Raw ES document search
├── document/                           # Elasticsearch document models
│   ├── IncidentDocument.java
│   ├── IncidentAuditDocument.java
│   ├── IncidentEvidenceDocument.java
│   └── RootCauseAnalysisDocument.java
├── dto/                                # Request/Response records
├── enums/                              # IncidentStatus, Severity, RcaCategory
├── exception/                          # Custom exceptions + GlobalExceptionHandler
├── filter/
│   └── MdcFilter.java                 # MDC context for structured logging
├── repository/                         # Spring Data ES repositories
├── reporting/                          # Shifu reporting engine
│   ├── ShifuEngine.java
│   ├── ESSearchQueryAdapter.java
│   ├── ReportRegistry.java
│   ├── ReportDefinition.java          # Interface for pluggable reports
│   └── definitions/                    # Concrete report definitions
├── service/                            # Business logic layer
│   ├── IncidentService.java
│   ├── AuditService.java
│   ├── SlaService.java
│   ├── RcaService.java
│   └── EvidenceService.java
└── statemachine/                       # Incident workflow engine
    ├── IncidentStateMachine.java
    ├── TransitionGuard.java            # Interface
    ├── TransitionAction.java           # Interface
    ├── guards/
    │   ├── OwnerAssignedGuard.java
    │   └── RcaExistsGuard.java
    └── actions/
        ├── AuditAction.java
        └── SlaAction.java
```

### Layered Design

```
┌────────────────────────────────────────────────────────────┐
│                    Controller Layer                         │
│  Handles HTTP, validation, delegates to services           │
│  Returns ProblemDetail (RFC 7807) for errors               │
├────────────────────────────────────────────────────────────┤
│                     Service Layer                           │
│  Orchestrates business logic, calls state machine          │
│  Manages cross-cutting: SLA init, audit recording          │
├────────────────────────────────────────────────────────────┤
│                  State Machine Layer                        │
│  Enforces transition rules, evaluates guards,              │
│  executes ordered actions (audit, SLA)                     │
├────────────────────────────────────────────────────────────┤
│                  Reporting Layer (Shifu)                    │
│  Generic report engine decoupled from domain logic         │
│  Pluggable definitions, dynamic query construction         │
├────────────────────────────────────────────────────────────┤
│                   Repository Layer                          │
│  Spring Data Elasticsearch repositories                    │
│  Thin abstraction over ES operations                       │
├────────────────────────────────────────────────────────────┤
│                   Elasticsearch                            │
│  Document store, full-text search, aggregations            │
└────────────────────────────────────────────────────────────┘
```

### Data Model

#### IncidentDocument (index: `incidents`, 3 shards, 1 replica)

```
┌──────────────────────────────────────────────────────────────┐
│                     IncidentDocument                          │
├──────────────┬───────────┬───────────────────────────────────┤
│ Field        │ ES Type   │ Purpose                           │
├──────────────┼───────────┼───────────────────────────────────┤
│ id           │ @Id       │ UUID primary key                  │
│ title        │ Text      │ Full-text searchable title        │
│ description  │ Text      │ Incident description              │
│ severity     │ Keyword   │ CRITICAL/HIGH/MEDIUM/LOW          │
│ status       │ Keyword   │ OPEN/INVESTIGATING/FIXED/...      │
│ reporterId   │ Keyword   │ Who reported it                   │
│ assigneeId   │ Keyword   │ Current owner                     │
│ tags         │ Object    │ Flexible key-value labels         │
├──────────────┼───────────┼───────────────────────────────────┤
│ acknowledgedAt│ Date     │ When moved to INVESTIGATING       │
│ resolvedAt   │ Date      │ When moved to FIXED               │
│ acknowledgeSlaMs │ Long  │ SLA threshold (from config)       │
│ resolutionSlaMs  │ Long  │ SLA threshold (from config)       │
│ acknowledgeSlaBreached │ Boolean │ Computed by SlaAction     │
│ resolutionSlaBreached  │ Boolean │ Computed by SlaAction     │
│ actualAcknowledgeMs    │ Long    │ Actual time to ack        │
│ actualResolutionMs     │ Long    │ Actual time to resolve    │
├──────────────┼───────────┼───────────────────────────────────┤
│ rcaCategory  │ Keyword   │ Denormalized from RCA doc         │
│ rcaSummary   │ Text      │ Denormalized from RCA doc         │
├──────────────┼───────────┼───────────────────────────────────┤
│ createdAt    │ Date      │ Creation timestamp                │
│ updatedAt    │ Date      │ Last modification                 │
│ timePartition│ Keyword   │ yyyy-MM partition key              │
└──────────────┴───────────┴───────────────────────────────────┘
```

**Design decision — Denormalization**: SLA and RCA fields are denormalized onto the incident document to enable single-index aggregation queries in the reporting engine. This avoids expensive cross-index joins in Elasticsearch.

#### Supporting Documents

```
IncidentAuditDocument (incident_audits)    RootCauseAnalysisDocument (root_cause_analyses)
┌──────────────────────┐                    ┌────────────────────────┐
│ id                   │                    │ id                     │
│ incidentId           │                    │ incidentId             │
│ userId               │                    │ category (Keyword)     │
│ field                │                    │ summary (Text)         │
│ oldValue             │                    │ details (Text)         │
│ newValue             │                    │ actionItems (Keyword[])│
│ createdAt            │                    │ createdBy              │
└──────────────────────┘                    │ createdAt              │
                                            └────────────────────────┘

IncidentEvidenceDocument (incident_evidence)
┌──────────────────────┐
│ id                   │
│ incidentId           │
│ fileName             │
│ fileUrl              │
│ description          │
│ uploadedBy           │
│ uploadedAt           │
└──────────────────────┘
```

---

## Incident Lifecycle & State Machine

### State Transition Diagram

```
                        ┌──────────────────────────────────────────────────────────────┐
                        │                  INCIDENT LIFECYCLE                           │
                        │                                                              │
                        │     ┌──────┐   assign    ┌───────────────┐   fix found       │
                        │     │      │   owner &   │               │   & verified      │
                        │     │ OPEN │────────────▶│ INVESTIGATING │──────────────▶     │
                        │     │      │  ack SLA    │               │  resolution SLA    │
                        │     └──────┘  starts     └───────────────┘  evaluated         │
                        │                  │                                 │           │
                        │        Guard:    │                                 │           │
                        │     Owner must   │                                 ▼           │
                        │     be assigned  │                          ┌───────────┐      │
                        │                  │                          │           │      │
                        │                  │                          │   FIXED   │      │
                        │                  │                          │           │      │
                        │                  │                          └─────┬─────┘      │
                        │                  │                                │            │
                        │                  │                                ▼            │
                        │                  │                       ┌─────────────┐       │
                        │                  │                       │             │       │
                        │                  │                       │ RCA_PENDING │       │
                        │                  │                       │             │       │
                        │                  │                       └──────┬──────┘       │
                        │                  │                              │              │
                        │                  │                     Guard:   │              │
                        │                  │                   RCA must   │              │
                        │                  │                   exist      ▼              │
                        │                  │                       ┌───────────┐         │
                        │                  │                       │           │         │
                        │                  │                       │  CLOSED   │         │
                        │                  │                       │           │         │
                        │                  │                       └───────────┘         │
                        │                  │                                             │
                        └──────────────────┴─────────────────────────────────────────────┘

     Transition          Guard                          Action (ordered)
     ──────────          ─────                          ────────────────
     OPEN → INVEST.      OwnerAssignedGuard             1. AuditAction  (writes audit record)
     INVEST. → FIXED     —                              2. SlaAction    (computes SLA breach)
     FIXED → RCA_PEND.   —
     RCA_PEND. → CLOSED  RcaExistsGuard
```

### Guards & Actions

**Guards** run *before* a transition is applied. If a guard fails, the transition is rejected with an appropriate HTTP error.

| Guard | Transition | Rule |
|-------|-----------|------|
| `OwnerAssignedGuard` | OPEN → INVESTIGATING | `assigneeId` must be non-null |
| `RcaExistsGuard` | RCA_PENDING → CLOSED | An `RootCauseAnalysisDocument` must exist for the incident |

**Actions** run *after* the status field is updated on the document (but before persistence). They are `@Order`-ed:

| Order | Action | Behavior |
|-------|--------|----------|
| 1 | `AuditAction` | Creates an `IncidentAuditDocument` recording the status change |
| 2 | `SlaAction` | On → INVESTIGATING: records `acknowledgedAt`, computes breach. On → FIXED: records `resolvedAt`, computes breach |

### SLA Tracking

```
Incident Created (createdAt)
        │
        │ ◄─── Time passes ───►
        │
        ▼
Transition to INVESTIGATING
        │
        ├─ acknowledgedAt = now
        ├─ actualAcknowledgeMs = now - createdAt
        └─ acknowledgeSlaBreached = (actualAcknowledgeMs > acknowledgeSlaMs)
        │
        │ ◄─── Time passes ───►
        │
        ▼
Transition to FIXED
        │
        ├─ resolvedAt = now
        ├─ actualResolutionMs = now - createdAt
        └─ resolutionSlaBreached = (actualResolutionMs > resolutionSlaMs)
```

SLA thresholds are configured per severity in `application.yml` and initialized on the incident document at creation time by `SlaService.initSlaFields()`.

---

## Shifu Reporting Engine

### Design Philosophy

Shifu is a **generic, pluggable reporting framework** that sits on top of Elasticsearch aggregations. It decouples report definitions from query mechanics:

- **Report authors** define *what* data is available (dimensions, measurements, base filters)
- **Consumers** request *which* slices they want (via `ShifuRequest`)
- **The engine** handles validation, query construction, execution, and response normalization

This means adding a new report type requires only implementing the `ReportDefinition` interface as a Spring bean — no query code, no controller changes.

### Request Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│              │     │              │     │                  │     │              │
│  ShifuRequest│────▶│ ShifuEngine  │────▶│ESSearchQuery     │────▶│Elasticsearch │
│              │     │              │     │    Adapter        │     │              │
│ • reportType │     │ 1. Resolve   │     │                  │     │ Aggregation  │
│ • timeRange  │     │    definition│     │ Builds:          │     │ Query        │
│ • dimensions │     │ 2. Validate  │     │ • Bool query     │     │              │
│ • measurements│    │ 3. Build     │     │ • Range filter   │     └──────┬───────┘
│ • filters    │     │ 4. Execute   │     │ • Term filters   │            │
│ • maxBuckets │     │ 5. Flatten   │     │ • Base filter    │            │
│              │     │              │     │ • Nested terms   │            │
└──────────────┘     └──────┬───────┘     │   aggregations   │            │
                            │             │ • Measurement    │            │
                            │             │   sub-aggs       │            │
                     ┌──────▼───────┐     └──────────────────┘            │
                     │              │                                      │
                     │ShifuResponse │◀─────────────────────────────────────┘
                     │              │     Flatten nested buckets
                     │ • totalHits  │     into ShifuBucket[]
                     │ • totals     │
                     │ • buckets[]  │
                     │   ├ dimensions│
                     │   ├ measurements│
                     │   └ docCount │
                     └──────────────┘
```

### Report Definitions

Each report is a Spring `@Component` implementing `ReportDefinition`:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ReportDefinition (interface)                  │
├─────────────────────────────────────────────────────────────────────┤
│  getReportType()          → "incident_summary"                      │
│  getIndexName()           → "incidents"                             │
│  getTimestampField()      → "createdAt"                             │
│  getDimensions()          → [status, severity, assignee, ...]       │
│  getMeasurements()        → [incident_count (VALUE_COUNT on status)] │
│  baseFilter()             → null (or a Query for pre-filtering)     │
│  getDimensionFieldMapping()  → {status→status, assignee→assigneeId} │
│  getMeasurementFieldMapping()→ {incident_count→status}              │
└─────────────────────────────────────────────────────────────────────┘
```

**Registered report types:**

| Report Type | Base Filter | Use Case |
|-------------|-------------|----------|
| `incident_summary` | None | General: cases by status, severity, assignee |
| `sla_breach` | `ackSlaBreached=true OR resSlaBreached=true` | Only SLA-breached incidents |
| `rca_category` | `rcaCategory exists` | Incidents with completed RCA |

New reports are auto-discovered by `ReportRegistry` (collects all `ReportDefinition` beans at startup).

### Query Building

The `ESSearchQueryAdapter` translates a `ShifuRequest` into an Elasticsearch `SearchRequest`:

```
ShifuRequest {
  reportType: "incident_summary",
  timeRange: { from: "2026-01-01", to: "2026-12-31" },
  dimensions: ["status", "severity"],
  measurements: [{ name: "count", type: VALUE_COUNT, field: "incident_count" }],
  filters: { severity: ["CRITICAL"] }
}

        │
        ▼ ESSearchQueryAdapter.buildSearchRequest()

{
  "index": "incidents",
  "size": 0,                           ◄── No documents, only aggregations
  "query": {
    "bool": {
      "must": [
        { "range": { "createdAt": { "gte": 1704067200000, "lte": 1735689600000 } } }
      ],
      "filter": [
        { "terms": { "severity": ["CRITICAL"] } }
      ]
    }
  },
  "aggregations": {
    "dim_status": {                     ◄── Outer dimension
      "terms": { "field": "status", "size": 100 },
      "aggregations": {
        "dim_severity": {               ◄── Inner dimension (nested)
          "terms": { "field": "severity", "size": 100 },
          "aggregations": {
            "count": {                  ◄── Leaf measurement
              "value_count": { "field": "status" }
            }
          }
        }
      }
    }
  }
}
```

### Response Flattening

ES returns deeply nested aggregation buckets. `ShifuEngine.flattenBuckets()` recursively walks the tree and produces flat `ShifuBucket` records:

```
ES Response (nested):                    Flattened ShifuBuckets:
                                         ┌──────────────────────────────┐
dim_status:                              │ {status:OPEN, severity:CRIT} │
  OPEN (3 docs)                          │  count: 1, docCount: 3      │
    dim_severity:                        ├──────────────────────────────┤
      CRITICAL (1)  count: 1   ───────▶  │ {status:OPEN, severity:HIGH} │
      HIGH (2)      count: 2             │  count: 2, docCount: 3      │
  CLOSED (5 docs)                        ├──────────────────────────────┤
    dim_severity:                        │ {status:CLOSED, severity:MED}│
      MEDIUM (3)    count: 3   ───────▶  │  count: 3, docCount: 5      │
      LOW (2)       count: 2             ├──────────────────────────────┤
                                         │ {status:CLOSED, severity:LOW}│
                                         │  count: 2, docCount: 5      │
                                         └──────────────────────────────┘
```

---

## REST API Design

### Incident Endpoints (`/api/incidents`)

```
POST   /api/incidents                     Create incident
GET    /api/incidents                     List (filterable: status, severity, pageable)
GET    /api/incidents/{id}                Get by ID
PUT    /api/incidents/{id}/owner          Assign owner
PUT    /api/incidents/{id}/status         Transition status (state machine enforced)
POST   /api/incidents/{id}/evidence       Attach evidence
POST   /api/incidents/{id}/rca            Record root cause analysis
GET    /api/incidents/{id}/audit          Get audit trail
GET    /api/incidents/{id}/sla            Get SLA metrics
```

### Reporting Endpoints (`/api/reports`)

```
POST   /api/reports/query                 Execute Shifu report query
GET    /api/reports/definitions            List all registered report definitions
GET    /api/reports/definitions/{type}     Get specific report definition metadata
```

### Database Explorer Endpoints (`/api/database`)

```
GET    /api/database/incidents            Raw ES search with filters
                                          Params: status, severity, assigneeId,
                                                  ackSlaBreached, resSlaBreached,
                                                  rcaCategory, q (full-text),
                                                  from, size
GET    /api/database/indices              List ES index metadata
```

**Error handling**: All errors return [RFC 7807 Problem Detail](https://datatracker.ietf.org/doc/html/rfc7807) responses via `GlobalExceptionHandler`.

---

## Frontend Architecture

### Page Structure

```
┌─────────────────────────────────────────────────────────────────────┐
│  Layout (sidebar + content area)                                    │
│  ┌─────────────┐  ┌─────────────────────────────────────────────┐  │
│  │             │  │                                             │  │
│  │  Sidebar    │  │  <Outlet /> — renders active page:          │  │
│  │             │  │                                             │  │
│  │  Incidents  │  │  /incidents      → Incidents (list + filter)│  │
│  │  New        │  │  /incidents/new  → CreateIncident (form)    │  │
│  │  Reports    │  │  /incidents/:id  → IncidentDetail           │  │
│  │  Database   │  │  /reports        → Reports (Shifu dashboard)│  │
│  │             │  │  /database       → DatabaseExplorer         │  │
│  │             │  │                                             │  │
│  └─────────────┘  └─────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Role |
|-----------|------|
| `Layout.jsx` | App shell with dark sidebar navigation |
| `StatusBadge.jsx` | Color-coded status pill (green=CLOSED, red=OPEN, etc.) |
| `SeverityBadge.jsx` | Color-coded severity pill |
| `client.js` | Centralized API client (fetch-based, error handling) |

### Pages

| Page | Description |
|------|-------------|
| **Incidents** | Paginated list with status/severity dropdown filters |
| **CreateIncident** | Form: title, description, severity, reporter ID |
| **IncidentDetail** | Workflow progress bar, action cards (assign, transition, evidence, RCA), SLA tab, audit tab |
| **Reports** | Shifu dashboard with 4 report cards (by status, closed total, SLA breaches, by assignee) |
| **DatabaseExplorer** | Raw ES document browser with 7 filter dimensions, expandable rows, SLA detail view, raw JSON |

### Proxy Configuration

Vite proxies `/api/*` requests to the Spring Boot backend:

```
Browser → localhost:3000/api/incidents → Vite proxy → localhost:8080/api/incidents
```

---

## Testing Strategy

```
┌──────────────────────────────────────────────────────────────────┐
│                        TEST PYRAMID                              │
│                                                                  │
│                         ╱╲                                       │
│                        ╱  ╲      E2E (Playwright)                │
│                       ╱    ╲     Browser-based UI tests          │
│                      ╱──────╲                                    │
│                     ╱        ╲   Integration (Testcontainers)    │
│                    ╱          ╲  IncidentLifecycleE2ETest         │
│                   ╱────────────╲ Full lifecycle + Shifu reports   │
│                  ╱              ╲                                 │
│                 ╱    Unit Tests  ╲                                │
│                ╱                  ╲ IncidentServiceTest           │
│               ╱                    ╲ IncidentStateMachineTest     │
│              ╱                      ╲ ShifuEngineTest             │
│             ╱________________________╲ ESSearchQueryAdapterTest   │
└──────────────────────────────────────────────────────────────────┘
```

| Layer | Framework | What's Tested |
|-------|-----------|---------------|
| **Unit** | JUnit 5 + Mockito | Service logic, state machine transitions/guards, Shifu validation, query building |
| **Integration** | Testcontainers (ES 8.11.4) | Full incident lifecycle: create → assign → investigate → fix → RCA → close; Shifu reports against real ES |
| **E2E/UI** | Playwright | Browser interaction flows (incident creation, workflow progression, database explorer) |

---

## Data Flow Diagrams

### 1. Create Incident Flow

```
Client                  IncidentController    IncidentService    SlaService     ES Repository
  │                          │                     │                │               │
  │ POST /api/incidents      │                     │                │               │
  │ {title, severity, ...}   │                     │                │               │
  │─────────────────────────▶│                     │                │               │
  │                          │ createIncident()    │                │               │
  │                          │────────────────────▶│                │               │
  │                          │                     │ initSlaFields()│               │
  │                          │                     │───────────────▶│               │
  │                          │                     │  Set ack/res   │               │
  │                          │                     │◀──thresholds───│               │
  │                          │                     │                                │
  │                          │                     │ save(doc)                      │
  │                          │                     │───────────────────────────────▶│
  │                          │                     │◀──────────────────────────────│
  │                          │◀────────────────────│                               │
  │◀─────────────────────────│ 200 IncidentResponse│                               │
```

### 2. Status Transition Flow

```
Client              Controller    IncidentService    StateMachine      Guards        Actions       ES
  │                     │              │                 │               │              │           │
  │ PUT /{id}/status    │              │                 │               │              │           │
  │ {status, userId}    │              │                 │               │              │           │
  │────────────────────▶│              │                 │               │              │           │
  │                     │ updateStatus │                 │               │              │           │
  │                     │─────────────▶│                 │               │              │           │
  │                     │              │ findOrThrow(id) │               │              │           │
  │                     │              │──────────────────────────────────────────────────────────▶│
  │                     │              │◀─────────────────────────────────────────────────────────│
  │                     │              │ transition()    │               │              │           │
  │                     │              │────────────────▶│               │              │           │
  │                     │              │                 │ check allowed │              │           │
  │                     │              │                 │──────────────▶│              │           │
  │                     │              │                 │  evaluate()   │              │           │
  │                     │              │                 │◀──────────────│              │           │
  │                     │              │                 │                              │           │
  │                     │              │                 │ set status                   │           │
  │                     │              │                 │                              │           │
  │                     │              │                 │ execute actions              │           │
  │                     │              │                 │─────────────────────────────▶│           │
  │                     │              │                 │              1. AuditAction  │           │
  │                     │              │                 │              2. SlaAction    │           │
  │                     │              │                 │◀─────────────────────────────│           │
  │                     │              │◀────────────────│               │              │           │
  │                     │              │ save(doc)       │               │              │           │
  │                     │              │──────────────────────────────────────────────────────────▶│
  │                     │◀─────────────│                 │               │              │           │
  │◀────────────────────│              │                 │               │              │           │
```

### 3. Shifu Report Query Flow

```
Client              ReportController    ShifuEngine     ReportRegistry    ESQueryAdapter     ES
  │                      │                  │                │                 │              │
  │ POST /reports/query  │                  │                │                 │              │
  │ {reportType,         │                  │                │                 │              │
  │  timeRange,          │                  │                │                 │              │
  │  dimensions,         │                  │                │                 │              │
  │  measurements}       │                  │                │                 │              │
  │─────────────────────▶│                  │                │                 │              │
  │                      │ execute(request) │                │                 │              │
  │                      │─────────────────▶│                │                 │              │
  │                      │                  │ get(reportType)│                 │              │
  │                      │                  │───────────────▶│                 │              │
  │                      │                  │◀───ReportDef───│                 │              │
  │                      │                  │                                  │              │
  │                      │                  │ validate(dims, measurements)     │              │
  │                      │                  │                                  │              │
  │                      │                  │ buildSearchRequest()             │              │
  │                      │                  │─────────────────────────────────▶│              │
  │                      │                  │◀──────SearchRequest──────────────│              │
  │                      │                  │                                                 │
  │                      │                  │ esClient.search()                               │
  │                      │                  │───────────────────────────────────────────────▶│
  │                      │                  │◀──────────────aggregation response─────────────│
  │                      │                  │                                                 │
  │                      │                  │ flattenBuckets()                                │
  │                      │                  │                                                 │
  │                      │◀─ShifuResponse───│                                                │
  │◀─────────────────────│                  │                                                │
  │                      │                  │                                                │
```

### 4. Complete Incident Lifecycle (End-to-End)

```
  Timeline ──────────────────────────────────────────────────────────────────────▶

  ┌────────┐    ┌───────────┐    ┌─────────────────┐    ┌───────┐    ┌─────────────┐    ┌────────┐
  │        │    │           │    │                 │    │       │    │             │    │        │
  │  OPEN  │───▶│  Assign   │───▶│  INVESTIGATING  │───▶│ FIXED │───▶│ RCA_PENDING │───▶│ CLOSED │
  │        │    │  Owner    │    │                 │    │       │    │             │    │        │
  └────────┘    └───────────┘    └─────────────────┘    └───────┘    └─────────────┘    └────────┘
       │              │                  │                   │              │                │
       │              │                  │                   │              │                │
  Created with    Guard:            SlaAction:          SlaAction:     Record RCA       Guard:
  SLA thresholds  OwnerAssigned     • acknowledgedAt    • resolvedAt   (RcaService)     RcaExists
  initialized     Guard checked     • actualAcknowledgeMs • actualResolutionMs           Guard
                                    • breach flag        • breach flag                   checked
       │              │                  │                   │              │                │
       ▼              ▼                  ▼                   ▼              ▼                ▼
  ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
  │                              AUDIT TRAIL (written at every transition)                       │
  │  OPEN→INVESTIGATING  │  INVESTIGATING→FIXED  │  FIXED→RCA_PENDING  │  RCA_PENDING→CLOSED    │
  └──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Extensibility

### Adding a New Report

1. Create a class implementing `ReportDefinition`
2. Annotate with `@Component`
3. Define dimensions, measurements, optional base filter
4. The engine auto-discovers it — no controller or query changes needed

```java
@Component
public class MyNewReport implements ReportDefinition {
    public String getReportType() { return "my_report"; }
    public String getIndexName() { return "incidents"; }
    // ... define dimensions, measurements, base filter
}
```

### Adding a New Guard

1. Implement `TransitionGuard`
2. Annotate with `@Component`
3. Define which transitions it applies to via `appliesTo(from, to)`

### Adding a New Action

1. Implement `TransitionAction`
2. Annotate with `@Component` and `@Order(n)`
3. Logic runs after every transition

---

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Database** | Elasticsearch (not PostgreSQL) | Native aggregation engine powers Shifu reports; full-text search on incidents; time-partitioned data model |
| **State machine** | Custom (not Spring Statemachine) | Simpler for a linear workflow; easier to test; transparent persistence |
| **Denormalization** | SLA + RCA fields on IncidentDocument | Single-index aggregations are orders of magnitude faster than cross-index joins in ES |
| **Reporting engine** | Generic Shifu framework | Decouples report definitions from query mechanics; new reports require zero query code |
| **SLA computation** | At transition time (not scheduled) | Deterministic, auditable; no background jobs needed |
| **Frontend bundler** | Vite | Fast HMR, native ESM support, simpler config than Webpack |
| **Error format** | RFC 7807 Problem Detail | Standard, machine-readable error responses |
