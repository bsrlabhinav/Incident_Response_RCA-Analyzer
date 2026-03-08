package org.example.incidentresponse.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/database")
public class DatabaseController {

    private final ElasticsearchClient esClient;

    public DatabaseController(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @GetMapping("/incidents")
    public ResponseEntity<Map<String, Object>> searchIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) Boolean ackSlaBreached,
            @RequestParam(required = false) Boolean resSlaBreached,
            @RequestParam(required = false) String rcaCategory,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "50") int size
    ) throws IOException {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        if (status != null && !status.isBlank())
            bool.filter(Query.of(qb -> qb.term(t -> t.field("status").value(status))));
        if (severity != null && !severity.isBlank())
            bool.filter(Query.of(qb -> qb.term(t -> t.field("severity").value(severity))));
        if (assigneeId != null && !assigneeId.isBlank())
            bool.filter(Query.of(qb -> qb.term(t -> t.field("assigneeId").value(assigneeId))));
        if (Boolean.TRUE.equals(ackSlaBreached))
            bool.filter(Query.of(qb -> qb.term(t -> t.field("acknowledgeSlaBreached").value(true))));
        if (Boolean.TRUE.equals(resSlaBreached))
            bool.filter(Query.of(qb -> qb.term(t -> t.field("resolutionSlaBreached").value(true))));
        if (rcaCategory != null && !rcaCategory.isBlank())
            bool.filter(Query.of(qb -> qb.term(t -> t.field("rcaCategory").value(rcaCategory))));
        if (q != null && !q.isBlank())
            bool.must(Query.of(qb -> qb.multiMatch(m -> m
                    .fields("title", "description", "rcaSummary")
                    .query(q))));

        SearchRequest searchReq = SearchRequest.of(s -> s
                .index("incidents")
                .query(Query.of(qb -> qb.bool(bool.build())))
                .from(from)
                .size(size)
                .sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)))
        );

        SearchResponse<JsonNode> response = esClient.search(searchReq, JsonNode.class);

        long total = response.hits().total() != null ? response.hits().total().value() : 0;
        List<Map<String, Object>> docs = new ArrayList<>();
        for (Hit<JsonNode> hit : response.hits().hits()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("_id", hit.id());
            entry.put("_source", hit.source());
            docs.add(entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("from", from);
        result.put("size", size);
        result.put("hits", docs);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/indices")
    public ResponseEntity<Map<String, Object>> listIndices() throws IOException {
        var catResponse = esClient.cat().indices();
        List<Map<String, String>> indices = new ArrayList<>();
        for (var record : catResponse.valueBody()) {
            Map<String, String> idx = new LinkedHashMap<>();
            idx.put("index", record.index());
            idx.put("health", record.health());
            idx.put("docsCount", record.docsCount());
            idx.put("storeSize", record.storeSize());
            indices.add(idx);
        }
        return ResponseEntity.ok(Map.of("indices", indices));
    }
}
