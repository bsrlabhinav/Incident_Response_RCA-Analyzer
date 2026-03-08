package org.example.incidentresponse.reporting.definitions;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.example.incidentresponse.reporting.*;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * SLA breach report -- filters to incidents with at least one SLA breach,
 * then allows grouping by severity, status, assignee.
 */
@Component
public class SlaBreachReport implements ReportDefinition {

    @Override
    public String getReportType() {
        return "sla_breach";
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
                new DimensionDefinition("severity", "severity", "Severity"),
                new DimensionDefinition("status", "status", "Incident Status"),
                new DimensionDefinition("assignee", "assigneeId", "Assignee"),
                new DimensionDefinition("time_partition", "timePartition", "Time Partition")
        );
    }

    @Override
    public Set<MeasurementDefinition> getAvailableMeasurements() {
        return Set.of(
                new MeasurementDefinition("breach_count", MeasurementType.VALUE_COUNT, "status", "Breach Count"),
                new MeasurementDefinition("avg_resolution_ms", MeasurementType.AVG, "actualResolutionMs", "Avg Resolution Time (ms)"),
                new MeasurementDefinition("avg_acknowledge_ms", MeasurementType.AVG, "actualAcknowledgeMs", "Avg Acknowledge Time (ms)"),
                new MeasurementDefinition("max_resolution_ms", MeasurementType.MAX, "actualResolutionMs", "Max Resolution Time (ms)")
        );
    }

    @Override
    public Query baseFilter() {
        return Query.of(q -> q.bool(BoolQuery.of(b -> b
                .should(Query.of(sq -> sq.term(t -> t.field("acknowledgeSlaBreached").value(true))))
                .should(Query.of(sq -> sq.term(t -> t.field("resolutionSlaBreached").value(true))))
                .minimumShouldMatch("1")
        )));
    }
}
