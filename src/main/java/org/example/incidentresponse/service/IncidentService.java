package org.example.incidentresponse.service;

import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.dto.*;
import org.example.incidentresponse.enums.IncidentStatus;
import org.example.incidentresponse.exception.IncidentNotFoundException;
import org.example.incidentresponse.repository.IncidentEsRepository;
import org.example.incidentresponse.statemachine.IncidentStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);
    private final IncidentEsRepository incidentRepository;
    private final IncidentStateMachine stateMachine;
    private final SlaService slaService;
    private final AuditService auditService;

    public IncidentService(IncidentEsRepository incidentRepository,
                           IncidentStateMachine stateMachine,
                           SlaService slaService,
                           AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.stateMachine = stateMachine;
        this.slaService = slaService;
        this.auditService = auditService;
    }

    public IncidentResponse createIncident(CreateIncidentRequest request) {
        IncidentDocument doc = new IncidentDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setTitle(request.title());
        doc.setDescription(request.description());
        doc.setSeverity(request.severity().name());
        doc.setStatus(IncidentStatus.OPEN.name());
        doc.setReporterId(request.reporterId().toString());
        doc.setTags(request.tags() != null ? request.tags() : new HashMap<>());
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        doc.computeTimePartition();

        slaService.initSlaFields(doc);
        doc = incidentRepository.save(doc);

        MDC.put("incidentId", doc.getId());
        log.info("Incident created: id={} title={} severity={}", doc.getId(), doc.getTitle(), doc.getSeverity());
        MDC.remove("incidentId");

        return toResponse(doc);
    }

    public IncidentResponse getIncident(String id) {
        return toResponse(findOrThrow(id));
    }

    public Page<IncidentResponse> listIncidents(String status, String severity, String assigneeId, Pageable pageable) {
        if (status != null && severity != null) {
            return incidentRepository.findByStatusAndSeverity(status, severity, pageable).map(this::toResponse);
        } else if (status != null) {
            return incidentRepository.findByStatus(status, pageable).map(this::toResponse);
        } else if (severity != null) {
            return incidentRepository.findBySeverity(severity, pageable).map(this::toResponse);
        }
        return incidentRepository.findAll(pageable).map(this::toResponse);
    }

    public IncidentResponse assignOwner(String incidentId, AssignOwnerRequest request) {
        IncidentDocument doc = findOrThrow(incidentId);

        String oldAssignee = doc.getAssigneeId();
        doc.setAssigneeId(request.assigneeId().toString());
        doc.setUpdatedAt(Instant.now());
        doc = incidentRepository.save(doc);

        auditService.recordChange(incidentId, request.userId().toString(), "assigneeId",
                oldAssignee, request.assigneeId().toString());

        log.info("Owner assigned: incident={} assignee={}", incidentId, request.assigneeId());
        return toResponse(doc);
    }

    public IncidentResponse updateStatus(String incidentId, UpdateStatusRequest request) {
        IncidentDocument doc = findOrThrow(incidentId);

        MDC.put("incidentId", incidentId);
        stateMachine.transition(doc, request.status(), request.userId().toString());
        doc.setUpdatedAt(Instant.now());
        doc = incidentRepository.save(doc);
        MDC.remove("incidentId");

        return toResponse(doc);
    }

    public SlaResponse getSlaMetrics(String incidentId) {
        IncidentDocument doc = findOrThrow(incidentId);
        return slaService.getSlaMetrics(doc);
    }

    IncidentDocument findOrThrow(String id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(UUID.fromString(id)));
    }

    private IncidentResponse toResponse(IncidentDocument doc) {
        return new IncidentResponse(
                doc.getId(), doc.getTitle(), doc.getDescription(),
                doc.getSeverity(), doc.getStatus(),
                doc.getReporterId(), doc.getAssigneeId(),
                doc.getTags(), doc.getCreatedAt(), doc.getUpdatedAt());
    }
}
