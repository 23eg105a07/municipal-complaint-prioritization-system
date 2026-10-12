# Municipal Complaint Prioritization System

> **Smart Civic Issue Management & Dynamic Priority Scoring Engine**  
> Automated Severity Assessment • Dynamic Priority Queue • Officer Routing • SLA Escalation Triggers

---

## 1. Abstract & Objectives

The **Municipal Complaint Prioritization System** replaces conventional static issue ticketing with an algorithm-driven civic resolution platform. Rather than processing issues on a first-come-first-served basis, the engine continuously computes dynamic multi-factor priority scores based on:
1. **Severity level** (1 = minor to 5 = critical emergency)
2. **Impact / Population density** (affected citizens + community upvotes)
3. **Complaint age in hours** (continuous aging multiplier)
4. **Critical safety hazard boost** (instant $+50$ priority boost overriding normal queues)

The system automatically routes civic issues to relevant municipal departments (`ROADS`, `WATER`, `ELECTRICAL`), facilitates dispatch to field officers, monitors SLAs with background escalation triggers, and provides full transparency through an auditable status pipeline and citizen feedback loop.

---

## 2. Architecture & Design Principles

### Layered Architecture
- **Controllers**: Strictly handle request binding, input validation (`@Valid`), role authorization, and delegation.
- **Services**: Own business transactions (`@Transactional`), domain invariant rules, and caching boundaries.
- **Repositories**: Spring Data JPA repositories with native Flyway schema versioning.
- **DTOs & Mappers**: Entities never escape the service layer; clean immutable Java records represent all API contracts.

### Request Flow
```text
HTTP Request
     │
     ▼
[JwtAuthenticationFilter] ──(Validates Bearer JWT)──► [SecurityFilterChain]
                                                              │
                                                              ▼
                                                        [Controller]
                                                              │
                                                              ▼
                                                     [Service Layer]
                                                   (@Transactional)
                                                     │           │
                          ┌──────────────────────────┘           └──────────────────────────┐
                          ▼                                                                 ▼
                [PostgreSQL (Flyway)]                                              [Redis / Cache]
          (Complaints, Logs, Upvotes)                                         (Priority Queue, Analytics)
```

### Background Workers
- **`PriorityRecalculationJob`**: Periodically updates age multipliers and dynamic scores for all active tickets.
- **`SlaEscalationJob`**: Scans unresolved complaints against severity-defined SLA targets and triggers automated escalation with audit logs.

---

## 3. Priority Scoring Algorithm

The prioritization engine evaluates issue urgency using a weighted dynamic algorithm:

$$\text{Priority Score } (P) = (\text{Base Severity} \times W_1) + (\text{Affected People} \times W_2) + (\text{Age in Hours} \times W_3) + [\text{Critical Boost}]$$

### Default Configuration
| Metric | Weight / Parameter | Default Value | Notes |
| :--- | :--- | :--- | :--- |
| **Base Severity** | $W_1$ | `0.5` | Range: $1$ (minor) to $5$ (critical emergency) |
| **Affected People** | $W_2$ | `0.3` | Baseline $1$ (reporter) $+$ unique citizen upvotes |
| **Age in Hours** | $W_3$ | `0.2` | Fractional hours elapsed ($\text{seconds} \div 3600$) |
| **Critical Safety Boost** | Boost | `+50.00` | Applied instantly when $\text{severity} \ge 5$ |
| **Rounding Mode** | Rounding | `HALF_UP` | Applied once at final step to 2 decimal places |

*Scores are compared using `BigDecimal.compareTo`. In the priority queue, ties break by older `created_at` first, then lower `id`.*

---

## 4. SLA Targets & Escalation Engine

Resolution deadlines are configured deterministically by issue severity:

| Severity Level | Description | SLA Target (Hours) | Escalation Rule |
| :---: | :--- | :---: | :--- |
| **1** | Minor civic inconvenience | **168 hours** (7 days) | Single escalation upon target breach |
| **2** | Moderate maintenance issue | **120 hours** (5 days) | Single escalation upon target breach |
| **3** | Noticeable disruption | **72 hours** (3 days) | Single escalation upon target breach |
| **4** | Major civic obstruction | **48 hours** (2 days) | Single escalation upon target breach |
| **5** | Critical safety emergency | **24 hours** (1 day) | Single escalation upon target breach |

When an unresolved ticket exceeds its target deadline:
1. `is_escalated` is flagged as `true`.
2. `escalated_at` timestamp is recorded.
3. An automated audit entry is logged in `complaint_status_logs`.

---

## 5. Resolution Workflow Pipeline

```text
[Citizen Reports] ──► [REPORTED]
                           │
                           ├──► [VERIFIED] ──► [ASSIGNED] ──► [IN_PROGRESS] ──► [RESOLVED] ──► [CLOSED]
                           │                      │                 │               ▲
                           ▼                      ▼                 ▼               │
                      [REJECTED]             [REJECTED]        [REJECTED]    (Citizen Feedback)
```

- **REPORTED**: Ticket received with geo-coordinates, photos, and initial priority score.
- **VERIFIED**: Municipal review verifies issue validity.
- **ASSIGNED**: Assigned by Admin to a field officer in the matching department and zone.
- **IN_PROGRESS**: Field officer begins resolution work.
- **RESOLVED**: Officer submits proof-of-work photo and remarks.
- **CLOSED**: Reporting citizen verifies resolution, rates 1–5 stars, and closes the ticket.

---

## 6. Directory Structure

```text
municipal-complaint-system/
├── .env.example
├── .gitignore
├── docker-compose.yml
├── README.md
│
├── frontend/                                       # Standalone Frontend Client (CivicPulse)
│   ├── index.html                                  # Single Page Application
│   └── README.md                                   # Frontend setup & guide
│
└── backend/                                        # Spring Boot 3.3.5 Backend API
    ├── pom.xml                                     # Maven dependency management
    ├── Dockerfile                                  # Multi-stage production container build
    ├── .dockerignore
    ├── http/
    │   └── municipal-api.http                      # Ready-to-run HTTP API requests
    └── src/
        ├── main/
        │   ├── java/com/municipal/system/
        │   │   ├── MunicipalSystemApplication.java # UTC bootstrap entry point
        │   │   ├── config/                         # AppProperties, Clock, Cache, OpenAPI, Seeder
        │   │   ├── security/                       # JWT Token Provider, Security Filter Chain
        │   │   ├── auth/                           # Register, Login, Auth tokens
        │   │   ├── user/                           # Users, Roles, Admin officer management
        │   │   ├── department/                     # Departments reference service
        │   │   ├── complaint/                      # Complaint entities, upvoting, priority queue
        │   │   ├── assignment/                     # Field officer assignment logic
        │   │   ├── prioritization/                 # Dynamic scoring engine & recalculator
        │   │   ├── workflow/                       # Transition policies, audit logs, feedback
        │   │   ├── sla/                            # SLA tracking & automated escalation worker
        │   │   ├── analytics/                      # Heatmaps, zone turnaround metrics, reports
        │   │   ├── common/                         # Base entity auditing, ApiError, PageResponse
        │   │   └── exception/                      # Global REST exception handling
        │   └── resources/
        │       ├── application.yml                 # Production PostgreSQL/Redis config
        │       ├── application-local.yml           # Zero-dependency local standalone profile
        │       ├── static/index.html               # Embedded UI copy
        │       └── db/migration/
        │           ├── V1__create_schema.sql
        │           └── V2__seed_departments.sql
        └── test/
            ├── java/com/municipal/system/
            │   ├── prioritization/PriorityScoringServiceTest.java
            │   ├── complaint/UpvoteServiceTest.java
            │   ├── workflow/WorkflowTransitionPolicyTest.java
            │   ├── sla/SlaServiceTest.java
            │   └── integration/
            │       ├── AbstractIntegrationTest.java
            │       └── ComplaintFlowIT.java
            └── resources/
                └── application-test.yml
```

---

## 7. REST API Endpoints

### Authentication & Public Reference Data
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Public | Register citizen account |
| `POST` | `/api/auth/login` | Public | Login & receive Bearer JWT |
| `GET` | `/api/departments` | Public | List all municipal departments |
| `GET` | `/api/departments/{id}` | Public | Get specific department details |

### Complaints & Prioritization
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/complaints` | Authenticated | Submit civic complaint with GPS & category |
| `GET` | `/api/complaints/priority` | Authenticated | Get dynamic priority queue (score desc) |
| `GET` | `/api/complaints` | Authenticated | List complaints (filter by status/zone/dept) |
| `GET` | `/api/complaints/{id}` | Authenticated | Get complaint details and active assignment |
| `POST` | `/api/complaints/{id}/upvote` | Authenticated | Citizen upvote (increments affected count) |

### Dispatch, Workflow & Feedback
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `PUT` | `/api/complaints/{id}/assign` | `ADMIN` | Assign ticket to matching field officer |
| `PATCH` | `/api/complaints/{id}/status` | `ADMIN`, `OFFICER` | Update pipeline stage with proof photo |
| `GET` | `/api/complaints/{id}/history` | Authenticated | Retrieve complete audit trail |
| `POST` | `/api/complaints/{id}/feedback` | Citizen (reporter) | Submit 1–5 star rating & close ticket |

### Administration & Analytics
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/admin/officers` | `ADMIN` | Create field officer with department & zone |
| `GET` | `/api/admin/officers` | `ADMIN` | List field officers |
| `GET` | `/api/reports/analytics` | `ADMIN`, `OFFICER` | Aggregated metrics, zone stats, ratings |

---

## 8. Getting Started & Running

### Prerequisites
- **Java 17+** (JDK 17 or JDK 21/23)
- **Maven 3.9+**
- **Docker & Docker Compose** (only for the containerized stack and integration tests)

### Run Locally (H2, No Docker Required)
Start the backend with its local profile:
   ```bash
   mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
   ```
The local profile uses an in-memory H2 database. To create an initial administrator, set `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` in your environment before starting the app. The defaults intentionally create no administrator.

Access the application at **`http://localhost:8085`** and Swagger UI at **`http://localhost:8085/swagger-ui.html`**.

### Run with Docker Compose
1. Copy `.env.example` to `.env` and replace the placeholder database password and JWT secret with unique local values.
2. Start the full stack:
   ```bash
   docker compose up -d --build
   ```
To create an initial administrator, also set `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` in `.env`. No administrator is created by default.
Swagger UI is available at **`http://localhost:8080/swagger-ui.html`**.

### Running Tests
- **Unit Tests** (no Docker required):
  ```bash
   mvn -f backend/pom.xml test
  ```
- **Integration Tests** (runs against real PostgreSQL container via Testcontainers):
  ```bash
   mvn -f backend/pom.xml verify
  ```

---

## 9. Interactive API Testing

An HTTP client request file is included at:
[`backend/http/municipal-api.http`](backend/http/municipal-api.http)

It can be executed directly inside IntelliJ IDEA, VS Code (REST Client extension), or converted to Postman collections.
