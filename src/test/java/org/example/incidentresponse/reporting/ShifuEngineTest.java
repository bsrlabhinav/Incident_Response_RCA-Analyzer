package org.example.incidentresponse.reporting;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.example.incidentresponse.reporting.definitions.IncidentSummaryReport;
import org.example.incidentresponse.reporting.definitions.SlaBreachReport;
import org.example.incidentresponse.reporting.definitions.RcaCategoryReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ShifuEngineTest {

    @Mock private ElasticsearchClient esClient;

    private ShifuEngine engine;

    @BeforeEach
    void setUp() {
        ReportRegistry registry = new ReportRegistry(List.of(
                new IncidentSummaryReport(),
                new SlaBreachReport(),
                new RcaCategoryReport()
        ));
        ESSearchQueryAdapter adapter = new ESSearchQueryAdapter();
        engine = new ShifuEngine(registry, adapter, esClient);
    }

    @Test
    @DisplayName("Unknown report type throws IllegalArgumentException")
    void unknownReportType_throws() {
        ShifuRequest request = new ShifuRequest(
                "nonexistent_report",
                new TimeRange(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()),
                List.of(),
                List.of(new MeasurementRequest("count", MeasurementType.VALUE_COUNT, "_id")),
                null, null);

        assertThatThrownBy(() -> engine.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown report type");
    }

    @Test
    @DisplayName("Invalid dimension throws IllegalArgumentException")
    void invalidDimension_throws() {
        ShifuRequest request = new ShifuRequest(
                "incident_summary",
                new TimeRange(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()),
                List.of("nonexistent_dim"),
                List.of(new MeasurementRequest("count", MeasurementType.VALUE_COUNT, "_id")),
                null, null);

        assertThatThrownBy(() -> engine.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("Invalid measurement field throws IllegalArgumentException")
    void invalidMeasurementField_throws() {
        ShifuRequest request = new ShifuRequest(
                "incident_summary",
                new TimeRange(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()),
                List.of(),
                List.of(new MeasurementRequest("bad", MeasurementType.AVG, "nonexistent_field")),
                null, null);

        assertThatThrownBy(() -> engine.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("ReportRegistry discovers all three report types")
    void registryDiscovery() {
        ReportRegistry registry = new ReportRegistry(List.of(
                new IncidentSummaryReport(),
                new SlaBreachReport(),
                new RcaCategoryReport()
        ));

        org.assertj.core.api.Assertions.assertThat(registry.exists("incident_summary")).isTrue();
        org.assertj.core.api.Assertions.assertThat(registry.exists("sla_breach")).isTrue();
        org.assertj.core.api.Assertions.assertThat(registry.exists("rca_category")).isTrue();
        org.assertj.core.api.Assertions.assertThat(registry.exists("nonexistent")).isFalse();
        org.assertj.core.api.Assertions.assertThat(registry.getAll()).hasSize(3);
    }
}
