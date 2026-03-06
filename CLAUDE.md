# PEER Project

Hive Helsinki (42 Network) developer productivity platform.

## Quick Reference
- GitHub: git@github.com:Hive-juhyeonl/PEER.git (branch: main)
- Domain: withpeer.work (Cloudflare Tunnel, systemd service)
- EC2: 51.20.114.75 (t3.micro, Amazon Linux 2023)
- SSH: `ssh -i peer-key.pem -o StrictHostKeyChecking=no ec2-user@51.20.114.75`
- RDS: peer-db.c7aa4uuk8lra.eu-north-1.rds.amazonaws.com (PostgreSQL, DB: peer, User: peer)
- Admin account: xx.juon@gmail.com (id=1, ROLE_ADMIN)

## Tech Stack
- Backend: Spring Boot 3 + Java 21 + JPA/Hibernate + PostgreSQL (RDS) + Redis
- Frontend: Next.js 16 (standalone output) + TailwindCSS
- Auth: Google OAuth 2.0 + JWT (role claim: ROLE_USER / ROLE_ADMIN)
- Tests: H2 in-memory (MODE=PostgreSQL), 26 integration tests
- DB migrations: Flyway (V1-V7), ddl-auto=validate

## Project Structure
### Backend (peer-backend/)
- com.peer.user - User entity (name, email, profileImageUrl, role, totalXp, level), UserController (GET/PUT /api/users/me)
- com.peer.community - Post (title, content, tag, likeCount, viewCount, reportCount, blinded), Comment, PostTag enum
- com.peer.algobank - Problem, Solution, Evaluation (code review + XP system)
- com.peer.scheduler - Event (calendar), Todo (subtasks)
- com.peer.admin - AdminController/Service (ADMIN only, @PreAuthorize)
- com.peer.security - JWT auth, Google OAuth2, SecurityConfig
- com.peer.notification - Notification system

### Frontend (peer-frontend/)
- /scheduler, /todos, /algobank, /community, /inquiry, /admin, /profile, /notifications
- AuthContext provides: user, loading, login, logout, refreshUser
- api client: src/lib/api.ts (auto token refresh)
- Types: src/types/index.ts

## Conventions
- DTO: @Getter + @NoArgsConstructor only (no setters)
- Tests: TestUtil.createDto() for DTO creation (reflection)
- Lazy loading tests: em.flush(); em.clear();
- Frontend build: NEXT_PUBLIC_API_URL="" (relative paths, nginx proxies)
- PostTag: ALGORITHM, DEVELOPMENT, HOBBY, IT_NEWS, JOB_INFO, LEARNING, FREE, QNA, INQUIRY
- INQUIRY tag: used by /inquiry page (user's own only), managed in /admin Inquiries tab

## Deploy Commands
### Backend
```bash
cd peer-backend && mvn package -DskipTests
scp -i peer-key.pem target/peer-backend-0.0.1-SNAPSHOT.jar ec2-user@51.20.114.75:/opt/peer/peer.jar
# On EC2:
ssh -i peer-key.pem -o StrictHostKeyChecking=no ec2-user@51.20.114.75
pkill -f 'java.*peer-backend.jar' || true
sleep 2 && cp /opt/peer/peer.jar /opt/peer/peer-backend.jar
set -a && source /opt/peer/.env && set +a
nohup /usr/bin/java -jar -Dspring.profiles.active=prod /opt/peer/peer-backend.jar > /opt/peer/backend.log 2>&1 & disown
```
IMPORTANT: Must `source /opt/peer/.env` before starting - env vars are NOT auto-loaded.

### Frontend
```bash
cd peer-frontend && NEXT_PUBLIC_API_URL="" npx next build
rm -rf /tmp/peer-deploy && mkdir -p /tmp/peer-deploy/.next
cp -r .next/standalone/* /tmp/peer-deploy/
cp -r .next/static /tmp/peer-deploy/.next/static
cp -r public /tmp/peer-deploy/public
cd /tmp && tar czf peer-frontend.tar.gz -C peer-deploy .
scp -i peer-key.pem /tmp/peer-frontend.tar.gz ec2-user@51.20.114.75:/tmp/
# On EC2:
sudo fuser -k 3000/tcp
sudo rm -rf /opt/peer-frontend/*
sudo tar xzf /tmp/peer-frontend.tar.gz -C /opt/peer-frontend/
cd /opt/peer-frontend && sudo HOSTNAME=0.0.0.0 PORT=3000 nohup node server.js > /tmp/frontend.log 2>&1 & disown
```

## Health Check
```bash
ssh -i peer-key.pem -o StrictHostKeyChecking=no ec2-user@51.20.114.75 "curl -s http://localhost:8080/actuator/health"
ssh -i peer-key.pem -o StrictHostKeyChecking=no ec2-user@51.20.114.75 "curl -s -o /dev/null -w '%{http_code}' http://localhost:3000/"
```
