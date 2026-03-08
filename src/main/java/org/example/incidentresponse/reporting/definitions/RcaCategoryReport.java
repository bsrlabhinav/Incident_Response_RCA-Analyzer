package org.example.incidentresponse.reporting.definitions;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.example.incidentresponse.reporting.*;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * RCA category report -- only considers incidents that have an RCA recorded,
 * allowing grouping by root cause category and severity.
 */
@Component
public class RcaCategoryReport implements ReportDefinition {

    @Override
    public String getReportType() {
        return "rca_category";
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
                new DimensionDefinition("rca_category", "rcaCategory", "RCA Category"),
                new DimensionDefinition("severity", "severity", "Severity"),
                new DimensionDefinition("status", "status", "Incident Status"),
                new DimensionDefinition("time_partition", "timePartition", "Time Partition")
        );
    }

    @Override
    public Set<MeasurementDefinition> getAvailableMeasurements() {
        return Set.of(
                new MeasurementDefinition("incident_count", MeasurementType.VALUE_COUNT, "status", "Incident Count"),
                new MeasurementDefinition("avg_resolution_ms", MeasurementType.AVG, "actualResolutionMs", "Avg Resolution Time (ms)"),
                new MeasurementDefinition("unique_assignees", MeasurementType.CARDINALITY, "assigneeId", "Unique Assignees")
        );
    }

    @Override
    public Query baseFilter() {
        return Query.of(q -> q.exists(e -> e.field("rcaCategory")));
    }
}
