#!/bin/bash
set -e

PEER_DIR="/home/juhyeonl/workspace/PEER"
ENV_FILE="$PEER_DIR/.env.local"

echo "=== PEER Deploy & Start Script ==="
echo ""

# Parse arguments
BUILD_BACKEND=false
BUILD_FRONTEND=false
START_ONLY=false

case "${1:-all}" in
    backend)  BUILD_BACKEND=true ;;
    frontend) BUILD_FRONTEND=true ;;
    all)      BUILD_BACKEND=true; BUILD_FRONTEND=true ;;
    start)    START_ONLY=true ;;
    *)
        echo "Usage: ./deploy-peer.sh [backend|frontend|all|start]"
        echo ""
        echo "  backend   - Build backend, then start all services"
        echo "  frontend  - Build frontend, then start all services"
        echo "  all       - Build both, then start all services (default)"
        echo "  start     - Skip build, just start all services"
        exit 1
        ;;
esac

# --- Step 1: Build ---

if [ "$START_ONLY" = false ]; then
    if [ "$BUILD_BACKEND" = true ]; then
        echo "[Build] Backend..."
        cd "$PEER_DIR/peer-backend"
        mvn package -DskipTests -q
        echo "[Build] Backend JAR ready."
    fi

    if [ "$BUILD_FRONTEND" = true ]; then
        echo "[Build] Frontend..."
        cd "$PEER_DIR/peer-frontend"
        NEXT_PUBLIC_API_URL="" npx next build
        cp -r .next/static .next/standalone/.next/static
        cp -r public .next/standalone/public
        echo "[Build] Frontend ready."
    fi

    echo ""
fi

# --- Step 2: Stop existing services ---

echo "[Stop] Killing existing services..."
pkill -f 'java.*peer-backend' 2>/dev/null || true
pkill -f 'node.*server.js' 2>/dev/null || true
sleep 2

# --- Step 3: Start PostgreSQL & Redis if not running ---

echo "[Check] PostgreSQL..."
if ! pg_isready -q 2>/dev/null; then
    echo "[Start] Starting PostgreSQL..."
    sudo service postgresql start
    sleep 2
fi
echo "[Check] PostgreSQL OK."

echo "[Check] Redis..."
if ! redis-cli ping &>/dev/null; then
    echo "[Start] Starting Redis..."
    sudo service redis-server start
    sleep 1
fi
echo "[Check] Redis OK."

# --- Step 4: Start Nginx ---

echo "[Check] Nginx..."
if ! pgrep -x nginx &>/dev/null; then
    echo "[Start] Starting Nginx..."
    sudo nginx
else
    echo "[Start] Reloading Nginx..."
    sudo nginx -s reload
fi
echo "[Check] Nginx OK."

# --- Step 5: Start Backend ---

echo "[Start] Backend..."
cd "$PEER_DIR"
set -a && source "$ENV_FILE" && set +a
cd peer-backend
nohup java -jar target/peer-backend-0.0.1-SNAPSHOT.jar > /tmp/peer-backend.log 2>&1 &
BACKEND_PID=$!
echo "[Start] Backend PID: $BACKEND_PID"

# --- Step 6: Start Frontend ---

echo "[Start] Frontend..."
cd "$PEER_DIR/peer-frontend"
HOSTNAME=0.0.0.0 PORT=3000 nohup node .next/standalone/server.js > /tmp/peer-frontend.log 2>&1 &
FRONTEND_PID=$!
echo "[Start] Frontend PID: $FRONTEND_PID"

# --- Step 7: Health check ---

echo ""
echo "[Health] Waiting for backend to start..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/actuator/health | grep -q '"UP"' 2>/dev/null; then
        echo "[Health] Backend: UP"
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo "[Health] Backend: TIMEOUT (check /tmp/peer-backend.log)"
    fi
    sleep 1
done

FRONTEND_STATUS=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:3000/)
echo "[Health] Frontend: $FRONTEND_STATUS"

# --- Step 8: Cloudflare Named Tunnel ---

echo ""
echo "[Tunnel] Checking Cloudflare Named Tunnel (systemd service)..."
if systemctl is-active --quiet cloudflared; then
    echo "[Tunnel] cloudflared: RUNNING"
else
    echo "[Tunnel] cloudflared: NOT RUNNING - starting..."
    sudo systemctl start cloudflared
    sleep 2
    if systemctl is-active --quiet cloudflared; then
        echo "[Tunnel] cloudflared: STARTED"
    else
        echo "[Tunnel] cloudflared: FAILED (check: sudo systemctl status cloudflared)"
    fi
fi

echo ""
echo "=== PEER is running ==="
echo "  Local:  http://localhost:80"
echo "  Public: https://withpeer.work"
echo ""
