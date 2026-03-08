package org.example.incidentresponse.reporting;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.example.incidentresponse.reporting.definitions.IncidentSummaryReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ESSearchQueryAdapterTest {

    private ESSearchQueryAdapter adapter;
    private ReportDefinition reportDef;

    @BeforeEach
    void setUp() {
        adapter = new ESSearchQueryAdapter();
        reportDef = new IncidentSummaryReport();
    }

    @Test
    @DisplayName("Builds search request with correct index and zero size")
    void buildSearchRequest_correctIndex() {
        ShifuRequest request = new ShifuRequest(
                "incident_summary",
                new TimeRange(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()),
                List.of("status"),
                List.of(new MeasurementRequest("incident_count", MeasurementType.VALUE_COUNT, "_id")),
                null, null);

        SearchRequest searchRequest = adapter.buildSearchRequest(request, reportDef);

        assertThat(searchRequest.index()).contains("incidents");
        assertThat(searchRequest.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Single dimension produces one level of terms aggregation")
    void singleDimension_termsAgg() {
        ShifuRequest request = new ShifuRequest(
                "incident_summary",
                new TimeRange(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()),
                List.of("status"),
                List.of(new MeasurementRequest("incident_count", MeasurementType.VALUE_COUNT, "_id")),
                null, null);

        Map<String, Aggregation> aggs = adapter.buildAggregations(request, reportDef);

        assertThat(aggs).containsKey("dim_status");
        Aggregation dimAgg = aggs.get("dim_status");
        assertThat(dimAgg.terms()).isNotNull();
        assertThat(dimAgg.aggregations()).containsKey("incident_count");
    }

    @Test
    @DisplayName("Multiple dimensions produce nested terms aggregations")
    void multipleDimensions_nestedAggs() {
        ShifuRequest request = new ShifuRequest(
                "incident_summary",
                new TimeRange(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()),
                List.of("status", "severity"),
                List.of(new MeasurementRequest("incident_count", MeasurementType.VALUE_COUNT, "_id")),
                null, null);

        Map<String, Aggregation> aggs = adapter.buildAggregations(request, reportDef);

        assertThat(aggs).containsKey("dim_status");
        Aggregation outerAgg = aggs.get("dim_status");
        assertThat(outerAgg.aggregations()).containsKey("dim_severity");
        Aggregation innerAgg = outerAgg.aggregations().get("dim_severity");
        assertThat(innerAgg.aggregations()).containsKey("incident_count");
    }

    @Test
    @DisplayName("No dimensions returns only measurement aggregations at root")
    void noDimensions_leafAggsOnly() {
        ShifuRequest request = new ShifuRequest(
                "incident_summary",
                new TimeRange(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()),
                List.of(),
                List.of(
                        new MeasurementRequest("incident_count", MeasurementType.VALUE_COUNT, "_id"),
                        new MeasurementRequest("avg_resolution_ms", MeasurementType.AVG, "actualResolutionMs")),
                null, null);

        Map<String, Aggregation> aggs = adapter.buildAggregations(request, reportDef);

        assertThat(aggs).containsKey("incident_count");
        assertThat(aggs).containsKey("avg_resolution_ms");
        assertThat(aggs).doesNotContainKey("dim_status");
    }

    @Test
    @DisplayName("Multiple measurement types are built correctly")
    void multipleMeasurements() {
        ShifuRequest request = new ShifuRequest(
                "incident_summary",
                new TimeRange(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()),
                List.of(),
                List.of(
                        new MeasurementRequest("count", MeasurementType.VALUE_COUNT, "_id"),
                        new MeasurementRequest("avg_res", MeasurementType.AVG, "actualResolutionMs"),
                        new MeasurementRequest("max_res", MeasurementType.MAX, "actualResolutionMs"),
                        new MeasurementRequest("min_res", MeasurementType.MIN, "actualResolutionMs"),
                        new MeasurementRequest("sum_res", MeasurementType.SUM, "actualResolutionMs"),
                        new MeasurementRequest("unique", MeasurementType.CARDINALITY, "assigneeId")),
                null, null);

        Map<String, Aggregation> aggs = adapter.buildAggregations(request, reportDef);

        assertThat(aggs).hasSize(6);
        assertThat(aggs.get("count").valueCount()).isNotNull();
        assertThat(aggs.get("avg_res").avg()).isNotNull();
        assertThat(aggs.get("max_res").max()).isNotNull();
        assertThat(aggs.get("min_res").min()).isNotNull();
        assertThat(aggs.get("sum_res").sum()).isNotNull();
        assertThat(aggs.get("unique").cardinality()).isNotNull();
    }

    @Test
    @DisplayName("Filters are applied as terms queries")
    void filtersApplied() {
        ShifuRequest request = new ShifuRequest(
                "incident_summary",
                new TimeRange(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()),
                List.of("status"),
                List.of(new MeasurementRequest("count", MeasurementType.VALUE_COUNT, "_id")),
                Map.of("severity", List.of("CRITICAL", "HIGH")),
                null);

        SearchRequest searchRequest = adapter.buildSearchRequest(request, reportDef);

        assertThat(searchRequest.query()).isNotNull();
        assertThat(searchRequest.query().bool()).isNotNull();
        assertThat(searchRequest.query().bool().filter()).isNotEmpty();
    }
}
