package org.example.incidentresponse.controller;

import jakarta.validation.Valid;
import org.example.incidentresponse.reporting.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Shifu reporting API. All reporting goes through ShifuRequest -> ShifuEngine -> ShifuResponse.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ShifuEngine shifuEngine;
    private final ReportRegistry reportRegistry;

    public ReportController(ShifuEngine shifuEngine, ReportRegistry reportRegistry) {
        this.shifuEngine = shifuEngine;
        this.reportRegistry = reportRegistry;
    }

    /**
     * Execute a Shifu query. The reportType in the request selects which ReportDefinition to use.
     * Dimensions and measurements can be freely combined from the definition's available set.
     */
    @PostMapping("/query")
    public ResponseEntity<ShifuResponse> query(@Valid @RequestBody ShifuRequest request) {
        return ResponseEntity.ok(shifuEngine.execute(request));
    }

    /**
     * List all available report definitions with their dimensions and measurements.
     */
    @GetMapping("/definitions")
    public ResponseEntity<List<ReportDefinitionResponse>> listDefinitions() {
        List<ReportDefinitionResponse> defs = reportRegistry.getAll().stream()
                .map(d -> new ReportDefinitionResponse(
                        d.getReportType(), d.getIndexName(),
                        d.getAvailableDimensions(), d.getAvailableMeasurements()))
                .toList();
        return ResponseEntity.ok(defs);
    }

    /**
     * Get a single report definition's details.
     */
    @GetMapping("/definitions/{reportType}")
    public ResponseEntity<ReportDefinitionResponse> getDefinition(@PathVariable String reportType) {
        return reportRegistry.get(reportType)
                .map(d -> new ReportDefinitionResponse(
                        d.getReportType(), d.getIndexName(),
                        d.getAvailableDimensions(), d.getAvailableMeasurements()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
