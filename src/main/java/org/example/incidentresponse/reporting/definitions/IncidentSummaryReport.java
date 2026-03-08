package org.example.incidentresponse.reporting.definitions;

import org.example.incidentresponse.reporting.*;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Incident summary report -- supports grouping by status, severity, assignee, and time partition
 * with measurements like count, avg resolution time, avg acknowledge time.
 */
@Component
public class IncidentSummaryReport implements ReportDefinition {

    @Override
    public String getReportType() {
        return "incident_summary";
    }

    @Override
    public String getIndexName() {
        return "incidents";
    }

    @Override
    public String getTimestampField() {
        return "createdAt";
    }

    @Override
    public Set<DimensionDefinition> getAvailableDimensions() {
        return Set.of(
                new DimensionDefinition("status", "status", "Incident Status"),
                new DimensionDefinition("severity", "severity", "Severity"),
                new DimensionDefinition("assignee", "assigneeId", "Assignee"),
                new DimensionDefinition("time_partition", "timePartition", "Time Partition"),
                new DimensionDefinition("rca_category", "rcaCategory", "RCA Category")
        );
    }

    @Override
    public Set<MeasurementDefinition> getAvailableMeasurements() {
        return Set.of(
                new MeasurementDefinition("incident_count", MeasurementType.VALUE_COUNT, "status", "Incident Count"),
                new MeasurementDefinition("avg_resolution_ms", MeasurementType.AVG, "actualResolutionMs", "Avg Resolution Time (ms)"),
                new MeasurementDefinition("avg_acknowledge_ms", MeasurementType.AVG, "actualAcknowledgeMs", "Avg Acknowledge Time (ms)"),
                new MeasurementDefinition("max_resolution_ms", MeasurementType.MAX, "actualResolutionMs", "Max Resolution Time (ms)"),
                new MeasurementDefinition("unique_assignees", MeasurementType.CARDINALITY, "assigneeId", "Unique Assignees")
        );
    }
}
