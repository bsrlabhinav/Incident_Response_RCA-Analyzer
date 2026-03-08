#!/usr/bin/env bash
#
# Seeds the incident database with realistic data:
# - 10 incidents across all severities
# - 3 different assignees (Alice, Bob, Charlie)
# - SLA breaches on CRITICAL and HIGH incidents (via deliberate delays)
# - Cases closed by different users
# - RCA recorded for closed incidents
#
set -euo pipefail
API="http://localhost:8080/api"

create_user() {
  local name="$1" email="$2" role="${3:-ENGINEER}"
  curl -sf -X POST "$API/users" \
    -H 'Content-Type: application/json' \
    -d "{\"displayName\":\"$name\",\"email\":\"$email\",\"role\":\"$role\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])"
}

# Ensure indices exist (they may have been deleted by --clean)
for idx in incidents incident_audits root_cause_analyses incident_evidence users; do
  curl -sf -X PUT "http://localhost:9200/$idx" >/dev/null 2>&1 || true
done

echo "=== Seeding Users ==="
ALICE=$(create_user "Alice Chen" "alice@company.com" "SRE")
echo "  Alice  → $ALICE"
BOB=$(create_user "Bob Kumar" "bob@company.com" "ENGINEER")
echo "  Bob    → $BOB"
CHARLIE=$(create_user "Charlie Park" "charlie@company.com" "ONCALL")
echo "  Charlie→ $CHARLIE"
REPORTER=$(create_user "Diana Ops" "diana@company.com" "MANAGER")
echo "  Diana  → $REPORTER (reporter)"
echo ""

create() {
  local title="$1" severity="$2"
  local id
  id=$(curl -sf -X POST "$API/incidents" \
    -H 'Content-Type: application/json' \
    -d "{\"title\":\"$title\",\"description\":\"Seeded: $title\",\"severity\":\"$severity\",\"reporterId\":\"$REPORTER\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
  echo "$id"
}

assign() { curl -sf -X PUT "$API/incidents/$1/owner" -H 'Content-Type: application/json' -d "{\"assigneeId\":\"$2\",\"userId\":\"$2\"}" >/dev/null; }
transition() { curl -sf -X PUT "$API/incidents/$1/status" -H 'Content-Type: application/json' -d "{\"status\":\"$2\",\"userId\":\"$3\"}" >/dev/null; }
rca() { curl -sf -X POST "$API/incidents/$1/rca" -H 'Content-Type: application/json' -d "{\"category\":\"$2\",\"summary\":\"$3\",\"details\":\"\",\"actionItems\":[],\"createdBy\":\"$4\"}" >/dev/null; }

echo "=== Seeding Incident Data ==="
echo ""

# ── CRITICAL incidents (SLA threshold: ack=5s, res=10s) ────────────

echo "Creating CRITICAL incidents (will breach SLA)…"
INC1=$(create "Production database cluster failover" "CRITICAL")
echo "  [1] $INC1 — CRITICAL — DB cluster failover"

INC2=$(create "Payment processing complete outage" "CRITICAL")
echo "  [2] $INC2 — CRITICAL — Payment outage"

INC3=$(create "Auth service returning 500 for all users" "CRITICAL")
echo "  [3] $INC3 — CRITICAL — Auth service down"

# Assign immediately but wait >5s before acknowledging → ack breach
assign "$INC1" "$ALICE"
assign "$INC2" "$BOB"
assign "$INC3" "$CHARLIE"

echo "  Waiting 7 seconds for CRITICAL SLA breach (ack threshold=5s)…"
sleep 7

# INC1: Close fully (Alice) — will breach both ack + resolution SLA
transition "$INC1" "INVESTIGATING" "$ALICE"
transition "$INC1" "FIXED" "$ALICE"
transition "$INC1" "RCA_PENDING" "$ALICE"
rca "$INC1" "INFRASTRUCTURE_FAILURE" "Primary DB node ran out of disk space causing failover" "$ALICE"
sleep 1
transition "$INC1" "CLOSED" "$ALICE"
echo "  [1] CLOSED by Alice — SLA breached (ack+res)"

# INC2: Close fully (Bob) — breaches ack, resolution depends on timing
transition "$INC2" "INVESTIGATING" "$BOB"
sleep 5
transition "$INC2" "FIXED" "$BOB"
transition "$INC2" "RCA_PENDING" "$BOB"
rca "$INC2" "DEPENDENCY_FAILURE" "Payment gateway provider had regional outage" "$BOB"
sleep 1
transition "$INC2" "CLOSED" "$BOB"
echo "  [2] CLOSED by Bob — SLA breached (ack+res)"

# INC3: Leave in INVESTIGATING (Charlie) — breaches ack only
transition "$INC3" "INVESTIGATING" "$CHARLIE"
echo "  [3] INVESTIGATING by Charlie — SLA breached (ack only)"

# ── HIGH incidents (SLA threshold: ack=30s, res=60s) ──────────────

echo ""
echo "Creating HIGH incidents…"
INC4=$(create "API gateway 50th percentile latency >2s" "HIGH")
echo "  [4] $INC4 — HIGH — API latency spike"

INC5=$(create "CDN serving stale content after deploy" "HIGH")
echo "  [5] $INC5 — HIGH — CDN stale content"

INC6=$(create "Kubernetes pod CrashLoopBackOff in prod" "HIGH")
echo "  [6] $INC6 — HIGH — K8s crash loop"

assign "$INC4" "$ALICE"
assign "$INC5" "$BOB"
assign "$INC6" "$CHARLIE"

# INC4: Close fast (Alice) — no breach
transition "$INC4" "INVESTIGATING" "$ALICE"
transition "$INC4" "FIXED" "$ALICE"
transition "$INC4" "RCA_PENDING" "$ALICE"
rca "$INC4" "CODE_BUG" "N+1 query in order history endpoint" "$ALICE"
sleep 1
transition "$INC4" "CLOSED" "$ALICE"
echo "  [4] CLOSED by Alice — no SLA breach"

# INC5: Close (Charlie) — no breach
transition "$INC5" "INVESTIGATING" "$BOB"
transition "$INC5" "FIXED" "$BOB"
transition "$INC5" "RCA_PENDING" "$BOB"
rca "$INC5" "CONFIGURATION_ERROR" "CDN cache invalidation rule was missing for new paths" "$CHARLIE"
sleep 1
transition "$INC5" "CLOSED" "$CHARLIE"
echo "  [5] CLOSED by Charlie — no SLA breach"

# INC6: RCA_PENDING (still open)
transition "$INC6" "INVESTIGATING" "$CHARLIE"
transition "$INC6" "FIXED" "$CHARLIE"
transition "$INC6" "RCA_PENDING" "$CHARLIE"
echo "  [6] RCA_PENDING by Charlie — awaiting RCA"

# ── MEDIUM incidents ──────────────────────────────────────────────

echo ""
echo "Creating MEDIUM incidents…"
INC7=$(create "Elevated error rate on search microservice" "MEDIUM")
echo "  [7] $INC7 — MEDIUM — Search errors"

INC8=$(create "Monitoring dashboard showing gaps" "MEDIUM")
echo "  [8] $INC8 — MEDIUM — Monitoring gaps"

assign "$INC7" "$BOB"
assign "$INC8" "$ALICE"

transition "$INC7" "INVESTIGATING" "$BOB"
echo "  [7] INVESTIGATING by Bob"

transition "$INC8" "INVESTIGATING" "$ALICE"
transition "$INC8" "FIXED" "$ALICE"
transition "$INC8" "RCA_PENDING" "$ALICE"
rca "$INC8" "MONITORING_GAP" "Prometheus scrape interval was too wide for bursty metrics" "$ALICE"
sleep 1
transition "$INC8" "CLOSED" "$ALICE"
echo "  [8] CLOSED by Alice — no SLA breach"

# ── LOW incidents ─────────────────────────────────────────────────

echo ""
echo "Creating LOW incidents…"
INC9=$(create "Documentation links broken on internal wiki" "LOW")
echo "  [9] $INC9 — LOW — Broken docs links"

INC10=$(create "Non-critical cron job logging excessive warnings" "LOW")
echo "  [10] $INC10 — LOW — Noisy cron logs"

assign "$INC9" "$CHARLIE"
# INC10 stays unassigned, OPEN

transition "$INC9" "INVESTIGATING" "$CHARLIE"
transition "$INC9" "FIXED" "$CHARLIE"
transition "$INC9" "RCA_PENDING" "$CHARLIE"
rca "$INC9" "PROCESS_FAILURE" "Wiki migration script did not update cross-reference links" "$CHARLIE"
sleep 1
transition "$INC9" "CLOSED" "$CHARLIE"
echo "  [9] CLOSED by Charlie"
echo "  [10] Still OPEN — unassigned"

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║  SEED COMPLETE — 10 incidents created                     ║"
echo "╠════════════════════════════════════════════════════════════╣"
echo "║  CLOSED: 6  (Alice=3, Bob=1, Charlie=2)                  ║"
echo "║  INVESTIGATING: 2  (Bob=1, Charlie=1)                    ║"
echo "║  RCA_PENDING: 1  (Charlie)                               ║"
echo "║  OPEN: 1  (unassigned)                                   ║"
echo "║                                                           ║"
echo "║  SLA BREACHES:                                            ║"
echo "║    INC-1 CRITICAL  ack ✗  res ✗  (Alice)                 ║"
echo "║    INC-2 CRITICAL  ack ✗  res ✗  (Bob)                   ║"
echo "║    INC-3 CRITICAL  ack ✗        (Charlie, still open)    ║"
echo "╚════════════════════════════════════════════════════════════╝"
