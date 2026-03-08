package org.example.incidentresponse.reporting;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Translates a ShifuRequest + ReportDefinition into an Elasticsearch SearchRequest.
 * Builds time-range filters, equality filters, nested dimension aggregations,
 * and measurement sub-aggregations in a fully generic manner.
 */
@Component
public class ESSearchQueryAdapter {

    /**
     * Builds a complete ES SearchRequest from the Shifu inputs.
     */
    public SearchRequest buildSearchRequest(ShifuRequest request, ReportDefinition reportDef) {
        Query query = buildQuery(request, reportDef);
        Map<String, Aggregation> aggregations = buildAggregations(request, reportDef);

        return SearchRequest.of(s -> s
                .index(reportDef.getIndexName())
                .size(0)
                .query(query)
                .aggregations(aggregations)
        );
    }

    private Query buildQuery(ShifuRequest request, ReportDefinition reportDef) {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        // Time range filter (use epoch millis for universal ES compatibility)
        if (request.timeRange() != null) {
            long fromMs = request.timeRange().from().toEpochMilli();
            long toMs = request.timeRange().to().toEpochMilli();
            bool.must(Query.of(q -> q
                    .range(r -> r
                            .field(reportDef.getTimestampField())
                            .gte(JsonData.of(fromMs))
                            .lte(JsonData.of(toMs))
                    )
            ));
        }

        // Equality filters from the request
        if (request.filters() != null) {
            Map<String, String> dimMapping = reportDef.getDimensionFieldMapping();
            for (Map.Entry<String, List<String>> entry : request.filters().entrySet()) {
                String esField = dimMapping.getOrDefault(entry.getKey(), entry.getKey());
                List<FieldValue> values = entry.getValue().stream()
                        .map(FieldValue::of)
                        .toList();
                bool.filter(Query.of(q -> q
                        .terms(TermsQuery.of(t -> t
                                .field(esField)
                                .terms(TermsQueryField.of(tf -> tf.value(values)))
                        ))
                ));
            }
        }

        // Base filter from the report definition
        Query baseFilter = reportDef.baseFilter();
        if (baseFilter != null) {
            bool.filter(baseFilter);
        }

        return Query.of(q -> q.bool(bool.build()));
    }

    /**
     * Builds nested dimension aggregations with measurement sub-aggregations at the leaf.
     * For dimensions [A, B] and measurements [m1, m2], the structure is:
     * dim_A -> dim_B -> {m1, m2}
     */
    Map<String, Aggregation> buildAggregations(ShifuRequest request, ReportDefinition reportDef) {
        Map<String, String> dimMapping = reportDef.getDimensionFieldMapping();
        Map<String, String> measMapping = reportDef.getMeasurementFieldMapping();

        // Build leaf measurement aggregations
        Map<String, Aggregation> leafAggs = new LinkedHashMap<>();
        for (MeasurementRequest m : request.measurements()) {
            String esField = measMapping.getOrDefault(m.field(), m.field());
            leafAggs.put(m.name(), buildMeasurementAgg(m.type(), esField));
        }

        List<String> dimensions = request.dimensions();
        if (dimensions == null || dimensions.isEmpty()) {
            return leafAggs;
        }

        // Wrap dimensions from innermost to outermost
        Map<String, Aggregation> currentAggs = leafAggs;
        int maxBuckets = request.effectiveMaxBuckets();

        for (int i = dimensions.size() - 1; i >= 0; i--) {
            String dimName = dimensions.get(i);
            String esField = dimMapping.getOrDefault(dimName, dimName);
            final Map<String, Aggregation> subAggs = currentAggs;

            Aggregation termAgg = Aggregation.of(a -> a
                    .terms(t -> t.field(esField).size(maxBuckets))
                    .aggregations(subAggs)
            );

            currentAggs = Map.of("dim_" + dimName, termAgg);
        }

        return currentAggs;
    }

    private Aggregation buildMeasurementAgg(MeasurementType type, String esField) {
        return switch (type) {
            case COUNT, VALUE_COUNT -> Aggregation.of(a -> a.valueCount(v -> v.field(esField)));
            case AVG -> Aggregation.of(a -> a.avg(v -> v.field(esField)));
            case SUM -> Aggregation.of(a -> a.sum(v -> v.field(esField)));
            case MIN -> Aggregation.of(a -> a.min(v -> v.field(esField)));
            case MAX -> Aggregation.of(a -> a.max(v -> v.field(esField)));
            case CARDINALITY -> Aggregation.of(a -> a.cardinality(v -> v.field(esField)));
        };
    }
}
