package org.example.incidentresponse.reporting;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contract for a pluggable report type within the Shifu framework.
 * Implement this interface and register as a Spring bean to make the report available.
 */
public interface ReportDefinition {

    String getReportType();

    String getIndexName();

    String getTimestampField();

    Set<DimensionDefinition> getAvailableDimensions();

    Set<MeasurementDefinition> getAvailableMeasurements();

    /**
     * Optional base filter applied to every query for this report
     * (e.g., filter to a specific document type or status subset).
     */
    default Query baseFilter() {
        return null;
    }

    default Map<String, String> getDimensionFieldMapping() {
        return getAvailableDimensions().stream()
                .collect(Collectors.toMap(DimensionDefinition::name, DimensionDefinition::esFieldName));
    }

    default Map<String, String> getMeasurementFieldMapping() {
        return getAvailableMeasurements().stream()
                .collect(Collectors.toMap(MeasurementDefinition::name, MeasurementDefinition::esFieldName));
    }
}
