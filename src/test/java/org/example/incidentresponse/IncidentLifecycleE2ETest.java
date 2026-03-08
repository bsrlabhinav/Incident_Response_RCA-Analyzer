package org.example.incidentresponse;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test that:
 *  1. Creates 6 incidents and progresses them through various lifecycle stages.
 *  2. Runs Shifu reporting queries to showcase:
 *      (a) Cases by current status
 *      (b) Overall cases closed
 *      (c) Cases with SLA breach
 *      (d) Cases resolved by a particular user
 *
 * Incident matrix:
 * ┌─────┬─────────────────────────┬──────────┬──────────┬───────────────┬─────────────┐
 * │  #  │ Title                   │ Severity │ Assignee │ Final Status  │ SLA Breach? │
 * ├─────┼─────────────────────────┼──────────┼──────────┼───────────────┼─────────────┤
 * │  1  │ DB outage prod          │ CRITICAL │ Alice    │ CLOSED        │ Yes         │
 * │  2  │ Payment gateway down    │ CRITICAL │ Alice    │ CLOSED        │ Yes         │
 * │  3  │ API latency spike       │ HIGH     │ Bob      │ RCA_PENDING   │ No          │
 * │  4  │ CDN cache miss          │ HIGH     │ Bob      │ CLOSED        │ No          │
 * │  5  │ Dashboard timeout       │ MEDIUM   │ Alice    │ INVESTIGATING │ No          │
 * │  6  │ Log rotation failure    │ LOW      │ (none)   │ OPEN          │ No          │
 * └─────┴─────────────────────────┴──────────┴──────────┴───────────────┴─────────────┘
 */
@TestPropertySource(properties = {
        "incident.sla.critical.acknowledge-ms=1",
        "incident.sla.critical.resolution-ms=1"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IncidentLifecycleE2ETest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ElasticsearchClient esClient;

    private static final String REPORTER  = UUID.randomUUID().toString();
    private static final String ALICE     = UUID.randomUUID().toString();
    private static final String BOB       = UUID.randomUUID().toString();
    private static final String ADMIN     = UUID.randomUUID().toString();

    private final List<String> incidentIds = new ArrayList<>();

    // ────────────────────────────────────────────────────────────────────
    //  Phase 1 — Full Lifecycle: create, assign, transition, RCA, close
    // ────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Create 6 incidents and drive them through their lifecycle")
    void lifecycle_createAndProgressIncidents() throws Exception {

        // ── 1. Create all 6 incidents ──────────────────────────────────
        incidentIds.add(createIncident("DB outage prod",       "CRITICAL"));
        incidentIds.add(createIncident("Payment gateway down", "CRITICAL"));
        incidentIds.add(createIncident("API latency spike",    "HIGH"));
        incidentIds.add(createIncident("CDN cache miss",       "HIGH"));
        incidentIds.add(createIncident("Dashboard timeout",    "MEDIUM"));
        incidentIds.add(createIncident("Log rotation failure", "LOW"));

        assertThat(incidentIds).hasSize(6);

        // ── 2. Assign owners (all except #6 which stays unassigned) ────
        assignOwner(incidentIds.get(0), ALICE);  // INC-1 → Alice
        assignOwner(incidentIds.get(1), ALICE);  // INC-2 → Alice
        assignOwner(incidentIds.get(2), BOB);    // INC-3 → Bob
        assignOwner(incidentIds.get(3), BOB);    // INC-4 → Bob
        assignOwner(incidentIds.get(4), ALICE);  // INC-5 → Alice

        // Small delay so CRITICAL incidents (1ms SLA threshold) are definitely breached
        Thread.sleep(50);

        // ── 3. Progress INC-1 (CRITICAL) → CLOSED ─────────────────────
        transitionTo(incidentIds.get(0), "INVESTIGATING");
        transitionTo(incidentIds.get(0), "FIXED");
        transitionTo(incidentIds.get(0), "RCA_PENDING");
        recordRca(incidentIds.get(0), "CODE_BUG", "Connection pool exhaustion due to leaked connections");
        refreshIndices();
        transitionTo(incidentIds.get(0), "CLOSED");

        // ── 4. Progress INC-2 (CRITICAL) → CLOSED ─────────────────────
        transitionTo(incidentIds.get(1), "INVESTIGATING");
        transitionTo(incidentIds.get(1), "FIXED");
        transitionTo(incidentIds.get(1), "RCA_PENDING");
        recordRca(incidentIds.get(1), "DEPENDENCY_FAILURE", "Third-party payment provider certificate expired");
        refreshIndices();
        transitionTo(incidentIds.get(1), "CLOSED");

        // ── 5. Progress INC-3 (HIGH) → RCA_PENDING (stays here) ───────
        transitionTo(incidentIds.get(2), "INVESTIGATING");
        transitionTo(incidentIds.get(2), "FIXED");
        transitionTo(incidentIds.get(2), "RCA_PENDING");

        // ── 6. Progress INC-4 (HIGH) → CLOSED ─────────────────────────
        transitionTo(incidentIds.get(3), "INVESTIGATING");
        transitionTo(incidentIds.get(3), "FIXED");
        transitionTo(incidentIds.get(3), "RCA_PENDING");
        recordRca(incidentIds.get(3), "CONFIGURATION_ERROR", "CDN TTL misconfigured after last deployment");
        refreshIndices();
        transitionTo(incidentIds.get(3), "CLOSED");

        // ── 7. Progress INC-5 (MEDIUM) → INVESTIGATING (stays here) ───
        transitionTo(incidentIds.get(4), "INVESTIGATING");

        // ── 8. INC-6 (LOW) stays OPEN, no assignee ────────────────────

        // Verify final states
        assertStatus(incidentIds.get(0), "CLOSED");
        assertStatus(incidentIds.get(1), "CLOSED");
        assertStatus(incidentIds.get(2), "RCA_PENDING");
        assertStatus(incidentIds.get(3), "CLOSED");
        assertStatus(incidentIds.get(4), "INVESTIGATING");
        assertStatus(incidentIds.get(5), "OPEN");

        // Verify SLA breaches on CRITICAL incidents
        assertSlaBreach(incidentIds.get(0), true);
        assertSlaBreach(incidentIds.get(1), true);

        // Final refresh to make all data available for reporting queries
        refreshIndices();

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  LIFECYCLE COMPLETE — 6 incidents created and processed ║");
        System.out.println("║  CLOSED=3  RCA_PENDING=1  INVESTIGATING=1  OPEN=1       ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    // ────────────────────────────────────────────────────────────────────
    //  Phase 2 — Shifu Reporting Queries
    // ────────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Report A: Cases open at any point — breakdown by current status")
    void reportA_casesByCurrentStatus() throws Exception {
        Map<String, Object> request = shifuRequest("incident_summary",
                List.of("status"),
                List.of(measurement("case_count", "VALUE_COUNT", "incident_count")),
                null);

        MvcResult result = mockMvc.perform(post("/api/reports/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportType").value("incident_summary"))
                .andExpect(jsonPath("$.totalHits").value(6))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode buckets = response.get("buckets");

        Map<String, Long> statusCounts = extractDimensionCounts(buckets, "status");

        assertThat(statusCounts.get("CLOSED")).isEqualTo(3L);
        assertThat(statusCounts.get("OPEN")).isEqualTo(1L);
        assertThat(statusCounts.get("INVESTIGATING")).isEqualTo(1L);
        assertThat(statusCounts.get("RCA_PENDING")).isEqualTo(1L);

        System.out.println("\n┌──────────────────────────────────────────────────────┐");
        System.out.println("│  REPORT A: Cases by Current Status                   │");
        System.out.println("├──────────────────┬───────────────────────────────────┤");
        statusCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> System.out.printf("│  %-16s │  %d case(s)                        │%n", e.getKey(), e.getValue()));
        System.out.printf("├──────────────────┴───────────────────────────────────┤%n");
        System.out.printf("│  Total incidents: %-34d │%n", response.get("totalHits").asLong());
        System.out.println("└──────────────────────────────────────────────────────┘\n");
    }

    @Test
    @Order(3)
    @DisplayName("Report B: Overall cases closed")
    void reportB_overallCasesClosed() throws Exception {
        Map<String, Object> request = shifuRequest("incident_summary",
                null,
                List.of(measurement("closed_count", "VALUE_COUNT", "incident_count")),
                Map.of("status", List.of("CLOSED")));

        MvcResult result = mockMvc.perform(post("/api/reports/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(3))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        long totalClosed = response.get("totalHits").asLong();
        JsonNode closedCountVal = response.get("buckets").get(0).get("measurements").get("closed_count");

        assertThat(totalClosed).isEqualTo(3L);

        System.out.println("\n┌──────────────────────────────────────────────────────┐");
        System.out.println("│  REPORT B: Overall Cases Closed                      │");
        System.out.println("├──────────────────────────────────────────────────────┤");
        System.out.printf("│  Total closed incidents:  %-26s │%n", totalClosed);
        System.out.printf("│  VALUE_COUNT measurement: %-26s │%n", closedCountVal);
        System.out.println("└──────────────────────────────────────────────────────┘\n");
    }

    @Test
    @Order(4)
    @DisplayName("Report C: Cases with SLA breach (CRITICAL incidents with 1ms threshold)")
    void reportC_casesWithSlaBreach() throws Exception {
        Map<String, Object> request = shifuRequest("sla_breach",
                List.of("severity"),
                List.of(
                        measurement("breach_total", "VALUE_COUNT", "breach_count"),
                        measurement("avg_resolution", "AVG", "avg_resolution_ms")
                ),
                null);

        MvcResult result = mockMvc.perform(post("/api/reports/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportType").value("sla_breach"))
                .andExpect(jsonPath("$.totalHits").value(2))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode buckets = response.get("buckets");

        Map<String, Long> severityCounts = extractDimensionCounts(buckets, "severity");

        assertThat(severityCounts.get("CRITICAL")).isEqualTo(2L);
        assertThat(severityCounts).doesNotContainKey("HIGH");
        assertThat(severityCounts).doesNotContainKey("MEDIUM");
        assertThat(severityCounts).doesNotContainKey("LOW");

        System.out.println("\n┌──────────────────────────────────────────────────────┐");
        System.out.println("│  REPORT C: Cases with SLA Breach                     │");
        System.out.println("├──────────────────┬───────────────────────────────────┤");
        for (JsonNode bucket : buckets) {
            String severity = bucket.get("dimensions").get("severity").asText();
            long count = bucket.get("docCount").asLong();
            String avgRes = bucket.get("measurements").has("avg_resolution")
                    ? String.format("%.0f ms", bucket.get("measurements").get("avg_resolution").asDouble())
                    : "N/A";
            System.out.printf("│  %-16s │  %d breach(es), avg resolution: %s%n", severity, count, avgRes);
        }
        System.out.printf("├──────────────────┴───────────────────────────────────┤%n");
        System.out.printf("│  Total SLA-breached incidents: %-21d │%n", response.get("totalHits").asLong());
        System.out.println("│  (CRITICAL SLA threshold in test: 1ms)               │");
        System.out.println("└──────────────────────────────────────────────────────┘\n");
    }

    @Test
    @Order(5)
    @DisplayName("Report D: Cases resolved (closed) by a particular user — grouped by assignee")
    void reportD_casesResolvedByParticularUser() throws Exception {
        Map<String, Object> request = shifuRequest("incident_summary",
                List.of("assignee"),
                List.of(measurement("resolved_count", "VALUE_COUNT", "incident_count")),
                Map.of("status", List.of("CLOSED")));

        MvcResult result = mockMvc.perform(post("/api/reports/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(3))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode buckets = response.get("buckets");

        Map<String, Long> assigneeCounts = extractDimensionCounts(buckets, "assignee");

        assertThat(assigneeCounts.get(ALICE)).isEqualTo(2L);
        assertThat(assigneeCounts.get(BOB)).isEqualTo(1L);

        System.out.println("\n┌──────────────────────────────────────────────────────┐");
        System.out.println("│  REPORT D: Cases Resolved by User                    │");
        System.out.println("├──────────────────────────────────────────────────────┤");
        for (JsonNode bucket : buckets) {
            String assignee = bucket.get("dimensions").get("assignee").asText();
            long count = bucket.get("docCount").asLong();
            String label = assignee.equals(ALICE) ? "Alice" : assignee.equals(BOB) ? "Bob" : assignee;
            System.out.printf("│  %-12s (%-8s…) :  %d case(s) closed            │%n",
                    label, assignee.substring(0, 8), count);
        }
        System.out.printf("├──────────────────────────────────────────────────────┤%n");
        System.out.printf("│  Total closed incidents: %-27d │%n", response.get("totalHits").asLong());
        System.out.println("└──────────────────────────────────────────────────────┘\n");
    }

    // ────────────────────────────────────────────────────────────────────
    //  Phase 3 — Bonus: verify audit trail is recorded
    // ────────────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("Audit trail recorded for all state transitions")
    void auditTrailIsRecorded() throws Exception {
        refreshIndices();

        // INC-1 went through OPEN→INVESTIGATING→FIXED→RCA_PENDING→CLOSED = 4 status transitions
        // plus 1 assignee change = at least 5 audit entries
        MvcResult result = mockMvc.perform(get("/api/incidents/{id}/audit", incidentIds.get(0)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode audits = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(audits.size()).isGreaterThanOrEqualTo(5);

        System.out.println("\n┌──────────────────────────────────────────────────────┐");
        System.out.println("│  AUDIT TRAIL for INC-1 (DB outage prod)              │");
        System.out.println("├──────────────────────────────────────────────────────┤");
        for (JsonNode audit : audits) {
            System.out.printf("│  %-12s : %-10s → %-10s  (by %s…)%n",
                    audit.get("field").asText(),
                    truncate(audit.get("oldValue").asText(), 10),
                    truncate(audit.get("newValue").asText(), 10),
                    audit.get("userId").asText().substring(0, 8));
        }
        System.out.println("└──────────────────────────────────────────────────────┘\n");
    }

    // ════════════════════════════════════════════════════════════════════
    //  Helper methods
    // ════════════════════════════════════════════════════════════════════

    private String createIncident(String title, String severity) throws Exception {
        Map<String, Object> body = Map.of(
                "title", title,
                "description", "E2E test incident: " + title,
                "severity", severity,
                "reporterId", REPORTER);

        MvcResult result = mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
        System.out.printf("  ✓ Created [%s] %s (%s)%n", severity, title, id.substring(0, 8) + "…");
        return id;
    }

    private void assignOwner(String incidentId, String assigneeId) throws Exception {
        Map<String, Object> body = Map.of(
                "assigneeId", assigneeId,
                "userId", ADMIN);

        mockMvc.perform(put("/api/incidents/{id}/owner", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(assigneeId));
    }

    private void transitionTo(String incidentId, String targetStatus) throws Exception {
        Map<String, Object> body = Map.of(
                "status", targetStatus,
                "userId", ADMIN);

        mockMvc.perform(put("/api/incidents/{id}/status", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(targetStatus));
    }

    private void recordRca(String incidentId, String category, String summary) throws Exception {
        Map<String, Object> body = Map.of(
                "category", category,
                "summary", summary,
                "details", "Root cause analysis for E2E test",
                "actionItems", List.of("Fix the issue", "Add monitoring"),
                "createdBy", ADMIN);

        mockMvc.perform(post("/api/incidents/{id}/rca", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    private void assertStatus(String incidentId, String expectedStatus) throws Exception {
        mockMvc.perform(get("/api/incidents/{id}", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }

    private void assertSlaBreach(String incidentId, boolean expectBreach) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/incidents/{id}/sla", incidentId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode sla = objectMapper.readTree(result.getResponse().getContentAsString());
        boolean ackBreach = sla.get("acknowledgeSlaBreached").asBoolean();
        boolean resBreach = sla.get("resolutionSlaBreached").asBoolean();

        if (expectBreach) {
            assertThat(ackBreach || resBreach)
                    .as("Expected SLA breach for incident " + incidentId)
                    .isTrue();
        }
    }

    private void refreshIndices() throws Exception {
        esClient.indices().refresh(r -> r.index("incidents", "root_cause_analyses", "incident_audits"));
    }

    /**
     * Builds a ShifuRequest map for JSON serialization.
     */
    private Map<String, Object> shifuRequest(String reportType,
                                              List<String> dimensions,
                                              List<Map<String, String>> measurements,
                                              Map<String, List<String>> filters) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("reportType", reportType);
        req.put("timeRange", Map.of(
                "from", Instant.now().minusSeconds(86400).toString(),
                "to", Instant.now().plusSeconds(86400).toString()));
        req.put("dimensions", dimensions);
        req.put("measurements", measurements);
        if (filters != null) {
            req.put("filters", filters);
        }
        return req;
    }

    private Map<String, String> measurement(String name, String type, String field) {
        return Map.of("name", name, "type", type, "field", field);
    }

    /**
     * Extracts dimension value → docCount from Shifu buckets.
     */
    private Map<String, Long> extractDimensionCounts(JsonNode buckets, String dimensionName) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (JsonNode bucket : buckets) {
            String key = bucket.get("dimensions").get(dimensionName).asText();
            long docCount = bucket.get("docCount").asLong();
            counts.put(key, docCount);
        }
        return counts;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "null";
        return value.length() > maxLen ? value.substring(0, maxLen) + "…" : value;
    }
}
