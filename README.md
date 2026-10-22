# Fitness Addicted Gym

An AI-powered gym management system built with **Java 21, Spring Boot and Spring AI**. It combines the
everyday operations of a gym (members, memberships, attendance, classes, payments) with a
**RAG-based assistant** that answers questions from the gym's own documents and live data, running
on **local open models through Ollama**.

> Status: **Phase 0 complete** (architecture, schema, project skeleton). See the [roadmap](#roadmap).

## Why this project

Gym owners answer the same questions all day (plans, timings, policies) and chase renewals by hand.
This system automates the routine work and gives members an assistant that is grounded in the gym's
real policies and schedule, instead of guessing.

## Features (planned)

- Role-based access: Member, Trainer, Staff, Admin
- Membership plans, subscriptions, freeze / renew / expiry reminders
- QR or front-desk check-in and live occupancy
- Class schedule, booking, capacity and waitlist
- Payments (Razorpay test mode) and invoices
- Trainer workflows: assignments, workout and diet plans with AI-assisted drafts
- **AI assistant (RAG):** policy / plan / FAQ answers with cited sources, plus read-only live lookups
  (class availability, "when does my plan expire?") through tool calling
- Admin dashboard: revenue, active members, churn, peak hours
- Privacy by design: audit log, purpose-specific consent, health data never sent to the LLM

## Architecture

Modular monolith: one Spring Boot app, one PostgreSQL database (with pgvector), strict module boundaries.

```mermaid
flowchart LR
    Clients[Member / Trainer / Staff / Admin portals] --> API[Spring Boot REST API + JWT]
    API --> Core[Core modules: members, memberships, attendance, bookings, payments]
    API --> AI[AI module: RAG + tools]
    Core --> PG[(PostgreSQL + pgvector)]
    AI --> PG
    AI --> OL[[Ollama: chat + embeddings]]
```

Details: [docs/architecture.md](docs/architecture.md) | [docs/er-diagram.md](docs/er-diagram.md)

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA |
| AI | Spring AI 1.1, Ollama (`llama3.2`, `nomic-embed-text`), PgVectorStore |
| Database | PostgreSQL 16 + pgvector, Flyway migrations |
| Frontend | React + TypeScript (Vite), planned |
| DevOps | Docker, Docker Compose, GitHub Actions |

## Quick start (local)

Prerequisites: JDK 21, Maven 3.9+, Docker.

```bash
# 1. Configure
cp .env.example .env

# 2. Start PostgreSQL + pgvector (and optionally Ollama in Docker)
docker compose up -d db
# or, to run Ollama in Docker too (pulls the models once):
docker compose --profile ollama up -d

# 3. If you run Ollama natively instead, pull the models yourself
ollama pull llama3.2
ollama pull nomic-embed-text

# 4. Run the backend
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Then open:

- Health: http://localhost:8080/actuator/health
- API docs: http://localhost:8080/swagger-ui.html

Flyway creates the schema on first start (`V1` core, `V2` AI tables, `V3` sample plans).

## Roadmap

- [x] **Phase 0:** architecture docs, ER diagram, schema v1, project skeleton, Docker Compose
- [ ] **Phase 1:** auth + roles, members, plans, subscriptions, attendance, React shell
- [ ] **Phase 2:** classes and bookings, payments, notifications, trainer plans
- [ ] **Phase 3:** document ingestion, RAG chat, tool calling, guardrails, evaluation set
- [ ] **Phase 4:** CI/CD, observability, backups, hosting

## Project structure

```
fitness-addicted-gym-rag/
├── backend/            Spring Boot application (modules under com.fitnessaddicted.gym)
├── frontend/           React app (Phase 1)
├── docs/               Architecture and ER diagram
├── .github/workflows/  CI
├── docker-compose.yml  PostgreSQL + pgvector, optional Ollama
└── .env.example
```

## License

To be decided (MIT is a common choice for portfolio projects).
