package org.example.incidentresponse.reporting;

import java.util.Set;

/**
 * API response describing a report definition's capabilities.
 */
public record ReportDefinitionResponse(
        String reportType,
        String indexName,
        Set<DimensionDefinition> availableDimensions,
        Set<MeasurementDefinition> availableMeasurements
) {}
