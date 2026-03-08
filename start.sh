#!/usr/bin/env bash
#
# One-command startup for the Incident Response Platform.
# Starts Elasticsearch, backend, frontend, and optionally seeds data.
#
# Usage:
#   ./start.sh           # Full startup with seed data
#   ./start.sh --no-seed # Skip seeding
#   ./start.sh --clean   # Wipe all data and re-seed
#

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
UI_DIR="$ROOT_DIR/incident-response-ui"
SEED="${SEED:-true}"
CLEAN="${CLEAN:-false}"

for arg in "$@"; do
  case "$arg" in
    --no-seed) SEED="false" ;;
    --clean)   CLEAN="true" ;;
  esac
done

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[start]${NC} $1"; }
ok()   { echo -e "${GREEN}  ✓${NC} $1"; }
warn() { echo -e "${YELLOW}  ⚠${NC} $1"; }
fail() { echo -e "${RED}  ✗${NC} $1"; exit 1; }

cleanup() {
  log "Shutting down..."
  [ -n "${BACKEND_PID:-}" ] && kill "$BACKEND_PID" 2>/dev/null
  [ -n "${FRONTEND_PID:-}" ] && kill "$FRONTEND_PID" 2>/dev/null
  wait 2>/dev/null
  log "Done."
}
trap cleanup EXIT INT TERM

# ── 1. Elasticsearch ─────────────────────────────────────────────

log "Checking Elasticsearch on port 9200..."

if curl -sf http://localhost:9200/_cluster/health >/dev/null 2>&1; then
  ok "Elasticsearch already running"
else
  log "Starting Elasticsearch via Docker..."

  if docker ps -a --format '{{.Names}}' | grep -q '^incident-es$'; then
    docker start incident-es >/dev/null 2>&1
  else
    docker run -d --name incident-es \
      -p 9200:9200 \
      -e "discovery.type=single-node" \
      -e "xpack.security.enabled=false" \
      -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
      docker.elastic.co/elasticsearch/elasticsearch:8.11.4 >/dev/null 2>&1 \
      || fail "Could not start Elasticsearch. Is Docker running?"
  fi

  log "Waiting for Elasticsearch to be ready..."
  for i in $(seq 1 30); do
    if curl -sf http://localhost:9200/_cluster/health >/dev/null 2>&1; then
      ok "Elasticsearch ready (took ~${i}s)"
      break
    fi
    [ "$i" -eq 30 ] && fail "Elasticsearch did not start within 30s"
    sleep 1
  done
fi

# ── 2. Clean data (optional) ─────────────────────────────────────

if [ "$CLEAN" = "true" ]; then
  log "Cleaning existing data..."
  for idx in incidents incident_audits root_cause_analyses incident_evidence users; do
    curl -sf -X DELETE "http://localhost:9200/$idx" >/dev/null 2>&1 || true
  done
  ok "All indices cleared"
  sleep 1
fi

# ── 3. Backend ────────────────────────────────────────────────────

log "Checking backend on port 8080..."

if curl -sf http://localhost:8080/api/incidents?page=0\&size=1 >/dev/null 2>&1; then
  ok "Backend already running"
else
  log "Building and starting Spring Boot backend..."

  if lsof -i :8080 -P >/dev/null 2>&1; then
    warn "Port 8080 is occupied. Attempting to free it..."
    lsof -ti :8080 | xargs kill -9 2>/dev/null
    sleep 2
  fi

  cd "$ROOT_DIR"
  ./gradlew bootRun > /tmp/incident-backend.log 2>&1 &
  BACKEND_PID=$!

  log "Waiting for backend to start (PID $BACKEND_PID)..."
  for i in $(seq 1 60); do
    if curl -sf "http://localhost:8080/api/incidents?page=0&size=1" >/dev/null 2>&1; then
      ok "Backend ready on :8080 (took ~${i}s)"
      break
    fi
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
      fail "Backend process died. Check /tmp/incident-backend.log"
    fi
    [ "$i" -eq 60 ] && fail "Backend did not start within 60s. Check /tmp/incident-backend.log"
    sleep 1
  done
fi

# ── 4. Frontend ───────────────────────────────────────────────────

log "Checking frontend on port 3000..."

if curl -sf http://localhost:3000 >/dev/null 2>&1; then
  ok "Frontend already running"
else
  log "Installing frontend dependencies..."
  cd "$UI_DIR"
  npm install --silent 2>/dev/null
  ok "Dependencies installed"

  log "Starting Vite dev server..."
  npm run dev > /tmp/incident-frontend.log 2>&1 &
  FRONTEND_PID=$!

  for i in $(seq 1 20); do
    if curl -sf http://localhost:3000 >/dev/null 2>&1; then
      ok "Frontend ready on :3000 (took ~${i}s)"
      break
    fi
    [ "$i" -eq 20 ] && fail "Frontend did not start within 20s"
    sleep 1
  done
fi

# ── 5. Seed data ─────────────────────────────────────────────────

if [ "$SEED" = "true" ]; then
  EXISTING=$(curl -sf "http://localhost:8080/api/incidents?page=0&size=1" 2>/dev/null \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('totalElements',0))" 2>/dev/null || echo "0")

  if [ "$EXISTING" -gt 0 ] && [ "$CLEAN" != "true" ]; then
    ok "Database has $EXISTING incidents — skipping seed (use --clean to reset)"
  else
    log "Seeding database with test data..."
    cd "$UI_DIR"
    bash seed-data.sh
  fi
fi

# ── 6. Ready ──────────────────────────────────────────────────────

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  Incident Response Platform is running!                  ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║                                                          ║${NC}"
echo -e "${GREEN}║  UI:            ${CYAN}http://localhost:3000${GREEN}                  ║${NC}"
echo -e "${GREEN}║  Elasticsearch: ${CYAN}http://localhost:9200${GREEN}                  ║${NC}"
echo -e "${GREEN}║                                                          ║${NC}"
echo -e "${GREEN}║  Pages:                                                  ║${NC}"
echo -e "${GREEN}║    ${NC}/incidents${GREEN}       Incident list                       ║${NC}"
echo -e "${GREEN}║    ${NC}/incidents/new${GREEN}   Create new incident                 ║${NC}"
echo -e "${GREEN}║    ${NC}/reports${GREEN}         Shifu reporting dashboard            ║${NC}"
echo -e "${GREEN}║    ${NC}/database${GREEN}        Raw ES document explorer             ║${NC}"
echo -e "${GREEN}║                                                          ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "Press ${YELLOW}Ctrl+C${NC} to stop all services."
echo ""

wait
