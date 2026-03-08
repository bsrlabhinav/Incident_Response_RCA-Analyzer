package org.example.incidentresponse.service;

import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.document.RootCauseAnalysisDocument;
import org.example.incidentresponse.dto.CreateRcaRequest;
import org.example.incidentresponse.dto.RcaResponse;
import org.example.incidentresponse.repository.IncidentEsRepository;
import org.example.incidentresponse.repository.RcaEsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;

@Service
public class RcaService {

    private static final Logger log = LoggerFactory.getLogger(RcaService.class);
    private final RcaEsRepository rcaRepository;
    private final IncidentEsRepository incidentRepository;

    public RcaService(RcaEsRepository rcaRepository, IncidentEsRepository incidentRepository) {
        this.rcaRepository = rcaRepository;
        this.incidentRepository = incidentRepository;
    }

    public RcaResponse recordRca(String incidentId, CreateRcaRequest request) {
        if (rcaRepository.existsByIncidentId(incidentId)) {
            throw new IllegalArgumentException("RCA already exists for incident: " + incidentId);
        }

        RootCauseAnalysisDocument rca = new RootCauseAnalysisDocument();
        rca.setIncidentId(incidentId);
        rca.setCategory(request.category().name());
        rca.setSummary(request.summary());
        rca.setDetails(request.details());
        rca.setActionItems(request.actionItems() != null ? request.actionItems() : Collections.emptyList());
        rca.setCreatedBy(request.createdBy().toString());
        rca.setCreatedAt(Instant.now());

        rca = rcaRepository.save(rca);

        // Denormalize RCA category onto the incident for reporting
        incidentRepository.findById(incidentId).ifPresent(incident -> {
            incident.setRcaCategory(request.category().name());
            incident.setRcaSummary(request.summary());
            incidentRepository.save(incident);
        });

        log.info("RCA recorded: incident={} category={}", incidentId, request.category());
        return toResponse(rca);
    }

    public RcaResponse getRca(String incidentId) {
        return rcaRepository.findByIncidentId(incidentId)
                .map(this::toResponse)
                .orElse(null);
    }

    public boolean hasRca(String incidentId) {
        return rcaRepository.existsByIncidentId(incidentId);
    }

    private RcaResponse toResponse(RootCauseAnalysisDocument doc) {
        return new RcaResponse(doc.getId(), doc.getIncidentId(), doc.getCategory(),
                doc.getSummary(), doc.getDetails(), doc.getActionItems(),
                doc.getCreatedBy(), doc.getCreatedAt());
    }
}
