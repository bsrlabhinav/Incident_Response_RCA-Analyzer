package org.example.incidentresponse.service;

import org.example.incidentresponse.document.IncidentAuditDocument;
import org.example.incidentresponse.dto.IncidentAuditResponse;
import org.example.incidentresponse.repository.IncidentAuditEsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final IncidentAuditEsRepository auditRepository;

    public AuditService(IncidentAuditEsRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void recordChange(String incidentId, String userId, String field, String oldValue, String newValue) {
        IncidentAuditDocument audit = new IncidentAuditDocument(incidentId, userId, field, oldValue, newValue);
        auditRepository.save(audit);
        log.debug("Audit recorded: incident={} field={} {} -> {}", incidentId, field, oldValue, newValue);
    }

    public List<IncidentAuditResponse> getAuditTrail(String incidentId) {
        return auditRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream()
                .map(this::toResponse)
                .toList();
    }

    private IncidentAuditResponse toResponse(IncidentAuditDocument doc) {
        return new IncidentAuditResponse(doc.getId(), doc.getIncidentId(), doc.getUserId(),
                doc.getField(), doc.getOldValue(), doc.getNewValue(), doc.getCreatedAt());
    }
}
