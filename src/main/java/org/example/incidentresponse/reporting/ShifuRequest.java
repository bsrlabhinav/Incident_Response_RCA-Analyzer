package org.example.incidentresponse.reporting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * The primary input to the Shifu reporting engine.
 *
 * @param reportType  Identifies which ReportDefinition to use (e.g. "incident_summary")
 * @param timeRange   Time window for the query
 * @param dimensions  Fields to group by (e.g. ["status", "severity"])
 * @param measurements Aggregations to compute within each bucket
 * @param filters     Additional equality filters (field -> list of accepted values)
 * @param maxBuckets  Maximum number of buckets per dimension level (default 100)
 */
public record ShifuRequest(
        @NotBlank String reportType,
        @NotNull @Valid TimeRange timeRange,
        List<String> dimensions,
        @NotNull List<@Valid MeasurementRequest> measurements,
        Map<String, List<String>> filters,
        Integer maxBuckets
) {
    public int effectiveMaxBuckets() {
        return maxBuckets != null && maxBuckets > 0 ? maxBuckets : 100;
    }
}
