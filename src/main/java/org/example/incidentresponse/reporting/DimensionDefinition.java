package org.example.incidentresponse.reporting;

/**
 * Describes one groupable dimension within a report.
 *
 * @param name        Logical name used in ShifuRequest (e.g. "status")
 * @param esFieldName Actual Elasticsearch field (e.g. "status")
 * @param displayName Human-readable label (e.g. "Incident Status")
 */
public record DimensionDefinition(
        String name,
        String esFieldName,
        String displayName
) {}
