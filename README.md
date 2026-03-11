# PEER

![Language](https://img.shields.io/badge/Language-Java%2021-red)
![Framework](https://img.shields.io/badge/Framework-Spring%20Boot%203.4-brightgreen)
![Type](https://img.shields.io/badge/Type-REST%20API-blue)
![Status](https://img.shields.io/badge/Status-Deployed-brightgreen)

> **Work with Peer in Life**

An all-in-one platform for developers — **Scheduler + Algorithm Peer Review + Community**, built with Spring Boot 3 and deployed on AWS.

---

## Features

### 1. Scheduler
- **Calendar Events:** Title, start/end time, color, repeat rules (NONE / DAILY / WEEKLY / BIWEEKLY / MONTHLY / YEARLY), all-day toggle
- **Todos:** Subtasks via self-referencing, priority levels (LOW / MEDIUM / HIGH / URGENT), due dates, custom sort order

### 2. AlgoBank (Algorithm Peer Review)
- Register algorithm problems (EASY / MEDIUM / HARD, tags)
- Submit solutions with code, language, time/space complexity, explanation
- **Peer Evaluation** on 4 criteria (1–5 each): Correctness, Code Readability, Comments Clarity, Condition Satisfaction
- **XP System:**

| Action | XP |
|---|---|
| Submit a solution | +1 |
| Evaluate a peer | +2 |
| Receive evaluation | +avg score |

- **Level formula:** Level N requires `N² × 10` cumulative XP

### 3. Community
- Tags: `ALGORITHM` `DEVELOPMENT` `HOBBY` `IT_NEWS` `JOB_INFO` `LEARNING` `FREE` `QNA` `INQUIRY`
- 2-level comment threading — replies to nested comments auto-flatten to root
- Like/unlike, report system
- **Auto-blind:** when reports exceed `max(3, ceil(totalUsers × 0.1))`
- Admin panel: role management, inquiry resolution, blind/delete

### 4. Auth & Notifications
- Google OAuth 2.0 login
- JWT access token (30min) + refresh token (7 days, rotation on each use)
- Redis blacklist for immediate logout invalidation
- In-app notifications: comments, likes, XP earned, evaluations received

---

## Architecture

```
                    ┌──────────────────┐
                    │     Browser      │
                    │  withpeer.work   │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │  Cloudflare      │
                    │  Tunnel          │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │  nginx (:80)     │
                    │  Reverse Proxy   │
                    └───┬─────────┬────┘
                        │         │
            /api/*      │         │   /*
                        │         │
              ┌─────────▼──┐  ┌───▼──────────┐
              │ Spring Boot │  │   Next.js    │
              │   (:8080)   │  │   (:3000)    │
              └──┬──────┬───┘  │  standalone  │
                 │      │      └──────────────┘
         ┌───────▼──┐ ┌─▼──────────┐
         │ Redis 7   │ │ PostgreSQL │
         │ (Lettuce)  │ │ 16 (RDS)  │
         │ JWT cache  │ │ eu-north-1│
         └───────────┘ └───────────┘
```

### API Request Flow

```
  Request
    │
    ▼
  SecurityFilterChain
    │
    ├─ /api/auth/**  ──►  OAuth2LoginFilter  ──►  Google OAuth
    │
    ├─ JwtAuthenticationFilter
    │   ├─ Extract Bearer token
    │   ├─ Check Redis blacklist (logout?)
    │   ├─ Validate & parse claims
    │   └─ Set SecurityContext
    │
    ▼
  @RestController
    │
    ▼
  @Service (business logic + @Transactional)
    │
    ▼
  Spring Data JPA Repository + QueryDSL
    │
    ▼
  PostgreSQL (Flyway-managed schema)
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Runtime** | Java 21 |
| **Framework** | Spring Boot 3.4.3 |
| **Security** | Spring Security + OAuth2 Client + jjwt 0.12.6 |
| **ORM** | Spring Data JPA + Hibernate |
| **Query** | QueryDSL 5.1 |
| **Mapping** | MapStruct 1.6 + Lombok |
| **Database** | PostgreSQL 16 (AWS RDS, eu-north-1) |
| **Cache** | Redis 7 (Lettuce client) |
| **Migration** | Flyway (V1–V8) + `ddl-auto: validate` |
| **API Docs** | SpringDoc / Swagger UI |
| **Testing** | JUnit 5 + Mockito + H2 in-memory |
| **Deploy** | AWS EC2 (systemd) + AWS RDS |

---

## Database Schema (Flyway Migrations)

| Version | Migration | Description |
|---------|-----------|-------------|
| **V1** | `create_users_table` | Users with OAuth profile, role (USER/ADMIN), XP, level |
| **V2** | `create_events_table` | Calendar events with repeat rules, color, all-day flag |
| **V3** | `create_todos_table` | Todos with self-referencing subtasks, priority, sort order |
| **V4** | `create_notifications_table` | In-app notifications (type, message, read status, link) |
| **V5** | `create_algobank_tables` | Problems, solutions, evaluations (4-criteria scoring) |
| **V6** | `create_community_tables` | Posts, comments (2-level threading), likes, reports |
| **V7** | `add_view_count_to_posts` | View count tracking for community posts |
| **V8** | `add_resolved_to_posts` | Resolved flag for inquiry posts (admin workflow) |

---

## Key Design Decisions

### 1. Redis JWT Blacklist for Immediate Logout

Stateless JWTs cannot be revoked — the biggest drawback of pure JWT auth. On logout, the access token is stored in a Redis blacklist with its **remaining TTL** as expiry. Every request checks the blacklist before accepting the token. Refresh tokens use **rotation**: each refresh issues a new pair and invalidates the old one.

```
  Logout Request
    │
    ▼
  Store access token in Redis
  (key: "blacklist:{token}", TTL: remaining expiry)
    │
    ▼
  Delete refresh token from store
    │
  Next request with old token:
    JwtFilter → Redis.exists(token) → true → 401 Unauthorized
```

### 2. Proportional Report Threshold

A fixed report count (e.g., 3) would let a small group easily censor content in a small community. The formula `max(3, ceil(totalUsers × 0.1))` scales with community size — at least 3 reports always required, but in a 100-user community it takes 10 reports to auto-blind. This prevents abuse while still protecting users.

### 3. Quadratic XP Leveling (N² × 10)

| Level | Cumulative XP | Delta |
|-------|--------------|-------|
| 1 | 10 | 10 |
| 2 | 40 | 30 |
| 5 | 250 | 90 |
| 10 | 1,000 | 190 |
| 20 | 4,000 | 390 |

Early levels are quick (keeps new users engaged), while higher levels require progressively more effort — creating a natural sense of achievement without a hard ceiling.

### 4. Auto-Flattening 2-Level Comment Threading

Instead of rejecting deeply nested replies (bad UX) or allowing unlimited depth (layout nightmare), replies to nested comments **silently attach to the root parent**. Users see a natural conversation flow without UI depth explosion. The `parentId` always points to either `null` (root) or a root comment — never deeper.

### 5. Flyway + ddl-auto: validate

Flyway manages all schema changes through versioned migrations (V1–V8). Hibernate's `ddl-auto` is set to `validate` — it verifies the entity model matches the database schema on startup but **never modifies it**. This catches schema drift immediately and keeps migration history as the single source of truth.

---

## Local Setup

### Prerequisites

- Java 21
- Maven
- PostgreSQL 16 (or Docker)
- Redis 7 (or Docker)

### Environment Variables

```bash
# peer-backend/src/main/resources/application.yml (or .env)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/peer
SPRING_DATASOURCE_USERNAME=peer
SPRING_DATASOURCE_PASSWORD=<password>
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
GOOGLE_CLIENT_ID=<your-google-client-id>
GOOGLE_CLIENT_SECRET=<your-google-client-secret>
JWT_SECRET=<base64-encoded-secret>
```

### Run Backend

```bash
cd peer-backend
mvn spring-boot:run
# API available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
```

### Run Frontend

```bash
cd peer-frontend
npm install
npm run dev
# Available at http://localhost:3000
```

---

## Running Tests

```bash
cd peer-backend

# Run all tests (uses H2 in-memory with PostgreSQL mode)
mvn test

# Run a specific test class
mvn test -Dtest=PostServiceTest

# Run with verbose output
mvn test -X
```

Tests use **H2 in-memory database** configured in PostgreSQL compatibility mode — no external database needed. DTO creation in tests uses a reflection-based `TestUtil.createDto()` utility.

---

## AWS Deployment

### Backend

```bash
cd peer-backend
mvn package -DskipTests
# Deploy JAR to EC2
scp target/peer-backend-*.jar ec2-user@<host>:/opt/peer/peer-backend.jar
# Restart via systemd
ssh ec2-user@<host> "sudo systemctl restart peer"
```

### Frontend

```bash
cd peer-frontend
NEXT_PUBLIC_API_URL="" npx next build
# Package standalone output
cp -r .next/standalone /tmp/peer-deploy
cp -r .next/static /tmp/peer-deploy/.next/static
cp -r public /tmp/peer-deploy/public
# Deploy to EC2
tar czf peer-frontend.tar.gz -C /tmp/peer-deploy .
scp peer-frontend.tar.gz ec2-user@<host>:/tmp/
ssh ec2-user@<host> "sudo systemctl restart peer-frontend"
```

### Health Check

```bash
curl -s http://localhost:8080/actuator/health
curl -s -o /dev/null -w '%{http_code}' http://localhost:3000/
```

---

## License

This is a personal project developed by **Juhyeon Lee**.
