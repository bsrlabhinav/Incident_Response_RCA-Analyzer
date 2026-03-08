package org.example.incidentresponse.reporting;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorCause;

import java.io.IOException;
import java.util.*;

/**
 * Orchestrates the Shifu reporting pipeline:
 * 1. Validates the ShifuRequest against the ReportDefinition
 * 2. Delegates to ESSearchQueryAdapter to build the ES query
 * 3. Executes the query
 * 4. Flattens the nested ES aggregation response into ShifuBuckets
 */
@Service
public class ShifuEngine {

    private static final Logger log = LoggerFactory.getLogger(ShifuEngine.class);

    private final ReportRegistry registry;
    private final ESSearchQueryAdapter adapter;
    private final ElasticsearchClient esClient;

    public ShifuEngine(ReportRegistry registry, ESSearchQueryAdapter adapter, ElasticsearchClient esClient) {
        this.registry = registry;
        this.adapter = adapter;
        this.esClient = esClient;
    }

    public ShifuResponse execute(ShifuRequest request) {
        ReportDefinition reportDef = registry.get(request.reportType())
                .orElseThrow(() -> new IllegalArgumentException("Unknown report type: " + request.reportType()));

        validate(request, reportDef);

        SearchRequest searchRequest = adapter.buildSearchRequest(request, reportDef);
        log.debug("Executing Shifu query for report={} dimensions={} measurements={}",
                request.reportType(), request.dimensions(), request.measurements());

        try {
            SearchResponse<Void> response = esClient.search(searchRequest, Void.class);
            return buildResponse(request, response);
        } catch (ElasticsearchException e) {
            ErrorCause root = e.response().error();
            String rootReason = root.reason() != null ? root.reason() : "unknown";
            ErrorCause cause = root.causedBy();
            String causeDetail = cause != null
                    ? cause.type() + ": " + cause.reason()
                    : "no nested cause";
            log.error("ES query failed for report={}: {} — root cause: {}", request.reportType(), rootReason, causeDetail);
            throw new RuntimeException("Elasticsearch query failed: " + rootReason + " [" + causeDetail + "]", e);
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch query failed for report: " + request.reportType(), e);
        }
    }

    private void validate(ShifuRequest request, ReportDefinition reportDef) {
        Set<String> availableDims = reportDef.getDimensionFieldMapping().keySet();
        Map<String, String> availableMeas = reportDef.getMeasurementFieldMapping();

        if (request.dimensions() != null) {
            for (String dim : request.dimensions()) {
                if (!availableDims.contains(dim)) {
                    throw new IllegalArgumentException(
                            "Dimension '" + dim + "' is not available for report '" + request.reportType()
                                    + "'. Available: " + availableDims);
                }
            }
        }

        for (MeasurementRequest m : request.measurements()) {
            if (m.field() != null && !availableMeas.containsKey(m.field())) {
                throw new IllegalArgumentException(
                        "Measurement field '" + m.field() + "' is not available for report '" + request.reportType()
                                + "'. Available: " + availableMeas.keySet());
            }
        }
    }

    private ShifuResponse buildResponse(ShifuRequest request, SearchResponse<Void> response) {
        long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;

        List<String> dimensions = request.dimensions() != null ? request.dimensions() : List.of();
        List<ShifuBucket> buckets;

        if (dimensions.isEmpty()) {
            Map<String, Object> measurements = extractMeasurements(response.aggregations(), request.measurements());
            buckets = List.of(new ShifuBucket(Map.of(), measurements, totalHits));
        } else {
            buckets = flattenBuckets(response.aggregations(), dimensions, 0,
                    new LinkedHashMap<>(), request.measurements());
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("totalHits", totalHits);

        return new ShifuResponse(request.reportType(), request.timeRange(), totalHits, totals, buckets);
    }

    /**
     * Recursively walks nested terms aggregations and flattens them into ShifuBuckets.
     */
    private List<ShifuBucket> flattenBuckets(Map<String, Aggregate> aggregations,
                                              List<String> dimensions,
                                              int dimIndex,
                                              Map<String, String> currentDimValues,
                                              List<MeasurementRequest> measurements) {
        if (dimIndex >= dimensions.size()) {
            Map<String, Object> mValues = extractMeasurements(aggregations, measurements);
            return List.of(new ShifuBucket(new LinkedHashMap<>(currentDimValues), mValues, -1));
        }

        String dimName = dimensions.get(dimIndex);
        String aggKey = "dim_" + dimName;

        Aggregate aggregate = aggregations.get(aggKey);
        if (aggregate == null) {
            return List.of();
        }

        List<ShifuBucket> result = new ArrayList<>();
        for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
            currentDimValues.put(dimName, bucket.key().stringValue());
            List<ShifuBucket> childBuckets = flattenBuckets(
                    bucket.aggregations(), dimensions, dimIndex + 1, currentDimValues, measurements);

            for (ShifuBucket child : childBuckets) {
                result.add(new ShifuBucket(child.dimensions(), child.measurements(), bucket.docCount()));
            }
            currentDimValues.remove(dimName);
        }

        return result;
    }

    private Map<String, Object> extractMeasurements(Map<String, Aggregate> aggregations,
                                                     List<MeasurementRequest> measurements) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (MeasurementRequest m : measurements) {
            Aggregate agg = aggregations.get(m.name());
            if (agg == null) continue;

            Object value = switch (m.type()) {
                case AVG -> agg.avg().value();
                case SUM -> agg.sum().value();
                case MIN -> agg.min().value();
                case MAX -> agg.max().value();
                case COUNT, VALUE_COUNT -> agg.valueCount().value();
                case CARDINALITY -> agg.cardinality().value();
            };
            result.put(m.name(), value);
        }
        return result;
    }
}
