package org.example.incidentresponse.controller;

import jakarta.validation.Valid;
import org.example.incidentresponse.dto.*;
import org.example.incidentresponse.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final AuditService auditService;
    private final EvidenceService evidenceService;
    private final RcaService rcaService;

    public IncidentController(IncidentService incidentService,
                              AuditService auditService,
                              EvidenceService evidenceService,
                              RcaService rcaService) {
        this.incidentService = incidentService;
        this.auditService = auditService;
        this.evidenceService = evidenceService;
        this.rcaService = rcaService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.createIncident(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncident(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.getIncident(id));
    }

    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> listIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String assigneeId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(incidentService.listIncidents(status, severity, assigneeId, pageable));
    }

    @PutMapping("/{id}/owner")
    public ResponseEntity<IncidentResponse> assignOwner(
            @PathVariable String id,
            @Valid @RequestBody AssignOwnerRequest request) {
        return ResponseEntity.ok(incidentService.assignOwner(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<IncidentResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(incidentService.updateStatus(id, request));
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<EvidenceResponse> attachEvidence(
            @PathVariable String id,
            @Valid @RequestBody CreateEvidenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evidenceService.attachEvidence(id, request));
    }

    @PostMapping("/{id}/rca")
    public ResponseEntity<RcaResponse> recordRca(
            @PathVariable String id,
            @Valid @RequestBody CreateRcaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rcaService.recordRca(id, request));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<IncidentAuditResponse>> getAuditTrail(@PathVariable String id) {
        return ResponseEntity.ok(auditService.getAuditTrail(id));
    }

    @GetMapping("/{id}/sla")
    public ResponseEntity<SlaResponse> getSlaMetrics(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.getSlaMetrics(id));
    }
}
