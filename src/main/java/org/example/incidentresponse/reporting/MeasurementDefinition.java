package org.example.incidentresponse.reporting;

/**
 * Describes one available measurement within a report.
 *
 * @param name           Logical name used in ShifuRequest (e.g. "avg_resolution_ms")
 * @param defaultType    Default aggregation type
 * @param esFieldName    Actual Elasticsearch field (e.g. "actualResolutionMs")
 * @param displayName    Human-readable label
 */
public record MeasurementDefinition(
        String name,
        MeasurementType defaultType,
        String esFieldName,
        String displayName
) {}
